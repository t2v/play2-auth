import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "play21.auth"

  lazy val baseSettings = Seq(
    version            := "0.8-SNAPSHOT",
    scalaVersion       := "2.10.0",
    scalaBinaryVersion := "2.10",
    crossScalaVersions := Seq("2.10.0"),
    organization       := "jp.t2v",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots"
  )

  lazy val core = Project("core", base = file("module"))
    .settings(baseSettings: _*)
    .settings(
      name := appName,
      libraryDependencies += "play"     %%   "play"                   % "2.1.0",
      libraryDependencies += "jp.t2v"   %%   "stackable-controller"   % "0.1-SNAPSHOT",
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
      publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT")) 
          Some("snapshots" at nexus + "content/repositories/snapshots") 
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },
      pomExtra := (
        <url>https://github.com/t2v/play20-auth</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:t2v/play20-auth.git</url>
          <connection>scm:git:git@github.com:t2v/play20-auth.git</connection>
        </scm>
        <developers>
          <developer>
            <id>gakuzzzz</id>
            <name>gakuzzzz</name>
            <url>https://github.com/gakuzzzz</url>
          </developer>
        </developers>
      )
    )

  lazy val test = Project("test", base = file("test"))
    .settings(baseSettings: _*)
    .settings(
      name := appName + ".test",
      libraryDependencies += "play" %% "play-test" % "2.1.0"
    ).dependsOn(core)

  lazy val sample = play.Project("sample", path = file("sample"))
    .settings(baseSettings: _*)
    .settings(
      libraryDependencies += jdbc,
      libraryDependencies += "org.mindrot"          % "jbcrypt"                    % "0.3m",
      libraryDependencies += "com.github.seratch"  %% "scalikejdbc"                % "[1.4,)",
      libraryDependencies += "com.github.seratch"  %% "scalikejdbc-test"           % "[1.4,)",
      libraryDependencies += "com.github.seratch"  %% "scalikejdbc-play-plugin"    % "[1.4,)",
      libraryDependencies += "com.github.seratch"  %% "scalikejdbc-interpolation"  % "[1.4,)",
      publishLocal := {},
      publish := {}
    ).dependsOn(core, test % "test")

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    ).aggregate(core, test, sample)

}
