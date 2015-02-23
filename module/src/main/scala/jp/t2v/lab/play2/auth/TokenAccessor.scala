package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, RequestHeader}
import play.api.libs.Crypto

trait TokenAccessor {

  def extract(request: RequestHeader): Option[String]

  def put(token: String)(result: Result): Result

  def delete(result: Result): Result

  protected def verifyHmac(token: String): Option[String] = {
    val (hmac, value) = token.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }

  protected def sign(token: String): String = Crypto.sign(token) + token

}
