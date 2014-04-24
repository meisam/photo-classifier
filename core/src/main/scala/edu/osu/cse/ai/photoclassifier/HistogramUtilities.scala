/*
 * Copyright (c) 2014.
 *
 *   This file is part of picture-classifier.
 *
 *     picture-classifier is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     picture-classifier is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.osu.cse.ai.photoclassifier

import javax.media.jai._
import java.awt.image.renderable.ParameterBlock
import java.io.File
import org.apache.log4j.Logger

/**
 * Created by fathi on 4/23/14.
 */
object HistogramUtilities extends Logging {
  override def logger: Logger = Logger.getLogger(this.getClass)

  // To get rid of "Could not find mediaLib accelerator wrapper classes" error.
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def histogramToFeaturesArray(histograms: Seq[Histogram]): Array[Double] = {
    histograms.flatMap(h => h.getBins).flatten.toArray.map(_.toDouble)
  }

  def getHistogram(image: PlanarImage): Histogram = {
    // set up the histogram
    val bins = Array(5)
    val low = Array(0.0)
    val high = Array(256.0)

    val pb = new ParameterBlock()
    pb.addSource(image)
    pb.add(null)
    pb.add(1)
    pb.add(1)
    pb.add(bins)
    pb.add(low)
    pb.add(high)

    val op = JAI.create("histogram", pb, null)
    val histogram = op.getProperty("histogram").asInstanceOf[Histogram]

    // get histogram contents
    println("histogram.getBins.length=%d".format(histogram.getBins.length))
    histogram
  }

  def processFile(file: File): Seq[Histogram] = {
    val image = JAI.create("fileload", file.getPath)
    //    val channels = Seq(
    //      ("red", Array(0xFF, 0x00, 0x00)),
    //      ("green", Array(0x00, 0xFF, 0x00)),
    //      ("blue", Array(0x00, 0x00, 0xFF))
    //    )
    //    val allChannels: Seq[RenderedOp] = channels.map(ch => getChanel(ch._2)(image))
    //    allChannels.zip(channels.map(_._1)).foreach(x => storeImage(x._1, x._2))
    val tileImages = getTile(image)
    //    tileImages.zipWithIndex.foreach({
    //      case (image, tileIndex) => storeImage(image, tileIndex.toString)
    //    })
    logger.debug("# of tiled images = %d".format(tileImages.size))
    tileImages.zipWithIndex.foreach({
      case (tile, i) => storeImage(tile, i.toString)
    })
    tileImages.map(image => getHistogram(image))
  }

  /**
   *
   * @param image
   * @return
   */
  def getTile(image: PlanarImage): Seq[TiledImage] = {
    val tilesDimension = 7
    val tiledImage = new TiledImage(image, image.getWidth / tilesDimension, image.getHeight / tilesDimension)
    val tiles = Seq((tilesDimension / 2, tilesDimension / 2, tilesDimension) //center
      , (0, 0, tilesDimension) // top left corner
      , (0, tilesDimension - 1, tilesDimension) // top right corner
      , (tilesDimension - 1, 0, tilesDimension) // bottom left corner
      , (tilesDimension - 1, tilesDimension - 1, tilesDimension) // bottom right corner
    )

    logger.debug("image=%d*%d".format(image.getWidth, image.getHeight))

    tiles.map({
      case (i, j, n) =>
        val x = image.getWidth * i / n
        val y = image.getHeight * j / n
        val w = image.getWidth / n
        val h = image.getHeight / n
        val thisTile = tiledImage.getSubImage(x, y, w, h)
        logger.debug(("tiled image=%d*%d,i=%d, j=%d n=%d, x=%d, y=%d, w=%d, h=%d").format(
          thisTile.getWidth, thisTile.getHeight, i, j, n, x, y, w, h)
        )
        thisTile
    })
  }

  //  private[HistogramUtilities] def storeImage(image: PlanarImage, name: String): RenderedOp = {
  //    val pb = new ParameterBlock()
  //    pb.addSource(image)
  //    // im as the source image
  //    pb.add("%s.tiff".format(name))
  //    pb.add(null)
  //    JAI.create("filestore", pb)
  //  }
  //
  def main(args: Array[String]) = {
  }

}
