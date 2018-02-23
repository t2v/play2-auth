package controllers.stack

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.util.Random
import java.security.SecureRandom

import play.api.libs.typedmap.TypedKey

trait Csrf extends Results {

  import Csrf._

  private class GenerateTokenActionFunction(implicit ec: ExecutionContext) extends ActionFunction[Request, Request]  {

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      val newToken = generateToken
      block(request.addAttr(PreventingCsrfTokenKey, newToken)) map {
        _.withSession(PreventingCsrfTokenSessionKey -> newToken.value)
      }
    }

    private val random = new Random(new SecureRandom)
    private val table = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "^`~:/?,.{[}}|+_()*^%$#@!"

    private def generateToken: PreventingCsrfToken = PreventingCsrfToken {
      Iterator.continually(random.nextInt(table.size)).map(table).take(32).mkString
    }

    override protected def executionContext = ec
  }

  private class ValidateTokenActionFunction(implicit ec: ExecutionContext) extends ActionFilter[Request] {

    override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful {
      if (validateToken(request)) None
      else Some(BadRequest("Invalid preventing CSRF token"))
    }

    private val tokenForm: Form[PreventingCsrfToken] = Form {
      PreventingCsrfToken.FormKey -> text.transform(PreventingCsrfToken.apply, (p: PreventingCsrfToken) => p.value)
    }

    private def validateToken(implicit request: Request[_]): Boolean = (for {
      tokenInForm <- tokenForm.bindFromRequest().value
      tokenInSession <- request.session.get(PreventingCsrfTokenSessionKey)
    } yield tokenInForm.value == tokenInSession) getOrElse false

    override protected def executionContext: ExecutionContext = ec
  }

  def GenerateTokenActionFunction(implicit ec: ExecutionContext): ActionFunction[Request, Request] = {
    new GenerateTokenActionFunction()
  }

  def ValidateTokenActionFunction(implicit ec: ExecutionContext): ActionFunction[Request, Request] = {
    GenerateTokenActionFunction compose (new ValidateTokenActionFunction)
  }

  implicit def preventingCsrfToken(implicit request: Request[_]): PreventingCsrfToken = {
    request.attrs.get(PreventingCsrfTokenKey).get
  }

}
object Csrf {
  val PreventingCsrfTokenKey: TypedKey[PreventingCsrfToken] = TypedKey("preventingCsrfToken")
  private val PreventingCsrfTokenSessionKey = "preventingCsrfToken"
}

case class PreventingCsrfToken(value: String)

object PreventingCsrfToken {

  val FormKey = "preventingCsrfToken"

}
