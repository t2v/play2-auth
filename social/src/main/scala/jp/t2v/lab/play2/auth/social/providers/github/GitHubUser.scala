package jp.t2v.lab.play2.auth.social.providers.github

case class GitHubUser(
  id: Long,
  login: String,
  avatarUrl: String,
  accessToken: String)
