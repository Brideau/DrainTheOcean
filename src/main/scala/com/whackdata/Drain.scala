package com.whackdata

import java.nio.file.Paths

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory

import scala.math.round

object Drain {

  private val logger = LoggerFactory.getLogger("Drain Logger")

  case class ProcessedLayer(waterRaster: SinglebandGeoTiff,
                            elevMask: SinglebandGeoTiff,
                            floodFillMask: SinglebandGeoTiff)
  object LastProcessedLayerStore {
    var layer: ProcessedLayer = _
  }

  def classifyByElevation(maxElevation: Int)(x: Int, y: Int, elevation: Int): Int = {
    if (elevation > maxElevation) 0 else 1
  }

  def floodFillTile(xStart: Int, yStart: Int, tileIn: Tile): Tile = {
    val fillObj = new FloodFill(tileIn.mutable)
    // 56s for full globe
    // 213s with wrap-around support
    fillObj.fill(xStart, yStart)
    fillObj.tileToFill
  }

  def simDrain(conf: ParseArgs): Unit = {
    val elevRasterPath = Paths.get(conf.elev_raster())
    val waterRasterPath = Paths.get(conf.water_raster())

    val xStart = conf.x()
    val yStart = conf.y()
    val elevStart = conf.elev()

    // Ensures that whatever number you start at, it gets snapped to
    // increments of 100 after
    val nextElev = (round(elevStart.toDouble / 100.0) * 100 - 100).toInt
    val elevRange: List[Int] = List(elevStart, nextElev)
    // val elevRange = elevStart :: (nextElev to (nextElev - 100) by -100).toList

    // Read in the elevation raster. This will stick around for the duration
    // of the program as it is required throughout.
    logger.info("Reading in Elevation Raster")
    val elevRaster: SinglebandGeoTiff = GeoTiffReader.readSingleband(
      elevRasterPath.toString,
      decompress = true,
      streaming = false
    )

    for (elev <- elevRange) {

      logger.info("Classifying raster by elevation")
      val binFn = classifyByElevation(maxElevation = elev)(_, _, _)
      val elevMask = elevRaster.tile.map(binFn)

      logger.info("Performing flood fill")
      val filled = floodFillTile(xStart, yStart, elevMask)
      val filledGeoTiff = elevRaster.copy(tile = filled)

      // For the first layer, seed the existing water from the original water raster.
      // For all others, use the previous layer's water raster, as it represents the
      // most recent state
//      val waterRaster: SinglebandGeoTiff = if (elev == elevStart) {
//        GeoTiffReader.readSingleband(
//          waterRasterPath.toString,
//          decompress = true,
//          streaming = false
//        )
//      } else {
//        LastProcessedLayerStore.layer.waterRaster // FIX
//      }

      // You'll need to store things on disk to allow for easy restart.
      // logger.info("Writing processed raster to disk")
       val imageOut = Utils.getOutputPath(elevRasterPath, elevStart)
       GeoTiffWriter.write(filledGeoTiff, imageOut.toString)
    }
  }

}
