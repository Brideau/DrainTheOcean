package com.whackdata

import java.nio.file.{Files, Path}

object Utils {

  def getOutputPath(inputPath: Path, outputPath: Path, outputType: String, elevation: Int): Path = {
    val inFileName = inputPath.getFileName.toString

    val fullOutputPath = outputPath.resolve(outputType)
    Files.createDirectories(fullOutputPath)

    val paddedElev = "%06d".format(elevation)

    val (baseName, ext) = inFileName.splitAt(inFileName.lastIndexOf('.'))
    val outName = baseName + "Processed_" + paddedElev + ext
    fullOutputPath.resolve(outName)
  }

  def timems[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }

  def timens[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ss")
    result
  }

}
