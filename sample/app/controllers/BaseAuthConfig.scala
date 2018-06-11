package controllers

import jp.t2v.lab.play2.auth.{AuthenticityToken, AsyncIdContainer, AuthConfig}
import jp.t2v.lab.play2.auth.sample.{Role, Account}
import jp.t2v.lab.play2.auth.sample.Role._
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{Future, ExecutionContext}
import scala.reflect._
import play.Logger
import scala.collection.concurrent.TrieMap
import scala.util.Random
import java.security.SecureRandom
import scala.annotation.tailrec

trait BaseAuthConfig  extends AuthConfig {

  type Id = Int
  type User = Account
  type Authority = Role

  val idTag: ClassTag[Id] = classTag[Id]
  val sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext) = Future.successful(Account.findById(id))
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = throw new AssertionError("don't use")
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext) = {
    Logger.info(s"authorizationFailed. userId: ${user.id}, userName: ${user.name}, authority: $authority")
    Future.successful(Forbidden("no permission"))
  }
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext) = Future.successful((user.role, authority) match {
    case (Administrator, _) => true
    case (NormalUser, NormalUser) => true
    case _ => false
  })

  override lazy val idContainer: AsyncIdContainer[Id] = new AsyncIdContainer[Id] {

    private val tokenSuffix = ":token"
    private val userIdSuffix = ":userId"
    private val random = new Random(new SecureRandom())

    override def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken] = {
      removeByUserId(userId)
      val token = generate()
      store(token, userId, timeoutInSeconds)
      Future.successful(token)
    }

    @tailrec
    private final def generate(): AuthenticityToken = {
      val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
      val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
      if (syncGet(token).isDefined) generate() else token
    }

    private def removeByUserId(userId: Id) {
      GlobalMap.container.get(userId.toString + userIdSuffix).map(_.asInstanceOf[String]) foreach unsetToken
      unsetUserId(userId)
    }

    override def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit] = {
      get(token).map(_ foreach unsetUserId)
      Future.successful(unsetToken(token))
    }

    private def unsetToken(token: AuthenticityToken) {
      GlobalMap.container.remove(token + tokenSuffix)
    }
    private def unsetUserId(userId: Id) {
      GlobalMap.container.remove(userId.toString + userIdSuffix)
    }

    override def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]] = {
      Future.successful(syncGet(token))
    }

    private def syncGet(token: AuthenticityToken): Option[Id] = {
      GlobalMap.container.get(token + tokenSuffix).map(_.asInstanceOf[Id])
    }

    private def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int) {
      GlobalMap.container.put(token + tokenSuffix, userId.asInstanceOf[AnyRef]/*, timeoutInSeconds*/) // TODO:
      GlobalMap.container.put(userId.toString + userIdSuffix, token.asInstanceOf[AnyRef]/*, timeoutInSeconds*/) // TODO:
    }

    override def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit] = {
      Future.successful(syncGet(token).foreach(store(token, _, timeoutInSeconds)))
    }

  }


}

object GlobalMap {
  private[controllers] val container: TrieMap[String, AnyRef] = new TrieMap[String, AnyRef]()
}