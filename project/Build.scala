import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sample"
  val appVersion      = "0.1"

  lazy val module = Project("play20-auth-module", file("module"))

  val appDependencies = Seq(
    jdbc,
    anorm,
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  lazy val root = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "jbcrypt repo" at "http://mvnrepository.com/"
  ).dependsOn(module)

}
