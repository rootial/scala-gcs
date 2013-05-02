package sand.gcs.util

/** Interface trait for a class that contains distances to embed
  *
  * This interface is kept completely abstract so as to be Java-friendly.
  */
trait DistanceStore extends Serializable {
  /** Gets the distance between two points
    *
    * @param source ID of source node to get distance from
    * @param destination ID of destination node to get distance to
    * @return Distance between source and destination
    */
  def apply(source: Int, destination: Int): Double

  /** Gets the distance between two points safely.
    *
    * @param source ID of source node to get distance from
    * @param destination ID of destination node to get distance to
    * @return Distance between source and destination wrapped in an Option
    */
  def get(source: Int, destination: Int): Option[Double]

  /** Adds a new distance to the store or updates if the pair exists already.
    *
    * @param source ID of source node to get distances from
    * @param destination ID of destination node to get distance of
    * @param distance Distance between source and destination
    */
  def update(source: Int, destination: Int, distance: Double)

  /** Returns a list of all IDs contained in the store
    *
    * @return an immutable Set containing the IDs of the nodes in the store
    */
  def ids: Set[Int]
}
