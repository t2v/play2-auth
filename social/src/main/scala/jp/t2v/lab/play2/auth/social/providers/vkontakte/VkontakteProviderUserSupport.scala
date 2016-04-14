package jp.t2v.lab.play2.auth.social.providers.vkontakte

import jp.t2v.lab.play2.auth.social.core.OAuthProviderUserSupport
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.JsString
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait VkontakteProviderUserSupport extends OAuthProviderUserSupport {
  self: VkontakteController =>

  val USERS_GET_URL: String = "https://api.vk.com/method/users.get"

  type ProviderUser = VkontakteUser

  type AccessToken = VkontakteToken

  private def readProviderUser(vkontakteToken: VkontakteToken, response: WSResponse): ProviderUser = {
    val j = response.json
    val resp = j \ "response"
    val uid = (resp.get \\ "uid").head.toString()
    val first_name = (resp.get \\ "first_name").head.asInstanceOf[JsString].value
    val last_name = (resp.get \\ "last_name").head.asInstanceOf[JsString].value
    val photo_50 = (resp.get \\ "photo_50").head.asInstanceOf[JsString].value

    VkontakteUser(
      uid,
      first_name,
      vkontakteToken.email,
      photo_50,
      vkontakteToken.access_token
    )
  }


  def retrieveProviderUser(vkontakteToken: VkontakteToken, userId: String)(implicit ctx: ExecutionContext): Future[ProviderUser] = {
    for {
      response <- WS.url(USERS_GET_URL)
        .withQueryString("user_id" -> userId.toString, "access_token" -> vkontakteToken.access_token, "fields" -> "photo_50")
        .get()
    } yield {
      Logger(getClass).debug("Retrieving user info from provider API: " + response.body)
      readProviderUser(vkontakteToken, response)
    }
  }

  def retrieveProviderUser(vkontakteToken: VkontakteToken)(implicit ctx: ExecutionContext): Future[ProviderUser] = {
    retrieveProviderUser(vkontakteToken, vkontakteToken.user_id.toString)
  }

}
