package jp.t2v.lab.play20.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader): Result = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    val value = Crypto.sign(token) + token
    loginSucceeded(request).withCookies(Cookie(cookieName, value, -1, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader): Result = {
    request.cookies.get(cookieName) flatMap CookieUtil.verifyHmac foreach idContainer.remove
    logoutSucceeded(request).discardingCookies(cookieName)
  }

}
