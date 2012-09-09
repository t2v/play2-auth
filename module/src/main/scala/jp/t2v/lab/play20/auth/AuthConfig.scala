package jp.t2v.lab.play20.auth

import play.api.mvc.{RequestHeader, PlainResult}

trait AuthConfig {

  type Id

  type User

  type Authority

  implicit def idManifest: ClassManifest[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id): Option[User]

  def loginSucceeded(request: RequestHeader): PlainResult

  def logoutSucceeded(request: RequestHeader): PlainResult

  def authenticationFailed(request: RequestHeader): PlainResult

  def authorizationFailed(request: RequestHeader): PlainResult

  def authorize(user: User, authority: Authority): Boolean

  def resolver(implicit request: RequestHeader): RelationResolver[Id] = new CacheRelationResolver[Id]

}
