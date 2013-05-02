import AssemblyKeys._

import scalariform.formatter.preferences._

name := "gcs"

version := "0.1.0-RC1"

organization := "sand"

scalaVersion := "2.10.1"

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.1.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.2"

libraryDependencies += "com.dongxiguo" %% "zero-log" % "0.3.3"

assemblySettings

scalariformSettings

ScalariformKeys.preferences := FormattingPreferences().
  setPreference(DoubleIndentClassDeclaration, true).
  setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
  setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
