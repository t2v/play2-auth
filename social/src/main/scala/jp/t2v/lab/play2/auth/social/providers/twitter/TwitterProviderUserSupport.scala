package jp.t2v.lab.play2.auth.social.providers.twitter

import jp.t2v.lab.play2.auth.social.core.OAuthProviderUserSupport
import play.api.Logger
import play.api.Play.current
import play.api.libs.oauth.{ OAuthCalculator, RequestToken }
import play.api.libs.ws.WSResponse

import scala.concurrent.{ ExecutionContext, Future }
import play.api.libs.ws.WSClient

trait TwitterProviderUserSupport extends OAuthProviderUserSupport {
  self: TwitterController =>
    
  val ws: WSClient

  type ProviderUser = TwitterUser
  
  private def splitToken(accessToken: AccessToken):(String, String) = {
    accessToken.split("|-sep-|").toList match {
      case a::b::Nil => (a,b)
      case _ => throw new IllegalArgumentException
    }
  }

  private def readProviderUser(accessToken: AccessToken, response: WSResponse): ProviderUser = {
    val j = response.json
    val (token, secret) = splitToken(accessToken)
    TwitterUser(
      (j \ "id").as[Long],
      (j \ "screen_name").as[String],
      (j \ "name").as[String],
      (j \ "description").as[String],
      (j \ "profile_image_url").as[String],
      token,
      secret
    )
  }

  def retrieveProviderUser(accessToken: AccessToken)(implicit ctx: ExecutionContext): Future[ProviderUser] = {
    val (token, secret) = splitToken(accessToken)
    for {
      response <- ws.url("https://api.twitter.com/1.1/account/verify_credentials.json")
        .sign(OAuthCalculator(authenticator.consumerKey, RequestToken(token, secret))).get()
    } yield {
      Logger(getClass).debug("Retrieving user info from Twitter API: " + response.body)
      readProviderUser(accessToken, response)
    }
  }

}
