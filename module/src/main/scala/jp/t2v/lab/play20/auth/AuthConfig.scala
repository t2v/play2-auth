package jp.t2v.lab.play20.auth

import play.api.mvc._

trait AuthConfig {

  type Id

  type User

  type Authority

  implicit def idManifest: ClassManifest[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id): Option[User]

  def loginSucceeded(request: RequestHeader): Result

  def logoutSucceeded(request: RequestHeader): Result

  def authenticationFailed(request: RequestHeader): Result

  def authorizationFailed(request: RequestHeader): Result

  def authorize(user: User, authority: Authority): Boolean

  def idContainer: IdContainer[Id] = new CacheIdContainer[Id]

  lazy val cookieName: String = "PLAY2AUTH_SESS_ID"

  lazy val cookieSecureOption: Boolean = false

  lazy val cookieHttpOnlyOption: Boolean = true

  lazy val cookieDomainOption: Option[String] = None

  lazy val cookiePathOption: String = "/"

}
