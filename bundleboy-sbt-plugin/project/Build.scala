import sbt._
import Keys._
import Process._
import java.io._



object BundleboySbtPluginBuild extends Build {

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
    versionFromFile(dir.getParent + File.separator + "version.conf")
  }

  val bundleboyScalaVersion = "2.10.2"

  val bundleboySbtPluginSettings = Defaults.defaultSettings ++ Seq(
    sbtPlugin := true,
    name := "bundleboy-sbt-plugin",
    scalaVersion := bundleboyScalaVersion,
    version <<= frameworkVersion,
    organization := "com.storm-enroute",
    libraryDependencies += "commons-io" % "commons-io" % "2.4",
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

  lazy val bundleboy = RootProject(uri("../"))

  lazy val bundleboySbtPlugin = Project(
    "bundleboy-sbt-plugin",
    file("."),
    settings = bundleboySbtPluginSettings
  ) dependsOn(bundleboy)

}
