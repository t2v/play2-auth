Play2.0 module for Authentication and Authorization
===========================================================

This module offers Authentication and Authorization features to Play2.0 applications

Target
---------------------------------------

This module targets the __Scala__ version of __Play2.0__.

For the Java version of Play2.0, there is an authorization module called [Deadbolt 2](https://github.com/schaloner/deadbolt-2).

This module has been tested on Play2.0final and Play2.0.1.

Motivation
---------------------------------------

### Play2.0's Existing Security trait

The existing `Security` trait in Play2.0 API does not define an identifier that identifies a user.

If you use an Email or a userID as an identier, 
users can not invalidate their session if the session cookie is intercepted.

This module creates a unique SessionID using a secure random number generator.
Even if the sessionId cookie is intercepted, users can invalidate the session by logging in again. 
Your application can expire sessions after a set time limit.


### Flexiblity

Since the `Security` trait in Play2.0 API returns `Action`, 
complicated action methods wind up deeply nested.

Play2.0-auth provides an interface that returns an [`Either[PlainResult, User]`](http://www.scala-lang.org/api/current/scala/Either.html)
Writing complicated action methods is easy.   `Either` is a wrapper similar to `Option`


Installation
---------------------------------------

1. Add a repository resolver into your `Build.scala` or `build.sbt` file:

        resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"

1. Add a dependency declaration into your `Build.scala` or `build.sbt` file:
    1. For the stable release:

            "jp.t2v" %% "play20.auth" % "0.2"

    1. Current snapshot version:

            "jp.t2v" %% "play20.auth" % "0.3-SNAPSHOT"

For example your `Build.scala` might look like this:

```scala
  val appDependencies = Seq(
    "jp.t2v" %% "play20.auth" % "0.1"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  )
```

You don't need to create a `play.plugins` file.


Usage
---------------------------------------

1. First create a trait that extends `jp.t2v.lab.play20.auth.AuthConfig` in `app/controllers`.

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
      val idManifest: ClassManifest[Id] = classManifest[Id]
    
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
      def loginSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Message.main)
    
      /**
       * Where to redirect the user after logging out
       */
      def logoutSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * If the user is not logged in and tries to access a protected resource then redirct them as follows:
       */
      def authenticationFailed[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * If authorization failed (usually incorrect password) redirect the user as follows:
       */
      def authorizationFailed[A](request: Request[A]): PlainResult = Forbidden("no permission")
    
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
    
    }
    ```

1. Next create a `Controller` that defines both login and logout actions.
   This `Controller` is mixes in the `jp.t2v.lab.play20.auth.LoginLogout` trait and
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
       * Since the `gotoLogoutSucceeded` returns `PlainResult`, 
       * you can add a procedure like the following.
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
       * Since the `gotoLoginSucceeded` returns `PlainResult`, 
       * you can add a procedure like the `gotoLogoutSucceeded`.
       */
      def authenticate = Action { implicit request =>
        loginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(html.login(formWithErrors)),
          user => gotoLoginSucceeded(user.get.id)
        )
      }
    
    }
    ```

1. Lastly, mix `jp.t2v.lab.play20.auth.Auth` trait and the trait that was created in the first step
   into your Controllers:

    ```scala
    object Message extends Controller with Auth with AuthConfigImpl {
    
      // The `authorizedAction` method
      //    takes `Authority` as the first argument and
      //    a function signature `User => Request[AnyContent] => Result` as the second argument and
      //    returns an `Action`
    
      def main = authorizedAction(NormalUser) { user => implicit request =>
        val title = "message main"
        Ok(html.message.main(title))
      }
    
      def list = authorizedAction(NormalUser) { user => implicit request =>
        val title = "all messages"
        Ok(html.message.list(title))
      }
    
      def detail(id: Int) = authorizedAction(NormalUser) { user => implicit request =>
        val title = "messages detail "
        Ok(html.message.detail(title + id))
      }
    
      // Only Administrator can execute this action.
      def write = authorizedAction(Administrator) { user => implicit request =>
        val title = "write message"
        Ok(html.message.write(title))
      }
    
    }
    ```


Advanced usage
---------------------------------------

### Changing the authorization depending on the request parameters.

For example, a Social networking application has a function to edit messages.

It should be possible for a user to edit their own messages and impossible for that user to edit other people's messages.

To achieve this you could define `Authority` as a `Function`:

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other setup is omitted. 

  type Authority = User => Boolean

  def authorize(user: User, authority: Authority): Boolean = authority(user)

}
```

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  private def sameAuthor(messageId: Int)(account: Account): Boolean =
    Message.getAuther(messageId) == account

  def edit(messageId: Int) = authorizedAction(sameAuthor(messageId)) { user => request =>
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

  def authenticationFailed[A](request: Request[A]): PlainResult = 
    Redirect(routes.Application.login).withSession("access_uri" -> request.uri)

  def loginSucceeded[A](request: Request[A]): PlainResult = {
    val uri = request.session.get("access_uri").getOrElse(routes.Message.main.url.toString)
    request.session - "access_uri"
    Redirect(uri)
  }

}
```

### changing the display depending on whether the user is logged in 

If you want to display the application's index differently to non-logged-in users
and logged-in users, you can use `optionalUserAction`:

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  // maybeUser is an instance of `Option[User]`.
  def index = optionalUserAction { maybeUser => request =>
    val user: User = maybeUser.getOrElse(GuestUser)
    Ok(html.index(user))
  }

}
```


### Action composition

Suppose you want to validate a token at every action in order to defeat a [Cross Site Request Forgery](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)) attack.

Since it is impractical to perform the validation in all actions, usually you would define a method as follows:

```scala
object Application extends Controller {

  // Other settings are omitted.

  val tokenForm = Form("token" -> text)

  private def validateToken(request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  private def validAction(f: Request[AnyContent] => Result) = Action { request =>
    if (validateToken(request)) f(request)
    else BadRequest
  }

  def page1 = validAction { request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = validAction { request =>
    // do something
    Ok(html.page2("result"))
  }

}
```

Authenticating and authorizing a user using a `validateToken` ?

You need to use the `authorized` method instead of the `authorizedAction` method.

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  // The token publication is omitted.

  val tokenForm = Form("token" -> text)

  private def validateToken(implicit request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  private authAndValidAction(authority: Authority)(f: User => Request[AnyContent] => Result) =
    Action { implicit request =>
      (for {
        user <- authorized(authority).right
        _    <- Either.cond(validateToken, (), BadRequest).right
      } yield f(user)(request)).merge
    }

  def page1 = authAndValidAction(NormalUser) { user => request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = authAndValidAction(NormalUser) { user => request =>
    // do something
    Ok(html.page2("result"))
  }

}
```

A complex example: Changing templates dynamically using [pjax](http://pjax.heroku.com/dinosaurs.html) 


```scala

  private type Template = String => Html
  private def pjax(implicit request: Request[AnyContent]): Template = {
    if (request.headers.keys("X-PJAX")) {
      html.pjaxTemplate.apply
    } else {
      val displayValues = DomainLogic.getDisplayValues()
      html.fullTemplate.apply(displayValues)
    }
  }

  private complexAction(authority: Authority)(f: User => Template => Request[AnyContent] => Result) =
    Action { implicit request =>
      (for {
        user     <- authorized(authority).right
        _        <- Either.cond(validateToken, (), BadRequest).right
        template <- Right(pjax).right
      } yield f(user)(template)(request)).merge
    }

  def page1 = complexAction(NormalUser) { user => template => request =>
    // do something
    Ok(template("result"))
  }

  def page2 = complexAction(NormalUser) { user => template => request =>
    // do something
    Ok(template("result"))
  }
```

Note that you can _combine functions_ for action methods.


### Stateless vs Stateful implementation.

Play20-auth follows the Play framework's stateless policy.
However, Play20-auth's default implementation is stateful, 
because the stateless implementation has the following security risk:

If user logs-in to your application in a internet-cafe, then returns home neglecting to logout.
If the user logs in again at home they will *not* invalidate the session.

Nevertheless, you want to use a fully stateless implementation then just override the `resolver` method of `AuthConfig` like this:

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other settings omitted.

  override def resolver[A](implicit request: Request[A]) =
    new CookieRelationResolver[Id, A](request)

}
```

You could also store the session data in a Relational Database by overriding the resolver.

Note: `CookieRelationResolver` doesn't support session timeout.


Running The Sample Application
---------------------------------------

1. `git clone https://github.com/t2v/play20-auth.git`
1. `cd play20-auth`
1. `play`
1. `run`
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

[Ehcache](http://ehcache.org), the default cache implementation used by Play2.0, does not work on distributed application servers.

If you have distributed servers, use the [Memcached Plugin](https://github.com/mumoshu/play2-memcached) or something similar.


License
---------------------------------------

This library is released under the Apache Software License, version 2, 
which should be included with the source in a file named `LICENSE`.

