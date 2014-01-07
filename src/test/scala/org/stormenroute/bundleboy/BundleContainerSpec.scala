package org.stormenroute.bundleboy



import org.scalatest._
import org.scalatest.matchers.ShouldMatchers



class BundleContainerSpec extends FlatSpec with ShouldMatchers {

  "A Bundle.Container" should "correctly read in multiple resources" in {
    val container = new Bundle.Container("composite")
    container += new EncryptedZipBundle("1", "resources/example-classes.bundle", "password")
    container += new EncryptedZipBundle("2", "resources/example-classes-2.bundle", "password")
    
    container.files.docs2.text("hello.txt") should equal ("Hello world!\n")
    container.files.docs.text("test.txt") should equal ("This is a test file.\n")
  }

}
