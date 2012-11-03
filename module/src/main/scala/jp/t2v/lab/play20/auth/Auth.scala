package jp.t2v.lab.play20.auth

import play.api.mvc._
import play.api.libs.iteratee.{Input, Done}

trait Auth {
  self: Controller with AuthConfig =>

  def authorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result): Action[AnyContent] =
    authorizedAction(BodyParsers.parse.anyContent, authority)(f)

  def authorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result): Action[A] = 
    Action(BodyParser(req => authorized(authority)(req) match {
      case Right(_) => p(req)
      case Left(result) => Done(Left(result), Input.Empty)
    })) { req =>
      authorized(authority)(req).right.map(u => f(u)(req)).merge
    }    
      
  def optionalUserAction(f: Option[User] => Request[AnyContent] => Result): Action[AnyContent] =
    optionalUserAction(BodyParsers.parse.anyContent)(f)

  def optionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result): Action[A] =
    Action(p)(req => f(restoreUser(req))(req))

  def authorized(authority: Authority)(implicit request: RequestHeader): Either[PlainResult, User] = for {
    user <- restoreUser(request).toRight(authenticationFailed(request)).right
    _ <- Either.cond(authorize(user, authority), (), authorizationFailed(request)).right
  } yield user

  private def restoreUser(implicit request: RequestHeader): Option[User] = for {
    sessionId <- request.session.get("sessionId")
    userId <- resolver.sessionId2userId(sessionId)
    user <- resolveUser(userId)
  } yield {
    resolver.prolongTimeout(sessionId, sessionTimeoutInSeconds)
    user
  }

}
