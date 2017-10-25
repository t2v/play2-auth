package jp.t2v.lab.play2.auth

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.RequestHeader

trait AsyncIdContainer[Id] {

  def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken]

  def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit]
  def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]]

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit]

}
object AsyncIdContainer {
}