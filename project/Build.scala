import sbt._
import Keys._
import Process._
import java.io._



object BundleBoyBuild extends Build {

  /* bundleboy */

  def versionFromFile(filename: String): String = {
    val fis = new FileInputStream(filename)
    val props = new java.util.Properties()
    try props.load(fis)
    finally fis.close()

    val major = props.getProperty("bundleboy_major")
    val minor = props.getProperty("bundleboy_minor")
    s"$major.$minor"
  }

  val frameworkVersion = baseDirectory { dir =>
    versionFromFile(dir + File.separator + "version.conf")
  }

  val bundleboySettings = Defaults.defaultSettings ++ Seq(
    name := "bundleboy",
    organization := "com.storm-enroute",
    version := frameworkVersion,
    scalaVersion := "2.10.2",
    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.10" % "2.1.0",
      "commons-io" % "commons-io" % "2.4",
      "org.apache.commons" % "commons-compress" % "1.8",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-optimise",
      "-Yinline-warnings"
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
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

  lazy val bundleboy = Project(
    "bundleboy",
    file("."),
    settings = bundleboySettings
  )

}
