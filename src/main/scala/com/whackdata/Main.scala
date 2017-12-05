package com.whackdata

import geotrellis.raster.io.geotiff._
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

// Parse input parameters
class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val elevRaster = opt[String]()
  val waterRaster = opt[String]()
  val x = opt[Int]()
  val y = opt[Int]()
  val elev = opt[Int]()
  val job = opt[String]()
  verify()
}

object Main extends App {

  private val logger = LoggerFactory.getLogger("Main Logger")

  val conf = new ParseArgs(args)
  val jobType = conf.job()

  jobType match {
    case "simDrain" =>
      logger.info("Starting Drain Simulation")
      Drain.simDrain(conf)
  }

}
