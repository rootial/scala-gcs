package sand.gcs.coordinate

import org.apache.commons.math3.util.FastMath

/** Representation of a point in an n-dimensional Hyperbolic space.
  *
  * Represents a points in Hyperbolic space - point is represented
  * by the wrapped array. Curvature information is not determined in
  * the class itself - it will be passed into the constructor of the
  * typeclass implementation for sand.gcs.coordinate.DistanceComputable.
  *
  * @param coordinates Representation of the point in array form
  */
class HyperbolicCoordinate(val coordinates: Array[Double]) extends Serializable

object HyperbolicCoordinate {
  /** Typeclass implementation of [[sand.gcs.coordinate.DistanceComputable]]
    * for HyperbolicCoordinate.
    */
  class HyperbolicCoordinateIsDistanceComputable(curvature: Int)
      extends DistanceComputable[HyperbolicCoordinate] {

    /** Computes the distance between two coordinates in the space.
      *
      * Computes the hyperbolic distance between two coordinate in hyperbolic
      * space, with curvature being the value passed into the consttor.
      *
      * @param source Source coordinate to compute distance from
      * @param destination Destination coordinate to compute distance to
      * @return Distance from source to destination
      */
    override def distanceBetween(
      source: HyperbolicCoordinate,
      destination: HyperbolicCoordinate): Double = {

      var tx = 1.0
      var ty = 1.0
      var tt = 0.0
      var i = 0
      val dimensionality = source.coordinates.size
      while (i != dimensionality) {
        val s = source.coordinates(i)
        val d = destination.coordinates(i)
        tx += (s * s)
        ty += (d * d)
        tt += (s * d)
        i += 1
      }
      val t = FastMath.sqrt(tx * ty) - tt
      FastMath.acosh(t) * FastMath.abs(curvature)
    }

    /** Converts the HyperbolicCoordinate to an array form.
      *
      * Used in optimization functions that use the array representation
      * as points in the system.
      *
      * The function should be the inverse of fromArray.
      *
      * The extracted array is a copy of the wrapped array of the class -
      * any mutations done to the array returned will not impact the
      * HyperbolicCoordinate instance.
      *
      * Curvature of the space is not captured in the array representation.
      * Similarly, curvature of the space will not be captured when reading
      * an array back into a HyperbolicCoordinate via fromArray.
      *
      * @param from Coordinate to convert
      * @return Array representation of the point
      */
    override def toArray(from: HyperbolicCoordinate): Array[Double] =
      from.coordinates

    /** Converts an array to the coordinate type.
      *
      * Used in optimization functions that use the array representation
      * as points in the system.
      *
      * The function should be the inverse of toArray.
      *
      * Curvature of the space will not be captured when reading
      * an array back into a HyperbolicCoordinate via fromArray.
      *
      * @param from Array to convert
      * @return Coordinate representation of the array
      */
    override def fromArray(from: Array[Double]): HyperbolicCoordinate =
      new HyperbolicCoordinate(from)
  }

  /** Convenience function to create instances of EuclideanCoordinate without
    * having to invoke "new."
    */
  def apply(coordinates: Array[Double]) =
    new HyperbolicCoordinate(coordinates)
}
