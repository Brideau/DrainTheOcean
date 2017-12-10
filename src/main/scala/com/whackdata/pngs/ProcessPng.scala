package com.whackdata.pngs

import java.nio.file.Paths

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.composite._
import com.whackdata.Utils.ProcessedFile
import com.whackdata.{ParseArgs, Utils}
import org.slf4j.LoggerFactory

object ProcessPng {

  private val logger = LoggerFactory.getLogger("PNG Processor Logger")

  def run(conf: ParseArgs): Unit = {

    logger.info("Starting to combine PNGs")
    val outPath = Paths.get(conf.output_path())
    val waterPngs = Utils.getAlreadyProcessed(outPath, "PNG", "png")

    val composedPngs = Utils.getAlreadyProcessed(outPath, "Composed", "png")

    val oceanBottom = -10800
    val numTotal = (0 to oceanBottom by -10).length
    logger.info(s"${composedPngs.length} of $numTotal have already been processed")

    val waterLayerList: List[ProcessedFile] = if (composedPngs.nonEmpty) {
      logger.info(s"Restarting where processing left off last time")
      val alreadyProcessed = composedPngs.map(_.elev)

      waterPngs.filterNot(x => alreadyProcessed.contains(x.elev))
    } else waterPngs

    val basePng = waterPngs
      .filter(_.elev == 0)
      .filter(_.path.getFileName.toString.contains("ETOP"))
      .head
    logger.info("Loading base image")
    val baseImage = Image.fromPath(basePng.path)

    val toProcess = waterLayerList
      .filterNot(_.path.getFileName.toString.contains("ETOP"))



      def composeImage(base: Image)(wtImg: Utils.ProcessedFile) = {
        Utils.timems {
          logger.info(s"Processing PNG for elevation ${wtImg.elev}")
          val img = Image.fromPath(wtImg.path)
          val composite = base.composite(new AlphaComposite(0.5), img)

          val outputName = Utils.getOutputPath(wtImg.path, outPath, "Composed", wtImg.elev)
          composite.output(outputName)
        }
      }

      toProcess.par.map(composeImage(baseImage)(_))

  }

}
