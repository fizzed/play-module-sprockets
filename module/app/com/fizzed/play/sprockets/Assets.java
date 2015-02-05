package com.fizzed.play.sprockets;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.Play;
import play.api.libs.MimeTypes;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;

public class Assets extends Controller {
	
	private static String TZ = "GMT";
	private static DateTimeFormatter DATE_FMT = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + TZ + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(TZ));
	private static String DEFAULT_CHARSET = Play.application().configuration().getString("default.charset", "utf-8");

	private static ConcurrentHashMap<String,String> LAST_MODIFIEDS = new ConcurrentHashMap<String,String>();
	private static ConcurrentHashMap<String,String> ETAGS = new ConcurrentHashMap<String,String>();
	public static final Map<String,AssetConfiguration> ASSET_CONFIGS_BY_NAME = AssetConfiguration.createAssetConfigurations();
	public static final Map<String,AssetConfiguration> ASSET_CONFIGS_BY_URI = AssetConfiguration.toMapByUri(ASSET_CONFIGS_BY_NAME);
	public static final boolean CACHE_CONTROL_ENABLED = Play.application().configuration().getBoolean(Constants.CACHE_CONTROL_ASSETS, Play.isProd());
	public static final boolean INFO_ENABLED = Play.application().configuration().getBoolean(Constants.INFO_ENABLED, Play.isDev());
	public static final boolean DEBUG_ENABLED = Play.application().configuration().getBoolean(Constants.DEBUG_ENABLED, false);
	
	// https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/controllers/ExternalAssets.scala
	// https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/controllers/Assets.scala
	public static Result at(String file) throws Exception {
		//Logger.debug("file requested: " + file);
		
		AssetFile af = new AssetFile(file);
		
		//
		// if provided etag matches what we have then return not-modified header
		//
		String currentETag = ETAGS.get(file);
		if (currentETag != null) {
			// check etag. Important, if there is an If-None-Match header, we MUST not check the
		    // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
		    // accordance with section 14.26 of RFC2616.
			String ifNoneMatchHeader = request().getHeader(IF_NONE_MATCH);
			if (ifNoneMatchHeader != null) {
				String[] etags = ifNoneMatchHeader.split(",");
				for (String etag : etags) {
					etag = etag.trim();
					if (etag.equals(currentETag)) {
						return status(NOT_MODIFIED);
					}
				}
			}
		}
		
		// TODO: support if-modified-since header as well???
		
		// search multiple locations in order until we find file
		URL url = null;
		
		// based on request uri -- find which if a configuration is defined for prefix
		// e.g. /assets/o/path/to/file/i/want
		String requestUri = request().uri();
		String assetPrefixUri = requestUri.replace("/" + file, "");
		AssetConfiguration ac = ASSET_CONFIGS_BY_URI.get(assetPrefixUri);
		
		// empty search by default, otherwise use configuration
		String[] dirs = new String[0];
		if (ac != null) {
			dirs = ac.getDirs();
		}
		
		SEARCH_LOOP:
		for (String dir : dirs) {
			java.io.File d = new java.io.File(dir);
			java.io.File f = af.createLocalFile(dir);
			
			// security: make sure the canonical path (with relative paths removed) matches
			// what we original had as our path
			String dcp = d.getCanonicalPath();
			String fcp = f.getCanonicalPath();
			if (!fcp.startsWith(dcp)) {
				Logger.error("security warning: canonical path of file " + fcp + " does not start with dir " + dcp);
				return notFound();
			}
			
			if (f.exists()) {
				url = f.toURI().toURL();
				break SEARCH_LOOP;
			}
		}
		
		// if asset has not yet been found, fallback to way controllers.Assets.at()
		// would search for the asset if the configuration was null OR fallback enabled
		if (url == null && (ac == null || ac.isFallbackEnabled())) {
			// search for assets as a resource
			url = OptimizedAssetsHelper.assetUrl(file);
		}
		
		if (url == null) {
			// exhaustive search yielded nothing
			return notFound();
		}
		
		String externalForm = url.toExternalForm();
		
		//
		// content-type header
		//
		response().setHeader(CONTENT_TYPE, getContentType(af));
		
		//
		// date header
		//
		String lastModified = LAST_MODIFIEDS.get(file);
		if (lastModified == null) {
			long ts = getLastModified(url);
			lastModified = DATE_FMT.print(ts);
			// only cache result in production
			if (Play.isProd()) {
				LAST_MODIFIEDS.put(file, lastModified);
			}
		} else {
			//Logger.debug("Using cached lastModified: file=" + file + "; value=" + lastModified);
		}
		
		if (lastModified != null) {
			response().setHeader(LAST_MODIFIED, lastModified);
		}
		
		//
		// etag & cache-control header
		//
		if (CACHE_CONTROL_ENABLED) {
			String etag = ETAGS.get(file);
			if (etag == null) {
				if (af.isFingerprinted()) {
					// etag isn't generated -- its pulled exactly from asset file name
					etag = getETag(af);
				} else {
					// The default Play Assets controller generates an ETag from the resource name
					// and the file’s last modification date. This is not really a "strong" method
					// for generating an etag, but it does mirror what play does
					etag = createETag(externalForm, lastModified);
				}
				
				// only cache result in production
				if (Play.isProd()) {
					ETAGS.put(file, etag);
				}
			} else {
				//Logger.debug("Using cached etag: file=" + file + "; value=" + etag);
			}
			
			if (etag != null) {
				response().setHeader(ETAG, etag);
				
				if (af.isFingerprinted()) {
					// fingprinted assets are safe to cache for a long, long time (1 year by default)
					response().setHeader(CACHE_CONTROL, "max-age=31556926");
				} else {
					// controllers.Assets.at() by default does just 1 hour
					response().setHeader(CACHE_CONTROL, "max-age=3600");
				}
			}
		} else {
			response().setHeader(CACHE_CONTROL, "no-cache");
		}
		
		// set to current time
		response().setHeader(DATE, DATE_FMT.print(System.currentTimeMillis()));
		
		InputStream is = url.openStream();
		int length = is.available();
		response().setHeader(CONTENT_LENGTH, length+"");
		
		return ok(is);
	}
	
	public static String cleanETag(String etag) {
		String s = etag.trim();
		if (s.startsWith("\"")) {
			s = s.substring(1);
		}
		if (s.endsWith("\"")) {
			s = s.substring(0, s.length()-1);
		}
		return s;
	}
	
	public static String getETag(AssetFile af) {
		if (af.isFingerprinted()) {
			return "\"" + af.getFingerprint() + "\"";
		} else {
			return "";
		}
	}
	
	public static String createETag(String externalForm, String lastModified) throws Exception {
		// exact format used by controllers.Assets.at()
		String data = lastModified + " -> " + externalForm;
		return "\"" + OptimizedAssetsHelper.sha1(data.getBytes("UTF-8")) + "\"";
	}
	
	public static String getContentType(AssetFile af) {
		// CONTENT_TYPE -> MimeTypes.forFileName(file).map(m => m + addCharsetIfNeeded(m)).getOrElse(BINARY),
		Option<String> o = MimeTypes.forExtension(af.getExtension());
		if (!o.isDefined()) {
			return "application/octet-stream";
		} else {
			String mimeType = o.get();
			if (MimeTypes.isText(mimeType)) {
				return mimeType + "; charset=" + DEFAULT_CHARSET;
			} else {
			    return mimeType;
			}
		}
	}
	
	public static long getLastModified(URL url) throws Exception {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) {
			java.io.File f = new java.io.File(url.getPath());
			return f.lastModified();
		} else if (protocol.equals("jar")) {
			JarURLConnection jar = (JarURLConnection)url.openConnection();
			return jar.getJarEntry().getTime();
		} else {
			return -1;
		}
	}
	
	public static long getSize(URL url) throws Exception {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) {
			java.io.File f = new java.io.File(url.getPath());
			return f.length();
		} else if (protocol.equals("jar")) {
			JarURLConnection jar = (JarURLConnection)url.openConnection();
			return jar.getJarEntry().getSize();
		} else {
			return -1;
		}
	}
}
