package controllers.stack

import scala.concurrent.Future
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.util.Random
import java.security.SecureRandom

import play.api.libs.typedmap.TypedKey

trait TokenValidateActionFunction[R[+_] <: Request[_]] extends ActionFilter[R] {

  import TokenValidateActionFunction._

  protected def filter[A](request: R[A]): Future[Option[Result]] = {
//    if (isIgnoreTokenValidation(request) || validateToken(request)) {
//      implicit val ctx = StackActionExecutionContext(request)
//      val newToken = generateToken
//      super.proceed(request.set(PreventingCsrfTokenKey, newToken))(f) map {
//        _.withSession(PreventingCsrfTokenSessionKey -> newToken.value)
//      }
//    } else {
//      Future.successful(BadRequest("Invalid preventing CSRF token"))
//    }

    ???
  }

  private val tokenForm = Form(PreventingCsrfToken.FormKey -> text)

  private val random = new Random(new SecureRandom)
  private val table = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "^`~:/?,.{[}}|+_()*^%$#@!"

  private def generateToken: PreventingCsrfToken = PreventingCsrfToken {
    Iterator.continually(random.nextInt(table.size)).map(table).take(32).mkString
  }

  private def validateToken(implicit request: R[_]): Boolean = (for {
    tokenInForm    <- tokenForm.bindFromRequest().value
    tokenInSession <- request.session.get(PreventingCsrfTokenSessionKey)
  } yield tokenInForm == tokenInSession) getOrElse false

  private def isIgnoreTokenValidation(implicit request: R[_]): Boolean =
    request.attrs.get(IgnoreTokenValidation).exists(identity)

}
object TokenValidateActionFunction {
  val PreventingCsrfTokenKey: TypedKey[String] = TypedKey("preventingCsrfToken")
  val IgnoreTokenValidation: TypedKey[Boolean] = TypedKey("ignoreTokenValidation")
  private val PreventingCsrfTokenSessionKey = "preventingCsrfToken"
}

trait TokenValidateElement {
    self: Controller =>


//
//  case object PreventingCsrfTokenKey extends RequestAttributeKey[PreventingCsrfToken]
//  case object IgnoreTokenValidation extends RequestAttributeKey[Boolean]
//
//  private def validateToken(request: Request[_]): Boolean = (for {
//    tokenInForm    <- tokenForm.bindFromRequest()(request).value
//    tokenInSession <- request.session.get(PreventingCsrfTokenSessionKey)
//  } yield tokenInForm == tokenInSession) getOrElse false
//
//  override def proceed[A](request: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
//    if (isIgnoreTokenValidation(request) || validateToken(request)) {
//      implicit val ctx = StackActionExecutionContext(request)
//      val newToken = generateToken
//      super.proceed(request.set(PreventingCsrfTokenKey, newToken))(f) map {
//        _.withSession(PreventingCsrfTokenSessionKey -> newToken.value)
//      }
//    } else {
//      Future.successful(BadRequest("Invalid preventing CSRF token"))
//    }
//  }
//
//  implicit def isIgnoreTokenValidation(implicit request: RequestWithAttributes[_]): Boolean =
//    request.get(IgnoreTokenValidation).exists(identity)
//
//  implicit def preventingCsrfToken(implicit request: RequestWithAttributes[_]): PreventingCsrfToken =
//    request.get(PreventingCsrfTokenKey).get
//

}

case class PreventingCsrfToken(value: String)

object PreventingCsrfToken {

  val FormKey = "preventingCsrfToken"

}
