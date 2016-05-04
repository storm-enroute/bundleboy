package org.stormenroute.bundleboy



import scala.language.dynamics
import java.net._
import java.io._
import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.zip._



class ZipBundleCreator extends Bundle.Creator {
  def fromFolders(
    files: Seq[File], zipName: String, passwordProvider: () => Array[Char]
  ) {
    var fos: FileOutputStream = null
    var zipos: ZipOutputStream = null
    val buffer = new Array[Byte](4096)
    try {
      fos = new FileOutputStream(zipName)
      zipos = new ZipOutputStream(fos)

      def addFolder(basePath: String, dir: File) {
        val files = dir.listFiles()
        for (file <- files) {
          if (file.isDirectory) {
            val path = basePath + file.getName() + "/"
            zipos.putNextEntry(new ZipEntry(path))
            addFolder(path, file)
            zipos.closeEntry()
          } else {
            var fin: FileInputStream = null
            try {
              fin = new FileInputStream(file)
              zipos.putNextEntry(new ZipEntry(basePath + file.getName()))
              var length = fin.read(buffer)
              while (length > 0) {
                zipos.write(buffer, 0, length)
                length = fin.read(buffer)
              }
              zipos.closeEntry()
            } finally {
              if (fin != null) fin.close()
            } 
        }
    }
}
      for (f <- files) addFolder(f.getName + "/", f)
    } finally {
      if (zipos != null) zipos.close()
      if (fos != null) fos.close()
    }
  }
}


