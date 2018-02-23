package jp.t2v.lab.play2.auth.sample

import com.softwaremill.macwire.wire
import controllers.{AssetsComponents, MemoryIdContainer, csrf}
import jp.t2v.lab.play2.auth._
import play.api
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import router.Routes
import play.filters.HttpFiltersComponents
import scalikejdbc.PlayInitializer

class Play2AuthSampleComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents with AssetsComponents {

  override def httpFilters = Seq(/*csrfFilter, */securityHeadersFilter, allowedHostsFilter)

  lazy val router = {
    val prefix: String = httpConfiguration.context
    wire[Routes]
  }

  protected[this] lazy val cookieTokenAccessor: TokenAccessor = new CookieTokenAccessor(secretKey="ZSn5z9l]1dhRTKM[iBjc_YJQlRH:M<RoFz5ZQ<]foaETnzb]QMn2lU6mK?8xxGGQ")
  protected[this] lazy val memoryIdContainer: AsyncIdContainer[Int] = new MemoryIdContainer
  protected[this] lazy val authComponents: AuthComponents[Int, Account, Role] =
    new DefaultAuthComponents(new csrf.AuthConfigImpl, memoryIdContainer, cookieTokenAccessor, environment)

  protected[this] lazy val csrfSessions: csrf.Sessions = wire[csrf.Sessions]
  protected[this] lazy val csrfPreventingCsrfSample: csrf.PreventingCsrfSample = wire[csrf.PreventingCsrfSample]

  wire[PlayInitializer]

  protected[this] lazy val bodyParsers = new api.mvc.BodyParsers.Default(playBodyParsers)

}
