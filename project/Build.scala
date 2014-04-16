import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName    = "play2-auth"

  val playVersion = "2.3-M1"

  lazy val baseSettings = Seq(
    version            := "0.12.0",
    scalaVersion       := "2.10.4",
    scalaBinaryVersion := "2.10",
    organization       := "jp.t2v",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "Sonatype Releases"  at "https://oss.sonatype.org/content/repositories/releases"
  )

  lazy val appPublishMavenStyle = true
  lazy val appPublishArtifactInTest = false
  lazy val appPomIncludeRepository = { _: MavenRepository => false }
  lazy val appPublishTo = { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) 
      Some("snapshots" at nexus + "content/repositories/snapshots") 
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }
  lazy val appPomExtra = {
        <url>https://github.com/t2v/play2-auth</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:t2v/play2-auth.git</url>
          <connection>scm:git:git@github.com:t2v/play2-auth.git</connection>
        </scm>
        <developers>
          <developer>
            <id>gakuzzzz</id>
            <name>gakuzzzz</name>
            <url>https://github.com/gakuzzzz</url>
          </developer>
        </developers>
  }


  lazy val core = Project("core", base = file("module"))
    .settings(baseSettings: _*)
    .settings(
      libraryDependencies += "com.typesafe.play"  %%   "play"                   % playVersion        % "provided",
      libraryDependencies += "com.typesafe.play"  %%   "play-cache"             % playVersion,
      libraryDependencies += "jp.t2v"             %%   "stackable-controller"   % "0.3.0",
      name                    := appName,
      publishMavenStyle       := appPublishMavenStyle,
      publishArtifact in Test := appPublishArtifactInTest,
      pomIncludeRepository    := appPomIncludeRepository,
      publishTo               <<=(version)(appPublishTo),
      pomExtra                := appPomExtra
    )

  lazy val test = Project("test", base = file("test"))
    .settings(baseSettings: _*)
    .settings(
      libraryDependencies += "com.typesafe.play"  %% "play-test"   % playVersion,
      name                    := appName + "-test",
      publishMavenStyle       := appPublishMavenStyle,
      publishArtifact in Test := appPublishArtifactInTest,
      pomIncludeRepository    := appPomIncludeRepository,
      publishTo               <<=(version)(appPublishTo),
      pomExtra                := appPomExtra
    ).dependsOn(core)

  lazy val sample = play.Project("sample", path = file("sample"))
    .settings(baseSettings: _*)
    .settings(playScalaSettings: _*)
    .settings(
      libraryDependencies += jdbc,
      libraryDependencies += "org.mindrot"           % "jbcrypt"                    % "0.3m",
      libraryDependencies += "com.github.seratch"   %% "scalikejdbc"                % "[1.6,)",
      libraryDependencies += "com.github.seratch"   %% "scalikejdbc-test"           % "[1.6,)",
      libraryDependencies += "com.github.seratch"   %% "scalikejdbc-play-plugin"    % "[1.6,)",
      libraryDependencies += "com.github.seratch"   %% "scalikejdbc-interpolation"  % "[1.6,)",
      libraryDependencies += "com.github.tototoshi" %% "play-flyway"                % "1.0.0",
      templatesImport     += "jp.t2v.lab.play2.auth.sample._",
      publishLocal := {},
      publish := {}
    )
    .dependsOn(core, test % "test")

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    ).aggregate(core, test, sample)

}
