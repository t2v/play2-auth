package controllers

import jp.t2v.lab.play2.auth.AuthConfig
import jp.t2v.lab.play2.auth.sample.{Permission, Account}
import jp.t2v.lab.play2.auth.sample.Permission._
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{Future, ExecutionContext}
import scala.reflect._

trait BaseAuthConfig  extends AuthConfig {

  type Id = Int
  type User = Account
  type Authority = Permission

  val idTag: ClassTag[Id] = classTag[Id]
  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext) = Future.successful(Account.findById(id))
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Forbidden("no permission"))
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext) = Future.successful((user.permission, authority) match {
    case (Administrator, _) => true
    case (NormalUser, NormalUser) => true
    case _ => false
  })

}
