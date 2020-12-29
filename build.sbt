scalaVersion := "2.12.8"

scalacOptions := Seq("-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)


libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.2"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.2.2"

// For FIFO buffers
libraryDependencies += "io.github.schoeberl" % "ip-contributions" % "1.0.0"
// this is only to make ip-contributions happy
libraryDependencies += "edu.berkeley.cs" %% "dsptools" % "1.3.0"

// library name
name := "soc-comm"

// library version
version := "0.1.0"

// groupId, SCM, license information
organization := "io.github.chiselverify"
homepage := Some(url("https://github.com/chiselverify/chiselverify"))
scmInfo := Some(ScmInfo(url("https://github.com/chiselverify/chiselverify"), "git@github.com: chiselverify/chiselverify.git"))
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