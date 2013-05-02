package sand.gcs.util

import com.typesafe.config.ConfigFactory
import java.io.File

/** Singleton object containing the parsed version of the configuration files. */
object Config {
  private val defaultConfig = ConfigFactory.parseURL(getClass.getResource("/reference.conf"))

  /** Parsed configuration file.
    *
    * Parsed configuration file. Defaults can be overridden by providing an "application.conf"
    * file in the root project directory.
    *
    * Configuration file paramters can be accessed via method calls such as getInt and getDouble.
    * The parameter to pass in is a string denoting the path - e.g. gcs.logging-level.
    */
  val config =
    ConfigFactory.parseFile(new File("application.conf")).resolve().withFallback(defaultConfig)
}
