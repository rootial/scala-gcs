package sand.gcs.util

import com.dongxiguo.zeroLog._
import com.dongxiguo.fastring.Fastring
import com.dongxiguo.fastring.Fastring.Implicits._
import com.dongxiguo.zeroLog.appenders.ConsoleAppender
import com.dongxiguo.zeroLog.context.CurrentClass
import com.dongxiguo.zeroLog.context.CurrentLine
import com.dongxiguo.zeroLog.context.CurrentMethodName
import com.dongxiguo.zeroLog.context.CurrentSource
import com.dongxiguo.zeroLog.formatters.SimpleFormatter
import sand.gcs.util.Config.config
import scala.compat.Platform

object Logger {
  implicit val (logger, formatter, appender) = ZeroLoggerFactory.newLogger()
}

/** Factory object that creates an appropriate logger based on configuration
  * file parameters.
  */
object ZeroLoggerFactory {
  /** Creates a new instance of a logger based on configuration file paramter gcs.logging-level. */
  def newLogger() = {
    val level = config.getString("gcs.logging-level") match {
      case "DEBUG" => Filter.Fine
      case "INFO" => Filter.Info
      case "NONE" => Filter.Severe
      case _ => throw new Exception("Invalid logging level")
    }
    (level, DefaultFormatter, ConsoleAppender)
  }
}

/** Override default ZeroLog output to something more appropriate for us */
object DefaultFormatter extends Formatter {
  private def levelName(level: Level) = level.name match {
    case "FINE" => "DEBUG"
    case _level => _level
  }

  /** Override default ZeroLog output to something more appropriate for us */
  override def format(
    level: Level,
    message: Fastring,
    currentSource: CurrentSource,
    currentLine: CurrentLine,
    currentClass: CurrentClass,
    currentMethodNameOption: Option[CurrentMethodName]): Fastring =
    fast"[${levelName(level)}] $message\n"

  /** Override default ZeroLog output to something more appropriate for us */
  override def format[A](
    level: Level,
    message: Fastring,
    throwable: Throwable,
    currentSource: CurrentSource,
    currentLine: CurrentLine,
    currentClass: CurrentClass,
    currentMethodNameOption: Option[CurrentMethodName]): Fastring =
    fast"[${levelName(level)}] $message\n"

  /** Override default ZeroLog output to something more appropriate for us */
  override def format(
    level: Level,
    throwable: Throwable,
    currentSource: CurrentSource,
    currentLine: CurrentLine,
    currentClass: CurrentClass,
    currentMethodNameOption: Option[CurrentMethodName]): Fastring =
    fast"[${levelName(level)}] EXCEPTION\n"
}
