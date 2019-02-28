/*
 *  IMUTest.scala
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

import com.tinkerforge.BrickIMUV2.QuaternionListener
import com.tinkerforge.{BrickIMUV2, IPConnection}
import de.sciss.achromatic.Geom.{Pt3, Quat}

import scala.concurrent.ExecutionContext.Implicits.global

object IMUTest {
  def main(args: Array[String]): Unit = {
    val futSet = ConvolutionTest.run(gui = false)
    futSet.foreach { set =>
      run(set)
    }
  }

  def run(set: (Double, Double) => Unit): Unit = {
    val c = new IPConnection
    // Create IP connection
    val imu = new BrickIMUV2(Common.TinkerDefaultIMU_UID, c) // Create device object
    c.connect(Common.TinkerHost, Common.TinkerPort)     // Connect to brickd


    imu.addQuaternionListener(new QuaternionListener {
      private[this] val scaleQuat = 1.0/16383.0
      private[this] val ref       = Pt3(1, 0, 0)

      def quaternion(wi: Short, xi: Short, yi: Short, zi: Short): Unit = {
        val w = wi * scaleQuat
        val x = xi * scaleQuat
        val y = yi * scaleQuat
        val z = zi * scaleQuat

        val rot = Quat(w, x, y , z).rotate(ref)
        val ll  = rot.toLatLon
        set(-ll.lon, ll.lat)
      }
    })

    imu.setQuaternionPeriod(20)
  }
}
