package com.whackdata.pngs

import java.nio.file.Paths
import javax.imageio.ImageIO

import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.whackdata.ParseArgs
import com.whackdata.Utils._
import org.slf4j.LoggerFactory

object Animate {

  private val logger = LoggerFactory.getLogger("Animation Logger")

  def run(conf: ParseArgs): Unit = {

    val outPath = Paths.get(conf.output_path())
    val outFile = outPath.resolve("FinalAnimation.gif")

    val waterPngs = getAlreadyProcessed(outPath, "Composed", "png")

    val waterPngsSorted = waterPngs.sortBy(-_.elev)

    logger.info("Starting encoder")
    val encoder = new AnimatedGifEncoder
    encoder.start(outFile.toString)
    encoder.setFrameRate(30)

    def prepareFrame(file: ProcessedFile) = {
      logger.info(s"Adding frame for ${file.elev}")
      ImageIO.read(file.path.toFile)
    }

    logger.info("Starting encoding")
    for (png <- waterPngsSorted) encoder.addFrame(prepareFrame(png))

    encoder.finish()

  }
}
