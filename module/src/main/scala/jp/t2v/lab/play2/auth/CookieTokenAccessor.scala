package jp.t2v.lab.play2.auth

import play.api.mvc.{DiscardingCookie, Cookie, Result, RequestHeader}

class CookieTokenAccessor(
    protected val cookieName: String = "PLAY2AUTH_SESS_ID",
    protected val cookieSecureOption: Boolean = false,
    protected val cookieHttpOnlyOption: Boolean = true,
    protected val cookieDomainOption: Option[String] = None,
    protected val cookiePathOption: String = "/",
    protected val cookieMaxAge: Option[Int] = None
) extends TokenAccessor {

  def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result = {
    val c = Cookie(cookieName, sign(token), cookieMaxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
    result.withCookies(c)
  }

  def extract(request: RequestHeader): Option[AuthenticityToken] = {
    request.cookies.get(cookieName).flatMap(c => verifyHmac(c.value))
  }

  def delete(result: Result)(implicit request: RequestHeader): Result = {

    // before...
    //result.discardingCookies(DiscardingCookie(cookieName))

    // Since the discardedMaxAge of the cookie is "-1.day.toSecond.toInt" and it gets 
    // caught in the check that it does not receive the minus of Max-age of HTMLUNIT,
    // operation avoidance. In the latest version of Playframework, DiscardedMaxAge is corrected to 0 ing
    result.withCookies(DiscardingCookie(cookieName).toCookie.copy(maxAge = Some(0)))
  }
}
