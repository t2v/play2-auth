package jp.t2v.lab.play20.auth

import play.api.mvc._

trait Auth {
  self: Controller with AuthConfig =>

  def authorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result): Action[AnyContent] =
    authorizedAction(BodyParsers.parse.anyContent, authority)(f)

  def authorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result): Action[A] =
    Action(p)(req => authorized(authority)(req).right.map(u => f(u)(req)).merge)

  def authorized[A](authority: Authority)(implicit request: Request[A]): Either[PlainResult, User] = for {
    user <- restoreUser(request).toRight(authenticationFailed(request)).right
    _ <- Either.cond(authorize(user, authority), (), authorizationFailed(request)).right
  } yield user

  private def restoreUser[A](implicit request: Request[A]): Option[User] = for {
    sessionId <- request.session.get("sessionId")
    userId <- resolver.sessionId2userId(sessionId)
    user <- resolveUser(userId)
  } yield {
    resolver.prolongTimeout(sessionId, sessionTimeoutInSeconds)
    user
  }

}
