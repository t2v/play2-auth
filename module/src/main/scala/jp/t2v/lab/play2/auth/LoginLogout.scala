package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto
import scala.concurrent.{Future, ExecutionContext}

trait Login {
  self: AbstractController with AuthConfig =>

  def markLoggedIn(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Result => Future[Result] = { result =>
    idContainer.startNewSession(userId, sessionTimeoutInSeconds).map(token => tokenAccessor.put(token)(result))
  }

}

trait Logout {
  self: AbstractController with AuthConfig =>

  def markLoggedOut()(implicit request: RequestHeader, ctx: ExecutionContext): Result => Future[Result] = { result =>
    tokenAccessor.extract(request) foreach idContainer.remove
    Future.successful(tokenAccessor.delete(result))
  }

}

trait LoginLogout extends Login with Logout {
  self: AbstractController with AuthConfig =>
}