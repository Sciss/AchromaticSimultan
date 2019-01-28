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

import de.sciss.file._
import de.sciss.lucre.artifact.{Artifact, ArtifactLocation}
import de.sciss.lucre.matrix.DataSource
import de.sciss.lucre.stm.{InMemory, Workspace}
import de.sciss.synth.io.{AudioFile, AudioFileSpec}
import de.sciss.synth.proc.GenContext
import ucar.nc2.NetcdfFile

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ConvertHRIR {
  def main(args: Array[String]): Unit = {
    run()
  }

  type S = InMemory

  /** Reads in a SOFA (HDF) file with head-related impulse responses,
    * assuming they are in the variable `Data.IR` with shape `[ir-index][channel][frame]`.
    * Then writes them into an AIFF file consecutively (preserving `channel`).
    */
  def run(): Unit = {
    implicit val cursor: S = InMemory()

    // http://audiogroup.web.th-koeln.de/ku100hrir.html
    // http://sofacoustics.org/data/database/thk/
    val baseDir = file("/data/projects/_chr_m_t_c")
    val fIn     = baseDir / "HRIR_L2702.sofa"
    val fOut    = fIn.replaceExt("aif")
    if (fOut.exists()) {
      println(s"File '$fOut' already exists.")
      return
    }

    val net = NetcdfFile.open(fIn.path).setImmutable()
    implicit val res: DataSource.Resolver[S] = DataSource.Resolver.seq(net)

    import Workspace.Implicits.dummy

    try {
      val (futReadIR, futReadSR) = cursor.step { implicit tx =>
        implicit val gen: GenContext[S] = GenContext.apply()
        val loc = ArtifactLocation.newConst[S](fIn.parent)
        val art = Artifact.apply(loc, fIn)
        val src = DataSource(art)
        src.variables.foreach { vr =>
          val shapeS = vr.shape.mkString("[", "][", "]")
          println(s"$shapeS ${vr.name}")
        }

        // val Some(vrPos) = src.variables.find(_.name == "SourcePosition")
        val Some(vrIR)  = src.variables.find(_.name == "Data.IR")
        val Some(vrSR)  = src.variables.find(_.name == "Data.SamplingRate")
        val shapeIR     = vrIR.shape
        val shapeSR     = vrSR.shape

        assert (shapeIR.drop(1) == Seq(2, 128), shapeIR.toString)  // two channels, 128 samples IR length
        assert (shapeSR == Seq(1), shapeSR.toString) // one value

        (vrIR.reader(0), vrSR.reader(0))
      }

      val futSeq = Future.sequence(Seq(futReadIR, futReadSR))
      val Seq(readIR, readSR) = Await.result(futSeq, Duration.Inf)
      val arrSR = new Array[Double](1)
      readSR.readDouble1D(arrSR, 0, arrSR.length)
      val sr = arrSR(0)

      val irLen = readIR.numChannels / 2
      println(s"input numChannels = ${readIR.numChannels} (2 x irLen of $irLen); numFrames = ${readIR.numFrames} (= number of IRs); sr = $sr")

      val arrIR = new Array[Double](readIR.size.toInt)
      readIR.readDouble1D(arrIR, 0, arrIR.length)

      val afOut = AudioFile.openWrite(fOut, AudioFileSpec(numChannels = 2, sampleRate = 44100))
      try {
        arrIR.grouped(readIR.numChannels).foreach { ir =>
          val (left, right) = ir.map(_.toFloat).splitAt(irLen)
          afOut.write(Array(left, right))
        }

      } finally {
        afOut.close()
      }

    } finally {
      net.close()
    }
  }
}
