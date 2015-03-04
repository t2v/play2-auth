package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, RequestHeader}
import play.api.libs.Crypto

trait TokenAccessor {

  def extract(request: RequestHeader): Option[AuthenticityToken]

  def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result

  def delete(result: Result)(implicit request: RequestHeader): Result

  protected def verifyHmac(token: SignedToken): Option[AuthenticityToken] = {
    val (hmac, value) = token.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }

  protected def sign(token: AuthenticityToken): SignedToken = Crypto.sign(token) + token

}
