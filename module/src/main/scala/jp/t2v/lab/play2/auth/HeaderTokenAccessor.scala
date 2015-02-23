package jp.t2v.lab.play2.auth

import play.api.mvc.{Result, RequestHeader}

class HeaderTokenAccessor(headerName: String = "X-Auth-Token") extends TokenAccessor {
  override def delete(result: Result): Result = result

  override def put(token: String)(result: Result): Result = {
    result.withHeaders(headerName -> sign(token))
  }

  override def extract(request: RequestHeader): Option[String] = {
    request.headers.get(headerName).flatMap(verifyHmac)
  }
}
