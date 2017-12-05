package com.whackdata

import java.nio.file.Paths

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster.{GridBounds, Raster}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.render.{ColorRamp, RGB}
import geotrellis.raster.reproject.Reproject
import geotrellis.vector.Extent
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

    logger.info("Resizing base layer to 1920x960")
    val smallTile = baseGeoTiff.tile.resample(1920, 960)

    logger.info("Reprojecting the raster")
    val tile = smallTile
    val ext = Extent(-180, -90, 180, 90)
    val raster = Raster(tile, ext)
    val projRaster = raster.reproject(
      geotrellis.proj4.CRS.fromString("+proj=eqc"),
      geotrellis.proj4.CRS.fromString("+proj=robin")
    )

    logger.info("Colouring the raster")
    val (min, max) = baseGeoTiff.tile.findMinMax
    val diff = max - min
    val nearMax = max - diff * 0.1
    val nearMin = min + diff * 0.1

    val colourRamp = ColorRamp(RGB(120, 66, 0), RGB(255, 255, 255))
    val breaks = (nearMax to nearMin).by(diff / -100).toArray
    val colouredTile = projRaster.tile.color(colourRamp.stops(100).toColorMap(breaks))

    logger.info("Rendering base layer PNG")
    val smallPng = colouredTile.renderPng()

    logger.info("Writing out base layer PNG")
    val outputPath = Utils.getOutputPath(inputPath, outputDir, "PNG", 0)
    smallPng.write(outputPath.toString)
  }

  def generateWaterLayers(conf: ParseArgs): Unit = {

  }

}
