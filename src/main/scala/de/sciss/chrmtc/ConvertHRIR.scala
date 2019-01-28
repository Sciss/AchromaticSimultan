/*
 *  ConvertHRIR.scala
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

import de.sciss.chrmtc.Common._
import de.sciss.file._
import de.sciss.lucre.artifact.{Artifact, ArtifactLocation}
import de.sciss.lucre.matrix.DataSource
import de.sciss.lucre.stm.{InMemory, Workspace}
import de.sciss.synth.io.{AudioFile, AudioFileSpec}
import de.sciss.synth.proc.GenContext
import ucar.nc2.NetcdfFile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ConvertHRIR {
  def main(args: Array[String]): Unit = {
    run()
  }

  type S = InMemory

  /** Reads in a SOFA (HDF) file with head-related impulse responses,
    * assuming they are in the variable `Data.IR` with shape `[ir-index][channel][frame]`.
    * Then writes them into an AIFF file consecutively (preserving `channel`).
    *
    * Also creates a corresponding AIFF file with the positions of the responses,
    * using two channels for (azimuth/longitude, elevation/latitude), in radians.
    */
  def run(): Unit = {
    implicit val cursor: S = InMemory()

    if (fAudioIR.exists() && fAudioPos.exists()) {
      println(s"Files '$fAudioIR' and '$fAudioPos' already exist.")
      return
    }

    val net = NetcdfFile.open(fSOFA.path).setImmutable()
    implicit val res: DataSource.Resolver[S] = DataSource.Resolver.seq(net)

    import Workspace.Implicits.dummy

    try {
      val (futReadPos, futReadIR, futReadSR) = cursor.step { implicit tx =>
        implicit val gen: GenContext[S] = GenContext.apply()
        val loc = ArtifactLocation.newConst[S](fSOFA.parent)
        val art = Artifact.apply(loc, fSOFA)
        val src = DataSource(art)
        src.variables.foreach { vr =>
          val shapeS = vr.shape.mkString("[", "][", "]")
          println(s"$shapeS ${vr.name}")
        }

        val Some(vrPos) = src.variables.find(_.name == "SourcePosition")
        val Some(vrIR)  = src.variables.find(_.name == "Data.IR")
        val Some(vrSR)  = src.variables.find(_.name == "Data.SamplingRate")
        val shapePos    = vrPos.shape
        val shapeIR     = vrIR.shape
        val shapeSR     = vrSR.shape

        assert (shapePos.drop(1) == Seq(3), shapePos.toString)  // azimuth, elevation, distance
        assert (shapeIR .drop(1) == Seq(2, 128), shapeIR.toString)  // two channels, 128 samples IR length
        assert (shapePos.head == shapeIR.head)  // matching number of IRs
        assert (shapeSR == Seq(1), shapeSR.toString) // one value

        (vrPos.reader(0), vrIR.reader(0), vrSR.reader(0))
      }

      val futSeq = Future.sequence(Seq(futReadPos, futReadIR, futReadSR))
      val Seq(readPos, readIR, readSR) = Await.result(futSeq, Duration.Inf)
      val arrSR = new Array[Double](1)
      readSR.readDouble1D(arrSR, 0, arrSR.length)
      val sr = arrSR(0)

      val irLen = readIR.numChannels / 2
      println(s"input numChannels = ${readIR.numChannels} (2 x irLen of $irLen); numFrames = ${readIR.numFrames} (= number of IRs); sr = $sr")

      val arrIR = new Array[Double](readIR.size.toInt)
      readIR.readDouble1D(arrIR, 0, arrIR.length)

      val arrPos = new Array[Double](readPos.size.toInt)
      readPos.readDouble1D(arrPos, 0, arrPos.length)

      val afOutPos = AudioFile.openWrite(fAudioPos, AudioFileSpec(numChannels = 2, sampleRate = 1))
      try {
        val bufPos = arrPos.grouped(readPos.numChannels).map(_.take(2).map(_.toRadians.toFloat)).toArray.transpose
        afOutPos.write(bufPos)

      } finally {
        afOutPos.close()
      }

      val afOutIR = AudioFile.openWrite(fAudioIR, AudioFileSpec(numChannels = 2, sampleRate = sr))
      try {
        arrIR.grouped(readIR.numChannels).foreach { ir =>
          val (left, right) = ir.map(_.toFloat).splitAt(irLen)
          afOutIR.write(Array(left, right))
        }

      } finally {
        afOutIR.close()
      }

    } finally {
      net.close()
    }
  }
}
