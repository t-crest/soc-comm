scalaVersion := "2.12.8"

scalacOptions := Seq("-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

// libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.2"
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.2"

// libraryDependencies += "edu.berkeley.cs" %% "chisel-testers2" % "0.2-SNAPSHOT"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.2.2"

// this is only to make ip-contributions happy
libraryDependencies += "edu.berkeley.cs" %% "dsptools" % "1.3.0"

Compile / unmanagedSourceDirectories += baseDirectory.value / "ip-contributions"

// sadly the below does not work :-(

// lazy val ipContrib = RootProject(uri("https://github.com/freechipsproject/ip-contributions"))

// lazy val root = Project("root", file(".")) dependsOn(ipContrib)

