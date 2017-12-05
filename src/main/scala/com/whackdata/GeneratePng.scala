package com.whackdata

import java.nio.file.Paths

import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.{Raster, SinglebandRaster, Tile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.render.{ColorRamp, RGB}
import geotrellis.vector.Extent
import org.slf4j.LoggerFactory
import Utils._


object GeneratePng {

  private val logger = LoggerFactory.getLogger("PNG Logger")

  def run(conf: ParseArgs): Unit = {
    val baseRaster = conf.base_raster()

    baseRaster match {
      case "None" => generateWaterLayers(conf)
      case _ => generateBaseLayer(conf)
    }

  }

  private def reproject(tile: Tile): SinglebandRaster = {
    logger.info("Reprojecting the raster")
    val ext = Extent(-180, -90, 180, 90)
    val raster = Raster(tile, ext)
    raster.reproject(
      geotrellis.proj4.CRS.fromString("+proj=eqc"),
      geotrellis.proj4.CRS.fromString("+proj=robin")
    )
  }

  private def resize(geoTiff: SinglebandGeoTiff): Tile = {
    logger.info("Resizing layer to 1920x960")
    geoTiff.tile.resample(1920, 960)
  }

  private def generateBaseLayer(conf: ParseArgs): Unit = {
    val inputPath = Paths.get(conf.base_raster())
    val outputDir = Paths.get(conf.output_path())

    logger.info("Reading in base layer")
    val baseGeoTiff = GeoTiffReader.readSingleband(
      inputPath.toString,
      decompress = false,
      streaming = false
    )

    val smallTile = resize(baseGeoTiff)
    val projRaster = reproject(smallTile)

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
    val outputPath = Utils
      .getOutputPath(inputPath, outputDir, "PNG", 0)
      .toString.replace(".tif", ".png")
    smallPng.write(outputPath)
  }

  private def generateWaterLayers(conf: ParseArgs): Unit = {

    val outPath = Paths.get(conf.output_path())
    val waterLayers = getAlreadyProcessed(outPath, "Water", "tif")

    for (layer <- waterLayers) {
      logger.info(s"Loading water layer @ elevation ${layer.elev}")
      val waterRaster = GeoTiffReader.readSingleband(
        layer.path.toString,
        decompress = false,
        streaming = false
      )

      val smallTile = resize(waterRaster)
      val projRaster = reproject(smallTile)

      val colourRamp = ColorRamp(RGB(0, 33, 143), RGB(0, 33, 143))
      val breaks = Array(0, 4)
      val colouredTile = projRaster.tile.color(colourRamp.toColorMap(breaks))

      logger.info("Rendering water layer PNG")
      val smallPng = colouredTile.renderPng()

      logger.info("Writing out water layer PNG")
      val outputPath = Utils
        .getOutputPath(layer.path, outPath, "PNG", layer.elev)
        .toString.replace(".tif", ".png")
      smallPng.write(outputPath)

    }

  }

}
