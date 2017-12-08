package com.whackdata.rasters

import java.nio.file.{Files, Paths}

import com.whackdata.Constants.noData
import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.{ParseArgs, Utils}
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

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
    val deepestProcessed = elevList.minBy(_.elev)
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

          val currElevMask = Future {
            logger.info(s"Loading elevation mask")
            val currElevMaskFile = elevMasks.filter(_.elev == layer.elev).head
            GeoTiffReader.readSingleband(
            currElevMaskFile.path.toString,
            decompress = false,
            streaming = false
          )
          }

          val lastFloodFill = Future {
            logger.info(s"Loading flood fill")
            val lastFloodFillFile = floodFills.filter(_.elev == layer.elev + 10).head
            GeoTiffReader.readSingleband(
              lastFloodFillFile.path.toString,
              decompress = false,
              streaming = false
            )
          }

          // The water from the last step, with the current land now
          // above the water removed
          def waterA(currElevMask: SinglebandGeoTiff) = Future {
            logger.info("Processing LHS of equation")
            lastWater.tile.combine(currElevMask.tile) { (lw, ce) =>
              if (ce == 0) noData else lw
            }
          }

          val LHS = currElevMask.flatMap(waterA)

          // The water from the last step, with the parts that were accessible
          // last time removed
          def waterB(lastFloodFill: SinglebandGeoTiff) = Future {
            logger.info("Processing RHS of equation")
            lastWater.tile.combine(lastFloodFill.tile) { (lw, lf) =>
              if (lf == 2) noData else lw
            }
          }

          val RHS = lastFloodFill.flatMap(waterB)

          // Merge these two components together
          def combineWaterAB(waterA: Tile, waterB: Tile) = Future {
            logger.info("Combining LHS with RHS")
            waterA.combine (waterB) {
              (a, b) => if (a == 2 || b == 2) 2 else noData
            }
          }

          val combined = for {
            a <- LHS
            b <- RHS
            waterCurrent <- combineWaterAB(a, b)
          } yield lastWater.copy(tile = waterCurrent)

          // We actually do have to block here because each iteration depends
          // on the last being complete
          logger.info(s"Awaiting result for elevation ${layer.elev}")
          Await.result(combined, Duration.create(1000, SECONDS))
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
