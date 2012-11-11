name := "play20.auth"

scalaVersion := "2.9.1"

version := "0.5-LOCAL-SNAPSHOT"

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "play" %% "play" % "2.0.3"
)

organization := "jp.t2v"

publishTo := sys.env.get("LOCAL_MAVEN_REPO").map { dir =>
  Resolver.file("maven-repo", file(dir))(Patterns(true, Resolver.mavenStyleBasePattern))
}
