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
import java.nio.file.FileSystems
import java.nio.file.FileSystem

/**
 * Created by fathi on 4/11/14.
 */
class TestPhotoMetadataExtractor extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  self: Suite =>

  val BASE_PHOTO_DIR = "scratch"
  val VALID_METADATA_FILE_NAME: String = "13673933685.txt"
  val INVALID_METADATA_FILE_NAME: String = "13084299633.txt"

  var fileSystem: FileSystem = _

  override protected def beforeAll() = {
    fileSystem = FileSystems.getDefault()
  }

  test("traverseAllFiles") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    val files = PhotoMetadataExtractor.traverseAllFiles(photoDbDir)
    val fileNames = files.map(_.getName)
    assert(fileNames.contains(VALID_METADATA_FILE_NAME))
    assert(fileNames.contains(INVALID_METADATA_FILE_NAME))
    assert(fileNames.length === 8713)
  }

  test("get all metadata") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    val metadataFiles = PhotoMetadataExtractor.getAllMetadata(photoDbDir)
    val validMetadataFiles = metadataFiles.filter(md => md.isDefined).map(_.get)
    assert(validMetadataFiles.filter(_.photoId.equals(VALID_METADATA_FILE_NAME)).length === 1)
    assert(validMetadataFiles.filter(_.photoId.equals(INVALID_METADATA_FILE_NAME)).length === 0)
  }

  test("getMetadata for valid raw metadata file") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, VALID_METADATA_FILE_NAME).toFile

    val metadata = PhotoMetadataExtractor.getMetadata(path)
    assert(metadata.isDefined)
  }

  test("getMetadata for invalid raw metadata file") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, INVALID_METADATA_FILE_NAME).toFile
    val metadata = PhotoMetadataExtractor.getMetadata(path)

    assert(metadata.isEmpty)
  }

  test("valid clusters") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    val metadataFiles = PhotoMetadataExtractor.getAllMetadata(photoDbDir)
    val validMetadataFiles = metadataFiles.filter(md => md.isDefined).map(_.get)

    val validGroups = Seq("portraits", "flowers", "landscape")
    val suspensionFiles = validMetadataFiles.filter(x => !(validGroups.contains(x.groupId)))
    assert(suspensionFiles.length === 0)
  }

  test("for valid metadata file") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, VALID_METADATA_FILE_NAME).toFile

    val rawMetadata = PhotoMetadataExtractor.getMetadata(path)
    val metadata: PhotoMetadata = PhotoMetadata.formRawMetadata(rawMetadata.get)
    assert(metadata.iso === 1000)
    assert(metadata.focalLength === 35.00)
    assert(metadata.exposureTime === (1.0 / 80))
    assert(metadata.aperture === 1.7)
    assert(metadata.dateTimeOriginal === 15.0)
  }

  test("file with bad iso value") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, "13294031173.txt").toFile

    val rawMetadata = PhotoMetadataExtractor.getMetadata(path)
    val metadata: PhotoMetadata = PhotoMetadata.formRawMetadata(rawMetadata.get)
    assert(metadata.iso === 16000)
    assert(metadata.focalLength === 35.00)
    assert(metadata.exposureTime === (1.0 / 125))
    assert(metadata.aperture === 2.0)
    assert(metadata.dateTimeOriginal === 0.0)
  }
  test("read all valid metadata files") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    val files = PhotoMetadataExtractor.getAllMetadata(photoDbDir).filter(_.isDefined).map(_.get)
    val metadataObjects: Array[PhotoMetadata] = files.map(md => PhotoMetadata.formRawMetadata(md))
    metadataObjects.foreach(metadata => {
      println(metadata.url)
      assert(metadata.iso <= 64000)
      println(metadata.iso)
      assert(metadata.iso >= 10)
      assert(metadata.focalLength >= 2.00)
      assert(metadata.focalLength <= 800.00)
      assert(metadata.exposureTime >= (1.0 / 10000))
      assert(metadata.exposureTime <= (2 * 60 * 60)) // two hours
      assert(metadata.aperture >= 0.4)
      assert(metadata.aperture <= 30.)
      assert(Seq(0.0, 1.0).contains(metadata.flash))
      assert(metadata.aperture <= 30.)
      assert(metadata.dateTimeOriginal >= 0.0)
      assert(metadata.dateTimeOriginal <= 24.0)
    })
  }

}
