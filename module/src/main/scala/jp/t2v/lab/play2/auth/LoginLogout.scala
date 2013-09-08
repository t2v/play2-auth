package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto
import scala.concurrent.{Future, ExecutionContext}

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    gotoLoginSucceeded(userId, loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: Id, result: => Future[SimpleResult])(implicit ctx: ExecutionContext): Future[SimpleResult] = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    val value = Crypto.sign(token) + token
    result.map(_.withCookies(Cookie(cookieName, value, None, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)))
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    gotoLogoutSucceeded(logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Future[SimpleResult])(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    request.cookies.get(cookieName) flatMap CookieUtil.verifyHmac foreach idContainer.remove
    result.map(_.discardingCookies(DiscardingCookie(cookieName)))
  }
}
