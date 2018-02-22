package jp.t2v.lab.play2.auth

import play.api.Environment

case class DefaultAuthComponents[Id, User, Authority](
  authConfig: AuthConfig[Id, User, Authority],
  idContainer: AsyncIdContainer[Id],
  tokenAccessor: TokenAccessor,
  env: Environment
) extends AuthComponents[Id, User, Authority]