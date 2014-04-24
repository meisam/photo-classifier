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
    val iso = {
      val rawValue = extractByKey("ISO", lines)
      logger.debug("ISO WAS DETERMINED TO BE %s".format(rawValue))
      if (rawValue.getOrElse("A").forall(Character.isDigit(_))) {
        logger.debug("ISO WAS DETERMINED TO BE all digits %s".format(rawValue))
        if (rawValue.getOrElse("0") != "0") {
          logger.debug("ISO WAS DETERMINED TO BE non zero %s".format(rawValue))
          rawValue
        } else {
          Option.empty[String]
        }
      } else {
        Option.empty[String]
      }
    }

    val focalLength = {
      val rawValue = extractByKey("FocalLength", lines)
      if (rawValue.getOrElse("0.0 mm").startsWith("0.0 mm")) Option.empty[String] else rawValue
    }
    val exposureTime = extractByKey("ExposureTime", lines)
    val aperture = {
      val rawValue = extractByKey("MaxApertureValue", lines)
      if (rawValue.getOrElse("inf") == "inf") Option.empty[String] else rawValue
    }
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

  override def toString: String = {
    "{photoId=%s, groupId=%s, url=%s, iso=%s, focalLength=%s, exposureTime=%s, aperture=%s, flash=%s, dateTimeOriginal=%s}".format(
      photoId, groupId, url, iso, focalLength, exposureTime, aperture, flash, dateTimeOriginal
    )

  }
}

class PhotoMetadata(val photoId: String
                    , val groupId: String
                    , val url: String
                    , val iso: Double
                    , val focalLength: Double
                    , val exposureTime: Double
                    , val aperture: Double
                    , val flash: Double
                    , val dateTimeOriginal: Double) {

  override def toString: String = {
    "{photoId=%s, groupId=%s, url=%s, iso=%s, focalLength=%s, exposureTime=%s, aperture=%s, flash=%s, dateTimeOriginal=%s}".format(
      photoId, groupId, url, iso, focalLength, exposureTime, aperture, flash, dateTimeOriginal
    )
  }
}

object PhotoMetadata extends Logging {
  override def logger: Logger = Logger.getLogger(PhotoMetadata.getClass)

  def formRawMetadata(rawMetadata: PhotoRawMetadata): PhotoMetadata = {
    logger.debug("Trying to find values for %s".format(rawMetadata))
    val photoId = rawMetadata.photoId
    val groupId = rawMetadata.groupId
    val url = rawMetadata.url
    val iso: Int = rawMetadata.iso.toInt
    val focalLength: Double = rawMetadata.focalLength.split(" ")(0).toDouble
    val exposureTime: Double = if (rawMetadata.exposureTime.contains("/")) {
      1.0 / rawMetadata.exposureTime.split("/")(1).toDouble
    } else {
      rawMetadata.exposureTime.toDouble
    }
    val aperture = rawMetadata.aperture.toDouble
    val flash = if (rawMetadata.flash.toLowerCase.contains("off")
      || rawMetadata.flash.toLowerCase.contains("no")) {
      0.0
    }
    else {
      if (rawMetadata.flash.toLowerCase.contains("fired") ||
        rawMetadata.flash.toLowerCase.contains("on")) {
        1.0
      }
      else {
        throw new IllegalArgumentException("Value %s is not for flash settings".format(rawMetadata.flash))
      }
    }
    val dateTimeOriginal = rawMetadata.dateTimeOriginal.split("[\\s:]")(3).toDouble

    new PhotoMetadata(photoId, groupId, url, iso, focalLength, exposureTime, aperture, flash, dateTimeOriginal)
  }
}