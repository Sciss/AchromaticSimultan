/*
 *  Geom.scala
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

import de.sciss.numbers.Implicits._

import scala.math._

object Geom {
  final val PiH = math.Pi/2

  final case class Pt3(x: Double, y: Double, z: Double) {
    override def toString = f"$productPrefix($x%g, $y%g, $z%g)"

    def length: Double = (x.squared + y.squared + z.squared).sqrt

    def * (d: Double): Pt3 = Pt3(x * d, y * d, z * d)

    def + (that: Pt3): Pt3 = Pt3(this.x + that.x, this.y + that.y, this.z + that.z)

    def normalized: Pt3 = this * length.reciprocal

    def dot(that: Pt3): Double = this.x * that.x + this.y * that.y + this.z * that.z

    def distance(that: Pt3): Double = distanceSq(that).sqrt

    def distanceSq(that: Pt3): Double = {
      val dx = this.x - that.x
      val dy = this.y - that.y
      val dz = this.z - that.z
      dx.squared + dy.squared + dz.squared
    }

    def cross(that: Pt3): Pt3 = {
      val xOut = this.y * that.z - this.z * that.y
      val yOut = this.z * that.x - this.x * that.z
      val zOut = this.x * that.y - this.y * that.x
      Pt3(xOut, yOut, zOut)
    }

    def normalizedCross (that: Pt3): Pt3 = (this cross that).normalized

    private def r8_asin(s: Double): Double = asin(s.clip2(1.0))

    // http://people.sc.fsu.edu/~jburkardt/m_src/geometry/r8_atan.m
    // John Burkardt, LGPL
    private def r8_atan(y: Double, x: Double): Double =
      if (x == 0.0) {
        if (y > 0.0)
          Pi / 2.0
        else if (y < 0.0)
          3.0 * Pi / 2.0
        else /* if (y == 0.0) */
          0.0

      } else if (y == 0.0) {
        if (x > 0.0)
          0.0
        else
          Pi

      } else {
        // We assume that ATAN2 is correct when both arguments are positive.

        val abs_y = abs(y)
        val abs_x = abs(x)

        val theta_0 = atan2(abs_y, abs_x)

        if (x > 0.0 && y > 0.0)
          theta_0
        else if (x < 0.0 && y > 0.0)
          Pi - theta_0
        else if (x < 0.0 && y < 0.0)
          Pi + theta_0
        else /* if (x > 0.0 && y < 0.0) */
          2.0 * Pi - theta_0
      }

    def centralAngleBlala(that: Pt3): Double = {
      val lat1 = r8_asin(this.z)
      val lon1 = r8_atan(this.y, this.x)

      val lat2 = r8_asin(that.z)
      val lon2 = r8_atan(that.y, that.x)

      val topSq = (cos(lat2) * sin(lon1 - lon2)).squared +
        (cos(lat1) * sin(lat2) -
          sin(lat1) * cos(lat2) * cos(lon1 - lon2)).squared

      val top = sqrt(topSq)

      val bot = sin(lat1) * sin(lat2) +
        cos(lat1) * cos(lat2) * cos(lon1 - lon2)

      val dist = atan2 ( top, bot )
      dist
    }

    def centralAngle(that: Pt3): Double = {
      val lat1 = r8_asin(this.z)
      val lon1 = r8_atan(this.y, this.x)

      val lat2 = r8_asin(that.z)
      val lon2 = r8_atan(that.y, that.x)

      val sSq = sin((lat1 - lat2) / 2.0).squared +
        cos(lat1) * cos(lat2) * sin((lon1 - lon2) / 2.0).squared

      val s = sqrt(sSq)

      val dist = 2.0 * asin(s)
      dist
    }

    def centralAngleNo(that: Pt3): Double = {
      val d = this distance that
      2 * asin(d/2)
    }

    def centralAngleBla(that: Pt3): Double = {
      val thetaA  = acos(this.z)
      val phiA    = atan2(this.y, this.x)
      val latA    = PiH - thetaA
      val lonA    = phiA

      val thetaB  = acos(that.z)
      val phiB    = atan2(that.y, that.x)
      val latB    = PiH - thetaB
      val lonB    = phiB

      //      2 * asin(sqrt(sin((latA absDif latB)/2).squared + cos(latA) * cos(latB) * sin((lonA absDif lonB)/2).squared))

      acos(latA.sin * latB.sin + latA.cos * latB.cos * (lonA absDif lonB).cos)
    }

    //    def centralAngle(that: Pt3): Double = {
    //      val thetaA  = acos(this.z)
    //      val phiA    = atan2(this.y, this.x)
    //      val latA    = PiH - thetaA
    //      val lonA    = phiA
    //      val cosLatA = cos(latA)
    //      val nax     = cosLatA * cos(lonA)
    //      val nay     = cosLatA * sin(lonA)
    //      val naz     = sin(latA)
    //
    //      val thetaB  = acos(that.z)
    //      val phiB    = atan2(that.y, that.x)
    //      val latB    = PiH - thetaB
    //      val lonB    = phiB
    //      val cosLatB = cos(latB)
    //      val nbx     = cosLatB * cos(lonB)
    //      val nby     = cosLatB * sin(lonB)
    //      val nbz     = sin(latB)
    //
    //      acos(nax * nbx + nay * nby + naz * nbz)
    //    }

    def toPolar: Polar = {
      val theta = acos(z)
      val phi   = atan2(y, x)
      Polar(theta, phi)
    }

    def toLatLon: LatLon = {
      val theta = acos(z)
      val phi   = atan2(y, x)
      val lat   = PiH - theta
      val lon   = phi
      LatLon(lat, lon)
    }

    def rotateX(a: Double): Pt3 = {
      val cosA  = a.cos
      val sinA  = a.sin
      val yR    = y*cosA - z*sinA
      val zR    = y*sinA + z*cosA
      copy(y = yR, z = zR)
    }

    def rotateY(a: Double): Pt3 = {
      val cosA  = a.cos
      val sinA  = a.sin
      val xR    = x*cosA - z*sinA
      val zR    = x*sinA + z*cosA
      copy(x = xR, z = zR)
    }

    def rotateZ(a: Double): Pt3 = {
      val cosA  = a.cos
      val sinA  = a.sin
      val xR    = x*cosA - y*sinA
      val yR    = x*sinA + y*cosA
      copy(x = xR, y = yR)
    }
  }

  /** Polar (two angles) representation of a 3D point.
    *
    * @param theta aka elevation
    * @param phi   aka azimuth
    */
  final case class Polar(theta: Double, phi: Double) {
    def toCartesian: Pt3 = {
      val sinTheta = sin(theta)
      val x = sinTheta * cos(phi)
      val y = sinTheta * sin(phi)
      val z = cos(theta)
      Pt3(x, y, z)
    }

    def toLatLon: LatLon = {
      val lat   = PiH - theta
      val lon   = phi
      LatLon(lat, lon)
    }
  }

  final case class LatLon(lat: Double, lon: Double) {
    override def toString = f"[lat: $lat%1.2f, lon: $lon%1.2f]"

    def toPolar: Polar = {
      val theta = PiH - lat
      val phi   = lon
      Polar(theta, phi)
    }

    def toCartesian: Pt3 = toPolar.toCartesian

    // cf. https://en.wikipedia.org/wiki/Great-circle_distance
    // cf. https://en.wikipedia.org/wiki/Spherical_coordinate_system
    def centralAngle(that: LatLon): Double =
      acos(this.lat.sin * that.lat.sin + this.lat.cos * that.lat.cos * (this.lon absDif that.lon).cos)

    //    def centralAngle(that: Polar): Double =
    //      acos(this.theta.sin * that.theta.sin + this.theta.cos * that.theta.cos * (this.phi absDif that.phi).cos)

    def interpolate(that: LatLon, f: Double): LatLon = {
      import Math._
      // http://edwilliams.org/avform.htm
      val d       = this centralAngle that
      val lat1    = this.lat
      val lon1    = this.lon
      val lat2    = that.lat
      val lon2    = that.lon

      val sinD    = sin(d)
      val a       = sin((1 - f) * d) / sinD
      val b       = sin( f      * d) / sinD
      val cosLat1 = cos(lat1)
      val cosLon1 = cos(lon1)
      val cosLat2 = cos(lat2)
      val cosLon2 = cos(lon2)
      val sinLat1 = sin(lat1)
      val sinLon1 = sin(lon1)
      val sinLat2 = sin(lat2)
      val sinLon2 = sin(lon2)
      val x       = a * cosLat1 * cosLon1 + b * cosLat2 * cosLon2
      val y       = a * cosLat1 * sinLon1 + b * cosLat2 * sinLon2
      val z       = a * sinLat1           + b * sinLat2
      val lat     = atan2(z, sqrt(x * x + y * y))
      val lon     = atan2(y, x)

      LatLon(lat, lon)
    }
  }

  case class Quat(w: Double, x: Double, y: Double, z: Double) {
    private def quatMul(q: Array[Double], r: Array[Double], res: Array[Double]): Unit = {
      res(0) = r(0) * q(0) - r(1) * q(1) - r(2) * q(2) - r(3) * q(3)
      res(1) = r(0) * q(1) + r(1) * q(0) - r(2) * q(3) + r(3) * q(2)
      res(2) = r(0) * q(2) + r(1) * q(3) + r(2) * q(0) - r(3) * q(1)
      res(3) = r(0) * q(3) - r(1) * q(2) + r(2) * q(1) + r(3) * q(0)
    }

    def rotate(p: Pt3): Pt3 = {
      val r     = Array(0.0, p.x, p.y, p.z)
      val q     = Array(w, x, y, z)
      val tmp   = new Array[Double](4)
      quatMul(q, r, tmp)  // first
      // conjugate
      q(1)      = -q(1)
      q(2)      = -q(2)
      q(3)      = -q(3)
      quatMul(tmp, q, r)  // second
      Pt3(r(1), r(2), r(3))
    }
  }
}
