package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.concurrent.Future

trait AuthActionBuilders extends AsyncAuth { self: AuthConfig with Controller =>

  final case class GenericOptionalAuthRequest[A, R[_] <: Request[_]](user: Option[User], underlying: R[A]) extends WrappedRequest[A](underlying.asInstanceOf[Request[A]])
  final case class GenericAuthRequest[A, R[_] <: Request[_]](user: User, request: R[A]) extends WrappedRequest[A](request.asInstanceOf[Request[A]])

  final case class GenericOptionalAuthRefiner[R[_] <: Request[_]]() extends ActionRefiner[R, ({type L[A] = GenericOptionalAuthRequest[A, R]})#L] {
    protected def refine[A](request: R[A]): Future[Either[Result, GenericOptionalAuthRequest[A, R]]] = {
      implicit val ctx = executionContext
      restoreUser(request.asInstanceOf[RequestHeader], executionContext) recover {
        case _ => None
      } map { user =>
        Right(GenericOptionalAuthRequest[A, R](user, request))
      }
    }
  }

  final case class GenericAuthenticationRefiner[R[_] <: Request[_]]() extends ActionRefiner[({type L[A] = GenericOptionalAuthRequest[A, R]})#L, ({type L[A] = GenericAuthRequest[A, R]})#L] {
    override protected def refine[A](request: GenericOptionalAuthRequest[A, R]): Future[Either[Result, GenericAuthRequest[A, R]]] = {
      request.user map { user =>
        Future.successful(Right[Result, GenericAuthRequest[A, R]](new GenericAuthRequest[A, R](user, request.underlying)))
      } getOrElse {
        implicit val ctx = executionContext
        authenticationFailed(request).map(Left.apply[Result, GenericAuthRequest[A, R]])
      }
    }
  }

  final case class GenericAuthorizationFilter[R[_] <: Request[_]](authority: Authority) extends ActionFilter[({type L[B] = GenericAuthRequest[B, R]})#L] {
    override protected def filter[A](request: GenericAuthRequest[A, R]): Future[Option[Result]] = {
      implicit val ctx = executionContext
      authorize(request.user, authority) collect {
        case true => None
      } recoverWith {
        case _ => authorizationFailed(request).map(Some.apply)
      }
    }
  }

  final def composeOptionalAuthAction[R[_] <: Request[_]](builder: ActionBuilder[R]): ActionBuilder[({type L[A] = GenericOptionalAuthRequest[A, R]})#L] = {
    builder.andThen[({type L[A] = GenericOptionalAuthRequest[A, R]})#L](GenericOptionalAuthRefiner[R]())
  }

  final def composeAuthenticationAction[R[_] <: Request[_]](builder: ActionBuilder[R]): ActionBuilder[({type L[A] = GenericAuthRequest[A, R]})#L] = {
    composeOptionalAuthAction[R](builder).andThen[({type L[A] = GenericAuthRequest[A, R]})#L](GenericAuthenticationRefiner[R]())
  }

  final def composeAuthorizationAction[R[_] <: Request[_]](builder: ActionBuilder[R])(authority: Authority): ActionBuilder[({type L[A] = GenericAuthRequest[A, R]})#L] = {
    composeAuthenticationAction(builder).andThen[({type L[A] = GenericAuthRequest[A, R]})#L](GenericAuthorizationFilter[R](authority))
  }

  final type OptionalAuthRequest[A] = GenericOptionalAuthRequest[A, Request]
  final type AuthRequest[A] = GenericAuthRequest[A, Request]
  final val OptionalAuthAction: ActionBuilder[OptionalAuthRequest] = composeOptionalAuthAction(Action)
  final val AuthenticationAction: ActionBuilder[AuthRequest] = composeAuthenticationAction(Action)
  final def AuthorizationAction(authority: Authority): ActionBuilder[AuthRequest] = composeAuthorizationAction(Action)(authority)

}
