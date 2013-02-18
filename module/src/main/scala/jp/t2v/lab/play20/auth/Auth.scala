package jp.t2v.lab.play20.auth

import play.api.mvc._
import play.api.libs.iteratee.{Input, Done}

trait Auth {
  self: AuthConfig =>

  def authorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result): Action[(AnyContent, User)] =
    authorizedAction(BodyParsers.parse.anyContent, authority)(f)

  def authorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result): Action[(A, User)] = {
    val parser = BodyParser {
      req => authorized(authority)(req) match {
        case Right(user)  => p.map((_, user))(req)
        case Left(result) => Done(Left(result), Input.Empty)
      }
    }
    Action(parser) { req => f(req.body._2)(req.map(_._1)) }
  }

  def optionalUserAction(f: Option[User] => Request[AnyContent] => Result): Action[AnyContent] =
    optionalUserAction(BodyParsers.parse.anyContent)(f)

  def optionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result): Action[A] =
    Action(p)(req => f(restoreUser(req))(req))

  def authorized(authority: Authority)(implicit request: RequestHeader): Either[Result, User] = for {
    user <- restoreUser(request).toRight(authenticationFailed(request)).right
    _    <- Either.cond(authorize(user, authority), (), authorizationFailed(request)).right
  } yield user

  private[auth] def restoreUser(implicit request: RequestHeader): Option[User] = for {
    cookie <- request.cookies.get(cookieName)
    token  <- CookieUtil.verifyHmac(cookie)
    userId <- idContainer.get(token)
    user   <- resolveUser(userId)
  } yield {
    idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
    user
  }

}
