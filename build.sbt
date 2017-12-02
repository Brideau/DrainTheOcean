name := "DrainOcean"

version := "0.1"

scalaVersion := "2.12.4"

val scrimageVersion = "2.1.8"
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-core" % scrimageVersion
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-io-extra" % scrimageVersion
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-filters" % scrimageVersion

libraryDependencies += "org.rogach" % "scallop_2.12" % "3.1.1"


// These let you enter input in the terminal if running via SBT
fork in run := true
connectInput in run := true

// Require Java 8
initialize := {
  val _ = initialize.value // run the previous initialization
  val required = "1.8"
  val current  = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
}
javaOptions in run += "-Xmx8G"
javaOptions in run += "-Xms8G"
