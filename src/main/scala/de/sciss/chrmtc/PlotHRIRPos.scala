/*
 *  PlotHRIRPos.scala
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

import java.awt.BorderLayout

import de.sciss.chrmtc.Geom.Pt3
import javax.swing.JPanel
import org.jzy3d.chart.{AWTChart, ChartLauncher}
import org.jzy3d.colors.Color
import org.jzy3d.maths.Coord3d
import org.jzy3d.plot3d.primitives.Point

import scala.swing.{Component, Dimension, Frame, MainFrame, SimpleSwingApplication}

/** Plots the position data, in order to verify its consistency */
object PlotHRIRPos extends SimpleSwingApplication {

  lazy val top: Frame = {
    val data = Common.readPos()
    val cart = data.map(_.toCartesian)
    val view = new Impl(cart)
    new MainFrame {
      contents = view.component
      pack().centerOnScreen()
      open()
    }
  }

  private final class Impl(data: Array[Pt3]) { impl =>

    private[this] var _chart  : AWTChart  = _
    private[this] var _component: Component = _

    def component: Component = _component

    guiInit()

//    private def clearChart(): Unit = {
//      _chart.clear()
//      _chart.add(new Point(new Coord3d(-1, -1, -1), Color.WHITE, 0f))
//      _chart.add(new Point(new Coord3d(+1, +1, +1), Color.WHITE, 0f))
//      setChartScale()
//    }

//    private def setChartScale(): Unit = {
//      val scaleN = new Scale(-1, +1)
//      val view = _chart.getView
//      view.setScaleX(scaleN)
//      view.setScaleY(scaleN)
//      view.setScaleZ(scaleN)
//    }

    private def guiInit(): Unit = {
      _chart = new AWTChart()
//      clearChart()

      data.foreach { pt =>
        val coord = new Coord3d(pt.x, pt.y, pt.z)
        val ptNew = new Point(coord, Color.RED, 2f)
        _chart.add(ptNew, false)
      }

      /* val mouse = */ ChartLauncher.configureControllers(_chart, "HRIR", true, false)
      _chart.render()
      //      ChartLauncher.frame(chart, bounds, title)

      val p = new JPanel(new BorderLayout())
      val chartC = _chart.getCanvas.asInstanceOf[java.awt.Component]

//      if (Mellite.isDarkSkin) {
//        val chartV = _chart.getAWTView
//        chartV.setBackgroundColor(Color.BLACK)
//        _chart.getAxeLayout.setMainColor(Color.GRAY)
        //        chartC.setForeground(java.awt.Color.WHITE)
//      }

      chartC.setPreferredSize(new Dimension(480, 480))
      p.add(BorderLayout.CENTER , chartC)
//      p.add(BorderLayout.SOUTH  , pBot.peer)

      _component = Component.wrap(p)
    }

    def dispose(): Unit = {
      _chart.dispose()
    }
  }
}
