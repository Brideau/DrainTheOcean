package com.whackdata

import java.nio.file.Paths

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.rogach.scallop.{ScallopConf, ScallopOption}
import org.slf4j.LoggerFactory

class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val image = opt[String]()
  val x = opt[Int]()
  val y = opt[Int]()
  val elev = opt[Int]()
  verify()
}

object Main extends App {

  val logger = LoggerFactory.getLogger("MainLogger")

  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val xStart = conf.x()
  val yStart = conf.y()
  val maxElev = conf.elev()

  val imageOut = Utils.getOutputPath(imagePath)

  logger.info("Reading in GeoTiff")
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(
    imagePath.toString,
    decompress = false,
    streaming = false
  )

  def classifyBinary(maxElevation: Int)(x: Int, y: Int, elevation: Int): Int = {
    if (elevation > maxElevation) 0 else 1
  }
  def mapOverTilePixels(mapFunction: (Int, Int, Int) => Int)(tileIn: Tile): Tile = {
    tileIn.map(mapFunction)
  }

  logger.info("Classifying raster by elevation")
  val binFn = classifyBinary(maxElevation = maxElev)(_,_,_)
  val classGeo = geoTiff.mapTile(mapOverTilePixels(binFn)(_))

  def floodFillTile(xStart: Int, yStart: Int)(tileIn: Tile): Tile = {
    val fillObj = new FloodFill(tileIn.mutable)
    Utils.timems {
      // 56s for full globe
      // 213s with wrap-around support
      fillObj.fill(xStart, yStart)
    }
    fillObj.tileToFill
  }

  logger.info("Performing flood fill")
  val filled = classGeo.mapTile(floodFillTile(xStart = xStart, yStart = yStart)(_))

  logger.info("Writing processed raster to disk")
  GeoTiffWriter.write(filled, imageOut.toString)

}
