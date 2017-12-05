package com.whackdata

import java.nio.file.{Path, Paths}

import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import org.slf4j.LoggerFactory

object Water {

  private val logger = LoggerFactory.getLogger("Water Logger")

  private def getWaterRaster(path: Path): SinglebandGeoTiff = {
    GeoTiffReader.readSingleband(
      path.toString,
      decompress = true,
      streaming = false
    )
  }

  def merge(conf: ParseArgs): Unit = {

    val waterA = Paths.get(conf.water_a())
    val waterB = Paths.get(conf.water_b())

    val elevation = conf.elev()

    logger.info("Loading Raster A")
    val waterRasterA = getWaterRaster(waterA)
    logger.info("Loading Raster B")
    val waterRasterB = getWaterRaster(waterB)

    logger.info("Combining both rasters")
    val combinedWater = waterRasterA.tile.combine(waterRasterB.tile) { (a, b) =>
      if (a == 2 || b == 2) 2 else -32768
    }
    val waterRasterC = waterRasterA.copy(tile = combinedWater)

    logger.info("Writing result to disk")
    val outPath = Utils.getOutputPath(waterA, elevation)
    GeoTiffWriter.write(waterRasterC, outPath.toString)
  }

}
