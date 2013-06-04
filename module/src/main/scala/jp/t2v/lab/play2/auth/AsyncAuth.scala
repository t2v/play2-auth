package jp.t2v.lab.play2.auth

import play.api.mvc._
import play.api.libs.iteratee.{Input, Done}
import concurrent.{ExecutionContext, Future}
import util.Success

trait AsyncAuth {
    self: AuthConfig with Controller =>

//  def authorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result)(implicit context: ExecutionContext): Action[(AnyContent, Future[User])] =
//    authorizedAction(BodyParsers.parse.anyContent, authority)(f)
//
//  def authorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result)(implicit context: ExecutionContext): Action[(A, Future[User])] = {
//
//
//    val parser = BodyParser {
//      req => authorized(authority)(req) match {
//        case Right(user)  => p.map((_, user))(req)
//        case Left(result) => Done(Left(result), Input.Empty)
//      }
//    }
//    Action(parser)(req => Async { req.body._2.map { u =>  f(u)(req.map(_._1)) } })
//  }
//
//  def optionalUserAction(f: Option[User] => Request[AnyContent] => Result)(implicit context: ExecutionContext): Action[AnyContent] =
//    optionalUserAction(BodyParsers.parse.anyContent)(f)
//
//  def optionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result)(implicit context: ExecutionContext): Action[A] =
//    Action(p)(req => Async { restoreUser(req, context).map(user => f(user)(req)) })

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, User]] = {
    restoreUser collect {
      case Some(user) => Right(user)
    } recover {
      case _ => Left(authenticationFailed(request))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[Option[User]] = {
    val userIdOpt = for {
      cookie <- request.cookies.get(cookieName)
      token  <- CookieUtil.verifyHmac(cookie)
      userId <- idContainer.get(token)
    } yield (token, userId)
    userIdOpt map { case (token, userId) =>
      resolveUserAsync(userId).andThen {
        case Success(_) => idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
      }
    } getOrElse {
      Future.successful(Option.empty)
    }
  }


}
