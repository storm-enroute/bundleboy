package org.storm.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
import scala.collection._
import scala.collection.convert.decorateAsScala._
import rapture.io._
import net.lingala.zip4j.core._
import net.lingala.zip4j.model._
import net.lingala.zip4j.util._



trait Bundle extends PathRoot[Bundle.Url] with Scheme[Bundle.Url] {

  def name: String

  protected def bundleUrl: String

  def schemeName = "bundleboy:" + bundleUrl + "?"

  def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) =
    new Bundle.Url(this, elements)
  
  def scheme = this

  def loadStream(url: Bundle.Url): java.io.InputStream

  def loadClass(name: String): Class[_]

  def loadSubclasses(packageName: String, baseClass: Class[_]): Iterable[Class[_]]

  val files = new Bundle.FilePath(this, Nil)

  val classes = new Bundle.Classes(this, Nil)

  implicit def byteReader = new StreamReader[Bundle.Url, Byte] {
    def input(t: Bundle.Url): Input[Byte] = {
      new ByteInput(new java.io.BufferedInputStream(loadStream(t)))
    }
  }

  implicit def charReader = new StreamReader[Bundle.Url, Char] {
    def input(t: Bundle.Url): Input[Char] = {
      new CharInput(new java.io.InputStreamReader(loadStream(t)))
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
      val is = new BufferedInputStream(self.loadStream(file(name)))
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
    def fullname = packages.reverse.mkString(".")
    def get: Class[_] = self.loadClass(fullname)
    def all: Iterable[Class[_]] = self.loadSubclasses(fullname, classOf[AnyRef])
    def subclasses(cls: Class[_]) = self.loadSubclasses(fullname, cls)
  }

  class Url(bundle: Bundle, elements: Seq[String]) extends rapture.io.Url[Url](elements, immutable.Map()) {
    def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) = new Url(bundle, elements)
    def schemeSpecificPart = elements.mkString("//", "/", "")
    val pathRoot = bundle
  }

}
