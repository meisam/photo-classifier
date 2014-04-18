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
