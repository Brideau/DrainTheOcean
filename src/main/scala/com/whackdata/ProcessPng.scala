package com.whackdata

import java.nio.file.Paths

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.composite._
import org.slf4j.LoggerFactory

object ProcessPng {

  private val logger = LoggerFactory.getLogger("PNG Processor Logger")

  def run(conf: ParseArgs): Unit = {

    logger.info("Starting to combine PNGs")
    val outputPath = Paths.get(conf.output_path())
    val existingPngs = Utils.getAlreadyProcessed(outputPath, "PNG", "png")

    val basePng = existingPngs
      .filter(_.elev == 0)
      .filter(_.path.getFileName.toString.contains("ETOP"))
      .head
    val baseImage = Image.fromPath(basePng.path)

    val waterPngs = existingPngs
      .filterNot(_.path.getFileName.toString.contains("ETOP"))
      .sortBy(-_.elev)

    for (wtImg <- waterPngs) {
      logger.info(s"Processing PNG for elevation ${wtImg.elev}")
      val img = Image.fromPath(wtImg.path)
      val composite = baseImage.composite(new ColorComposite(0.5), img)

      val outputName = Utils.getOutputPath(wtImg.path, outputPath, "Composed", wtImg.elev)
      composite.output(outputName)
    }

  }

}
