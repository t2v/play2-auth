package jp.t2v.lab.play2.auth

trait AuthComponents[Id, User, Authority] {
  def authConfig: AuthConfig[Id, User, Authority]
  def idContainer: AsyncIdContainer[Id]
  def tokenAccessor: TokenAccessor
}
