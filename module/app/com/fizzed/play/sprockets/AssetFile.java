package com.fizzed.play.sprockets;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.Logger;

public class AssetFile {

	private static Pattern fingerprintPattern = Pattern.compile(".*\\.([A-Za-z0-9]{32})\\..*");
	
	private String path;			// e.g. stylesheets
	private String name;			// e.g. app
	private String extension;		// e.g. css
	private boolean bundle;			// true if bundle, otherwise false
	private boolean minified;		// true if minified, otherwise false
	private String fingerprint;		// e.g. 04df1c6031020ea2a11f2fa6254b4b2d
	private File localFile;
	
	public AssetFile(AssetFile af) {
		this(af.path, af.name, af.extension, af.bundle, af.minified, af.fingerprint, af.localFile);
	}
	
	public AssetFile(String path, String name, String extension, boolean bundle, boolean minified, String fingerprint, File localFile) {
		super();
		this.path = path;
		this.name = name;
		this.extension = extension;
		this.bundle = bundle;
		this.minified = minified;
		this.fingerprint = fingerprint;
		this.localFile = localFile;
	}

	public AssetFile(String relativeUri) {
		String tempUri = relativeUri;
		
		// extract path
		int lastSlashPos = relativeUri.lastIndexOf('/');
		if (lastSlashPos >= 0) {
			this.path = tempUri.substring(0, lastSlashPos);
			tempUri = tempUri.substring(lastSlashPos+1);
		} else {
			// do not use a null to represent an empty path
			this.path = "";
		}
		
		// bundled?
		int bundlePos = tempUri.indexOf(".bundle.");
		if (bundlePos > 0) {
			this.bundle = true;
			tempUri = tempUri.replace(".bundle.", ".");
		}
		
		// minified?
		int minifiedPos = tempUri.indexOf(".min.");
		if (minifiedPos > 0) {
			this.minified = true;
			tempUri = tempUri.replace(".min.", ".");
		}
		
		// fingerprinted?
		Matcher fingerprintMatcher = fingerprintPattern.matcher(tempUri);
		if (fingerprintMatcher.matches()) {
			this.fingerprint = fingerprintMatcher.group(1);
			tempUri = tempUri.replace("." + this.fingerprint + ".", ".");
		}
		
		// extension should always be the last part
		this.extension = extractExtension(tempUri);
		tempUri = tempUri.substring(0, tempUri.length()- 1 - this.extension.length());
		
		// remaining uri would be the name
		this.name = tempUri;
	}
	
	public AssetFile setPath(String path) {
		this.path = path;
		return this;
	}

	public AssetFile setName(String name) {
		this.name = name;
		return this;
	}

	public AssetFile setExtension(String extension) {
		this.extension = extension;
		return this;
	}

	public AssetFile setBundle(boolean bundle) {
		this.bundle = bundle;
		return this;
	}

	public AssetFile setMinified(boolean minified) {
		this.minified = minified;
		return this;
	}

	public AssetFile setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
		return this;
	}

	public AssetFile setLocalFile(File localFile) {
		this.localFile = localFile;
		return this;
	}

	public String getPath() {
		return path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getExtension() {
		return extension;
	}
	
	public boolean isBundle() {
		return bundle;
	}
	
	public boolean isMinified() {
		return minified;
	}
	
	public boolean isFingerprinted() {
		return this.fingerprint != null;
	}
	
	public String getFingerprint() {
		return fingerprint;
	}
	
	public File getLocalFile() {
		return localFile;
	}
	
	public File createLocalFile(String path) {
		this.localFile = new File(path, toRelativeUri());
		// make sure the directory to the relative uri exists
		localFile.getParentFile().mkdirs();
		return this.localFile;
	}
	
	public String toFileName() {
		StringBuilder s = new StringBuilder();
		s.append(name);
		if (bundle) s.append(".bundle");
		if (minified) s.append(".min");
		if (fingerprint != null) s.append(".").append(fingerprint);
		s.append('.').append(extension);
		return s.toString();
	}
	
	public String toRelativeUri() {
		StringBuilder s = new StringBuilder();
		s.append(path);
		if (!path.equals("")) {
			s.append("/");
		}
		s.append(toFileName());
		return s.toString();
	}
	
	public static String extractExtension(String file) {
		int pos = file.lastIndexOf('.');
		if (pos > 0) {
			return file.substring(pos+1);
		} else {
			return null;
		}
	}
}
