package jp.t2v.lab.play2.auth.social.providers.twitter

import jp.t2v.lab.play2.auth.social.core.OAuth10aController
import jp.t2v.lab.play2.auth.{ AuthConfig, Login, OptionalAuthElement }
import play.api.libs.oauth.RequestToken
import jp.t2v.lab.play2.auth.LoginLogout

trait TwitterController extends OAuth10aController
    with AuthConfig
    with OptionalAuthElement
    with LoginLogout {

  val authenticator = new TwitterAuthenticator

  override def requestTokenToAccessToken(requestToken: RequestToken): AccessToken = {
      requestToken.token ++ "|-sep-|" ++ requestToken.secret 
//    TwitterOAuth10aAccessToken(
//      requestToken.token,
//      requestToken.secret
//    )
  }

}
