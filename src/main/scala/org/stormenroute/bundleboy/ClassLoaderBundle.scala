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



class ClassLoaderBundle(classloader: ClassLoader)
extends Bundle {
  def name = s"ClassLoaderBundle($classloader)" 

  def loadPaths(path: String) = throw new UnsupportedOperationException

  def loadClass(name: String) = classloader.loadClass(name)

  def loadSubclasses(packageName: String, baseClass: Class[_]) = throw new UnsupportedOperationException

  def loadStream(path: String) = classloader.getResourceAsStream(path)
}


object ClassLoaderBundle

