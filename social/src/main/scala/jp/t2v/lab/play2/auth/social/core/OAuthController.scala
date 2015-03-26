package jp.t2v.lab.play2.auth.social.core

import jp.t2v.lab.play2.auth.{ AuthConfig, OptionalAuthElement }
import play.api.mvc.{ Result, RequestHeader }

import scala.concurrent.{ ExecutionContext, Future }

trait OAuthController { self: OptionalAuthElement with AuthConfig =>

  protected val authenticator: OAuthAuthenticator

  type AccessToken = authenticator.AccessToken

  def onOAuthLoginSucceeded(token: AccessToken)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result]

  def onOAuthLinkSucceeded(token: AccessToken, consumerUser: User)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result]

  protected lazy val OAuthExecutionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

}
