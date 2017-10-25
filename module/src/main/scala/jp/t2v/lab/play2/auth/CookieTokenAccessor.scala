package jp.t2v.lab.play2.auth

import play.api.mvc.{DiscardingCookie, Cookie, Result, RequestHeader}

class CookieTokenAccessor(
  protected val cookieName: String = "PLAY2AUTH_SESS_ID",
  protected val cookieSecureOption: Boolean = false,
  protected val cookieHttpOnlyOption: Boolean = true,
  protected val cookieDomainOption: Option[String] = None,
  protected val cookiePathOption: String = "/",
  protected val cookieMaxAge: Option[Int] = None,
  protected val algorithmName: String = "HmacSHA256",
  protected val secretKey: String
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

  protected def verifyHmac(token: SignedToken): Option[AuthenticityToken] = {
//    val (hmac, value) = token.splitAt(40)
//    if (safeEquals(Crypto.sign(value), hmac)) Some(value) else None
    ???
  }

  protected def sign(token: AuthenticityToken): SignedToken = ??? //Crypto.sign(token) + token

  // Do not change this unless you understand the security issues behind timing attacks.
  // This method intentionally runs in constant time if the two strings have the same length.
  // If it didn't, it would be vulnerable to a timing attack.
  protected def safeEquals(a: String, b: String): Boolean = {
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- Array.range(0, a.length)) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }

}
