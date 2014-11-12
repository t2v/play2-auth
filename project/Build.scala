import sbt._
import Keys._
import play.twirl.sbt.Import.TwirlKeys

object ApplicationBuild extends Build {

  val appName    = "play2-auth"

  val playVersion = play.core.PlayVersion.current

  lazy val baseSettings = Seq(
    version            := "0.13.0-SNAPSHOT",
    scalaVersion       := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    organization       := "jp.t2v",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers ++= {
      if (isSnapshot.value) {
        Seq("Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      } else {
        Nil
      }
    },
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
      libraryDependencies += play.PlayImport.cache,
      libraryDependencies += "jp.t2v"             %%   "stackable-controller"   % "0.4.1",
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

  lazy val sample = Project("sample", file("sample"))
    .enablePlugins(play.PlayScala)
    .settings(baseSettings: _*)
    .settings(
      libraryDependencies += play.Play.autoImport.jdbc,
      libraryDependencies += "org.mindrot"           % "jbcrypt"                    % "0.3m",
      libraryDependencies += "org.scalikejdbc"      %% "scalikejdbc"                % "2.2.0",
      libraryDependencies += "org.scalikejdbc"      %% "scalikejdbc-test"           % "2.2.0"   % "test",
      libraryDependencies += "org.scalikejdbc"      %% "scalikejdbc-play-plugin"    % "2.3.0",
      libraryDependencies += "com.github.tototoshi" %% "play-flyway"                % "1.1.0",
      TwirlKeys.templateImports in Compile += "jp.t2v.lab.play2.auth.sample._",
      publish           := { },
      publishArtifact   := false,
      packagedArtifacts := Map.empty,
      publishTo         <<=(version)(appPublishTo),
      pomExtra          := appPomExtra
    )
    .dependsOn(core, test % "test")

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publish           := { },
      publishArtifact   := false,
      packagedArtifacts := Map.empty,
      publishTo         <<=(version)(appPublishTo),
      pomExtra          := appPomExtra
    ).aggregate(core, test, sample)

}
