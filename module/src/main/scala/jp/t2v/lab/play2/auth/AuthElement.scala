package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, Controller}
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}

trait AuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    implicit val r = req
    req.get(AuthorityKey) map { authority =>
      Async {
        authorized(authority) map {
          case Right(user) => super.proceed(req.set(AuthKey, user))(f)
          case Left(result) => result
        }
      }
    } getOrElse {
      authorizationFailed(req)
    }
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): User = req.get(AuthKey).get

}

trait OptionalAuthElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    implicit val r = req
    val maybeUserFuture = restoreUser.recover { case _ => Option.empty }
    Async {
      maybeUserFuture.map(maybeUser => super.proceed(maybeUser.map(u => req.set(AuthKey, u)).getOrElse(req))(f))
    }
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): Option[User] = req.get(AuthKey)
}

trait AuthenticationElement extends StackableController with AsyncAuth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    implicit val r = req
    Async {
      restoreUser collect {
        case Some(u) => super.proceed(req.set(AuthKey, u))(f)
      } recover {
        case _ => authenticationFailed(req)
      }
    }
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): User = req.get(AuthKey).get

}
