package jp.t2v.lab.play20.auth

import play.api.mvc.{Request, PlainResult}


trait AuthConfig {

  type Id

  type User

  type Authority

  def idManifest: ClassManifest[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id): Option[User]

  def loginSucceeded(request: Request[Any]): PlainResult

  def logoutSucceeded(request: Request[Any]): PlainResult

  def authenticationFailed(request: Request[Any]): PlainResult

  def authorizationFailed(request: Request[Any]): PlainResult

  def authorize(user: User, authority: Authority): Boolean

}