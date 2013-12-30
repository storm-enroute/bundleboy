package org.storm.bundleboy



import scala.language.dynamics
import rapture.io._
import net.lingala.zip4j.core._
import net.lingala.zip4j.model._
import net.lingala.zip4j.util._



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

  def newJarUrlClassLoader(url: String) = {
    import java.net._
    new URLClassLoader(Array(new URL(url)))
  }

  class Jar(name: String, url: String) extends Loader(name, newJarUrlClassLoader(url))

  def newEncryptedZipUrlClassLoader(filename: String, password: String) = {
    import java.net._
    new EncryptedClassLoader(filename, password)
  }

  class EncryptedClassLoader(filename: String, password: String) extends ClassLoader {
    val zipfile = new ZipFile(filename)
    var buffer: Array[Byte] = new Array[Byte](1024)
    if (zipfile.isEncrypted) zipfile.setPassword(password)

    private def grow() {
      val nbuffer = new Array[Byte](buffer.length * 2)
      System.arraycopy(buffer, 0, nbuffer, 0, buffer.length)
      buffer = nbuffer
    }

    override def findClass(name: String) = {
      val path = name.replace('.', '/') + ".class"
      val header = zipfile.getFileHeader(path)
      val is = zipfile.getInputStream(header)
      var len = 0
      try {
        var lastread = 0
        do {
          len += lastread
          lastread = is.read(buffer, len, buffer.length - len)
          if (len == buffer.length) grow()
        } while (lastread != -1)
      } finally {
        is.close()
      }
      defineClass(name, buffer, 0, len)
    }

    override def findResource(name: String) = ???
  }

  class EncryptedZip(name: String, filename: String, password: String) extends Loader(name, newEncryptedZipUrlClassLoader(filename, password))

}
