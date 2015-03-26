package jp.t2v.lab.play2.auth.social.providers.facebook

case class FacebookUser(
  id: String,
  name: String,
  email: String,
  coverUrl: String,
  accessToken: String)
