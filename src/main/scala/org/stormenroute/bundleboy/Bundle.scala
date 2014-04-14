package org.stormenroute.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
import scala.collection._
import scala.collection.convert.decorateAsScala._



trait Bundle {

  def name: String

  def loadStream(name: String): java.io.InputStream

  def loadPaths(path: String): Iterable[String]

  def loadClass(name: String): Class[_]

  def loadSubclasses(packageName: String, baseClass: Class[_]): Iterable[Class[_]]

  val files = new Bundle.FilePath(this, Nil)

  val classes = new Bundle.Classes(this, Nil)

}


object Bundle extends BundleApi {

  trait Creator {
    def fromFolders(files: Seq[File], zipName: String, passwordProvider: () => Array[Char]): Unit
  }

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
      (filename :: folders).reverse.mkString("/")
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
    def all(): Iterable[String] = self.loadPaths(file(""))
  }

  class Classes(val self: Bundle, val packages: List[String]) extends Dynamic {
    def selectDynamic(name: String) = new Classes(self, name :: packages)
    def fullname = packages.reverse.mkString(".")
    def get: Class[_] = self.loadClass(fullname)
    def all: Iterable[Class[_]] = self.loadSubclasses(fullname, classOf[AnyRef])
    def subclasses(cls: Class[_]) = self.loadSubclasses(fullname, cls)
  }

  class Container(val name: String) extends Bundle {
    val bundles = mutable.Set[Bundle]()
    val mapping = mutable.Map[String, Bundle]()

    def +=(b: Bundle) = if (!bundles.contains(b)) {
      bundles += b
      for (f <- b.loadPaths(""); if !mapping.contains(f)) mapping(f) = b
    }

    def loadStream(name: String) = mapping.get(name) match {
      case Some(b) => b.loadStream(name)
      case None => null
    }

    def loadPaths(path: String) = bundles.foldLeft(Iterable[String]())(_ ++ _.loadPaths(path)).toSeq.toSeq
    
    def loadClass(name: String) = mapping.get(name.replace(".", "/") + ".class") match {
      case Some(b) => b.loadClass(name)
      case None => throw new ClassNotFoundException(name)
    }
    
    def loadSubclasses(packageName: String, baseClass: Class[_]) = {
      val cs = for (b <- bundles) yield b.loadSubclasses(packageName, baseClass)
      cs.flatten
    }
  }

  object Container {
    def apply(name: String) = new Container(name)
  }

  object Clazzloader {
    def apply(classloader: ClassLoader) = new ClassLoaderBundle(classloader)
  }

  object Zip {
    def apply(name: String, filename: String) = new ZipBundle(name, filename, () => null)
    def apply(name: String, file: File) = new ZipBundle(name, file, () => null)
  }

}
