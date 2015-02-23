package jp.t2v.lab.play2.auth.sample

import org.mindrot.jbcrypt.BCrypt
import scalikejdbc._

case class Account(id: Int, email: String, password: String, name: String, role: Role)

object Account extends SQLSyntaxSupport[Account] {

  private val a = syntax("a")

  def apply(a: SyntaxProvider[Account])(rs: WrappedResultSet): Account = autoConstruct(rs, a)

  private val auto = AutoSession

  def authenticate(email: String, password: String)(implicit s: DBSession = auto): Option[Account] = {
    findByEmail(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  def findByEmail(email: String)(implicit s: DBSession = auto): Option[Account] = withSQL {
    select.from(Account as a).where.eq(a.email, email)
  }.map(Account(a)).single.apply()

  def findById(id: Int)(implicit s: DBSession = auto): Option[Account] = withSQL {
    select.from(Account as a).where.eq(a.id, id)
  }.map(Account(a)).single.apply()

  def findAll()(implicit s: DBSession = auto): Seq[Account] = withSQL { 
    select.from(Account as a) 
  }.map(Account(a)).list.apply()

  def create(account: Account)(implicit s: DBSession = auto) {
    withSQL { 
      import account._
      val pass = BCrypt.hashpw(account.password, BCrypt.gensalt())
      insert.into(Account).values(id, email, pass, name, role.toString) 
    }.update.apply()
  }

}
