package sand.gcs.coordinate

/** Type representing the coordinate type of the GCS.
  *
  * DistanceComputable is intended to be used as a typeclass,
  * representing a type of coordinate for a particular
  * coordinate system (e.g. Euclidean, Hyperbolic).
  */
trait DistanceComputable[A] extends Serializable {
  /** Computes the distance between two coordinates in the space.
    *
    * @param source Source coordinate to compute distance from
    * @param destination Destination coordinate to compute distance to
    * @return Distance from source to destination
    */
  def distanceBetween(source: A, destination: A): Double

  /** Converts the coordinate type to an array form.
    *
    * Used in optimization functions that use the array representation
    * as points in the system.
    *
    * The function should be the inverse of fromArray.
    *
    * The extracted array is a copy of the wrapped array of the class -
    * any mutations done to the array returned will not impact the
    * class instance.
    *
    * @param from Coordinate to convert
    * @return Array representation of the point
    */
  def toArray(from: A): Array[Double]

  /** Converts an array to the coordinate type.
    *
    * Used in optimization functions that use the array representation
    * as points in the system.
    *
    * The function should be the inverse of toArray.
    *
    * @param from Array to convert
    * @return Coordinate representation of the array
    */
  def fromArray(from: Array[Double]): A
}
