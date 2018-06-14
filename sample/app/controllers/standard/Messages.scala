package controllers.standard

import javax.inject.Inject

import controllers.stack.Pjax
import jp.t2v.lab.play2.auth.AuthElement
import views.html
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

class Messages @Inject() (components: ControllerComponents, val environment: Environment) extends AbstractController(components) with Pjax with AuthElement with AuthConfigImpl {
  
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

  protected val fullTemplate: User => Template = html.standard.fullTemplate.apply

}
