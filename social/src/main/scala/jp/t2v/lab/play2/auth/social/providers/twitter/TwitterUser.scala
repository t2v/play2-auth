package jp.t2v.lab.play2.auth.social.providers.twitter

case class TwitterUser(
  id: Long,
  screenName: String,
  name: String,
  description: String,
  profileImageUrl: String,
  accessToken: String,
  accessTokenSecret: String)
