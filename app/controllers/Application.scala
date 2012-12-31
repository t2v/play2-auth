package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import models._
import views._
import play.api.mvc._
import play.api.mvc.Results._
import jp.t2v.lab.play20.auth._
import play.api.Play._
import play.api.cache.Cache
import scala.reflect.classTag

object Application extends Controller with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def logout = Action { implicit request =>
    gotoLogoutSucceeded.flashing(
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
    Cache.set("hoge", "testtttttt")
    Ok(html.message.main(title))
  }

  def list = compositeAction(NormalUser) { user => implicit template => implicit request =>
    val title = Cache.getAs[String]("hoge").getOrElse("all messages")
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

  type Id = String

  type User = Account

  type Authority = Permission

  val idManifest = classTag[Id]

  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id) = Account.findById(id)

  def loginSucceeded(request: RequestHeader) = Redirect(routes.Message.main)

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

trait Base extends Controller with Auth with Pjax with AuthConfigImpl {

  def compositeAction(permission: Permission)(f: Account => Template => RequestHeader => PlainResult) =
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
  def pjax(user: Account)(implicit request: RequestHeader): Either[PlainResult, Template] = Right {
    if (request.headers.keys("X-PJAX")) html.pjaxTemplate.apply
    else html.fullTemplate.apply(user)
  }

}
