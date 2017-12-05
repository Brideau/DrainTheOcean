package com.whackdata

import java.awt.{Color, Font}
import java.nio.file.Paths
import javax.imageio.ImageIO

import com.whackdata.Utils.ProcessedFile
import org.slf4j.LoggerFactory

object AddTextPng {

  private val logger = LoggerFactory.getLogger("Add Text Logger")

  def run(conf: ParseArgs): Unit = {
    val outputPath = Paths.get(conf.output_path())
    val existingPngs = Utils.getAlreadyProcessed(outputPath, "Composed", "png")


    def addText(layer: ProcessedFile) = {
      logger.info(s"Adding text for elevation ${layer.elev}")
      val text = s"Elevation ${layer.elev}"
      val image = ImageIO.read(layer.path.toFile)

      val graphics = image.getGraphics
      graphics.setColor(Color.WHITE)
      graphics.fillRect(0, 0, 200, 50)
      graphics.setColor(Color.BLACK)
      graphics.setFont(new Font("Arial Black", Font.BOLD, 40))
      graphics.drawString(text, 10, 25)

      val outName = Utils.getOutputPath(layer.path, outputPath, "WithText", layer.elev)
      ImageIO.write(image, "png", outName.toFile)
    }

    existingPngs.par.map(addText)

  }
}

