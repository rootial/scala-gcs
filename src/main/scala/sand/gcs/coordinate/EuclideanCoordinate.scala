package sand.gcs.coordinate

import org.apache.commons.math3.util.FastMath
import scala.language.implicitConversions

/** Representation of a point in an n-dimensional Euclidean space
  *
  * Represents a points in Euclidean space - point is represented
  * by the wrapped array.
  *
  * @param coordinates Representation of the point in array form
  */
class EuclideanCoordinate(val coordinates: Array[Double]) extends Serializable

object EuclideanCoordinate {
  /** Typeclass implementation of [[sand.gcs.coordinate.DistanceComputable]]
    * for EuclideanCoordinate.
    */
  implicit object EuclideanCoordinateIsDistanceComputable
      extends DistanceComputable[EuclideanCoordinate] {

    override def distanceBetween(
      source: EuclideanCoordinate,
      destination: EuclideanCoordinate): Double = {

      var sum = 0.0
      var i = 0
      val dimensionality = source.coordinates.size
      while (i != dimensionality) {
        val s = source.coordinates(i)
        val d = destination.coordinates(i)
        sum += FastMath.pow(s - d, 2)
        i += 1
      }

      FastMath.sqrt(sum)
    }

    /** Converts the EuclideanCoordinate to an array form.
      *
      * Used in optimization functions that use the array representation
      * as points in the system.
      *
      * The function should be the inverse of fromArray.
      *
      * The extracted array is a copy of the wrapped array of the class -
      * any mutations done to the array returned will not impact the
      * EuclideanCoordinate instance.
      *
      * @param from Coordinate to convert
      * @return Array representation of the point
      */
    override def toArray(from: EuclideanCoordinate): Array[Double] =
      from.coordinates.map(x => x)

    /** Converts an array to a EuclideanCoordinate.
      *
      * Used in optimization functions that use the array representation
      * as points in the system.
      *
      * The function should be the inverse of toArray.
      *
      * @param from Array to convert
      * @return EuclideanCoordinate representation of the array
      */
    override def fromArray(from: Array[Double]): EuclideanCoordinate =
      new EuclideanCoordinate(from)
  }

  /** Convenience function to create instances of EuclideanCoordinate without
    * having to invoke "new."
    */
  def apply(coordinates: Array[Double]) = new EuclideanCoordinate(coordinates)
}
