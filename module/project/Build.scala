import sbt._
import Keys._
import play.Project._
import com.typesafe.sbt.SbtPgp._

object ApplicationBuild extends Build {

  val appName         = "fizzed-play-module-sprockets"
  val appVersion      = "1.0.0"

  val appDependencies = Seq(
    "com.yahoo.platform.yui" % "yuicompressor" % "2.4.7" exclude("rhino", "js"),
    javaCore,
    javaJdbc,
    javaEbean,
    anorm
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "com.fizzed",
    organizationName := "Fizzed, Inc.",
    organizationHomepage := Some(new URL("http://fizzed.com")),
    
    // required for publishing artifact to maven central via sonatype
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials"),
    publishMavenStyle := true,
    publishTo <<= version { v: String =>
	  val nexus = "https://oss.sonatype.org/"
	  if (v.trim.endsWith("SNAPSHOT"))
	    Some("snapshots" at nexus + "content/repositories/snapshots")
	  else
	    Some("releases" at nexus + "service/local/staging/deploy/maven2")
	},
	
	// in order to pass sonatype's requirements the following properties are required as well
	startYear := Some(2013),
	description := "PlayFramework 2.x module to create a Ruby-on-Rails inspired asset pipeline (optimize, bundle, and/or fingerprint assets such as CSS, JavaScript, and more)",
    licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("http://fizzed.com/oss/play-module-sprockets")),
    scmInfo := Some(ScmInfo(url("https://github.com/fizzed/play-module-sprockets"), "https://github.com/fizzed/play-module-sprockets.git")),
    pomExtra := (
      <developers>
        <developer>
    	    <name>Fizzed Inc</name>
          <email>oss@fizzed.com</email>
        </developer>
      </developers>
    )
  )
}
