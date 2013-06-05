package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.reflect.ClassTag
import concurrent.{ExecutionContext, Future}

trait AuthConfig {

  type Id

  type User

  type Authority

  implicit def idTag: ClassTag[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id): Option[User]

  def resolveUserAsync(id: Id)(implicit context: ExecutionContext): Future[Option[User]] = Future.successful(resolveUser(id))

  def loginSucceeded(request: RequestHeader): Result

  def logoutSucceeded(request: RequestHeader): Result

  def authenticationFailed(request: RequestHeader): Result

  def authorizationFailed(request: RequestHeader): Result

  def authorize(user: User, authority: Authority): Boolean

  def authorizeAsync(user: User, authority: Authority)(implicit context: ExecutionContext): Future[Boolean] = Future.successful(authorize(user, authority))

  lazy val idContainer: IdContainer[Id] = new CacheIdContainer[Id]

  lazy val cookieName: String = "PLAY2AUTH_SESS_ID"

  lazy val cookieSecureOption: Boolean = false

  lazy val cookieHttpOnlyOption: Boolean = true

  lazy val cookieDomainOption: Option[String] = None

  lazy val cookiePathOption: String = "/"

}
