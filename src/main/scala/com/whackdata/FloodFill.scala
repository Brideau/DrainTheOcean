package com.whackdata

import geotrellis.raster.MutableArrayTile

import scala.util.Try

// From http://lodev.org/cgtutor/floodfill.html#8-Way_Method_With_Stack

class FloodFill(val tileToFill: MutableArrayTile,
                val barrierVal: Int = 0,
                val reachableValue: Int = 1,
                val colorValue: Int = 2) {

  // A simple class to store information about rows of the raster
  // that still need to be scanned
  private case class Pixel(x: Int, y: Int)

  // The stack where yet-to-be-scanned rows will be stored
  private var rowStack = List[Pixel]()

  private def needsPainting(x: Int, y: Int): Try[Boolean] = Try {
    tileToFill.get(col = x, row = y) == reachableValue
  }

  def fill(x: Int, y: Int): Unit = {
    if (needsPainting(x, y).getOrElse(false)) {

      rowStack = Pixel(x, y) :: rowStack

      val tileWidth = tileToFill.cols
      val tileHeight = tileToFill.rows

      while (rowStack.nonEmpty) {
        val currPixel = rowStack.head
        rowStack = rowStack.tail

        var x1 = currPixel.x
        val y = currPixel.y
        // Find the left-most point in the row that requires painting
        while (needsPainting(x1, y).getOrElse(false)) x1 = x1 - 1
        x1 = x1 + 1

        // Have we checked the rows above and below?
        var spanAbove = false
        var spanBelow = false

        // Scan the row left to right until you reach a barrier
        while (x1 < tileWidth && needsPainting(x1, y).getOrElse(false)) {
          // Colour in the pixels as you go
          tileToFill.set(x1, y, colorValue)

          // If the row above hasn't been checked yet, and the pixel above
          // the current pixel needs painting
          if (!spanAbove && needsPainting(x1, y - 1).getOrElse(false)) {
            // Add the pixel to the stack and set a flag saying that you've
            // check the row above
            rowStack = Pixel(x1, y - 1) :: rowStack
            spanAbove = true
            // Once you come across a barrier in the above row, reset the spanAbove
            // flag since it represents a discontinuity in the line above
          } else if (spanAbove && !needsPainting(x1, y - 1).getOrElse(false)) {
            spanAbove = false
          }

          // Same as above, but checks the pixels below the current line
          if (!spanBelow && needsPainting(x1, y + 1).getOrElse(false)) {
            rowStack = Pixel(x1, y + 1) :: rowStack
          } else if (spanBelow && y < tileHeight && !needsPainting(x1, y + 1).getOrElse(false)) {
            spanBelow = false
          }

          x1 = x1 + 1
        }

      }

    }
  }
}
