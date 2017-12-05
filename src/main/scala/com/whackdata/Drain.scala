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
      decompress = false,
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

      logger.info("Writing elevation mask raster to disk")
      val elevOutPath = Utils.getOutputPath(elevRasterPath, outputPath, "ElevMask", elev)
      GeoTiffWriter.write(elevMaskGeoTiff, elevOutPath.toString)

      logger.info("Performing flood fill")
      // ~37s for entire world
      val filled = Utils.timems(floodFillTile(xStart, yStart, elevMask))
      val filledGeoTiff = elevRaster.copy(tile = filled)

      // Write things to disk to allow for easy restart if the program fails
      logger.info("Writing flood filled raster to disk")
      val fillOutPath = Utils.getOutputPath(elevRasterPath, outputPath, "Fill", elev)
      GeoTiffWriter.write(filledGeoTiff, fillOutPath.toString)

      // For the first layer, seed the existing water from the original water raster.
      // For all others, use the previous layer's water raster, as it represents the
      // most recent state
      val waterRasterGeoTiff: SinglebandGeoTiff = if (elev == elevStart) {
        logger.info("Loading initial water raster")
        GeoTiffReader.readSingleband(
          waterRasterPath.toString,
          decompress = false,
          streaming = false
        )
      } else {
        // TODO: Insert water calculations
        LastProcessedLayerStore.layer.waterRaster
      }

      // Create an object from each of the processed components and store it so that
      // we can use it for calculations during the next iteration
      val processedLayer = ProcessedLayer(waterRasterGeoTiff, elevMaskGeoTiff, filledGeoTiff)
      LastProcessedLayerStore.layer = processedLayer

      logger.info("Writing water raster to disk")
      val waterOutPath = Utils.getOutputPath(waterRasterPath, outputPath, "Water", elev)
      // TODO: Fix once water has been calculated properly
      GeoTiffWriter.write(waterRasterGeoTiff, waterOutPath.toString)
    }
  }

}
