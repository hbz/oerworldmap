import java.net.InetSocketAddress

import play.sbt.PlayRunHook
import sbt._

object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      var watchProcess: Option[Process] = None

      override def beforeStarted(): Unit = {
        Process("./node_modules/.bin/grunt dist", base).run
      }

      override def afterStarted(addr: InetSocketAddress): Unit = {
        watchProcess = Some(Process("./node_modules/.bin/grunt watch", base).run)
      }

      override def afterStopped(): Unit = {
        watchProcess.map(p => p.destroy())
        watchProcess = None
      }
    }

    GruntProcess
  }
}
