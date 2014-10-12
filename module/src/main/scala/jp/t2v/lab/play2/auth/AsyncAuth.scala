package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Done}
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth {
    self: AuthConfig with Controller =>

  final case class GenericOptionalAuthRequest[A, R[_] <: Request[_]](user: Option[User], request: R[A]) extends WrappedRequest[A](request.asInstanceOf[Request[A]])
  final case class GenericAuthRequest[A, R[_] <: Request[_]](user: User, request: R[A]) extends WrappedRequest[A](request.asInstanceOf[Request[A]])

  final type OptionalAuthRequest[A] = GenericOptionalAuthRequest[A, Request]
  object OptionalAuthRequest {
    def apply[A](user: Option[User], request: Request[A]): OptionalAuthRequest[A] = GenericOptionalAuthRequest[A, Request](user, request)
  }
  final type AuthRequest[A] = GenericAuthRequest[A, Request]
  object AuthRequest {
    def apply[A](user: User, request: Request[A]): AuthRequest[A] = GenericAuthRequest[A, Request](user, request)
  }

  final case class GenericOptionalAuthRefiner[R[_] <: Request[_]]() extends ActionRefiner[R, ({type L[B] = GenericOptionalAuthRequest[B, R]})#L] {
    protected def refine[A](request: R[A]): Future[Either[Result, GenericOptionalAuthRequest[A, R]]] = {
      implicit val ctx = executionContext
      restoreUser(request.asInstanceOf[RequestHeader], executionContext) recover {
        case _ => None
      } map { user =>
        Right(GenericOptionalAuthRequest[A, R](user, request))
      }
    }
  }

  final val OptionalAuthRefiner: ActionRefiner[Request, OptionalAuthRequest] = GenericOptionalAuthRefiner[Request]()
  final val OptionalAuthAction: ActionBuilder[OptionalAuthRequest] = Action andThen OptionalAuthRefiner

  final case class GenericAuthenticationRefiner[R[_] <: Request[_]]() extends ActionRefiner[({type L[B] = GenericOptionalAuthRequest[B, R]})#L, ({type L[C] = GenericAuthRequest[C, R]})#L] {
    override protected def refine[A](request: GenericOptionalAuthRequest[A, R]): Future[Either[Result, GenericAuthRequest[A, R]]] = {
      request.user map { user =>
        Future.successful(Right[Result, GenericAuthRequest[A, R]](new GenericAuthRequest[A, R](user, request.request)))
      } getOrElse {
        implicit val ctx = executionContext
        authenticationFailed(request).map(Left.apply[Result, GenericAuthRequest[A, R]])
      }
    }
  }

  final val AuthenticationRefiner: ActionRefiner[OptionalAuthRequest, AuthRequest] = GenericAuthenticationRefiner[Request]()

  final case class GenericAuthorizationFilter[R[_] <: Request[_]](authority: Authority) extends ActionFilter[({type L[B] = GenericAuthRequest[B, R]})#L] {
    override protected def filter[A](request: GenericAuthRequest[A, R]): Future[Option[Result]] = {
      implicit val ctx = executionContext
      authorize(request.user, authority) collect {
        case true => None
      } recoverWith {
        case _ => authorizationFailed(request).map(Some.apply)
      }
    }
  }

  final def AuthorizationFilter(authority: Authority): ActionFilter[AuthRequest] = GenericAuthorizationFilter[Request](authority)

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
