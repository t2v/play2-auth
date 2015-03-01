package jp.t2v.lab.play2.auth

import scala.util.control.Exception._

class TransparentIdContainer[Id: ToString: FromString] extends IdContainer[Id] {

  def startNewSession(userId: Id, timeoutInSeconds: Int) = implicitly[ToString[Id]].apply(userId)

  def remove(token: AuthenticityToken) {
  }

  def get(token: AuthenticityToken) = implicitly[FromString[Id]].apply(token)

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) {
    // Cookie Id Container does not support timeout.
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
