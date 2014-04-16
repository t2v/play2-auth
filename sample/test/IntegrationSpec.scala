package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import java.io.File

class IntegrationSpec extends Specification {
  
  "Application" should {
    
    "work from within a browser" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = FakeApplication(additionalConfiguration = inMemoryDatabase(name = "default", options = Map("DB_CLOSE_DELAY" -> "-1")))) {

      val baseURL = s"http://localhost:${port}"
      // login failed
      browser.goTo(baseURL)
      browser.$("#email").text("alice@example.com")
      browser.$("#password").text("secretxxx")
      browser.$("#loginbutton").click()
      browser.pageSource must contain("Invalid email or password")

      // login succeded
      browser.$("#email").text("alice@example.com")
      browser.$("#password").text("secret")
      browser.$("#loginbutton").click()
      browser.$("dl.error").size must equalTo(0)
      browser.pageSource must not contain("Sign in")
      browser.pageSource must contain("logout")

      // logout
      browser.$("a").click()
      browser.pageSource must contain("Sign in")

      browser.goTo(s"$baseURL/message/write")
      browser.pageSource must contain("Sign in")

    }

    "authorize" in new WithBrowser(webDriver = WebDriverFactory(HTMLUNIT), app = FakeApplication(additionalConfiguration = inMemoryDatabase(name = "default", options = Map("DB_CLOSE_DELAY" -> "-1")))) {

      val baseURL = s"http://localhost:${port}"

      // login succeded
      browser.goTo(baseURL)
      browser.$("#email").text("bob@example.com")
      browser.$("#password").text("secret")
      browser.$("#loginbutton").click()
      browser.$("dl.error").size must equalTo(0)
      browser.pageSource must not contain("Sign in")
      browser.pageSource must contain("logout")

      browser.goTo(s"${baseURL}/message/write")
      browser.pageSource must contain("no permission")

      browser.goTo(s"${baseURL}/logout")
      browser.$("#email").text("alice@example.com")
      browser.$("#password").text("secret")
      browser.$("#loginbutton").click()
      browser.$("dl.error").size must equalTo(0)
      browser.goTo(s"${baseURL}/message/write")
      browser.pageSource must not contain("no permission")

    }

  }
  
}

