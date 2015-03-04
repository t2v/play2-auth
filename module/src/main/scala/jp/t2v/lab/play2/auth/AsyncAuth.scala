package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Done}
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth {
    self: AuthConfig with Controller =>

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, (User, ResultUpdater)]] = {
    restoreUser collect {
      case (Some(user), resultUpdater) => Right(user -> resultUpdater)
    } recoverWith {
      case _ => authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right((user, resultUpdater)) => authorize(user, authority) collect {
        case true => Right(user -> resultUpdater)
      } recoverWith {
        case _ => authorizationFailed(request, user, Some(authority)).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[(Option[User], ResultUpdater)] = {
    (for {
      token  <- tokenAccessor.extract(request)
    } yield for {
      Some(userId) <- idContainer.get(token)
      Some(user)   <- resolveUser(userId)
      _            <- idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
    } yield {
      Option(user) -> tokenAccessor.put(token) _
    }) getOrElse {
      Future.successful(Option.empty -> identity)
    }
  }

  @deprecated(message = "AuthActionBuilder#AuthorizationAction should be preferred", since = "0.13.0")
  object authorizedAction {
    def async(authority: Authority)(f: User => Request[AnyContent] => Future[Result])(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(BodyParsers.parse.anyContent, authority)(f)

    def async[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[(A, User)] = {
      val parser = BodyParser {
        req => Iteratee.flatten(authorized(authority)(req, context).map {
          case Right((user, _))  => p.map((_, user)).apply(req)
          case Left(result) => Done[Array[Byte], Either[Result, (A, User)]](Left(result))
        })
      }
      Action.async(parser) { req => f(req.body._2)(req.map(_._1)) }
    }

    def apply(authority: Authority)(f: User => (Request[AnyContent] => Result))(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(authority)(f.andThen(_.andThen(Future.successful)))

    def apply[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result)(implicit context: ExecutionContext): Action[(A, User)] =
      async(p,authority)(f.andThen(_.andThen(Future.successful)))
  }

  @deprecated(message = "AuthActionBuilder#OptionalAuthAction should be preferred", since = "0.13.0")
  object optionalUserAction {
    def async(f: Option[User] => Request[AnyContent] => Future[Result])(implicit context: ExecutionContext): Action[AnyContent] =
      async(BodyParsers.parse.anyContent)(f)

    def async[A](p: BodyParser[A])(f: Option[User] => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] =
      Action.async(p)(req => restoreUser(req, context).flatMap { case (user, _) => f(user)(req)})

    def apply(f: Option[User] => (Request[AnyContent] => Result))(implicit context: ExecutionContext): Action[AnyContent] =
      async(f.andThen(_.andThen(Future.successful)))

    def apply[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result)(implicit context: ExecutionContext): Action[A] =
      async(p)(f.andThen(_.andThen(Future.successful)))
  }

}
