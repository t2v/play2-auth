package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.{AuthConfigImpl, Messages}
import jp.t2v.lab.play2.auth.test.Helpers._

class ApplicationSpec extends Specification {

  object config extends AuthConfigImpl

  "Messages" should {
    "return list when user is authorized" in new WithApplication {
      val res = Messages.list(FakeRequest().withLoggedIn(config)(1))
      contentType(res) must equalTo("text/html")
    }
  }

}
