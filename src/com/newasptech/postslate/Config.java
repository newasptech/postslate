/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class Config extends Properties {
	private static final long serialVersionUID = 1L;
	public static final String DEBUG = "com.newasptech.postslate.debug";
	public static final String DEBUG_DEF = "false";
	public static final String FILESPEC_AUDIO = "com.newasptech.postslate.filespec_audio";
	public static final String FILESPEC_AUDIO_DEF = "*.wav";
	public static final String FILESPEC_VIDEO = "com.newasptech.postslate.filespec_video";
	public static final String FILESPEC_VIDEO_DEF = "*.avi *.dv *.h264 *.mp4 *.mpeg* *.mpg* *.mov *.mts";
	public static final String FILESPEC_IS_CASE_SENSITIVE = "com.newasptech.postslate.filespec_case_sensitive";
	private static final String FILESPEC_IS_CASE_SENSITIVE_DEF = "false";
	public static final String MERGE_FORMAT = "com.newasptech.postslate.merge_format";
	private static final String MERGE_FORMAT_DEF = "mov";
	public static final String PREVIEW_WIDTH = "com.newasptech.postslate.preview_width";
	public static final String PREVIEW_HEIGHT = "com.newasptech.postslate.preview_height";
	private static final String PREVIEW_WIDTH_DEF = "800", PREVIEW_HEIGHT_DEF = "450";
	public static final String PRE_CLAP = "com.newasptech.postslate.preview_pre_clap_secs";
	public static final String POST_CLAP = "com.newasptech.postslate.preview_post_clap_secs";
	private static final String PRE_CLAP_DEF = "3.0", POST_CLAP_DEF = "1.0";
	public static final String SCAN_EVENTS = "com.newasptech.postslate.scan_events";
	private static final String SCAN_EVENTS_DEF = "100";
	public static final String SEARCH_PATH = "com.newasptech.postslate.search_path";
	private static final String SEARCH_PATH_DEF = "";
	public static final String QUANTIZE_FACTOR = "com.newasptech.postslate.qfactor";
	private static final String QUANTIZE_FACTOR_DEF = "2";
	public static final String TYPICAL_CLAP_DURATION = "com.newasptech.postslate.typical_clap_duration";
	private static final String TYPICAL_CLAP_DURATION_DEF = "0.005";
	
	public static String comments() {
		StringBuffer c = new StringBuffer("This file contains parameter settings for postslate.\nIf a parameter is not set in this file, its default value (shown) is used.\n\n");
		ac(c, DEBUG, DEBUG_DEF, "Enable debug output");
		ac(c, FILESPEC_AUDIO, FILESPEC_AUDIO_DEF, "Wildcard patterns for audio file names");
		ac(c, FILESPEC_VIDEO, FILESPEC_VIDEO_DEF, "Wildcard patterns for video file names");
		ac(c, FILESPEC_IS_CASE_SENSITIVE, FILESPEC_IS_CASE_SENSITIVE_DEF, "If true, match files against the wildcard patterns exactly as written, taking upper/lower case into account. If false, allow matches against different upper/lower case combinations of the same letters.");
		ac(c, MERGE_FORMAT, MERGE_FORMAT_DEF, "File container to use for merged clips");
		ac(c, PREVIEW_HEIGHT, PREVIEW_HEIGHT_DEF, "Height in pixels of the preview window.");
		ac(c, PREVIEW_WIDTH, PREVIEW_WIDTH_DEF, "Width in pixels of the preview window.");
		ac(c, PRE_CLAP, PRE_CLAP_DEF, "When previewing the clap, how many seconds of footage should be shown prior to the clap?");
		ac(c, POST_CLAP, POST_CLAP_DEF, "When previewing the clap, how many seconds of footage should be shown following the clap?");
		ac(c, SCAN_EVENTS, SCAN_EVENTS_DEF, "When scanning an audio stream for possible claps, how many events (i.e., clap candidates) should be collected? (You must re-scan a directory after changing this parameter for the new value to take effect.)");
		ac(c, SEARCH_PATH, SEARCH_PATH_DEF, "Extra paths to search for executables; will be prepended to the PATH environment variable value.  Use the platform-appropriate delimiter (';' for Windows, ':' for Linux/Mac/UNIX) to separate items.");
		ac(c, QUANTIZE_FACTOR, QUANTIZE_FACTOR_DEF, "When scanning for claps, 'quantize' the results into chunks of this size, which represents a multiple of the typical clap duration.  In other words, if a typical clap lasts 0.005 seconds and the quantize factor is set to 2, then each 0.01-second interval of audio can contain at most one clap candidate.");
		ac(c, TYPICAL_CLAP_DURATION, TYPICAL_CLAP_DURATION_DEF, "How long, in seconds, does a typical clap last? (You must re-scan a directory after changing this parameter for the new value to take effect.)");
		return c.toString();
	}
	
	private static void ac(StringBuffer c, String p, String defValue, String desc) {
		c.append(String.format("\t%s\n%s=%s\n\n", desc, p, defValue));
	}
	
	public static Properties getDefaults() {
		Properties p = new Properties();
		p.setProperty(DEBUG, DEBUG_DEF);
		p.setProperty(FILESPEC_AUDIO, FILESPEC_AUDIO_DEF);
		p.setProperty(FILESPEC_VIDEO, FILESPEC_VIDEO_DEF);
		p.setProperty(FILESPEC_IS_CASE_SENSITIVE, FILESPEC_IS_CASE_SENSITIVE_DEF);
		p.setProperty(MERGE_FORMAT, MERGE_FORMAT_DEF);
		p.setProperty(PREVIEW_WIDTH, PREVIEW_WIDTH_DEF);
		p.setProperty(PREVIEW_HEIGHT, PREVIEW_HEIGHT_DEF);
		p.setProperty(PRE_CLAP, PRE_CLAP_DEF);
		p.setProperty(POST_CLAP, POST_CLAP_DEF);
		p.setProperty(SCAN_EVENTS, SCAN_EVENTS_DEF);
		p.setProperty(SEARCH_PATH, SEARCH_PATH_DEF);
		p.setProperty(QUANTIZE_FACTOR, QUANTIZE_FACTOR_DEF);
		p.setProperty(TYPICAL_CLAP_DURATION, TYPICAL_CLAP_DURATION_DEF);
		return p;
	}

	public Config() {
		super(getDefaults());
	}
	
	public Config(String cacheBaseDir)
			throws FileNotFoundException, IOException  {
		super(getDefaults());
		File f = new File(saveloc(cacheBaseDir));
		if (f.exists())
			load(cacheBaseDir);
		else {
			store(cacheBaseDir);
		}
	}
	
	public String saveloc(String cacheBaseDir) {
		StringBuffer p = new StringBuffer(Cache.location(cacheBaseDir).getPath());
		p.append(System.getProperty("file.separator"));
		p.append("postslate.properties");
		return p.toString();
	}
	
	public void load(String cacheBaseDir)
			throws FileNotFoundException, IOException {
		InputStream istr = null;
		try {
			istr = new FileInputStream(saveloc(cacheBaseDir));
			load(istr);
		}
		finally {
			if (istr != null)
				istr.close();
		}
	}
	
	public void store(String cacheBaseDir)
			throws FileNotFoundException, IOException {
		OutputStream ostr = null;
		try {
			ostr = new FileOutputStream(saveloc(cacheBaseDir));
			store(ostr, comments());
		}
		finally {
			if (ostr != null)
				ostr.close();
		}
	}
	
	public boolean bvalue(String property) {
		return Boolean.valueOf(getProperty(property));
	}
	
	public short svalue(String property) {
		return Short.valueOf(getProperty(property));
	}

	public int ivalue(String property) {
		return Integer.valueOf(getProperty(property));
	}
	
	public float fvalue(String property) {
		return Float.valueOf(getProperty(property));
	}
}