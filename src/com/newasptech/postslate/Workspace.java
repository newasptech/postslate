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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
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
	
	public void merge(String outputDir,	String container, boolean separate, boolean retainVideo,
			boolean retainAudio, boolean retainOther, float vshift,
			String vcodec, String acodec, ProgressMonitor m) throws Exception {
		try {
			List<AVPair> contents = contents();
			int i = 0;
			for (Iterator<AVPair> pPair = contents.iterator(); pPair.hasNext();) {
				AVPair pair = pPair.next();
				AVClip vClip = pair.video(), aClip = pair.audio();
				if (vClip == null || aClip == null) continue;
				if (m != null) {
					if (m.isCanceled()) throw new CancellationException();
					m.setNote(String.format("%s / %s", vClip.getName(), aClip.getName()));
					m.setProgress(Math.round(100.0f * i++ / contents.size()));
				}
				merge(vClip, aClip, vshift, container, retainVideo, retainAudio, retainOther,
						vcodec, acodec, outputDir);
			}
		}
		finally {
			if (m != null) m.close();
		}
	}
	
	// WIP: factor-out a MergeController class
	/** Perform a merge by re-packing video and audio streams from different
	 *  source files into a single output container. */
	private String merge(AVClip vFile, AVClip aFile, float vAdjustment, String outputContainer,
			boolean copyAllVideoStreams, boolean copyCameraAudioStream, boolean copyOtherStreams,
			String vcodec, String acodec, String outputDir) {
		AVEngine e = session.getAVEngine();
		float vClipLen = vFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue(),
				aClipLen = aFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue();
		TrimValues bounds = new TrimValues(vClipLen, vFile.getOffset(), vAdjustment, aClipLen, aFile.getOffset());
		String mergeFile = mergeTarget(vFile, outputContainer, outputDir);
		SortedSet<Integer> vIndexSet = streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO), !copyAllVideoStreams);
		SortedSet<Integer> aIndexSet = streamIndices(aFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), !copyAllVideoStreams);
		if (copyCameraAudioStream) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), false));
		}
		if (copyOtherStreams) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_DATA), false));
		}
		AVClip vClip = new AVClip(vFile, bounds.getVideoStart(), bounds.getDuration(), vIndexSet);
		AVClip aClip = new AVClip(aFile, bounds.getAudioStart(), bounds.getDuration(), aIndexSet);
		if (vcodec == null && acodec == null)
			e.repackage(vdir, vClip, adir, aClip, mergeFile);
		else
			e.transcode(vdir, vClip, adir, aClip, vcodec, acodec, mergeFile);
		return mergeFile;
	}
	
	/** Return the output filename for a merge operation */
	private static String mergeTarget(AVFileRef vFile, String outputContainer, String outputDir) {
		StringBuffer s = new StringBuffer(outputDir);
		s.append(System.getProperty("file.separator"));
		String name =vFile.getName(); 
		s.append(name.substring(0, 1 + name.lastIndexOf('.')));
		s.append(outputContainer);
		return s.toString();
	}
	
	private static SortedSet<Integer> streamIndices(AVFileRef avf, String key, String value, boolean firstOnly) {
		SortedSet<Integer> retval = new TreeSet<Integer>();
		if (firstOnly) {
			int i = avf.getMeta().findFirstIndex(key, value);
			retval.add(Integer.valueOf(i));
		}
		else {
			retval.addAll(avf.getMeta().findAllIndices(key, value));
		}
		return retval;
	}
	
	private AVClipNDir findAssociate(AVClipNDir cnd) {
		for (Iterator<AVPair> pp = contents().iterator(); pp.hasNext();) {
			AVPair p = pp.next();
			switch (cnd.dir.getType()) {
			case VIDEO:
				if (p.video() != null && 0 == p.video().compareTo(cnd.clip)) {
					return new AVClipNDir(p.audio(), getAudioDir());
				}
				break;
			case AUDIO:
				if (p.audio() != null && 0 == p.audio().compareTo(cnd.clip)) {
					return new AVClipNDir(p.video(), getVideoDir());
				}
				break;
			}
		}
		throw new NoSuchElementException();
	}
	
	private String getContainer(String c) {
		if (c != null && c.length() > 0)
			return c;
		return session.getConfig().getProperty(Config.MERGE_FORMAT);
	}

	// WIP: factor-out a ViewController class
	public static final String VIEW_CLAP = "clap", VIEW_FULL = "full",
			VIEW_VIDEO = "video", VIEW_AUDIO = "audio";
	public void view(File avFile, String target, float vshift, String container,
			int width, int height, int x, int y)
		throws Exception {
		AVClip vclip = null, aclip = null;
		AVClipNDir cd = findClip(avFile), associate = null;
		try {
			associate = findAssociate(cd);
			if (cd.dir.getType() == AVDirRef.Type.VIDEO) {
				vclip = cd.clip;
				vdir = cd.dir;
				aclip = associate.clip;
			}
			else {
				aclip = cd.clip;
				adir = cd.dir;
				vclip = associate.clip;
			}
		}
		catch(NoSuchElementException nsee) {}
		if (vclip == null) {
			target = VIEW_AUDIO;
			_l.log(Level.FINE, "There is no video, so the view target will be " + target);
		}
		else if (aclip == null) {
			target = VIEW_VIDEO;
			_l.log(Level.FINE, "There is no audio, so the view target will be " + target);
		}
		if (target.contentEquals(VIEW_CLAP)) {
			previewClap(vclip, aclip, vshift, container, width, height, x, y);
		}
		else if (target.contentEquals(VIEW_FULL)) {
			previewMerge(vclip, aclip, vshift, container, System.getProperty("java.io.tmpdir"),
					width, height, x, y);
		}
		else if (target.contentEquals(VIEW_VIDEO)) {
			AVClip playClip = new AVClip(vclip, 0.0f, AVClip.duration(vclip,
					AVEngine.MetaValue.CODEC_TYPE_VIDEO, session.getAVEngine()));
			showClip(getVideoDir(), playClip, width, height, x, y);
			
		}
		else if (target.contentEquals(VIEW_AUDIO)) {
			AVClip playClip = new AVClip(aclip, 0.0f, AVClip.duration(aclip,
					AVEngine.MetaValue.CODEC_TYPE_AUDIO, session.getAVEngine()));
			showClip(getAudioDir(), playClip, width, height, x, y);
		}
	}
	
	private void previewMerge(AVClip vFile, AVClip aFile, float vshift, String container,
			String workdir, int width, int height, int x, int y) throws IOException {
		boolean allVideo = false, allAudio = false, copyOther = false;
		String vcodec = null, acodec = null;
		String previewFilePath = merge(vFile, aFile, vshift, getContainer(container),
				allVideo, allAudio, copyOther, vcodec, acodec, workdir);
		File previewFile = new File(previewFilePath);
		TrimValues bounds = new TrimValues(AVClip.duration(vFile, AVEngine.MetaValue.CODEC_TYPE_VIDEO, session.getAVEngine()),
				vFile.getOffset(), vshift, AVClip.duration(aFile, AVEngine.MetaValue.CODEC_TYPE_AUDIO, session.getAVEngine()),
				aFile.getOffset());
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null), 0.0f, bounds.getDuration() + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip,
				width, height, x, y);
		previewFile.deleteOnExit();
	}
	
	private static float POST_VIDEO_PADDING = 0.5f;
	private void previewClap(AVClip vFile, AVClip aFile, float vshift, String container,
			int width, int height, int x, int y) throws IOException {
		// Normally, PRE_CLAP is a fixed value, but what if the clap comes less than
		// that interval after the start of the clip?  Adjust if needed.
		Config cfg = session.getConfig();
		AVEngine e = session.getAVEngine();
		float usePreClap = Math.min(Math.min(vFile.getOffset(),
				aFile.getOffset()), cfg.fvalue(Config.PRE_CLAP));
		AVClip vClip = new AVClip(vFile, vFile.getOffset() + vshift - usePreClap,
				usePreClap+cfg.fvalue(Config.POST_CLAP),
				new int[]{vFile.getMeta().findFirstIndex(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO))});
		AVClip aClip = new AVClip(aFile, aFile.getOffset() - usePreClap,
				usePreClap+cfg.fvalue(Config.POST_CLAP),
				new int[]{aFile.getMeta().findFirstIndex(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO))});
		File previewFile = File.createTempFile("preview", "." + getContainer(container));
		previewFile.deleteOnExit();
		e.repackage(vdir, vClip, adir, aClip, previewFile.toString());
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null),
				0.0f, usePreClap + cfg.fvalue(Config.POST_CLAP) + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip,
				width, height, x, y);
	}
	
	private void showClip(AVDirRef dir, AVClip clip, int width, int height, int x, int y) {
		session.getAVEngine().play(dir, clip, width, height, x, y);
	}
}
