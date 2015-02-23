package jp.t2v.lab.play2.auth

import play.api.mvc.{DiscardingCookie, Cookie, Result, RequestHeader}

class CookieTokenAccessor(
    cookieName: => String = "PLAY2AUTH_SESS_ID",
    cookieSecureOption: => Boolean = false,
    cookieHttpOnlyOption: => Boolean = true,
    cookieDomainOption: => Option[String] = None,
    cookiePathOption: => String = "/",
    cookieMaxAge: => Option[Int] = None
) extends TokenAccessor {

  def put(token: String)(result: Result): Result = {
    val c = Cookie(cookieName, sign(token), cookieMaxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
    result.withCookies(c)
  }

  def extract(request: RequestHeader): Option[String] = {
    request.cookies.get(cookieName).flatMap(c => verifyHmac(c.value))
  }

  def delete(result: Result): Result = {
    result.discardingCookies(DiscardingCookie(cookieName))
  }
}
