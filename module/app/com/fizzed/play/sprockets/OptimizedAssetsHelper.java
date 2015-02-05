package com.fizzed.play.sprockets;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import play.Logger;
import play.Play;
import play.api.templates.Html;
import play.mvc.Controller;
import scala.collection.mutable.StringBuilder;

import com.google.common.io.ByteStreams;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class OptimizedAssetsHelper extends Controller {

	public static String publicPath = "public";
	public static String assetsUri = "/assets";
	
	// use configured value or default to true if in prod, otherwise false
	public static boolean OPTIMIZE_ENABLED = Play.application().configuration().getBoolean(Constants.OPTIMIZE_ASSETS, Play.isProd());
	
	public static AssetConfiguration CONFIG = null; 
	public static ConcurrentHashMap<String,Html> ASSET_REQUESTS = new ConcurrentHashMap<String,Html>();
	public static ConcurrentHashMap<String,Long> ASSET_REQUESTS_AT = new ConcurrentHashMap<String,Long>();
	
	public static Html stylesheets(String name, boolean minify, boolean bundle, String[] files, String media) throws Exception {
		String tagStart = "<link rel=\"stylesheet\" type=\"text/css\" href=\"";
		String tagEnd = "\"" + (media != null && !media.equals("") ? " media=\"" + media + "\"" : "") + ">";
		return optimize(name, tagStart, tagEnd, minify, bundle, files);
	}
	
	public static Html javascripts(String name, boolean minify, boolean bundle, String[] files) throws Exception {
		// <script type="text/javascript" src="@routes.Assets.at("js/jquery-1.10.2.js")"></script>
		String tagStart = "<script type=\"text/javascript\" src=\"";
		String tagEnd = "\"></script>";
		return optimize(name, tagStart, tagEnd, minify, bundle, files);
	}
	
	public static void loadConfigIfNeeded() {
		if (CONFIG == null) {
			CONFIG = Assets.ASSET_CONFIGS_BY_NAME.get(Constants.OPTIMIZED_ASSETS);
			if (CONFIG == null) {
				throw new RuntimeException("configuration for optimized-assets cannot be null!");
			}
		}
	}
	
	public static Html optimize(String name, String tagStart, String tagEnd, boolean minify, boolean bundle, String[] files) throws Exception {
		loadConfigIfNeeded();
		
		// generate key of asset request (basically all parameters)
		String assetRequestKey = assetKey(name, minify, bundle, files);
		
		// if a valid & cached response exists then return it
		Html response = ASSET_REQUESTS.get(assetRequestKey);
		if (response != null) {
			// in dev mode even with optimize enabled, we also will check if any of the underlying
			// assets have been modified from when the response was cached
			if (OPTIMIZE_ENABLED && Play.isDev()) {
				Long cutoff = ASSET_REQUESTS_AT.get(assetRequestKey);
				if (cutoff == null || haveAnyAssetsBeenModifiedAfter(cutoff, files)) {
					Logger.info("Invalidating cached response; creating new optimized asset");
					ASSET_REQUESTS.remove(assetRequestKey);
					ASSET_REQUESTS_AT.remove(assetRequestKey);
				} else {
					return response;
				}
			} else {
				return response;
			}
		}
		
		// first file will establish baseline for extensions
		String extension = getExtension(files[0]);
		
		// verify each file is of the same type
		verifyAllExtensionsMatch(extension, files);
		
		if (!OPTIMIZE_ENABLED) {
			// if optimize is disabled, return a link tag for each asset
			StringBuilder html = new StringBuilder();

			// return normal response from "Assets.at"
			for (String file : files) {
				html.append(tagStart);
				html.append(assetsUri + "/" + file);
				html.append(tagEnd);
			}
			
			response = new Html(html);
			ASSET_REQUESTS_AT.putIfAbsent(assetRequestKey, System.currentTimeMillis());
			ASSET_REQUESTS.putIfAbsent(assetRequestKey, response);
			return response;
		} else {
			ArrayList<AssetFile> assetFiles = new ArrayList<AssetFile>();
			int fileCount = files.length;
			
			// bundle files together, minimize, md5, create new asset single time
			if (Assets.INFO_ENABLED) Logger.debug("Optimizing " + fileCount + " " + extension + " asset(s): minify=" + minify + "; bundle=" + bundle + "; files=" + assetList(files));
			
			// metrics
			long start = System.currentTimeMillis();
			long startSize = 0;
			long finalSize = 0;
			
			//
			// prep each file as an asset to be processed
			//
			for (String file : files) {
				// create a new asset file wrapper for each asset
				AssetFile af = new AssetFile(file);
				
				// verify the source file exists
				URI uri = assetFileToURI(af);
				if (uri == null) {
					throw new Exception("Asset file " + file + " does not exist");
				}
				
				if (Assets.INFO_ENABLED) {
					startSize += Assets.getSize(uri.toURL());
				}
				
				assetFiles.add(af);
			}
			
			//
			// minify 1 or more files if needed!
			// processing each file separately speeds up vs. minifying a large bundle later
			//
			if (minify) {
				for (int i = 0; i < assetFiles.size(); i++) {
					AssetFile af = assetFiles.get(i);
					// only minify files that aren't yet minified
					if (!af.isMinified()) {
						AssetFile maf = minify(af);
						if (Assets.DEBUG_ENABLED) Logger.debug("Minified asset: " + af.toRelativeUri() + " -> " + maf.toRelativeUri());
						// replace our assets with this one
						assetFiles.set(i, maf);
					} else {
						if (Assets.DEBUG_ENABLED) Logger.debug("Asset already minified: " + af.toRelativeUri());
					}
				}
			}
			
			//
			// bundle if needed!
			//
			if (bundle && fileCount > 1) {
				// bundle all assets into a single asset file
				AssetFile baf = bundle(name, extension, assetFiles);
				if (Assets.DEBUG_ENABLED) Logger.debug("Bundled asset: " + fileCount + " assets -> " + baf.toRelativeUri());
				// clear all assets and just continue processing the bundle
				assetFiles.clear();
				assetFiles.add(baf);
			}
			
			//
			// fingerprint if needed!
			//
			for (int i = 0; i < assetFiles.size(); i++) {
				AssetFile af = assetFiles.get(i);
				// only fingerprint files that aren't yet fingerprinted
				if (!af.isFingerprinted()) {
					AssetFile fpaf = fingerprint(af);
					// replace our assets with this one
					assetFiles.set(i, fpaf);
				}
			}
			
			//
			// final tally of size
			//
			if (Assets.INFO_ENABLED) {
				for (AssetFile af : assetFiles) {
					finalSize += assetFileSize(af);
				}
			}
			
			long stop = System.currentTimeMillis();
			if (Assets.INFO_ENABLED) {
				double ratio = (1 - ((double)finalSize/(double)startSize))*100;
				String ratioPct = String.format("%.2f", ratio); 
				Logger.debug("Optimized " + fileCount + " " + extension + " asset(s): time=" + (stop-start) + " ms; final/start bytes=" + finalSize + "/" + startSize + "; optimized=" + ratioPct + "%");
			}
			
			StringBuilder html = new StringBuilder();
			
			for (AssetFile af : assetFiles) {
				html.append(tagStart);
				
				// we only optimized the file if a "localFile" exists for the asset
				if (af.getLocalFile() != null) {
					html.append(CONFIG.getUri() + "/" + af.toRelativeUri());
				} else {
					html.append(assetsUri + "/" + af.toRelativeUri());
				}
				
				html.append(tagEnd);
			}
			
			response = new Html(html);
			ASSET_REQUESTS_AT.putIfAbsent(assetRequestKey, System.currentTimeMillis());
			ASSET_REQUESTS.putIfAbsent(assetRequestKey, response);
			return response;
		}
	}
	
	public static String getExtension(String file) {
		int pos = file.lastIndexOf('.');
		if (pos > 0) {
			return file.substring(pos+1);
		} else {
			return null;
		}
	}
	
	public static void verifyAllExtensionsMatch(String extension, String ... files) throws Exception {
		// single files will obviously verify
		if (files.length <= 1) { return; }
		
		// verify each file is of the same type
		for (String file : files) {
			String tempExtension = getExtension(file);
			if (!extension.equals(tempExtension)) {
				throw new Exception("All file extensions must match! File " + file + " extension " + tempExtension + " != " + extension);
			}
		}
	}
	
	public static AssetFile bundle(String bundleName, String extension, ArrayList<AssetFile> afs) throws Exception {
		java.lang.StringBuilder bundledContentBuffer = new java.lang.StringBuilder();
		
		// spit out header
		bundledContentBuffer.append("/*!\n * Fizzed Optimized Asset\n * Bundle | Minify | Fingerprint!\n * http://fizzed.com\n */");

		// track if each bundled assets is minified (if they are then the bundle is minified!)
		boolean allMinified = true;
		
		for (AssetFile af : afs) {
			// logical and with each asset
			allMinified &= af.isMinified();
			
			String file = af.toRelativeUri();
			
			//String content = assetReadAllToString(file);
			String content = assetFileReadAllToString(af);
			if (content == null) {
				throw new Exception("Unable to read asset: " + file + " (probably doesn't exist?)");
			}
			
			// directory of current file we're bundling (by default assume root path of website)
			String dir = "";
			int lastSlashPos = file.lastIndexOf('/');
			if (lastSlashPos >= 0) {
				dir = file.substring(0, lastSlashPos);
			}
			
			// always write out comments of bundled file
			bundledContentBuffer.append("\n/*! bundled file: " + file + " */\n");
			
			if (extension.equals("css")) {
				addStylesheetFileToBundle(file, dir, content, bundledContentBuffer);
			} else {
				bundledContentBuffer.append(content);
			}
		}
		
		byte[] data = bundledContentBuffer.toString().getBytes("UTF-8");
		String md5 = md5(data);
		
		// new bundle file at root of optimized!
		AssetFile af = new AssetFile("", bundleName, extension, true, allMinified, md5, null);
		java.io.File localFile = af.createLocalFile(CONFIG.getTargetDir());
		
		// optimization -- only persist if it doesn't yet exist
		if (!localFile.exists()) {
			java.io.FileOutputStream fos = new java.io.FileOutputStream(localFile);
			fos.write(data);
			fos.flush();
			fos.close();
		}
		
		return af;
	}
	
	public static void addStylesheetFileToBundle(String file, String dir, String content, java.lang.StringBuilder bundledContentBuffer) throws Exception {
		// bundling css is dangerous - every url() will break
		// replace all url() with adjusted path
		boolean keepReplacing = true;
		int replaced = 0;
		int atPos = 0;
		int searchPos = 0;
		java.lang.StringBuilder replacementBuffer = new java.lang.StringBuilder();
		while (keepReplacing) {
			int urlStartPos = content.indexOf("url(", searchPos);
			if (urlStartPos > 0) {
				int urlEndPos = content.indexOf(")", urlStartPos);
				if (urlEndPos > 0) {
					String url = content.substring(urlStartPos+4, urlEndPos).trim();
					
					// chop off first ' or " ?
					char firstChar = url.charAt(0);
					char lastChar = url.charAt(url.length()-1);
					
					if (firstChar == '"' || firstChar == '\'') {
						url = url.substring(1);
					}
					
					if (lastChar == '"' || lastChar == '\'') {
						url = url.substring(0, url.length()-1);
					}
					
					// only relative urls need replaced
					if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("/")) {
						// skip replacement
					} else {
						// need to replace url with adjusted url
						// first, append whatever was in-between
						replacementBuffer.append(content, atPos, urlStartPos);
						
						// canonical will resolve .. or . in the path to something clean
						String resolvedUrl = new java.io.File(assetsUri + "/" + dir + "/" + url).getCanonicalPath();
						//Logger.debug("bundling file=" + file + " url=" + url + " to=" + resolvedUrl + " @ " + urlStartPos);
						
						// append the adjusted url
						replacementBuffer.append("url(\"");
						replacementBuffer.append(resolvedUrl);
						replacementBuffer.append("\")");
						
						replaced++;
						atPos = urlEndPos + 1;
					}
					
					searchPos = urlEndPos + 1;
				} else {
					// unacceptable error!
					throw new Exception("Unable to find matching url closing parenthese!");
				}
			} else {
				keepReplacing = false;
			}
		}
		
		// if replacements were made
		if (replaced > 0) {
			// append replacement buffer, the rest of file
			bundledContentBuffer.append(replacementBuffer);
			bundledContentBuffer.append(content, atPos, content.length());
		} else {
			// optimization -- no replacement buffer was filled, just append content
			bundledContentBuffer.append(content);
		}
	}
	
	public static String assetKey(String name, boolean minify, boolean bundle, String[] files) {
	    return new StringBuilder()
	    	.append(name)
	    	.append(':')
	    	.append(minify)
	    	.append(':')
	    	.append(bundle)
	    	.append(';')
	    	.append(assetList(files))
	    	.toString();
	}
	
	public static String assetList(String[] files) {
	    if (files.length == 1) {
	    	return files[0];
	    }
	    StringBuilder key = new StringBuilder();
	    int i = 0;
	    for (String f : files) {
	    	if (i != 0) { key.append(", "); }
	    	key.append(f);
	    	i++;
	    }
	    return key.toString();
	}
	
	public static URL assetUrl(String file) {
		// where assets are located either in jar OR local dir
		String resourceName = publicPath + "/" + file;
		return Play.application().resource(resourceName);
	}
	
	public static long assetLastModified(String file) throws Exception {
		URL url = assetUrl(file);
		return Assets.getLastModified(url);
	}
	
	public static long assetFileSize(AssetFile af) throws Exception {
		URI uri = assetFileToURI(af);
		return Assets.getSize(uri.toURL());
	}
	
	public static boolean haveAnyAssetsBeenModifiedAfter(long cutoff, String ... files) throws Exception {
		for (String file : files) {
			long ts = assetLastModified(file);
			if (ts > cutoff) {
				Logger.info("Asset " + file + " was recently modified");
				return true;
			}
		}
		return false;
	}
	
	public static URI assetFileToURI(AssetFile af) throws URISyntaxException {
		if (af.getLocalFile() != null) {
			return af.getLocalFile().toURI();
		} else {
			// asset must be a resource
			URL url = assetUrl(af.toRelativeUri());
			if (url == null) {
				return null;
			}
			return url.toURI();
		}
	}
	
	public static InputStream assetFileToInputStream(AssetFile af) throws Exception {
		if (af.getLocalFile() != null) {
			return new FileInputStream(af.getLocalFile());
		} else {
			// asset must be a resource
			URL url = assetUrl(af.toRelativeUri());
			if (url == null) {
				return null;
			}
			return url.openStream();
		}
	}
	
	public static byte[] assetFileReadAllToByteArray(AssetFile af) throws Exception {
		// modified version using an input stream instead
		InputStream is = assetFileToInputStream(af);
		
		if (is == null) { return null; }
		
		try {
			return ByteStreams.toByteArray(is);
		} finally {
			if (is != null) { is.close(); }
		}
	}
	
	public static String assetFileReadAllToString(AssetFile af) throws Exception {
		byte[] bytes = assetFileReadAllToByteArray(af);
		
		if (bytes == null) { return null; }
		
		return new String(bytes, "UTF-8");
	}
	
	public static AssetFile fingerprint(AssetFile af) throws Exception {
		byte[] bytes = assetFileReadAllToByteArray(af);
		
		if (bytes == null) { return null; }
		
        String hash = md5(bytes);
        
        AssetFile fpaf = new AssetFile(af)
        	.setFingerprint(hash);
        
        java.io.File targetFile = fpaf.createLocalFile(CONFIG.getTargetDir());
        
        // copy asset (if it does not yet exist)
        if (!targetFile.exists()) {
        	Files.write(targetFile.toPath(), bytes);
        }
        
        return fpaf;
	}
	
	public static AssetFile minify(AssetFile af) throws Exception {
		// get correct reader for file
		InputStream is = assetFileToInputStream(af);
		
		if (is == null) {
			throw new Exception("Unable to find resource for asset " + af.toRelativeUri());
		}
		
		try {
			// minify file, then fingerprint it
			StringWriter out = new StringWriter();
			
			if (af.getExtension().equals("css")) {
				CssCompressor compressor = new CssCompressor(new InputStreamReader(is, "UTF-8"));
				compressor.compress(out, 300);
			} else if (af.getExtension().equals("js")) {
				ErrorReporter reporter = new ErrorReporter() {
					@Override
					public void error(String arg0, String arg1, int arg2, String arg3, int arg4) {
						Logger.error("javascript minify error: " + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + arg4);
					}

					@Override
					public EvaluatorException runtimeError(String arg0, String arg1, int arg2, String arg3, int arg4) {
						Logger.error("javascript minify error: " + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + arg4);
						return null;
					}

					@Override
					public void warning(String arg0, String arg1, int arg2, String arg3, int arg4) {
						Logger.warn("javascript minify warn: " + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + arg4);
					}
				};
				
				JavaScriptCompressor compressor = new JavaScriptCompressor(new InputStreamReader(is, "UTF-8"), reporter);
				// munge = true (much smaller files), verbose=false, preserveAllSemis=false; disableOptimizations=true
				compressor.compress(out, 300, true, false, false, true);
			} else {
				throw new UnsupportedOperationException("Unable to minify type " + af.getExtension());
			}
			
			byte[] data = out.toString().getBytes("UTF-8");
			String md5 = md5(data);
			
			// create the new minified version
			AssetFile maf = new AssetFile(af)
				.setMinified(true)
				.setFingerprint(md5);
			
			java.io.File localFile = maf.createLocalFile(CONFIG.getTargetDir());
			
			// only persist out file if it does not yet exist
			if (!localFile.exists()) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream(localFile);
				fos.write(data);
				fos.flush();
				fos.close();
			}
			
			return maf;
		} finally {
			if (is != null) { is.close(); }
		}
	}
	
	public static String md5(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digest = md.digest(data);
		return toHexString(digest);
	}
	
	public static String sha1(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		byte[] digest = md.digest(data);
		return toHexString(digest);
	}
	
	public static String toHexString(byte[] bytes) {
	    char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j*2] = hexArray[v/16];
	        hexChars[j*2 + 1] = hexArray[v%16];
	    }
	    return new String(hexChars);
	}
}
