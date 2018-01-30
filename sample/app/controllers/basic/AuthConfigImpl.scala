package controllers.basic

import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{Future, ExecutionContext}
import jp.t2v.lab.play2.auth.AuthConfig
import jp.t2v.lab.play2.auth.sample.{Role, Account}
import jp.t2v.lab.play2.auth.sample.Role._

trait AuthConfigImpl extends AuthConfig[Account, Account, Role] {

  def resolveUser(id: Account)(implicit ctx: ExecutionContext) = Future.successful(Some(id))

  def authorize(user: Account, authority: Role)(implicit ctx: ExecutionContext) = Future.successful((user.role, authority) match {
    case (Administrator, _)       => true
    case (NormalUser, NormalUser) => true
    case _                        => false
  })

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful {
    Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="SECRET AREA"""")
  }
  def authorizationFailed(request: RequestHeader, user: Account, authority: Option[Role])(implicit ctx: ExecutionContext) = Future.successful(Forbidden("no permission"))

}