Play2.0 module for Authentication and Authorization
===========================================================

This module offers Authentication and Authorization features to Play2.0 applications

Target
---------------------------------------

This module is targets the __Scala__ version of __Play2.0__.

For the Java version of Play2.0, there is an authorization module called [Deadbolt 2](https://github.com/schaloner/deadbolt-2).

This module has been tested on Play2.0final and Play2.0.1.

Motivation
---------------------------------------

### Secure

`Security` trait in Play2.0 API does not define an identifier that identifies a user.

If you use an E-mail or a user ID as an identier, 
users can not invalidate the session when the cookie leaks.

This module creates a unique SessionID using a secure random number generator.
Even if the cookie leaks, users can invalidate the session by logging in again and 
your application can set a time limit for sessions.


### Flexible

Since `Security` trait in Play2.0 API returns `Action`, 
complicated action methods are nested too deep.

This module provides an interface that return `Either[PlainResult, User]`.
so, writing complicated action methods is easy.


Installation
---------------------------------------

1. add a repository resolver into your `Build.scala` or `build.sbt` file.

        resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"

1. add a dependency declaration into your `Build.scala` or `build.sbt` file.
    1. stable release

            "jp.t2v" %% "play20.auth" % "0.2"

    1. current version

            "jp.t2v" %% "play20.auth" % "0.3-SNAPSHOT"

For example: `Build.scala`

```scala
  val appDependencies = Seq(
    "jp.t2v" %% "play20.auth" % "0.1"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  )
```

It is not necessary to create a `play.plugins` file.


Usage
---------------------------------------

1. First step, create a trait that is mixed-in `jp.t2v.lab.play20.auth.AuthConfig` in `app/controllers`.

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
       * This sample uses the following trait.
       *
       * sealed trait Permission
       * case object Administrator extends Permission
       * case object NormalUser extends Permission
       */
      type Authority = Permission
    
      /**
       * A `ClassManifest` is used to get an id from the Cache API.
       * Basically use the same setting as the following.
       */
      val idManifest: ClassManifest[Id] = classManifest[Id]
    
      /**
       * A duration of the session timeout in seconds
       */
      val sessionTimeoutInSeconds: Int = 3600
    
      /**
       * A function that returns a `User` object from an `Id`.
       * Describe the procedure according to your application.
       */
      def resolveUser(id: Id): Option[User] = Account.findById(id)
    
      /**
       * A redirect target after a successful user login.
       */
      def loginSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Message.main)
    
      /**
       * A redirect target after a successful user logout.
       */
      def logoutSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * A redirect target after a failed authentication.
       */
      def authenticationFailed[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * A redirect target after a failed authorization.
       */
      def authorizationFailed[A](request: Request[A]): PlainResult = Forbidden("no permission")
    
      /**
       * A function that authorizes a user by `Authority`.
       * Describe the procedure according to your application.
       */
      def authorize(user: User, authority: Authority): Boolean = 
        (user.permission, authority) match {
          case (Administrator, _) => true
          case (NormalUser, NormalUser) => true
          case _ => false
        }
    
    }
    ```

1. Next step, create a `Controller` that defines login and logout actions.
   This `Controller` is mixed with `jp.t2v.lab.play20.auth.LoginLogout` trait and
   the trait that was created in first step.

    ```scala
    object Application extends Controller with LoginLogout with AuthConfigImpl {
    
      /** Describe the login form according to your application. */
      val loginForm = Form {
        mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
          .verifying("Invalid email or password", result => result.isDefined)
      }
    
      /** Describe the login page action according to your application. */
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

1. Last step, mix `jp.t2v.lab.play20.auth.Auth` trait and the trait that was created in first step
   into your Controllers.

    ```scala
    object Message extends Controller with Auth with AuthConfigImpl {
    
      // The `authorizedAction` method
      //    takes an `Authority` as a first argument and
      //    takes a function whose type is `User => Request[AnyContent] => Result` as a second argument and
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

### Changing the authorization according to request parameters.

For example, a SNS application has a function that edit messages.

Your application should make it possible for a user to edit their own messages and impossible to edit other people's messages.

In this case, it is easy if `Authority` is a `Function` as follows.

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

For example, when an unauthenticated user requests access to non-login page, 
your application redirects the user to the login page.
Then, when the user successfully logs in, your application redirects the user to the originally requested page.

In this case, you only have to change `authenticationFailed` and `loginSucceeded` as follows.

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other settings are omitted.

  def authenticationFailed[A](request: Request[A]): PlainResult = 
    Redirect(routes.Application.login).withSession("access_uri" -> request.uri)

  def loginSucceeded[A](request: Request[A]): PlainResult = {
    val uri = request.session.get("access_uri").getOrElse(routes.Message.main.url)
    request.session - "access_uri"
    Redirect(uri)
  }

}
```

### changing a disply by the login state 

When you want to display the application's index to un-logged-in users
and logged-in users, you only have to user `optionalUserAction` as follows.

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  // maybeUser is an Option[User] instance.
  def index = optionalUserAction { maybeUser => request =>
    val user: User = maybeUser.getOrElse(GuestUser)
    Ok(html.index(user))
  }

}
```


### Action composition

For example, you want to validate token at every action to defeat a CSRF attack.

Since it is impractical to perform the validation in all actions, Usually a method is defined as follows.

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

How do you incorporate a function that authenticates and authorizes a user in `validateToken` ?

You need to use the `authorized` method insted of `authorizedAction` method.

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

This example is complex.

Then, how do you incorporate function that changes templates dynamically by pjax ?

This is easy:

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

Thus, you can combine functions for action methods.


### Stateless

We respect the Play framework stateless policy.
But, this module's default implementation is statefull, 
since the stateless implementation has the security risk as follow.

For example, A user log-in your application in a internet-cafe.
And the user returns home without logout.
By the stateless implementation, he(or she) can not invalidate the session.

Nevertheless, you want to use stateless, override the `resolver` method on `AuthConfig` as follows

```scala
trait AuthConfigImpl extends AuthConfig {

  // Other settings are omitted.

  override def resolver[A](implicit request: Request[A]) =
    new CookieRelationResolver[Id, A](request)

}
```

You make your application possible to save the authentication data on RDBMS by overriding the resolver.

`CookieRelationResolver` is not support session timeout.


Sample Application
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


Attention
---------------------------------------

This module uses the [Cache API](http://www.playframework.org/documentation/2.0/ScalaCache) of Play2.0.

[Ehcache](http://ehcache.org), the default implementation, 
can not treat authentication information appropriately when the application servers are distributed.

If you have distributed servers, 
you should rather use the [Memcached Plugin](https://github.com/mumoshu/play2-memcached) or something similar.


License
---------------------------------------

This library is released under the Apache Software License, version 2, 
which should be included with the source in a file named `LICENSE`.

