package jp.t2v.lab.play2.auth.social.core

class AccessTokenRetrievalFailedException(message: String, exception: Throwable)
    extends RuntimeException(message, exception) {

  def this(message: String) = this(message, null)

}
