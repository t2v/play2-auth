package jp.t2v.lab.play20.auth

import play.api.cache.Cache
import play.api.Play._
import play.api.mvc.Session

class CacheRelationResolver[Id: ClassManifest] extends RelationResolver[Id] {

  private[auth] val sessionIdSuffix = ":sessionId"
  private[auth] val userIdSuffix = ":userId"

  def exists(sessionId: String) = sessionId2userId(sessionId).isDefined

  def sessionId2userId(sessionId: String): Option[Id] =
    Cache.get(sessionId + sessionIdSuffix).map(_.asInstanceOf[Id])

  def userId2sessionId(userId: Id): Option[String] =
    Cache.getAs[String](userId.toString + userIdSuffix)

  def removeBySessionId(sessionId: String) {
    sessionId2userId(sessionId) foreach unsetUserId
    unsetSessionId(sessionId)
  }
  def removeByUserId(userId: Id) {
    userId2sessionId(userId) foreach unsetSessionId
    unsetUserId(userId)
  }
  private[auth] def unsetSessionId(sessionId: String) {
    Cache.set(sessionId + sessionIdSuffix, null, 1)
  }
  private[auth] def unsetUserId(userId: Id) {
    Cache.set(userId.toString + userIdSuffix, null, 1)
  }

  def store(sessionId: String, userId: Id, timeoutInSeconds: Int) = {
    Cache.set(sessionId + sessionIdSuffix, userId, timeoutInSeconds)
    Cache.set(userId.toString + userIdSuffix, sessionId, timeoutInSeconds)
    Session()
  }

  def prolongTimeout(sessionId: String, timeoutInSeconds: Int) {
    sessionId2userId(sessionId).foreach(store(sessionId, _, timeoutInSeconds))
  }

}
