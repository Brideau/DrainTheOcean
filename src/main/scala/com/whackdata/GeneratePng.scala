package com.whackdata

import java.nio.file.Paths

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.render.ColorRamps
import org.slf4j.LoggerFactory

object GeneratePng {

  private val logger = LoggerFactory.getLogger("PNG Logger")

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

    logger.info("Reading in base layer")
    val baseGeoTiff = GeoTiffReader.readSingleband(
      inputPath.toString,
      decompress = false,
      streaming = false
    )

    val colourRamp = ColorRamps.ClassificationMutedTerrain
    val (min, max) = baseGeoTiff.tile.findMinMax
    val breaks = (max to min).by(-18).toArray
    val colouredTile = baseGeoTiff.tile.color(colourRamp.stops(100).toColorMap(breaks))

    logger.info("Resizing base layer to 1920x960")
    val smallTile = colouredTile.resample(1920, 960)

    logger.info("Rendering base layer PNG")
    val smallPng = smallTile.renderPng()

    logger.info("Writing out base layer PNG")
    val outputPath = Utils.getOutputPath(inputPath, outputDir, "PNG", 0)
    smallPng.write(outputPath.toString)
  }

  def generateWaterLayers(conf: ParseArgs): Unit = {

  }

}
