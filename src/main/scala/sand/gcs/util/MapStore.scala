package sand.gcs.util

import scala.collection.mutable

/** Implementation of [[sand.gcs.util.DistanceStore]] backed by a Map. */
class MapStore extends DistanceStore {
  private val store = mutable.Map.empty[(Int, Int), Double]

  override def apply(source: Int, destination: Int): Double =
    store((source, destination))

  def get(source: Int, destination: Int): Option[Double] = {
    store.get((source, destination))
  }

  override def update(source: Int, destination: Int, distance: Double) = {
    store((source, destination)) = distance
  }

  override def ids: Set[Int] = {
    val ids = mutable.Set.empty[Int]
    store.keysIterator foreach {
      case (source, destination) =>
        ids += source
        ids += destination
    }
    ids.toSet
  }
}
