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

  test("test getMetadata for valid metadata file") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, VALID_METADATA_FILE_NAME).toFile

    val metadata = PhotoMetadataExtractor.getMetadata(path)
    assert(metadata.isDefined)
  }

  test("test getMetadata for invalid metadata file") {
    val path = fileSystem.getPath(BASE_PHOTO_DIR, INVALID_METADATA_FILE_NAME).toFile
    val metadata = PhotoMetadataExtractor.getMetadata(path)

    assert(metadata.isEmpty)
  }

  test("test valid classes") {
    val photoDbDir = fileSystem.getPath(BASE_PHOTO_DIR).toFile
    val metadataFiles = PhotoMetadataExtractor.getAllMetadata(photoDbDir)
    val validMetadataFiles = metadataFiles.filter(md => md.isDefined).map(_.get)

    val validGroups = Seq("portraits", "flowers", "landscape")
    val suspensionFiles = validMetadataFiles.filter(x => !(validGroups.contains(x.groupId)))
    assert(suspensionFiles.length === 0)
  }
}
