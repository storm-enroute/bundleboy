package org.storm.bundleboy



import org.scalatest._
import org.scalatest.matchers.ShouldMatchers



class EncryptedZipBundleSpec extends FlatSpec with ShouldMatchers {
  
  "EncryptedClassLoader" should "load the class correctly" in {
    val loader = new Bundle.EncryptedClassLoader("resources/example-classes.bundle", "password")
    val cls = loader.findClass("org.storm.bundleboy.TestExampleClass")
    val inst = cls.newInstance
    val string = cls.getMethod("someString").invoke(inst)
    string should equal ("test string")
  }

  "Bundle.EncryptedZip" should "fetch the resource correctly" in {
    
  }

}
