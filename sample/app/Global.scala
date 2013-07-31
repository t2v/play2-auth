import play.api._

import jp.t2v.lab.play2.auth.sample._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    if (Account.findAll.isEmpty) {
      Seq(
        Account(1, "alice@example.com", "secret", "Alice", Administrator),
        Account(2, "bob@example.com",   "secret", "Bob",   NormalUser),
        Account(3, "chris@example.com", "secret", "Chris", NormalUser)
      ) foreach Account.create
    }

  }

}