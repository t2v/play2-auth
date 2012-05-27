package jp.t2v.lab.play20.auth

import play.api.mvc.{Request, PlainResult}

trait AuthConfig {

  type Id

  type User

  type Authority

  implicit def idManifest: ClassManifest[Id]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id): Option[User]

  def loginSucceeded[A](request: Request[A]): PlainResult

  def logoutSucceeded[A](request: Request[A]): PlainResult

  def authenticationFailed[A](request: Request[A]): PlainResult

  def authorizationFailed[A](request: Request[A]): PlainResult

  def authorize(user: User, authority: Authority): Boolean

  def resolver[A](implicit request: Request[A]): RelationResolver[Id] = new CacheRelationResolver[Id]

}