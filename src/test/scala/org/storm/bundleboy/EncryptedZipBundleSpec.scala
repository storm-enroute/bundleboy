package org.storm.bundleboy



import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import rapture.io._



class EncryptedZipBundleSpec extends FlatSpec with ShouldMatchers {
  
  "EncryptedClassLoader" should "load the class correctly" in {
    val loader = new Bundle.EncryptedClassLoader("resources/example-classes.bundle", "password")
    val cls = loader.findClass("org.storm.bundleboy.TestExampleClass")
    val inst = cls.newInstance
    val string = cls.getMethod("someString").invoke(inst)
    string should equal ("test string")
  }

  "Bundle.EncryptedZip" should "fetch the resource correctly" in {
    val bundle = new Bundle.EncryptedZip("testBundle", "resources/example-classes.bundle", "password")
    implicit val utf8 = Encodings.`UTF-8`
    implicit val reader = bundle.charReader
    val contents = (bundle / "resources" / "test.txt").slurp[Char]
    assert(contents == "This is a test file.\n")
  }

}
