package controllers.csrf

import javax.inject.Inject

import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, Controller, ControllerComponents}

class PreventingCsrfSample @Inject() (val environment: Environment, cc: ControllerComponents) extends AbstractController(cc) {

  def formWithToken = TODO
//    StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> true) { implicit req =>
//    Ok(views.html.csrf.formWithToken())
//  }

  def formWithoutToken = TODO
//    StackAction(AuthorityKey -> NormalUser, IgnoreTokenValidation -> true) { implicit req =>
//    Ok(views.html.csrf.formWithoutToken())
//  }

  val form = Form { single("message" -> text) }

  def submitTarget = TODO
//    StackAction(AuthorityKey -> NormalUser) { implicit req =>
//    form.bindFromRequest.fold(
//      _       => throw new Exception,
//      message => Ok(message).as("text/plain")
//    )
//  }

}
