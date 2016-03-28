package jp.t2v.lab.play2.auth.social.providers.vkontakte

import java.net.URLEncoder

import jp.t2v.lab.play2.auth.social.core.{AccessTokenRetrievalFailedException, OAuth2Authenticator}
import play.api.Logger
import play.api.Play.current
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Results

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


class VkontakteAuthenticator extends OAuth2Authenticator {

  type AccessToken = VkontakteToken

  val providerName: String = "vkontakte"

  val accessTokenUrl = "https://oauth.vk.com/access_token"

  val authorizationUrl = "https://oauth.vk.com/authorize"

  val display = "page"

  val response_type = "code"

  lazy val clientId = current.configuration.getString("vkontakte.clientId").getOrElse(sys.error("vkontakte.clientId is missing"))

  lazy val clientSecret = current.configuration.getString("vkontakte.clientSecret").getOrElse(sys.error("vkontakte.clientSecret is missing"))

  lazy val callbackUrl = current.configuration.getString("vkontakte.callbackURL").getOrElse(sys.error("vkontakte.callbackURL is missing"))

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

  override def getAuthorizationUrl(scope: String, state: String): String = {
    val encodedClientId = URLEncoder.encode(clientId, "utf-8")
    val encodedRedirectUri = URLEncoder.encode(callbackUrl, "utf-8")
    val encodedScope = URLEncoder.encode(scope, "utf-8")
    val encodedState = URLEncoder.encode(state, "utf-8")
    s"${authorizationUrl}?client_id=${encodedClientId}" +
      s"&redirect_uri=${encodedRedirectUri}" +
      s"&display=${display}" +
      s"&response_type=${response_type}" +
      s"&scope=${encodedScope}" +
      s"&state=${encodedState}"
  }

  override def parseAccessTokenResponse(response: WSResponse): VkontakteToken = {
    Logger(getClass).debug("Parsing access token response: " + response.body)
    try {
      val jsonValue = Json.parse(response.body.toString)
      var access_token = jsonValue \ "access_token"
      var email = jsonValue \ "email"
      var expires_in = jsonValue \ "expires_in"
      var user_id = jsonValue \ "user_id"
      new VkontakteToken(access_token.get.toString(), email.get.toString(), expires_in.get.as[Long], user_id.get.as[Long])
    } catch {
      case NonFatal(e) =>
        throw new AccessTokenRetrievalFailedException(s"Failed to retrieve access token. ${response.body}", e)
    }
  }

}

