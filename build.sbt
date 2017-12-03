name := "DrainOcean"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "org.rogach" %% "scallop" % "3.1.1"

libraryDependencies += "org.locationtech.geotrellis" %% "geotrellis-raster" % "1.1.1"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"



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
