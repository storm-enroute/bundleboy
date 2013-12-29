package org.storm.bundleboy



import scala.language.dynamics
import rapture.io._



trait Bundle extends PathRoot[Bundle.Url] with Scheme[Bundle.Url] {

  def name: String

  def schemeName = "bundle:" + name

  def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) =
    new Bundle.Url(this, elements)
  
  def scheme = this

  def urlToStream(url: Bundle.Url): java.io.InputStream

  implicit object reader extends StreamReader[Bundle.Url, Byte] {
    def input(t: Bundle.Url): Input[Byte] =
      new ByteInput(new java.io.BufferedInputStream(urlToStream(t)))
  }

}


object Bundle {

  class Url(bundle: Bundle, elements: Seq[String]) extends rapture.io.Url[Url](elements, Map()) {
    def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) = new Url(bundle, elements)
    def schemeSpecificPart = elements.mkString("//", "/", "")
    val pathRoot = bundle
  }

  class Loader(val name: String, val classloader: ClassLoader) extends Bundle {
    def urlToStream(url: Bundle.Url) = classloader.getResourceAsStream(url.pathString.substring(1))
  }

  def newUrlClassLoader(url: String) = {
    import java.net._
    new URLClassLoader(Array(new URL(url)))
  }

  class Jar(name: String, url: String) extends Loader(name, newUrlClassLoader(url))

}
