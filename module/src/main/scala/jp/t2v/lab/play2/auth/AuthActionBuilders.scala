package jp.t2v.lab.play2.auth

import play.api.libs.typedmap.TypedKey
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import languageFeature.higherKinds


case class OptionalAuthFunction[Id, User, Authority](
  asyncAuth: AsyncAuth[Id, User, Authority],
  protected val executionContext: ExecutionContext
) extends ActionFunction[Request, Request] {

  import asyncAuth._

  val UserKey: TypedKey[User] = TypedKey("play2-auth-user")

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val ec: ExecutionContext = executionContext
    restoreUser(request, executionContext) recover {
      case _ => None -> identity[Result] _
    } flatMap { case (userOpt, cookieUpdater) =>
      userOpt.fold(block(request))(user => block(request.addAttr(UserKey, user)).map(cookieUpdater))
    }
  }
}

class AuthActionBuilders[Id, User, Authority](asyncAuth: AsyncAuth[Id, User, Authority]) {

  import asyncAuth._



//  final case class GenericAuthenticationRefiner[R[+_] <: Request[_]]() extends ActionRefiner[({type L[+A] = GenericOptionalAuthRequest[A, R]})#L, ({type L[+A] = GenericAuthRequest[A, R]})#L] {
//    override protected def refine[A](request: GenericOptionalAuthRequest[A, R]): Future[Either[Result, GenericAuthRequest[A, R]]] = {
//      request.user map { user =>
//        Future.successful(Right[Result, GenericAuthRequest[A, R]](GenericAuthRequest[A, R](user, request.underlying)))
//      } getOrElse {
//        implicit val ctx = executionContext
//        authenticationFailed(request).map(Left.apply[Result, GenericAuthRequest[A, R]])
//      }
//    }
//  }
//
//  final case class GenericAuthorizationFilter[R[+_] <: Request[_]](authority: Authority) extends ActionFilter[({type L[+B] = GenericAuthRequest[B, R]})#L] {
//    override protected def filter[A](request: GenericAuthRequest[A, R]): Future[Option[Result]] = {
//      implicit val ctx = executionContext
//      authorize(request.user, authority) collect {
//        case true => None
//      } recoverWith {
//        case _ => authorizationFailed(request, request.user, Some(authority)).map(Some.apply)
//      }
//    }
//  }
//
//  final def composeOptionalAuthAction[R[+_] <: Request[_]](builder: ActionBuilder[R]): ActionBuilder[({type L[+A] = GenericOptionalAuthRequest[A, R]})#L] = {
//    builder.andThen[({type L[A] = GenericOptionalAuthRequest[A, R]})#L](GenericOptionalAuthFunction[R]())
//  }
//
//  final def composeAuthenticationAction[R[+_] <: Request[_]](builder: ActionBuilder[R]): ActionBuilder[({type L[+A] = GenericAuthRequest[A, R]})#L] = {
//    composeOptionalAuthAction[R](builder).andThen[({type L[+A] = GenericAuthRequest[A, R]})#L](GenericAuthenticationRefiner[R]())
//  }
//
//  final def composeAuthorizationAction[R[+_] <: Request[_]](builder: ActionBuilder[R])(authority: Authority): ActionBuilder[({type L[+A] = GenericAuthRequest[A, R]})#L] = {
//    composeAuthenticationAction(builder).andThen[({type L[+A] = GenericAuthRequest[A, R]})#L](GenericAuthorizationFilter[R](authority))
//  }
//
//  final type OptionalAuthRequest[+A] = GenericOptionalAuthRequest[A, Request]
//  final type AuthRequest[+A] = GenericAuthRequest[A, Request]
//  final val OptionalAuthFunction: ActionFunction[Request, OptionalAuthRequest] = GenericOptionalAuthFunction[Request]()
//  final val AuthenticationRefiner: ActionRefiner[OptionalAuthRequest, AuthRequest] = GenericAuthenticationRefiner[Request]()
//  final def AuthorizationFilter(authority: Authority): ActionFilter[AuthRequest] = GenericAuthorizationFilter[Request](authority)
//
//  final val OptionalAuthAction: ActionBuilder[OptionalAuthRequest] = composeOptionalAuthAction(Action)
//  final val AuthenticationAction: ActionBuilder[AuthRequest] = composeAuthenticationAction(Action)
//  final def AuthorizationAction(authority: Authority): ActionBuilder[AuthRequest] = composeAuthorizationAction(Action)(authority)

}
