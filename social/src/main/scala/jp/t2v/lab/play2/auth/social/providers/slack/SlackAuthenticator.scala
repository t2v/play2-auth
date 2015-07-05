package jp.t2v.lab.play2.auth.social.providers.slack

import java.net.URLEncoder

import jp.t2v.lab.play2.auth.social.core.{ AccessTokenRetrievalFailedException, OAuth2Authenticator }
import play.api.Logger
import play.api.http.{ HeaderNames, MimeTypes }
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import play.api.mvc.Results

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class SlackAuthenticator extends OAuth2Authenticator {

  type AccessToken = String

  override val providerName: String = "slack"

  override val authorizationUrl: String = "https://slack.com/oauth/authorize"

  override val accessTokenUrl: String = "https://slack.com/api/oauth.access"

  override val clientId: String = current.configuration.getString("slack.clientId").getOrElse("slack.clientId is missing")

  override val clientSecret: String = current.configuration.getString("slack.clientSecret").getOrElse("slack.clientSecret is missing")

  override val callbackUrl: String = current.configuration.getString("slack.callbackURL").getOrElse("slack.callbackURL is missing")

  def getAuthorizationUrl(scope: String, state: String): String = {
    val encodedClientId = URLEncoder.encode(clientId, "utf-8")
    val encodedRedirectUri = URLEncoder.encode(callbackUrl, "utf-8")
    val encodedScope = URLEncoder.encode(scope, "utf-8")
    val encodedState = URLEncoder.encode(state, "utf-8")
    s"${authorizationUrl}?client_id=${encodedClientId}&redirect_uri=${encodedRedirectUri}&scope=${encodedScope}&state=${encodedState}"
  }

  override def retrieveAccessToken(code: String)(implicit ctx: ExecutionContext): Future[AccessToken] = {
    WS.url(accessTokenUrl)
      .withQueryString(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "redirect_uri" -> callbackUrl,
        "code" -> code)
      .withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(Results.EmptyContent())
      .map { response =>
        Logger(getClass).debug("Retrieving access token from provider API: " + response.body)
        parseAccessTokenResponse(response)
      }
  }

  override def parseAccessTokenResponse(response: WSResponse): AccessToken = {
    val j = response.json
    try {
      (j \ "access_token").as[String]
    } catch {
      case NonFatal(e) =>
        throw new AccessTokenRetrievalFailedException("Failed to retrieve access token", e)
    }
  }

}
