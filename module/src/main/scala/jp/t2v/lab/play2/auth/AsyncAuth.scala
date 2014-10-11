package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Done}
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth {
    self: AuthConfig with Controller =>

  case class OptionalAuthRequest[A](user: Option[User], request: Request[A]) extends WrappedRequest[A](request)
  case class AuthRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

  object OptionalAuthAction extends ActionBuilder[OptionalAuthRequest] {
    override def invokeBlock[A](request: Request[A], block: (OptionalAuthRequest[A]) => Future[Result]): Future[Result] = {
      implicit val ctx = executionContext
      restoreUser(request, executionContext) recover {
        case _ => None
      } map {
        OptionalAuthRequest(_, request)
      } flatMap {
        block
      }
    }
  }

  object AuthenticationRefiner extends ActionRefiner[OptionalAuthRequest, AuthRequest] {
    override protected def refine[A](request: OptionalAuthRequest[A]): Future[Either[Result, AuthRequest[A]]] = {
      request.user map { user =>
        Future.successful(Right[Result, AuthRequest[A]](AuthRequest[A](user, request)))
      } getOrElse {
        implicit val ctx = executionContext
        authenticationFailed(request).map(Left.apply[Result, AuthRequest[A]])
      }
    }
  }

  case class AuthorizationFilter(authority: Authority) extends ActionFilter[AuthRequest] {
    override protected def filter[A](request: AuthRequest[A]): Future[Option[Result]] = {
      implicit val ctx = executionContext
      authorize(request.user, authority) collect {
        case true => None
      } recoverWith {
        case _ => authorizationFailed(request).map(Some.apply)
      }
    }
  }

  final val AuthenticationAction: ActionBuilder[AuthRequest] = OptionalAuthAction andThen AuthenticationRefiner
  final def AuthorizationAction(authority: Authority): ActionBuilder[AuthRequest] = AuthenticationAction andThen AuthorizationFilter(authority)

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, User]] = {
    restoreUser collect {
      case Some(user) => Right(user)
    } recoverWith {
      case _ => authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right(user)  => authorize(user, authority) collect {
        case true => Right(user)
      } recoverWith {
        case _ => authorizationFailed(request).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[Option[User]] = {
    (for {
      cookie <- request.cookies.get(cookieName)
      token  <- CookieUtil.verifyHmac(cookie)
    } yield for {
      Some(userId) <- idContainer.get(token)
      Some(user)   <- resolveUser(userId)
      _            <- idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
    } yield {
      Option(user)
    }) getOrElse {
      Future.successful(Option.empty)
    }
  }

  @deprecated
  object authorizedAction {
    def async(authority: Authority)(f: User => Request[AnyContent] => Future[Result])(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(BodyParsers.parse.anyContent, authority)(f)

    def async[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[(A, User)] = {
      val parser = BodyParser {
        req => Iteratee.flatten(authorized(authority)(req, context).map {
          case Right(user)  => p.map((_, user)).apply(req)
          case Left(result) => Done[Array[Byte], Either[Result, (A, User)]](Left(result))
        })
      }
      Action.async(parser) { req => f(req.body._2)(req.map(_._1)) }
    }

    def apply(authority: Authority)(f: User => (Request[AnyContent] => Result))(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(authority)(f.andThen(_.andThen(t=>Future.successful(t))))

    def apply[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result)(implicit context: ExecutionContext): Action[(A, User)] =
      async(p,authority)(f.andThen(_.andThen(t=>Future.successful(t))))
  }

  @deprecated
  object optionalUserAction {
    def async(f: Option[User] => Request[AnyContent] => Future[Result])(implicit context: ExecutionContext): Action[AnyContent] =
      async(BodyParsers.parse.anyContent)(f)

    def async[A](p: BodyParser[A])(f: Option[User] => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] =
      Action.async(p)(req => restoreUser(req, context).flatMap(user => f(user)(req)) )

    def apply(f: Option[User] => (Request[AnyContent] => Result))(implicit context: ExecutionContext): Action[AnyContent] =
      async(f.andThen(_.andThen(t=>Future.successful(t))))

    def apply[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result)(implicit context: ExecutionContext): Action[A] =
      async(p)(f.andThen(_.andThen(t=>Future.successful(t))))
  }

}
