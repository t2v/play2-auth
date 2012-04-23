package jp.t2v.lab.play20.auth

import play.api.mvc.{Action, PlainResult, Request, Controller}
import play.api.cache.Cache
import play.api.Play._

trait Auth {
  self: Controller with AuthConfig =>

  def authorizedAction(authority: AUTHORITY)(f: USER => Request[Any] => PlainResult) =
    Action(req => authorized(authority)(req).right.map(u => f(u)(req)).merge)

  def authorized(authority: AUTHORITY)(implicit request: Request[Any]): Either[PlainResult, USER] = for {
    user <- restoreUser(request).toRight(authenticationFailed).right
    _ <- Either.cond(authorize(user, authority), (), authorizationFailed).right
  } yield user

  private def restoreUser(request: Request[Any]): Option[USER] = for {
    sessionId <- request.session.get("sessionId")
    userId <- Cache.getAs[ID](sessionId + ":sessionId")(current, idManifest)
    user <- resolveUser(userId)
  } yield {
    Cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    Cache.set(userId.toString + ":userId", sessionId, sessionTimeoutInSeconds)
    user
  }

}
