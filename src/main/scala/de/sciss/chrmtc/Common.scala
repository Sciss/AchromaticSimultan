/*
 *  Common.scala
 *  (_chr_m_t_c)
 *
 *  Copyright (c) 2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.chrmtc

import de.sciss.chrmtc.Geom.LatLon
import de.sciss.file._
import de.sciss.synth.io.AudioFile

object Common {
  // http://audiogroup.web.th-koeln.de/ku100hrir.html
  // http://sofacoustics.org/data/database/thk/
  val baseDir  : File = file("/data/projects/_chr_m_t_c")
  val fSOFA    : File = baseDir / "HRIR_L2702.sofa"
  val fAudioIR : File = fSOFA.replaceExt("aif")
  val fAudioPos: File = fAudioIR.replaceName(s"${fAudioIR.base}-pos.aif")

  def readPos(): Array[LatLon] = {
    val afIn = AudioFile.openRead(fAudioPos)
    try {
      val buf = afIn.buffer(afIn.numFrames.toInt)
      afIn.read(buf)
      buf.transpose.map { arr =>
        val lon = arr(0).toDouble
        val lat = arr(1).toDouble
//        Polar(theta = arr(0), phi = arr(1))
        LatLon(lat = lat, lon = lon)
      }

    } finally {
      afIn.close()
    }
  }

  final val TinkerHost            = "localhost"
  final val TinkerPort            = 4223
  final val TinkerDefaultIMU_UID  = "6jDAtS"   // the one I've got...
}
