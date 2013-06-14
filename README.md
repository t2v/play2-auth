Play2.x module for Authentication and Authorization [![Build Status](https://secure.travis-ci.org/t2v/play20-auth.png)](http://travis-ci.org/t2v/play20-auth)
===========================================================

This module offers Authentication and Authorization features to Play2.x applications

Target
----------------------------------------

This module targets the __Scala__ version of __Play2.x__.

For the Java version of Play2.x, there is an authorization module called [Deadbolt 2](https://github.com/schaloner/deadbolt-2).

Play2.1.0

Motivation
---------------------------------------

### Play2.x's Existing Security trait

The existing `Security` trait in Play2.x API does not define an identifier that identifies a user.

If you use an Email or a userID as an identier, 
users can not invalidate their session if the session cookie is intercepted.

This module creates a unique SessionID using a secure random number generator.
Even if the sessionId cookie is intercepted, users can invalidate the session by logging in again. 
Your application can expire sessions after a set time limit.


### Flexiblity

Since the `Security` trait in Play2.x API returns `Action`, 
complicated action methods wind up deeply nested.

Play2x-Auth provides an interface that returns an [`Either[PlainResult, User]`](http://www.scala-lang.org/api/current/scala/Either.html)
making writing complicated action methods easier.   [`Either`](http://www.scala-lang.org/api/current/scala/Either.html) is a wrapper similar to `Option`


Previous Version
---------------------------------------

for Play2.0.x, Please see [previous version README](https://github.com/t2v/play20-auth/tree/release0.7)


Attention
---------------------------------------

<strong style="font-size: 200%; color: red;">The artifact ID and package name was changed at version 0.8</strong>

<strong style="font-size: 200%;">you should be careful to version up from 0.7</strong>


Installation
---------------------------------------

Add a dependency declaration into your `Build.scala` or `build.sbt` file:

* __for Play2.1.0__

        "jp.t2v" %% "play2.auth"      % "0.10-SNAPSHOT",
        "jp.t2v" %% "play2.auth.test" % "0.10-SNAPSHOT" % "test"

For example your `Build.scala` might look like this:

```scala
  val appDependencies = Seq(
    "jp.t2v" %% "play2.auth"      % "0.10-SNAPSHOT",
    "jp.t2v" %% "play2.auth.test" % "0.10-SNAPSHOT" % "test"
  )

  val main = play.Project(appName, appVersion, appDependencies){
  resolvers += "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots"
}
```

You don't need to create a `play.plugins` file.

Usage
---------------------------------------

1. First create a trait that extends `jp.t2v.lab.play2.auth.AuthConfig` in `app/controllers`.

    ```scala
    // Example
    trait AuthConfigImpl extends AuthConfig {

      /**
       * A type that is used to identify a user.
       * `String`, `Int`, `Long` and so on.
       */
      type Id = String

      /**
       * A type that represents a user in your application.
       * `User`, `Account` and so on.
       */
      type User = Account

      /**
       * A type that is defined by every action for authorization.
       * This sample uses the following trait:
       *
       * sealed trait Permission
       * case object Administrator extends Permission
       * case object NormalUser extends Permission
       */
      type Authority = Permission

      /**
       * A `ClassManifest` is used to retrieve an id from the Cache API.
       * Use something like this:
       */
      val idTag: ClassTag[Id] = classTag[Id]

      /**
       * The session timeout in seconds
       */
      val sessionTimeoutInSeconds: Int = 3600

      /**
       * A function that returns a `User` object from an `Id`.
       * You can alter the procedure to suit your application.
       */
      def resolveUser(id: Id): Option[User] = Account.findById(id)

      /**
       * Where to redirect the user after a successful login.
       */
      def loginSucceeded(request: RequestHeader): Result = Redirect(routes.Message.main)

      /**
       * Where to redirect the user after logging out
       */
      def logoutSucceeded(request: RequestHeader): Result = Redirect(routes.Application.login)

      /**
       * If the user is not logged in and tries to access a protected resource then redirct them as follows:
       */
      def authenticationFailed(request: RequestHeader): Result = Redirect(routes.Application.login)

      /**
       * If authorization failed (usually incorrect password) redirect the user as follows:
       */
      def authorizationFailed(request: RequestHeader): Result = Forbidden("no permission")

      /**
       * A function that determines what `Authority` a user has.
       * You should alter this procedure to suit your application.
       */
      def authorize(user: User, authority: Authority): Boolean =
        (user.permission, authority) match {
          case (Administrator, _) => true
          case (NormalUser, NormalUser) => true
          case _ => false
        }

      /**
       * Whether use the secure option or not use it in the cookie.
       * However default is false, I strongly recommend using true in a production.
       */
      override lazy val cookieSecureOption: Boolean = play.api.Play.current.configuration.getBoolean("auth.cookie.secure").getOrElse(true)

    }
    ```

1. Next create a `Controller` that defines both login and logout actions.
   This `Controller` mixes in the `jp.t2v.lab.play2.auth.LoginLogout` trait and
   the trait that you created in first step.

    ```scala
    object Application extends Controller with LoginLogout with AuthConfigImpl {

      /** Your application's login form.  Alter it to fit your application */
      val loginForm = Form {
        mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
          .verifying("Invalid email or password", result => result.isDefined)
      }

      /** Alter the login page action to suit your application. */
      def login = Action { implicit request =>
        Ok(html.login(loginForm))
      }

      /**
       * Return the `gotoLogoutSucceeded` method's result in the logout action.
       *
       * Since the `gotoLogoutSucceeded` returns `Result`,
       * If you import `jp.t2v.lab.play2.auth._`, you can add a procedure like the following.
       *
       *   gotoLogoutSucceeded.flashing(
       *     "success" -> "You've been logged out"
       *   )
       */
      def logout = Action { implicit request =>
        // do something...
        gotoLogoutSucceeded
      }

      /**
       * Return the `gotoLoginSucceeded` method's result in the login action.
       *
       * Since the `gotoLoginSucceeded` returns `Result`,
       * If you import `jp.t2v.lab.play2.auth._`, you can add a procedure like the `gotoLogoutSucceeded`.
       */
      def authenticate = Action { implicit request =>
        loginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(html.login(formWithErrors)),
          user => gotoLoginSucceeded(user.get.id)
        )
      }

    }
    ```

1. Lastly, mix `jp.t2v.lab.play2.auth.AuthElement` trait and the trait that was created in the first step
   into your Controllers:

    ```scala
    object Message extends Controller with AuthElement with AuthConfigImpl {

      // The `StackAction` method
      //    takes `(AuthorityKey, Authority)` as the first argument and
      //    a function signature `RequestWithAttributes[AnyContent] => Result` as the second argument and
      //    returns an `Action`

      // thw `loggedIn` method
      //     returns current logged in user

      def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "message main"
        Ok(html.message.main(title))
      }

      def list = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "all messages"
        Ok(html.message.list(title))
      }

      def detail(id: Int) = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "messages detail "
        Ok(html.message.detail(title + id))
      }

      // Only Administrator can execute this action.
      def write = StackAction(AuthorityKey -> Administrator) { implicit request =>
        val user = loggedIn
        val title = "write message"
        Ok(html.message.write(title))
      }

    }
    ```


Test
---------------------------------------

play2.auth provides test module at version 0.8

You can use `FakeRequest` with logged-in status.

```scala
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
```

1. Import `jp.t2v.lab.play2.auth.test.Helpers._`
1. Define instance what is mixed-in `AuthConfigImpl`

        object config extends AuthConfigImpl

1. Call `withLoggedIn` method on `FakeRequest`
    * first argument: `AuthConfigImpl` instance.
    * second argument: user ID of the user who is logged-in at this request


It makes enable to test controllers with play2.auth


Advanced usage
---------------------------------------

### Changing the authorization depending on the request parameters.

For example, a Social networking application has a function to edit messages.

A user must be able to edit their own messages but not other people's messages.

To achieve this you could define `Authority` as a `Function`:

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other setup is omitted. 

  type Authority = User => Boolean

  def authorize(user: User, authority: Authority): Boolean = authority(user)

}
```

```scala
object Application extends Controller with AuthElement with AuthConfigImpl {

  private def sameAuthor(messageId: Int)(account: Account): Boolean =
    Message.getAuther(messageId) == account

  def edit(messageId: Int) = StackAction(AuthorityKey -> sameAuthor(messageId)) { implicit request =>
    val user = loggedIn
    val target = Message.findById(messageId)
    Ok(html.message.edit(messageForm.fill(target)))
  }

}
```


### Returning to the originally requested page after login

When an unauthenticated user requests access to page requiring authentication, 
you first redirect the user to the login page, then, after the user successfully logs in, you redirect the user to the page they originally requested.

To achieve this change `authenticationFailed` and `loginSucceeded`:

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other settings are omitted.

  def authenticationFailed(request: RequestHeader): Result =
    Redirect(routes.Application.login).withSession("access_uri" -> request.uri)

  def loginSucceeded(request: RequestHeader): Result = {
    val uri = request.session.get("access_uri").getOrElse(routes.Message.main.url.toString)
    Redirect(uri).withSession(request.session - "access_uri")
  }

}
```


### Changing the display depending on whether the user is logged in 

If you want to display the application's index differently to non-logged-in users
and logged-in users, you can use `OptionalAuthElement` insted of `AuthElement`:

```scala
object Application extends Controller with OptionalAuthElement with AuthConfigImpl {

  // maybeUser is an instance of `Option[User]`.
  // `OptionalAuthElement` dont need `AuthorityKey`
  def index = StackAction { implicit request =>
    val maybeUser: Option[User] = loggedIn
    val user: User = maybeUser.getOrElse(GuestUser)
    Ok(html.index(user))
  }

}
```


### For action that doesn't require authorization

you can `AuthenticationElement` insted of `AuthElement` for authentication without authorization.

```scala
object Application extends Controller with AuthenticationElement with AuthConfigImpl {

  def index = StackAction { implicit request =>
    val user: User = loggedIn
    Ok(html.index(user))
  }

}
```


### return 401 when a request is sent by Ajax

Normally, you want to return a login page redirection at a authentication failed.
Although, when the request is sent by Ajax you want to return 301.

You can do it as follows.

```scala
def authenticationFailed(request: RequestHeader) = {
  request.headers.get("X-Requested-With") match {
    case Some("XMLHttpRequest") => Unauthorized("Authentication failed")
    case _ => Redirect(routes.Application.login)
  }
}
```


### Action composition

play2.auth use [stackable-controller](https://github.com/t2v/stackable-controller)

Suppose you want to validate a token at every action in order to defeat a [Cross Site Request Forgery](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF) attack.

Since it is impractical to perform the validation in all actions, you would define a trait like this:

```scala
trait TokenValidateElement extends StackableController {
    self: Controller =>

  private val tokenForm = Form("token" -> text)

  private def validateToken(request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  override proceed[A](reqest: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    if (validateToken(request)) super.proceed(request)(f)
    else BadRequest
  }

}
```

You can use `TokenValidateElement` trait with `AuthElement` trait.

```scala
object Application extends Controller with TokenValidateElement with AuthElement with AuthConfigImpl {

  def page1 = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    // do something
    Ok(html.page2("result"))
  }

}
```


### Stateless vs Stateful implementation.

Play2x-Auth follows the Play framework's stateless policy.
However, Play2x-Auth's default implementation is stateful, 
because the stateless implementation has the following security risk:

If user logs-in to your application in a internet-cafe, then returns home neglecting to logout.
If the user logs in again at home they will *not* invalidate the session.

Nevertheless, you want to use a fully stateless implementation then just override the `idContainer` method of `AuthConfig` like this:

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other settings omitted.

  override lazy val idContainer: IdContainer[Id] = new CookieIdContainer[Id]

}
```

You could also store the session data in a Relational Database by overriding the id container.

Note: `CookieIdContainer` doesn't support session timeout.


Running The Sample Application
---------------------------------------

1. `git clone https://github.com/t2v/play20-auth.git`
1. `cd play20-auth`
1. `play "project sample" play run`
1. access to `http://localhost:9000/` on your browser.
    1. click `Apply this script now!`
    1. login
    
        defined accounts
        
            Email             | Password | Permission
            alice@example.com | secret   | Administrator
            bob@example.com   | secret   | NormalUser
            chris@example.com | secret   | NormalUser


Attention -- Distributed Servers
---------------------------------------

[Ehcache](http://ehcache.org), the default cache implementation used by Play2.x, does not work on distributed application servers.

If you have distributed servers, use the [Memcached Plugin](https://github.com/mumoshu/play2-memcached) or something similar.


License
---------------------------------------

This library is released under the Apache Software License, version 2, 
which should be included with the source in a file named `LICENSE`.

