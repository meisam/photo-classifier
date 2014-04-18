package edu.osu.cse.ai.photoclassifier

import org.apache.log4j.Logger
import java.nio.file._
import java.nio.charset.StandardCharsets
import scala.collection.convert.Wrappers.JListWrapper
import java.io.File

/**
 * Created by fathi on 4/17/14.
 */
object PhotoMetadataExtractor extends Logging {
  override def logger: Logger = Logger.getLogger(PhotoMetadataExtractor.getClass)


  def traverseAllFiles(parentDir: File): Array[File] = {
    parentDir.listFiles.filter(_.getName.endsWith("txt"))
  }

  def getAllMetadata(parentDir: File): Array[Option[PhotoRawMetadata]] = {
    traverseAllFiles(parentDir).map(x => getMetadata(x))
  }

  def getMetadata(file: File): Option[PhotoRawMetadata] = {

    logger.debug("Reading file" + file)
    logger.debug("Type of this scala object is %s".format(PhotoMetadataExtractor.getClass))
    val photoId = file.getName
    logger.debug(" Match: photoID is %s".format(photoId))
    val path = FileSystems.getDefault.provider().getPath(file.toURI)
    val lines: JListWrapper[String] = JListWrapper(Files.readAllLines(path, StandardCharsets.UTF_8))
    val groupId = extractByKey("GroupKeyword", lines)
    val url = extractByKey("URL", lines)
    val iso = extractByKey("ISO", lines)
    val focalLength = extractByKey("FocalLength", lines)
    val exposureTime = extractByKey("ExposureTime", lines)
    val aperture = extractByKey("MaxApertureValue", lines)
    val flash = extractByKey("Flash", lines)
    val dateTimeOriginal = extractByKey("DateTimeOriginal", lines)

    if (groupId.isDefined && url.isDefined && iso.isDefined && focalLength.isDefined && exposureTime.isDefined && aperture.isDefined && flash.isDefined && dateTimeOriginal.isDefined)
      Option(
        new PhotoRawMetadata(photoId, groupId.get, url.get, iso.get, focalLength.get, exposureTime.get, aperture.get, flash.get, dateTimeOriginal.get))
    else Option.empty[PhotoRawMetadata]
  }

  def extractByKey(key: String, lines: JListWrapper[String], separator: String = "="): Option[String] = {
    val extendedKey = key + separator
    val targetLines = lines.filter(_.startsWith(extendedKey))
    if (targetLines.isEmpty || targetLines.length > 1) {
      logger.debug(" NO NO NO MATCH: %s is %s".format(key, ""))
      Option.empty[String]
    }
    else {
      val value = targetLines(0).substring(extendedKey.length)
      logger.debug(" MATCH: %s is %s".format(key, value))
      Option(value)
    }
  }

  def getMetadataStats(photoDbDir: File, validGroups: Seq[String]): Seq[(String, Int)] = {
    val metadataFiles = PhotoMetadataExtractor.getAllMetadata(photoDbDir)
    val validMetadataFiles = metadataFiles.filter(md => md.isDefined).map(_.get)
    validGroups.map(group => (group, validMetadataFiles.filter(_.groupId == group).length))
  }
}

class PhotoRawMetadata(val photoId: String
                       , val groupId: String
                       , val url: String
                       , val iso: String
                       , val focalLength: String
                       , val exposureTime: String
                       , val aperture: String
                       , val flash: String
                       , val dateTimeOriginal: String) {

}
