package jp.t2v.lab.play20

import play.api.mvc._

package object auth {

  type AuthenticityToken = String

  implicit def wrapResult(result: Result): ResultWrapper = new ResultWrapper(result)

}

class ResultWrapper(result: Result) {

  private def applyRecursively(f: PlainResult => Result): Result = {
    def apply(r: Result): Result = r match {
      case p: PlainResult => f(p)
      case AsyncResult(p) => AsyncResult(p.map(apply))
    }
    apply(result)
  }

  def withHeaders(headers: (String, String)*): Result = applyRecursively(_.withHeaders(headers: _*))
  def as(contentType: String): Result = applyRecursively(_.as(contentType))
  def discardingCookies(names: String*): Result = applyRecursively(_.discardingCookies(names: _*))
  def flashing(values: (String, String)*): Result = applyRecursively(_.flashing(values: _*))
  def flashing(flash: Flash): Result = applyRecursively(_.flashing(flash))
  def withCookies(cookies: Cookie*): Result = applyRecursively(_.withCookies(cookies: _*))
  def withNewSession: Result = applyRecursively(_.withNewSession)
  def withSession(session: (String, String)*): Result = applyRecursively(_.withSession(session: _*))
  def withSession(session: Session): Result = applyRecursively(_.withSession(session))

}
