package jp.t2v.lab.play2.auth.sample

import com.softwaremill.macwire.wire
import controllers.{AssetsComponents, MemoryIdContainer, builder, csrf, basic}
import jp.t2v.lab.play2.auth._
import play.api
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.filters.HttpFiltersComponents
import router.Routes
import scalikejdbc.PlayInitializer

class Play2AuthSampleComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with CsrfComponents
    with BuilderComponents
    with BasicAuthComponents
{

  override def httpFilters = Seq(/*csrfFilter, */securityHeadersFilter, allowedHostsFilter)

  lazy val defaultController: controllers.Default = wire[controllers.Default]

  lazy val router: Routes = {
    val prefix: String = httpConfiguration.context
    new Routes(httpErrorHandler, builderSessions, builderMessages, csrfSessions, csrfPreventingCsrfSample, defaultController, basicMessages, assets, prefix)
//    wire[Routes]
  }

  wire[PlayInitializer]

  protected[this] lazy val bodyParsers = new api.mvc.BodyParsers.Default(playBodyParsers)

}

trait AuthCommonComponents {
  protected[this] lazy val cookieTokenAccessor: TokenAccessor = new CookieTokenAccessor(secretKey="ZSn5z9l]1dhRTKM[iBjc_YJQlRH:M<RoFz5ZQ<]foaETnzb]QMn2lU6mK?8xxGGQ")
  protected[this] lazy val memoryIdContainer: AsyncIdContainer[Int] = new MemoryIdContainer
}

trait CsrfComponents extends AuthCommonComponents { self: BuiltInComponentsFromContext =>

  private lazy val csrfAuthComponents: AuthComponents[Int, Account, Role] = new DefaultAuthComponents(new csrf.AuthConfigImpl, memoryIdContainer, cookieTokenAccessor, environment)

  protected[this] lazy val csrfSessions: csrf.Sessions = wire[csrf.Sessions]
  protected[this] lazy val csrfPreventingCsrfSample: csrf.PreventingCsrfSample = wire[csrf.PreventingCsrfSample]

}
trait BuilderComponents extends AuthCommonComponents { self: BuiltInComponentsFromContext =>

  private lazy val builderAuthComponents: AuthComponents[Int, Account, Role] = new DefaultAuthComponents(new builder.AuthConfigImpl, memoryIdContainer, cookieTokenAccessor, environment)

  protected[this] lazy val builderSessions: builder.Sessions = wire[builder.Sessions]
  protected[this] lazy val builderMessages: builder.Messages = wire[builder.Messages]

}
trait BasicAuthComponents extends AuthCommonComponents { self: BuiltInComponentsFromContext =>

  private lazy val builderAuthComponents: AuthComponents[Int, Account, Role] = new DefaultAuthComponents(new basic.AuthConfigImpl, memoryIdContainer, cookieTokenAccessor, environment)

  protected[this] lazy val basicMessages: basic.Messages = wire[basic.Messages]

}