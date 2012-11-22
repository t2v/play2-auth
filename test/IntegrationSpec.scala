package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class IntegrationSpec extends Specification {
  
  "Application" should {
    
    "work from within a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>

        // login failed
        browser.goTo("http://localhost:3333/")
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

        browser.goTo("http://localhost:3333/message/write")
        browser.pageSource must contain("Sign in")

      }
    }

    "authorize" in {
      running(TestServer(3333), HTMLUNIT) { browser =>

        // login succeded
        browser.goTo("http://localhost:3333/")
        browser.$("#email").text("bob@example.com")
        browser.$("#password").text("secret")
        browser.$("#loginbutton").click()
        browser.$("dl.error").size must equalTo(0)
        browser.pageSource must not contain("Sign in")
        browser.pageSource must contain("logout")

        browser.goTo("http://localhost:3333/message/write")
        browser.pageSource must contain("no permission")

        browser.goTo("http://localhost:3333/logout")
        browser.$("#email").text("alice@example.com")
        browser.$("#password").text("secret")
        browser.$("#loginbutton").click()
        browser.$("dl.error").size must equalTo(0)
        browser.goTo("http://localhost:3333/message/write")
        browser.pageSource must not contain("no permission")
      }
    }

  }
  
}

