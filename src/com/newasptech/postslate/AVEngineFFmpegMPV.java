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
import com.newasptech.postslate.util.SubprocessSingletonWrapper;
import com.newasptech.postslate.util.Text;

public class AVEngineFFmpegMPV implements AVEngine {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.AVEngineFFmpeg");
	private Config cfg;
	private SubprocessSingletonWrapper playSubWrap;
	
	public AVEngineFFmpegMPV(Config _cfg) {
		_l.log(Level.FINE, "New AVEngine");
		cfg = _cfg;
		playSubWrap = new SubprocessSingletonWrapper();
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
	
	public void play(AVDirRef inputDir, AVClip input, int width, int height, int x, int y,
			boolean cancelAnyCurrent) {
		if (!cancelAnyCurrent && playSubWrap.isRunning()) return;
		List<String> cmd = new LinkedList<String>();
		cmd.add(Subprocess.execName("mpv"));
		cmd.add("--no-config");
		cmd.add("--ontop");
		cmd.add("--no-border");
		cmd.add("--no-osd-bar");
		cmd.add("--quiet");
		cmd.add("--stop-screensaver");
		// options to improve A/V sync
		cmd.add("--forceidx");
		cmd.add("--framedrop=yes");
		cmd.add("--initial-audio-sync");
		if (width > 0 && height > 0)
			cmd.add(String.format("--autofit-larger=%dx%d", width, height));
		if (x >= 0 && y >= 0)
			cmd.add(String.format("--geometry=%d:%d", x, y));
		if (input.getDuration() > 0.0f) {
			cmd.add("--length=" + input.getDuration());
		}
		if (input.getOffset() > 0.0f) {
			cmd.add("--start=" + input.getOffset());
		}
		String inputFilePath = inputDir.getPath() + System.getProperty("file.separator") + input.getName();
		_l.log(Level.FINE, "Play AV clip " + inputFilePath + " from offset " + input.getOffset() + " with duration " + input.getDuration());
		cmd.add(inputFilePath);
		String[] cmdArray = cmd.toArray(new String[]{});
		StringBuilder msg = new StringBuilder();
		for (String arg : cmdArray) {
			msg.append(arg);
			msg.append(" ");
		}
		_l.log(Level.FINE, msg.toString());
		Subprocess p = new Subprocess(cmdArray, cfg.getProperty(Config.SEARCH_PATH));
		try {
			playSubWrap.set(p.start());
		}
		catch(IOException ioe) {}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void stopCurrent() {
		if (playSubWrap != null)
			playSubWrap.kill();
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
	
	public String[] outputFormats() {
		List<String> mfList = new LinkedList<String>();
		String formats = null;
		try {
			formats = Subprocess.checkOutput(new String[]{"ffmpeg", "-formats"});
		}
		catch(Exception ex) {
			_l.log(Level.SEVERE, "An error occurred while checking output formats", ex);
			return null;
		}
		boolean passedHeader = false;
		for (String _line : formats.split("\n")) {
			String line = _line.trim();
			if (!passedHeader) {
				if (line.contentEquals("--"))
					passedHeader = true;
				continue;
			}
			String[] tokens = line.split("\\s");
			String de = tokens[0], format = tokens[1];
			if (de.endsWith("E"))
				mfList.add(format);
		}
		return mfList.toArray(new String[]{});
	}
	
	private static String[] FFMPEG_PROGS = new String[] {
		Subprocess.execName("ffmpeg"), Subprocess.execName("ffprobe") },
		MPV_PROGS = new String[] {Subprocess.execName("mpv")};
	private static String URL_FFMPEG = "http://ffmpeg.org/download.html", URL_MPV = "http://mpv.io";
	public void check() throws RequiredComponentMissing, OptionalComponentMissing, ComponentCheckFailed {
		for (String p : FFMPEG_PROGS)
			check(p, URL_FFMPEG);
		for (String p : MPV_PROGS)
			check(p, URL_MPV);
	}
	
	private void check(String p, String url)
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
				throw new RequiredComponentMissing(p, url, cfg.getProperty(Config.SEARCH_PATH), msg);
			}
			_l.log(Level.SEVERE, "Error while checking for "+p, ioex);
			System.exit(4);
		}
		catch(Subprocess.NonzeroExit nze) {
			throw new ComponentCheckFailed(p, sp != null ? sp.getErr() : "");
		}
	}
}
