package jp.t2v.lab.play2.auth.social.providers.vkontakte

/**
  * Created by Yuri Rastegaev on 19.03.2016.
  */

import java.net.URLEncoder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import jp.t2v.lab.play2.auth.social.core.{AccessTokenRetrievalFailedException, OAuth2Authenticator}
import play.api.Logger
import play.api.Play.current
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Results

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


class VkontakteAuthenticator extends OAuth2Authenticator {

  type AccessToken = String

  val providerName: String = "vkontakte"

  val accessTokenUrl = "https://oauth.vk.com/access_token"

  val authorizationUrl = "https://oauth.vk.com/authorize"

  val display = "page"

  val response_type = "code"

  lazy val clientId = current.configuration.getString("vkontakte.clientId").getOrElse(sys.error("vkontakte.clientId is missing"))

  lazy val clientSecret = current.configuration.getString("vkontakte.clientSecret").getOrElse(sys.error("vkontakte.clientSecret is missing"))

  lazy val callbackUrl = current.configuration.getString("vkontakte.callbackURL").getOrElse(sys.error("vkontakte.callbackURL is missing"))

  def retrieveAccessToken(code: String)(implicit ctx: ExecutionContext): Future[AccessToken] = {
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

  def getAuthorizationUrl(scope: String, state: String): String = {
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

  def parseAccessTokenResponse(response: WSResponse): String = {
    Logger(getClass).debug("Parsing access token response: " + response.body)
    try {
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      val responseMap = mapper.readValue[Map[String, Object]](response.body)

      var access_token = responseMap.get("access_token")
      var email = responseMap.get("email")
      var expires_in = responseMap.get("expires_in")
      var user_id = responseMap.get("user_id")

      VkontakteAuthenticator.email = email.get.toString
      VkontakteAuthenticator.expires_in = expires_in.get.toString
      VkontakteAuthenticator.user_id = user_id.get.toString

      access_token.get.toString
    } catch {
      case NonFatal(e) =>
        throw new AccessTokenRetrievalFailedException(s"Failed to retrieve access token. ${response.body}", e)
    }
  }

}

object VkontakteAuthenticator {
  var email = ""
  var expires_in = ""
  var user_id = ""

}
