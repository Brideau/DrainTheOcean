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

  // geoTiff.tile.map(fn(Int => Int)
  // geoTiff.tile.map(fn( Int, Int, Int => Int))
  // geoTiff.tile.mutable
  // geoTiff.tile.get(col, row)
  // geoTiff.tile.combine((Tile2)(Int, Int) => Int)
  // geoTiff.tile.rows / cols / size / dimensions
  // geoTiff.tile

  def classifyBinary(maxElevation: Int)(x: Int, y: Int, elevation: Int): Int = {
    if (elevation <= maxElevation) 0 else 1
  }
  val classify0 = classifyBinary(0)(_,_,_)

  def mapSomeTile(tileIn: Tile): Tile = {
    tileIn.map(classify0)
  }

  Utils.timems {
    val classGeo = geoTiff.mapTile(mapSomeTile)
      // .map(classify0)
      // .convert(ByteConstantNoDataCellType)
    GeoTiffWriter.write(classGeo, imageOut.toString)
  }
  // Try mutable






//  def printPixel(x: Int, y: Int, pixel: Pixel): Pixel = {
//    val color = pixel.toColor
//    println(s"X: $x Y: $y RGB: (${color.red}, ${color.green}, ${color.blue})")
//    pixel
//  }
//

}
