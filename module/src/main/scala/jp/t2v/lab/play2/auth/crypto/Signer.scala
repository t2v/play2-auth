package jp.t2v.lab.play2.auth.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.typesafe.config.ConfigFactory
import jp.t2v.lab.play2.auth._
import org.apache.commons.codec.binary.Hex

trait Signer {
  val secret: String

  private lazy val mac: Mac = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret.getBytes, "HmacSHA1"))
    mac
  }

  def sign(token: AuthenticityToken): AuthenticityTokenSignature = Hex.encodeHexString(mac.doFinal(token.getBytes("utf-8")))

  def verify(token: AuthenticityToken, signature: AuthenticityTokenSignature): Boolean = safeEquals(sign(token), signature)

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

object Signer extends Signer {
  override val secret: String = ConfigFactory.load().getString("play.crypto.secret")
}
