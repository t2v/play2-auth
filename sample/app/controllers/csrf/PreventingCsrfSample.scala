package controllers.csrf

import jp.t2v.lab.play2.auth.AuthComponents
import jp.t2v.lab.play2.auth.sample.Role.NormalUser
import jp.t2v.lab.play2.auth.sample.{Account, Role}
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class PreventingCsrfSample(ev: Environment, cc: ControllerComponents, auth: AuthComponents[Int, Account, Role])(implicit ec: ExecutionContext) extends AbstractCsrfController(ev, cc, auth) {

  def formWithToken = GenerateTokenAction(NormalUser).apply { implicit req =>
    Ok(views.html.csrf.formWithToken())
  }

  def formWithoutToken = GenerateTokenAction(NormalUser).apply { implicit req =>
    Ok(views.html.csrf.formWithoutToken())
  }

  val form = Form { single("message" -> text) }

  def submitTarget = ValidateTokenAction(NormalUser).apply { implicit req =>
    form.bindFromRequest.fold(
      _       => throw new Exception,
      message => Ok(message).as("text/plain")
    )
  }

}
