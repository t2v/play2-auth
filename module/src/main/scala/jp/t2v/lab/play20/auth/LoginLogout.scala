package jp.t2v.lab.play20.auth

import play.api.Play._
import play.api.cache.Cache
import play.api.mvc.{Request, PlainResult, Controller}
import scala.util.Random
import java.security.SecureRandom

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: ID): PlainResult = {
    Cache.getAs[String](userId + ":userId").foreach { old =>
      Cache.set(old + ":sessionId", "", 1)
    }
    val sessionId = generateSessionId()
    Cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    Cache.set(userId + ":userId", sessionId, sessionTimeoutInSeconds)
    loginSucceeded.withSession("sessionId" -> sessionId)
  }

  private def generateSessionId(): String = {
    def isAlphaNum(c: Char) = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
    Stream.continually(random.nextPrintableChar).filter(isAlphaNum).take(64).mkString
  }

  private val random = new Random(new SecureRandom())

  def gotoLogoutSucceeded(request: Request[Any]): PlainResult = {
    for {
      sessionId <- request.session.get("sessionId")
      userId <- Cache.getAs[ID](sessionId + ":sessionId")(current, idManifest)
    } {
      Cache.set(sessionId + ":sessionId", "", 1)
      Cache.set(userId + ":userId", "", 1)
    }
    logoutSucceeded.withNewSession
  }

}
