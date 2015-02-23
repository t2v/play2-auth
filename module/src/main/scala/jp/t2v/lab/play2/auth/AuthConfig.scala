package jp.t2v.lab.play2.auth

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

  def authorizationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authorize(user: User, authority: Authority)(implicit context: ExecutionContext): Future[Boolean]

  lazy val idContainer: AsyncIdContainer[Id] = AsyncIdContainer(new CacheIdContainer[Id])

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val cookieName: String = "PLAY2AUTH_SESS_ID"

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val cookieSecureOption: Boolean = false

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val cookieHttpOnlyOption: Boolean = true

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val cookieDomainOption: Option[String] = None

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val cookiePathOption: String = "/"

  @deprecated(since = "0.14.0", message = "it will be deleted since 0.15.x. use CookieTokenAccessor constructor")
  lazy val isTransientCookie: Boolean = false

  lazy val tokenAccessor: TokenAccessor = new CookieTokenAccessor(
    cookieName = cookieName,
    cookieSecureOption = cookieSecureOption,
    cookieHttpOnlyOption = cookieHttpOnlyOption,
    cookieDomainOption = cookieDomainOption,
    cookiePathOption = cookiePathOption,
    cookieMaxAge = if (isTransientCookie) None else Some(sessionTimeoutInSeconds)
  )

}
