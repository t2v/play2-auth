package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Input, Done}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

@deprecated
trait Auth {
  self: AuthConfig =>

  private implicit val ctx = play.api.libs.concurrent.Execution.defaultContext

  object authorizedAction {
    def async(authority: Authority)(f: User => Request[AnyContent] => Future[SimpleResult]): Action[(AnyContent, User)] =
      async(BodyParsers.parse.anyContent, authority)(f)

    def async[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Future[SimpleResult]): Action[(A, User)] = {
      val parser = BodyParser {
        req => authorized(authority)(req) match {
          case Right(user)  => p.map((_, user))(req)
          case Left(result) => Done(Left(result), Input.Empty)
        }
      }
      Action.async(parser) { req => f(req.body._2)(req.map(_._1)) }
    }

    def apply(authority: Authority)(f: User => (Request[AnyContent] => SimpleResult)): Action[(AnyContent, User)] =
      async(authority)(f.andThen(_.andThen(t=>Future.successful(t))))
    def apply[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => SimpleResult): Action[(A, User)] =
      async(p,authority)(f.andThen(_.andThen(t=>Future.successful(t))))
  }

  def optionalUserAction(f: Option[User] => Request[AnyContent] => Future[SimpleResult]): Action[AnyContent] =
    optionalUserAction(BodyParsers.parse.anyContent)(f)

  def optionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Future[SimpleResult]): Action[A] =
    Action.async(p)(req => f(restoreUser(req))(req))

  def authorized(authority: Authority)(implicit request: RequestHeader): Either[SimpleResult, User] = for {
    user <- restoreUser(request).toRight(Await.result(authenticationFailed(request), 10.seconds)).right
    _    <- Either.cond(Await.result(authorize(user, authority), 10.seconds), (), Await.result(authorizationFailed(request), 10.seconds)).right
  } yield user

  private[auth] def restoreUser(implicit request: RequestHeader): Option[User] = for {
    cookie <- request.cookies.get(cookieName)
    token  <- CookieUtil.verifyHmac(cookie)
    userId <- idContainer.get(token)
    user   <- Await.result(resolveUser(userId), 10.seconds)
  } yield {
    idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
    user
  }

}
