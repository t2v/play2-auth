package jp.t2v.lab.play2.auth.social.providers.facebook

import jp.t2v.lab.play2.auth.social.core.OAuth2Controller
import jp.t2v.lab.play2.auth.{ AuthConfig, Login, OptionalAuthElement }

trait FacebookController extends OAuth2Controller
    with AuthConfig
    with OptionalAuthElement
    with Login {

  val authenticator = new FacebookAuthenticator

}
