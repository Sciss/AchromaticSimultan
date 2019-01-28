/*
 *  ConvolutionTest.scala
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
import de.sciss.lucre.stm.InMemory
import de.sciss.synth.{Buffer, Server, Synth, SynthDef}
import de.sciss.synth.ugen
import de.sciss.synth.Ops._

import scala.swing.event.ValueChanged
import scala.swing.{BoxPanel, FlowPanel, Label, MainFrame, Orientation, Slider, Swing}

object ConvolutionTest {
  type S = InMemory

  def any2stringadd: Any = ()

  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    val fDisk = file("/data/projects/Maeanderungen/audio_work/edited/HB_1_NC_T176.wav")
    val IrLen = 128
    // val IrNum = 2702
    val IrPt  = Common.readPos().map(_.toCartesian)

    Server.run { s =>
      require (s.sampleRate == 48000.0)
      val syn = Synth(s)
      val df = SynthDef("test") {
        import ugen._
        val bufIr   = "bufIr".ir
        val bufDisk = "bufDisk".ir
        val index0  = "index".ar(0f)
        val disk    = DiskIn.ar(numChannels = 1, buf = bufDisk, loop = 1)
        val irPhase = Phasor.ar(hi = IrLen)
        val index   = Latch.ar(index0, irPhase sig_== 0)
        val irIndex = irPhase + (index * IrLen)
        val ir      = BufRd.ar(numChannels = 2, buf = bufIr, index = irIndex, interp = 1)
        val conv    = Convolution.ar(in = disk, kernel = ir, frameSize = IrLen)
        val sig     = Limiter.ar(conv * "amp".kr(1f))
        Out.ar(0, sig)
      }
      df.recv(s)
      val bufDisk = Buffer.cue(s, path = fDisk.path)
      Buffer.read(s, path = Common.fAudioIR.path, completion = { b: Buffer =>
        syn.play("test", args = Seq("bufIr" -> b.id, "bufDisk" -> bufDisk.id))
      })

      Swing.onEDT {
        var azi = 0
        var ele = 0

        def mkLabel(): Label = {
          val lb = new Label("-360°")
          val dim = lb.preferredSize
          lb.preferredSize  = dim
          lb.minimumSize    = dim
          lb.maximumSize    = dim
          lb
        }

        val lbAzi = mkLabel()
        val lbEle = mkLabel()

        def updateLabels(): Unit = {
          lbAzi.text = s"$azi°"
          lbEle.text = s"$ele°"
        }

        updateLabels()

        def setPos(): Unit = {
          val ll      = LatLon(lon = -azi.toRadians, lat = ele.toRadians)
          val pt      = ll.toCartesian
          val bestPt  = IrPt.minBy(_.distanceSq(pt))
          val index   = IrPt.indexOf(bestPt)
          syn.set("index" -> index)
          updateLabels()
        }

        val slidAzi = new Slider {
          orientation = Orientation.Horizontal
          min   = -180
          max   = +180
          value = azi
          listenTo(this)
          reactions += {
            case ValueChanged(_) =>
              azi = value
              setPos()
          }
        }

        val slidEle = new Slider {
          orientation = Orientation.Vertical
          min   = -90
          max   = +90
          value = ele
          listenTo(this)
          reactions += {
            case ValueChanged(_) =>
              ele = value
              setPos()
          }
        }

        new MainFrame {
          title = "HRIR"
          contents = new FlowPanel(
            new BoxPanel(Orientation.Horizontal) {
              contents ++= Seq(new FlowPanel(new Label("Azimuth:"), lbAzi), slidAzi)
            },
            new BoxPanel(Orientation.Vertical) {
              contents ++= Seq(new FlowPanel(new Label("Elevation:"), lbEle), slidEle)
            }
          )

          pack().centerOnScreen()
          open()
        }
      }
    }
  }
}
