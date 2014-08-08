package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto
import scala.concurrent.{Future, ExecutionContext}

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLoginSucceeded(userId, loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = for {
    token <- idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    value = Crypto.sign(token) + token
    r     <- result
  } yield {
    r.withCookies(Cookie(cookieName, value, None, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLogoutSucceeded(logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    request.cookies.get(cookieName) flatMap CookieUtil.verifyHmac foreach idContainer.remove
    result.map(_.discardingCookies(DiscardingCookie(cookieName)))
  }
}
