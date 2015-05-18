


import sbt._
import sbt.Keys._
import sbt.Process._
import sbt.complete.DefaultParsers._
import java.io.File
import org.apache.commons.io._
import org.stormenroute.bundleboy._



object BundleboySbtPlugin extends Plugin {

  val bundlePasswordKey = SettingKey[String](
    "bundlePassword",
    "Contains the password for the bundle file."
  )

  val bundlePathKey = SettingKey[String](
    "bundlePath",
    "Contains the path for the output bundle file."
  )

  val bundlePathSetting = bundlePathKey <<= (crossTarget, name, scalaVersion, version) { (path, name, scalaVersion, version) =>
    s"$path${File.separator}${name}_$scalaVersion-$version.bundle"
  }

  val bundleKey = TaskKey[Unit](
    "bundle",
    "Creates a bundle with the project contents."
  )

  val bundleCreatorKey = SettingKey[String](
    "bundleCreator",
    "Name of the bundle creator implementation."
  )

  val bundleTask = bundleKey <<= (bundleCreatorKey, bundlePasswordKey, bundlePathKey, classDirectory in Compile, streams) {
    (bundler, password, bundlepath, classdir, streams) =>
    streams map { streams =>
      assert(password != "", "Password cannot be empty.")
      val bundlefile = new File(bundlepath)
      if (bundlefile.exists()) bundlefile.delete()
      val alldirs = classdir.listFiles()
      streams.log.info("Bundling dirs: " + alldirs.mkString(", "))
      Class.forName(bundler).newInstance.asInstanceOf[Bundle.Creator].fromFolders(alldirs, bundlepath, () => password.toCharArray)
    }
  } dependsOn (packageBin in Compile)

  val bundleBintrayUserApiKey = SettingKey[(String, String)](
    "bundleBintrayUserApi",
    "Contains the username and the API key for Bintray."
  )

  val bundleBintrayUserApiSetting = bundleBintrayUserApiKey := {
    val credentialsPath = sys.props("user.home") + File.separator + ".bintray" + File.separator + ".userApiKey"
    val credentialsFile = new File(credentialsPath)
    if (credentialsFile.exists()) {
      val credentials = FileUtils.readFileToString(credentialsFile).trim.split(",")
      (credentials(0), credentials(1))
    } else {
      ("", "")
    }
  }

  private def withLogging(log: Logger, welcome: String, bye: String)(body: =>com.ning.http.client.Response) = {
    log.info(welcome)
    val response = body
    log.info(s"$bye, response message: '" + response.getResponseBody("UTF-8") + "'")
    val status = response.getStatusCode
    if (status.toString.apply(0) == '2') log.success("Successfully uploaded: " + status)
    else log.warn("Response code: " + status)
  }

  val bundleBintrayResetVersionTask = InputKey[Unit](
    "bundleBintrayResetVersion",
    "Deletes and creates a new version for the package on Bintray: <repo> <package> <version>    " +
    "(note: existing version and all related files will be deleted)"
  ) <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, bundleBintrayUserApiKey, bundleKey, streams) map { (args, userapi, _, streams) =>
      import scala.concurrent._
      import scala.concurrent.duration._
      
      withLogging(streams.log, "Deleting version...", "Version deleted") {
        val responseFuture = Bundle.Bintray.deleteVersion(userapi._1, userapi._2, args(0), args(1), args(2))
        Await.result(responseFuture, 6 seconds)
      }

      withLogging(streams.log, "Creating version...", "Version created") {
        val responseFuture = Bundle.Bintray.createVersion(userapi._1, userapi._2, args(0), args(1), args(2), args(3))
        Await.result(responseFuture, 6 seconds)
      }
    }
  }

  private def sleepRetry(noTimes: Int, pauseTime: Long)(body: =>Boolean) {
    var left = noTimes
    while (left > 0) {
      Thread.sleep(pauseTime)
      if (body) left = 0
      else left -= 1
    }
  }

  val bundleLocalTargetKey = SettingKey[String](
    "bundleLocalTarget",
    "Target file path for the bundle if it should be deployed locally."
  )

  val bundleLocalKey = TaskKey[Unit](
    "bundleLocal",
    "Bundles the file and copies it locally to the target file."
  )

  val bundleLocalTask = bundleLocalKey := {
    val bundle = bundleKey.value
    val bundlePath = bundlePathKey.value
    val targetPath = baseDirectory.value / bundleLocalTargetKey.value
    val log = streams.value.log
    println("Copying: " + bundlePath)
    println("Target:  " + targetPath)
    FileUtils.copyFile(new File(bundlePath), targetPath)
  }

  val bundleBintrayTask = InputKey[Unit](
    "bundleBintray",
    "Creates a bundle and deploys it to Bintray: <repo> <package> <version> <bundleName>."
  ) <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, bundleBintrayUserApiKey, bundlePathKey, bundleKey, streams) map { (args, userapi, path, _, streams) =>
      import scala.concurrent._
      import scala.concurrent.duration._
      
      withLogging(streams.log, "Uploading to Bintray...", "Upload completed") {
        val onChunkSent = (a: Long, c: Long, t: Long) => { streams.log.info(s"${c / 1024}kB/${t / 1024}kB transferred"); () }
        val responseFuture = Bundle.Bintray.upload(userapi._1, userapi._2, args(0), args(1), args(2), args(3), path, onChunkSent)
        streams.log.info("Tracking progress...")
        sleepRetry(600, 4000L) {
          streams.log.info("...")
          responseFuture.isCompleted
        }
        Await.result(responseFuture, 0 seconds)
      }
    }
  }

  override val projectSettings = Seq(
    bundlePasswordKey := "",
    bundleCreatorKey := "org.stormenroute.bundleboy.ZipBundleCreator",
    bundlePathSetting,
    bundleTask,
    bundleBintrayUserApiSetting,
    bundleBintrayResetVersionTask,
    bundleBintrayTask,
    bundleLocalTargetKey := "",
    bundleLocalTask
  )

}
