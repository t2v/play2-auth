package controllers.stateless

import controllers.BaseAuthConfig
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{Future, ExecutionContext}
import jp.t2v.lab.play2.auth.{TransparentIdContainer, AsyncIdContainer}

import jp.t2v.lab.play2.auth.TokenAccessor
import jp.t2v.lab.play2.auth.CookieTokenAccessor
import play.api.Environment

trait AuthConfigImpl extends BaseAuthConfig {

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Messages.main))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Sessions.login))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Sessions.login))

  override lazy val idContainer = AsyncIdContainer(new TransparentIdContainer[Id])

  val environment: Environment
  override lazy val tokenAccessor: TokenAccessor = new CookieTokenAccessor(
    cookieName = "PLAY2AUTH_SESS_ID",
    cookieSecureOption = environment.mode == play.api.Mode.Prod,
    cookieHttpOnlyOption = true,
    cookieDomainOption = None,
    cookiePathOption = "/",
    cookieMaxAge = None
  )
}