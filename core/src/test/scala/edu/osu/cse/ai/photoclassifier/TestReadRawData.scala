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

import org.apache.spark.SparkContext
import org.scalatest.{Suite, FunSuite, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers
import org.apache.hadoop.mapred.TextInputFormat
import org.apache.hadoop.io.{Text, LongWritable}
import org.apache.spark.rdd.RDD

/**
 * Created by fathi on 4/11/14.
 */
class TestReadRawData extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  self: Suite =>
  @transient var sc: SparkContext = _

  override def beforeAll() {
    sc = new SparkContext("local", "test")
    super.beforeAll()
  }

  override def afterAll() {
    if (sc != null) {
      sc.stop()
    }
    System.clearProperty("spark.driver.port")

    super.afterAll()
  }

  test("Test local") {
    val x = sc.parallelize(1 to 100).reduce(_ + _)
    assert(x === 5050)
  }

  test("read image metadata") {
    val mdFiles: RDD[String] = sc.hadoopFile[LongWritable, Text, TextInputFormat]("/home/fathi/workspace/photo-classifier/scratch/*.txt").map(x => x._2.toString)
    val x = mdFiles.collect()
    println(x.mkString("\n=========================================\n"))

  }
}
