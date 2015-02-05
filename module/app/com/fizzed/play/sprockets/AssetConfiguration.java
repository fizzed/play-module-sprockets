package com.fizzed.play.sprockets;

import java.util.HashMap;
import java.util.Map;

import play.Configuration;
import play.Logger;
import play.Play;

public class AssetConfiguration {
	
	private final String name;
	private final String uri;
	private final String[] dirs;
	private final boolean fallbackEnabled;
	
	public AssetConfiguration(String name, String uri, String[] dirs, boolean fallbackEnabled) {
		this.name = name;
		this.uri = uri;
		this.dirs = dirs;
		this.fallbackEnabled = fallbackEnabled;
	}
	
	public String getName() {
		return name;
	}

	public String getUri() {
		return uri;
	}

	public String[] getDirs() {
		return dirs;
	}
	
	public boolean isFallbackEnabled() {
		return fallbackEnabled;
	}
	
	public String getTargetDir() {
		if (this.dirs != null && this.dirs.length > 0) {
			return this.dirs[0];	// first entry
		} else {
			return null;
		}
	}

	static public Map<String,AssetConfiguration> createAssetConfigurations() {
		Configuration configuration = Play.application().configuration();
		
		// configs by name
		Map<String,AssetConfiguration> acs = new HashMap<String,AssetConfiguration>();
		
		Configuration assetConfigs = configuration.getConfig(Constants.ASSETS);
        if (assetConfigs != null) {
        	for (String assetConfigName : assetConfigs.subKeys()) {
        		String uri = configuration.getString(Constants.ASSETS + "." + assetConfigName + ".uri");
        		boolean fallback = configuration.getBoolean(Constants.ASSETS + "." + assetConfigName + ".fallback", true);
        		String dirsTemp = configuration.getString(Constants.ASSETS + "." + assetConfigName + ".dirs");
        		String[] dirs = dirsTemp.split(",");
        		for (int i = 0; i < dirs.length; i++) {
        			dirs[i] = dirs[i].trim();
        		}
        		AssetConfiguration ac = new AssetConfiguration(assetConfigName, uri, dirs, fallback);
        		acs.put(ac.getName(), ac);
        	}
        }
        
        // and "assets" entry for "optimized-assets" is SUPER important for OptimizedAssets to work
        if (!acs.containsKey(Constants.OPTIMIZED_ASSETS)) {
        	// create a default entry
        	Logger.warn("Configuration section key [" + Constants.ASSETS + "." + Constants.OPTIMIZED_ASSETS + "] does not exist; creating default suitable for dev");
        	AssetConfiguration ac = new AssetConfiguration(Constants.OPTIMIZED_ASSETS, "/assets/o", new String[] { "target/optimized-assets" }, true);
        	acs.put(ac.getName(), ac);
        }
        
        return acs;
	}
	
	static public Map<String,AssetConfiguration> toMapByUri(Map<String,AssetConfiguration> configs) {
		Map<String,AssetConfiguration> byUri = new HashMap<String,AssetConfiguration>();
		for (AssetConfiguration c : configs.values()) {
			byUri.put(c.getUri(), c);
		}
		return byUri;
	}
	
}