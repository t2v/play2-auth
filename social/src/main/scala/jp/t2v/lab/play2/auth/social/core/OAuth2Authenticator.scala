package jp.t2v.lab.play2.auth.social.core

import play.api.libs.ws.WSResponse

import scala.concurrent.{ ExecutionContext, Future }

trait OAuth2Authenticator extends OAuthAuthenticator {

  val providerName: String

  val callbackUrl: String

  val accessTokenUrl: String

  val authorizationUrl: String

  val clientId: String

  val clientSecret: String

  def retrieveAccessToken(code: String)(implicit ctx: ExecutionContext): Future[AccessToken]

  def getAuthorizationUrl(scope: String, state: String): String

  def parseAccessTokenResponse(response: WSResponse): String

}
