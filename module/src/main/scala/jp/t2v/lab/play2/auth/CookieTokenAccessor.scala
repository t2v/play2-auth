package jp.t2v.lab.play2.auth

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import play.api.mvc.{Cookie, DiscardingCookie, RequestHeader, Result}

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

  final protected val hmacSecretKey: SecretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithmName)

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
    PartialFunction.condOpt(token.split("""\.""", 2)) {
      case Array(hmac, value) if safeEquals(calculateHmac(value), hmac) => value
    }
  }

  protected def sign(token: AuthenticityToken): SignedToken = s"${calculateHmac(token)}.$token"

  // Do not change this unless you understand the security issues behind timing attacks.
  // This method intentionally runs in constant time if the two strings have the same length.
  // If it didn't, it would be vulnerable to a timing attack.
  final protected def safeEquals(a: String, b: String): Boolean = {
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

  protected def calculateHmac(value: String): String = {
    val mac = Mac.getInstance(hmacSecretKey.getAlgorithm)
    mac.init(hmacSecretKey)
    val hmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8))
    Base64.getUrlEncoder.encodeToString(hmac)
  }

}
