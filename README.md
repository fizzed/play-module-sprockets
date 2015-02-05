Sprockets for PlayFramework
================================

 - [Fizzed, Inc.](http://fizzed.com)
 - Joe Lauer (Twitter: [@jjlauer](http://twitter.com/jjlauer))


## Overview

Module for [Play Framework](http://www.playframework.org/) 2.x. It provides an
asset pipeline that optimizes, bundles, and/or fingerprints assets such CSS, JavaScript, and more.
Inspired by Ruby on Rails and it's asset pipeline.


## Compatibility matrix

| PlayFramework version | Module version | 
|:----------------------|:---------------|
| 2.1.x                 | 1.0.0          |


## Usage

This module is published to Maven Central.  You will need to include the module in your
dependencies list, in `build.sbt` or `Build.scala` file:


### build.sbt

```scala
libraryDependencies ++= Seq(
  "com.fizzed" %% "fizzed-play-module-sprockets" % "1.0.0"
)
```

### Build.scala

```scala
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    javaCore,
    javaJdbc,
    javaEbean,
    "com.fizzed" %% "fizzed-play-module-sprockets" % "1.0.0"
  )
  
  ...
}


