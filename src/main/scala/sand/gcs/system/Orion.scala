package sand.gcs.system

import sand.gcs.coordinate.DistanceComputable
import sand.gcs.coordinate.EuclideanCoordinate
import sand.gcs.optimize._
import sand.gcs.util.MapStore
import scala.Console.err
import scala.collection.mutable
import scala.io.Source

/** Implementation of the Orion GCS (Euclidean Space).
  *
  * Implementation of the Orion graph coordinate system which embeds
  * distances in an n-dimensional Euclidean space, using the Nelder-Mead
  * method of optimizing the alignment of nodes.
  *
  * @param dimension Dimension of the system
  */
class Orion(override val dimension: Int) extends GraphCoordinateSystem[EuclideanCoordinate] with NelderMead[EuclideanCoordinate] {
  override def requireDistanceComputable: DistanceComputable[EuclideanCoordinate] = implicitly
}

/** Container for the command line tool interface for Orion GCS. */
object Orion {
  /** Command line tool interface for Orion GCS.
    *
    * @param args Holds the three parameters for the CLI - distance, dimension, and output filename. An optional fourth parameter may be provided, a file with a single column denoting the primary landmarks to embed first.
    */
  def main(args: Array[String]) {
    if (args.size < 3) {
      err.println("Parameters: [distances] [dimension] [output filename] [optional: list of primaries]")
      return
    }

    val inputFilename = args(0)
    val dimension = args(1).toInt
    val outputFilename = args(2)
    val primariesFile = if (args.length == 4) Some(args(3)) else None

    val distances = new MapStore()
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
    orion.embed(distances, landmarks.toSet, primaries)
    orion.save(outputFilename)
  }
}
