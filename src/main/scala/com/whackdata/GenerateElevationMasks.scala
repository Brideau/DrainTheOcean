package com.whackdata

import java.nio.file.{Files, Paths}

import com.whackdata.Utils.getAlreadyProcessed
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory

object GenerateElevationMasks {

  private val logger = LoggerFactory.getLogger("Elevation Mask Logger")


  def classifyByElevation(maxElevation: Int)(x: Int, y: Int, elevation: Int): Int = {
    if (elevation > maxElevation) 0 else 1
  }

  def run(conf: ParseArgs): Unit = {
    val elevRasterPath = Paths.get(conf.elev_raster())

    val outputPath = Paths.get(conf.output_path())
    val elevStart = conf.elev()

    // Read in the elevation raster. This will stick around for the duration
    // of the program as it is required throughout.
    logger.info("Reading in Elevation Raster")
    val elevRaster: SinglebandGeoTiff = GeoTiffReader.readSingleband(
      elevRasterPath.toString,
      decompress = false,
      streaming = false
    )

    // Start where you last finished
//    Files.createDirectories(outputPath.resolve("ElevMasks"))
//    val alreadyProcessed = getAlreadyProcessed(outputPath, "ElevMasks", "tif")

//    val elevLoopStart: Int = if (alreadyProcessed.nonEmpty) {
//      val deepestProcessed = alreadyProcessed.minBy(_.elev)
//      val elev = deepestProcessed.elev
//      logger.info(s"Restarting where processing left off last time @ elevation $elev")
//      // Tell the loop to start processing at the next elevation
//      elev
//    } else elevStart
    val elevLoopStart = elevStart

    val oceanBottom = -10800
    val elevRange: List[Int] = (elevLoopStart to oceanBottom by -10).toList

    def processElevMask(elev: Int) {
      Utils.timems {
        logger.info(s"Classifying raster by elevation @ elevation = $elev")
        val binFn = classifyByElevation(maxElevation = elev)(_, _, _)
        val elevMask = elevRaster.tile.map(binFn)
        val elevMaskGeoTiff = elevRaster.copy(tile = elevMask)

        logger.info("Writing elev raster to disk")
        val outPath = Utils.getOutputPath(elevRasterPath, outputPath, "ElevMasks", elev)
        GeoTiffWriter.write(elevMaskGeoTiff, outPath.toString)
      }
    }

    elevRange.par.foreach(processElevMask)
  }

}
