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

import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.REST
import com.flickr4java.flickr.RequestContext
import com.flickr4java.flickr.auth.Auth
import com.flickr4java.flickr.auth.AuthInterface
import com.flickr4java.flickr.auth.Permission
import com.flickr4java.flickr.photos._
import com.flickr4java.flickr.util.FileAuthStore
import org.apache.commons.io.FileUtils
import org.scribe.model.Token
import org.scribe.model.Verifier
import java.io.File
import java.io.PrintWriter
import java.net.URL
import java.util.Scanner
import scala.collection.JavaConversions._

/**
 * A simple program to backup all of a users private and public photos in a photoset aware manner. If photos are
 * classified in multiple photosets, they will be copied. Its a sample, its not perfect :-)
 * <p/>
 * This sample also uses the AuthStore interface, so users will only be asked to authorize on the first run.
 *
 * @author Matthew MacKenzie
 * @version $Id: edu.osu.cse.ai.photoclassifier.FlickrPhotoFetcher.java,v 1.6 2009/01/01 16:44:57 x-mago Exp $
 */
object FlickrPhotoFetcher {
  def main(args: Array[String]) {
    val API_KEY = args(0)
    val SHARED_SECRET = args(1)
    val flickr: Flickr = new Flickr(API_KEY, SHARED_SECRET, new REST)
    val authenticationDir: File = new File(System.getProperty("user.home"), ".flickerAppAuthentication")
    var auth: Auth = null
    if (authenticationDir.exists) {
      val authStore: FileAuthStore = new FileAuthStore(authenticationDir)
      auth = authStore.retrieveAll()(0)
    }
    else {
      val authInterface: AuthInterface = flickr.getAuthInterface
      val requestToken: Token = authInterface.getRequestToken
      val authorizationUrl = authInterface.getAuthorizationUrl(requestToken, Permission.READ)
      System.out.println("Follow this URL to authorise yourself on Flickr")
      System.out.println(authorizationUrl)
      System.out.println("Paste in the token it gives you:")
      System.out.print(">>")
      val tokenKey = new Scanner(System.in).nextLine
      val accessToken: Token = authInterface.getAccessToken(requestToken, new Verifier(tokenKey))
      auth = authInterface.checkToken(accessToken)
      RequestContext.getRequestContext.setAuth(auth)
      val authStore: FileAuthStore = new FileAuthStore(authenticationDir)
      authStore.store(auth)
      System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.")
    }
    try {
      val photosInterface = flickr.getPhotosInterface
      val groupInfos: Array[FlickrPhotoFetcher.GroupInfo] = Array(
        new FlickrPhotoFetcher.GroupInfo("375216@N23", "landscape", "Landscapes!!!")
        , new FlickrPhotoFetcher.GroupInfo("929911@N23", "landscape", "Landscape & Wildlife Photos of the World")
        , new FlickrPhotoFetcher.GroupInfo("571054@N24", "landscape", "Landscapes in an Upright Format")
        , new FlickrPhotoFetcher.GroupInfo("1420712@N25", "landscape", "Landscape ART /// Inspiration landscapes in the world")
        , new FlickrPhotoFetcher.GroupInfo("1003995@N21", "landscape", "Landscape Beauty !")
        , new FlickrPhotoFetcher.GroupInfo("979936@N25", "landscape", "landscape around the world")
        , new FlickrPhotoFetcher.GroupInfo("1293043@N21", "landscape", "Landscape,Seascape,Skyscape QUALITY Photography! Post 1 Award 2")
        , new FlickrPhotoFetcher.GroupInfo("716873@N20", "landscape", "*Landscapes of the West*")
        , new FlickrPhotoFetcher.GroupInfo("738590@N25", "landscape", "\"Landscapes Shot In Portrait Format\"")
        , new FlickrPhotoFetcher.GroupInfo("1063369@N24", "landscape", "Landscape Photographs - P 1 A 1")
        , new FlickrPhotoFetcher.GroupInfo("1709532@N23", "flowers", "Flower Flower Flower")
        , new FlickrPhotoFetcher.GroupInfo("1147116@N22", "flowers", "Flowers of Oman")
        , new FlickrPhotoFetcher.GroupInfo("98634088@N00", "flowers", "Flowers Group")
        , new FlickrPhotoFetcher.GroupInfo("1221043@N25", "flowers", "flowers of the world (Post 1, Comment 1)")
        , new FlickrPhotoFetcher.GroupInfo("562492@N21", "flowers", "Flowers all kinds")
        , new FlickrPhotoFetcher.GroupInfo("735470@N20", "flowers", "Flowers Planet Photography")
        , new FlickrPhotoFetcher.GroupInfo("1646577@N20", "flowers", "flower power!")
        , new FlickrPhotoFetcher.GroupInfo("68767127@N00", "flowers", "Flower decorations")
        , new FlickrPhotoFetcher.GroupInfo("98213740@N00", "flowers", "Flowers Without Limits!")
        , new FlickrPhotoFetcher.GroupInfo("73053811@N00", "flowers", "Flowers in Vases")
        , new FlickrPhotoFetcher.GroupInfo("1528452@N23", "portraits", "Portraits (Individuality, Imagination, Originality)")
        , new FlickrPhotoFetcher.GroupInfo("1430965@N20", "portraits", "portraits and faces.")
        , new FlickrPhotoFetcher.GroupInfo("1311364@N25", "portraits", "PORTRAITS with credits of 'GOOD PHOTO'")
        , new FlickrPhotoFetcher.GroupInfo("1994388@N22", "portraits", "PORTRAITS POUR FEMMES DU MONDE")
        , new FlickrPhotoFetcher.GroupInfo("1693861@N23", "portraits", "Portraits love")
        , new FlickrPhotoFetcher.GroupInfo("1267409@N20", "portraits", "Portrait Beauty !")
        , new FlickrPhotoFetcher.GroupInfo("1684858@N22", "portraits", "Portraits fantastique - MUST AWARD 1")
        , new FlickrPhotoFetcher.GroupInfo("2172681@N22", "portraits", "PORTRAITS ***")
        , new FlickrPhotoFetcher.GroupInfo("1597446@N21", "portraits", "Portraits Only")
        , new FlickrPhotoFetcher.GroupInfo("998710@N21", "portraits", "Portraits B&W/ Ritratti in B&N"))
      for (group <- groupInfos) {
        val searchParams = new SearchParameters
        searchParams.setGroupId(group.id)
        val photoList = photosInterface.search(searchParams, 1000, 10)
        var count: Int = 0
        val iterator = photoList.iterator
        while (iterator.hasNext && count <= MAX_PICTURES_FETCH) {
          val photo = iterator.next
          System.out.println("------------------------------")
          System.out.println(photo.getTitle)
          System.out.println(photo.getId)
          val url: URL = new URL(photo.getMedium800Url)
          val photoFile: File = new File(BASE_DIR, String.format("%s.jpg", photo.getId))
          try {
            val exifData = photosInterface.getExif(photo.getId, SHARED_SECRET)
            val metadataFile: File = new File(BASE_DIR, String.format("%s.txt", photo.getId))
            val printWriter: PrintWriter = new PrintWriter(metadataFile)
            printWriter.printf("GroupID=%s", group.id)
            printWriter.println
            printWriter.printf("GroupName=%s", group.name)
            printWriter.println
            printWriter.printf("GroupKeyword=%s", group.keyword)
            printWriter.println
            printWriter.printf("URL=%s", url)
            printWriter.println
            printWriter.printf("Title=%s", photo.getTitle)
            printWriter.println
            printWriter.printf("Author=%s", photo.getOwner.getId)
            printWriter.println
            for (exif <- exifData) {
              printWriter.printf("%s=%s", exif.getTag, exif.getRaw)
              printWriter.println
            }
            printWriter.close
            if (!photoFile.exists) {
              FileUtils.copyURLToFile(url, photoFile)
            }
            count += 1;
          }
          catch {
            case ex: Exception => {
              ex.printStackTrace
            }
          }
        }
      }
    }
    catch {
      case ex: Exception => {
        ex.printStackTrace
      }
    }
  }

  final val MAX_PICTURES_FETCH: Int = 100000
  private var BASE_DIR = "scratch"

  private[photoclassifier] class GroupInfo(val id: String, val keyword: String, val name: String) {

  }

}
