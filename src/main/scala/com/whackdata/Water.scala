package com.whackdata

import java.nio.file.{Path, Paths}

import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter

object Water {

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

    val waterRasterA = getWaterRaster(waterA)
    val waterRasterB = getWaterRaster(waterB)

    val combinedWater = waterRasterA.tile.combine(waterRasterB.tile) { (a, b) =>
      if (a == 2 || b == 2) 2 else -32768
    }

    val waterRasterC = waterRasterA.copy(tile = combinedWater)

    val outPath = Utils.getOutputPath(waterA, elevation)
    GeoTiffWriter.write(waterRasterC, outPath.toString)
  }

}
