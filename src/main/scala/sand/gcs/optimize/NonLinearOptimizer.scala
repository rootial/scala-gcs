package sand.gcs.optimize

import org.apache.commons.math3.optim.nonlinear.scalar._
import org.apache.commons.math3.analysis._
import org.apache.commons.math3.util.FastMath
import sand.gcs.coordinate.DistanceComputable
import sand.gcs.util.Config.config
import sand.gcs.util.{ DistanceStore, ZeroLoggerFactory }
import scala.util.Random

/** An optimizer for placement of nodes and landmarks
  *
  * Each NonLinearOptimizer implements a different optimization algorithm
  */
trait NonLinearOptimizer[CoordType] {
  /** Require an implicit [[sand.gcs.coordinate.DistanceComputable]] of the type parameter in scope.
    *
    * This essentially puts a context bound on CoordType, allowing
    * for a Typeclass Pattern on a trait. We keep this a trait instead
    * of an abstract class to use the Cake Pattern as well.
    *
    * @return Typeclass implementation of [[sand.gcs.coordinate.DistanceComputable]]
    */
  protected implicit def requireDistanceComputable: DistanceComputable[CoordType]

  // Memoized instance of typeclass implementation
  protected lazy val distanceCalculator = requireDistanceComputable

  /** Optimize a node against a set of landmarks
    *
    * @param neighbors A sequence of pairs of ids and points
    * @param dim The dimension of the space we are working in
    * @return A point who's distance to each point is as close as possible to the distance associated with the point
    */
  protected def optimizeNode(neighbors: IndexedSeq[(Int, CoordType)], id: Int)(implicit store: DistanceStore, dim: Int): CoordType

  /** Optimize a set of landmarks against each other
    *
    * @param ids The ids of landmarks to embed
    * @param store The distance store
    * @param dim The dimension of the space we are working in
    * @return A sequence of points where the distances between any two points is as close as possible to the desired distance
    */
  protected def optimizeLandmarks(ids: IndexedSeq[Int])(implicit store: DistanceStore, dim: Int): Seq[CoordType]

  /** Measure how much distances from a node to landmarks deviate
    *
    * (|estimated - groundtruth| / groundtruth)^2 is the function used for the difference between one node and landmark
    * @param distances The distances between a node and a landmark
    * @param points The actual coordinates of the landmarks
    * @return An ObjectiveFunction to be used with an optimizer
    */
  protected def nodeCostFunction(distances: IndexedSeq[Double], points: IndexedSeq[CoordType]): ObjectiveFunction = {
    new ObjectiveFunction(
      new MultivariateFunction() {
        def value(nodeA: Array[Double]): Double = {
          // the following code should probably be optimized
          val node = distanceCalculator.fromArray(nodeA)

          // compute (|estimated - groundtruth| / groundtruth)^2 - WITH MUTATION
          var sum = 0.0
          var i = 0
          val numberOfDistances = distances.size
          while (i != numberOfDistances) {
            val distance = distances(i)
            sum += FastMath.pow(FastMath.abs(
              distanceCalculator.distanceBetween(node, points(i)) - distance) / distance, 2)
            i += 1
          }
          sum
        }
      })
  }

  /** Measure how much an embeding of landmarks deviates from optimum
    *
    * (|estimated - groundtruth| / groundtruth)^2 is the function used for the difference between one node and landmark
    * @param distances A mapping form pairs of landmark ids to the distances between them
    * @param ids The ids of landmarks to embed
    * @return An ObjectiveFunction to be used with an optimizer
    */
  protected def landmarkCostFunction(ids: IndexedSeq[Int])(implicit store: DistanceStore, dim: Int): ObjectiveFunction = {
    new ObjectiveFunction(
      new MultivariateFunction() {
        def value(params: Array[Double]): Double = {
          var sum = 0.0
          val points = (params.grouped(dim) map { distanceCalculator.fromArray(_) }).toIndexedSeq

          var i = 0
          val numberOfIds = ids.size
          while (i != numberOfIds) {
            var j = i + 1
            while (j != numberOfIds) { // double while loop - BECAUSE WE CAN
              val pointA = ids(i)
              val pointB = ids(j)
              val actual = distanceCalculator.distanceBetween(points(i), points(j))
              val desired = store(pointA, pointB)
              sum += FastMath.pow(FastMath.abs(actual - desired) / desired, 2)
              j += 1
            }
            i += 1
          }
          sum
        }
      })
  }
}

object NonLinearOptimizer {
  val LANDMARK_COORDINATE_BOUND = config.getInt("gcs.embed.landmark-coordinate-bound")
  val NONLANDMARK_COORDINATE_BOUND = config.getInt("gcs.embed.nonlandmark-coordinate-bound")

  /** Get random coordinatesin a particular dimension with specified bound
    *
    * @param bound Random coordinates generated will be in (-bound, bound)
    * @param dim Dimension of the coordinate to return
    * @return Array of length "dim" representing the coordinate
    */
  def randomCoordinates(bound: Int)(implicit dim: Int): Array[Double] =
    Array.fill(dim) { Random.nextDouble * (bound * 2) - bound }
}
