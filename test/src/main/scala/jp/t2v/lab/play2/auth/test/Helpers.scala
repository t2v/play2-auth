package jp.t2v.lab.play2.auth.test

import play.api.test._
import jp.t2v.lab.play2.auth.AsyncAuth

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait Helpers {

  implicit class AuthFakeRequest[A](fakeRequest: FakeRequest[A]) {

    def withLoggedIn[Id](implicit auth: AsyncAuth[Id, _, _]): Id => FakeRequest[A] = { id =>
      val token = Await.result(auth.idContainer.startNewSession(id, auth.authConfig.sessionTimeoutInSeconds)(fakeRequest, global), 10.seconds)
      fakeRequest.withHeaders("PLAY2_AUTH_TEST_TOKEN" -> token)
    }

  }

}
object Helpers extends Helpers
