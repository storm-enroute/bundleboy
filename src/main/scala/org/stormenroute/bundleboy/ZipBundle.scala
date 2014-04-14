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

  class ZipClassLoader(val filename: String, val passwordProvider: () => Array[Char])
  extends BundleLoader(Bundle.getClass.getClassLoader) {
    lazy val zipfile = new ZipFile(filename)

    def allEntries = {
      zipfile.entries.asScala.toSeq.map(_.getName)
    }

    def inputStream(path: String) = {
      val ze = zipfile.getEntry(path)
      zipfile.getInputStream(ze)
    }

  }

}

