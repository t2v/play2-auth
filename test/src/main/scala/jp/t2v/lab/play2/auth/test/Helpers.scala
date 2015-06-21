package jp.t2v.lab.play2.auth.test

import play.api.test._
import play.api.mvc.Cookie
import jp.t2v.lab.play2.auth.{AuthConfig, CookieTokenAccessor}
import play.api.libs.Crypto
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) {

    def withLoggedIn(implicit config: AuthConfig): config.Id => FakeRequest[A] = { id =>
      config.tokenAccessor match {
        case cta: CookieTokenAccessor =>
          val token = Await.result(config.idContainer.startNewSession(id, config.sessionTimeoutInSeconds)(fakeRequest, global), 10.seconds)
          val value = Crypto.sign(token) + token
          fakeRequest.withCookies(Cookie(cta.cookieName, value, None, cta.cookiePathOption, cta.cookieDomainOption, cta.cookieSecureOption, cta.cookieHttpOnlyOption))

        case _ => throw new UnsupportedOperationException("withLoggedIn is currently " +
          "only supported when using the CookieTokenAccessor")
      }
    }

  }

}
object Helpers extends Helpers
