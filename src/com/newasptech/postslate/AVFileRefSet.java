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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import javax.swing.ProgressMonitor;
import com.newasptech.postslate.audio.ClapFinder;
import com.newasptech.postslate.audio.FileReader;
import com.newasptech.postslate.audio.FileReaderWAV;
import com.newasptech.postslate.audio.wave.WaveStreamReader;
import com.newasptech.postslate.util.SimpleGlobFilter;

public class AVFileRefSet {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.WaveStreamReader");
	private Cache cache;
	private AVDirRef dir;
	private AVEngine avEngine;
	private short maxEvents;
	private short quantizeFactor;
	private float typicalClapDuration;
	private List<AVFileRef> fileRefs = null;
	
	public AVFileRefSet(AVDirRef _dir, boolean update, Config cfg, Cache _cache,
			AVEngine _e, ProgressMonitor monitor)
			throws FileNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		cache = _cache;
		dir = _dir;
		avEngine = _e;
		maxEvents = cfg.svalue(Config.SCAN_EVENTS);
		quantizeFactor = cfg.svalue(Config.QUANTIZE_FACTOR);
		typicalClapDuration = cfg.fvalue(Config.TYPICAL_CLAP_DURATION);
		if (!(indexFile().exists()) || update) {
			scan(cfg.bvalue(Config.FILESPEC_IS_CASE_SENSITIVE), cfg, monitor);
			save();
		}
		else {
			load();
		}
	}
	
	public List<AVFileRef> getFileRefs() {
		return Collections.unmodifiableList(fileRefs);
	}
	
	public AVFileRef findByName(String name) {
		for (Iterator<AVFileRef> pRef = fileRefs.iterator(); pRef.hasNext();) {
			AVFileRef ref = pRef.next();
			if (ref.getName().contentEquals(name))
				return ref;
		}
		throw new NoSuchElementException();
	}
	
	private File saveloc() {
		return new File(cache.indexForFiles(dir.getPath(), dir.getType()));
	}
	
	private void save() throws FileNotFoundException, IOException {
		AVFileRef[] fileRefsArray = new AVFileRef[]{};
		if (fileRefs != null) {
			fileRefsArray = fileRefs.toArray(new AVFileRef[]{});
		}
		ObjectOutputStream ostr = new ObjectOutputStream(new FileOutputStream(saveloc()));
		ostr.writeObject(fileRefsArray);
		ostr.close();
	}
	
	private void load() throws FileNotFoundException, IOException {
		ObjectInputStream istr = new ObjectInputStream(new FileInputStream(saveloc()));
		try {
			AVFileRef[] fileRefsArray = (AVFileRef[])istr.readObject();
			fileRefs = new LinkedList<AVFileRef>();
			for (AVFileRef r : fileRefsArray)
				fileRefs.add(r);
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace(System.err);
			System.exit(5);
		}
		istr.close();
	}
	
	private File indexFile() {
		return new File(cache.indexForFiles(dir.getPath(), dir.getType()));
	}
	
	private FileReader wavReader(AVFileRef fref, Config cfg)
		throws FileNotFoundException, IOException, WaveStreamReader.NotWave {
		FileReader audioReader = null;
		if (dir.getType() == AVDirRef.Type.AUDIO) {
			try {
				audioReader = new FileReaderWAV(avMakeFile(dir, fref));
			}
			catch(WaveStreamReader.NotWave nw) {}
		}
		if (audioReader != null) return audioReader;
		File wav = File.createTempFile("pslate", ".wav");
		wav.deleteOnExit();
		AVClip sourceClip = new AVClip(fref, 0.0f, -1.0f,
				new int[]{fref.getMeta().findFirstIndex(avEngine.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				avEngine.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO))});
		avEngine.transcode(null, null, dir, sourceClip, null, null, wav.toString());
		audioReader = new FileReaderWAV(wav);
		return audioReader;
	}

	/** Scan the directory and create an index of matching files */
	private void scan(boolean caseSensitiveFilemask, Config cfg, ProgressMonitor m)
			throws FileNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		try {
			List<File> files = new LinkedList<File>(SimpleGlobFilter.ls(dir.getPath(), dir.getFileMask(), caseSensitiveFilemask));
			List<AVFileRef> newFileRefs = new LinkedList<AVFileRef>();
			ClapFinder cf = new ClapFinder(typicalClapDuration);
			int completed = 0;
			for (Iterator<File> pF = files.iterator(); pF.hasNext();) {
				File f = pF.next();
				String fPath = dir.getPath() + System.getProperty("file.separator") + f.getName();
				_l.log(Level.FINE, "Inspecting " + fPath);
				if (m != null) {
					if (m.isCanceled()) throw new CancellationException();
					m.setNote(fPath);
					m.setProgress(Math.round(100.0f * completed++ / files.size()));
				}
				AVFileRef fref = new AVFileRef(f, avEngine.streamMeta(f));
				if (fileRefs != null && fileRefs.contains(fref)) {
					newFileRefs.add(fileRefs.get(fileRefs.indexOf(fref)));
				}
				else {
					fref.setEvents(cf.findClaps(wavReader(fref, cfg), maxEvents, quantizeFactor));
					newFileRefs.add(fref);
				}
			}
			fileRefs = newFileRefs;
		}
		finally {
			if (m != null) m.close();
		}
	}
	
	private static File avMakeFile(AVDirRef d, AVFileRef f) {
		StringBuffer s = new StringBuffer(d.getPath());
		s.append(System.getProperty("file.separator"));
		s.append(f.getName());
		return new File(s.toString());
	}
}
