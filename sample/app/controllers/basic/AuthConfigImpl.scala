package controllers.basic

import controllers.BaseAuthConfig
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}
import jp.t2v.lab.play2.auth.sample.Account

class AuthConfigImpl extends BaseAuthConfig {

  def resolveUser(id: Account)(implicit ctx: ExecutionContext) = Future.successful(Some(id))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful {
    Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="SECRET AREA"""")
  }

}