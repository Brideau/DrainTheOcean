package com.whackdata

import java.nio.file.Paths
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter

import org.rogach.scallop.{ScallopConf, ScallopOption}

class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val image: ScallopOption[String] = opt[String]()
  verify()
}

object Main extends App {

  // Geotrellis hydrology fill

  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val imageOut = Utils.getOutputPath(imagePath)
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

  val binFn = classifyBinary(maxElevation = 1100)(_,_,_)
  val classGeo = geoTiff.mapTile(mapOverTilePixels(binFn)(_))

  def floodFillTile(xStart: Int, yStart: Int)(tileIn: Tile): Tile = {
    val fillObj = new FloodFill(tileIn.mutable)
    Utils.timems {
      fillObj.fill(xStart, yStart) // 56s for full globe
    }
    fillObj.tileToFill
  }
  val filled = classGeo.mapTile(floodFillTile(xStart = 3000, yStart = 1000)(_))

  // TODO: Reproject to nicer projection
  // TODO: Color the earth brown and water blue
  // TODO: Join all the layers

  GeoTiffWriter.write(filled, imageOut.toString)

}
