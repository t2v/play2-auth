package controllers.builder

import jp.t2v.lab.play2.auth.{AuthComponents, LoginLogout}
import jp.t2v.lab.play2.auth.sample.{Account, Role}
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html

import scala.concurrent.{ExecutionContext, Future}

class Sessions (val environment: Environment, val cc: ControllerComponents, val auth: AuthComponents[Int, Account, Role]) extends AbstractController(cc) with LoginLogout[Int, Account, Role] {

  implicit val ec: ExecutionContext = cc.executionContext

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.builder.login(loginForm))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded(Future.successful(Redirect(routes.Sessions.login).flashing("success" -> "You've been logged out")))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.csrf.login(formWithErrors))),
      user           => gotoLoginSucceeded(user.get.id, Future.successful(Redirect(routes.Messages.main)))
    )
  }

}