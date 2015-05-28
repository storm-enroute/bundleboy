


import java.io._
import org.stormenroute.mecha._
import sbt._
import sbt.Keys._
import sbt.Process._



object BundleBoyBuild extends MechaRepoBuild {

  def repoName = "bundleboy"

  /* bundleboy */

  val frameworkVersion = Def.setting {
    ConfigParsers.versionFromFile(
      (baseDirectory in bundleboy).value / "version.conf",
      List("bundleboy_major", "bundleboy_minor"))
  }

  val bundleboyCrossScalaVersions = Def.setting {
    val dir = (baseDirectory in bundleboy).value
    val path = dir + File.separator + "cross.conf"
    scala.io.Source.fromFile(path).getLines.filter(_.trim != "").toSeq
  }

  val bundleboyScalaVersion = Def.setting {
    bundleboyCrossScalaVersions.value.head
  }

  val bundleboySettings = Defaults.defaultSettings ++
    MechaRepoPlugin.defaultSettings ++ Seq(
    name := "bundleboy",
    organization := "com.storm-enroute",
    version <<= frameworkVersion,
    scalaVersion <<= bundleboyScalaVersion,
    crossScalaVersions <<= bundleboyCrossScalaVersions,
    libraryDependencies <++= (scalaVersion)(sv => dependencies(sv)),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-optimise",
      "-Yinline-warnings"
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at
        "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at
        "https://oss.sonatype.org/content/repositories/releases"
    ),
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>http://storm-enroute.com/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:storm-enroute/bundleboy.git</url>
        <connection>scm:git:git@github.com:storm-enroute/bundleboy.git</connection>
      </scm>
      <developers>
        <developer>
          <id>axel22</id>
          <name>Aleksandar Prokopec</name>
          <url>http://axel22.github.com/</url>
        </developer>
      </developers>,
    mechaPublishKey <<= mechaPublishKey.dependsOn(publish),
    mechaDocsRepoKey := "git@github.com:storm-enroute/apidocs.git",
    mechaDocsBranchKey := "gh-pages",
    mechaDocsPathKey := "bundleboy"
  )

  def dependencies(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => Seq(
      "org.scalatest" % "scalatest_2.11" % "2.1.7" % "test",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2",
      "org.json4s" %% "json4s-native" % "3.2.10",
      "commons-io" % "commons-io" % "2.4",
      "org.apache.commons" % "commons-compress" % "1.8",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
    )
    case Some((2, 10)) => Seq(
      "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test",
      "org.json4s" %% "json4s-native" % "3.2.10",
      "commons-io" % "commons-io" % "2.4",
      "org.apache.commons" % "commons-compress" % "1.8",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
    )
    case _ => Nil
  }

  lazy val bundleboy: Project = Project(
    "bundleboy",
    file("."),
    settings = bundleboySettings
  )

  val bundleboySbtPluginScalaVersion = Def.setting {
    "2.10.4"
  }

  val bundleboySbtPluginCrossScalaVersions = Def.setting {
    Seq("2.10.4")
  }

  val bundleboySbtPluginSettings = Defaults.defaultSettings ++ Seq(
    sbtPlugin := true,
    name := "bundleboy-sbt-plugin",
    scalaVersion <<= bundleboySbtPluginScalaVersion,
    version <<= frameworkVersion,
    organization := "com.storm-enroute",
    crossScalaVersions <<= bundleboySbtPluginCrossScalaVersions,
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.4",
      "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test",
      "org.json4s" %% "json4s-native" % "3.2.10",
      "org.apache.commons" % "commons-compress" % "1.8",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
    ),
    unmanagedSourceDirectories in Compile +=
      baseDirectory.value / ".." / "src" / "main" / "scala",
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at
        "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at
        "https://oss.sonatype.org/content/repositories/releases"
    ),
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>http://storm-enroute.com/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:storm-enroute/bundleboy.git</url>
        <connection>scm:git:git@github.com:storm-enroute/bundleboy.git</connection>
      </scm>
      <developers>
        <developer>
          <id>axel22</id>
          <name>Aleksandar Prokopec</name>
          <url>http://axel22.github.com/</url>
        </developer>
      </developers>
  )

  lazy val bundleboySbtPlugin = Project(
    "bundleboy-sbt-plugin",
    file("bundleboy-sbt-plugin"),
    settings = bundleboySbtPluginSettings
  )

}
