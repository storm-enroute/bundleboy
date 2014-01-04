package org.storm.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
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

  def stream(url: Bundle.Url): java.io.InputStream

  def loadClass(name: String): Class[_]

  val files = new Bundle.FilePath(this, Nil)

  val classes = new Bundle.Classes(this, Nil)

  implicit def byteReader = new StreamReader[Bundle.Url, Byte] {
    def input(t: Bundle.Url): Input[Byte] = {
      new ByteInput(new java.io.BufferedInputStream(stream(t)))
    }
  }

  implicit def charReader = new StreamReader[Bundle.Url, Char] {
    def input(t: Bundle.Url): Input[Char] = {
      new CharInput(new java.io.InputStreamReader(stream(t)))
    }
  }

}


object Bundle {

  trait Depickler[T] {
    def apply(is: InputStream): T
  }

  object Depickler {
    implicit def image = new Depickler[java.awt.image.BufferedImage] {
      def apply(is: InputStream) = javax.imageio.ImageIO.read(is)
    }
  }

  class FilePath(val self: Bundle, val folders: List[String]) extends Dynamic {
    def selectDynamic(name: String) = new FilePath(self, name :: folders)
    def file(filename: String) = 
      (filename :: folders).init.foldRight(self / folders.last) {
        (folder, path) => path / folder
      }
    import self.byteReader
    import self.charReader
    def bytes(name: String): Array[Byte] = {
      file(name).slurp[Byte]
    }
    def text(name: String)(implicit encoding: Encoding = Encodings.`UTF-8`): String = {
      file(name).slurp[Char]
    }
    def resource[T](name: String)(implicit depickler: Depickler[T]): T = {
      val is = new BufferedInputStream(self.stream(file(name)))
      try {
        depickler(is)
      } finally {
        is.close()
      }
    }
    def image(name: String): java.awt.image.BufferedImage = {
      resource(name)(Depickler.image)
    }
  }

  class Classes(val self: Bundle, val packages: List[String]) extends Dynamic {
    def selectDynamic(name: String) = new Classes(self, name :: packages)
    def get(cls: Symbol) = {
      val fullname = (cls.name :: packages).reverse.mkString(".")
      self.loadClass(fullname)
    }
  }

  class Url(bundle: Bundle, elements: Seq[String]) extends rapture.io.Url[Url](elements, Map()) {
    def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) = new Url(bundle, elements)
    def schemeSpecificPart = elements.mkString("//", "/", "")
    val pathRoot = bundle
  }

  abstract class ClazzLoader(val name: String, val classloader: ClassLoader) extends Bundle {
    def loadClass(name: String) = classloader.loadClass(name)
  }

  def newJarUrlClassLoader(url: String) = {
    import java.net._
    new URLClassLoader(Array(new URL(url)))
  }

  class Jar(name: String, url: String) extends ClazzLoader(name, newJarUrlClassLoader(url)) {
    def stream(url: Bundle.Url) = classloader.getResourceAsStream(url.pathString.substring(1))
  }

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

    object BundleURLStreamHandler extends URLStreamHandler {
      def openConnection(url: URL) = new BundleURLConnection(url)
    }
  
    class BundleURLConnection(url: URL) extends URLConnection(url) {
      def connect() {}
      override def getInputStream = {
        val fq = url.getFile.split("\\?")
        val fn = fq(0)
        assert(fn == filename)
        val query = fq(1)
        val path = URLDecoder.decode(query, "UTF-8")
        val header = zipfile.getFileHeader(path)
        zipfile.getInputStream(header)
      }
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

    private def getBundleResource(name: String) = {
      val encoded = URLEncoder.encode(name, "UTF-8")
      new URL("bundleboy:file", "", -1, filename + "?" + encoded, BundleURLStreamHandler)
    }

    override def findResource(name: String) = {
      val url = super.findResource(name)
      if (url != null) url
      else getBundleResource(name)
    }
  }

  class EncryptedZip(name: String, filename: String, password: String)
  extends ClazzLoader(name, newEncryptedZipUrlClassLoader(filename, password)) {
    def stream(url: Bundle.Url) = {
      val path = URLDecoder.decode(url.pathString.substring(1), "UTF-8")
      classloader.getResourceAsStream(path)
    }
  }

}
