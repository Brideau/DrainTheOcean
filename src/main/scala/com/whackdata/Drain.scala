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

    val outputPath = Paths.get(conf.output_path())

    val xStart = conf.x()
    val yStart = conf.y()
    val elevStart = conf.elev()

    // Ensures that whatever number you start at, it gets snapped to
    // increments of 100 after
    val nextElev = (round(elevStart.toDouble / 100.0) * 100 - 100).toInt
    val elevRange: List[Int] = List(elevStart)
    // val elevRange = elevStart :: (nextElev to (nextElev - 100) by -100).toList

    // Read in the elevation raster. This will stick around for the duration
    // of the program as it is required throughout.
    logger.info("Reading in Elevation Raster")
    val elevRaster: SinglebandGeoTiff = GeoTiffReader.readSingleband(
      elevRasterPath.toString,
      decompress = true,
      streaming = false
    )

    object LastProcessedLayerStore {
      var layer: ProcessedLayer = _
    }

    for (elev <- elevRange) {

      logger.info("Classifying raster by elevation")
      val binFn = classifyByElevation(maxElevation = elev)(_,_,_)
      val elevMask = elevRaster.tile.map(binFn)
      val elevMaskGeoTiff = elevRaster.copy(tile = elevMask)

      logger.info("Performing flood fill")
      val filled = Utils.timems(floodFillTile(xStart, yStart, elevMask))
      val filledGeoTiff = elevRaster.copy(tile = filled)

      // For the first layer, seed the existing water from the original water raster.
      // For all others, use the previous layer's water raster, as it represents the
      // most recent state
      val waterRaster: SinglebandGeoTiff = if (elev == elevStart) {
        logger.info("Loading initial water raster")
        GeoTiffReader.readSingleband(
          waterRasterPath.toString,
          decompress = false,
          streaming = false
        )
      } else {
        LastProcessedLayerStore.layer.waterRaster
      }

      // TODO: Insert water calculations

      // Create an object from each of the processed components and store it so that
      // we can use it for calculations during the next iteration
      val processedLayer = ProcessedLayer(waterRaster, elevMaskGeoTiff, filledGeoTiff)
      LastProcessedLayerStore.layer = processedLayer

      // You'll need to store things on disk to allow for easy restart.
      // logger.info("Writing processed raster to disk")
      val fillOutPath = Utils.getOutputPath(elevRasterPath, outputPath, "Fill", elev)
      GeoTiffWriter.write(filledGeoTiff, fillOutPath.toString)
    }
  }

}
