import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appVersion = "0.1"

  lazy val module = Project("play20-auth-module", file("module"))

  val appDependencies = Seq(
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  lazy val root = PlayProject("sample", appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "jbcrypt repo" at "http://mvnrepository.com/"
  ).dependsOn(module)

}
