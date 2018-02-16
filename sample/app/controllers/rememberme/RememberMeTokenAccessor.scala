//package controllers.rememberme
//
//import jp.t2v.lab.play2.auth._
//import play.api.mvc.{Cookie, RequestHeader, Result}
//
//class RememberMeTokenAccessor(maxAge: Int) extends CookieTokenAccessor() {
//
//  override def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result = {
//    val remember = request.tags.get("rememberme").exists("true" ==) || request.session.get("rememberme").exists("true" ==)
//    val _maxAge = if (remember) Some(maxAge) else None
//    val c = Cookie(cookieName, sign(token), _maxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
//    result.withCookies(c)
//  }
//
//
//}
