package jp.t2v.lab.play2.auth

import play.api.cache.AsyncCacheApi
import play.api.Play._

import scala.annotation.tailrec
import scala.util.Random
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Await

class CacheIdContainer[Id: ClassTag] (cacheApi: AsyncCacheApi)(implicit val ec: ExecutionContext) extends IdContainer[Id] {

  private[auth] val tokenSuffix = ":token"
  private[auth] val userIdSuffix = ":userId"
  private[auth] val random = new Random(new SecureRandom())

  private def intToDuration(seconds: Int): Duration = if (seconds == 0) Duration.Inf else seconds.seconds

  def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    removeByUserId(userId)
    val token = generate
    store(token, userId, timeoutInSeconds)
    token
  }

  @tailrec
  private[auth] final def generate: AuthenticityToken = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
    val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (get(token).isDefined) generate else token
  }

  private[auth] def removeByUserId(userId: Id) {
    cacheApi.get[String](userId.toString + userIdSuffix).foreach(x=>x.foreach(unsetToken))(ec)
    unsetUserId(userId)
  }

  def remove(token: AuthenticityToken) {
    get(token) foreach unsetUserId
    unsetToken(token)
  }

  private[auth] def unsetToken(token: AuthenticityToken) {
    cacheApi.remove(token + tokenSuffix)
  }
  private[auth] def unsetUserId(userId: Id) {
    cacheApi.remove(userId.toString + userIdSuffix)
  }

  def get(token: AuthenticityToken) = {
    val f = cacheApi.get(token + tokenSuffix).map(_.map(_.asInstanceOf[Id]))
    Await.result(f, Duration.Inf)
  }

  private[auth] def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int) {
    def intToDuration(seconds: Int): Duration = if (seconds == 0) Duration.Inf else Duration(seconds, TimeUnit.SECONDS)
    cacheApi.set(token + tokenSuffix, userId, intToDuration(timeoutInSeconds))
    cacheApi.set(userId.toString + userIdSuffix, token, intToDuration(timeoutInSeconds))
  }

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) {
    get(token).foreach(store(token, _, timeoutInSeconds))
  }

}
