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
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.SparkContext
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.classification.{LogisticRegressionModel, LogisticRegressionWithSGD}
import org.apache.spark.rdd.RDD

/**
 * Created by fathi on 4/11/14.
 */
class HistogramUtilitiesTest extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  self: Suite =>

  val BASE_PHOTO_DIR = "test-data"
  val DARK_PHOTO: String = "13673933685.jpg"
  val BRIGHT_PHOTO: String = "13470477704.jpg"

  var fileSystem: FileSystem = _
  var sc: SparkContext = _

  override protected def beforeAll() = {
    fileSystem = FileSystems.getDefault()
    sc = new SparkContext("local", "test")
  }

  override protected def afterAll() {
    sc.stop()
  }

  def dumpHistograms(file: File) {
    println()
    val histograms: Seq[Histogram] = HistogramUtilities.roiHistograms(file)
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

  //  test("classify based on histograms") {
  //    val photoDbDir = fileSystem.getPath("scratch").toFile
  //    assert(photoDbDir.exists())
  //
  //    val allPhotoMdFiles: Array[File] = photoDbDir.listFiles.filter(_.getName.endsWith("txt"))
  //    val validPhotoMdOptFiles: Array[(File, Option[PhotoRawMetadata])] = allPhotoMdFiles.map(f => (f, PhotoMetadataExtractor.getMetadata(f)))
  //
  //    val validPhotoMdFiles: Array[(File, PhotoRawMetadata)] = validPhotoMdOptFiles.filter(_._2.isDefined).map({
  //      case (f, md) => (f, md.get)
  //    })
  //    //        .map({
  //    //      case (f, md) => (f, md)
  //    //    })
  //
  //    val validPhotoFiles = validPhotoMdFiles.map({
  //      case (file, md) => (new File(file.getParentFile, file.getName.replace("txt", "jpg")), md)
  //    })
  //
  //    println("# valid photos=%d".format(validPhotoFiles.length))
  //    val histograms = validPhotoFiles.map({
  //      case (file, md) => {
  //        try {
  //          val hists = HistogramUtilities.roiHistograms(file)
  //          //        println("NOT mv %s ../garbage".format(file.getName.replace("jpg", "*")))
  //          (file, md, hists)
  //        }
  //        catch {
  //          case ex: RuntimeException => {
  //            println("mv %s ../garbage".format(file.getAbsolutePath.replace("jpg", "*")))
  //            (null, null, null)
  //          }
  //        }
  //      }
  //    }).filter(_._1 != null)
  //
  //    val photosDataPoints: Array[(File, LabeledPoint)] = histograms.map({
  //      case (file, md, hist) => {
  //        val features = HistogramUtilities.histogramToFeaturesArray(hist)
  //        val label: Double = md.groupId match {
  //          case "portraits" => 1.0
  //          case "flowers" => 0.0
  //          case "landscape" => 0.0
  //          case _ => throw new IllegalArgumentException("Unknown group ID %s".format(md.groupId))
  //        }
  //        (file, new LabeledPoint(label, features))
  //      }
  //    })
  //
  //
  //
  //    val trainDataPoints: Array[(File, LabeledPoint)] = photosDataPoints.filter(_._1.getName.contains("11"))
  //    val testDataPoints: Array[(File, LabeledPoint)] = photosDataPoints.filter(x => x._1.getName.contains("11") == false)
  //
  //    val trainRdd: RDD[(File, LabeledPoint)] = sc.parallelize(trainDataPoints)
  //
  //    val dataPoints: RDD[LabeledPoint] = trainRdd.map(_._2)
  //    dataPoints.cache()
  //
  //    MLUtils.saveLabeledData(dataPoints, "photos-histogram-features.data")
  //    val clusteringModel: LogisticRegressionModel = LogisticRegressionWithSGD.train(dataPoints, 2)
  //
  //    println(clusteringModel.intercept)
  //    println(clusteringModel.weights.mkString(","))
  //
  //    val predictions: Array[Double] = testDataPoints.collect.map(clusteringModel.predict(_))
  //
  //    //    val predictions: Array[Double] = clusteringModel.predict(testDataPoints).collect()
  //    val str = predictions.zip(testRdd.collect().map(_._2.label)).mkString("\n")
  //    println(str)
  //  }

  test("load and run model") {
    val dataPoints: RDD[LabeledPoint] = MLUtils.loadLabeledData(sc, "labeledPoints")
    val fileNames = sc.textFile("fileNames")

    val allPoints = fileNames.zip(dataPoints).filter(_._2.features.length == 75)

    val trainPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11"))
    val testPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11") == false)
    val clusteringModel: LogisticRegressionModel = LogisticRegressionWithSGD.train(trainPoints.map(_._2), 10000)
    println(clusteringModel.intercept)
    println(clusteringModel.weights.mkString(","))

    val predictions: RDD[Double] = clusteringModel.predict(testPoints.map(_._2.features))
    val zipped = predictions.zip(testPoints).map({
      case (d, p) => {
        math.abs(p._2.label - d).toInt
      }
    })
    val results = zipped.collect
    val wrongCount = results.reduce(_ + _)

    println("#correct/all = %d/%d = %3f%%".format(results.size - wrongCount, results.size, 100.0 - 100.0 * wrongCount / results.size))
    val str = results.mkString("\n")
    println(str)
  }

  test("extract histogram data into file") {
    val photoDbDir = fileSystem.getPath("scratch").toFile
    assert(photoDbDir.exists())

    val allPhotoMdFiles: Array[File] = photoDbDir.listFiles.filter(_.getName.endsWith("txt"))
    val validPhotoMdOptFiles: Array[(File, Option[PhotoRawMetadata])] = allPhotoMdFiles.map(f => (f, PhotoMetadataExtractor.getMetadata(f)))

    val validPhotoMdFiles: Array[(File, PhotoRawMetadata)] = validPhotoMdOptFiles.filter(_._2.isDefined).map({
      case (f, md) => (f, md.get)
    })
    //        .map({
    //      case (f, md) => (f, md)
    //    })

    val validPhotoFiles = validPhotoMdFiles.map({
      case (file, md) => (new File(file.getParentFile, file.getName.replace("txt", "jpg")), md)
    })

    println("# valid photos=%d".format(validPhotoFiles.length))
    val histograms = validPhotoFiles.map({
      case (file, md) => {
        try {
          val hists = HistogramUtilities.roiHistograms(file)
          //        println("NOT mv %s ../garbage".format(file.getName.replace("jpg", "*")))
          (file, md, hists)
        }
        catch {
          case ex: RuntimeException => {
            println("mv %s ../garbage".format(file.getAbsolutePath.replace("jpg", "*")))
            (null, null, null)
          }
        }
      }
    }).filter(_._1 != null)

    val photosDataPoints: Array[(File, LabeledPoint)] = histograms.map({
      case (file, md, hist) => {
        val features = HistogramUtilities.histogramToFeaturesArray(hist)
        val label: Double = md.groupId match {
          case "portraits" => 1.0
          case "flowers" => 0.0
          case "landscape" => 0.0
          case _ => throw new IllegalArgumentException("Unknown group ID %s".format(md.groupId))
        }
        (file, new LabeledPoint(label, features))
      }
    })

    val dataPointsRdd: RDD[(File, LabeledPoint)] = sc.parallelize(photosDataPoints)
    dataPointsRdd.map(_._1).saveAsTextFile("fileNames")
    MLUtils.saveLabeledData(dataPointsRdd.map(_._2), "labeledPoints")
  }

}
