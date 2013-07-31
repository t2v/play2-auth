package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import models._
import views._
import play.api.mvc._
import play.api.mvc.Results._
import jp.t2v.lab.play2.auth._
import play.api.Play._
import play.api.cache.Cache
import reflect.classTag
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}

object Application extends Controller with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
println("start login")
    Ok(html.login(loginForm))
  }

  def logout = Action { implicit request =>
    gotoLogoutSucceeded.flashing(
      "success" -> "You've been logged out"
    )
  }

  def authenticate = Action { implicit request =>
println("start act")
    loginForm.bindFromRequest.fold(
      formWithErrors => { println("form eror"); BadRequest(html.login(formWithErrors))},
      user => gotoLoginSucceeded(user.get.id)
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

trait OldMessages extends Controller with AsyncAuth with AuthConfigImpl {

  import scala.concurrent.ExecutionContext.Implicits.global

  def main = authorizedAction(NormalUser) { user => implicit request =>
    val title = "message main"
    Ok(html.message.main(title)(html.fullTemplate.apply(user)))
  }

  def list = authorizedAction(NormalUser) { user => implicit request =>
    val title = "all messages"
    Ok(html.message.list(title)(html.fullTemplate.apply(user)))
  }

  def detail(id: Int) = authorizedAction(NormalUser) { user => implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id)(html.fullTemplate.apply(user)))
  }

  def write = authorizedAction(Administrator) { user => implicit request =>
    val title = "write message"
    Ok(html.message.write(title)(html.fullTemplate.apply(user)))
  }

}
object OldMessages extends OldMessages


trait AuthConfigImpl extends AuthConfig {

  type Id = Int

  type User = Account

  type Authority = Permission

  val idTag = classTag[Id]

  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id) = Account.findById(id)

  def loginSucceeded(request: RequestHeader) = Redirect(routes.Messages.main)

  def logoutSucceeded(request: RequestHeader) = Redirect(routes.Application.login)

  def authenticationFailed(request: RequestHeader) = Redirect(routes.Application.login)

  def authorizationFailed(request: RequestHeader) = Forbidden("no permission")

  def authorize(user: User, authority: Authority) = (user.permission, authority) match {
    case (Administrator, _) => true
    case (NormalUser, NormalUser) => true
    case _ => false
  }

//  override lazy val idContainer = new CookieIdContainer[Id]

}

trait Pjax extends StackableController {
    self: Controller with AuthElement with AuthConfigImpl =>

  type Template = String => Html => Html

  case object TemplateKey extends RequestAttributeKey[Template]

  abstract override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Result): Result = {
    val template: Template = if (req.headers.keys("X-Pjax")) html.pjaxTemplate.apply else html.fullTemplate.apply(loggedIn(req))
    super.proceed(req.set(TemplateKey, template))(f)
  }

  implicit def template[A](implicit req: RequestWithAttributes[A]): Template = req.get(TemplateKey).get

}
