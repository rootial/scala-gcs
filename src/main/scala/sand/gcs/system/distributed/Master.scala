package sand.gcs.system.distributed

import akka.actor.{ Actor, ActorLogging, ActorRef, Address, PoisonPill, Terminated }
import akka.remote.{ RemoteClientLifeCycleEvent, RemoteClientShutdown }
import sand.gcs.system.distributed.Worker._
import sand.gcs.system.GraphCoordinateSystem
import scala.collection.mutable

object Master {
  case class DistributeWork(nodeId: Int)
  case object AllWorkSent

  case class WorkerCreated(worker: ActorRef)
  case class WorkerRequestsWork(worker: ActorRef)
  case class WorkIsDone[CoordType](worker: ActorRef, result: (Option[(Int, CoordType)]))
}

class Master[CoordType](gcs: GraphCoordinateSystem[CoordType], outputFilename: String)
    extends Actor with ActorLogging {
  import Master._

  val workers = mutable.Map.empty[ActorRef, Option[Int]]
  val workQ = mutable.Queue.empty[Int]

  var allWorkSent = false
  var inShutdownSequence = false

  def checkIfAllWorkIsFinished() {
    val noneWorking = workers.forall(_._2 == None)
    if (allWorkSent && noneWorking && workQ.isEmpty) {
      inShutdownSequence = true
      workers.foreach(_._1 ! PoisonPill)
      self.tell(PoisonPill, self)
      gcs.save(outputFilename)
    }
  }

  def killAllWorkersAtAddress(addr: Address) {
    log.error("Worker(s) at {} has (have) died.", addr)

    def hasAddress(worker: ActorRef): Boolean = worker.path.address == addr

    workers.filter(p => hasAddress(p._1)).foreach { workerWorkPair =>
      val worker = workerWorkPair._1
      if (workers.contains(worker) && workers(worker) != None) {
        val work = workers(worker).get
        self.tell(DistributeWork(work), self)
      }
      workers -= worker
    }
  }

  def notifyWorkers() {
    if (!workQ.isEmpty) {
      workers.foreach {
        case (worker, m) if (m.isEmpty) => worker ! WorkIsReady
        case _ =>
      }
    }
  }

  override def receive = {
    case WorkerCreated(worker) =>
      log.info("Worker created: {}", worker)
      context.watch(worker)
      workers += (worker -> None)
      notifyWorkers()

    case WorkerRequestsWork(worker) =>
      if (workers.contains(worker)) {
        if (workQ.isEmpty)
          worker ! NoWorkToBeDone
        else if (workers(worker) == None) {
          val work = workQ.dequeue()
          workers += (worker -> Some(work))
          worker ! WorkToBeDone(work)
        }
      }

    case WorkIsDone(worker, result) =>
      if (!workers.contains(worker)) {
        log.error("Unregistered worker {} tried to return finished work!", worker)
      } else {
        result match {
          case Some((id, coordinates)) =>
            gcs.storeCoordinate(id, coordinates.asInstanceOf[CoordType], false)
          case None =>
        }
        workers += (worker -> None)
        checkIfAllWorkIsFinished()
      }

    case Terminated(worker) =>
      if (workers.contains(worker) && workers(worker) != None && !inShutdownSequence) {
        log.error("Worker {} died while processing {}.", worker, workers(worker))
        val work = workers(worker).get
        self.tell(DistributeWork(work), self)
      }
      workers -= worker

    case RemoteClientShutdown(_, addr) =>
      if (!inShutdownSequence) killAllWorkersAtAddress(addr)

    case DistributeWork(work) =>
      workQ.enqueue(work)
      notifyWorkers()

    case AllWorkSent => allWorkSent = true

    case _: RemoteClientLifeCycleEvent =>

    case badMessage => log.error("Bad message received: {}", badMessage)
  }
}
