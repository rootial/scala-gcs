package sand.gcs.system

import java.io.PrintWriter
import java.util.{ Set => JavaSet }
import sand.gcs.optimize.NonLinearOptimizer
import sand.gcs.util.Config.config
import sand.gcs.util.DistanceStore
import sand.gcs.util.Logger._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.compat.Platform
import scala.io.Source
import scala.util.Random

/** Graph Coordinate System "Cake layer" to be instantiated into a concrete instance
  *
  * This layer requires a [[sand.gcs.optimize.NonLinearOptimizer]] to be layered
  * on top of it that will be used to optimize the distances between landmarks.
  * For the [[sand.gcs.system.Orion]] and [[sand.gcs.system.Rigel]] systems,
  * this will be [[sand.gcs.optimize.NelderMead]]. The type parameter passed into
  * [[sand.gcs.optimize.NonLinearOptimizer]] must match the one passed into this trait.
  *
  * The type parameter passed into the trait will need to have a typeclass implementation
  * of [[sand.gcs.coordinate.DistanceComputable]] in scope upon instantiation.
  *
  * @tparam CoordType A type that can be used as a coordinate via [[sand.gcs.coordinate.DistanceComputable]]
  */
trait GraphCoordinateSystem[CoordType] extends Serializable { this: NonLinearOptimizer[CoordType] =>
  import NonLinearOptimizer._

  /** Dimension of the coordinate system */
  implicit def dimension: Int

  /** Stores the coordinates of the landmark nodes */
  private var LANDMARKS_EMBEDDED = false
  private val landmarks = mutable.Map.empty[Int, CoordType]
  private var landmarkIds = Vector.empty[Int] // TODO: this is probably bad #yolo

  /** Stores the coordinates of the non-landmark nodes */
  private val nodes = mutable.Map.empty[Int, CoordType]

  private def getRandomPrimaries(): Set[Int] = {
    val primaries = mutable.Set.empty[Int]

    val numberOfLandmarks = landmarkIds.size

    while (primaries.size != config.getInt("gcs.embed.primary-landmarks")) {
      val r = Random.nextInt(numberOfLandmarks)
      primaries.add(landmarkIds(r))
    }

    primaries.toSet
  }

  /** Embed the landmarks into the coordinate system.
    *
    * Embed both primary and non-primary landmarks into the coordinate system.
    * This function should only be called once per instantiation, enforced by
    * the LANDMARKS_EMBEDDED flag.
    *
    * Primary landmarks by default are chosen randomly. The number of such
    * primary landmarks is set by default at 16. This value can be overridden
    * in application.conf via gcs.embed.primary-landmarks.
    *
    * If the primaries parameter is passed in, it will override the configuration
    * files.
    *
    * @param landmarkSet A set of landmark IDs
    * @param primaries Optional parameter denoting primary landmarks to embed first
    * @param store DistanceStore implementation containing distance values to embed
    */
  def embedLandmarks(landmarkSet: Set[Int], primaries: Option[Set[Int]] = None)(implicit store: DistanceStore) {
    if (LANDMARKS_EMBEDDED)
      throw new Exception("Can only call embedLandmarks or load once per GCS instance!")

    landmarkIds = landmarkSet.toVector

    val primaryLandmarks = primaries.getOrElse(getRandomPrimaries()).toIndexedSeq

    val embedding = optimizeLandmarks(primaryLandmarks)

    var i = 0
    val size = embedding.size
    while (i != size) {
      val id = primaryLandmarks(i)
      val coord = embedding(i)
      landmarks(id) = coord
      i += 1
    }

    val secondaryLandmarks = landmarkIds.filter(id => !primaryLandmarks.contains(id))
    val pairedEmbedding = primaryLandmarks zip embedding

    secondaryLandmarks foreach { i =>
      landmarks(i) = optimizeNode(pairedEmbedding, i)
    }

    LANDMARKS_EMBEDDED = true
  }

  /** Java API for embedLandmarks.
    *
    * If you would like to use random selection of primary landmarks, set
    * "primaries" to null.
    *
    * All work done will be forwarded to the Scala version of embedLandmarks.
    * This method simply converts data structures to the Scala equivalent before
    * passing it along.
    *
    * @param landmarkSet A set of landmark IDs
    * @param primaries Contains IDs of primary landmarks to embed - pass null for default
    * @param store DistanceStore implementation containing distance values to embed
    */
  def embedLandmarks(landmarkSet: JavaSet[Int], primaries: JavaSet[Int])(implicit store: DistanceStore) {
    embedLandmarks(landmarkSet.asScala.toSet, Option(primaries.asScala.toSet))(store)
  }

  /** Embeds non-landmark into the coordinate system.
    *
    * Embeds a non-landmark into the coordinate system by randomly selecting X amount
    * of "neighbor" landmarks to align against. By default X is set to 16. This can
    * be overridden via config with gcs.embed.neighbors.
    *
    * @param id ID of the node to embed
    * @param store DistanceStore implementation containing distance values to embed
    * @return coordinates of the embedded non-landmark
    */
  def embedNonLandmark(id: Int)(implicit store: DistanceStore): CoordType = {
    val pairedLandmarks = getRandomPrimaries().toIndexedSeq.map(i => (i, landmarks(i)))
    nodes(id) = optimizeNode(pairedLandmarks, id)
    nodes(id)
  }

  /** Embeds a set of distances into the coordinate system.
    *
    * Embeds a set of distances into the coordinate system - all work is forwarded
    * to embedLandmarks and embedNonLandmark.
    *
    * @param distances DistanceStore implementation containing distance values to embed
    * @param landmarkSet A set of landmark IDs
    * @param primaries Optional parameter denoting primary landmarks to embed first
    */
  def embed(distances: DistanceStore, landmarkSet: Set[Int], primaries: Option[Set[Int]] = None) {
    implicit val store = distances
    landmarkIds = landmarkSet.toVector

    logger.info("Embedding landmarks...")
    val timeBeforeLandmarkEmbedding = Platform.currentTime
    embedLandmarks(landmarkSet, primaries)
    val timeTakenForLandmarkEmbedding = Platform.currentTime - timeBeforeLandmarkEmbedding
    logger.info(s"Landmark embedding took $timeTakenForLandmarkEmbedding milliseconds.")

    val nonLandmarksNodes = distances.ids -- landmarkIds
    logger.info("Embedding non-landmarks...")
    val timeBeforeNonLandmarkEmbedding = Platform.currentTime
    nonLandmarksNodes.toSeq.zipWithIndex foreach {
      case (id, index) =>
        logger.info(s"Embedding ${index + 1}th non-landmark...")
        embedNonLandmark(id)
    }
    val timeTakenForNonLandmarkEmbedding = Platform.currentTime - timeBeforeNonLandmarkEmbedding
    logger.info(s"Non-landmark embedding took $timeTakenForNonLandmarkEmbedding milliseconds.")
  }

  /** Java API for embed.
    *
    * If you would like to use random selection of primary landmarks, set
    * "primaries" to null.
    *
    * All work done will be forwarded to the Scala version of embed.
    * This method simply converts data structures to the Scala equivalent before
    * passing it along.
    *
    * @param store DistanceStore implementation containing distance values to embed
    * @param landmarkSet A set of landmark IDs
    * @param primaries Contains IDs of primary landmarks to embed - pass null for default
    */
  def embed(distances: DistanceStore, landmarkSet: JavaSet[Int], primaries: JavaSet[Int]) {
    embed(distances, landmarkSet.asScala.toSet, Option(primaries.asScala.toSet))
  }

  /** Loads a set of coordinates from a given file.
    *
    * File should be formatted such that each row represents the embedding
    * of a single node. The format for each row should look like
    * (using the Array[Double] view):
    *
    * {{{
    * [L/N if (non-)landmark] [Node ID] [Coordinate 1] ... [Coordinate N]
    * }}}
    *
    * @param filename Path of the file containing the formatted coordinates.
    */
  def load(filename: String) {
    if (LANDMARKS_EMBEDDED)
      throw new Exception("Can only call embedLandmarks or load once per GCS instance!")

    val (landmarkEmbedding, nodeEmbedding) =
      Source.fromFile(filename).getLines().toVector.map(_.split("\\s+")).partition(_(0) == "L")

    var i = 0
    val landmarkEmbeddingSize = landmarkEmbedding.size
    while (i != landmarkEmbeddingSize) {
      val linesplit = landmarkEmbedding(i)
      val id = linesplit(1).toInt
      landmarkIds = landmarkIds :+ id
      landmarks += id -> distanceCalculator.fromArray(linesplit.drop(2).map(_.toDouble))
      i += 1
    }

    i = 0
    val nodeEmbeddingSize = nodeEmbedding.size
    while (i != nodeEmbeddingSize) {
      val linesplit = nodeEmbedding(i)
      nodes += linesplit(1).toInt -> distanceCalculator.fromArray(linesplit.drop(2).map(_.toDouble))
      i += 1
    }

    LANDMARKS_EMBEDDED = true
  }

  /** Saves the sand landmark and node embeddings of the coordinate system.
    *
    * File saved will be formatted as (using the Array[Double] view):
    *
    * {{{
    * [L/N if (non-)landmark] [Node ID] [Coordinate 1] ... [Coordinate N]
    * }}}
    *
    * @param filename Path of the file to save the coordinates into.
    */
  def save(filename: String) {
    val outfile = new PrintWriter(filename)

    landmarks foreach { p =>
      outfile.println("L " + p._1 + " " + distanceCalculator.toArray(p._2).mkString(" "))
    }

    nodes foreach { p =>
      outfile.println("N " + p._1 + " " + distanceCalculator.toArray(p._2).mkString(" "))
    }

    outfile.close()
  }

  /** Query the embedded distance given two node IDs.
    *
    * @param source ID of the source node
    * @param destination ID of the destination node
    * @return Estimated distance between source and destination
    */
  def query(source: Int, destination: Int): Double = {
    val sourceCoordinates =
      landmarks.get(source).orElse(nodes.get(source))

    if (sourceCoordinates.isEmpty)
      throw new Exception(s"Node with ID $source was not found in the embedding.")

    val destinationCoordinates =
      landmarks.get(destination).orElse(nodes.get(destination))

    if (destinationCoordinates.isEmpty)
      throw new Exception(s"Node with ID $destination was not found in the embedding.")

    distanceCalculator.distanceBetween(sourceCoordinates.get, destinationCoordinates.get)
  }

  /** Query the embedded distance given two node IDs.
    *
    * @param source ID of the source node
    * @param destination ID of the destination node
    * @return Estimated distance between source and destination
    */
  def distanceBetween(source: Int, destination: Int): Double = query(source, destination)

  /** Retrieve the embedded coordinate given a node ID.
    *
    * @param ID of the node
    * @return Embedded coordinate
    */
  def coordinateOf(id: Int): Option[CoordType] =
    landmarks.get(id).orElse(nodes.get(id))

  def storeCoordinate(id: Int, coordinate: CoordType, isLandmark: Boolean) {
    if (isLandmark) landmarks(id) = coordinate
    else nodes(id) = coordinate
  }
}
