package sand.gcs.system.distributed

import sand.gcs.system.Orion
import sand.gcs.util.Logger._
import sand.gcs.util.MapStore
import scala.Console.err
import scala.collection.mutable
import scala.io.Source

/** Container for the command lind tool interface for Distributed Orion GCS
  *
  * Before calling this interface, make sure every Worker machine specified
  * in the configuration file under gcs.deploy.active has a
  * [[sand.gcs.system.distributed.Worker]] instance loaded up. This instance
  * needs to be loaded up before the system can spawn Workers and load the
  * GCS on remote machines.
  */
object DistributedOrion {
  /** Command line tool interface for Distributed Orion GCS.
    *
    * @param args Holds the three parameters for the CLI - distance, dimension, and output filename. An optional fourth parameter may be provided, a file with a single column denoting the primary landmarks to embed first.
    */
  def main(args: Array[String]) {
    if (args.size < 3) {
      err.println("Parameters: [distance] [dimension] [output filename] [Optional: list of primaries]")
      return
    }

    val inputFilename = args(0)
    val dimension = args(1).toInt
    val outputFilename = args(2)
    val primariesFile = if (args.length == 4) Some(args(3)) else None

    implicit val distances = new MapStore()
    val landmarks = mutable.Set[Int]()

    Source.fromFile(inputFilename).getLines().
      map(_.split("\\s+")) foreach { linesplit =>
        val (source, destination, distance) =
          (linesplit(0).toInt, linesplit(1).toInt, linesplit(2).toDouble)
        distances(source, destination) = distance
        landmarks += source
      }

    val primaries = primariesFile.map { filename =>
      Source.fromFile(filename).
        getLines().
        map(_.split("\\s+")).
        map(_(0).toInt).
        toSet
    }

    val orion = new Orion(dimension)
    logger.info("Embedding landmarks...")
    orion.embedLandmarks(landmarks.toSet, primaries)

    val nodes = (distances.ids -- landmarks).toVector
    logger.info("Distributing workload...")
    DistributedGCS.execute(orion, distances, nodes, outputFilename)
  }
}
