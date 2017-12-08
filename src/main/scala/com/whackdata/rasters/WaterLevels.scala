package com.whackdata.rasters

import java.nio.file.{Files, Paths}

import com.whackdata.Constants.noData
import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.{ParseArgs, Utils}
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory

object WaterLevels {

  private val logger = LoggerFactory.getLogger("Water Level Logger")

  def generate(conf: ParseArgs): Unit = {
    val baseWaterRaster = Paths.get(conf.water_raster())
    val outputPath = Paths.get(conf.output_path())

    object LastProcessed {
      var layer: SinglebandGeoTiff = _
    }

    // Get lists of files already processed
    Files.createDirectories(outputPath.resolve("FloodFill"))
    val floodFills = getAlreadyProcessed(outputPath, "FloodFill", "tif")

    Files.createDirectories(outputPath.resolve("ElevMasks"))
    val elevMasks = getAlreadyProcessed(outputPath, "ElevMasks", "tif")

    Files.createDirectories(outputPath.resolve("Water"))
    val processedWater = getAlreadyProcessed(outputPath, "Water", "tif")

    val oceanBottom = -10800
    val numTotal = (0 to oceanBottom by -10).length
    logger.info(s"${processedWater.length} of $numTotal have already been processed")

    val elevList: List[ProcessedFile] = if (processedWater.nonEmpty) {
      logger.info(s"Restarting where processing left off last time")
      val alreadyProcessed = processedWater.map(_.elev)

      floodFills.filterNot(x => alreadyProcessed.contains(x.elev))
    } else floodFills

    logger.info("Loading the last calculated water raster")
    val deepestProcessed = processedWater.minBy(_.elev)
    val waterRasterGeoTiff = GeoTiffReader.readSingleband(
      deepestProcessed.path.toString,
      decompress = false,
      streaming = false
    )
    // Store it as the last processed layer
    LastProcessed.layer = waterRasterGeoTiff

    for (layer <- elevList.sortBy(-_.elev)) {
      Utils.timems {
        // For the first layer, seed the existing water from the original water raster.
        // For all others, use the previous layer's water raster, as it represents the
        // most recent state
        val waterRasterGeoTiff: SinglebandGeoTiff = if (layer.elev == 0) {
          logger.info("Loading initial water raster")
          GeoTiffReader.readSingleband(
            baseWaterRaster.toString,
            decompress = false,
            streaming = false
          )
        } else {
          logger.info(s"Processing water raster @ elevation = ${layer.elev}")
          val lastWater = LastProcessed.layer

          val lastFloodFillFile = floodFills.filter(_.elev == layer.elev + 10).head
          val lastFloodFill = GeoTiffReader.readSingleband(
            lastFloodFillFile.path.toString,
            decompress = false,
            streaming = false
          )

          val currElevMaskFile = elevMasks.filter(_.elev == layer.elev).head
          val currElevMask = GeoTiffReader.readSingleband(
            currElevMaskFile.path.toString,
            decompress = false,
            streaming = false
          )

          // The water from the last step, with the current land now
          // above the water removed
          val waterA = lastWater.tile.combine(currElevMask.tile) { (lw, ce) =>
            if (ce == 0) noData else lw
          }

          // The water from the last step, with the parts that were accessible
          // last time removed
          val waterB = lastWater.tile.combine(lastFloodFill.tile) { (lw, lf) =>
            if (lf == 2) noData else lw
          }

          // Merge these two components together
          val waterCurrent = waterA.combine(waterB) { (a, b) =>
            if (a == 2 || b == 2) 2 else noData
          }

          lastWater.copy(tile = waterCurrent)
        }

        logger.info("Writing water raster to disk")
        val waterOutPath = Utils.getOutputPath(baseWaterRaster, outputPath, "Water", layer.elev)
        GeoTiffWriter.write(waterRasterGeoTiff, waterOutPath.toString)

        // Create an object from each of the processed components and store it so that
        // we can use it for calculations during the next iteration
        LastProcessed.layer = waterRasterGeoTiff
      }
    }
  }

}
