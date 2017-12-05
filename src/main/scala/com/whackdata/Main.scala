package com.whackdata

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

// Parse input parameters
class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val job = opt[String](required = true)
  val elev_raster = opt[String]()
  val water_raster = opt[String]()
  val x = opt[Int]()
  val y = opt[Int]()
  val elev = opt[Int]()
  val water_a = opt[String]()
  val water_b = opt[String]()
  val output_path = opt[String]()
  val base_raster = opt[String](default = Some("None"))
  verify()
}

object Main extends App {

  private val logger = LoggerFactory.getLogger("Main Logger")

  val conf = new ParseArgs(args)
  val jobType = conf.job()

  jobType match {
    case "simDrain" =>
      logger.info("Starting drain simulation")
      Drain.simDrain(conf)
    case "combineWater" =>
      logger.info("Starting water combining process")
      Water.merge(conf)
    case "generatePngs" =>
      logger.info("Start PNG generation")
      GeneratePng.run(conf)
    case "processPngs" =>
      logger.info("Start PNG processing")
      ProcessPng.run(conf)
  }

}
