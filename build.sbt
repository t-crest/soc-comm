scalaVersion := "2.12.8"

scalacOptions := Seq("-deprecation", "-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)


libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.1"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1"

// For FIFO buffers
libraryDependencies += "edu.berkeley.cs" % "ip-contributions" % "0.4.0"
// this is only to make ip-contributions happy
libraryDependencies += "edu.berkeley.cs" %% "dsptools" % "1.4.1"

// library name
name := "soc-comm"

// library version
version := "0.1.1"

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
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
