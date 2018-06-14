package jp.t2v.lab.play2.auth.social.providers.facebook

import jp.t2v.lab.play2.auth.social.core.OAuth2Controller
import jp.t2v.lab.play2.auth.{ AuthConfig, Login, OptionalAuthElement }
import play.api.libs.ws.WSClient

trait FacebookController extends OAuth2Controller
    with AuthConfig
    with OptionalAuthElement
    with Login {
  
  val ws: WSClient

  val authenticator = new FacebookAuthenticator(ws)

}
