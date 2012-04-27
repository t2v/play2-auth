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
    Cache.getAs[String](userId + ":userId").foreach { old =>
      Cache.set(old + ":sessionId", "", 1)
    }
    val sessionId = generateSessionId()
    Cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    Cache.set(userId + ":userId", sessionId, sessionTimeoutInSeconds)
    loginSucceeded(request).withSession("sessionId" -> sessionId)
  }

  @tailrec
  private def generateSessionId(): String = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890-_.!~*'()"
    val token = Stream.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (Cache.getAs[String](token + ":sessionId").isEmpty) token else generateSessionId()
  }

  private val random = new Random(new SecureRandom())

  def gotoLogoutSucceeded[A](implicit request: Request[A]): PlainResult = {
    for {
      sessionId <- request.session.get("sessionId")
      userId <- Cache.getAs[Id](sessionId + ":sessionId")(current, idManifest)
    } {
      Cache.set(sessionId + ":sessionId", "", 1)
      Cache.set(userId + ":userId", "", 1)
    }
    logoutSucceeded(request).withNewSession
  }

}
