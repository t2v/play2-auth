package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.concurrent.{Future, ExecutionContext}

class Login[Id, User, Authority](
  authConfig: AuthConfig[Id, User, Authority],
  idContainer: AsyncIdContainer[Id],
  tokenAccessor: TokenAccessor
) {
  def gotoLoginSucceeded(userId: Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = for {
    token <- idContainer.startNewSession(userId, authConfig.sessionTimeoutInSeconds)
    r     <- result
  } yield tokenAccessor.put(token)(r)
}

class Logout[Id, User, Authority](
  authConfig: AuthConfig[Id, User, Authority],
  idContainer: AsyncIdContainer[Id],
  tokenAccessor: TokenAccessor
) {
  def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    tokenAccessor.extract(request) foreach idContainer.remove
    result.map(tokenAccessor.delete)
  }
}
