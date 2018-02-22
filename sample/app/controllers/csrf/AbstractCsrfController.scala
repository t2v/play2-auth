package controllers.csrf

import jp.t2v.lab.play2.auth.{AuthComponents, BaseAuthActionBuilders}
import jp.t2v.lab.play2.auth.sample.{Account, Role}
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}

class AbstractCsrfController[Id, User, Authority](
  val environment: Environment,
  val cc: ControllerComponents,
  val auth: AuthComponents[Int, Account, Role]
) extends AbstractController(cc) with BaseAuthActionBuilders[Int, Account, Role] {





}
