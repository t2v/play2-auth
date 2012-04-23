import play.api._

import models._
import anorm._

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    if (Account.findAll.isEmpty) {
      Seq(
        Account("aaaaaa", "alice@example.com", "secret", "Alice", Administrator),
        Account("bbbbbb", "bob@example.com", "secret", "Bob", NormalUser),
        Account("cccccc", "chris@example.com", "secret", "Chris", NormalUser)
      ) foreach Account.create
    }

  }

}