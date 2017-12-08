package com.whackdata

import akka.actor._
import com.github.tototoshi.csv._

object WriterActor {
  def props(writer: CSVWriter) = Props(new WriterActor(writer))

  case class CsvLine(line: List[Double])
}

class WriterActor(writer: CSVWriter) extends Actor with ActorLogging {
  import WriterActor._

  override def preStart(): Unit = {
    log.info("\nCSV writer has started")
  }

  override def receive: PartialFunction[Any, Unit] = {
    case CsvLine(line) =>
      writer.writeRow(line)
    case _ =>
      log.info("Unknown message type")
  }

  override def postStop(): Unit = {
    writer.close()
    log.info("\nCSV writer has stopped")
  }
}
