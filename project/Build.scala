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
      "com.propensive" % "rapture-io" % "0.7.2",
      "net.lingala.zip4j" % "zip4j" % "1.2.3"
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
