package jp.t2v.lab.play2.auth.test

import jp.t2v.lab.play2.auth.AuthConfig
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) {

    def withLoggedIn(implicit config: AuthConfig): config.Id => FakeRequest[A] = { id =>
      val token = Await.result(config.idContainer.startNewSession(id, config.sessionTimeoutInSeconds)(fakeRequest, global), 10.seconds)
      fakeRequest.withHeaders("PLAY2_AUTH_TEST_TOKEN" -> token)
    }

  }

}
object Helpers extends Helpers
