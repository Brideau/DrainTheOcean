package com.whackdata

import java.io.BufferedInputStream
import java.nio.file.{Files, Paths}

import com.sksamuel.scrimage.{Color, Image, Pixel}
import com.sksamuel.scrimage.filter.BlurFilter
import org.rogach.scallop.{ScallopConf, ScallopOption}

class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val image: ScallopOption[String] = opt[String]()
  verify()
}

object Main extends App {

  // Make a copy so that you don't modify the original
  // Use filters. Filters can be chained.
  // Use overlay
  // Map function for going over each pixel
  // .color returns the value at a coordinate
  // .pix returns the pixel at a point

  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val imageIn = new BufferedInputStream(Files.newInputStream(imagePath))
  val imageOut = Utils.getOutputPath(imagePath)

  def printPixel(x: Int, y: Int, pixel: Pixel): Pixel = {
    val color = pixel.toColor
    println(s"X: $x Y: $y RGB: (${color.red}, ${color.green}, ${color.blue})")
    pixel
  }

  def classifyBinary(nonBorderColor: Color)(x: Int, y: Int, pixel: Pixel): Pixel = {
    if (pixel.toColor == nonBorderColor) Pixel.apply(Color.White)
    else Pixel.apply(Color.Black)
  }

  def NaiveFill(x: Int, y: Int, fillColour: Color, borderColor: Color)(image: Image): Image = {
    try {
      if (borderColor != image.color(x, y).toAWT) {
        val img1 = NaiveFill(x, y, fillColour, borderColor)(image)
        val img2 = NaiveFill(x - 1, y, fillColour, borderColor)(img1)
        val img3 = NaiveFill(x + 1, y, fillColour, borderColor)(img2)
        val img4 = NaiveFill(x, y - 1, fillColour, borderColor)(img3)
        NaiveFill(x, y + 1, fillColour, borderColor)(img4)
      } else {
        image
      }
    } catch {
      case _: Exception => image
    }
  }

  val imgCopy = Image.fromStream(imageIn).copy
  val (width, height) = imgCopy.dimensions
  val fillBlack = NaiveFill(width / 2, height / 2, Color.Black, Color.Black)(_)

  Utils.timems {
    val reclassImg = imgCopy.map(classifyBinary(Color.White)(_,_,_))
    val filledImg = fillBlack(reclassImg)
    filledImg.output(imageOut)
  }

}
