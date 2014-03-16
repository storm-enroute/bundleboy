import sbt._
import Keys._
import Process._
import java.io.File



object BundleBoyBuild extends Build {

  /* bundleboy */

  val publishUser = "SONATYPE_USER"
  
  val publishPass = "SONATYPE_PASS"
  
  val userPass = for {
    user <- sys.env.get(publishUser)
    pass <- sys.env.get(publishPass)
  } yield (user, pass)

  val publishCreds: Seq[Setting[_]] = Seq(userPass match {
    case Some((user, pass)) =>
      credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    case None =>
      // prevent publishing
      publish <<= streams.map(_.log.info("Publishing to Sonatype is disabled since the \"" + publishUser + "\" and/or \"" + publishPass + "\" environment variables are not set."))
  })

  val bundleboySettings = Defaults.defaultSettings ++ publishCreds ++ Seq(
    name := "bundleboy",
    organization := "com.storm-enroute",
    version := "0.2-SNAPSHOT",
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
