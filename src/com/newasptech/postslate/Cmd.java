/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.text.DecimalFormat;
import javax.swing.ProgressMonitor;

import com.newasptech.postslate.AVClipNDir;
import com.newasptech.postslate.Workspace.AVPair;
import com.newasptech.postslate.util.Subprocess;
import com.newasptech.postslate.util.Text;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class Cmd {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.Cmd");

	private static final DecimalFormat TIME_FORMAT = new DecimalFormat("###,##0.000000");
	
	public static void initLogging(Config cfg) {
		Level logLevel = cfg.bvalue(Config.DEBUG) ? Level.FINE : Level.WARNING;
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(logLevel);
		Logger l = Logger.getLogger("com.newasptech.postslate");
		l.setLevel(logLevel);
		l.addHandler(ch);
		l.setUseParentHandlers(false);
	}
	
	public static Workspace matchDirs(Cache cache, String vdir, String adir, String vspec,
			String aspec, boolean update, Config cfg, ProgressMonitor m) throws Exception {
		AVDirRef vdref = new AVDirRef(AVDirRef.Type.VIDEO, vdir, vspec, adir);
		AVDirRef adref = new AVDirRef(AVDirRef.Type.AUDIO, adir, aspec, vdir);
		vdref.save(cache, update);
		adref.save(cache, update);
		return new Workspace(vdref, adref, true, cfg, cache, newAVEngine(cfg), m);
	}
	
	public static void matchFiles(Cache cache, String vpath, float vpos, String apath,
			float apos, Config cfg) throws Exception {
		Workspace wksp = new Workspace(vpath, cfg, cache, newAVEngine(cfg), null);
		File vfile = new File(vpath), afile = new File(apath);
		AVDirRef vdref = wksp.findAVDir(vfile);
		AVDirRef adref = wksp.findAVDir(afile);
		AVClip vclip = new AVClip(wksp.findAVFile(vdref, vfile.getName()), vpos),
				aclip = new AVClip(wksp.findAVFile(adref, afile.getName()), apos);
		wksp.getMatchBox().addMatch(vclip, aclip);
	}
	
	public static void unmatch(Cache cache, String vpath, String apath, Config cfg)
		throws Exception {
		Workspace wksp = new Workspace(vpath, cfg, cache, newAVEngine(cfg), null);
		AVClipNDir vcd = wksp.findClip(new File(vpath));
		AVClipNDir acd = wksp.findClip(new File(apath));
		if (0 != acd.clip.compareTo( wksp.getMatchBox().getMatchedAudio(vcd.clip))) {
			StringBuffer s = new StringBuffer(vpath);
			s.append(" is not matched with ");
			s.append(apath);
			throw new RuntimeException(s.toString());
		}
	}
	
	public static void list(Cache cache, String dir, String delim, Config cfg)
			throws Exception {
		Workspace wksp = new Workspace(dir, cfg, cache, newAVEngine(cfg), null);
		for (Iterator<Workspace.AVPair> pp = wksp.contents().iterator(); pp.hasNext();) {
			Workspace.AVPair p = pp.next();
			String[] parts = new String[]{"", "", "", ""};
			int i=0;
			if (p.video() != null) {
				parts[i++] = p.video().getName();
				parts[i++] = TIME_FORMAT.format(p.video().getOffset());
			}
			if (p.audio() != null) {
				parts[i++] = p.audio().getName();
				parts[i++] = TIME_FORMAT.format(p.audio().getOffset());
			}
			System.out.println(Text.join(parts, delim));
		}
	}
	
	public static void stag(Cache cache, List<String> filePaths, Config cfg)
		throws Exception {
		Map<Workspace, String[]> wfMap = arrangeByWorkspace(cache, filePaths, cfg);
		for (Workspace wksp : wfMap.keySet()) {
			for (String filePath : wfMap.get(wksp)) {
				AVClipNDir cd = wksp.findClip(new File(filePath));
				wksp.getMatchBox().remove(cd.clip);
				wksp.getMatchBox().addStag(cd.clip, cd.dir.getType());
			}
			wksp.saveMatches();
		}
	}
	
	public static void unstag(Cache cache, List<String> filePaths, Config cfg)
		throws Exception {
		Map<Workspace, String[]> wfMap = arrangeByWorkspace(cache, filePaths, cfg);
		for (Workspace wksp : wfMap.keySet()) {
			for (String filePath : wfMap.get(wksp)) {
				AVClipNDir cd = wksp.findClip(new File(filePath));
				if (wksp.getMatchBox().isStag(cd.clip, cd.dir.getType()))
					wksp.getMatchBox().remove(cd.clip);
			}
			wksp.saveMatches();
		}
	}
	
	private static Map<Workspace, String[]> arrangeByWorkspace(Cache cache,
			List<String> filePaths, Config cfg)
			throws Exception {
		Map<Workspace, String[]> wfMap = new TreeMap<Workspace, String[]>();
		String dir = null;
		Workspace wksp = null;
		List<String> wfList = null;
		Collections.sort(filePaths);
		for (String filePath : filePaths) {
			File file = new File(filePath);
			String fdir = file.getParentFile().getCanonicalPath();
			if (dir == null || !dir.equals(fdir)) {
				if (wksp != null) wfMap.put(wksp, wfList.toArray(new String[]{}));
				wksp = new Workspace(filePath, cfg, cache, newAVEngine(cfg), null);
				dir = fdir;
				wfList = new LinkedList<String>();
			}
			wfList.add(filePath);
		}
		if (wksp != null) wfMap.put(wksp, wfList.toArray(new String[]{}));
		return wfMap;
	}
	
	public static void merge(Cache cache, String vpath, String outputDir,
			String container, boolean separate, boolean retainVideo,
			boolean retainAudio, boolean retainOther, float vshift,
			String vcodec, String acodec, Config cfg, ProgressMonitor m) throws Exception {
		try {
			AVEngine avEngine = newAVEngine(cfg);
			Workspace wksp = new Workspace(vpath, cfg, cache, avEngine, null);
			AVDirRef vdir = wksp.getVideoDir(), adir = wksp.getAudioDir();
			List<AVPair> contents = wksp.contents();
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
				merge(vdir, vClip, adir, aClip, vshift, container, retainVideo, retainAudio,
						retainOther, vcodec, acodec, outputDir, avEngine, cfg);
			}
		}
		finally {
			if (m != null) m.close();
		}
	}
	
	/** Perform a merge by re-packing video and audio streams from different
	 *  source files into a single output container. */
	private static String merge(AVDirRef vdir, AVClip vFile, AVDirRef adir,
			AVClip aFile, float vAdjustment, String outputContainer,
			boolean copyAllVideoStreams, boolean copyCameraAudioStream,
			boolean copyOtherStreams, String vcodec, String acodec, String outputDir, AVEngine e, Config cfg) {
		float vClipLen = vFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue(),
				aClipLen = aFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue();
		float[] bounds = trimBoundaries(vClipLen, vFile.getOffset() + vAdjustment, aClipLen, aFile.getOffset());
		String mergeFile = mergeTarget(vFile, outputContainer, outputDir);
		SortedSet<Integer> vIndexSet = streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO), !copyAllVideoStreams);
		SortedSet<Integer> aIndexSet = streamIndices(aFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), !copyAllVideoStreams);
		if (copyCameraAudioStream) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), false));
		}
		if (copyOtherStreams) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_DATA), false));
		}
		AVClip vClip = new AVClip(vFile, bounds[0], bounds[2], vIndexSet);
		AVClip aClip = new AVClip(aFile, bounds[1], bounds[2], aIndexSet);
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
	
	public static float[] trimBoundaries(float vClipLen, float vClapPos, float aClipLen, float aClapPos) {
		float[] bounds = new float[3];
		float preClapLen = Math.min(vClapPos, aClapPos),
				postClapLen = Math.min(vClipLen - vClapPos, aClipLen - aClapPos);
		bounds[0] = vClapPos - preClapLen;
		bounds[1] = aClapPos - preClapLen;
		bounds[2] = preClapLen + postClapLen;
		return bounds;
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
	
	private static AVClipNDir findAssociate(AVClipNDir cnd, Workspace wksp) {
		for (Iterator<Workspace.AVPair> pp = wksp.contents().iterator(); pp.hasNext();) {
			Workspace.AVPair p = pp.next();
			switch (cnd.dir.getType()) {
			case VIDEO:
				if (p.video() != null && 0 == p.video().compareTo(cnd.clip)) {
					return new AVClipNDir(p.audio(), wksp.getAudioDir());
				}
				break;
			case AUDIO:
				if (p.audio() != null && 0 == p.audio().compareTo(cnd.clip)) {
					return new AVClipNDir(p.video(), wksp.getVideoDir());
				}
				break;
			}
		}
		throw new NoSuchElementException();
	}
	
	private static final String VIEW_CLAP = "clap", VIEW_FULL = "full",
			VIEW_VIDEO = "video", VIEW_AUDIO = "audio";
	public static void view(Cache cache, String filePath, String target, float vshift, String container, Config cfg)
		throws Exception {
		AVEngine avEngine = newAVEngine(cfg);
		Workspace wksp = new Workspace(filePath, cfg, cache, avEngine, null);
		AVDirRef vdir = null, adir = null;
		AVClip vclip = null, aclip = null;
		AVClipNDir cd = wksp.findClip(new File(filePath)), associate = null;
		try {
			associate = findAssociate(cd, wksp);
			if (cd.dir.getType() == AVDirRef.Type.VIDEO) {
				vclip = cd.clip;
				vdir = cd.dir;
				aclip = associate.clip;
				adir = associate.dir;
			}
			else {
				aclip = cd.clip;
				adir = cd.dir;
				vclip = associate.clip;
				vdir = associate.dir;
			}
		}
		catch(NoSuchElementException nsee) {}
		if (vclip == null)
			target = VIEW_AUDIO;
		else if (aclip == null)
			target = VIEW_VIDEO;
		if (target.contentEquals(VIEW_CLAP)) {
			previewClap(vdir, vclip, adir, aclip, vshift, container, avEngine, cfg);
		}
		else if (target.contentEquals(VIEW_FULL)) {
			previewMerge(vdir, vclip, adir, aclip, vshift, container, tmpdir(), avEngine, cfg);
		}
		else if (target.contentEquals(VIEW_VIDEO)) {
			AVClip playClip = new AVClip(vclip, 0.0f, AVClip.duration(vclip,
					AVEngine.MetaValue.CODEC_TYPE_VIDEO, avEngine));
			showClip(wksp.getVideoDir(), playClip, avEngine, cfg);
		}
		else if (target.contentEquals(VIEW_AUDIO)) {
			AVClip playClip = new AVClip(aclip, 0.0f, AVClip.duration(aclip,
					AVEngine.MetaValue.CODEC_TYPE_AUDIO, avEngine));
			showClip(wksp.getAudioDir(), playClip, avEngine, cfg);
		}
	}
	
	private static void showClip(AVDirRef dir, AVClip clip, AVEngine e, Config cfg) {
		String customViewerCmd = cfg.getProperty(Config.VIDEO_PLAY_CMD);
		if (customViewerCmd.length() > 0) {
			File f = new File(dir.getPath() + System.getProperty("file.separator") + clip.getName());
			String s = customViewerCmd.replace("%f", f.toString());
			s = s.replace("%H", cfg.getProperty(Config.PREVIEW_HEIGHT));
			s = s.replace("%W", cfg.getProperty(Config.PREVIEW_WIDTH));
			_l.log(Level.FINE, s);
			Subprocess p = new Subprocess(Text.tokenizeCommand(s), cfg.getProperty(Config.SEARCH_PATH));
			try {
				p.run(Subprocess.timeout(clip.getDuration()));
			}
			catch(IOException ioe) {}
			catch(InterruptedException ie) {}
			catch(Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		else {
			e.play(dir, clip, cfg.ivalue(Config.PREVIEW_WIDTH), cfg.ivalue(Config.PREVIEW_HEIGHT));
		}
	}
	
	private static String tmpdir() {
		return System.getProperty("java.io.tmpdir");
	}
	
	private static String getContainer(String c, Config cfg) {
		if (c != null && c.length() > 0)
			return c;
		return cfg.getProperty(Config.MERGE_FORMAT);
	}
	
	private static void previewMerge(AVDirRef vdir, AVClip vFile, AVDirRef adir, AVClip aFile,
			float vshift, String container, String workdir, AVEngine e, Config cfg) throws IOException {
		boolean allVideo = false, allAudio = false, copyOther = false;
		String vcodec = null, acodec = null;
		String previewFilePath = merge(vdir, vFile, adir, aFile, vshift,
				getContainer(container, cfg), allVideo, allAudio, copyOther, vcodec, acodec, workdir, e, cfg);
		File previewFile = new File(previewFilePath);
		float[] mergeBounds = trimBoundaries(AVClip.duration(vFile, AVEngine.MetaValue.CODEC_TYPE_VIDEO, e), vFile.getOffset(),
				AVClip.duration(aFile, AVEngine.MetaValue.CODEC_TYPE_AUDIO, e), aFile.getOffset());
		float mergedFileDuration = mergeBounds[2];
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null), 0.0f, mergedFileDuration + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip, e, cfg);
		previewFile.deleteOnExit();
	}
	
	private static float POST_VIDEO_PADDING = 0.5f;
	private static void previewClap(AVDirRef vdir, AVClip vFile, AVDirRef adir, AVClip aFile,
			float vshift, String container, AVEngine e, Config cfg) throws IOException {
		// Normally, PRE_CLAP is a fixed value, but what if the clap comes less than
		// that interval after the start of the clip?  Adjust if needed.
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
		File previewFile = File.createTempFile("preview", "." + getContainer(container, cfg));
		previewFile.deleteOnExit();
		e.repackage(vdir, vClip, adir, aClip, previewFile.toString());
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null),
				0.0f, usePreClap + cfg.fvalue(Config.POST_CLAP) + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip, e, cfg);
	}
		
	public static AVEngine newAVEngine(Config cfg) {
		return new AVEngineFFmpeg(cfg);
	}
	
	private static List<String> optArgs(String[] opts, int pos) {
		List<String> a = new LinkedList<String>();
		for (int i = pos; i != opts.length; ++i)
			a.add(opts[i]);
		return a;
	}
	
	public static void printHelpAndExit() {
		String h = "postslate - a tool to assist in synchronizing slate claps in a collection of video and audio clips\n" + 
				"\n" + 
				"Usage:\n" + 
				"  postslate command [options]\n" + 
				"\n" + 
				"General options:\n" + 
				"  --cache={dir}	use the named cache directory (defaults to ${userdir}/.postslate, may be overridden by environment variable POSTSLATE_CACHE_DIR)\n" + 
				"  --help	print this help\n" + 
				"\n" + 
				"Commands and their options:\n" + 
				"match - pair two directories or two files\n" + 
				"  match --vdir= [--vspec=] --adir= [--aspec] [--update]\n" + 
				"  match --vfile= --vpos= --afile= --apos=\n" + 
				"\n" + 
				"For safety's sake, the --update flag is required when either --vdir or --adir refers to a currently-matched directory.\n" + 
				"\n" + 
				"unmatch - remove a match previously created between two files\n" + 
				"  unmatch { --vfile= | --afile= }\n" + 
				"\n" + 
				"list - given either an audio or a video directory, list clips, showing both stag and matched files.\n" + 
				"  list --dir= [--delim=]\n" + 
				"stag - mark a file as not having any match\n" + 
				"  stag {file}\n" + 
				"\n" + 
				"unstag - remove a previous stag designation\n" + 
				"\n" + 
				"view - view media\n" + 
				"  view --target=" + Text.join(new String[]{VIEW_CLAP, VIEW_FULL, VIEW_VIDEO, VIEW_AUDIO}, "|") + " {file}\n" + 
				"\n" + 
				"merge - trim clips and merge\n" + 
				"  merge --output={dir} --container= [--separate] [--retain-video=default|all] [--retain-audio=none|all] [--retain-other=none|all] [--vshift=] [--vcodec=] [--acodec=]\n" + 
				"\n" + 
				"By default, the first video stream from the video clip and all audio streams from the audio clip are merged into a single file.\n" + 
				"No transcoding is done, but the streams are packed into a possibly different container.\n" + 
				"\n" + 
				"If --separate is used, video and audio are placed into separate, synchronized, equal-length files with the same filename stem and different extensions.\n" + 
				"If --retain-video=all is used, all video streams from the video clip are copied into the merge video target file.\n" + 
				"If --retain-audio=all is used, all audio streams from the video source clip are copied into the merge audio target, but placed after the audio streams from the source audio file.\n" + 
				"The --retain-other option controls whether non-video, non-audio streams from the video source file are copied verbtaim into the video target file.\n" + 
				"The --vshift= option specifies a time-shift value in seconds to be applied post-sync to video streams with respect to audio.  This value may be negative.\n" + 
				"The --vcodec option requests video transcoding.  The source bitrate is used for the target codec.\n" + 
				"The --acodec option requests audio transcoding.  The source bitrate is used for the target codec.\n" + 
				"";
		System.out.println(h);
	}
	
	public static final char OPT_AUDIO_CODEC = 'C';
	public static final char OPT_AUDIO_DIR = 'd';
	public static final char OPT_AUDIO_FILE = 'f';
	public static final char OPT_AUDIO_FILESPEC = 'w';
	public static final char OPT_AUDIO_POS = 'p';
	public static final char OPT_CACHE = 'c';
	public static final char OPT_CONTAINER = 'R';
	public static final char OPT_DELIM = 'L';
	public static final char OPT_HELP = 'h';
	public static final char OPT_LIST_DIR = 'l';
	public static final char OPT_OUTPUT_DIR = 'o';
	public static final char OPT_RETAIN_AUDIO = 'y';
	public static final char OPT_RETAIN_OTHER = 'z';
	public static final char OPT_RETAIN_VIDEO = 'x';
	public static final char OPT_SEPARATE = 's';
	public static final char OPT_UPDATE = 'u';
	public static final char OPT_VIDEO_CODEC = 'V';
	public static final char OPT_VIDEO_DIR = 'D';
	public static final char OPT_VIDEO_FILE = 'F';
	public static final char OPT_VIDEO_FILESPEC = 'W';
	public static final char OPT_VIDEO_POS = 'P';
	public static final char OPT_VIDEO_SHIFT = 'S';
	public static final char OPT_VIEW_TARGET = 'T';
	
	private static Getopt options(String[] opts) {
		LongOpt[] longopts = new LongOpt[] {
			new LongOpt("acodec", LongOpt.REQUIRED_ARGUMENT, null, OPT_AUDIO_CODEC)
			, new LongOpt("adir", LongOpt.REQUIRED_ARGUMENT, null, OPT_AUDIO_DIR)
			, new LongOpt("afile", LongOpt.REQUIRED_ARGUMENT, null, OPT_AUDIO_FILE)
			, new LongOpt("apos", LongOpt.REQUIRED_ARGUMENT, null, OPT_AUDIO_POS)
			, new LongOpt("aspec", LongOpt.REQUIRED_ARGUMENT, null, OPT_AUDIO_FILESPEC)
			, new LongOpt("cache", LongOpt.REQUIRED_ARGUMENT, null, OPT_CACHE)
			, new LongOpt("container", LongOpt.REQUIRED_ARGUMENT, null, OPT_CONTAINER)
			, new LongOpt("help", LongOpt.NO_ARGUMENT, null, OPT_HELP)
			, new LongOpt("dir", LongOpt.REQUIRED_ARGUMENT, null, OPT_LIST_DIR)
			, new LongOpt("delim", LongOpt.REQUIRED_ARGUMENT, null, OPT_DELIM)
			, new LongOpt("output-dir", LongOpt.REQUIRED_ARGUMENT, null, OPT_OUTPUT_DIR)
			, new LongOpt("retain-audio", LongOpt.NO_ARGUMENT, null, OPT_RETAIN_AUDIO)
			, new LongOpt("retain-other", LongOpt.NO_ARGUMENT, null, OPT_RETAIN_OTHER)
			, new LongOpt("retain-video", LongOpt.NO_ARGUMENT, null, OPT_RETAIN_VIDEO)
			, new LongOpt("separate", LongOpt.NO_ARGUMENT, null, OPT_SEPARATE)
			, new LongOpt("update", LongOpt.NO_ARGUMENT, null, OPT_UPDATE)
			, new LongOpt("vcodec", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_CODEC)
			, new LongOpt("vdir", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_DIR)
			, new LongOpt("vfile", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_FILE)
			, new LongOpt("vpos", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_POS)
			, new LongOpt("vspec", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_FILESPEC)
			, new LongOpt("vshift", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIDEO_SHIFT)
			, new LongOpt("target", LongOpt.REQUIRED_ARGUMENT, null, OPT_VIEW_TARGET)
		};
		return new Getopt("com.newasptech.postslate.Cmd", opts, new String(new char[]{OPT_CACHE, ':', OPT_HELP}), longopts);
	}
	
	public static void realMain(String[] argv) throws Exception {
		String[] opts = new String[argv.length - 1];
		for (int i = 0; i != opts.length;)
			opts[i] = argv[++i];
		String command = argv[0], acodec = null, adir = null, afile = null,
				aspec = null, cacheDir = null, container = null,
				delim = "\t", listDir = null, outputDir = null, vcodec = null,
				vdir = null, vfile = null, vspec = null, target = VIEW_CLAP;
		float apos = -1.0f, vpos = -1.0f, vshift = 0.0f;
		Getopt g = options(argv);
		boolean retainAudio = false, retainOther = false, retainVideo = false,
				separate = false, update = false;
		int c;
		while ((c = g.getopt()) != -1) {
			switch (c)
			{
			case OPT_AUDIO_CODEC:		acodec = g.getOptarg(); break;
			case OPT_AUDIO_DIR:			adir = g.getOptarg(); break;
			case OPT_AUDIO_FILE:		afile = g.getOptarg(); break;
			case OPT_AUDIO_POS:			apos = Float.valueOf(g.getOptarg()); break;
			case OPT_AUDIO_FILESPEC:	aspec = g.getOptarg(); break;
			case OPT_CACHE:				cacheDir = g.getOptarg(); break;
			case OPT_CONTAINER:			container = g.getOptarg(); break;
			case OPT_DELIM:				delim = g.getOptarg(); break;
			case OPT_LIST_DIR:			listDir = g.getOptarg(); break;
			case OPT_OUTPUT_DIR:		outputDir = g.getOptarg(); break;
			case OPT_RETAIN_AUDIO:		retainAudio = true; break;
			case OPT_RETAIN_OTHER:		retainOther = true; break;
			case OPT_RETAIN_VIDEO:		retainVideo = true; break;
			case OPT_SEPARATE:			separate = true; break;
			case OPT_UPDATE:			update = true; break;
			case OPT_VIDEO_CODEC:		vcodec = g.getOptarg(); break;
			case OPT_VIDEO_DIR:			vdir = g.getOptarg(); break;
			case OPT_VIDEO_FILE:		vfile = g.getOptarg(); break;
			case OPT_VIDEO_POS:			vpos = Float.valueOf(g.getOptarg()); break;
			case OPT_VIDEO_FILESPEC:	vspec = g.getOptarg(); break;
			case OPT_VIDEO_SHIFT:		vshift = Float.valueOf(g.getOptarg()); break;
			case OPT_VIEW_TARGET:		target = g.getOptarg(); break;
			default:					printHelpAndExit(); break;
			}
		}
		Cache cache = new Cache(cacheDir);
		Config cfg = new Config(cacheDir);
		initLogging(cfg);
		newAVEngine(cfg).check();
		if (aspec == null) aspec = cfg.getProperty(Config.FILESPEC_AUDIO);
		if (vspec == null) vspec = cfg.getProperty(Config.FILESPEC_VIDEO);
		if (command.contentEquals("match")) {
			if (vdir != null)
				matchDirs(cache, vdir, adir, vspec, aspec, update, cfg, null);
			else
				matchFiles(cache, vfile, vpos, afile, apos, cfg);
		}
		else if (command.contentEquals("unmatch"))
			unmatch(cache, vfile, afile, cfg);
		else if (command.contentEquals("list"))
			list(cache, listDir, delim, cfg);
		else if (command.contentEquals("stag"))
			stag(cache, optArgs(opts, g.getOptind()), cfg);
		else if (command.contentEquals("unstag"))
			unstag(cache, optArgs(opts, g.getOptind()), cfg);
		else if (command.contentEquals("view"))
			view(cache, opts[g.getOptind()], target, vshift, container, cfg);
		else if (command.contentEquals("merge"))
			merge(cache, vdir, outputDir, container, separate, retainVideo,
				retainAudio, retainOther, vshift, vcodec, acodec, cfg, null);
		else
			printHelpAndExit();
	}
	
	public static void main(String[] argv) {
		try {
			realMain(argv);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
			System.exit(2);
		}
		System.exit(0);
	}
}