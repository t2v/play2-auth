package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.concurrent.{Future, ExecutionContext}

trait Login[Id, User, Authority] {

  def auth: AuthComponents[Id, User, Authority]

  def gotoLoginSucceeded(userId: Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = for {
    token <- auth.idContainer.startNewSession(userId, auth.authConfig.sessionTimeoutInSeconds)
    r     <- result
  } yield auth.tokenAccessor.put(token)(r)
}

trait Logout[Id, User, Authority] {

  def auth: AuthComponents[Id, User, Authority]

  def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    auth.tokenAccessor.extract(request) foreach auth.idContainer.remove
    result.map(auth.tokenAccessor.delete)
  }
}
