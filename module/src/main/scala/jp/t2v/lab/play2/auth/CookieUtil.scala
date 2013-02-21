package jp.t2v.lab.play2.auth

import play.api.libs.Crypto
import play.api.mvc.Cookie
import akka.japi.Option.Some

trait CookieUtil {

  def verifyHmac(cookie: Cookie): Option[String] = {
    val (hmac, value) = cookie.value.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }

}

object CookieUtil extends CookieUtil