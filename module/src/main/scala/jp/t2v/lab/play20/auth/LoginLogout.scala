package jp.t2v.lab.play20.auth

import play.api.mvc._
import play.api.mvc.AsyncResult
import play.api.mvc.Cookie
import scala.annotation.tailrec

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader): Result = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    setCookie(loginSucceeded(request), token)
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader): Result = {
    request.cookies.get(cookieName) map (_.value) foreach idContainer.remove
    unsetCookie(logoutSucceeded(request))
  }

  protected[auth] final def setCookie(result: Result, token: AuthenticityToken): Result = {
    setCookie_(result, Cookie(cookieName, token, -1, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
  }

  protected[auth] final def unsetCookie(result: Result): Result = {
    setCookie_(result, Cookie(cookieName, "", 0))
  }

  protected[auth] final def setCookie_(result: Result, cookie: Cookie): Result = {
    def set(r: Result): Result = r match {
      case p: PlainResult => p.withCookies(cookie)
      case a: AsyncResult => AsyncResult(a.result.map(set))
    }
    set(result)
  }


}
