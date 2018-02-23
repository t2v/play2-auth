package jp.t2v.lab.play2.auth.sample

import jp.t2v.lab.play2.auth.sample.Role._
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, LoggerConfigurator}

class Play2AuthSampleApplicationLoader extends ApplicationLoader {
  override def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach(_.configure(context.environment))

    val app = new Play2AuthSampleComponents(context).application

    {
      import scalikejdbc._

      DB.localTx { implicit sess =>
        sql"""CREATE TABLE account (
         id         integer NOT NULL PRIMARY KEY,
         email      varchar NOT NULL UNIQUE,
         password   varchar NOT NULL,
         name       varchar NOT NULL,
         role       varchar NOT NULL
         );""".execute().apply()
      }

      if (Account.findAll.isEmpty) {
        Seq(
          Account(1, "alice@example.com", "secret", "Alice", Administrator),
          Account(2, "bob@example.com", "secret", "Bob", NormalUser),
          Account(3, "chris@example.com", "secret", "Chris", NormalUser)
        ) foreach Account.create
      }
    }

    app
  }
}
