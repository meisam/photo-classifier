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
import java.nio.file.{FileSystems, FileSystem}
import org.apache.spark.SparkContext

/**
 * Created by fathi on 4/11/14.
 */
class TestPhotoClmeusterer extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  self: Suite =>
  @transient var sc: SparkContext = _
  val BASE_PHOTO_DIR = "test-data"
  val VALID_METADATA_FILE_NAME: String = "13673933685.txt"
  val INVALID_METADATA_FILE_NAME: String = "13084299633.txt"

  var fileSystem: FileSystem = FileSystems.getDefault()

  test("cluster all pictures files") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    assert(photoDbDir.exists())
    val photos: Array[PhotoMetadata] = PhotoMetadataExtractor.getAllValidMetadata(photoDbDir)
    assert(photos.length === 17)

    val model = PhotoClusterer.kMeansCluster(photos)
    assert(model.predict(PhotoClusterer.toFeatureArray(photos(0))) === 2)
  }

}
