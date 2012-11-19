package jp.t2v.lab.play20.auth

import play.api.mvc.{Request, PlainResult, Controller}
import scala.annotation.tailrec
import scala.util.Random
import java.security.SecureRandom

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded[A](userId: Id)(implicit request: Request[A]): PlainResult = {
    resolver.removeByUserId(userId)
    val sessionId = generateSessionId(request)
    val session = resolver.store(sessionId, userId, sessionTimeoutInSeconds)
    loginSucceeded(request).withSession(session + ("sessionId" -> sessionId))
  }

  def gotoLogoutSucceeded[A](implicit request: Request[A]): PlainResult = {
    request.session.get("sessionId") foreach resolver.removeBySessionId
    logoutSucceeded(request).withNewSession
  }

  @tailrec
  private def generateSessionId[A](implicit request: Request[A]): String = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.!~*'()"
    val token = Stream.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (resolver.exists(token)) generateSessionId(request) else token
  }

  private val random = new Random(new SecureRandom())

}
