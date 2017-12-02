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

}
