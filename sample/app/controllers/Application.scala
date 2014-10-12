package controllers

import play.api.data._
import play.api.data.Forms._
import play.twirl.api.Html
import jp.t2v.lab.play2.auth.sample._
import views._
import play.api.mvc._
import play.api.mvc.Results._
import jp.t2v.lab.play2.auth._
import play.api.Play._
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import reflect.{ClassTag, classTag}
import scalikejdbc._

object Application extends Controller with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.map(_.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.login(formWithErrors))),
      user           => gotoLoginSucceeded(user.get.id)
    )
  }

}
trait Messages extends Controller with Pjax with AuthElement with AuthConfigImpl {

  def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "message main"
    Ok(html.message.main(title))
  }

  def list = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "all messages"
    Ok(html.message.list(title))
  }

  def detail(id: Int) = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id))
  }

  def write = StackAction(AuthorityKey -> Administrator) { implicit request =>
    val title = "write message"
    Ok(html.message.write(title))
  }

}
object Messages extends Messages

case class TransactionalRequest[A](dbSession: DBSession, request: Request[A]) extends WrappedRequest[A](request)
object TransactionalAction extends ActionBuilder[TransactionalRequest] {
  import scala.util.{Success, Failure}
  override def invokeBlock[A](request: Request[A], block: (TransactionalRequest[A]) => Future[Result]): Future[Result] = {
    val db = DB.connect()
    val tx = db.newTx
    tx.begin()
    val session = db.withinTxSession(tx)
    block(TransactionalRequest(session, request)).andThen {
      case Success(_) =>
        db.currentTx.commit()
        session.close()
      case Failure(_) =>
        db.currentTx.rollback()
        session.close()
    }
  }
}


trait Messages2 extends Controller with AuthActionBuilders with AuthConfigImpl {

  type AuthTxRequest[A] = GenericAuthRequest[A, TransactionalRequest]
  final def AuthorizationTxAction(authority: Authority): ActionBuilder[AuthTxRequest] = composeAuthorizationAction(TransactionalAction)(authority)

  def main = AuthorizationTxAction(NormalUser) { implicit request =>
    val title = "message main"
    println(Account.findAll()(request.underlying.dbSession))
    Ok(html.message.main(title)(html.fullTemplate.apply(request.user)))
  }

  def list = AuthorizationTxAction(NormalUser) { implicit request =>
    val title = "all messages"
    Ok(html.message.list(title)(html.fullTemplate.apply(request.user)))
  }

  def detail(id: Int) = AuthorizationTxAction(NormalUser) {implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id)(html.fullTemplate.apply(request.user)))
  }

  def write = AuthorizationTxAction(Administrator) { implicit request =>
    val title = "write message"
    Ok(html.message.write(title)(html.fullTemplate.apply(request.user)))
  }

}
object Messages2 extends Messages2


trait AuthConfigImpl extends AuthConfig {

  type Id = Int

  type User = Account

  type Authority = Permission

  val idTag: ClassTag[Id] = classTag[Id]

  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext) = Future.successful(Account.findById(id))

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Messages.main))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Application.login))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Redirect(routes.Application.login))

  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful(Forbidden("no permission"))

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext) = Future.successful((user.permission, authority) match {
    case (Administrator, _) => true
    case (NormalUser, NormalUser) => true
    case _ => false
  })

//  override lazy val idContainer = new CookieIdContainer[Id]

}

trait Pjax extends StackableController {
    self: Controller with AuthElement with AuthConfigImpl =>

  type Template = String => Html => Html

  case object TemplateKey extends RequestAttributeKey[Template]

  abstract override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    val template: Template = if (req.headers.keys("X-Pjax")) html.pjaxTemplate.apply else html.fullTemplate.apply(loggedIn(req))
    super.proceed(req.set(TemplateKey, template))(f)
  }

  implicit def template[A](implicit req: RequestWithAttributes[A]): Template = req.get(TemplateKey).get

}
