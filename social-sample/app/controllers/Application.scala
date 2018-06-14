package controllers

import javax.inject.Inject

import models._
import play.api.mvc.Results._
import play.api.mvc._
import scalikejdbc.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.{ ClassTag, classTag }
import jp.t2v.lab.play2.auth._
import jp.t2v.lab.play2.auth.social.providers.twitter.{ TwitterController, TwitterProviderUserSupport }
import jp.t2v.lab.play2.auth.social.providers.facebook.{ FacebookController, FacebookProviderUserSupport }
import jp.t2v.lab.play2.auth.social.providers.github.{ GitHubController, GitHubProviderUserSupport }
import jp.t2v.lab.play2.auth.social.providers.slack.SlackController
import play.api.Environment
import play.api.cache.AsyncCacheApi
import play.api.libs.ws.WSClient

class Application @Inject() (components: ControllerComponents, val environment: Environment, val cacheApi: AsyncCacheApi) extends AbstractController(components) with OptionalAuthElement with AuthConfigImpl with Logout {

  def index = StackAction { implicit request =>
    DB.readOnly { implicit session =>
      val user = loggedIn
      val gitHubUser = user.flatMap(u => GitHubUser.findByUserId(u.id))
      val facebookUser = user.flatMap(u => FacebookUser.findByUserId(u.id))
      val twitterUser = user.flatMap(u => TwitterUser.findByUserId(u.id))
      val slackAccessToken = user.flatMap(u => SlackAccessToken.findByUserId(u.id))
      Ok(views.html.index(user, gitHubUser, facebookUser, twitterUser, slackAccessToken))
    }
  }

  def logout = Action.async { implicit request =>
    loginSucceeded(request)
  }

}

trait AuthConfigImpl extends AuthConfig {
  type Id = Long
  type User = models.User
  type Authority = models.Authority
  val idTag: ClassTag[Id] = classTag[Id]
  val sessionTimeoutInSeconds: Int = 3600

  val cacheApi: AsyncCacheApi

  val idContainer: AsyncIdContainer[Id] = AsyncIdContainer(new CacheIdContainer[Id](cacheApi))
  
  override lazy val tokenAccessor: TokenAccessor = new CookieTokenAccessor()
  
  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] =
    Future.successful(DB.readOnly { implicit session =>
      User.find(id)
    })

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.Application.index()))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.Application.index))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.Application.index))

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext) =
    Future.successful(Forbidden("no permission"))

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    true
  }

}

class FacebookAuthController @Inject() (components: ControllerComponents, val ws: WSClient, val environment: Environment, val cacheApi: AsyncCacheApi) extends AbstractController(components)
    with FacebookController
    with AuthConfigImpl
    with FacebookProviderUserSupport {

  override def onOAuthLinkSucceeded(token: AccessToken, consumerUser: User)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).map { providerUser =>
      DB.localTx { implicit session =>
        FacebookUser.save(consumerUser.id, providerUser)
        Redirect(routes.Application.index)
      }
    }
  }

  override def onOAuthLoginSucceeded(token: AccessToken)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).flatMap { providerUser =>
      DB.localTx { implicit session =>
        FacebookUser.findById(providerUser.id) match {
          case None =>
            val id = User.create(providerUser.name, providerUser.coverUrl).id
            FacebookUser.save(id, providerUser)
            loginSucceeded(request).flatMap(markLoggedIn(id))
          case Some(fu) =>
            loginSucceeded(request).flatMap(markLoggedIn(fu.userId))
        }
      }
    }
  }

}

class GitHubAuthController @Inject() (components: ControllerComponents, val ws: WSClient, val environment: Environment, val cacheApi: AsyncCacheApi) extends AbstractController(components) 
    with GitHubController
    with AuthConfigImpl
    with GitHubProviderUserSupport {

  override def onOAuthLinkSucceeded(token: AccessToken, consumerUser: User)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).map { providerUser =>
      DB.localTx { implicit session =>
        GitHubUser.save(consumerUser.id, providerUser)
        Redirect(routes.Application.index)
      }
    }
  }

  override def onOAuthLoginSucceeded(token: AccessToken)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).flatMap { providerUser =>
      DB.localTx { implicit session =>
        GitHubUser.findById(providerUser.id) match {
          case None =>
            val id = User.create(providerUser.login, providerUser.avatarUrl).id
            GitHubUser.save(id, providerUser)
            loginSucceeded(request).flatMap(markLoggedIn(id))
          case Some(gh) =>
            loginSucceeded(request).flatMap(markLoggedIn(gh.userId))
        }
      }
    }
  }

}

class TwitterAuthController @Inject() (components: ControllerComponents, val ws: WSClient, val environment: Environment, val cacheApi: AsyncCacheApi) extends AbstractController(components)
    with TwitterController
    with AuthConfigImpl
    with TwitterProviderUserSupport {

  override def onOAuthLinkSucceeded(token: AccessToken, consumerUser: User)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).map { providerUser =>
      DB.localTx { implicit session =>
        TwitterUser.save(consumerUser.id, providerUser)
        Redirect(routes.Application.index)
      }
    }
  }

  override def onOAuthLoginSucceeded(token: AccessToken)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    retrieveProviderUser(token).flatMap { providerUser =>
      DB.localTx { implicit session =>
        TwitterUser.findById(providerUser.id) match {
          case None =>
            val id = User.create(providerUser.screenName, providerUser.profileImageUrl).id
            TwitterUser.save(id, providerUser)
            //gotoLoginSucceeded(id)
            loginSucceeded(request).flatMap(markLoggedIn(id))
          case Some(tu) =>
            loginSucceeded(request).flatMap(markLoggedIn(tu.userId))
        }
      }
    }
  }

}

class SlackAuthController @Inject() (components: ControllerComponents, val ws: WSClient, val environment: Environment, val cacheApi: AsyncCacheApi) extends AbstractController(components)
    with SlackController
    with AuthConfigImpl {

  override def onOAuthLinkSucceeded(accessToken: AccessToken, consumerUser: User)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    Future.successful {
      DB.localTx { implicit session =>
        SlackAccessToken.save(consumerUser.id, accessToken)
        Redirect(routes.Application.index)
      }
    }
  }

  override def onOAuthLoginSucceeded(accessToken: AccessToken)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    ???
  }

}
