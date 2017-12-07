package com.whackdata.rasters

import java.nio.file.{Files, Paths}

import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.{ParseArgs, Utils}
import geotrellis.raster.{EightNeighbors, Tile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.regiongroup.{RegionGroup, RegionGroupOptions}
import org.slf4j.LoggerFactory

object Cleanup {

  // TODO: This is incomplete. The plan was to use a region group tool compared with zonal statistics to find all the small clusters of water pixels to remove them, but testing on a small area showed that even the first stage of the process takes a lot of compute power. Not feasible at this time.

  private val logger = LoggerFactory.getLogger("Cleanup Logger")

  def generate(conf: ParseArgs): Unit = {
    val outputPath = Paths.get(conf.output_path())

    // Get the list of already processed flood fills and the full list of
    // elevation masks to be processed so that we process what remains
    Files.createDirectories(outputPath.resolve("FloodFill"))
    val processedFills = getAlreadyProcessed(outputPath, "FloodFill", "tif")

    Files.createDirectories(outputPath.resolve("Cleanup"))
    val cleanedFills = getAlreadyProcessed(outputPath, "Cleanup", "tif")

    val oceanBottom = -10800
    val numElev = (0 to oceanBottom by -10).length

    val elevList: List[ProcessedFile] = if (processedFills.nonEmpty) {
      logger.info(s"Restarting where processing left off last time")
      val alreadyProcessed = cleanedFills.map(_.elev)

      processedFills.filterNot(x => alreadyProcessed.contains(x.elev))
    } else cleanedFills

    var numProcessed = numElev - elevList.length
    logger.info(s"$numProcessed of $numElev already processed")

    def processLayer(floodFill: ProcessedFile): Unit = {
      Utils.timems {
        val elev = floodFill.elev

        logger.info(s"Loading raster for elevation = $elev")

        val filledGeoTiff = GeoTiffReader.readSingleband(
          floodFill.path.toString,
          decompress = false,
          streaming = false
        )

        val rgo = RegionGroupOptions(connectivity = EightNeighbors, ignoreNoData = false)
        val fillRegions = RegionGroup(filledGeoTiff.tile, rgo).tile

        val regionGeoTiff = filledGeoTiff.copy(tile = fillRegions)

        val filledOutPath = Utils.getOutputPath(floodFill.path, outputPath, "Testing", elev)
        GeoTiffWriter.write(regionGeoTiff, filledOutPath.toString)

        numProcessed += 1
        logger.info(s"$numProcessed of $numElev processed")
      }
    }

    elevList.foreach(processLayer)
  }

}
