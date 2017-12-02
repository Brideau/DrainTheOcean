package com.whackdata

import java.nio.file.Paths

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
  println(imagePath)

}
