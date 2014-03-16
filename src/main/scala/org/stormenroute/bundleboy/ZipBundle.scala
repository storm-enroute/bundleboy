package org.stormenroute.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.zip._



class ZipBundle(val name: String, val filename: String, val passwordProvider: () => Array[Char])
extends Bundle {
  def this(name: String, file: File, passwordProvider: () => Array[Char]) = this(name, file.toString, passwordProvider)

  val classloader = new ZipBundle.ZipClassLoader(filename, passwordProvider)

  def loadPaths(name: String) = classloader.loadPaths(name)

  def loadClass(name: String) = classloader.loadClass(name)

  def loadSubclasses(packageName: String, baseClass: Class[_]) = classloader.loadSubclasses(packageName, baseClass)

  def loadStream(path: String) = classloader.getResourceAsStream(path)
}


object ZipBundle {

  class ZipClassLoader(filename: String, passwordProvider: () => Array[Char])
  extends ClassLoader(Bundle.getClass.getClassLoader) {
    val zipfile = new ZipFile(filename)
    val root = Package("")
    val packages = mutable.Map[String, Package]()
    var buffer: Array[Byte] = new Array[Byte](1024)

    def loadPaths(name: String) = for {
      ze <- zipfile.entries.asScala.toSeq
      if ze.getName.startsWith(name)
    } yield {
      ze.getName
    }

    case class Package(name: String) {
      val classes = mutable.Set[String]()
      val children = mutable.Set[Package]()
    }

    private def addPackages(parent: Package, ps: Array[String]): Unit = if (ps.nonEmpty) {
      val currentName = if (parent == root) ps.head else parent.name + "." + ps.head
      if (!packages.contains(currentName)) {
        packages(currentName) = Package(currentName)
        parent.children += packages(currentName)
      }
      val current = packages(currentName)
      addPackages(current, ps.tail)
    }

    packages(root.name) = root
    for (ze <- zipfile.entries.asScala) {
      val name = ze.getName
      if (name.endsWith(".class")) {
        val path = name.substring(0, name.length - 6)
        val parts = path.split("/")
        addPackages(root, parts.init)
        val pname = parts.init.mkString(".")
        val cname = parts.last
        val p = packages(pname)
        p.classes += pname + "." + cname
      }
    }

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
        val ze = zipfile.getEntry(path)
        zipfile.getInputStream(ze)
      }
    }

    override def findClass(name: String) = {
      val path = name.replace('.', '/') + ".class"
      val ze = zipfile.getEntry(path)
      val is = zipfile.getInputStream(ze)
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

    def loadSubclasses(packageName: String, baseClass: Class[_]) = {
      def classes(p: Package): Iterable[Class[_]] = {
        val cs = for (cname <- p.classes) yield loadClass(cname)
        val subcs = for (cp <- p.children) yield classes(cp)
        cs ++ subcs.flatten
      }
      val cs = classes(packages(packageName))
      cs.filter(c => baseClass.isAssignableFrom(c))
    }

    private def getBundleResource(name: String) = {
      val encoded = URLEncoder.encode(name, "UTF-8")
      new URL("bundleboy:file:", "", -1, filename + "?" + encoded, BundleURLStreamHandler)
    }

    override def findResource(name: String) = {
      val url = super.findResource(name)
      if (url != null) url
      else getBundleResource(name)
    }
  }

}

