package jp.t2v.lab.play2.auth.sample

import com.softwaremill.macwire.wire
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.routing.Router.Routes
import play.filters.HttpFiltersComponents

class Play2AuthSampleComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {

  lazy val router = {
    val prefix: String = httpConfiguration.context
    wire[Routes]
  }



}
