package controllers.stack

import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import scala.concurrent.Future
import play.api.mvc.{Result, Request, Controller}
import play.api.data._
import play.api.data.Forms._
import scala.util.Random
import java.security.SecureRandom

trait TokenValidateElement extends StackableController {
    self: Controller =>

  private val PreventingCsrfTokenSessionKey = "preventingCsrfToken"

  private val tokenForm = Form(PreventingCsrfToken.FormKey -> text)

  private val random = new Random(new SecureRandom)
  private val table = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "^`~:/?,.{[}}|+_()*^%$#@!"

  private def generateToken: PreventingCsrfToken = PreventingCsrfToken {
    Iterator.continually(random.nextInt(table.size)).map(table).take(32).mkString
  }

  case object PreventingCsrfTokenKey extends RequestAttributeKey[PreventingCsrfToken]
  case object IgnoreTokenValidation extends RequestAttributeKey[Boolean]

  private def validateToken(request: Request[_]): Boolean = (for {
    tokenInForm    <- tokenForm.bindFromRequest()(request).value
    tokenInSession <- request.session.get(PreventingCsrfTokenSessionKey)
  } yield tokenInForm == tokenInSession) getOrElse false

  override def proceed[A](request: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    if (isIgnoreTokenValidation(request) || validateToken(request)) {
      implicit val ctx = StackActionExecutionContext(request)
      val newToken = generateToken
      super.proceed(request.set(PreventingCsrfTokenKey, newToken))(f) map {
        _.withSession(PreventingCsrfTokenSessionKey -> newToken.value)
      }
    } else {
      Future.successful(BadRequest("Invalid preventing CSRF token"))
    }
  }

  implicit def isIgnoreTokenValidation(implicit request: RequestWithAttributes[_]): Boolean =
    request.get(IgnoreTokenValidation).exists(identity)

  implicit def preventingCsrfToken(implicit request: RequestWithAttributes[_]): PreventingCsrfToken =
    request.get(PreventingCsrfTokenKey).get


}

case class PreventingCsrfToken(value: String)

object PreventingCsrfToken {

  val FormKey = "preventingCsrfToken"

}
