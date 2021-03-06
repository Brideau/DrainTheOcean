package com.whackdata.rasters

import geotrellis.raster.MutableArrayTile

import scala.util.Try

// Based on the flood fill algorithm by Lode Vandevenne:
// http://lodev.org/cgtutor/floodfill.html#8-Way_Method_With_Stack

// Modified to support wrap-around at the left and right edges
// to support geographic flood fills. Expects a mutable raster
// tile that has already been classified into areas with elevation
// above or below some threshold.

class FloodFill(val tileToFill: MutableArrayTile,
                val barrierVal: Int = 0,
                val reachableValue: Int = 1,
                val colorValue: Int = 2) {

  // The stack that stores yet-to-be-checked seed pixels in the
  // form of Array(x, y)
  private var rowStack = List[Array[Int]]()

  // Checks if a pixel represents a "barrier" point (elevation too high),
  // an "reachable" point (elevation at or below current threshold) or
  // returns as a Failure if the pixel is outside the raster's bounds
  private def needsPainting(x: Int, y: Int): Try[Boolean] = Try {
    tileToFill.get(col = x, row = y) == reachableValue
  }

  // Used to allow the flood fill algorithm to wrap around the
  // edges of the map raster
  def wrapLongitude(width: Int)(x: Int): Int = {
    if (x >= 1 && x <= width) x
    else if (x > width) x - width
    else x + width
  }

  def fill(x: Int, y: Int): Unit = {
    // If the starting pixel is a barrier or is already painted,
    // don't do anything
    if (needsPainting(x, y).getOrElse(false)) {

      // Add the starting pixel to the stack
      rowStack = Array(x, y) :: rowStack

      val tileWidth = tileToFill.cols
      val tileHeight = tileToFill.rows

      val wrap = wrapLongitude(tileWidth)(_)

      while (rowStack.nonEmpty) {
        // Pop the next pixel off the stack
        val currPixel = rowStack.head
        rowStack = rowStack.tail

        var currX = currPixel(0)
        val y = currPixel(1)

        // Find the left-most point in the row that requires painting
        while (
          needsPainting(wrap(currX), y).getOrElse(false) &&
          currPixel(0) - currX != tileWidth
        ) currX -= 1
        currX =  wrap(currX + 1)

        // Have we checked the rows above and below?
        var spanAbove = false
        var spanBelow = false

        // Are we at the start of the line for this row?
        var atStartAbove = true
        var atStartBelow = true

        // Scan the row left to right until you reach a barrier
        while (needsPainting(currX, y).getOrElse(false)) {
          // Colour in the pixels as you go
          tileToFill.set(currX, y, colorValue)

          // Check the diagonal at the start to support 8-way filling
          if (!spanAbove && needsPainting(wrap(currX - 1), y - 1).getOrElse(false) && atStartAbove) {
            rowStack = Array(wrap(currX - 1), y - 1) :: rowStack
            atStartAbove = false
          }

          // If the row above hasn't been checked yet, and the pixel above
          // the current pixel needs painting
          if (!spanAbove && needsPainting(currX, y - 1).getOrElse(false)) {

            // Add the pixel to the stack and set a flag saying that you've
            // check the row above
            rowStack = Array(currX, y - 1) :: rowStack
            spanAbove = true
            // Once you come across a barrier in the above row, reset the spanAbove
            // flag since it represents a discontinuity in the line above
          } else if (spanAbove && !needsPainting(currX, y - 1).getOrElse(false)) {
            spanAbove = false
          }

          if (!spanBelow && needsPainting(wrap(currX - 1), y + 1).getOrElse(false) && atStartBelow) {
            rowStack = Array(wrap(currX - 1), y + 1) :: rowStack
            atStartBelow = false
          }

          // Same as above, but checks the pixels below the current line
          if (!spanBelow && needsPainting(currX, y + 1).getOrElse(false)) {

            rowStack = Array(currX, y + 1) :: rowStack
          } else if (spanBelow && y < tileHeight && !needsPainting(currX, y + 1).getOrElse(false)) {
            spanBelow = false
          }

          currX = wrap(currX + 1)
        }

        // Check diagonals a the end of the row
        if (needsPainting(wrap(currX), y - 1).getOrElse(false)) {
          rowStack = Array(wrap(currX), y - 1) :: rowStack
        }

        if (needsPainting(wrap(currX), y + 1).getOrElse(false)) {
          rowStack = Array(wrap(currX), y + 1) :: rowStack
        }

      }

    }
  }
}
