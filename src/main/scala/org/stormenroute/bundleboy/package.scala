package org.stormenroute



import java.io._
import dispatch._
import Defaults._



package object bundleboy {

  trait BundleApi {

    object Bintray {
      def createVersion(username: String, api: String, repo: String, pack: String, version: String, description: String) = {
        val address = s"http://api.bintray.com/packages/$username/$repo/$pack/versions"
        val request = url(address).secure.as(username, api) << s"""{
          "name": "$version",
          "desc": "$description"
        }"""
        Http(request POST)
      }

      def deleteVersion(username: String, api: String, repo: String, pack: String, version: String) = {
        val address = s"http://api.bintray.com/packages/$username/$repo/$pack/versions/$version"
        val request = url(address).secure.as(username, api)
        Http(request DELETE)
      }

      def upload(
        username: String, api: String, repo: String, pack: String, version: String,
        bundleName: String, path: String, onChunkSent: (Long, Long, Long) => Unit = (a, c, t) => ()
      ) = {
        val addressPath = s"$username/$repo/$pack/$version/$bundleName"

        val bundleFile = new File(path)
        val address = s"http://api.bintray.com/content/$addressPath;publish=1"
        val request = url(address).secure.as(username, api) <<< bundleFile

        import com.ning.http.client._
        val handler = new AsyncCompletionHandlerBase {
          override def onContentWriteProgress(amount: Long, current: Long, total: Long) = {
            onChunkSent(amount, current, total)
            super.onContentWriteProgress(amount, current, total)
          }
        }

        Http((request PUT) toRequest, handler)
      }

      def getPackageNames(ownername: String, repo: String) = {
        val address = s"http://api.bintray.com/repos/$ownername/$repo/packages"
        val request = url(address).secure

        Http(request GET) map { response =>
          import org.json4s._
          val content = response.getResponseBody
          val json = native.JsonMethods.parse(content)
          (json: @unchecked) match {
            case JArray(packages) =>
              for (JObject(fields) <- packages) yield {
                val JString(packageName) = fields.find(_._1 == "name").get._2
                packageName
              }
          }
        }
      }

      def download(ownername: String, repo: String, bundleName: String, destinationPath: String, logger: String => Unit = x => ()) = {
        val address = s"https://dl.bintray.com/$ownername/$repo/$bundleName"
        val destFile = new File(destinationPath)
        val request = url(address)

        logger("Sending request: " + address)

        Http.configure(_ setFollowRedirects true)(request > as.File(destFile))
      }
    }

  }

}
