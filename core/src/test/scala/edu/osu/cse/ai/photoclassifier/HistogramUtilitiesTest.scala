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

  test("load and run model for portrait prediction") {
    val dataPoints: RDD[LabeledPoint] = MLUtils.loadLabeledData(sc, "labeledPoints")
    val fileNames = sc.textFile("fileNames")

    val allPoints = fileNames.zip(dataPoints).filter(_._2.features.length == 75)

    val trainPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11"))
    val testPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11") == false)
    val clusteringModel: LogisticRegressionModel = LogisticRegressionWithSGD.train(trainPoints.map(_._2), 2000)
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

  def fixLabels(points: RDD[(String, LabeledPoint)], desiredLabel: String): RDD[(String, LabeledPoint)] = {
    points.map({
      case (fileName, labeledPoint) => {
//        println(fileName)
        val md = PhotoMetadataExtractor.getMetadata(new File(fileName.replace("jpg", "txt")))
        val newLabel: Double = if (md.get.groupId == desiredLabel) 1.0 else 0.0
        (fileName, new LabeledPoint(newLabel, labeledPoint.features))
      }
    }

    )

  }

  test("load and run model for landscape prediction") {
    val dataPoints: RDD[LabeledPoint] = MLUtils.loadLabeledData(sc, "labeledPoints")
    val fileNames = sc.textFile("fileNames")

    val allPoints = fixLabels(fileNames.zip(dataPoints).filter(_._2.features.length == 75), "flowers")

    val trainPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11"))
    val testPoints: RDD[(String, LabeledPoint)] = allPoints.filter(_._1.contains("11") == false)
    val clusteringModel: LogisticRegressionModel = LogisticRegressionWithSGD.train(trainPoints.map(_._2), 500)
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

  test("predict portrait by weights and bias") {

    val bias = 0.8973637736114621
    val weights = Array(27.597362511333298, 4.754193151154503, -5.823551456961994, -14.627563271337408, -15.851642324946244, -11.788946936353167, -10.302539067857033, -17.991119596685, 17.684515597044047, 18.446888613095748, -29.321222535253526, 22.864380737176145, 13.32789197194986, 10.74956970808049, -21.571821272707854, -1.368798865968949, 1.923268761154226, 4.769061305658358, 17.672026192656116, -26.94675878425704, -14.704869058114104, -13.04730583088602, -10.149134945519242, -12.827786973034097, 46.77789541679279, 8.939388392268057, 10.09363474097748, -9.818523455124607, -10.663144962224912, -2.502556106657395, -10.864842133887247, -12.08795993075973, 8.430821453228152, -5.065881010320968, 15.636660230982285, 12.7418776012566, 12.981638206176545, -24.413294879878208, 3.6659931837191695, -8.927415502029184, -22.381661318601722, 0.37694065982460634, 4.435845246062957, 18.811316360985117, -5.193642339027374, -3.692530289289115, -17.406604746954663, 9.91634318602135, -12.112178307529947, 19.343768766994838, -5.342802583902086, 9.180000010464585, -5.445435733054637, 9.788960832737754, -12.131923917002347, -0.17997541063375966, -1.9787792818305965, 2.1557114107929074, -2.0882338683925936, -1.8599242406932213, -1.6989046331277615, 2.991453557490161, 5.564745539491543, -0.18295191370488206, -10.625543940907686, 10.359191575651732, 0.9368823237073474, -3.131009889610704, -20.4871567084037, 8.37089130789958, -28.219814805021418, 2.8861838842025764, -0.5511169660275925, 25.310121288885817, -3.3765747928002905)
    val model = new LogisticRegressionModel(weights, bias)

    val file = fileSystem.getPath(BASE_PHOTO_DIR, BRIGHT_PHOTO).toFile


    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile

    val photoFiles: Array[File] = photoDbDir.listFiles.filter(_.getName.endsWith("jpg"))

    val histograms = photoFiles.map(HistogramUtilities.roiHistograms(_))
    val features = histograms.map(HistogramUtilities.histogramToFeaturesArray(_))
    val photoFeaturePairs = photoFiles.zip(features).filter(_._2.length == 75)
    val filePredictionPairs = photoFeaturePairs.map({
      case (file, features) => (file, model.predict(features).toInt)
    })
    val portraits = filePredictionPairs.filter(_._2 == 1).map(_._1)
    val nonPortraits = filePredictionPairs.filter(_._2 == 0).map(_._1)

    val portraitStr = portraits.map(x => """<img src="%s"/>""".format(x.getPath)).mkString(
      """
  <html>
  <body>
      """.stripMargin,
      "\n",
      """</body>
  </html>""".stripMargin
    )
    println(portraitStr)
    println("------------------------")
    val nonPortraitStr = nonPortraits.map(x => """<img src="%s"/>""".format(x.getPath)).mkString(
      """
  <html>
  <body>
      """.stripMargin,
      "\n",
      """</body>
  </html>""".stripMargin
    )
    println(nonPortraitStr)
  }

  test("extract histogram data into file") {
    val photoDbDir = fileSystem.getPath("scratch").toFile
    assert(photoDbDir.exists())

    val allPhotoMdFiles: Array[File] = photoDbDir.listFiles.filter(_.getName.endsWith("txt"))
    val validPhotoMdOptFiles: Array[(File, Option[PhotoRawMetadata])] = allPhotoMdFiles.map(f => (f, PhotoMetadataExtractor.getMetadata(f)))

    val validPhotoMdFiles: Array[(File, PhotoRawMetadata)] = validPhotoMdOptFiles.filter(_._2.isDefined).map({
      case (f, md) => (f, md.get)
    })

    val validPhotoFiles = validPhotoMdFiles.map({
      case (file, md) => (new File(file.getParentFile, file.getName.replace("txt", "jpg")), md)
    })

    println("# valid photos=%d".format(validPhotoFiles.length))
    val histograms = validPhotoFiles.map({
      case (file, md) => {
        try {
          val hists = HistogramUtilities.roiHistograms(file)
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
