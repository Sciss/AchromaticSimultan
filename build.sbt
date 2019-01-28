lazy val baseName       = "_chr_m_t_c"
lazy val baseNameL      = "chrmtc" // baseName.toLowerCase
lazy val appDescription = "Sketches for a sound art piece"
lazy val projectVersion = "0.1.0-SNAPSHOT"
lazy val mimaVersion    = "0.1.0"
  
lazy val authorName     = "Hanns Holger Rutz"
lazy val authorEMail    = "contact@sciss.de"

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val soundProcesses  = "3.24.0"
    val jzy3d           = "1.0.2"
    val lucre           = "3.11.0"
    val lucreMatrix     = "1.6.0"
    val slf4j           = "1.7.25"
    val tinker          = "2.1.21"
  }
}

// ---- common ----

lazy val commonSettings = Seq(
  version            := projectVersion,
  organization       := "de.sciss",
  homepage           := Some(url(s"https://git.iem.at/sciss/$baseName")),
  licenses           := Seq("GNU Affero General Public License v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
  scalaVersion       := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint:-stars-align,_", "-Xsource:2.13"
  ),
  updateOptions := updateOptions.value.withLatestSnapshots(false)
)

// ---- projects ----

lazy val root = project.withId(baseNameL).in(file("."))
  .settings(commonSettings)
  .settings(
    name        := baseName,
    description := appDescription,
    resolvers         ++= Seq(
      "Oracle Repository" at "http://download.oracle.com/maven",                                          // required for sleepycat
      "Unidata Releases"  at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases",  // required for NetCDF
      "jzv3d releases"    at "http://maven.jzy3d.org/releases"                                            // 3D chart
    ),
    libraryDependencies ++= Seq(
      "com.tinkerforge" %  "tinkerforge"          % deps.main.tinker,           // IMU sensor interface
      "de.sciss"        %% "lucre-bdb"            % deps.main.lucre,            // object system (database backend)
      "de.sciss"        %% "lucrematrix"          % deps.main.lucreMatrix,      // HDF support
      "de.sciss"        %% "soundprocesses-core"  % deps.main.soundProcesses,   // computer-music framework
      "org.jzy3d"       %  "jzy3d-api"            % deps.main.jzy3d,            // 3D Plot
      "org.slf4j"       %  "slf4j-nop"            % deps.main.slf4j,            // disable logger output
    )
  )
