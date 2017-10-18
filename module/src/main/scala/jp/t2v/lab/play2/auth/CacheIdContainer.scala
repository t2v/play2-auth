package jp.t2v.lab.play2.auth

import play.api.cache.AsyncCacheApi

import scala.annotation.tailrec
import scala.util.Random
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class CacheIdContainer[Id: ClassTag] (cacheApi: AsyncCacheApi) extends AsyncIdContainer[Id] {

  private[auth] val tokenSuffix = ":token"
  private[auth] val userIdSuffix = ":userId"
  private[auth] val random = new Random(new SecureRandom())

  def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken] = {
    val removeF = removeByUserId(userId)
    for {
      token <- generate()
      _     <- removeF
      _     <- store(token, userId, timeoutInSeconds)
    } yield token
  }

  private[auth] final def generate()(implicit context: ExecutionContext): Future[AuthenticityToken] = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
    val token = Iterator.continually(random.nextInt(table.length)).map(table).take(64).mkString
    get(token).flatMap {
      case Some(_) => generate()
      case None    => Future.successful(token)
    }
  }

  private[auth] def removeByUserId(userId: Id)(implicit context: ExecutionContext): Future[Unit] = {
    val userIdF = unsetUserId(userId)
    for {
      tokenOpt <- cacheApi.get[AuthenticityToken](userId.toString + userIdSuffix)
      _        <- tokenOpt.fold(Future.successful(()), unsetToken _)
      _        <- userIdF
    } yield ()
  }

  def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit] = {
    val tokenF = unsetToken(token)
    for {
      userIdOpt <- get(token)
      _         <- userIdOpt.fold(Future.successful(()), unsetUserId _)
      _         <- tokenF
    } yield ()
  }

  private[auth] def unsetToken(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit] = {
    cacheApi.remove(token + tokenSuffix).map(_ => ())
  }
  private[auth] def unsetUserId(userId: Id)(implicit context: ExecutionContext): Future[Unit] = {
    cacheApi.remove(userId.toString + userIdSuffix).map(_ => ())
  }

  def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]] = cacheApi.get(token + tokenSuffix)

  private[auth] def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int)(implicit context: ExecutionContext): Future[Unit] =  {
    def intToDuration(seconds: Int): Duration = if (seconds == 0) Duration.Inf else Duration(seconds, TimeUnit.SECONDS)
    val idFuture    = cacheApi.set(token + tokenSuffix, userId, intToDuration(timeoutInSeconds))
    val tokenFuture = cacheApi.set(userId.toString + userIdSuffix, token, intToDuration(timeoutInSeconds))
    for (_ <- idFuture; _ <- tokenFuture) yield ()
  }

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit] = {
    for {
      tokenOpt <- get(token)
      _        <- tokenOpt.fold(Future.successful(()), store(token, _, timeoutInSeconds))
    } yield ()
  }

}
