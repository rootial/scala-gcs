package sand.gcs.system.distributed

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef, ActorSystem, Props }
import akka.pattern.pipe
import akka.remote.RemoteClientLifeCycleEvent
import com.typesafe.config.ConfigFactory
import sand.gcs.system.distributed.Master._
import sand.gcs.system.distributed.Reaper._
import sand.gcs.system.GraphCoordinateSystem
import sand.gcs.util.Config.config
import sand.gcs.util.DistanceStore
import java.net.InetAddress
import scala.Console.err
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.io.Source
import scala.util.{ Failure, Success, Try }

object Worker {
  case class WorkToBeDone(nodeId: Int)
  case object WorkIsReady
  case object NoWorkToBeDone

  private val workerAkkaConfig = ConfigFactory.parseString("""
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        netty {
          hostname = """ + "\"" + InetAddress.getLocalHost().getHostName() + "\"" + """
          port = 2552
        }
      }
    }
  """)
  def main(args: Array[String]) {
    val availableProcessors = Runtime.getRuntime().availableProcessors()
    val hostname = InetAddress.getLocalHost().getHostName()

    val numberOfThreads = config.getInt("gcs.deploy." + hostname + ".nr-of-workers")

    val system = ActorSystem("Worker", ConfigFactory.load(workerAkkaConfig))
    val reaper = system.actorOf(Props[Reaper], "reaper")
  }
}

class Worker[CoordType](
  master: ActorRef,
  reaper: ActorRef,
  gcs: GraphCoordinateSystem[CoordType],
  distanceStore: DistanceStore)
    extends Actor with ActorLogging {
  import Worker._
  case class WorkComplete[CoordType](result: Option[(Int, CoordType)])

  def doWork(nodeId: Int) {
    future {
      val result = Try {
        gcs.embedNonLandmark(nodeId)(distanceStore) /* Embeds and returns coordinates */
      }
      result match {
        case Success(res) =>
          WorkComplete(Some((nodeId, res)))
        case Failure(exception) =>
          log.error("Input {} resulted in exception {}.", nodeId, exception.getMessage())
          WorkComplete(None)
      }
    } pipeTo self
  }

  override def preStart() = {
    reaper ! WatchMe(self)
    master ! WorkerCreated(self)
  }

  def working: Receive = {
    case WorkIsReady =>
    case NoWorkToBeDone =>
    case WorkToBeDone =>
      log.error("Received work while working.")
    case WorkComplete(result) =>
      master ! WorkIsDone(self, result)
      master ! WorkerRequestsWork(self)
      context.become(idle)
  }

  def idle: Receive = {
    case WorkIsReady =>
      master ! WorkerRequestsWork(self)
    case WorkToBeDone(work) =>
      val resultHandler = sender
      doWork(work)
      context.become(working)
    case NoWorkToBeDone =>
  }

  override def receive = idle
}
