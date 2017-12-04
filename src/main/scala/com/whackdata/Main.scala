package com.whackdata

import java.nio.file.Paths

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

// Parse input parameters
class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val image = opt[String]()
  val x = opt[Int]()
  val y = opt[Int]()
  val elev = opt[Int]()
  verify()
}

object Main extends App {

  val logger = LoggerFactory.getLogger("MainLogger")

  def classifyBinary(maxElevation: Int)(x: Int, y: Int, elevation: Int): Int = {
    if (elevation > maxElevation) 0 else 1
  }
  def mapOverTilePixels(mapFunction: (Int, Int, Int) => Int)(tileIn: Tile): Tile = {
    tileIn.map(mapFunction)
  }

  def floodFillTile(xStart: Int, yStart: Int)(tileIn: Tile): Tile = {
    val fillObj = new FloodFill(tileIn.mutable)
    // 56s for full globe
    // 213s with wrap-around support
    fillObj.fill(xStart, yStart)
    fillObj.tileToFill
  }

  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val xStart = conf.x()
  val yStart = conf.y()
  val elevStart = conf.elev()

  logger.info("Reading in GeoTiff")
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(
    imagePath.toString,
    decompress = false,
    streaming = false
  )

  logger.info("Classifying raster by elevation")
  val binFn = classifyBinary(maxElevation = elevStart)(_,_,_)
  val classGeo = geoTiff.mapTile(mapOverTilePixels(binFn)(_))

  logger.info("Performing flood fill")
  val filled = classGeo.mapTile(floodFillTile(xStart = xStart, yStart = yStart)(_))

  logger.info("Writing processed raster to disk")
  val imageOut = Utils.getOutputPath(imagePath, elevStart)
  GeoTiffWriter.write(filled, imageOut.toString)

}
