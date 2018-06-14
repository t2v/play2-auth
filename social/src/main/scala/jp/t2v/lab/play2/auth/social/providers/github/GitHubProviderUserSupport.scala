package jp.t2v.lab.play2.auth.social.providers.github

import jp.t2v.lab.play2.auth.social.core.OAuthProviderUserSupport
import play.api.Play.current
import play.api.libs.ws.WSResponse

import scala.concurrent.{ ExecutionContext, Future }

trait GitHubProviderUserSupport extends OAuthProviderUserSupport {
    self: GitHubController =>

  type ProviderUser = GitHubUser

  private def readProviderUser(accessToken: String, response: WSResponse): ProviderUser = {
    val j = response.json
    GitHubUser(
      (j \ "id").as[Long],
      (j \ "login").as[String],
      (j \ "avatar_url").as[String],
      accessToken
    )
  }

  def retrieveProviderUser(accessToken: String)(implicit ctx: ExecutionContext): Future[ProviderUser] = {
    for {
      response <- ws.url("https://api.github.com/user").withHttpHeaders("Authorization" -> s"token ${accessToken}").get()
    } yield {
      readProviderUser(accessToken, response)
    }
  }

}
