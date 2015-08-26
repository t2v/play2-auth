package jp.t2v.lab.play2.auth

import play.api.libs.Crypto
import play.api.mvc.Cookie
import akka.japi.Option.Some

trait CookieUtil {

  def verifyHmac(cookie: Cookie): Option[String] = {
    val (hmac, value) = cookie.value.splitAt(40)
    if (safeEquals(Crypto.sign(value), hmac)) Some(value) else None
  }

  // Do not change this unless you understand the security issues behind timing attacks.
  // This method intentionally runs in constant time if the two strings have the same length.
  // If it didn't, it would be vulnerable to a timing attack.
  protected def safeEquals(a: String, b: String) = {
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

object CookieUtil extends CookieUtil