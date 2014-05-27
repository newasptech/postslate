/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;

public class Cache {
	public static final String INDEX_EXT = "idx";
	private File basedir = null;
	
	public Cache(String _basedir) {
		basedir = location(_basedir);
	}
	
	public static File location(String _basedir) {
		if (_basedir != null)
			return new File(_basedir);
		String basedir_s = System.getenv("POSTSLATE_CACHE_DIR");
		if (basedir_s == null) {
			StringBuffer b = new StringBuffer(System.getProperty("user.home"));
			b.append("/.postslate");
			basedir_s = b.toString();
		}
		return new File(basedir_s.toString());
	}
	
	private static File getUsableDirectory(File path) {
		if (!path.exists()) {
			path.mkdirs();
		}
		if (!path.isDirectory()) {
			throw new RuntimeException("Cache location " + path.toString() + " is not a directory.");
		}
		if (!(path.canRead() && path.canWrite() && path.canExecute())) {
			throw new RuntimeException("Permissions are set incorrectly for cache location " + path.toString());
		}
		return path;
	}
	
	private static String getUsableDirectory(String path) {
		return getUsableDirectory(new File(path)).toString();
	}
	
	public String getBasedir() {
		return getUsableDirectory(basedir).toString();
	}

	/** Given the path to a directory where A/V files are stored, return a
	 * path to a directory in the cache where that A/V directory and its
	 * file indexes are located. */
	public String forDirectory(String dir) {
		StringBuffer cdpath = new StringBuffer(getBasedir());
		cdpath.append(System.getProperty("file.separator"));
		cdpath.append((new File(dir)).getAbsolutePath().replace(System.getProperty("file.separator").charAt(0), '_'));
		return getUsableDirectory(cdpath.toString());
	}
	
	/** Given a directory path where A/V files are stored, return a path to
	 *  the cache file where an index for that directory will be stored. */
	public String indexForDirectory(String avdir, AVDirRef.Type type) {
		StringBuffer path = new StringBuffer(forDirectory(avdir));
		path.append("/dir.");
		path.append(type.toString());
		path.append(".");
		path.append(INDEX_EXT);
		return path.toString();
	}
	
	/** Given the path to an A/V file, return a path to the location in the
	 * cache of the index for that file. */
	public String indexForFiles(String avdir, AVDirRef.Type type) {
		StringBuffer path = new StringBuffer(forDirectory(avdir));
		path.append("/file.");
		path.append(type.toString());
		path.append(".");
		path.append(INDEX_EXT);
		return path.toString();
	}
	
	public String indexForMatches(String avdir) {
		StringBuffer path = new StringBuffer(forDirectory(avdir));
		path.append("/match.");
		path.append(INDEX_EXT);
		return path.toString();
	}
}
