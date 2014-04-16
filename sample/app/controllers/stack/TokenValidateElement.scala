package controller.stack

import jp.t2v.lab.play2.stackc.{RequestWithAttributes, StackableController}
import scala.concurrent.Future
import play.api.mvc.{Result, Request, Controller}
import play.api.data._
import play.api.data.Forms._

trait TokenValidateElement extends StackableController {
    self: Controller =>

  // snip token publishing

  private val tokenForm = Form("token" -> text)

  private def validateToken(request: Request[_]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest()(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  override def proceed[A](request: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    if (validateToken(request)) super.proceed(request)(f)
    else Future.successful(BadRequest)
  }

}

