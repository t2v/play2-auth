package jp.t2v.lab.play2.auth

import play.api.{Application, Play}
import play.api.libs.crypto.CookieSigner

/**
  * Code from play.api.libs.Crypto. Which is private object now.
  */
object AuthCookieSigner {

  private val cookieSignerCache: (Application) => CookieSigner = Application.instanceCache[CookieSigner]

  def cookieSigner: CookieSigner = {
    Play.maybeApplication.fold {
      sys.error("The global cookie signer instance requires a running application!")
    }(cookieSignerCache)
  }

}
