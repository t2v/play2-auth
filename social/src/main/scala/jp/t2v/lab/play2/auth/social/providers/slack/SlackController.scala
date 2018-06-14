package jp.t2v.lab.play2.auth.social.providers.slack

import jp.t2v.lab.play2.auth.social.core.OAuth2Controller
import jp.t2v.lab.play2.auth.{ AuthConfig, Login, OptionalAuthElement }
import play.api.libs.ws.WSClient

trait SlackController extends OAuth2Controller
    with AuthConfig
    with OptionalAuthElement
    with Login {

  val ws: WSClient
  
  val authenticator = new SlackAuthenticator(ws)

}