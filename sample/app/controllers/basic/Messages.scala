package controllers.basic

import jp.t2v.lab.play2.auth.{AuthComponents, BaseAuthActionBuilders}
import jp.t2v.lab.play2.auth.sample.{Account, Role}
import play.api.mvc.{AbstractController, ControllerComponents}
import views.html
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.Environment
import play.twirl.api.Html

import scala.concurrent.ExecutionContext


class Messages(val environment: Environment, val cc: ControllerComponents, val auth: AuthComponents[Int, Account, Role]) extends AbstractController(cc) with BaseAuthActionBuilders[Int, Account, Role] {

  implicit val ec: ExecutionContext = cc.executionContext

  protected implicit def template(implicit req: AuthRequest[_]): String => Html => Html = html.basic.fullTemplate(req.user)

  def main = AuthorizationAction(NormalUser).apply { implicit request =>
    val title = "message main"
    Ok(html.message.main(title))
  }

  def list = AuthorizationAction(NormalUser).apply { implicit request =>
    val title = "all messages"
    Ok(html.message.list(title))
  }

  def detail(id: Int) = AuthorizationAction(NormalUser).apply { implicit request =>
    val title = "messages detail "
    Ok(html.message.detail(title + id))
  }

  def write = AuthorizationAction(Administrator).apply { implicit request =>
    val title = "write message"
    Ok(html.message.write(title))
  }

}