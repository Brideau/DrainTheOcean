package com.whackdata.pngs

import java.nio.file.Paths

import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.{ParseArgs, Utils}
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.render.{ColorRamp, RGB}
import geotrellis.raster.{Raster, SinglebandRaster, Tile}
import geotrellis.vector.Extent
import org.slf4j.LoggerFactory
object GeneratePng {

  // TODO: Refactor to remove this useless case matching

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
    logger.info("Downsampling layer")
    geoTiff.tile.resample(10800, 5400)
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

    val waterPngs = getAlreadyProcessed(outPath, "PNG", "png")

    val oceanBottom = -10800
    val numTotal = (0 to oceanBottom by -10).length
    logger.info(s"${waterPngs.length} of $numTotal have already been processed")

    val waterLayerList: List[ProcessedFile] = if (waterPngs.nonEmpty) {
      logger.info(s"Restarting where processing left off last time")
      val alreadyProcessed = waterPngs.map(_.elev)

      waterLayers.filterNot(x => alreadyProcessed.contains(x.elev))
    } else waterLayers

    Utils.timems {
      def createPng(layer: ProcessedFile): Unit = {
        logger.info(s"Loading water layer @ elevation ${layer.elev}")
        val waterRaster = GeoTiffReader.readSingleband(
          layer.path.toString,
          decompress = false,
          streaming = false
        )

        val smallTile = resize(waterRaster)
        val projRaster = reproject(smallTile)

        val colourRamp = ColorRamp(RGB(0, 33, 143), RGB(0, 33, 143))
        val breaks = (0 to 10).toArray
        val colouredTile = projRaster.tile.color(colourRamp.stops(10).toColorMap(breaks))

        logger.info("Rendering water layer PNG")
        val smallPng = colouredTile.renderPng()

        logger.info("Writing out water layer PNG")
        val outputPath = Utils
          .getOutputPath(layer.path, outPath, "PNG", layer.elev)
          .toString.replace(".tif", ".png")
        smallPng.write(outputPath)
      }

      waterLayerList.par.foreach(createPng)

    }

  }

}
