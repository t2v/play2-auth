package jp.t2v.lab.play2.auth.test

import play.api.test._
import play.api.mvc.Cookie
import jp.t2v.lab.play2.auth.AuthConfig
import play.api.libs.Crypto
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import jp.t2v.lab.play2.auth.CookieTokenAccessor
import jp.t2v.lab.play2.auth.AuthCookieSigner

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) {

    def withLoggedIn(implicit config: AuthConfig): config.Id => FakeRequest[A] = { id =>
      def sign(s: String) = AuthCookieSigner.cookieSigner.sign(s) + s
      val token = Await.result(config.idContainer.startNewSession(id, config.sessionTimeoutInSeconds)(fakeRequest, global), 10.seconds)
      val c = Cookie("PLAY2AUTH_SESS_ID", sign(token))
      fakeRequest.withCookies(c)
      //fakeRequest.withHeaders("PLAY2_AUTH_TEST_TOKEN" -> token)
    }

    
    
  }

}
object Helpers extends Helpers
