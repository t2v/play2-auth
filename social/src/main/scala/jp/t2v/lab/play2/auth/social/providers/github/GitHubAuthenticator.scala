package jp.t2v.lab.play2.auth.social.providers.github

import java.net.URLEncoder

import jp.t2v.lab.play2.auth.social.core.{ AccessTokenRetrievalFailedException, OAuth2Authenticator }
import play.api.Logger
import play.api.Play.current
import play.api.http.{ HeaderNames, MimeTypes }
import play.api.libs.ws.WSResponse
import play.api.mvc.Results

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import play.api.libs.ws.WSClient
import play.api.libs.ws.EmptyBody

class GitHubAuthenticator(ws: WSClient) extends OAuth2Authenticator {

  type AccessToken = String

  val providerName: String = "github"

  val accessTokenUrl = "https://github.com/login/oauth/access_token"

  val authorizationUrl = "https://github.com/login/oauth/authorize"

  lazy val clientId = current.configuration.getString("github.clientId").getOrElse(sys.error("github.clientId is missing"))

  lazy val clientSecret = current.configuration.getString("github.clientSecret").getOrElse(sys.error("github.clientSecret is missing"))

  lazy val callbackUrl = current.configuration.getString("github.callbackURL").getOrElse(sys.error("github.callbackURL is missing"))

  def retrieveAccessToken(code: String)(implicit ctx: ExecutionContext): Future[AccessToken] = {
    ws.url(accessTokenUrl)
      .withQueryStringParameters(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "code" -> code)
      .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(EmptyBody)
      .map { response =>
        Logger(getClass).debug("Retrieving access token from provider API: " + response.body)
        parseAccessTokenResponse(response)
      }
  }

  def getAuthorizationUrl(scope: String, state: String): String = {
    val encodedClientId = URLEncoder.encode(clientId, "utf-8")
    val encodedRedirectUri = URLEncoder.encode(callbackUrl, "utf-8")
    val encodedScope = URLEncoder.encode(scope, "utf-8")
    val encodedState = URLEncoder.encode(state, "utf-8")
    s"${authorizationUrl}?client_id=${encodedClientId}&redirect_uri=${encodedRedirectUri}&scope=${encodedScope}&state=${encodedState}"
  }

  def parseAccessTokenResponse(response: WSResponse): String = {
    Logger(getClass).debug("Parsing access token response: " + response.body)
    try {
      (response.json \ "access_token").as[String]
    } catch {
      case NonFatal(e) =>
        throw new AccessTokenRetrievalFailedException(s"Failed to parse access token: ${response.body}", e)
    }
  }

}

