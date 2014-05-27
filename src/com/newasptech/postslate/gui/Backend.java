/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CancellationException;

import javax.swing.ProgressMonitor;
import javax.swing.JTable;

import com.newasptech.postslate.AVClip;
import com.newasptech.postslate.AVEngine;
import com.newasptech.postslate.Cache;
import com.newasptech.postslate.Cmd;
import com.newasptech.postslate.Config;
import com.newasptech.postslate.Workspace;
import com.newasptech.postslate.AVClipNDir;
import com.newasptech.postslate.audio.Event;
import com.newasptech.postslate.audio.wave.AsyncLoader;
import com.newasptech.postslate.gui.FileViewPanel.AVFileTableModel;
import com.newasptech.postslate.gui.FileViewPanel.AVFileTableRenderer;
import com.newasptech.postslate.util.Misc;

/** Class for back-end GUI-related A/V manipulation. */
class Backend {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.Backend");
	private Cache cache = null;
	private Config config = null;
	private Workspace wksp = null;

	public Backend(String cacheDir) throws Exception {
		cache = new Cache(cacheDir);
		config = new Config(cache.getBasedir());
		Cmd.initLogging(config);
		getAVEngine(config).check();
	}

	public Cache getCache() {
		return cache;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public Workspace getWorkspace() {
		return wksp;
	}

	public Workspace loadWorkspace(String path, MainFrame.Controls c) throws Exception {
		wksp = new Workspace(path, getConfig(),	getCache(), getAVEngine(getConfig()), null);
		updateWorkspaceFileList(c);
		return wksp;
	}
	
	public void scanNewWorkspace(String pathCamera, String pathExtAudio, String filemaskVideo,
			String filemaskAudio, boolean update, ProgressMonitor m, MainFrame.Controls c)
			throws Exception {
		try {
			wksp = Cmd.matchDirs(getCache(), pathCamera, pathExtAudio,
				filemaskVideo, filemaskAudio, update, getConfig(), m);
		}
		catch(CancellationException cex) {
			_l.log(Level.INFO, "User canceled the operation");
		}
		updateWorkspaceFileList(c);
	}
	
	public void clearWorkspace(MainFrame.Controls c) {
		wksp = null;
		updateWorkspaceFileList(c);
	}
	
	private void updateWorkspaceFileList(MainFrame.Controls c) {
		JTable fl = c.getFileList();
		List<Workspace.AVPair> avPairs = wksp != null ? wksp.contents() : null;
		if (avPairs != null) {
			((AVFileTableModel)fl.getModel()).setFiles(avPairs);
			fl.getColumn(AVFileTableModel.CAMERA_CLIP_HEADER).setCellRenderer(new AVFileTableRenderer(this));
			fl.getColumn(AVFileTableModel.EXTAUDIO_CLIP_HEADER).setCellRenderer(new AVFileTableRenderer(this));
			fl.revalidate();
			fl.repaint();
		}
		else {
			fl.removeAll();
		}
	}
	
	public AVEngine getAVEngine(Config cfg) {
		return Cmd.newAVEngine(cfg);
	}
	
	private String getFilePath(int row, int col, MainFrame.Controls c) {
		Workspace w = getWorkspace();
		StringBuilder filePath = new StringBuilder(((col == 0) ? w.getVideoDir() : w.getAudioDir()).getPath());
		filePath.append(System.getProperty("file.separator"));
		filePath.append((String)c.getFileList().getValueAt(row, col));
		return filePath.toString();
	}

	public static final String STAG_MATCH = "";
	public void toggleStag(MainFrame.Controls c, int row, int col) throws Exception {
		if (STAG_MATCH.equals((String)c.getFileList().getValueAt(row, col))) return;
		Workspace w = getWorkspace();
		AVClipNDir cd = w.findClip(new File(getFilePath(row, col, c)));
		boolean needStag = !w.getMatchBox().isStag(cd.clip, cd.dir.getType());
		w.getMatchBox().remove(cd.clip);
		if (needStag)
			w.getMatchBox().addStag(cd.clip, cd.dir.getType());
		w.saveMatches();
		updateWorkspaceFileList(c);
	}
	
	/** Given an AVClip, change which event represents the clap. 
	 * @param vClip video clip
	 * @param aClip audio clip
	 * @param videoChanged true if the video clip needs to be changed, false otherwise
	 * @param clap an Event representing the new clap position
	 * */
	public AVClip setClapEvent(AVClip vClip, AVClip aClip, boolean videoChanged, Event clap, MainFrame.Controls c) {
    	_l.log(Level.FINE, "Selected new " + (videoChanged ? "camera" : "ext. audio") + " clap point of " + clap.getTime());
    	Workspace wksp = getWorkspace();
    	AVClip ret = null;
    	if (videoChanged) {
    		AVClip newVClip = new AVClip(vClip, clap.getTime(), vClip.getDuration(),
    				Misc.range(vClip.getMeta().size()));
    		if (wksp.getMatchBox().hasMatchForVideo(newVClip))
    			wksp.getMatchBox().removeVideo(newVClip);
    		vClip = newVClip;
    		ret = newVClip;
	    }
    	else {
    		AVClip newAClip = new AVClip(aClip, clap.getTime(), aClip.getDuration(),
    				Misc.range(aClip.getMeta().size()));
    		if (wksp.getMatchBox().hasMatchForAudio(newAClip))
    			wksp.getMatchBox().removeAudio(newAClip);
    		aClip = newAClip;
    		ret = newAClip;
	    }
    	wksp.getMatchBox().addMatch(vClip, aClip);
    	try {
    		wksp.saveMatches();
    		updateWaveGraphs(new AVClipNDir(vClip, wksp.getVideoDir()),
        			new AVClipNDir(aClip, wksp.getAudioDir()),
        			c.getVideoGraphPanel(),	c.getAudioGraphPanel());
        	autoPlayIfNeeded(-1, c);
    	}
    	catch(Exception ex) {
    		_l.log(Level.SEVERE, "Caught an exception while updating matches", ex);
    	}
    	return ret;
	}
	
	public void startWaveGraphs(MainFrame.Controls c, int row) throws Exception {
		AVClipNDir vClip = null, aClip = null;
		boolean haveVideo = !(STAG_MATCH.equals((String)c.getFileList().getValueAt(row, 0)));
		if (haveVideo)
			vClip = wksp.findClip(new File(getFilePath(row, 0, c)));
		boolean haveAudio = !(STAG_MATCH.equals((String)c.getFileList().getValueAt(row, 1)));
		if (haveAudio)
			aClip = wksp.findClip(new File(getFilePath(row, 1, c)));
		updateWaveGraphs(vClip, aClip, c.getVideoGraphPanel(), c.getAudioGraphPanel());
		c.getSyncPanel().setClips(vClip != null ? vClip.clip : null, aClip != null ? aClip.clip : null);
	}
	
	private void updateWaveGraphs(AVClipNDir vc0, AVClipNDir acd, WaveGraphPanel vGraphPanel, WaveGraphPanel aGraphPanel)
		throws IOException {
		vGraphPanel.nullifyLoader();
		aGraphPanel.nullifyLoader();
		AsyncLoader audioCacheCam = null, audioCacheExt = null;
		float vTrimStart = 0.0f, aTrimStart = 0.0f, trimDuration = -1.0f, vClipLen = -1.0f, aClipLen = -1.0f,
			graphTimeSpan = -1.0f, vStartGraphOffset = 0.0f, aStartGraphOffset = 0.0f;
		AVClipNDir vcd = null;
		AVEngine ave = getAVEngine(getConfig());
		if (vc0 != null) {
			vcd = new AVClipNDir(new AVClip(vc0.clip, vc0.clip.getOffset(), vc0.clip.getDuration(),
				new int[]{vc0.clip.getMeta().findFirstIndex(ave.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				ave.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO))}), vc0.dir);
			vClipLen = AVClip.duration(vcd.clip, AVEngine.MetaValue.CODEC_TYPE_AUDIO, ave);
			if (acd == null) {
				trimDuration = vClipLen;
				graphTimeSpan = vClipLen;
			}
			String format = "wav";
			File camAudio = File.createTempFile("pslm", "." + format);
			camAudio.deleteOnExit();
			_l.log(Level.FINE, "Transcode audio from " + vcd.dir.getPath() + "/" + vcd.clip.getName() + " to temporary file " + camAudio.getPath());
			AVClip sourceClip = new AVClip(vcd.clip, 0.0f); // extract the whole clip. In vcd.clip, the Offset represents the clap position.
			ave.transcode(vcd.dir, sourceClip, null, null, null, null, camAudio.getPath());
			_l.log(Level.FINE, "Create AsyncLoader for file "+camAudio.getPath());
			audioCacheCam = new AsyncLoader(new FileInputStream(camAudio.getPath()));
		}
		else _l.log(Level.FINE, "video clip is null");
		if (acd != null) {
			aClipLen = AVClip.duration(acd.clip, AVEngine.MetaValue.CODEC_TYPE_AUDIO, ave);
			if (vcd == null) {
				trimDuration = aClipLen;
				graphTimeSpan = aClipLen;
			}
			StringBuilder p = new StringBuilder(acd.dir.getPath());
			p.append(System.getProperty("file.separator"));
			p.append(acd.clip.getName());
			_l.log(Level.FINE, "Create AsyncLoader for file " + p.toString());
			audioCacheExt = new AsyncLoader(new FileInputStream(p.toString()));
		}
		else _l.log(Level.FINE, "audio clip is null");
		if (vcd != null && acd != null) {
			float[] bounds = Cmd.trimBoundaries(vClipLen, vcd.clip.getOffset(),
					aClipLen, acd.clip.getOffset());
			vTrimStart = bounds[0];
			aTrimStart = bounds[1];
			trimDuration = bounds[2];
			float graphBeforeClap, graphAfterClap = Math.max(vClipLen - vcd.clip.getOffset(), aClipLen - acd.clip.getOffset());
			if (vcd.clip.getOffset() > acd.clip.getOffset()) {
				graphBeforeClap = vcd.clip.getOffset();
				aStartGraphOffset = vcd.clip.getOffset() - acd.clip.getOffset();
			}
			else {
				graphBeforeClap = acd.clip.getOffset();
				vStartGraphOffset = acd.clip.getOffset() - vcd.clip.getOffset();
			}
			graphTimeSpan = graphBeforeClap + graphAfterClap;
			_l.log(Level.FINE, String.format("graphTimeSpan = %f", graphTimeSpan));
		}
		if (vcd != null)
			vGraphPanel.prepareGraph(audioCacheCam, vTrimStart, trimDuration,
				vcd.clip.getOffset(), vStartGraphOffset, graphTimeSpan);
		if (acd != null)
			aGraphPanel.prepareGraph(audioCacheExt, aTrimStart, trimDuration,
				acd.clip.getOffset(), aStartGraphOffset, graphTimeSpan);
	}
	
	public void autoPlayIfNeeded(int row, MainFrame.Controls c) throws Exception {
		if (row < 0)
			row = c.getFileList().getSelectedRow();
		if (c.getAutoView().isSelected()) {
			int vcol = 0, acol=1;
			String vfp = getFilePath(row, vcol, c);
			String afn = (String)c.getFileList().getValueAt(row, acol),
					vfn = (String)c.getFileList().getValueAt(row, vcol);
			if (vfn.length() > 0 && afn.length() > 0) {
				String view = c.getViewType().toString().toLowerCase();
				_l.log(Level.FINE, "View "+vfp+" portion "+view);
				Cmd.view(getCache(), vfp, view, (Float)c.getVideoShift().getValue(),
						c.getMergeFormat().getText(), getConfig());
			}
		}
	}
	
	public void play(PreviewPanel.ViewType viewType, MainFrame.Controls c) {
		int row = c.getFileList().getSelectedRow();
		String playFile = null, playDir = null;
		switch(viewType) {
		case CLAP:
		case FULL:
		case VIDEO:
			playFile = (String)c.getFileList().getValueAt(row, 0);
			playDir = wksp.getVideoDir().getPath();
			break;
		case AUDIO:
			playFile = (String)c.getFileList().getValueAt(row, 1);
			playDir = wksp.getAudioDir().getPath();
			break;
		}
		if (playFile == null) return;
		StringBuilder filePath = new StringBuilder(playDir);
		filePath.append(System.getProperty("file.separator"));
		filePath.append(playFile);
		try {
			Cmd.view(getCache(), filePath.toString(), viewType.toString().toLowerCase(),
					(Float)c.getVideoShift().getValue(), c.getMergeFormat().getText(), getConfig());
		}
		catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
