package com.whackdata

import java.io.BufferedInputStream
import java.nio.file.{Files, Paths}

import com.sksamuel.scrimage.{Image, Pixel}
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

  def printPixel(x: Int, y: Int, pixel: Pixel) = {
    val color = pixel.toColor
    println(s"X: $x Y: $y RGB: (${color.red}, ${color.green}, ${color.blue})")
  }

  Image.fromStream(imageIn)
    .foreach(printPixel)

}
