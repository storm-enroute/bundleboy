package org.storm.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
import scala.collection._
import scala.collection.convert.decorateAsScala._
import net.lingala.zip4j.core._
import net.lingala.zip4j.model._
import net.lingala.zip4j.util._



trait Bundle {

  def name: String

  def loadStream(name: String): java.io.InputStream

  def loadClass(name: String): Class[_]

  def loadSubclasses(packageName: String, baseClass: Class[_]): Iterable[Class[_]]

  val files = new Bundle.FilePath(this, Nil)

  val classes = new Bundle.Classes(this, Nil)

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
    private def file(filename: String) = {
      (filename :: folders).reverse.foldLeft("")(_ + "/" + _)
    }
    def stream(name: String) = self.loadStream(file(name))
    def bytes(name: String): Array[Byte] = {
      val is = stream(name)
      try {
        org.apache.commons.io.IOUtils.toByteArray(is)
      } finally {
        is.close()
      }
    }
    def text(name: String): String = {
      val is = stream(name)
      try {
        org.apache.commons.io.IOUtils.toString(is, "UTF-8")
      } finally {
        is.close()
      }
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

}
