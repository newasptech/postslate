/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Simple implementation of a file-glob matcher that checks only filenames,
 *  not directory paths. */
public class SimpleGlobFilter implements FilenameFilter {
	private List<Pattern> regexes = new LinkedList<Pattern>();
	
	/** Given a filename glob pattern, return an equivalent regex */
	public static String glob2Regex(String glob) {
		String r = glob;
		r = r.replace(".", "\\.");
		r = r.replace("?", ".");
		r = r.replace("*", ".*");
		return r;
	}

	/** Initialize with a space-delimited list of wildcard expressions */
	public SimpleGlobFilter(String wildcardExpr, boolean matchCase) {
		for (String wildcard : wildcardExpr.split("\\s+")) {
			String re = glob2Regex(wildcard);
			Pattern p;
			if (matchCase)
				p = Pattern.compile(re);
			else
				p = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
			regexes.add(p);
		}
	}
	
	public boolean accept(File dir, String name) {
		boolean match = false;
		for (Iterator<Pattern> p = regexes.iterator(); p.hasNext() && !match;) {
			match = p.next().matcher(name).matches();
		}
		return match;
	}

	/** Should return results comparable to "ls {path}/*.ext1 {path}/*.ext2 ..." */
	public static SortedSet<File> ls(String dirPath, String fileSpec, boolean matchCase) {
		SortedSet<File> files = new TreeSet<File>();
		File dir = new File(dirPath);
		FilenameFilter filter = new SimpleGlobFilter(fileSpec, matchCase);
		for (File f : dir.listFiles(filter)) {
			files.add(f);
		}
		return files;
	}
}