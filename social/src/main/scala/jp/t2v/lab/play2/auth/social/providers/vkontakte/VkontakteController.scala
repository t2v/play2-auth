package controllers.providers.vkontakte

/**
  * Created by Yuri Rastegaev on 19.03.2016.
  */

import jp.t2v.lab.play2.auth.social.core.OAuth2Controller
import jp.t2v.lab.play2.auth.{AuthConfig, Login, OptionalAuthElement}

trait VkontakteController extends OAuth2Controller
    with AuthConfig
    with OptionalAuthElement
    with Login {

  val authenticator = new VkontakteAuthenticator

}
