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

  def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[SimpleResult]

  def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[SimpleResult]

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[SimpleResult]

  def authorizationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[SimpleResult]

  def authorize(user: User, authority: Authority, request: RequestHeader)(implicit context: ExecutionContext): Future[Boolean]

  lazy val idContainer: IdContainer[Id] = new CacheIdContainer[Id]

  lazy val cookieName: String = "PLAY2AUTH_SESS_ID"

  lazy val cookieSecureOption: Boolean = false

  lazy val cookieHttpOnlyOption: Boolean = true

  lazy val cookieDomainOption: Option[String] = None

  lazy val cookiePathOption: String = "/"

}
