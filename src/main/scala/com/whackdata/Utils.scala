package com.whackdata

import java.io.File
import java.nio.file.{Files, Path}

import com.github.tototoshi.csv.CSVReader

import scala.collection.JavaConverters._
import scala.util.Try

object Utils {

  def getOutputPath(inputPath: Path, outputPath: Path, outputType: String, elevation: Int): Path = {
    val inFileName = inputPath.getFileName.toString

    val fullOutputPath = outputPath.resolve(outputType)
    Files.createDirectories(fullOutputPath)

    val paddedElev = "%06d".format(elevation)

    val (baseName, ext) = inFileName.splitAt(inFileName.lastIndexOf('.'))
    val strippedBaseName = baseName.split('_')(0)

    val outName = strippedBaseName + "Processed_" + paddedElev + ext
    fullOutputPath.resolve(outName)
  }

  case class ProcessedFile(elev: Int, path: Path)

  def getAlreadyProcessed(outputPath: Path, subFolder: String, fileExt: String): List[ProcessedFile] = {
    // Get the paths of the water files already processed
    val existingFileList = Files.newDirectoryStream(outputPath.resolve(subFolder))
    // Convert stream to a Scala vector
    val fileList = existingFileList.iterator().asScala.toList

    // Filter out any files that aren't tifs
    val filePaths = fileList
      .filter(_.toString.split('.').last == fileExt)

    // Extract the elevation from the filename
    val fileElev = filePaths
      .map(_.getFileName)
      .map(_.toString)
      .map(fn => fn.splitAt(fn.lastIndexOf('_'))._2)
      .map(_.replaceAll("_", "").replaceAll("." + fileExt, ""))
      .map(_.toInt)

    fileElev
      .zip(filePaths)
      .map(tup => ProcessedFile(tup._1, tup._2))
  }

  def getCsv(file: File): Try[List[List[String]]] = Try {
    val reader = CSVReader.open(file)
    val records = reader.all()
    reader.close()
    records
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
