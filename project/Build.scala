import sbt._
import Keys._
import Process._
import java.io.File



object BundleBoyBuild extends Build {

  val bundleBoySettings = Defaults.defaultSettings ++ Seq(
    organization := "org.storm",
    version := "0.0.1",
    scalaVersion := "2.10.1",
    libraryDependencies ++= Seq(
      "com.propensive" % "rapture-io" % "0.7.2"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-optimise",
      "-Yinline-warnings"
    )
  )

  lazy val bundleboy = Project(
    "bundleboy",
    file("."),
    settings = bundleBoySettings
  )

}
