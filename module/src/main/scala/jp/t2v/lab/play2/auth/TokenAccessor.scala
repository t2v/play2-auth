package jp.t2v.lab.play2.auth

import jp.t2v.lab.play2.auth.crypto.Signer
import play.api.mvc.{RequestHeader, Result}

trait TokenAccessor {

  def extract(request: RequestHeader): Option[AuthenticityToken]

  def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result

  def delete(result: Result)(implicit request: RequestHeader): Result

  protected def sign(token: AuthenticityToken): SignedToken = Signer.sign(token) + token

  protected def verifySignedToken(token: SignedToken): Option[AuthenticityToken] = {
    val (signature, value) = token.splitAt(40)
    if (Signer.verify(value, signature)) Some(value) else None
  }

}
