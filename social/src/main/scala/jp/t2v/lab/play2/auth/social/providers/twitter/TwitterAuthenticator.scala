package jp.t2v.lab.play2.auth.social.providers.twitter

import jp.t2v.lab.play2.auth.social.core.OAuth10aAuthenticator
import play.api.Play.current
import play.api.libs.oauth.ConsumerKey

class TwitterAuthenticator extends OAuth10aAuthenticator {

  type AccessToken = String//TwitterOAuth10aAccessToken

  val providerName: String = "twitter"

  val requestTokenURL = "https://api.twitter.com/oauth/request_token"

  val accessTokenURL = "https://api.twitter.com/oauth/access_token"

  val authorizationURL = "https://api.twitter.com/oauth/authorize"

  lazy val consumerKey = ConsumerKey(
    current.configuration.getString("twitter.consumerKey").getOrElse(sys.error("twitter.consumerKey is missing")),
    current.configuration.getString("twitter.consumerSecret").getOrElse(sys.error("twitter.consumerSecret is missing"))
  )

  lazy val callbackURL = current.configuration.getString("twitter.callbackURL").getOrElse(
    sys.error("twitter.callbackURL is missing")
  )

}

