package com.fizzed.play.sprockets

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import java.io._

import com.fizzed.play.sprockets.OptimizedAssetsHelper;

import play.api.templates.Html

/**
 * 	Implements http://guides.rubyonrails.org/asset_pipeline.html
 */
object OptimizedAssets extends Controller {
  
    def fingerprint(file: String): Html = {
	  OptimizedAssetsHelper.optimize("", "", "", false, false, Array(file))
	}

    def stylesheet(file: String, media: String = "", minify: Boolean = true): Html = {
	  OptimizedAssetsHelper.stylesheets("", minify, false, Array(file), media)
	}
    
    def stylesheets(name: String, files: Array[String], media: String = "", minify: Boolean = true, bundle: Boolean = true): Html = {
	  OptimizedAssetsHelper.stylesheets(name, minify, bundle, files, media)
	}
	
	def javascript(file: String, minify: Boolean = true): Html = {
	  OptimizedAssetsHelper.javascripts("", minify, false, Array(file))
	}
	
	def javascripts(name: String, files: Array[String], minify: Boolean = true, bundle: Boolean = true): Html = {
	  OptimizedAssetsHelper.javascripts(name, minify, bundle, files)
	}
  
}