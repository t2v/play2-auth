package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import controllers.standard.{ AuthConfigImpl, Messages }
import jp.t2v.lab.play2.auth.test.Helpers._
import java.io.File

import play.api.Environment

class ApplicationSpec extends Specification {

  object config extends AuthConfigImpl {
    override val environment: Environment = Environment.simple()
  }

  "Messages" should {
    "return list when user is authorized" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase(name = "default", options = Map("DB_CLOSE_DELAY" -> "-1")))) {
      val res = new Messages(Environment.simple()).list(FakeRequest().withLoggedIn(config)(1))
      contentType(res) must beSome("text/html")
    }
  }

}
