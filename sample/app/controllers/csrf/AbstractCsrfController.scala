package controllers.csrf

import controllers.stack.Csrf
import jp.t2v.lab.play2.auth.{AuthComponents, BaseAuthActionBuilders}
import jp.t2v.lab.play2.auth.sample.{Account, Role}
import play.api.Environment
import play.api.mvc.{AbstractController, ActionBuilder, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class AbstractCsrfController[Id, User, Authority](
  val environment: Environment,
  val cc: ControllerComponents,
  val auth: AuthComponents[Int, Account, Role]
) extends AbstractController(cc) with BaseAuthActionBuilders[Int, Account, Role] with Csrf {

  final def GenerateTokenAction(authority: Role)(implicit ec: ExecutionContext): ActionBuilder[AuthRequest, AnyContent] = {
    composeAuthorizationAction(Action.andThen(GenerateTokenActionFunction))(authority)
  }

  final def ValidateTokenAction(authority: Role)(implicit ec: ExecutionContext): ActionBuilder[AuthRequest, AnyContent] = {
    composeAuthorizationAction(Action.andThen(ValidateTokenActionFunction))(authority)
  }

}
