package controllers.basic

import javax.inject.Inject

import controllers.stack.Pjax
import jp.t2v.lab.play2.auth.AuthElement
import play.api.mvc.Controller
import views.html
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.twirl.api.Html
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import javax.inject.Inject

class Messages @Inject() (components: ControllerComponents) extends AbstractController(components) with AuthElement with AuthConfigImpl {

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

  protected implicit def template(implicit user: User): String => Html => Html = html.basic.fullTemplate(user)

}
