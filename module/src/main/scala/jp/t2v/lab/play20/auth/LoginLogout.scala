package jp.t2v.lab.play20.auth

import play.api.Play._
import play.api.cache.Cache
import play.api.mvc.{Request, PlainResult, Controller}
import scala.annotation.tailrec
import scala.util.Random
import java.security.SecureRandom

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded[A](userId: Id)(implicit request: Request[A]): PlainResult = {
    getSessionId(userId).foreach(deleteUserId)
    val sessionId = generateSessionId()
    storeId(sessionId, userId)
    loginSucceeded(request).withSession("sessionId" -> sessionId)
  }

  def gotoLogoutSucceeded[A](implicit request: Request[A]): PlainResult = {
    for {
      sessionId <- request.session.get("sessionId")
      userId <- getUserId(sessionId)
    } {
      deleteUserId(sessionId)
      deleteSessionId(userId)
    }
    logoutSucceeded(request).withNewSession
  }

  @tailrec
  private def generateSessionId(): String = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890-_.!~*'()"
    val token = Stream.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (getUserId(token).isEmpty) token else generateSessionId()
  }

  private val random = new Random(new SecureRandom())

  val sessionIdSuffix = ":sessionId"
  val userIdSuffix = ":userId"

  private def getUserId(sessionId: String): Option[Id] =
    Cache.getAs[Id](sessionId + sessionIdSuffix)(current, idManifest)

  private def getSessionId[A](userId: Id)(implicit request: Request[A]): Option[String] =
    Cache.getAs[String](userId + userIdSuffix)

  private def deleteUserId(sessionId: String) {
    Cache.set(sessionId + sessionIdSuffix, null, 1)
  }

  private def deleteSessionId(userId: Id) {
    Cache.set(userId + userIdSuffix, null, 1)
  }

  private def storeId(sessionId: String, userId: Id) {
    Cache.set(sessionId + sessionIdSuffix, userId, sessionTimeoutInSeconds)
    Cache.set(userId + userIdSuffix, sessionId, sessionTimeoutInSeconds)
  }

}
