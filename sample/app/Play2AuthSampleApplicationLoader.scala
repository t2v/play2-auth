package jp.t2v.lab.play2.auth.sample

import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, LoggerConfigurator}

class Play2AuthSampleApplicationLoader extends ApplicationLoader {
  override def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach(_.configure(context.environment))

    new Play2AuthSampleComponents(context).application
  }
}
