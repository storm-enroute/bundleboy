package org.stormenroute.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import java.util.jar.JarFile
import scala.collection._
import scala.collection.convert.decorateAsScala._



class ClassPathBundle(classloader: ClassLoader)
extends Bundle {
  def name = s"ClassPathBundle($classloader)" 

  def loadPaths(path: String) = throw new UnsupportedOperationException

  def loadClass(name: String) = classloader.loadClass(name)

  def loadSubclasses(packageName: String, baseClass: Class[_]) = throw new UnsupportedOperationException

  def loadStream(path: String) = classloader.getResourceAsStream(path)
}
