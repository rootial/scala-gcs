package sand.gcs.optimize

import org.apache.commons.math3.analysis._
import org.apache.commons.math3.optim._
import org.apache.commons.math3.optim.nonlinear.scalar._
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv._
import org.apache.commons.math3.random._
import sand.gcs.util.Config.config
import sand.gcs.util.DistanceStore
import sand.gcs.util.Logger._
import scala.util.Random

/** This optimizer uses the NelderMead simplex method of optimization
  *
  * Convenience wrapper trait around Apache's implementation of
  * the Nelder Mead optimizer (aka Downhill Simplex or Amoeba).
  *
  * The type parameter passed into the trait will need to have a typeclass implementation
  * of [[sand.gcs.coordinate.DistanceComputable]] in scope upon instantiation.
  *
  * @tparam CoordType A type that can be used as a coordinate via [[sand.gcs.coordinate.DistanceComputable]]
  */
trait NelderMead[CoordType] extends NonLinearOptimizer[CoordType] {
  import NelderMead._
  import NonLinearOptimizer._

  override def optimizeNode(neighbors: IndexedSeq[(Int, CoordType)], id: Int)(implicit store: DistanceStore, dim: Int): CoordType = {
    val (ids, points) = neighbors.unzip
    val distances = ids map { store(_, id) }

    val simplex = new NelderMeadSimplex(dim, NONLANDMARK_SIDE_LENGTH)

    val func = nodeCostFunction(distances, points)

    val initialPoint = randomCoordinates(NONLANDMARK_COORDINATE_BOUND)

    val optimizer = new MultiStartMultivariateOptimizer(
      new SimplexOptimizer(
        new SimpleValueChecker(RELATIVE_THRESHOLD,
          ABSOLUTE_THRESHOLD,
          MAX_NONLANDMARK_ITERATIONS)),
      NONLANDMARK_RESTARTS,
      new UncorrelatedRandomVectorGenerator(
        Array.fill(dim) { NONLANDMARK_RESTART_DISTANCE },
        Array.fill(dim) { NONLANDMARK_RESTART_STD },
        new GaussianRandomGenerator(new MersenneTwister())))

    val optimum = optimizer.optimize(
      MaxEval.unlimited(), // #yolo
      new InitialGuess(initialPoint.toArray),
      SimpleBounds.unbounded(dim),
      GoalType.MINIMIZE,
      simplex,
      func)
    logger.fine("Non-primary landmark error: " + optimum.getValue())
    distanceCalculator.fromArray(optimum.getPoint())
  }

  override def optimizeLandmarks(ids: IndexedSeq[Int])(implicit store: DistanceStore, dim: Int): Seq[CoordType] = {
    val dimensionality = ids.size
    val fakeDimension = dim * dimensionality
    val simplex = new NelderMeadSimplex(fakeDimension, LANDMARK_SIDE_LENGTH)

    val func = landmarkCostFunction(ids)

    val initialPoint = randomCoordinates(LANDMARK_COORDINATE_BOUND)(fakeDimension)

    val optimizer = new SimplexOptimizer(
      new SimpleValueChecker(RELATIVE_THRESHOLD, ABSOLUTE_THRESHOLD, MAX_LANDMARK_ITERATIONS))

    val initialOptimum = optimizer.optimize(
      MaxEval.unlimited(), // #yolo
      new InitialGuess(initialPoint.toArray),
      SimpleBounds.unbounded(fakeDimension),
      GoalType.MINIMIZE,
      simplex,
      func)

    def restart(previous: PointValuePair, times: Int): PointValuePair =
      if (times == 0) previous
      else {
        val newSimplex = new NelderMeadSimplex(fakeDimension, LANDMARK_SIDE_LENGTH)
        val startingPoint = previous.getPoint map {
          _ + Random.nextDouble * 2 * LANDMARK_RESTART_DISTANCE - LANDMARK_RESTART_DISTANCE
        }
        val newOptimum = optimizer.optimize(simplex, new InitialGuess(startingPoint)) // previous parameters are remembered
        logger.fine("Restart error: " + newOptimum.getValue)
        if (newOptimum.getValue < previous.getValue) restart(newOptimum, times - 1)
        else restart(previous, times - 1)
      }

    val best = restart(initialOptimum, LANDMARK_RESTARTS)
    logger.fine("Aggregate landmark error: " + best.getValue())
    best.getPoint().grouped(dim).toSeq.map(arr => distanceCalculator.fromArray(arr))
  }
}

object NelderMead {
  val ABSOLUTE_THRESHOLD = config.getDouble("gcs.embed.nelder-mead.absolute-threshold")
  val LANDMARK_RESTARTS = config.getInt("gcs.embed.nelder-mead.landmarks.restarts")
  val LANDMARK_RESTART_DISTANCE = config.getDouble("gcs.embed.nelder-mead.landmarks.restart-distance")
  val LANDMARK_SIDE_LENGTH = config.getDouble("gcs.embed.nelder-mead.landmarks.side-length")
  val MAX_LANDMARK_ITERATIONS = config.getInt("gcs.embed.nelder-mead.landmarks.max-iterations")
  val MAX_NONLANDMARK_ITERATIONS = config.getInt("gcs.embed.nelder-mead.nonlandmarks.max-iterations")
  val NONLANDMARK_RESTARTS = config.getInt("gcs.embed.nelder-mead.nonlandmarks.restarts")
  val NONLANDMARK_RESTART_DISTANCE = config.getDouble("gcs.embed.nelder-mead.nonlandmarks.restart-distance")
  val NONLANDMARK_RESTART_STD = config.getDouble("gcs.embed.nelder-mead.nonlandmarks.restart-std")
  val NONLANDMARK_SIDE_LENGTH = config.getDouble("gcs.embed.nelder-mead.nonlandmarks.side-length")
  val RELATIVE_THRESHOLD = config.getDouble("gcs.embed.nelder-mead.relative-threshold")
}
