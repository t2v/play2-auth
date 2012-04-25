name := "play20.auth"

version := "0.1"

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "play" %% "play" % "2.0.1"
)

organization := "jp.t2v"

publishTo := Some(Resolver.file("maven-repo", file("../../maven-repo/")))
