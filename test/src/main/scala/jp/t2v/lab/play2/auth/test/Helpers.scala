package jp.t2v.lab.play2.auth.test

import play.api.test._
import play.api.mvc.{Cookie, Request}
import jp.t2v.lab.play2.auth.AuthConfig
import play.api.libs.Crypto

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) {

    def withLoggedIn(implicit config: AuthConfig): config.Id => FakeRequest[A] = { id =>
      val token = config.idContainer.startNewSession(id, config.sessionTimeoutInSeconds)
      val value = Crypto.sign(token) + token
      import config._
      fakeRequest.withCookies(Cookie(cookieName, value, None, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
    }

  }

}
object Helpers extends Helpers
