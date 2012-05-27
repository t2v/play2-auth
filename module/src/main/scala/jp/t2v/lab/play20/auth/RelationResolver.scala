package jp.t2v.lab.play20.auth

trait RelationResolver[Id] {

  def sessionId2userId(sessionId: String): Option[Id]
  def userId2sessionId(userId: Id): Option[String]

  def removeBySessionId(sessionId: String): Unit
  def removeByUserId(userId: Id): Unit

  def store(sessionId: String, userId: Id, timeoutInSeconds: Int): Unit
  def prolongTimeout(sessionId: String, timeoutInSeconds: Int): Unit

}
