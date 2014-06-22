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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;
import javax.swing.ProgressMonitor;
import com.newasptech.postslate.audio.wave.WaveStreamReader;

/** A Workspace represents a collection of audio files plus associated video.
 *  Usually, audio and video are stored in separate directories, and they are stored as
 *  separate entities, but they can point to the same directory.. */
public class Workspace implements Comparable<Workspace> {
	private static final Logger _l = Logger.getLogger("com.newasptech.postslate.Workspace");
	private AVDirRef vdir, adir;
	private AVFileRefSet vfiles = null, afiles = null;
	private MatchBox matches = null;
	private Session session = null;
	
	/** Constructor to create or update a new Workspace. */
	public Workspace(AVDirRef _vdir, AVDirRef _adir, boolean forceUpdate,
			Session _session, ProgressMonitor monitor)
		throws FileNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		vdir = _vdir;
		adir = _adir;
		session = _session;
		initFiles(forceUpdate, monitor);
		loadMatches();
	}
	
	/** Constructor to initialize a Workspace by loading one previously created. */
	public Workspace(String pathToDirOrFile, Session _session, ProgressMonitor monitor)
			throws ClassNotFoundException, FileNotFoundException, InterruptedException,
			IOException, WaveStreamReader.NotWave {
		AVDirRef[] dref = AVDirRef.dirsFor(pathToDirOrFile, _session.getCache());
		vdir = dref[AVDirRef.VIDX];
		adir = dref[AVDirRef.AIDX];
		session = _session;
		initFiles(false, monitor);
		loadMatches();
	}
	
	public Session getSession() {
		return session;
	}
	
	public AVDirRef getVideoDir() {
		return vdir;
	}
	
	public AVDirRef getAudioDir() {
		return adir;
	}
	
	public AVFileRef findAVFile(AVDirRef dir, String filename) {
		return ((dir.getType() == AVDirRef.Type.VIDEO) ? vfiles : afiles).findByName(filename); 
	}
	
	/** Given a File object that points to a file or file symlink, return the
	 * audio/video directory reference corresponding to that file; or throw an
	 * exception if it matches neither. */
	public AVDirRef findAVDir(File ff) throws FileNotInWorkspace, IOException {
		File cf = ff.getParentFile();
		if ((new File(vdir.getPath()).getCanonicalPath().contentEquals(cf.getCanonicalPath()))) {
			try {
				vfiles.findByName(ff.getName());
				return vdir;
			}
			catch(NoSuchElementException nse) {}
		}
		if ((new File(adir.getPath()).getCanonicalPath().contentEquals(cf.getCanonicalPath()))) {
			try {
				afiles.findByName(ff.getName());
				return adir;
			}
			catch(NoSuchElementException nse) {}
		}
		throw new FileNotInWorkspace(ff.getPath());
	}
	
	public MatchBox getMatchBox() {
		return matches;
	}
	
	private void initFiles(boolean forceUpdate, ProgressMonitor monitor)
			throws FileNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		vfiles = new AVFileRefSet(vdir, forceUpdate, session, monitor);
		afiles = new AVFileRefSet(adir, forceUpdate, session, monitor);
	}
	
	private File matchloc() {
		return new File(session.getCache().indexForMatches(vdir.getPath()));
	}
	
	public void saveMatches() throws FileNotFoundException, IOException {
		File fMatchloc = matchloc(), newMatchloc = new File(matchloc().getPath() + ".new"),
				oldMatchloc = new File(matchloc().getPath() + ".old");
		if (oldMatchloc.exists() && !oldMatchloc.delete())
			_l.log(Level.WARNING, "File " + oldMatchloc.getPath() + " was not deleted successfully");
		ObjectOutputStream ostr = new ObjectOutputStream(new FileOutputStream(newMatchloc));
		ostr.writeObject(matches);
		ostr.close();
		fMatchloc.renameTo(oldMatchloc);
		newMatchloc.renameTo(fMatchloc);
	}
	
	private void loadMatches() throws IOException {
		ObjectInputStream istr = null;
		try {
			istr = new ObjectInputStream(new FileInputStream(matchloc()));
			matches = (MatchBox)istr.readObject();
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace(System.err);
			System.exit(5);
		}
		catch(FileNotFoundException fe) {
			matches = new MatchBox();
		}
		if (istr != null)
			istr.close();
	}
	
	/** Return the clips from the two directories, sorted, as either
	 *  matched pairs or stag (i.e., matched with null). */
	public List<AVPair> contents() {
		List<AVPair> clist = new LinkedList<AVPair>();
		AVFileRef v = null, a = null;
		Iterator<AVFileRef> pV = vfiles.getFileRefs().iterator(),
				pA = afiles.getFileRefs().iterator();
		while (pV.hasNext() || pA.hasNext() || v != null || a != null) {
			if (v == null && pV.hasNext()) v = pV.next();
			if (a == null && pA.hasNext()) a = pA.next();
			if (v != null && a != null) {
				AVClip vclip = videoClip(v), aclip = audioClip(a);
				if (matches.isStagVideo(vclip)) {
					clist.add(new AVPair(vclip, null));
					v = null;
				}
				else if (matches.isStagAudio(aclip)) {
					clist.add(new AVPair(null, aclip));
					a = null;
				}
				else if (matches.hasMatchForVideo(vclip)) {
					AVClip aclipM = matches.matchForVideo(vclip);
					int c = aclip.compareTo(aclipM);
					if (c == 0) {
						clist.add(new AVPair(matches.getMatchedVideo(vclip), aclipM));
						v = null; a = null;
					}
					else if (c < 0) {
						clist.add(new AVPair(null, aclip));
						a = null;
					}
					else
						assert(false); // shouldn't happen
				}
				else if (matches.hasMatchForAudio(aclip)) {
					AVClip vclipM = matches.matchForAudio(aclip);
					int c = vclip.compareTo(vclipM);
					if (c == 0) {
						clist.add(new AVPair(vclipM, matches.getMatchedAudio(aclip)));
						a = null; v = null;
					}
					else if (c < 0) {
						clist.add(new AVPair(vclip, null));
						v = null;
					}
				}
				else { // implied match
					clist.add(new AVPair(videoClip(v), audioClip(a)));
					v = null; a = null;
				}
			}
			else if (v != null && a == null) {
				clist.add(new AVPair(videoClip(v), null));
				v = null;
			}
			else if (a != null && v == null) {
				clist.add(new AVPair(null, audioClip(a)));
				a = null;
			}
		}
		return clist;
	}
	
	/** Given the path to an audio or video file, return the AVClip representing
	 * that file in the workspace, plus its AVDirRef directory reference. */
	public AVClipNDir findClip(File f) throws FileNotInWorkspace, IOException {
		AVDirRef dir = findAVDir(f);
		AVFileRef avFile = findAVFile(dir, f.getName());
		AVClip clip = null;
		if (dir.getType() == AVDirRef.Type.VIDEO) {
			clip = videoClip(avFile);
		}
		else {
			clip = audioClip(avFile);
		}
		return new AVClipNDir(clip, dir);
	}
	
	private AVClip videoClip(AVFileRef vfile) {
		AVClip vclip = new AVClip(vfile, vfile.getEvents()[0].getTime());
		if (matches.hasMatchForVideo(vclip))
			return matches.getMatchedVideo(vclip);
		return vclip;
	}
	
	private AVClip audioClip(AVFileRef afile) {
		AVClip aclip = new AVClip(afile, afile.getEvents()[0].getTime());
		if (matches.hasMatchForAudio(aclip))
			return matches.getMatchedAudio(aclip);
		return aclip;
	}
	
	public int compareTo(Workspace rhs) {
		return getVideoDir().getPath().compareTo(rhs.getVideoDir().getPath());
	}
	
	public class FileNotInWorkspace extends Exception {
		private static final long serialVersionUID = 1L;
		public FileNotInWorkspace(String msg) {
			super(msg);
		}
	}
	
	public void matchFiles(File vfile, float vpos, File afile, float apos)
			throws Exception {
		assert(vfile.isFile());
		assert(afile.isFile());
		AVClip vclip = new AVClip(findAVFile(findAVDir(vfile), vfile.getName()), vpos),
				aclip = new AVClip(findAVFile(findAVDir(afile), afile.getName()), apos);
		getMatchBox().addMatch(vclip, aclip);
	}
	
	public void unmatch(File vfile, File afile)
		throws Exception {
		AVClipNDir vcd = findClip(vfile);
		AVClipNDir acd = findClip(afile);
		if (0 != acd.clip.compareTo(getMatchBox().getMatchedAudio(vcd.clip))) {
			StringBuffer s = new StringBuffer(vfile.getPath());
			s.append(" is not matched with ");
			s.append(afile.getPath());
			throw new RuntimeException(s.toString());
		}
	}
}
