/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class AVDirRef implements Serializable {
	public static enum Type {AUDIO, VIDEO};
	private static final long serialVersionUID = 1L;
	private Type type;
	private String dirPath;
	private String fileMask = null;
	private String matchDir = null; // points to the counterpart directory
	
	public AVDirRef(Type _type, String _dirPath, String _fileMask, String _matchDir) {
		type = _type;
		dirPath = _dirPath;
		fileMask = _fileMask;
		matchDir = _matchDir;
	}
	
	public String getPath() {
		return dirPath;
	}
	
	public String getFileMask() {
		return fileMask;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getMatchDir() {
		return matchDir;
	}
	
	public void save(Cache cache, boolean update) 
			throws FileNotFoundException, IOException, RefAlreadyExists {
		File ofile = new File(cache.indexForDirectory(getPath(), getType()));
		if (ofile.exists() && !update) {
			StringBuffer s = new StringBuffer("Directory ");
			s.append(getPath());
			s.append(" is already matched. Specify --update to re-match this directory with ");
			s.append(getMatchDir());
			throw new RefAlreadyExists(s.toString());
		}
		ObjectOutputStream ostr = new ObjectOutputStream(new FileOutputStream(ofile));
		ostr.writeObject(this);
		ostr.close();
	}
	
	public class RefAlreadyExists extends RuntimeException {
    	private static final long serialVersionUID = 1L;
    	RefAlreadyExists(String msg) {
			super(msg);
		}
	}
	
	/** Given the path to a directory, and the type of file sought, load and return the appropriate directory reference. */
	public static AVDirRef loadFor(String dirPath, Type type, Cache cache)
			throws ClassNotFoundException, FileNotFoundException, IOException {
		ObjectInputStream istr = new ObjectInputStream(new FileInputStream(cache.indexForDirectory(dirPath, type)));
		Object o = istr.readObject();
		istr.close();
		return (AVDirRef)o;
	}
	
	/** Given the path to a file or directory, load both Audio and Video AVDirRef objects that apply. */
	public static final int VIDX = 0;
	public static final int AIDX = 1;
	public static AVDirRef[] dirsFor(String path, Cache cache)
			throws ClassNotFoundException, FileNotFoundException, IOException {
		File f = new File(path);
		String dirPath = null;
		if (f.isDirectory())
			dirPath = path;
		else if (f.isFile())
			dirPath = f.getParent();
		if (dirPath == null)
			dirPath = ".";
		AVDirRef[] refs = new AVDirRef[]{null, null};
		try {
			refs[VIDX] = loadFor(dirPath, Type.VIDEO, cache);
			refs[AIDX] = loadFor(refs[VIDX].getMatchDir(), Type.AUDIO, cache);
		}
		catch (FileNotFoundException fnf) {
			refs[AIDX] = loadFor(dirPath, Type.AUDIO, cache);
			refs[VIDX] = loadFor(refs[AIDX].getMatchDir(), Type.VIDEO, cache);
		}
		return refs;
	}
}
