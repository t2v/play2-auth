package jp.t2v.lab.play2.auth.social.core

import java.util.UUID

import jp.t2v.lab.play2.auth.{ AuthConfig, OptionalAuthElement }
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.control.NonFatal

trait OAuth2Controller extends Controller with OAuthController { self: OptionalAuthElement with AuthConfig =>

  protected val authenticator: OAuth2Authenticator

  protected val OAuth2StateKey = "play.auth.social.oauth2.state"

  // TODO scope is optional in some services
  // TODO some services have more optional parameter
  def login(scope: String) = AsyncStack(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    implicit val ec = StackActionExecutionContext
    loggedIn match {
      case Some(u) =>
        loginSucceeded(request)
      case None =>
        // should be more random ?
        val state = UUID.randomUUID().toString
        Future.successful(
          Redirect(authenticator.getAuthorizationUrl(scope, state)).withSession(
            request.session + (OAuth2StateKey -> state)
          )
        )
    }
  }

  // TODO scope is optional in some services
  // TODO some services have more optional parameter
  def link(scope: String) = StackAction(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    loggedIn match {
      case Some(u) =>
        // TODO should it be more random ?
        val state = UUID.randomUUID().toString
        Redirect(authenticator.getAuthorizationUrl(scope, state)).withSession(
          request.session + (OAuth2StateKey -> state)
        )
      case None =>
        Unauthorized
    }
  }

  def authorize = AsyncStack(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    implicit val ec = StackActionExecutionContext
    val form = Form(
      tuple(
        "code"  -> nonEmptyText,
        "state" -> nonEmptyText.verifying(s => request.session.get(OAuth2StateKey).exists(_ == s))
      )
    )

    form.bindFromRequest.fold({
      formWithError => Future.successful(BadRequest)
    }, {
      case (code, _) =>
        val action: AccessToken => Future[Result] = loggedIn match {
          case Some(consumerUser) => onOAuthLinkSucceeded(_, consumerUser)
          case None               => onOAuthLoginSucceeded
        }

        (for {
          token  <- authenticator.retrieveAccessToken(code)
          result <- action(token)
        } yield result).recover {
          case NonFatal(e) =>
            Logger(getClass).error(e.getMessage, e)
            InternalServerError
        }
    }).map(_.removingFromSession(OAuth2StateKey))
  }

}
