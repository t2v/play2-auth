package jp.t2v.lab.play20.auth

import play.api.cache.Cache
import play.api.Play._
import play.api.mvc.Request

private[auth] trait CacheAdapter {
  self: AuthConfig =>

  private[auth] val sessionIdSuffix = ":sessionId"
  private[auth] val userIdSuffix = ":userId"

  private[auth] def getUserId(sessionId: String): Option[Id] =
    Cache.getAs[Id](sessionId + sessionIdSuffix)(current, idManifest)

  private[auth] def getSessionId[A](userId: Id)(implicit request: Request[A]): Option[String] =
    Cache.getAs[String](userId.toString + userIdSuffix)

  private[auth] def deleteUserId(sessionId: String) {
    Cache.set(sessionId + sessionIdSuffix, null, 1)
  }

  private[auth] def deleteSessionId(userId: Id) {
    Cache.set(userId.toString + userIdSuffix, null, 1)
  }

  private[auth] def storeId(sessionId: String, userId: Id) {
    Cache.set(sessionId + sessionIdSuffix, userId, sessionTimeoutInSeconds)
    Cache.set(userId.toString + userIdSuffix, sessionId, sessionTimeoutInSeconds)
  }
  
}
