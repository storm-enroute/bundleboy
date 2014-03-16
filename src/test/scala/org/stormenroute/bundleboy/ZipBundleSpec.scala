package org.stormenroute.bundleboy



import org.scalatest._



class ZipBundleSpec extends FlatSpec with Matchers {
  
  val testTxtContents = "This is a test file.\n"

  "ZipBundle.ClazzLoader" should "load the class correctly" in {
    val loader = new ZipBundle.ZipClassLoader("resources/example-classes.bundle", () => "".toCharArray)
    val cls = loader.findClass("org.storm.bundleboy.TestExampleClass")
    val inst = cls.newInstance
    val string = cls.getMethod("someString").invoke(inst)
    string should equal ("test string")
  }

  "ZipBundle" should "fetch the resources correctly" in {
    val bundle = new ZipBundle("testBundle", "resources/example-classes.bundle", () => "".toCharArray)

    val textContents = bundle.files.docs.text("test.txt")
    assert(textContents == testTxtContents)

    val image = bundle.files.imgs.image("ski.png")
    assert(image != null)
  }

  it should "instantiate the class object" in {
    val bundle = new ZipBundle("testBundle", "resources/example-classes.bundle", () => "".toCharArray)
    val cls = bundle.classes.org.storm.bundleboy.TestExampleClass.get
    val inst = cls.newInstance
    val string = cls.getMethod("someString").invoke(inst)
    string should equal ("test string")
  }

  it should "retrieve all the classes in a package" in {
    val bundle = new ZipBundle("testBundle", "resources/example-classes.bundle", () => "".toCharArray)
    val cs = bundle.classes.org.storm.bundleboy.all
    cs.map(_.getSimpleName) should equal (Set("TestExampleClass", "TestExampleClass2"))
    val cs2 = bundle.classes.org.storm.all
    cs2.map(_.getSimpleName) should equal (Set("TestExampleClass", "TestExampleClass2", "TestExampleClassAbove"))
  }

  it should "retrieve all the subclasses in a package" in {
    val bundle = new ZipBundle("testBundle", "resources/example-classes.bundle", () => "".toCharArray)
    val cs = bundle.classes.org.storm.bundleboy.subclasses(classOf[Serializable])
    cs.map(_.getSimpleName) should equal (Set("TestExampleClass"))
  }

}

// (new ZipBundleCreator).fromFolders(List("resources/docs", "resources/org", "resources/imgs").map(x => new java.io.File(x)), "example-classes.bundle", () => "".toCharArray)
// (new ZipBundleCreator).fromFolders(List("resources/docs2").map(x => new java.io.File(x)), "example-classes-2.bundle", () => "".toCharArray)
