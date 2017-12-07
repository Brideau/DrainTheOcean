package com.whackdata

import com.whackdata.pngs.{AddTextPng, GeneratePng, ProcessPng}
import com.whackdata.rasters.{Drain, ElevationMasks, FloodFillMasks}
import com.whackdata.scripts.{GetMinLocation, Water}
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
  val testFill = toggle(default = Some(false))
  verify()
}

object Main extends App {

  // TODO: Clean up naming so that they don't grow forever
  private val logger = LoggerFactory.getLogger("Main Logger")

  val conf = new ParseArgs(args)
  val jobType = conf.job()

  jobType match {
    case "simDrain" =>
      logger.info("Starting drain simulation")
      Drain.simDrain(conf)
    case "generateElevMasks" =>
      logger.info("Generating elevation masks")
      ElevationMasks.generate(conf)
    case "generateFloodFillMasks" =>
      logger.info("Generating flood fill masks")
      FloodFillMasks.generate(conf)
    case "combineWater" =>
      logger.info("Starting water combining process")
      Water.merge(conf)
    case "generatePngs" =>
      logger.info("Starting PNG generation")
      GeneratePng.run(conf)
    case "processPngs" =>
      logger.info("Starting PNG processing")
      ProcessPng.run(conf)
    case "addText" =>
      logger.info("Starting to add Text to PNGs ")
      AddTextPng.run(conf)
    case "getMinLoc" =>
      logger.info("Finding min location")
      GetMinLocation.run(conf)
    case _ =>
      logger.error("Unknown Job type")
  }

}
