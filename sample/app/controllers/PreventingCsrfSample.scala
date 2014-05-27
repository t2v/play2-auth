package controllers

import controllers.stack.TokenValidateElement
import play.api.mvc.Controller
import jp.t2v.lab.play2.auth.AuthElement
import jp.t2v.lab.play2.auth.sample.NormalUser
import play.api.data.Form
import play.api.data.Forms._

trait PreventingCsrfSample extends Controller with TokenValidateElement with AuthElement with AuthConfigImpl {

  def formWithToken = StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> ()) { implicit req =>
    Ok(views.html.PreventingCsrfSample.formWithToken())
  }

  def formWithoutToken = StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> ()) { implicit req =>
    Ok(views.html.PreventingCsrfSample.formWithoutToken())
  }

  val form = Form { single("message" -> text) }

  def submitTarget = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    form.bindFromRequest.fold(
      _       => throw new Exception,
      message => Ok(message).as("text/plain")
    )
  }

}
object PreventingCsrfSample extends PreventingCsrfSample
