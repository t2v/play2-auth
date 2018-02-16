package controllers.stack

import play.api.libs.typedmap.TypedKey
import play.api.mvc.{ActionFunction, Controller, Request, Result}
import play.twirl.api.Html
import views.html

import scala.concurrent.Future

trait Pjax extends ActionFunction[Request, Request] {

  type Template = String => Html => Html

//  abstract override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
//    super.proceed(req) { req =>
//      val template: Template = if (req.headers.keys("X-Pjax")) html.pjaxTemplate.apply else fullTemplate(loggedIn(req))
//      f(req.set(TemplateKey, template))
//    }
//  }

//  implicit def template(implicit req: RequestWithAttributes[_]): Template = req.get(TemplateKey).get

//  protected val fullTemplate: User => Template

}
object Pjax {
  type Template = String => Html => Html
  val TemplateKey = TypedKey[Template]("pjaxTemplateKey")
}