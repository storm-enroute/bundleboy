package org.stormenroute.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
import scala.collection._
import scala.collection.convert.decorateAsScala._
import org.stormenroute.zip4j.core._
import org.stormenroute.zip4j.model._
import org.stormenroute.zip4j.util._



class EncryptedZipBundle(val name: String, val filename: String, val password: String)
extends Bundle {
  def this(name: String, file: File, password: String) = this(name, file.toString, password)

  val classloader = new EncryptedZipBundle.ZipClassLoader(filename, password)

  def loadPaths(name: String) = classloader.loadPaths(name)

  def loadClass(name: String) = classloader.loadClass(name)

  def loadSubclasses(packageName: String, baseClass: Class[_]) = classloader.loadSubclasses(packageName, baseClass)

  def loadStream(path: String) = classloader.getResourceAsStream(path)
}


object EncryptedZipBundle {

  def fromFolder(files: Seq[File], zipName: String, passwordOpt: Option[String]) {
    val zipfile = new ZipFile(zipName, passwordOpt.orNull)
    val params = new ZipParameters
    params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE)
    params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL)
    
    for (password <- passwordOpt) {
      params.setEncryptFiles(true)
      params.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES)
      params.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256)
      params.setPassword(password)
    }

    for (f <- files) zipfile.addFolder(f, params)
  }

  class ZipClassLoader(filename: String, password: String)
  extends ClassLoader(Bundle.getClass.getClassLoader) {
    val zipfile = new ZipFile(filename, password)
    val root = Package("")
    val packages = mutable.Map[String, Package]()
    var buffer: Array[Byte] = new Array[Byte](1024)

    def loadPaths(name: String) = for {
      fraw <- zipfile.getFileHeaders.asScala
      fh = fraw.asInstanceOf[FileHeader]
      if fh.getFileName.startsWith(name)
    } yield {
      fh.getFileName
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
    if (zipfile.isEncrypted) zipfile.setPassword(password)
    for (header <- zipfile.getFileHeaders.asScala) {
      val name = header.asInstanceOf[FileHeader].getFileName
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

