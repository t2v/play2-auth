package jp.t2v.lab.play2.auth

import play.api.mvc.{ActionBuilder, AnyContent, BaseController}

import scala.concurrent.ExecutionContext

trait BaseAuthActionBuilders[Id, User, Authority] extends AuthActionBuilders[Id, User, Authority] { self: BaseController =>

  final def OptionalAuthAction(implicit ec: ExecutionContext): ActionBuilder[OptionalAuthRequest, AnyContent] = composeOptionalAuthAction(Action)
  final def AuthenticationAction(implicit ec: ExecutionContext): ActionBuilder[AuthRequest, AnyContent] = composeAuthenticationAction(Action)
  final def AuthorizationAction(authority: Authority)(implicit ec: ExecutionContext): ActionBuilder[AuthRequest, AnyContent] = composeAuthorizationAction(Action)(authority)

}
