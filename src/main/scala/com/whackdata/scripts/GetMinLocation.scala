package com.whackdata.scripts

import java.nio.file.Paths

import com.whackdata.ParseArgs
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

object GetMinLocation {

  def run(conf: ParseArgs) = {
    val elevRasterPath = Paths.get(conf.elev_raster())

    val elevRaster = GeoTiffReader.readSingleband(
      elevRasterPath.toString,
      decompress = false,
      streaming = false
    )

    val (min, _) = elevRaster.tile.findMinMax

    var minList = List[(Int, Int)]()
    def recordMin(minElev: Int)(x: Int, y: Int, elevation: Int) = {
      if (elevation == minElev) minList = (x, y) :: minList
      elevation
    }

    elevRaster.tile.map(recordMin(min)(_,_,_))
    minList.foreach(println)

  }

}
