scalaVersion := "2.12.8"

scalacOptions := Seq("-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

// libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.2"
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.0"

// libraryDependencies += "edu.berkeley.cs" %% "chisel-testers2" % "0.2-SNAPSHOT"
libraryDependencies += "edu.berkeley.cs" %% "chisel-testers2" % "0.2.0"
