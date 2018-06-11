package controllers.builder

import javax.inject.Inject

import jp.t2v.lab.play2.auth.LoginLogout
import jp.t2v.lab.play2.auth.sample.Account
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ Action, Controller }
import views.html

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.AbstractController
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.Environment

class Sessions @Inject() (components: ControllerComponents, val environment: Environment) extends AbstractController(components) with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.builder.login(loginForm))
  }

  def logout = Action.async { implicit request =>
    val x = markLoggedOut()
    x(Redirect(routes.Sessions.login).flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.builder.login(formWithErrors))),
      user           => {val x = markLoggedIn(user.get.id); x(Redirect(routes.Messages.main))}
    )
  }

}