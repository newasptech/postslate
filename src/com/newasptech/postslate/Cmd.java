/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
//import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.TreeMap;
import java.text.DecimalFormat;

import com.newasptech.postslate.AVClipNDir;
import com.newasptech.postslate.util.Text;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class Cmd {
	@SuppressWarnings("unused")
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.Cmd");

	private static final DecimalFormat TIME_FORMAT = new DecimalFormat("###,##0.000000");
	
	private static void list(String dir, String delim, float vshift, Session s)
			throws Exception {
		Workspace wksp = s.getWorkspaceForPath(dir);
		for (Iterator<AVPair> pp = wksp.contents().iterator(); pp.hasNext();) {
			AVPair p = pp.next();
			String Z = TIME_FORMAT.format(0.0f), NF = "-";
			String[] parts = new String[]{NF, Z, Z, NF, Z, Z};
			float vDuration = (p.video() != null) ? AVClip.duration(p.video(), AVEngine.MetaValue.CODEC_TYPE_AUDIO, s.getAVEngine()) : 0.0f,
					aDuration = (p.audio() != null) ? AVClip.duration(p.audio(), AVEngine.MetaValue.CODEC_TYPE_AUDIO, s.getAVEngine()) : 0.0f;
			TrimValues bounds = null;
			if (p.video() != null && p.audio() != null) {
				bounds = new TrimValues(vDuration, p.video().getOffset(), vshift,
					aDuration, p.audio().getOffset());
			}
			int i=0;
			if (p.video() != null) {
				parts[i++] = p.video().getName();
				parts[i++] = TIME_FORMAT.format((bounds != null) ? bounds.getVideoStart() : 0.0f);
				parts[i++] = TIME_FORMAT.format((bounds != null) ? bounds.getVideoStart() + bounds.getDuration() : vDuration);
			}
			else
				i += parts.length / 2;
			if (p.audio() != null) {
				parts[i++] = p.audio().getName();
				parts[i++] = TIME_FORMAT.format((bounds != null) ? bounds.getAudioStart() : 0.0f);
				parts[i++] = TIME_FORMAT.format((bounds != null) ? bounds.getAudioStart() + bounds.getDuration() : aDuration);
			}
			System.out.println(Text.join(parts, delim));
		}
	}
	
	private static void stag(List<String> filePaths, Session s)
		throws Exception {
		Map<Workspace, String[]> wfMap = arrangeByWorkspace(filePaths, s);
		for (Workspace wksp : wfMap.keySet()) {
			for (String filePath : wfMap.get(wksp)) {
				AVClipNDir cd = wksp.findClip(new File(filePath));
				wksp.getMatchBox().remove(cd.clip);
				wksp.getMatchBox().addStag(cd.clip, cd.dir.getType());
			}
			wksp.saveMatches();
		}
	}
	
	private static void unstag(List<String> filePaths, Session s)
		throws Exception {
		Map<Workspace, String[]> wfMap = arrangeByWorkspace(filePaths, s);
		for (Workspace wksp : wfMap.keySet()) {
			for (String filePath : wfMap.get(wksp)) {
				AVClipNDir cd = wksp.findClip(new File(filePath));
				if (wksp.getMatchBox().isStag(cd.clip, cd.dir.getType()))
					wksp.getMatchBox().remove(cd.clip);
			}
			wksp.saveMatches();
		}
	}
	
	private static Map<Workspace, String[]> arrangeByWorkspace(List<String> filePaths, Session s)
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
				wksp = s.getWorkspaceForPath(filePath);
				dir = fdir;
				wfList = new LinkedList<String>();
			}
			wfList.add(filePath);
		}
		if (wksp != null) wfMap.put(wksp, wfList.toArray(new String[]{}));
		return wfMap;
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
				"\tFor each clip, show the trimmed start and end times.\n" + 
				"  list --dir= [--delim=]\n" + 
				"stag - mark a file as not having any match\n" + 
				"  stag {file}\n" + 
				"\n" + 
				"unstag - remove a previous stag designation\n" + 
				"\n" + 
				"view - view media\n" + 
				"  view --target=" + Text.join(new String[]{
						ViewController.ViewType.CLAP.toString(),
						ViewController.ViewType.FULL.toString(),
						ViewController.ViewType.VIDEO.toString(),
						ViewController.ViewType.AUDIO.toString()
						}, "|") + " {file}\n" + 
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
				vdir = null, vfile = null, vspec = null;
		ViewController.ViewType target = ViewController.ViewType.CLAP;
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
			case OPT_VIEW_TARGET:		target = ViewController.ViewType.fromString(g.getOptarg()); break;
			default:					printHelpAndExit(); break;
			}
		}
		Session session = new Session(cacheDir);
		if (command.contentEquals("match") && vdir != null) {
			if (aspec == null)
				aspec = session.getConfig().getProperty(Config.FILESPEC_AUDIO);
			if (vspec == null)
				vspec = session.getConfig().getProperty(Config.FILESPEC_VIDEO);
			session.getWorkspaceFromScan(vdir, vspec, adir, aspec, update, null);
		}
		else if (command.contentEquals("match") && vdir == null) {
			File v = new File(vfile), a = new File(afile);
			Workspace w = session.getWorkspaceForFile(v);
			w.matchFiles(v, vpos, a, apos);
		}
		else if (command.contentEquals("unmatch")) {
			File v = new File(vfile), a = new File(afile);
			Workspace w = session.getWorkspaceForFile(v);
			w.unmatch(v, a);
		}
		else if (command.contentEquals("list"))
			list(listDir, delim, vshift, session);
		else if (command.contentEquals("stag"))
			stag(optArgs(opts, g.getOptind()), session);
		else if (command.contentEquals("unstag"))
			unstag(optArgs(opts, g.getOptind()), session);
		else if (command.contentEquals("view")) {
			File avFile = new File(opts[g.getOptind()]);
			Workspace w = session.getWorkspaceForFile(avFile);
			ViewController vc = new ViewController(vshift, container, w);
			vc.view(avFile, target,	session.getConfig().ivalue(Config.PREVIEW_WIDTH),
					session.getConfig().ivalue(Config.PREVIEW_HEIGHT), -1, -1);
		}
		else if (command.contentEquals("merge")) {
			Workspace w = session.getWorkspaceForPath(vdir);
			MergeController mc = new MergeController(outputDir, container, separate,
					retainVideo, retainAudio, retainOther, vshift, vcodec, acodec, w);
			mc.mergeAll(null);
		}
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