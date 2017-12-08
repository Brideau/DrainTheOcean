package com.whackdata.pngs

import java.awt.{Color, Font}
import java.nio.file.Paths
import javax.imageio.ImageIO

import com.whackdata.Utils.ProcessedFile
import com.whackdata.{ParseArgs, Utils}
import org.slf4j.LoggerFactory

object AddTextPng {

  private val logger = LoggerFactory.getLogger("Add Text Logger")

  def run(conf: ParseArgs): Unit = {
    val outputPath = Paths.get(conf.output_path())
    val existingPngs = Utils.getAlreadyProcessed(outputPath, "Composed", "png")



    def addText(layer: ProcessedFile) = {
      logger.info(s"Adding text for elevation ${layer.elev}")
      val text = s"${math.abs(layer.elev)}m below current sea level"
      val image = ImageIO.read(layer.path.toFile)

      val graphics = image.getGraphics
      graphics.setColor(Color.BLACK)
      graphics.setFont(new Font("Arial", Font.BOLD, 200))
      graphics.drawString(text, 20, 160)

      val outName = Utils.getOutputPath(layer.path, outputPath, "WithText", layer.elev)
      ImageIO.write(image, "png", outName.toFile)
    }

    existingPngs.par.map(addText)

  }
}

