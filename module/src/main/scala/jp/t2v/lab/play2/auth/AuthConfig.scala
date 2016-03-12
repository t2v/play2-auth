package jp.t2v.lab.play2.auth

import play.api.cache.CacheApi
import play.api.mvc._
import scala.reflect.{ClassTag, classTag}
import scala.concurrent.{ExecutionContext, Future}

trait AuthConfig {

  type Id

  type User

  type Authority

  implicit def idTag: ClassTag[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id)(implicit context: ExecutionContext): Future[Option[User]]

  def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result]

  def authorize(user: User, authority: Authority)(implicit context: ExecutionContext): Future[Boolean]

  val idContainer: AsyncIdContainer[Id]

  @deprecated("it will be deleted since 0.14.x. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val cookieName: String = throw new AssertionError("use tokenAccessor setting instead.")

  @deprecated("it will be deleted since 0.14.0. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val cookieSecureOption: Boolean = throw new AssertionError("use tokenAccessor setting instead.")

  @deprecated("it will be deleted since 0.14.0. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val cookieHttpOnlyOption: Boolean = throw new AssertionError("use tokenAccessor setting instead.")

  @deprecated("it will be deleted since 0.14.0. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val cookieDomainOption: Option[String] = throw new AssertionError("use tokenAccessor setting instead.")

  @deprecated("it will be deleted since 0.14.0. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val cookiePathOption: String = throw new AssertionError("use tokenAccessor setting instead.")

  @deprecated("it will be deleted since 0.14.0. use CookieTokenAccessor constructor", since = "0.13.1")
  final lazy val isTransientCookie: Boolean = throw new AssertionError("use tokenAccessor setting instead.")

  lazy val tokenAccessor: TokenAccessor = new CookieTokenAccessor(
    cookieName = "PLAY2AUTH_SESS_ID",
    cookieSecureOption = play.api.Play.maybeApplication.exists(app => play.api.Play.isProd(app)),
    cookieHttpOnlyOption = true,
    cookieDomainOption = None,
    cookiePathOption = "/",
    cookieMaxAge = Some(sessionTimeoutInSeconds)
  )

}
