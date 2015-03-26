package jp.t2v.lab.play2.auth.social.core

import play.api.libs.oauth._

trait OAuth10aAuthenticator extends OAuthAuthenticator {

  val callbackURL: String

  val requestTokenURL: String

  val accessTokenURL: String

  val authorizationURL: String

  val consumerKey: ConsumerKey

  lazy val serviceInfo: ServiceInfo = ServiceInfo(
    requestTokenURL,
    accessTokenURL,
    authorizationURL,
    consumerKey
  )

  lazy val oauth = OAuth(serviceInfo, use10a = true)

}

