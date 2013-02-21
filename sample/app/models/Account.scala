package models

import org.mindrot.jbcrypt.BCrypt
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class Account(id: String, email: String, password: String, name: String, permission: Permission)

object Account {

  val * = { rs: WrappedResultSet => 
    Account(
      id         = rs.string("id"),
      email      = rs.string("email"),
      password   = rs.string("password"),
      name       = rs.string("name"),
      permission = Permission.valueOf(rs.string("permission"))
    )
  }

  def authenticate(email: String, password: String): Option[Account] = {
    findByEmail(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  def findByEmail(email: String): Option[Account] = {
    DB localTx { implicit s =>
      sql"SELECT * FROM account WHERE email = ${email}".map(*).single.apply()
    }
  }

  def findById(id: String): Option[Account] = {
    DB localTx { implicit s =>
      sql"SELECT * FROM account WHERE id = ${id}".map(*).single.apply()
    }
  }

  def findAll: Seq[Account] = {
    DB localTx { implicit s =>
      sql"SELECT * FROM account".map(*).list.apply()
    }
  }

  def create(account: Account) {
    DB localTx { implicit s =>
      import account._
      val pass = BCrypt.hashpw(account.password, BCrypt.gensalt())
      sql"INSERT INTO account VALUES (${id}, ${email}, ${pass}, ${name}, ${permission.toString})".update.apply()
    }
  }


}