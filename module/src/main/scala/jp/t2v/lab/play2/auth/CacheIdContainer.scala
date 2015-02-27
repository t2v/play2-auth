package jp.t2v.lab.play2.auth

import play.api.cache.Cache
import play.api.Play._
import scala.annotation.tailrec
import scala.util.Random
import java.security.SecureRandom
import scala.reflect.ClassTag

class CacheIdContainer[Id: ClassTag] extends IdContainer[Id] {

  private[auth] val tokenSuffix = ":token"
  private[auth] val userIdSuffix = ":userId"
  private[auth] val random = new Random(new SecureRandom())

  def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    removeByUserId(userId)
    val token = generate
    store(token, userId, timeoutInSeconds)
    token
  }

  @tailrec
  private[auth] final def generate: AuthenticityToken = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.!~*'()"
    val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (get(token).isDefined) generate else token
  }

  private[auth] def removeByUserId(userId: Id) {
    Cache.getAs[String](userId.toString + userIdSuffix) foreach unsetToken
    unsetUserId(userId)
  }

  def remove(token: AuthenticityToken) {
    get(token) foreach unsetUserId
    unsetToken(token)
  }

  private[auth] def unsetToken(token: AuthenticityToken) {
    Cache.remove(token + tokenSuffix)
  }
  private[auth] def unsetUserId(userId: Id) {
    Cache.remove(userId.toString + userIdSuffix)
  }

  def get(token: AuthenticityToken) = Cache.get(token + tokenSuffix).map(_.asInstanceOf[Id])

  private[auth] def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int) {
    Cache.set(token + tokenSuffix, userId, timeoutInSeconds)
    Cache.set(userId.toString + userIdSuffix, token, timeoutInSeconds)
  }

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) {
    get(token).foreach(store(token, _, timeoutInSeconds))
  }

}
