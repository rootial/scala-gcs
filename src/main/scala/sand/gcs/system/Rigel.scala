package sand.gcs.system

import sand.gcs.coordinate.DistanceComputable
import sand.gcs.coordinate.HyperbolicCoordinate
import sand.gcs.optimize._
import sand.gcs.util.MapStore
import scala.collection.mutable
import scala.Console.err
import scala.io.Source

/** Implementation of the Rigel GCS (Hyperbolic Space).
  *
  * Implementation of the Rigel graph coordinate system which embeds
  * distances in an n-dimensional Hyperbolic space, using the Nelder-Mead
  * method of optimizing the alignment of nodes.
  *
  * @param dimension Dimension of the system
  * @param curvature Curvature of the hyperbolic space
  */
class Rigel(override val dimension: Int, curvature: Int) extends GraphCoordinateSystem[HyperbolicCoordinate] with NelderMead[HyperbolicCoordinate] {
  override def requireDistanceComputable: DistanceComputable[HyperbolicCoordinate] =
    new HyperbolicCoordinate.HyperbolicCoordinateIsDistanceComputable(curvature)
}

object Rigel {
  /** Command line tool interface for Rigel GCS.
    *
    * @param args Holds the four parameters for the CLI - distance, dimension, output filename, and curvature. An optional fifth parametes may be provided, which is a file containins a single column denoting the primary landmarks to embed first.
    */
  def main(args: Array[String]) {
    if (args.size < 4) {
      err.println("Parameters: [distances] [dimension] [output filename] [curvature] [optional: list of primaries]")
      return
    }

    val inputFilename = args(0)
    val dimension = args(1).toInt
    val outputFilename = args(2)
    val curvature = args(3).toInt
    val primariesFile = if (args.length == 5) Some(args(4)) else None

    val distances = new MapStore()
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
    rigel.embed(distances, landmarks.toSet, primaries)
    rigel.save(outputFilename)
  }
}
