package com.whackdata

import java.io.BufferedInputStream
import java.nio.file.{Files, Paths}

import com.sksamuel.scrimage.Image
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
  val conf = new ParseArgs(args)
  val imagePath = Paths.get(conf.image())

  val imageIn = new BufferedInputStream(Files.newInputStream(imagePath))
  val imageOut = Utils.getOutputPath(imagePath)

  Image.fromStream(imageIn).filter(BlurFilter).flipX.output(imageOut)

}
