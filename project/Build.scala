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
      "org.scalatest" % "scalatest_2.10" % "1.9.1",
      "net.lingala.zip4j" % "zip4j" % "1.2.3",
      "commons-io" % "commons-io" % "2.4"
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
