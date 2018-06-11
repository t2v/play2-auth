package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import controllers.standard.{ AuthConfigImpl, Messages }
import jp.t2v.lab.play2.auth.test.Helpers._
import java.io.File
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode

class ApplicationSpec extends Specification with play.api.test.StubControllerComponentsFactory {

  lazy val fakeApp = new GuiceApplicationBuilder()
            .configure(Helpers.inMemoryDatabase(name = "default", options = Map("DB_CLOSE_DELAY" -> "-1")))
            .in(Mode.Test)
            .build()

  object config extends AuthConfigImpl {
    val environment = fakeApp.environment 
  }

  def withApp = new play.api.test.WithApplication(fakeApp) {
    val con = new Messages(stubControllerComponents(), fakeApp.environment)
    val res = con.list(FakeRequest().withLoggedIn(config)(1))
    contentType(res) must beSome("text/html")
    
    
  }
            
  "Messages" should {
    "return list when user is authorized" in withApp 
  }

}
