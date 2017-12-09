package com.whackdata.pngs

import java.awt.{Color, Font}
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO

import com.whackdata.Utils.{ProcessedFile, getCsv}
import com.whackdata.{ParseArgs, Utils}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object AddTextPng {

  private val logger = LoggerFactory.getLogger("Add Text Logger")

  def run(conf: ParseArgs): Unit = {
    val outputPath = Paths.get(conf.output_path())
    val existingPngs = Utils.getAlreadyProcessed(outputPath, "Composed", "png")

    val fileName = "TimeResults.csv"
    val file = new File(outputPath.toString, fileName)

    val timeRecords = getCsv(file) match {
      case Success(result) => result
      case Failure(ex) => println("Missing time calculations: " + ex.getMessage)
        System.exit(1)
        List()
    }

    // Elev     Time
    //  0         0
    //  -10      0 + time_for_elev_0

    val timeRecordPrep = timeRecords.map{
      case elev::time => (elev.toDouble, time.head.toDouble)
    }.sortBy(-_._1)
    val elevs = timeRecordPrep.map(_._1)

    val times = timeRecordPrep.map(_._2)
    val cumTime = times.scanLeft(0.0)((b, a) => b + a)


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

