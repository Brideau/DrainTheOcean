package com.whackdata

import geotrellis.raster.{MutableArrayTile, Tile}
import util.control.Breaks._

class FloodFill(val tileToFill: MutableArrayTile,
                val barrierVal: Int = 0,
                val reachableValue: Int = 1,
                val colorValue: Int = 2) {

  // A simple class to store information about rows of the raster
  // that still need to be scanned
  private case class RasterRow(xMin: Int,
                               xMax: Int,
                               y: Int,
                               down: Boolean,
                               extendLeft: Boolean,
                               extendRight: Boolean)

  // The stack where yet-to-be-scanned rows will be stored
  private var rowStack = List[RasterRow]()

  // A simple function for checking if a pixel should be painted
  // Need to double check this as it might require it to be OK
  // if a cell gets painted twice
  private def needsPainting(x: Int, y: Int): Boolean = {
    tileToFill.get(col = x, row = y) == reachableValue
  }

  private def addNextLine(minX: Int,
                          maxX: Int,
                          newY: Int,
                          startingX: Int,
                          prevRow: RasterRow,
                          isNext: Boolean,
                          downwards: Boolean) = {
    var rangeMinX = minX
    var inRange = false

    breakable {
      (minX to maxX).foreach { x =>
        val empty = (isNext || x < prevRow.xMin || x > prevRow.xMax) && needsPainting(x, newY)

        if (!inRange && empty) {
          rangeMinX = x
          inRange = true
        } else if (inRange && !empty) {
          rowStack = RasterRow(xMin = rangeMinX,
            xMax = x - 1,
            y = newY,
            down = downwards,
            extendLeft = rangeMinX == minX,
            extendRight = false) :: rowStack
          inRange = false
        }

        if (inRange) {
          tileToFill.set(col = x, row = newY, value = colorValue)
        }

        if (!isNext && x == prevRow.xMin) {
          break
        }
      }
    }

    if (inRange) {
      rowStack = RasterRow(xMin = rangeMinX,
        xMax = startingX - 1,
        y = newY,
        down = downwards,
        extendLeft = rangeMinX == minX,
        extendRight = true) :: rowStack
    }
  }

  def fill(x: Int, y: Int): Unit = {

    // Add the starting point to the stack
    val initialRow = RasterRow(xMin = x,
                               xMax = x,
                               y = y,
                               down = null,
                               extendLeft = true,
                               extendRight = true)
    rowStack = initialRow :: rowStack

    // Paint the starting pixel
    tileToFill.set(col = x, row = y, value = colorValue)

    while (rowStack.nonEmpty) {
      // Pop off the next value in the stack
      val currRow = rowStack.head
      rowStack = rowStack.tail

      val down = if (currRow.down)  true else false
      val up =   if (!currRow.down) true else false

      var minX = currRow.xMin
      val y = currRow.y

      // Paint leftward until you reach a barrier
      if (currRow.extendLeft) {
        while (minX > 0 && needsPainting(minX - 1, y)) {
          minX = minX - 1
          tileToFill.set(col = minX, row = y, value = colorValue)
        }
      }

      var maxX = currRow.xMax
      if (currRow.extendRight) {
        while (maxX < tileToFill.cols - 1 && needsPainting(maxX + 1, y)) {
          maxX = maxX + 1
          tileToFill.set(col = maxX, row = y, value = colorValue)
        }
      }

      if (y < tileToFill.rows) addNextLine(minX = minX,
                                           maxX = maxX,
                                           newY = y + 1,
                                           startingX = x,
                                           prevRow = currRow,
                                           isNext = !up,
                                           downwards = true)

      if (y < tileToFill.rows) addNextLine(minX = minX,
                                           maxX = maxX,
                                           newY = y - 1,
                                           startingX = x,
                                           prevRow = currRow,
                                           isNext = !down,
                                           downwards = false)

    }

  }
}
