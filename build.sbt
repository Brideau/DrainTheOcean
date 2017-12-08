name := "DrainOcean"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "org.rogach" %% "scallop" % "3.1.1"

libraryDependencies += "org.locationtech.geotrellis" %% "geotrellis-raster" % "1.1.1"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"

val scrimmageVersion = "2.1.8"
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-core" % scrimmageVersion
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-io-extra" % scrimmageVersion
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-filters" % scrimmageVersion

libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "0.13.2",
  "org.scalanlp" %% "breeze-natives" % "0.13.2"
)
resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.5"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.7"


mainClass in assembly := Some("com.whackdata.Main")

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
