package jp.t2v.lab.play2.auth

import play.api.mvc.{SimpleResult, Controller}
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}
import scala.concurrent.Future

trait AuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[SimpleResult]): Future[SimpleResult] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    req.get(AuthorityKey) map { authority =>
      authorized(authority) flatMap {
        case Right(user) => super.proceed(req.set(AuthKey, user))(f)
        case Left(result) => Future.successful(result)
      }
    } getOrElse {
      authorizationFailed(req)
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = req.get(AuthKey).get

}

trait OptionalAuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[SimpleResult]): Future[SimpleResult] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    val maybeUserFuture = restoreUser.recover { case _ => Option.empty }
    maybeUserFuture.flatMap(maybeUser => super.proceed(maybeUser.map(u => req.set(AuthKey, u)).getOrElse(req))(f))
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): Option[User] = req.get(AuthKey)
}

trait AuthenticationElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[SimpleResult]): Future[SimpleResult] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    restoreUser recover {
      case _ => Option.empty
    } flatMap {
      case Some(u) => super.proceed(req.set(AuthKey, u))(f)
      case None    => authenticationFailed(req)
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = req.get(AuthKey).get

}
