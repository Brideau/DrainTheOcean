package com.whackdata.rasters

import java.nio.file.{Files, Paths}

import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.{ParseArgs, Utils}
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import me.tongfei.progressbar.ProgressBar
import org.slf4j.LoggerFactory

object FloodFillMasks {

  private val logger = LoggerFactory.getLogger("Flood Fill Logger")

  def floodFillTile(xStart: Int, yStart: Int, tileIn: Tile): Tile = {
    val fillObj = new FloodFill(tileIn.mutable)
    fillObj.fill(xStart, yStart)
    fillObj.tileToFill
  }

  def generate(conf: ParseArgs): Unit = {
    val outputPath = Paths.get(conf.output_path())

    val xStart = conf.x()
    val yStart = conf.y()

    // Get the list of already processed flood fills and the full list of
    // elevation masks to be processed so that we process what remains
    Files.createDirectories(outputPath.resolve("FloodFill"))
    val processedFills = getAlreadyProcessed(outputPath, "FloodFill", "tif")

    Files.createDirectories(outputPath.resolve("ElevMasks"))
    val elevMasks = getAlreadyProcessed(outputPath, "ElevMasks", "tif")

    val oceanBottom = -10800
    val numElev = (0 to oceanBottom by -10).length

    val elevList: List[ProcessedFile] = if (processedFills.nonEmpty) {
      logger.info(s"Restarting where processing left off last time")
      val alreadyProcessedElevs = processedFills.map(_.elev)

      elevMasks.filterNot(x => alreadyProcessedElevs.contains(x.elev))
    } else elevMasks

    var numProcessed = numElev - elevList.length
    logger.info(s"$numProcessed of $numElev already processed")

    def processLayer(elevMask: ProcessedFile): Unit = {
      Utils.timems {
        val elev = elevMask.elev

        logger.info(s"Loading raster for elevation = $elev")
        val elevRaster = GeoTiffReader.readSingleband(
          elevMask.path.toString,
          decompress = false,
          streaming = false
        )

        logger.info(s"Performing flood fill @ elevation = $elev")
        val filled = floodFillTile(xStart, yStart, elevRaster.tile)
        val filledGeoTiff = elevRaster.copy(tile = filled)

        val filledOutPath = Utils.getOutputPath(elevMask.path, outputPath, "FloodFill", elev)
        GeoTiffWriter.write(filledGeoTiff, filledOutPath.toString)

        numProcessed += 1
        logger.info(s"$numProcessed of $numElev already processed")
      }
    }


    elevList.foreach(processLayer)
  }

}
