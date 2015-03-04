package jp.t2v.lab.play2

import play.api.mvc.Result

package object auth {

  type AuthenticityToken = String
  type SignedToken = String

  type ResultUpdater = Result => Result

  @deprecated("renamed to TransparentIdContainer", since = "0.13.1")
  type CookieIdContainer[Id] = TransparentIdContainer[Id]

}
