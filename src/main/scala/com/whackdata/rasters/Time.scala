package com.whackdata.rasters

import java.io.File
import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem}
import breeze.numerics.constants._
import breeze.numerics._
import com.whackdata.{ParseArgs, Utils, WriterActor}
import com.github.tototoshi.csv._
import com.whackdata.Utils.{ProcessedFile, getAlreadyProcessed}
import com.whackdata.WriterActor.CsvLine
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import org.slf4j.LoggerFactory

import scala.io.StdIn
import scala.util.{Failure, Success}
import Utils._

object Time {

  implicit val system: ActorSystem = ActorSystem("csv-writer-actor-system")

  private val logger = LoggerFactory.getLogger("Time Logger")

  private def calculateTimeYears(rasterCells: Long, elev: Int): Double = {
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
    round(volumeWater / flowRate / secondsPerYear)
  }

  def calculate(conf: ParseArgs): Unit = {

    val outputPath = Paths.get(conf.output_path())

    val fileName = "TimeResults.csv"
    val file = new File(outputPath.toString, fileName)

    val records = getCsv(file) match {
      case Success(result) => result
      case Failure(_) => List()
    }

    Files.createDirectories(outputPath.resolve("FloodFill"))
    val floodFillMasks = getAlreadyProcessed(outputPath, "FloodFill", "tif")

    val alreadyProcessed = records.map(_.head.toDouble.toInt)
    val remainingElev = floodFillMasks.map(_.elev).diff(alreadyProcessed)
    val toProcess = floodFillMasks.filter(x => remainingElev.contains(x.elev))

    val writer = CSVWriter.open(file, append = true)
    val writerActor = system.actorOf(WriterActor.props(writer))

    def processLayer(wa: ActorRef)(file: ProcessedFile): Unit = {
      logger.info(s"Calculating drain time for layer ${file.elev}")

      val floodRaster = GeoTiffReader.readSingleband(
        file.path.toString,
        decompress = false,
        streaming = false
      )

      // Get the surface area count of cells for the water that
      // dropped as a result of the draining
      val cells = floodRaster.tile.histogram.itemCount(2)
      val time = calculateTimeYears(cells, file.elev)

      wa ! CsvLine(List(file.elev, time))
    }
    val processLayerWActor = processLayer(writerActor)(_)

    Utils.timems {
      toProcess.par.foreach(processLayerWActor)
    }

    logger.info(s"Press RETURN to stop...")
    StdIn.readLine()
    system.terminate;
    System.exit(0)

  }


}
