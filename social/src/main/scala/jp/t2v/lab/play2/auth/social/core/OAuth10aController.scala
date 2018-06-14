package jp.t2v.lab.play2.auth.social.core

import jp.t2v.lab.play2.auth.{ AuthConfig, OptionalAuthElement }
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.oauth._
import play.api.mvc._
import jp.t2v.lab.play2.stackc.StackableController


import scala.concurrent.Future
import jp.t2v.lab.play2.auth.LoginLogout

trait OAuth10aController extends AbstractController with OAuthController {
  self: OptionalAuthElement with AuthConfig with LoginLogout =>

  protected val authenticator: OAuth10aAuthenticator

  protected val RequestTokenSecretKey = "play.social.requestTokenSecret"

  def login = AsyncStack(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    implicit val ec = StackActionExecutionContext
    loggedIn match {
      case Some(u) => loginSucceeded(request)
      case None => authenticator.oauth.retrieveRequestToken(authenticator.callbackURL) match {
        case Right(token) =>
          Future.successful(
            Redirect(authenticator.oauth.redirectUrl(token.token)).withSession(
              request.session + (RequestTokenSecretKey -> token.secret)
            )
          )
        case Left(e) =>
          Logger(getClass).error(e.getMessage)
          Future.successful(InternalServerError(e.getMessage))
      }
    }
  }

  def link = StackAction(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    loggedIn match {
      case Some(_) =>
        authenticator.oauth.retrieveRequestToken(authenticator.callbackURL) match {
          case Right(token) =>
            Redirect(authenticator.oauth.redirectUrl(token.token)).withSession(
              request.session + (RequestTokenSecretKey -> token.secret)
            )
          case Left(e) =>
            Logger(getClass).error(e.getMessage)
            InternalServerError(e.getMessage)
        }
      case None =>
        Unauthorized
    }
  }

  def authorize = AsyncStack(ExecutionContextKey -> OAuthExecutionContext) { implicit request =>
    implicit val ec = StackActionExecutionContext
    val form = Form(
      tuple(
        "oauth_token"    -> optional(nonEmptyText),
        "oauth_verifier" -> optional(nonEmptyText),
        "denied"         -> optional(nonEmptyText)
      )
    )

    form.bindFromRequest.fold({
      formWithError => Future.successful(BadRequest)
    }, {
      case (Some(oauthToken), Some(oauthVerifier), None) =>
        val action: AccessToken => Future[Result] = loggedIn match {
          case Some(consumerUser) => onOAuthLinkSucceeded(_, consumerUser)
          case None               => onOAuthLoginSucceeded
        }
        (for {
          tokenSecret  <- request.session.get(RequestTokenSecretKey)
          requestToken = RequestToken(oauthToken, tokenSecret)
          token        <- authenticator.oauth.retrieveAccessToken(requestToken, oauthVerifier).right.toOption
        } yield {
          action(requestTokenToAccessToken(token))
        }).getOrElse(Future.successful(BadRequest))

      case (None, None, Some(denied)) => Future.successful(Unauthorized)
      case _ => Future.successful(BadRequest)
    }).map(_.removingFromSession(RequestTokenSecretKey))

  }

  def requestTokenToAccessToken(requestToken: RequestToken): AccessToken

}

