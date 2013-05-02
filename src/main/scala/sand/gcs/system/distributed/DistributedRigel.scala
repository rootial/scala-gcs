package sand.gcs.system.distributed

import sand.gcs.system.Rigel
import sand.gcs.util.Logger._
import sand.gcs.util.MapStore
import scala.Console.err
import scala.collection.mutable
import scala.io.Source

/** Container for the command lind tool interface for Distributed Rigel GCS
  *
  * Before calling this interface, make sure every Worker machine specified
  * in the configuration file under gcs.deploy.active has a
  * [[sand.gcs.system.distributed.Worker]] instance loaded up. This instance
  * needs to be loaded up before the system can spawn Workers and load the
  * GCS on remote machines.
  */
object DistributedRigel {
  /** Command line tool interface for Rigel GCS.
    *
    * @param args Holds the four parameters for the CLI - distance, dimension, output filename, and curvature. An optional fifth parametes may be provided, which is a file containins a single column denoting the primary landmarks to embed first.
    */
  def main(args: Array[String]) {
    if (args.size < 4) {
      err.println("Parameters: [distance] [dimension] [output filename] [curvature] [Optional: list of primaries]")
      return
    }

    val inputFilename = args(0)
    val dimension = args(1).toInt
    val outputFilename = args(2)
    val curvature = args(3).toInt
    val primariesFile = if (args.length == 5) Some(args(4)) else None

    implicit val distances = new MapStore()
    val landmarks = mutable.Set.empty[Int]

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

    val rigel = new Rigel(dimension, curvature)
    logger.info("Embedding landmarks...")
    rigel.embedLandmarks(landmarks.toSet, primaries)

    val nodes = (distances.ids -- landmarks).toVector
    logger.info("Distributing workload...")
    DistributedGCS.execute(rigel, distances, nodes, outputFilename)
  }
}
