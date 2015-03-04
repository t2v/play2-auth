package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, Controller}
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}
import scala.concurrent.Future

trait AuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    req.get(AuthorityKey) map { authority =>
      authorized(authority) flatMap {
        case Right((user, resultUpdater)) => super.proceed(req.set(AuthKey, user))(f).map(resultUpdater)
        case Left(result)                 => Future.successful(result)
      }
    } getOrElse {
      restoreUser collect {
        case (Some(user), _) => user
      } flatMap {
        authorizationFailed(req, _, None)
      } recoverWith {
        case _ => authenticationFailed(req)
      }
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = req.get(AuthKey).get

}

trait OptionalAuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    val maybeUserFuture = restoreUser.recover { case _ => None -> identity[Result] _ }
    maybeUserFuture.flatMap { case (maybeUser, cookieUpdater) =>
      super.proceed(maybeUser.map(u => req.set(AuthKey, u)).getOrElse(req))(f).map(cookieUpdater)
    }
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): Option[User] = req.get(AuthKey)
}

trait AuthenticationElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    restoreUser recover {
      case _ => None -> identity[Result] _
    } flatMap {
      case (Some(u), cookieUpdater) => super.proceed(req.set(AuthKey, u))(f).map(cookieUpdater)
      case (None, _)                => authenticationFailed(req)
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = req.get(AuthKey).get

}
