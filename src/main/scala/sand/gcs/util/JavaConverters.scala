package sand.gcs.util

import scala.collection.JavaConverters._

/** Convenience object that provides a smoother conversion between Java data structures and that of Scala's */
object JavaConverters {
  /** Converts a Java Set to a Scala Set.
    *
    * @param javaSet Java Set of Integers
    * @return Scala Set of Ints
    */
  def setToScala(javaSet: java.util.Set[Integer]): Set[Int] =
    javaSet.asScala.toSet map { a: Integer => a.intValue }
}
