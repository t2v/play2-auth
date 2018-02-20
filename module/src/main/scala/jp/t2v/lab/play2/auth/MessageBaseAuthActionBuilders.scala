package jp.t2v.lab.play2.auth

import play.api.mvc.{ActionBuilder, AnyContent, MessagesBaseController, MessagesRequest}

import scala.concurrent.ExecutionContext

trait MessageBaseAuthActionBuilders[Id, User, Authority] extends AuthActionBuilders[Id, User, Authority] { self: MessagesBaseController =>

//  MessagesRequest is invariant!!!!!
//  https://github.com/playframework/playframework/blob/152844c56eed25ba562c4c6c5f5e137e18414734/framework/src/play/src/main/scala/play/api/mvc/MessagesRequest.scala#L45
//  Can not compile this.

  type MessageOptionalAuthRequest[+A] = GenericOptionalAuthRequest[A, MessagesRequest]
  type MassageAuthRequest[+A] = GenericAuthRequest[A, MessagesRequest]
//
//  final def OptionalAuthAction(implicit ec: ExecutionContext): ActionBuilder[MessageOptionalAuthRequest, AnyContent] = composeOptionalAuthAction(Action)
//  final def AuthenticationAction(implicit ec: ExecutionContext): ActionBuilder[MassageAuthRequest, AnyContent] = composeAuthenticationAction(Action)
//  final def AuthorizationAction(authority: Authority)(implicit ec: ExecutionContext): ActionBuilder[MassageAuthRequest, AnyContent] = composeAuthorizationAction(Action)(authority)

}
