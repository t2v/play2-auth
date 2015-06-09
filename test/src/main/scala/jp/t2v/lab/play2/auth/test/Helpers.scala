package jp.t2v.lab.play2.auth.test

import play.api.test._
import play.api.mvc.Cookie
import jp.t2v.lab.play2.auth.{AuthConfig, CookieTokenAccessor}
import play.api.libs.Crypto
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) extends CookieTokenAccessor {

    def withLoggedIn(implicit config: AuthConfig): config.Id => FakeRequest[A] = { id =>
      val token = Await.result(config.idContainer.startNewSession(id, config.sessionTimeoutInSeconds)(fakeRequest, global), 10.seconds)
      val value = Crypto.sign(token) + token
      fakeRequest.withCookies(Cookie(cookieName, value, None, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption))
    }

  }

}
object Helpers extends Helpers
