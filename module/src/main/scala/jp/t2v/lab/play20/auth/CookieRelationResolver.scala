package jp.t2v.lab.play20.auth

import play.api.mvc._
import scala.util.control.Exception._

class CookieRelationResolver[Id : ToString : FromString, A](request: Request[A]) extends RelationResolver[Id] {

  private[auth] val UserIdKey = "AUTH_USER_ID"

  def exists(sessionId: String) = false

  def sessionId2userId(sessionId: String): Option[Id] = for {
    userIdStr <- request.session.get(UserIdKey)
    userId <- implicitly[FromString[Id]].apply(userIdStr)
  } yield userId

  def userId2sessionId(userId: Id): Option[String] =
    request.session.get("sessionId") // TODO: commonalize

  def removeBySessionId(sessionId: String) {}
  def removeByUserId(userId: Id) {}

  def store(sessionId: String, userId: Id, timeoutInSeconds: Int) =
    Session() + (UserIdKey -> implicitly[ToString[Id]].apply(userId))
  def prolongTimeout(sessionId: String, timeoutInSeconds: Int) {
    // This resolver does not support the session timeout
  }

}

trait ToString[A] {
  def apply(id: A): String
}
object ToString {
  def apply[A](f: A => String) = new ToString[A] {
    def apply(id: A) = f(id)
  }
  implicit val string = ToString[String](identity)
  implicit val int = ToString[Int](_.toString)
  implicit val long = ToString[Long](_.toString)
}
trait FromString[A] {
  def apply(id: String): Option[A]
}
object FromString {
  def apply[A](f: String => A) = new FromString[A] {
    def apply(id: String) = allCatch opt f(id)
  }
  implicit val string = FromString[String](identity)
  implicit val int = FromString[Int](_.toInt)
  implicit val long = FromString[Long](_.toLong)
}