package controllers.builder

import javax.inject.Inject

import jp.t2v.lab.play2.auth.AuthActionBuilders
import jp.t2v.lab.play2.auth.sample.Account
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.api.mvc._
import play.twirl.api.Html
import scalikejdbc.{ DB, DBSession }
import views.html

import scala.concurrent.Future
import javax.inject.Inject
import play.api.Environment
import scala.concurrent.ExecutionContext

class TransactionalRequest[+A](val dbSession: DBSession, request: Request[A]) extends WrappedRequest[A](request)

class TransactionalAction @Inject() (val parser: BodyParsers.Default, val executionContext: ExecutionContext) extends ActionBuilder[TransactionalRequest, AnyContent] {
  override def invokeBlock[A](request: Request[A], block: (TransactionalRequest[A]) => Future[Result]): Future[Result] = {
    import scalikejdbc.TxBoundary.Future._
    implicit val ctx = executionContext
    DB.localTx { session =>
      block(new TransactionalRequest(session, request))
    }
  }
}

class Messages @Inject() (components: ControllerComponents, val environment: Environment, parser: BodyParsers.Default, transactionalAction: TransactionalAction) extends AbstractController(components) with AuthActionBuilders with AuthConfigImpl {

  type AuthTxRequest[+A] = GenericAuthRequest[A, TransactionalRequest]
  final def AuthorizationTxAction(authority: Authority): ActionBuilder[AuthTxRequest, AnyContent] = composeAuthorizationAction(transactionalAction)(authority)

  class PjaxAuthRequest[+A](val template: String => Html => Html, val authRequest: AuthTxRequest[A]) extends WrappedRequest[A](authRequest)
  class PjaxRefiner(val executionContext: ExecutionContext) extends ActionTransformer[AuthTxRequest, PjaxAuthRequest] {
    override protected def transform[A](request: AuthTxRequest[A]): Future[PjaxAuthRequest[A]] = {
      val template: String => Html => Html = if (request.headers.keys("X-Pjax")) html.pjaxTemplate.apply else html.builder.fullTemplate.apply(request.user)
      Future.successful(new PjaxAuthRequest(template, request))
    }
  }

  def MyAction(authority: Authority): ActionBuilder[PjaxAuthRequest, AnyContent] = AuthorizationTxAction(authority) andThen (new PjaxRefiner(components.executionContext))

  def main = MyAction(NormalUser) { implicit request =>
    val title = "message main"
    println(Account.findAll()(request.authRequest.underlying.dbSession))
    Ok(html.message.main(title)(request.template))
  }

  def list = MyAction(NormalUser) { implicit request =>
    val title = "all messages"
    Ok(html.message.list(title)(request.template))
  }

  def detail(id: Int) = MyAction(NormalUser) {implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id)(request.template))
  }

  def write = MyAction(Administrator) { implicit request =>
    val title = "write message"
    Ok(html.message.write(title)(request.template))
  }

}
