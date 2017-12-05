package com.whackdata

import java.nio.file.{Files, Path, Paths}

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory
import Constants.noData

import scala.math.round
import scala.collection.JavaConverters._

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

  case class ProcessedFile(elev: Int, path: Path)

  def getAlreadyProcessed(outputPath: Path): List[ProcessedFile] = {
    // Get the paths of the water files already processed
    val existingFileList = Files.newDirectoryStream(outputPath.resolve("Water"))
    // Convert stream to a Scala vector
    val fileList = existingFileList.iterator().asScala.toList

    // Filter out any files that aren't tifs
    val filePaths = fileList
      .filter(_.toString.split('.').last == "tif")

    // Extract the elevation from the filename
    val fileElev = filePaths
      .map(_.getFileName)
      .map(_.toString)
      .map(fn => fn.splitAt(fn.lastIndexOf('_'))._2)
      .map(_.replaceAll("_", "").replaceAll(".tif", ""))
      .map(_.toInt)

    fileElev
      .zip(filePaths)
      .map(tup => ProcessedFile(tup._1, tup._2))
  }

  def simDrain(conf: ParseArgs): Unit = {
    val elevRasterPath = Paths.get(conf.elev_raster())
    val waterRasterPath = Paths.get(conf.water_raster())

    val outputPath = Paths.get(conf.output_path())

    val xStart = conf.x()
    val yStart = conf.y()
    val elevStart = conf.elev()

    // Read in the elevation raster. This will stick around for the duration
    // of the program as it is required throughout.
    logger.info("Reading in Elevation Raster")
    val elevRaster: SinglebandGeoTiff = GeoTiffReader.readSingleband(
      elevRasterPath.toString,
      decompress = false,
      streaming = false
    )

    object LastProcessed {
      var layer: ProcessedLayer = _
    }

    // Start where you last finished
    val alreadyProcessed = getAlreadyProcessed(outputPath)

    val elevLoopStart: Int = if (alreadyProcessed.nonEmpty) {
      val deepestProcessed = alreadyProcessed.minBy(_.elev)
      val elev = deepestProcessed.elev
      logger.info(s"Restarting where processing left off last time @ elevation $elev")

      // The only type of processed layer that requires carrying state forward
      // is the water raster, so we'll just load that from disk and re-process
      // the other ones. We end up repeating this code below, but it doesn't
      // really matter since it's only repeated once.

      logger.info("Re-classifyig the elevation of the last layer")
      val binFn = classifyByElevation(maxElevation = elev)(_,_,_)
      val elevMask = elevRaster.tile.map(binFn)
      val elevMaskGeoTiff = elevRaster.copy(tile = elevMask)

      logger.info("Re-flood-filling the elevation")
      val filled = Utils.timems(floodFillTile(xStart, yStart, elevMask))
      val filledGeoTiff = elevRaster.copy(tile = filled)

      logger.info("Loading the last calculated water raster")
      val waterRasterGeoTiff = GeoTiffReader.readSingleband(
        deepestProcessed.path.toString,
        decompress = false,
        streaming = false
      )

      val processedLayer = ProcessedLayer(waterRasterGeoTiff, elevMaskGeoTiff, filledGeoTiff)
      LastProcessed.layer = processedLayer

      // Tell the loop to start processing at the next elevation
      elev - 10
    } else elevStart

    val oceanBottom = -10800
    val elevRange = (elevLoopStart to oceanBottom by -10).toList

    for (elev <- elevRange) {

      logger.info(s"Classifying raster by elevation @ elevation = $elev")
      val binFn = classifyByElevation(maxElevation = elev)(_,_,_)
      val elevMask = elevRaster.tile.map(binFn)
      val elevMaskGeoTiff = elevRaster.copy(tile = elevMask)

      logger.info(s"Performing flood fill @ elevation = $elev")
      // ~37s for entire world
      val filled = Utils.timems(floodFillTile(xStart, yStart, elevMask))
      val filledGeoTiff = elevRaster.copy(tile = filled)

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
        logger.info(s"Processing water raster @ elevation = $elev")
        val lastWater = LastProcessed.layer.waterRaster
        val lastFloodFill = LastProcessed.layer.floodFillMask
        val currElevMask = elevMaskGeoTiff.tile

        // The water from the last step, with the current land now
        // above the water removed
        val waterA = lastWater.tile.combine(currElevMask) { (lw, ce) =>
          if (ce == 0) noData else lw
        }

        // The water from the last step, with the parts that were accessible
        // last time removed
        val waterB = lastWater.tile.combine(lastFloodFill.tile) { (lw, lf) =>
          if (lf == 2) noData else lw
        }

        // Merge these two components together
        val waterCurrent = waterA.combine(waterB) {(a, b) =>
          if (a == 2 || b == 2) 2 else noData
        }

        lastWater.copy(tile = waterCurrent)
      }

      logger.info("Writing water raster to disk")
      val waterOutPath = Utils.getOutputPath(waterRasterPath, outputPath, "Water", elev)
      GeoTiffWriter.write(waterRasterGeoTiff, waterOutPath.toString)

      // Create an object from each of the processed components and store it so that
      // we can use it for calculations during the next iteration
      val processedLayer = ProcessedLayer(waterRasterGeoTiff, elevMaskGeoTiff, filledGeoTiff)
      LastProcessed.layer = processedLayer

    }
  }

}
