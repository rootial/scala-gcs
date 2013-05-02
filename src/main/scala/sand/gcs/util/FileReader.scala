package sand.gcs.util

import scala.io.Source
import scala.collection.mutable.{ Set => MSet }
import scala.collection.JavaConverters._

/** Convenience object to read a file into a data structure */
object FileReader {
  private def _readFile(filename: String, store: DistanceStore): Set[Int] = {
    val contents = Source.fromFile(filename).getLines().
      map(_.split("\\s+")).toVector
    val landmarks = MSet[Int]();

    contents foreach { linesplit =>
      val (source, destination, distance) =
        (linesplit(0).toInt, linesplit(1).toInt, linesplit(2).toDouble)
      landmarks += source
      store(source, destination) = distance
    }

    landmarks.toSet
  }

  /** Read the contents of a file into provided store, and returns landmark IDs
    *
    * @param filename Name of the file to read in
    * @param store The DistanceStore to read into
    * @return A set of landmarks IDs
    */
  def readFile(filename: String, store: DistanceStore): Set[Int] =
    _readFile(filename, store)

  /** Read the contents of a file into provided store, and returns landmark IDs
    *
    * This is a Java-friendly implementation of readFile.
    *
    * @param filename Name of the file to read in
    * @param store The DistanceStore to read into
    * @return A Java Set of landmarks IDs
    */
  def javaReadFile(filename: String, store: DistanceStore): java.util.Set[Integer] =
    _readFile(filename, store).map(scala.Int.box(_)).asJava
}
