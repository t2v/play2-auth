package jp.t2v.lab.play2.auth

import java.util.Base64

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
    val c = Cookie(cookieName, new String(Base64.getEncoder.encode(sign(token).getBytes)), cookieMaxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
    result.withCookies(c)
  }

  def extract(request: RequestHeader): Option[AuthenticityToken] = {
    request.cookies.get(cookieName).flatMap(c => verifyHmac(new String(Base64.getDecoder.decode(c.value.getBytes))))
  }

  def delete(result: Result)(implicit request: RequestHeader): Result = {
    result.discardingCookies(DiscardingCookie(cookieName))
  }
}
