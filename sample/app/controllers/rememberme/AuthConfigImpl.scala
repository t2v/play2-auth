//package controllers.rememberme
//
//import controllers.BaseAuthConfig
//import play.api.mvc.RequestHeader
//import play.api.mvc.Results._
//
//import scala.concurrent.{Future, ExecutionContext}
//
//trait AuthConfigImpl extends BaseAuthConfig {
//
//  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Messages.main))
//
//  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Sessions.login))
//
//  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Sessions.login))
//
////  override lazy val tokenAccessor = new RememberMeTokenAccessor(sessionTimeoutInSeconds)
//
//}