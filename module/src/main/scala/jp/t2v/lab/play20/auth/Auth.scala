package jp.t2v.lab.play20.auth

import play.api.cache.Cache
import play.api.Play._
import play.api.mvc._

trait Auth {
  self: Controller with AuthConfig =>

  def authorizedAction(authority: Authority)(f: User => Request[Any] => Result) =
    Action(req => authorized(authority)(req).right.map(u => f(u)(req)).merge)

  def authorized(authority: Authority)(implicit request: Request[Any]): Either[PlainResult, User] = for {
    user <- restoreUser(request).toRight(authenticationFailed(request)).right
    _ <- Either.cond(authorize(user, authority), (), authorizationFailed(request)).right
  } yield user

  private def restoreUser(request: Request[Any]): Option[User] = for {
    sessionId <- request.session.get("sessionId")
    userId <- Cache.getAs[Id](sessionId + ":sessionId")(current, idManifest)
    user <- resolveUser(userId)
  } yield {
    Cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    Cache.set(userId.toString + ":userId", sessionId, sessionTimeoutInSeconds)
    user
  }

}
