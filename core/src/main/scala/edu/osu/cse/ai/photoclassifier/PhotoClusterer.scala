
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

import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.{KMeansModel, KMeans}
import java.nio.file.{FileSystems, FileSystem}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import java.io.File
import java.util


object PhotoClusterer extends Logging {
  override def logger: Logger = Logger.getLogger(this.getClass)

  var fileSystem: FileSystem = _

  val sc = new SparkContext("local", "test")

  def kMeansCluster(photoMetadata: Seq[PhotoMetadata]) = {
    val k = 3
    val iters = 100
    val data = sc.parallelize(photoMetadata).map(toFeatureArray(_))
    val mins = data.reduce(reduceElements(_, _, math.min))
    val maxs = data.reduce(reduceElements(_, _, math.max))
    val normalizedData = data.map(d => normalize(d, mins, maxs)).cache()
    //    println(normalizedData.collect().map(java.util.Arrays.toString(_)).mkString("\n"))
    val model = KMeans.train(normalizedData, k, iters)
    model
  }

  def supportVectorMachineMClassifier(photoMetadata: Seq[PhotoMetadata], numIterations: Int
                                      //                                      ,stepSize: Double
                                      //                                      ,regParam: Double
                                       ): SVMModel = {
    val classes = Seq("flowers", "landscape", "portraits")
    val data: RDD[LabeledPoint] = sc.parallelize(photoMetadata).map(p => {
      val featuresArray: Array[Double] = toFeatureArray(p)
      val label: Double = if (classes.indexOf(p.groupId) == 0) 1 else 0
      logger.info("Label=%3.2f, groupID=%s".format(label, p.groupId))
      new LabeledPoint(label, featuresArray)
    })

    val mins = data.reduce({
      case (x: LabeledPoint, y: LabeledPoint) => new LabeledPoint(-1, reduceElements(x.features, y.features, math.min))
    })
    val maxs = data.reduce({
      case (x: LabeledPoint, y: LabeledPoint) => new LabeledPoint(-1, reduceElements(x.features, y.features, math.max))
    })
    val normalizedData = data.map(d => new LabeledPoint(d.label, normalize(d.features, mins.features, maxs.features))).cache()
    //    println(normalizedData.collect().map(java.util.Arrays.toString(_)).mkString("\n"))

    val model = SVMWithSGD.train(normalizedData, numIterations)
    model
  }

  def normalize(d: Array[Double], mins: Array[Double], maxs: Array[Double]): Array[Double] = {
    val t: Array[((Double, Double), Double)] = mins.zip(maxs).zip(d)
    t.map({
      case ((min, max), x) => (x - min) / (max - min)
    })
  }

  def reduceElements(x: Array[Double], y: Array[Double], f: (Double, Double) => Double): Array[Double] = {
    x.zip(y).map(pair => f(pair._1, pair._2))
  }

  def toFeatureArray(md: PhotoMetadata): Array[Double] = {
    val y = (md.iso, md.aperture).productIterator.zipWithIndex;
    val x: Array[Double] = new Array[Double](6)
    x(0) = md.iso
    x(1) = md.focalLength
    x(2) = md.exposureTime
    x(3) = md.aperture
    x(4) = md.flash
    x(5) = md.dateTimeOriginal
    x
  }

  def printClusters(photos: Array[PhotoMetadata], model: KMeansModel) = {
    println("Cluster centers:")
    for (c <- model.clusterCenters) {
      println("  " + c.map("%6.3f".format(_)).mkString(" "))
    }
    val classes = 0 until (model.k)
    classes.foreach(prettyPrintCluster(_, photos, model))
  }

  def prettyPrintCluster(i: Int, photos: Array[PhotoMetadata], model: KMeansModel) = {
    val thisCluster = photos.filter(x => model.predict(PhotoClusterer.toFeatureArray(x)) == i)
    println("Cluster %d with %d elements".format(i, thisCluster.length))
    thisCluster.foreach(x => println("Group: %10s, photo=%s".format(x.groupId, x.url)))
    println("------------------------------")
  }

  def printClasses(photos: Array[PhotoMetadata], model: SVMModel) = {
    val predictedPhotos: Array[(Double, PhotoMetadata)] = photos.map(x => (model.predict(PhotoClusterer.toFeatureArray(x)), x)).sortBy(_._1)
    predictedPhotos.foreach(x => println("Prediction:%3.2f, Group: %10s, photo=%s".format(x._1, x._2.groupId, x._2.url)))
  }

  def toCsvFile(photos: Array[PhotoMetadata], file: File) = {
    val data = photos.map(toFeatureArray(_))
//    val mins = data.reduce(reduceElements(_, _, math.min))
//    val maxs = data.reduce(reduceElements(_, _, math.max))
//    val normalizedData: Array[Array[Double]] = data.map(d => normalize(d, mins, maxs))

    val str = data.map(util.Arrays.toString(_)).mkString("\n")
    println(str)

  }

  def main(args: Array[String]) {
    val photoDbDir = FileSystems.getDefault().getPath("scratch").toFile

    val photos: Array[PhotoMetadata] = PhotoMetadataExtractor.getAllValidMetadata(photoDbDir)
    val csvFile = new File(photoDbDir, "all_valid_photos.csv")
    toCsvFile(photos, csvFile)

    //
    //    val model: SVMModel = PhotoClusterer.supportVectorMachineMClassifier(photos, 1000)
    //    printClasses(photos, model)


  }
}
