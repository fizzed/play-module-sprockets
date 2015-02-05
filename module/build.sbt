name := "fizzed-play-module-sprockets"

version := "1.2.0"

startYear := Some(2013)

description := "PlayFramework 2.x module to create a Ruby-on-Rails inspired asset pipeline (optimize, bundle, and/or fingerprint assets such as CSS, JavaScript, and more)"

organization := "com.fizzed"

organizationName := "Fizzed, Inc."

organizationHomepage := Some(new URL("http://fizzed.com"))

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.yahoo.platform.yui" % "yuicompressor" % "2.4.7" exclude("rhino", "js")
)

// required for publishing artifact to maven central via sonatype
// credentials now in ~/.sbt/0.13/sonatype.sbt

publishMavenStyle := true

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://fizzed.com/oss/play-module-sprockets"))

scmInfo := Some(ScmInfo(url("https://github.com/fizzed/play-module-sprockets"), "https://github.com/fizzed/play-module-sprockets.git"))

pomExtra := (
<developers>
  <developer>
    <name>Fizzed Inc</name>
    <email>oss@fizzed.com</email>
  </developer>
</developers>
)
