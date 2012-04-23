package jp.t2v.lab.play20.auth

import play.api.mvc.PlainResult

trait AuthConfig {

  type ID

  type USER

  type AUTHORITY

  def idManifest: ClassManifest[ID]

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: ID): Option[USER]

  def loginSucceeded: PlainResult

  def logoutSucceeded: PlainResult

  def authenticationFailed: PlainResult

  def authorizationFailed: PlainResult

  def authorize(user: USER, authority: AUTHORITY): Boolean

}