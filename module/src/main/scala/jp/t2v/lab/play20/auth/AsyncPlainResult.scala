package jp.t2v.lab.play20.auth

import play.api.mvc.{PlainResult, AsyncResult, Result}
import play.api.libs.concurrent.Promise

//case class AsyncPlainResult(underlying: AsyncResult) extends PlainResult {
//
//  def withHeaders(headers : (String, String)*): PlainResult = {
//    def set_(r: Result): Result = r match {
//      case AsyncResult(p) => AsyncPlainResult(p.map(set))
//      case p: PlainResult => p.withHeaders(headers: _*)
//    }
//    set_(underlying)
//  }
//
//}
//
//object AsyncPlainResult {
//  def apply(promise: Promise[Result]): AsyncPlainResult = new AsyncPlainResult(AsyncResult(promise))
//  def apply(result: Result): PlainResult = result match {
//    case a: AsyncResult => AsyncPlainResult(a)
//    case p: PlainResult => p
//  }
//}
