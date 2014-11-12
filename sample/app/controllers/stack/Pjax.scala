package controllers.stack

import controllers.BaseAuthConfig
import jp.t2v.lab.play2.auth.AuthElement
import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.mvc.{Controller, Result}
import play.twirl.api.Html
import views.html

import scala.concurrent.Future

trait Pjax extends StackableController with AuthElement {
    self: Controller with BaseAuthConfig =>

  type Template = String => Html => Html

  case object TemplateKey extends RequestAttributeKey[Template]

  abstract override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    super.proceed(req) { req =>
      val template: Template = if (req.headers.keys("X-Pjax")) html.pjaxTemplate.apply else fullTemplate(loggedIn(req))
      f(req.set(TemplateKey, template))
    }
  }

  implicit def template(implicit req: RequestWithAttributes[_]): Template = req.get(TemplateKey).get

  protected val fullTemplate: User => Template

}
