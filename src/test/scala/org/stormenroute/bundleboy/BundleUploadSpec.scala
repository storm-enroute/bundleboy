package org.stormenroute.bundleboy



import scala.concurrent._
import scala.concurrent.duration._
import org.scalatest._



class BundleUploadSpec extends FunSuite with Matchers {

  ignore("Bundle.Upload should correctly upload to Bintray") {
    val username = "user"
    val api = "fe8c"
    val repo = "repos"
    val pack = "packs"
    val version = "1.0"
    val bundleName = "example-classes.bundle"
    val bundleFile = "resources/example-classes.bundle"
    val responseFuture = Bundle.Bintray.upload(username, api, repo, pack, version, bundleName, bundleFile, (a, c, t) => println(c + " bytes sent"))
    val response = Await.result(responseFuture, 10 second)
    println(response.getResponseBody("UTF-8"))
  }

}
