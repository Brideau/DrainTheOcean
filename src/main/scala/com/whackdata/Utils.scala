package com.whackdata

import java.nio.file.Path

object Utils {

  def getOutputPath(inputPath: Path): Path = {
    val inFileName = inputPath.getFileName.toString
    val inDirectory = inputPath.getParent

    val (baseName, ext) = inFileName.splitAt(inFileName.lastIndexOf('.'))
    val outName = baseName + "Processed" + ext
    inDirectory.resolve(outName)
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
