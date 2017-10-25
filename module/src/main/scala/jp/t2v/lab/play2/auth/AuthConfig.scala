package jp.t2v.lab.play2.auth

import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

trait AuthConfig[Id, User, Authority] {

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: Id)(implicit context: ExecutionContext): Future[Option[User]]

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result]

  def authorize(user: User, authority: Authority)(implicit context: ExecutionContext): Future[Boolean]

}
