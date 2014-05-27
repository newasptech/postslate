/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.newasptech.postslate.util.Subprocess;
import com.newasptech.postslate.util.Text;

public class AVEngineFFmpeg implements AVEngine {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.AVEngineFFmpeg");
	private Config cfg;
	
	public AVEngineFFmpeg(Config _cfg) {
		cfg = _cfg;
	}
	
	public String metaKeyName(MetaKey key) {
		String n = null;
		switch(key) {
		case CODEC_TYPE:
			n = "codec_type"; break;
		case DURATION:
			n = "duration"; break;
		}
		return n;
	}
	
	public String metaValueName(MetaValue value) {
		String n = null;
		switch(value) {
		case CODEC_TYPE_VIDEO:
			n = "video"; break;
		case CODEC_TYPE_AUDIO:
			n = "audio"; break;
		case CODEC_TYPE_DATA:
			n = "data"; break;
		}
		return n;
	}

	public StreamMetaList streamMeta(File f) {
		StreamMetaList metaOut = new StreamMetaList(new LinkedList<StreamMeta>());
		String[] cmd = {Subprocess.execName("ffprobe"), "-loglevel", "error",
				"-of", "compact", "-show_streams", "-count_frames", f.getPath()};
		try {
			_l.log(Level.FINE, Text.join(cmd));
			String cmdOut = Subprocess.checkOutput(cmd, cfg.getProperty(Config.SEARCH_PATH));
			for (String line : cmdOut.split("\n")) {
				_l.log(Level.FINE, line);
				StreamMeta sm = new StreamMeta();
				int pipePos = line.indexOf('|');
				if (!line.substring(0, pipePos).contentEquals("stream")) continue;
				for (String item : line.substring(pipePos+1).split("\\|")) {
					String[] iparts = item.split("=");
					if (iparts.length != 2) {
						throw new RuntimeException("Unexpected term in metadata list: "+item);
					}
					sm.put(iparts[0], new StreamMetaValue(iparts[1]));
					_l.log(Level.FINEST, Text.join(iparts, "="));
				}
				metaOut.add(sm);
			}
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
		return metaOut;
	}
	
	public void play(AVDirRef inputDir, AVClip input, int width, int height) {
		StringBuffer scaleArg = new StringBuffer("scale=");
		scaleArg.append(width);
		scaleArg.append(":");
		scaleArg.append(height);
		String inputFilePath = inputDir.getPath() + System.getProperty("file.separator") + input.getName();
		_l.log(Level.FINE, "Play AV clip " + inputFilePath + " with duration " + input.getDuration());
		String[] cmd = { Subprocess.execName("ffplay"),
				"-x", cfg.getProperty(Config.PREVIEW_WIDTH),
				"-y", cfg.getProperty(Config.PREVIEW_HEIGHT),
				"-window_title", "Preview",
				"-autoexit",
				"-vf", scaleArg.toString(),
				inputFilePath
			};
		Subprocess p = new Subprocess(cmd, cfg.getProperty(Config.SEARCH_PATH));
		try {
			p.run(Subprocess.timeout(input.getDuration()));
		}
		catch(IOException ioe) {}
		catch(InterruptedException ie) {}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void addCmdArgs(AVDirRef inputDir, AVClip clip, int filepos, List<String> iList, List<String> oList, Map<String, Integer> oStreamPos) {
		if (clip == null) return;
		iList.add("-ss");
		iList.add(Float.toString(clip.getOffset()));
		iList.add("-i");
		iList.add(inputDir.getPath() + System.getProperty("file.separator") + clip.getName());
		for (Iterator<StreamMeta> pMeta = clip.getMeta().iterator(); pMeta.hasNext();) {
			StreamMeta m = pMeta.next();
			String streamType = m.get("codec_type").value().substring(0, 1);
			if (!oStreamPos.containsKey(streamType)) {
				oStreamPos.put(streamType, 0);
			}
			oList.add("-map");
			StringBuffer a = new StringBuffer();
			a.append(filepos);
			a.append(":");
			a.append(streamType);
			a.append(":");
			int spos = oStreamPos.get(streamType);
			a.append(spos++);
			oStreamPos.put(streamType, spos);
			oList.add(a.toString());
		}
	}
	
	private File ffmpegVoodoo(AVDirRef vdir, AVClip vinput, AVDirRef adir, AVClip ainput,
			String[] codecArgs, String outputPath) {
		List<String> iList = new LinkedList<String>(), oList = new LinkedList<String>();
		int filepos = 0;
		Map<String, Integer> aStreamPos = new Hashtable<String, Integer>(), vStreamPos = new Hashtable<String, Integer>();
		iList.add(Subprocess.execName("ffmpeg"));
		iList.add("-y");
		if (ainput != null)
			addCmdArgs(adir, ainput, filepos++, iList, oList, aStreamPos);
		if (vinput != null)
			addCmdArgs(vdir, vinput, filepos++, iList, oList, vStreamPos);
		for (String a : codecArgs) {
			oList.add(a);
		}
		AVClip dsource = (vinput != null) ? vinput : ainput;
		if (dsource.getDuration() >= 0.0f) {
			oList.add("-t");
			oList.add(Float.toString(dsource.getDuration()));
		}
		oList.add(outputPath);
		List<String> cmdList = new LinkedList<String>(iList);
		cmdList.addAll(oList);
		try {
			String[] cmdArgs = cmdList.toArray(new String[]{});
			_l.log(Level.FINE, Text.join(cmdArgs));
			Subprocess.check(cmdArgs, cfg.getProperty(Config.SEARCH_PATH));
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
		return new File(outputPath);
	}

	public File repackage(AVDirRef vdir, AVClip vinput, AVDirRef adir, AVClip ainput,
			String outputPath) {
		String[] codecArgs = new String[]{"-codec", "copy"};
		return ffmpegVoodoo(vdir, vinput, adir, ainput, codecArgs, outputPath);
	}
	
	public File transcode(AVDirRef vdir, AVClip vinput, AVDirRef adir, AVClip ainput, String videoCodec,
			String audioCodec, String outputPath) {
		LinkedList<String> cargs = new LinkedList<String>();
		if (videoCodec != null) {
			cargs.add("-vcodec");
			cargs.add(videoCodec);
		}
		if (audioCodec != null) {
			cargs.add("-acodec");
			cargs.add(audioCodec);
		}
		String[] codecArgs = cargs.toArray(new String[]{});
		return ffmpegVoodoo(vdir, vinput, adir, ainput, codecArgs, outputPath);
	}
	
	private static String[] REQUIRED_PROGS = new String[]{Subprocess.execName("ffmpeg"),
		Subprocess.execName("ffprobe")}; 
	private static String URL = "http://ffmpeg.org/download.html";
	public void check() throws RequiredComponentMissing, OptionalComponentMissing, ComponentCheckFailed {
		for (String p : REQUIRED_PROGS) {
			check(p, null);
		}
		check(Subprocess.execName("ffplay"), "Alternatively, you can set the parameter" + Config.VIDEO_PLAY_CMD + " to an alternate multimedia viewer.");
	}
	
	private void check(String p, String extra)
			throws RequiredComponentMissing, OptionalComponentMissing, ComponentCheckFailed {
		String CANNOT_RUN_PROGRAM = "Cannot run program";
		Subprocess sp = null;
		try {
			sp = new Subprocess(new String[]{p, "-h"}, cfg.getProperty(Config.SEARCH_PATH));
			sp.run(200);
		}
		catch(InterruptedException iex) {
			_l.log(Level.SEVERE, "Error while checking for "+p, iex);
			System.exit(3);
		}
		catch(IOException ioex) {
			String msg = ioex.getMessage();
			_l.log(Level.FINE, msg);
			if (msg != null && msg.substring(0, CANNOT_RUN_PROGRAM.length()).contentEquals(CANNOT_RUN_PROGRAM)) {
				if (extra == null)
					throw new RequiredComponentMissing(p, URL, cfg.getProperty(Config.SEARCH_PATH), msg);
				else
					throw new OptionalComponentMissing(p, URL, extra);
			}
			_l.log(Level.SEVERE, "Error while checking for "+p, ioex);
			System.exit(4);
		}
		catch(Subprocess.NonzeroExit nze) {
			throw new ComponentCheckFailed(p, sp != null ? sp.getErr() : "");
		}
	}
}
