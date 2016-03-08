package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth {
    self: AuthConfig with Controller =>

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, (User, ResultUpdater)]] = {
    restoreUser collect {
      case (Some(user), resultUpdater) => Right(user -> resultUpdater)
    } recoverWith {
      case _ => authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right((user, resultUpdater)) => authorize(user, authority) collect {
        case true => Right(user -> resultUpdater)
      } recoverWith {
        case _ => authorizationFailed(request, user, Some(authority)).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[(Option[User], ResultUpdater)] = {
    (for {
      token  <- extractToken(request)
    } yield for {
      Some(userId) <- idContainer.get(token)
      Some(user)   <- resolveUser(userId)
      _            <- idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
    } yield {
      Option(user) -> tokenAccessor.put(token) _
    }) getOrElse {
      Future.successful(Option.empty -> identity)
    }
  }

  private[auth] def extractToken(request: RequestHeader): Option[AuthenticityToken] = {
    if (play.api.Play.maybeApplication.forall(app => play.api.Play.isTest(app))) {
      request.headers.get("PLAY2_AUTH_TEST_TOKEN") orElse tokenAccessor.extract(request)
    } else {
      tokenAccessor.extract(request)
    }
  }

}
