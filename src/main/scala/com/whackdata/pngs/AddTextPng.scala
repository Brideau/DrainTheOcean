package com.whackdata.pngs

import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, Font, Graphics2D, Image, RenderingHints}
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO

import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.whackdata.Utils.{ProcessedFile, getCsv}
import com.whackdata.{ParseArgs, Utils}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object AddTextPng {

  private val logger = LoggerFactory.getLogger("Add Text Logger")

  def toBufferedImage(img: Image): BufferedImage = {
    img match {
      case image: BufferedImage => image
      case image =>
        val bImage = new BufferedImage(
          image.getWidth(null),
          image.getHeight(null),
          BufferedImage.TYPE_INT_ARGB
        )
        // Draw the image on to the buffered image
        val bGr = bImage.createGraphics
        bGr.drawImage(image, 0, 0, null)
        bGr.dispose()
        // Return the buffered image
        bImage
    }
  }

  def run(conf: ParseArgs): Unit = {
    val outPath = Paths.get(conf.output_path())
    val outFile = outPath.resolve("FinalAnimation.gif")

    val existingPngs = Utils.getAlreadyProcessed(outPath, "Composed", "png")
    val existingPngsSorted = existingPngs.sortBy(-_.elev)

    val fileName = "TimeResults.csv"
    val file = new File(outPath.toString, fileName)

    val timeRecords = getCsv(file) match {
      case Success(result) => result
      case Failure(ex) => println("Missing time calculations: " + ex.getMessage)
        System.exit(1)
        List()
    }

    val timeRecordPrep = timeRecords.map{
      case elev::time => (elev.toDouble, time.head.toDouble)
    }.sortBy(-_._1)
    val elevs = timeRecordPrep.map(_._1.toLong)

    val times = timeRecordPrep.map(_._2)
    val cumTime = times.scanLeft(0.0)((b, a) => b + a).map(_.toLong)
    val timeLookup = elevs.zip(cumTime).toMap

    logger.info("Starting encoder")
    val encoder = new AnimatedGifEncoder
    encoder.start(outFile.toString)
    encoder.setRepeat(0)
    encoder.setFrameRate(30)

    // Properties for setting the text style
    val formatter = java.text.NumberFormat.getIntegerInstance
    val outlineColor = Color.white
    val fillColor = Color.black
    val stroke = new BasicStroke(10)


    def addText(layer: ProcessedFile) = Utils.timems {
      logger.info(s"Adding text for elevation ${layer.elev}")

      val year = timeLookup.getOrElse(layer.elev, 0)
      val yearStr = formatter.format(year)
      val text = s"$yearStr Years After Pulling the Plug"

      val image = ImageIO.read(layer.path.toFile)

      val graphics = image.getGraphics.asInstanceOf[Graphics2D]
      val originalColor = graphics.getColor
      val originalStroke = graphics.getStroke
      val originalHints = graphics.getRenderingHints

      graphics.setFont(new Font("Roboto", Font.BOLD, 240))
      graphics.setTransform(AffineTransform.getTranslateInstance(3200, 200))
      val glyphVector = graphics.getFont.createGlyphVector(
        graphics.getFontRenderContext, text
      )
      val textShape = glyphVector.getOutline

      graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
      )
      graphics.setRenderingHint(
        RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY
      )

      graphics.setColor(outlineColor)
      graphics.setStroke(stroke)

      graphics.draw(textShape)

      graphics.setColor(fillColor)
      graphics.fill(textShape)

      graphics.setColor(originalColor)
      graphics.setStroke(originalStroke)
      graphics.setRenderingHints(originalHints)

      val scaled = toBufferedImage(image.getScaledInstance(1920, 1087, Image.SCALE_SMOOTH))

      encoder.addFrame(scaled)
    }

    existingPngsSorted.foreach(addText)

    encoder.finish()

  }
}

