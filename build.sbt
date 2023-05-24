scalaVersion := "2.12.13"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:reflectiveCalls",
)

val chiselVersion = "3.5.5"
addCompilerPlugin("edu.berkeley.cs" %% "chisel3-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.5"

// For FIFO buffers
libraryDependencies += "edu.berkeley.cs" % "ip-contributions" % "0.5.1"

// library name
name := "soc-comm"

// library version
version := "0.1.5"

// groupId, SCM, license information
organization := "io.github.t-crest"
homepage := Some(url("https://github.com/t-crest/soc-comm"))
scmInfo := Some(ScmInfo(url("https://github.com/t-crest/soc-comm"), "https://github.com/t-crest/soc-comm.git"))
developers := List(Developer("schoeberl", "schoeberl", "martin@jopdesign.com", url("https://github.com/schoeberl")))
licenses += ("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause"))
publishMavenStyle := true

// disable publishw ith scala version, otherwise artifact name will include scala version
// e.g cassper_2.11
crossPaths := false

// add sonatype repository settings
// snapshot versions publish to sonatype snapshot repository
// other versions publish to sonatype staging repository
publishTo := Some(Opts.resolver.sonatypeStaging)


lazy val soc_comm = (project in file("."))
