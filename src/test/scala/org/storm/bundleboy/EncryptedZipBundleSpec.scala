package org.storm.bundleboy



import org.scalatest._
import org.scalatest.matchers.ShouldMatchers



class EncryptedZipBundleSpec extends FlatSpec with ShouldMatchers {
  
  val testTxtContents = "This is a test file.\n"

  "EncryptedClassLoader" should "load the class correctly" in {
    val loader = new Bundle.EncryptedClassLoader("resources/example-classes.bundle", "password")
    val cls = loader.findClass("org.storm.bundleboy.TestExampleClass")
    val inst = cls.newInstance
    val string = cls.getMethod("someString").invoke(inst)
    string should equal ("test string")
  }

  "Bundle.EncryptedZip" should "fetch the resource correctly" in {
    val bundle = new Bundle.EncryptedZip("testBundle", "resources/example-classes.bundle", "password")
    import bundle.charReader
    import Encodings.`UTF-8`
    val contents = (bundle / "docs" / "test.txt").slurp[Char]
    assert(contents == testTxtContents)
  }

  it should "fetch the resources in a dynamic way" in {
    val bundle = new Bundle.EncryptedZip("testBundle", "resources/example-classes.bundle", "password")
    val contents = bundle.files.docs.file("test.txt")
    import bundle.charReader
    import Encodings.`UTF-8`
    assert(contents.slurp[Char] == testTxtContents)

    val textContents = bundle.files.docs.text("test.txt")
    assert(textContents == testTxtContents)

    val image = bundle.files.imgs.image("ski.png")
    assert(image != null)
  }

}
