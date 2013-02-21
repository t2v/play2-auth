package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import java.sql.Clob
import org.mindrot.jbcrypt.BCrypt

case class Account(id: String, email: String, password: String, name: String, permission: Permission)

object Account {

  object Clob {
    def unapply(clob: Clob): Option[String] = Some(clob.getSubString(1, clob.length.toInt))
  }

  implicit val rowToPermission: Column[Permission] = {
    Column.nonNull[Permission] { (value, meta) =>
      value match {
        case Clob("Administrator") => Right(Administrator)
        case Clob("NormalUser") => Right(NormalUser)
        case _ => Left(TypeDoesNotMatch(
          "Cannot convert %s : %s to Permission for column %s".format(value, value.getClass, meta.column)))
      }
    }
  }

  val simple = {
    get[String]("account.id") ~
    get[String]("account.email") ~
    get[String]("account.password") ~
    get[String]("account.name") ~
    get[Permission]("account.permission") map {
      case id~email~pass~name~perm => Account(id, email, pass, name, perm)
    }
  }

  def authenticate(email: String, password: String): Option[Account] = {
    findByEmail(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  def findByEmail(email: String): Option[Account] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM account WHERE email = {email}").on(
        'email -> email
      ).as(simple.singleOpt)
    }
  }

  def findById(id: String): Option[Account] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM account WHERE id = {id}").on(
        'id -> id
      ).as(simple.singleOpt)
    }
  }

  def findAll: Seq[Account] = {
    DB.withConnection { implicit connection =>
      SQL("select * from account").as(simple.*)
    }
  }

  def create(account: Account) {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO account VALUES ({id}, {email}, {pass}, {name}, {permission})").on(
        'id -> account.id,
        'email -> account.email,
        'pass -> BCrypt.hashpw(account.password, BCrypt.gensalt()),
        'name -> account.name,
        'permission -> account.permission.toString
      ).executeUpdate()
    }
  }


}