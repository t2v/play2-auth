package jp.t2v.lab.play2.auth

import play.api.mvc.{DiscardingCookie, Cookie, Result, RequestHeader}

class CookieTokenAccessor(
    val cookieName: String = "PLAY2AUTH_SESS_ID",
    val cookieSecureOption: Boolean = false,
    val cookieHttpOnlyOption: Boolean = true,
    val cookieDomainOption: Option[String] = None,
    val cookiePathOption: String = "/",
    val cookieMaxAge: Option[Int] = None
) extends TokenAccessor {

  def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result = {
    val c = Cookie(cookieName, sign(token), cookieMaxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
    result.withCookies(c)
  }

  def extract(request: RequestHeader): Option[AuthenticityToken] = {
    request.cookies.get(cookieName).flatMap(c => verifyHmac(c.value))
  }

  def delete(result: Result)(implicit request: RequestHeader): Result = {
    result.discardingCookies(DiscardingCookie(cookieName))
  }
}
