package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Input, Done}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait AsyncAuth {
    self: AuthConfig with Controller =>

  object authorizedAction {
    def async(authority: Authority)(f: User => Request[AnyContent] => Future[SimpleResult])(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(BodyParsers.parse.anyContent, authority)(f)

    def async[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Future[SimpleResult])(implicit context: ExecutionContext): Action[(A, User)] = {
      val parser = BodyParser {
        req => Iteratee.flatten(authorized(authority)(req, context).map {
          case Right(user)  => p.map((_, user))(req)
          case Left(result) => Done[Array[Byte], Either[SimpleResult, (A, User)]](Left(result))
        })
      }
      Action.async(parser) { req => f(req.body._2)(req.map(_._1)) }
    }

    def apply(authority: Authority)(f: User => (Request[AnyContent] => SimpleResult))(implicit context: ExecutionContext): Action[(AnyContent, User)] =
      async(authority)(f.andThen(_.andThen(t=>Future.successful(t))))
    def apply[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => SimpleResult)(implicit context: ExecutionContext): Action[(A, User)] =
      async(p,authority)(f.andThen(_.andThen(t=>Future.successful(t))))
  }

  def optionalUserAction(f: Option[User] => Request[AnyContent] => Future[SimpleResult])(implicit context: ExecutionContext): Action[AnyContent] =
    optionalUserAction(BodyParsers.parse.anyContent)(f)

  def optionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Future[SimpleResult])(implicit context: ExecutionContext): Action[A] =
    Action.async(p)(req => restoreUser(req, context).flatMap(user => f(user)(req)) )

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[SimpleResult, User]] = {
    restoreUser collect {
      case Some(user) => Right(user)
    } recoverWith {
      case _ => authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right(user)  => authorize(user, authority, request) collect {
        case true => Right(user)
      } recoverWith {
        case _ => authorizationFailed(request).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[Option[User]] = {
    val userIdOpt = for {
      cookie <- request.cookies.get(cookieName)
      token  <- CookieUtil.verifyHmac(cookie)
      userId <- idContainer.get(token)
    } yield (token, userId)
    userIdOpt map { case (token, userId) =>
      resolveUser(userId) andThen {
        case Success(Some(_)) => idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
      }
    } getOrElse {
      Future.successful(Option.empty)
    }
  }


}
