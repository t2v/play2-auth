package jp.t2v.lab.play2.auth

import play.api.{Environment, Mode}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class AsyncAuth[Id, User, Authority](
  authConfig: AuthConfig[Id, User, Authority],
  idContainer: AsyncIdContainer[Id],
  tokenAccessor: TokenAccessor,
  env: Environment
) {

  def authorized(authority: Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, (User, ResultUpdater)]] = {
    restoreUser collect {
      case (Some(user), resultUpdater) => Right(user -> resultUpdater)
    } recoverWith {
      case _ => authConfig.authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right((user, resultUpdater)) => authConfig.authorize(user, authority) collect {
        case true => Right(user -> resultUpdater)
      } recoverWith {
        case _ => authConfig.authorizationFailed(request, user, Some(authority)).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[(Option[User], ResultUpdater)] = {
    (for {
      token  <- extractToken(request)
    } yield for {
      Some(userId) <- idContainer.get(token)
      Some(user)   <- authConfig.resolveUser(userId)
      _            <- idContainer.prolongTimeout(token, authConfig.sessionTimeoutInSeconds)
    } yield {
      Option(user) -> tokenAccessor.put(token) _
    }) getOrElse {
      Future.successful(Option.empty -> identity)
    }
  }

  private[auth] def extractToken(request: RequestHeader): Option[AuthenticityToken] = {
    if (env.mode == Mode.Test) {
      request.headers.get("PLAY2_AUTH_TEST_TOKEN") orElse tokenAccessor.extract(request)
    } else {
      tokenAccessor.extract(request)
    }
  }

}
