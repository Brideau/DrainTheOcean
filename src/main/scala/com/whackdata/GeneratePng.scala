package com.whackdata

import java.nio.file.{Path, Paths}

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample.ResampleMethod

object GeneratePng {

  def run(conf: ParseArgs): Unit = {
    val baseRaster = conf.base_raster()
    val water = conf.water_dir()

    baseRaster match {
      case "None" =>
      case _ => generateBaseLayer(conf)
    }

    water match {
      case "None" =>
      case _ => generateWaterLayers(conf)
    }

  }

  def generateBaseLayer(conf: ParseArgs): Unit = {
    val inputPath = Paths.get(conf.base_raster())
    val outputDir = Paths.get(conf.output_path())

    val baseGeoTiff = GeoTiffReader.readSingleband(
      inputPath.toString,
      decompress = false,
      streaming = false
    )

    val smallTile = baseGeoTiff.tile.resample(1920, 960)
    val smallPng = smallTile.renderPng()

    val outputPath = Utils.getOutputPath(inputPath, outputDir, "PNG", 0)
    smallPng.write(outputPath.toString)
  }

  def generateWaterLayers(conf: ParseArgs): Unit = {

  }

}
