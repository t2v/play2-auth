package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import models._
import views._
import play.api.mvc._
import play.api.mvc.Results._
import jp.t2v.lab.play20.auth._


object Application extends Controller with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def logout = Action { request =>
    gotoLogoutSucceeded(request).flashing(
      "success" -> "You've been logged out"
    )
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => gotoLoginSucceeded(user.get.id)
    )
  }

}
object Message extends Base {

  def main = compositeAction(NormalUser) { user => implicit template => implicit request =>
    val title = "message main"
    Ok(html.message.main(title))
  }

  def list = compositeAction(NormalUser) { user => implicit template => implicit request =>
    val title = "all messages"
    Ok(html.message.list(title))
  }

  def detail(id: Int) = compositeAction(NormalUser) { user => implicit template => implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id))
  }

  def write = compositeAction(Administrator) { user => implicit template => implicit request =>
    val title = "write message"
    Ok(html.message.write(title))
  }

}
trait AuthConfigImpl extends AuthConfig {

  type ID = String

  type USER = Account

  type AUTHORITY = Permission

  val idManifest = classManifest[ID]

  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: ID) = Account.findById(id)

  val loginSucceeded = Redirect(routes.Message.main)

  val logoutSucceeded = Redirect(routes.Application.login)

  val authenticationFailed = Redirect(routes.Application.login)

  val authorizationFailed = Forbidden("no permission")

  def authorize(user: USER, authority: AUTHORITY) = user.permission <= authority

}

trait Base extends Controller with Auth with Pjax with AuthConfigImpl {

  def compositeAction(permission: Permission)(f: Account => Template => Request[Any] => PlainResult) =
    Action { implicit request =>
      (for {
        user     <- authorized(permission).right
        template <- pjax(user).right
      } yield f(user)(template)(request)).merge
    }

}

trait Pjax {
  self: Controller =>

  type Template = String => Html => Html
  def pjax(user: Account)(implicit request: Request[Any]): Either[PlainResult, Template] = Right {
    if (request.headers.keys("X-PJAX")) html.pjaxTemplate.apply
    else html.fullTemplate.apply(user)
  }

}
