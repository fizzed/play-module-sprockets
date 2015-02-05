Sprockets for PlayFramework
================================

 - [Fizzed, Inc.](http://fizzed.com)
 - Joe Lauer (Twitter: [@jjlauer](http://twitter.com/jjlauer))


## Overview

[Play Framework](http://www.playframework.org/) 2.x module that provides an
asset pipeline.  Pipeline will optimize, bundle, and/or fingerprint assets such CSS, JavaScript, and more.
Inspired by Ruby on Rails asset pipeline.

In development mode, optimization can be disabled and normal HTML tags will
be produced which skips bundling, optimization, or minification. In production mode,
optimization will occur at runtime the first time a user connects.


## Compatibility matrix

| PlayFramework version | Module version | 
|:----------------------|:---------------|
| 2.2.x                 | 1.1.0          |
| 2.1.x                 | 1.0.0          |


## Usage

This module is published to Maven Central.  You will need to include the module in your
dependencies list, in `build.sbt` or `Build.scala` file:


### build.sbt

```scala
libraryDependencies ++= Seq(
  "com.fizzed" %% "fizzed-play-module-sprockets" % "1.1.0"
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
    "com.fizzed" %% "fizzed-play-module-sprockets" % "1.1.0"
  )
  
  ...
}
```

## Configuration


### conf/application.conf

Add the following section:

```
mfz-sprockets {
  # true by default in dev, false by default in prod
  #info-enabled = true

  # false by default in either dev or prod
  #debug-enabled = false

  # by default true in prod, false in dev; if set then forced regardless of prod vs. dev
  #optimize-assets = true

  # by default true in prod, false in dev; if set then forced regardless of prod vs. dev
  #cache-control-assets = false
 
  assets {
    # must exist for OptimizedAssets to work
    optimized-assets {
      uri = "/assets/o"
      dirs = "target/optimized-assets"
      fallback = true
    }
  }
}
```

### Within templates

See `samples` project for full example.

To optionally bundle, minify, and fingerprint stylesheets:

```html
@com.fizzed.play.sprockets.OptimizedAssets.stylesheets("app", Array(
  "plugins/bootstrap/css/bootstrap.css",
  "plugins/font-awesome/css/font-awesome.css",
  "css/app.css"))
```

To optionally bundle, minify, and fingerprint javascripts:

```html
@com.fizzed.play.sprockets.OptimizedAssets.javascripts("app", Array(
  "js/jquery-1.10.2.min.js",
  "plugins/bootstrap/js/bootstrap.js", "js/app.js", "js/html5.js"), minify = true)
```

