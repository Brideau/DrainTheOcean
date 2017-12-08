package com.whackdata.rasters

import java.io.File
import java.nio.file.Paths

import breeze.numerics.constants._
import breeze.numerics._
import com.whackdata.ParseArgs
import com.github.tototoshi.csv._

import scala.util.{Failure, Success, Try}

object Time {

  def calculateTimeYears(rasterCells: Int, elev: Int): Double = {
    val heightDiff = 10 // metres

    // Each cell is about 1855m on both sides (very rough)
    val cellArea = pow(1855.324, 2)
    val volumeWater: Double = heightDiff * rasterCells * cellArea

    val holeRadius = 5 // metres
    val holeArea: Double = Pi * pow(holeRadius, 2)

    val oceanBottom = -10800 // metres
    val waterHeight = elev - oceanBottom
    val flowRate: Double = sqrt(2 * StandardAccelerationOfGravity * waterHeight) * holeArea

    val secondsPerYear: Double = 365.25 * 24 * 60 * 30
    volumeWater / flowRate / secondsPerYear
  }

  def calculate(conf: ParseArgs) = {
    val data = List((0, 1000), (-10, 800), (-20, 700), (-30, 45345), (-40, 232123))

    val outputPath = Paths.get(conf.output_path())

    val fileName = "TimeResults.csv"
    val file = new File(outputPath.toString, fileName)

    def getCsv(file: File) = Try {
      val reader = CSVReader.open(file)
      val records = reader.all()
      reader.close()
      records
    }

    val records = getCsv(file) match {
      case Success(result) => result
      case Failure(_) => List()
    }

    val alreadyProcessed = records.map(_.head.toDouble.toInt)

    val remainingElev = data.map{case (a, _) => a}.diff(alreadyProcessed)
    remainingElev.foreach(println)
    val toProcess = data.filter{case (elev, _) => remainingElev.contains(elev)}

    val writer = CSVWriter.open(file, append = true)

    toProcess.map{case (elev, cells) => List(elev, calculateTimeYears(cells, elev))}
      .foreach(writer.writeRow)

    writer.close()
  }


}
