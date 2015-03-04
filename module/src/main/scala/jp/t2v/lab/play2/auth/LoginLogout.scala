package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto
import scala.concurrent.{Future, ExecutionContext}

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLoginSucceeded(userId, loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = for {
    token <- idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    r     <- result
  } yield tokenAccessor.put(token)(r)

  def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLogoutSucceeded(logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    tokenAccessor.extract(request) foreach idContainer.remove
    result.map(tokenAccessor.delete)
  }
}
