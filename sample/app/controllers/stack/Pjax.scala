package controllers.stack

import jp.t2v.lab.play2.auth.AuthActionBuilders
import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import play.twirl.api.Html
import views.html

import scala.concurrent.Future

trait Pjax[Id, User, Authority] extends AuthActionBuilders[Id, User, Authority] {

  import Pjax._

  trait PjaxFunction extends ActionFunction[AuthRequest, AuthRequest] {

    def invokeBlock[A](request: AuthRequest[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {
      val template: Template = if (request.headers.keys("X-Pjax")) html.pjaxTemplate.apply else fullTemplate(request.user)
      block(AuthRequest(request.user, request.underlying.addAttr(TemplateKey, template)))
    }

  }

  implicit def template(implicit req: Request[_]): Template = req.attrs.get(TemplateKey).get

  protected val fullTemplate: User => Template

}
object Pjax {
  type Template = String => Html => Html
  val TemplateKey = TypedKey[Template]("pjaxTemplateKey")
}