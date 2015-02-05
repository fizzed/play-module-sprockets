package com.fizzed.play.sprockets;

import junit.framework.Assert;

import org.junit.Test;

import com.fizzed.play.sprockets.AssetFile;

public class AssetFileTest {

	@Test
	public void nameExt() throws Exception {
		AssetFile af = new AssetFile("stylesheets/app.css");
		Assert.assertEquals("stylesheets", af.getPath());
		Assert.assertEquals("app", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertFalse(af.isBundle());
		Assert.assertFalse(af.isMinified());
		Assert.assertFalse(af.isFingerprinted());
		Assert.assertEquals("app.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.css", af.toRelativeUri());
	}
	
	@Test
	public void nameMinifiedExt() throws Exception {
		AssetFile af = new AssetFile("stylesheets/app.min.css");
		Assert.assertEquals("stylesheets", af.getPath());
		Assert.assertEquals("app", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertTrue(af.isMinified());
		Assert.assertFalse(af.isBundle());
		Assert.assertFalse(af.isFingerprinted());
		Assert.assertEquals("app.min.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.min.css", af.toRelativeUri());
	}
	
	@Test
	public void nameBundleMinifiedExt() throws Exception {	
		AssetFile af = new AssetFile("stylesheets/app.bundle.min.css");
		Assert.assertEquals("stylesheets", af.getPath());
		Assert.assertEquals("app", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertTrue(af.isMinified());
		Assert.assertTrue(af.isBundle());
		Assert.assertFalse(af.isFingerprinted());
		Assert.assertEquals("app.bundle.min.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.bundle.min.css", af.toRelativeUri());
	}
	
	@Test
	public void nameBundleMinifiedFingerprintedExt() throws Exception {	
		AssetFile af = new AssetFile("stylesheets/app.bundle.min.04df1c6031020ea2a11f2fa6254b4b2d.css");
		Assert.assertTrue(af.isFingerprinted());
		Assert.assertEquals("04df1c6031020ea2a11f2fa6254b4b2d", af.getFingerprint());
		Assert.assertEquals("stylesheets", af.getPath());
		Assert.assertEquals("app", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertTrue(af.isMinified());
		Assert.assertTrue(af.isBundle());
		Assert.assertEquals("app.bundle.min.04df1c6031020ea2a11f2fa6254b4b2d.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.bundle.min.04df1c6031020ea2a11f2fa6254b4b2d.css", af.toRelativeUri());
	}
	
	@Test
	public void nameBundleMinifiedInvalidFingerprintExt() throws Exception {	
		AssetFile af = new AssetFile("stylesheets/app.bundle.min.AEGEABE.css");
		// the AEGEABE is not a valid fingerprint so it'll become part of the name
		Assert.assertEquals("stylesheets", af.getPath());
		Assert.assertEquals("app.AEGEABE", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertTrue(af.isMinified());
		Assert.assertTrue(af.isBundle());
		Assert.assertFalse(af.isFingerprinted());
		Assert.assertEquals("app.AEGEABE.bundle.min.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.AEGEABE.bundle.min.css", af.toRelativeUri());
	}
	
	@Test
	public void nameExtWithNoDir() throws Exception {
		AssetFile af = new AssetFile("app.css");
		Assert.assertEquals("", af.getPath());
		Assert.assertEquals("app", af.getName());
		Assert.assertEquals("css", af.getExtension());
		Assert.assertFalse(af.isBundle());
		Assert.assertFalse(af.isMinified());
		Assert.assertFalse(af.isFingerprinted());
		Assert.assertEquals("app.css", af.toFileName());
		Assert.assertEquals("stylesheets/app.css", af.toRelativeUri());
	}
	
}
