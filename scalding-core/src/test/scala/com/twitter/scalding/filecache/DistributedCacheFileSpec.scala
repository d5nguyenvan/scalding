package com.twitter.scalding.filecache

import cascading.tuple.Tuple
import com.twitter.scalding._
import java.io.File
import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.specs.Specification
import org.specs.mock.Mockito
import scala.collection.mutable


class DistributedCacheFileSpec extends Specification with Mockito {
  case class UnknownMode(buffers: Map[Source, mutable.Buffer[Tuple]]) extends TestMode with CascadingLocal

  val conf = smartMock[Configuration]

  lazy val hdfsMode = {
    val mode = smartMock[Hdfs]
    mode.conf returns conf
    mode.strict returns true
    mode
  }

  lazy val hadoopTestMode = {
    val mode = smartMock[HadoopTest]
    mode.conf returns conf
    mode
  }

  lazy val testMode = smartMock[Test]
  lazy val localMode = smartMock[Local]

  val uriString = "hdfs://foo.example:1234/path/to/the/stuff/thefilename.blah"
  val uri = new URI(uriString)
  val hashHex = URIHasher(uri)
  val hashedFilename = "thefilename.blah-" + hashHex

  "DistributedCacheFile" should {
    "symlinkNameFor must return a hashed name" in {
      DistributedCacheFile.symlinkNameFor(uri) must_== hashedFilename
    }
  }

  "UncachedFile.add" should {
    val dcf = new UncachedFile(Right(uri))

    def sharedLocalBehavior(implicit mode: Mode) = {
      "use the local file path" in {
        val cf = dcf.add()(mode)

        cf.path must_== uri.getPath
        cf.file must_== new File(uri.getPath).getCanonicalFile
      }
    }

    "with a Test mode" in {
      sharedLocalBehavior(testMode)
    }

    "with a Local mode" in {
      sharedLocalBehavior(localMode)
    }

    "throw RuntimeException when the current mode isn't recognized" in {
      val mode = smartMock[UnknownMode]
      dcf.add()(mode) must throwA[RuntimeException]
    }
  }
}