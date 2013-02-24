package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader): Result = {
    gotoLoginSucceeded(userId, loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: Id, result: => Result): Result = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    val value = Crypto.sign(token) + token
    result.withCookies(Cookie(cookieName, value, None, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader): Result = {
    gotoLogoutSucceeded(logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Result)(implicit request: RequestHeader): Result = {
    request.cookies.get(cookieName) flatMap CookieUtil.verifyHmac foreach idContainer.remove
    result.discardingCookies(DiscardingCookie(cookieName))
  }
}
