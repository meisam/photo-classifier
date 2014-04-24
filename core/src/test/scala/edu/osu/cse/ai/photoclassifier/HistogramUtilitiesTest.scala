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

import org.scalatest.{Suite, FunSuite, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileSystem
import javax.media.jai.Histogram
import java.util

/**
 * Created by fathi on 4/11/14.
 */
class HistogramUtilitiesTest extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  self: Suite =>

  val BASE_PHOTO_DIR = "test-data"
  val DARK_PHOTO: String = "13673933685.jpg"
  val BRIGHT_PHOTO: String = "13470477704.jpg"

  var fileSystem: FileSystem = _

  override protected def beforeAll() = {
    fileSystem = FileSystems.getDefault()
  }

  def dumpHistograms(file: File) {
    println()
    val histograms: Seq[Histogram] = HistogramUtilities.processFile(file)
    println(file.getName)
    val features: Array[Double] = HistogramUtilities.histogramToFeaturesArray(histograms)
    println(util.Arrays.toString(features))
    histograms.zipWithIndex.foreach({
      case (h, i) => {
        h.getBins.foreach(bin => println(bin.mkString("barplot(c(", ",", """), main="%s %d")""".format(file.getName, i))))
      }
    })
  }

  test("histogram of dark photo") {
    val file = fileSystem.getPath(BASE_PHOTO_DIR, DARK_PHOTO).toFile
    dumpHistograms(file)
  }

  test("histogram of bright photo") {
    val file = fileSystem.getPath(BASE_PHOTO_DIR, BRIGHT_PHOTO).toFile
    dumpHistograms(file)
  }

}
