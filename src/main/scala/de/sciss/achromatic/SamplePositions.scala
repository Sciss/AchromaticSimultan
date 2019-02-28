/*
 *  SamplePositions.scala
 *  (Achromatic simultan)
 *
 *  Copyright (c) 2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.achromatic

import de.sciss.achromatic.Geom.{LatLon, Pt3}
import Common.fAudioIdxG
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

/** Creates a file mapping a grid of 1/S degrees sampling of azimuth and elevation to
  * IR indices.
  *
  * We write a 1D file of IR indices, with frame number given by
  * ((azimuth-in-degrees + 360) % 360) * S + (elevation-in-degrees + 90) * S * (360 * S)
  */
object SamplePositions {
  final val S = 2

  def main(args: Array[String]): Unit = {
    if (fAudioIdxG.exists()) {
      println(s"File '$fAudioIdxG' already exists.")
    } else {
      run()
    }
  }

  def run(): Unit = {
    val IrPt: Array[Pt3] = Common.readPosC()
//    val azi = 0.0 until 360.0 by (1.0/S)
//    assert (azi.size == 360 * S)
//    val ele = 0.0 until 180.0 by (1.0/S)
//    assert (ele.size == 180 * S)

    var taken = Set.empty[Int]

    // var foo = Vector.empty[Pt3]

    val coord = Array.tabulate(360 * S * 180 * S) { i =>
      val azi     = (i % (360 * S)).toDouble / S
      // assert(azi >= 0 && azi < 360)
      val ele     = (i / (360 * S)).toDouble / S - 90.0
      // assert(ele >= -90 && ele < 90)
      val a       = -azi.toRadians
      val e       =  ele.toRadians
      val ll      = LatLon(lon = a, lat = e)
      val pt      = ll.toCartesian

      // foo :+= pt

      val bestPt  = IrPt.minBy(_.distanceSq(pt))
      val index   = IrPt.indexOf(bestPt)
      taken += index
      index.toFloat
    }

    println(s"Of ${IrPt.length} indices, ${taken.size} were taken.")

    // PlotHRIRPos.run(foo.toArray)

    val af = AudioFile.openWrite(fAudioIdxG, AudioFileSpec(numChannels = 1, sampleRate = 48000.0))
    try {
      af.write(Array(coord))
    } finally {
      af.close()
    }
  }
}
