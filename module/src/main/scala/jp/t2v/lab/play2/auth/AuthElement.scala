package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, Controller}
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}

trait AuthElement extends StackableController with Auth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    (for {
      authority <- req.get(AuthorityKey).toRight(authorizationFailed(req)).right
      user      <- authorized(authority)(req).right
    } yield super.proceed(req.set(AuthKey, user))(f)).merge
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): User = req.get(AuthKey).get

}

trait OptionalAuthElement extends StackableController with Auth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    val maybeUser = restoreUser(req)
    super.proceed(maybeUser.map(u => req.set(AuthKey, u)).getOrElse(req))(f)
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): Option[User] = req.get(AuthKey)
}

trait AuthenticationElement extends StackableController with Auth {
    self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    restoreUser(req).map {
      user => super.proceed(req.set(AuthKey, user))(f)
    }.getOrElse(authenticationFailed(req))
  }

  implicit def loggedIn[A](implicit req: RequestWithAttributes[A]): User = req.get(AuthKey).get

}
