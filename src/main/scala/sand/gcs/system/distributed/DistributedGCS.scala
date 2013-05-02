package sand.gcs.system.distributed

import akka.actor.{ Actor, ActorRef, ActorSystem, Address, AddressFromURIString, Deploy, Props }
import akka.remote.{ RemoteClientLifeCycleEvent, RemoteScope }
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import sand.gcs.system.GraphCoordinateSystem
import sand.gcs.system.distributed.Master._
import sand.gcs.system.distributed.Reaper._
import sand.gcs.util.Config.config
import sand.gcs.util.DistanceStore
import scala.Console.err
import scala.collection.JavaConverters._

/** Helper object for distributed implementations of graph coordinate systems. */
object DistributedGCS {
  private val akkaConfig = ConfigFactory.parseString("""
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        netty {
          hostname = """ + "\"" + InetAddress.getLocalHost().getHostName() + "\"" + """
          message-frame-size = 500 MiB
          port = 2554
        }
      }
    }
    """)

  /** Begins executing the graph coordinate system embedding in a distributed environment.
    *
    * Making this call will spawn a "Master" on the same JVM. It is expected that before
    * this call is made, all machines that have been specified in the configuration file
    * list gcs.deploy.active have [[sand.gcs.system.distributed.Worker]] running.
    *
    * Once the Master is spawned, it will begin embedding the landmarks in a single threaded
    * fashion. Once the landmarks are embedded, it will send a message to all Workers which
    * triggers them to spawn Worker actors and load the partially completed GCS in memory.
    * The number of Worker actors spawned is specified in configuration under
    * gcs.deploy.[worker address].nr-of-workers. This means for each worker in the
    * gcs.deploy.active list there must be a corresponding entry in
    * gcs.deploy.[worker address].nr-of-workers.
    *
    * Each Worker will then send a message to the Master notifying the Master of the Worker's
    * existence, and the Master will begin sending the Workers one non-landmark ID at a time.
    *
    * Once all non-landmark ID's have been distributed and the results received, this will
    * trigger the Master to write the result to disk, and gracefully shutdown the entire
    * system.
    *
    * @param gcs Graph Coordinate System instance to embed with
    * @param distanceStore Container that holds the requisite distances to embed
    * @param nodeIds A sequence of non-landmark IDs to embed (the "work")
    * @param outputFilename Name of the file to write the results of the embedding to
    */
  def execute[CoordType](
    gcs: GraphCoordinateSystem[CoordType],
    distanceStore: DistanceStore,
    nodeIds: Seq[Int],
    outputFilename: String) =
    new DistributedGCS(gcs, distanceStore, nodeIds, outputFilename)

  /** Creates a Props object for Akka Actors.
    *
    * Putting this inside the DistributedGCS class may cause it to create a closure
    * which captures the outer scope, including the ActorSystemImpl in Akka, which
    * is not intended to be Serialized. This helper function prevents the creation
    * of a closure.
    */
  private def workerProps[CoordType](
    workerAddress: Address,
    master: ActorRef,
    workerReaper: ActorRef,
    gcs: GraphCoordinateSystem[CoordType],
    distanceStore: DistanceStore) =
    Props(new Worker(master, workerReaper, gcs, distanceStore)).withDeploy(Deploy(scope = RemoteScope(workerAddress)))
}

/** Class that provides an ActorSystem environment and sets the foundation for the system.
  *
  * Upon instantiation, this class will create an ActorSystem, spawn the Master,
  * the Reaper ("kills" Actors after computation is completed) and spawns the Workers
  * on remote machines.
  */
class DistributedGCS[CoordType] private (gcs: GraphCoordinateSystem[CoordType], distanceStore: DistanceStore, nodes: Seq[Int], outputFilename: String) extends Serializable {
  import DistributedGCS._
  val system = ActorSystem("GCS", ConfigFactory.load(akkaConfig))

  val master = system.actorOf(Props(new Master(gcs, outputFilename)), "master")
  val reaper = system.actorOf(Props[Reaper], "reaper")

  reaper ! WatchMe(master)

  system.eventStream.subscribe(master, classOf[RemoteClientLifeCycleEvent])

  err.println("Master deployed on " + InetAddress.getLocalHost().getHostName + " as " + master)

  nodes.map(DistributeWork(_)).foreach(master ! _)
  master ! AllWorkSent

  val workerNodes = config.getStringList("gcs.deploy.active").asScala

  workerNodes.foreach { node =>
    val workerReaper = system.actorFor(s"akka://Worker@$node" + ":2552/user/reaper")
    val workerAddress = AddressFromURIString(s"akka://Worker@$node" + ":2552")
    val numberOfWorkers = config.getInt("gcs.deploy." + node + ".nr-of-workers")

    for (i <- 0 until numberOfWorkers) {
      system.actorOf(workerProps(workerAddress, master, workerReaper, gcs, distanceStore))
    }
  }
}
