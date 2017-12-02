package com.whackdata

import java.nio.file.Paths
import geotrellis.raster._

import org.rogach.scallop.{ScallopConf, ScallopOption}

class ParseArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
  val image: ScallopOption[String] = opt[String]()
  verify()
}

object Main extends App {

  // Geotrellis hydrology fill

  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val imageOut = Utils.getOutputPath(imagePath)



//  def printPixel(x: Int, y: Int, pixel: Pixel): Pixel = {
//    val color = pixel.toColor
//    println(s"X: $x Y: $y RGB: (${color.red}, ${color.green}, ${color.blue})")
//    pixel
//  }
//
//  def classifyBinary(nonBorderColor: Color)(x: Int, y: Int, pixel: Pixel): Pixel = {
//    if (pixel.toColor == nonBorderColor) Pixel.apply(Color.White)
//    else Pixel.apply(Color.Black)
//  }

}
