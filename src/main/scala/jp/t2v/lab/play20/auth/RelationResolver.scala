package jp.t2v.lab.play20.auth

import play.api.mvc._

trait RelationResolver[Id] {

  def exists(sessionId: String): Boolean
  def sessionId2userId(sessionId: String): Option[Id]
  def userId2sessionId(userId: Id): Option[String]

  def removeBySessionId(sessionId: String): Unit
  def removeByUserId(userId: Id): Unit

  def store(sessionId: String, userId: Id, timeoutInSeconds: Int): Session
  def prolongTimeout(sessionId: String, timeoutInSeconds: Int): Unit

}
