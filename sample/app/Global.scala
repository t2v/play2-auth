
import play.api.inject.ApplicationLifecycle
import jp.t2v.lab.play2.auth.sample._
import jp.t2v.lab.play2.auth.sample.Role._
import scalikejdbc._
import javax.inject._


@Singleton
class GlobalInitializer @Inject() (appLifecycle: ApplicationLifecycle) {
  if (Account.findAll.isEmpty) {
    Seq(
      Account(1, "alice@example.com", "secret", "Alice", Administrator),
      Account(2, "bob@example.com",   "secret", "Bob",   NormalUser),
      Account(3, "chris@example.com", "secret", "Chris", NormalUser)
    ) foreach Account.create
  }
}
