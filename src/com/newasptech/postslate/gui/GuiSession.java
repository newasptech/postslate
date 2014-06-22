/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CancellationException;

import java.awt.Point;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.newasptech.postslate.AVClip;
import com.newasptech.postslate.AVEngine;
import com.newasptech.postslate.Config;
import com.newasptech.postslate.Session;
import com.newasptech.postslate.TrimValues;
import com.newasptech.postslate.ViewController;
import com.newasptech.postslate.Workspace;
import com.newasptech.postslate.AVClipNDir;
import com.newasptech.postslate.audio.Event;
import com.newasptech.postslate.audio.wave.AsyncLoader;
import com.newasptech.postslate.gui.FileViewPanel.AVFileTableModel;
import com.newasptech.postslate.gui.FileViewPanel.AVFileTableRenderer;
import com.newasptech.postslate.util.Misc;

/** Class for back-end GUI-related A/V manipulation. */
class GuiSession extends Session {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.Backend");
	private Workspace workspace = null;
	
	public GuiSession(String cacheDir)
			throws IOException, AVEngine.ComponentCheckFailed, AVEngine.OptionalComponentMissing,
			AVEngine.RequiredComponentMissing {
		super(cacheDir);
	}

	public void loadWorkspace(String path, Controls c) throws Exception {
		workspace = getWorkspaceForPath(path);
		updateWorkspaceFileList(c);
	}
	
	public void scanNewWorkspace(String pathCamera, String pathExtAudio, String filemaskVideo,
			String filemaskAudio, boolean update, ProgressMonitor m, Controls c)
			throws Exception {
		try {
			workspace = getWorkspaceFromScan(pathCamera, filemaskVideo, pathExtAudio, filemaskAudio,
					update, m);
		}
		catch(CancellationException cex) {
			_l.log(Level.INFO, "User canceled the operation");
		}
		updateWorkspaceFileList(c);
	}
	
	public void clearWorkspace(Controls c) {
		workspace = null;
		updateWorkspaceFileList(c);
	}
	
	public Workspace getWorkspace() {
		return workspace;
	}
	
	private void updateWorkspaceFileList(Controls c) {
		JTable fl = c.getFileList();
		if (workspace != null) {
			((AVFileTableModel)fl.getModel()).setFiles(workspace.contents());
			fl.getColumn(AVFileTableModel.CAMERA_CLIP_HEADER).setCellRenderer(new AVFileTableRenderer(this));
			fl.getColumn(AVFileTableModel.EXTAUDIO_CLIP_HEADER).setCellRenderer(new AVFileTableRenderer(this));
			fl.revalidate();
			fl.repaint();
		}
		else {
			fl.removeAll();
		}
	}
	
	public static final String STAG_MATCH = "";
	public void toggleStag(Controls c, int row, int col) throws Exception {
		if (STAG_MATCH.equals((String)c.getFileList().getValueAt(row, col))) return;
		AVClipNDir cd = workspace.findClip(new File(getFilePath(row, col, c)));
		boolean needStag = !workspace.getMatchBox().isStag(cd.clip, cd.dir.getType());
		workspace.getMatchBox().remove(cd.clip);
		if (needStag)
			workspace.getMatchBox().addStag(cd.clip, cd.dir.getType());
		workspace.saveMatches();
		updateWorkspaceFileList(c);
	}
	
	/** Given an AVClip, change which event represents the clap. 
	 * @param vClip video clip
	 * @param aClip audio clip
	 * @param videoChanged true if the video clip needs to be changed, false otherwise
	 * @param clap an Event representing the new clap position
	 * */
	public AVClip setClapEvent(AVClip vClip, AVClip aClip, boolean videoChanged, Event clap, Controls c) {
    	_l.log(Level.FINE, "Selected new " + (videoChanged ? "camera" : "ext. audio") + " clap point of " + clap.getTime());
    	AVClip ret = null;
    	if (videoChanged) {
    		AVClip newVClip = new AVClip(vClip, clap.getTime(), vClip.getDuration(),
    				Misc.range(vClip.getMeta().size()));
    		if (workspace.getMatchBox().hasMatchForVideo(newVClip))
    			workspace.getMatchBox().removeVideo(newVClip);
    		vClip = newVClip;
    		ret = newVClip;
	    }
    	else {
    		AVClip newAClip = new AVClip(aClip, clap.getTime(), aClip.getDuration(),
    				Misc.range(aClip.getMeta().size()));
    		if (workspace.getMatchBox().hasMatchForAudio(newAClip))
    			workspace.getMatchBox().removeAudio(newAClip);
    		aClip = newAClip;
    		ret = newAClip;
	    }
    	workspace.getMatchBox().addMatch(vClip, aClip);
    	try {
    		workspace.saveMatches();
    		updateWaveGraphs(new AVClipNDir(vClip, workspace.getVideoDir()),
    				(Float)c.getVideoShift().getValue(),
        			new AVClipNDir(aClip, workspace.getAudioDir()),
        			c.getVideoGraphPanel(),	c.getAudioGraphPanel());
        	autoPlayIfNeeded(-1, c);
    	}
    	catch(Exception ex) {
    		_l.log(Level.SEVERE, "Caught an exception while updating matches", ex);
    	}
    	return ret;
	}
	
	public void startWaveGraphs(Controls c, int row) throws Exception {
		AVClipNDir vClip = null, aClip = null;
		boolean haveVideo = !(STAG_MATCH.equals((String)c.getFileList().getValueAt(row, 0)));
		if (haveVideo)
			vClip = workspace.findClip(new File(getFilePath(row, 0, c)));
		boolean haveAudio = !(STAG_MATCH.equals((String)c.getFileList().getValueAt(row, 1)));
		if (haveAudio)
			aClip = workspace.findClip(new File(getFilePath(row, 1, c)));
		updateWaveGraphs(vClip, (Float)c.getVideoShift().getValue(), aClip, c.getVideoGraphPanel(), c.getAudioGraphPanel());
		c.getSyncPanel().setClips(vClip != null ? vClip.clip : null, aClip != null ? aClip.clip : null);
	}
	
	private void updateWaveGraphs(AVClipNDir vc0, float vShift, AVClipNDir acd, WaveGraphPanel vGraphPanel, WaveGraphPanel aGraphPanel)
		throws IOException {
		vGraphPanel.nullifyLoader();
		aGraphPanel.nullifyLoader();
		AsyncLoader audioCacheCam = null, audioCacheExt = null;
		float vTrimStart = 0.0f, aTrimStart = 0.0f, trimDuration = -1.0f, vClipLen = -1.0f, aClipLen = -1.0f,
			graphTimeSpan = -1.0f, vStartGraphOffset = 0.0f, aStartGraphOffset = 0.0f;
		AVClipNDir vcd = null;
		AVEngine ave = getAVEngine();
		if (vc0 != null) {
			vcd = new AVClipNDir(new AVClip(vc0.clip, vc0.clip.getOffset(), vc0.clip.getDuration(),
				new int[]{vc0.clip.getMeta().findFirstIndex(ave.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				ave.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO))}), vc0.dir);
			vClipLen = AVClip.duration(vcd.clip, AVEngine.MetaValue.CODEC_TYPE_AUDIO, ave);
			_l.log(Level.FINE, String.format("vc0.clip.getDuration() = %.6f, vClipLen = %.6f", vc0.clip.getDuration(), vClipLen));
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
			TrimValues trimValues = new TrimValues(vClipLen, vcd.clip.getOffset(),
					vShift,	aClipLen, acd.clip.getOffset());
			vTrimStart = trimValues.getVideoStart();
			aTrimStart = trimValues.getAudioStart();
			trimDuration = trimValues.getDuration();
			_l.log(Level.FINE, String.format("video trim start = %.6f, audio trim start = %.6f, trim duration = %.6f",
					vTrimStart, aTrimStart, trimDuration));
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
	
	private String getFilePath(int row, int col, Controls c) {
		StringBuilder filePath = new StringBuilder(((col == 0) ? workspace.getVideoDir() : workspace.getAudioDir()).getPath());
		filePath.append(System.getProperty("file.separator"));
		filePath.append((String)c.getFileList().getValueAt(row, col));
		return filePath.toString();
	}
	
	/** Return the selected video/audio file, or null if none is currently selected.
	 * @param row a row index from the file list, or -1 to use the current selection
	 * @param col the column number of the video/audio file (0 or 1)
	 * @param c Controls reference
	 *  */
	private String getSelectedFilePath(int row, int col, Controls c) {
		if (row < 0) row = c.getFileList().getSelectedRow();
		String fileName = (String)c.getFileList().getValueAt(row, col);
		if (fileName.contentEquals(STAG_MATCH))
			return null;
		return getFilePath(row, col, c);
	}
	
	/** All GUI media-play calls get funneled down to this. */
	private void doPlay(String filePath, Controls c) throws Exception {
		_l.log(Level.FINE, "View " + filePath + " portion " + c.getViewType().toString());
		JPanel vp = c.getViewPanel();
		Point lhCorner = vp.getLocationOnScreen();
		int width = vp.getWidth(), height = vp.getHeight(),
				x = (int)lhCorner.getX(), y = (int)lhCorner.getY();
		File avFile = new File(filePath);
		ViewController vc = new ViewController((Float)c.getVideoShift().getValue(),
				(String)c.getMergeFormat().getSelectedItem(), workspace);
		vc.view(avFile, c.getViewType(), width, height, x, y);
	}
	
	public void autoPlayIfNeeded(int row, Controls c) throws Exception {
		if (!c.getAutoView().isSelected()) return;
		int vcol = 0, acol = 1;
		String vpath = getSelectedFilePath(row, vcol, c),
				apath = getSelectedFilePath(row, acol, c);
		if (vpath == null || apath == null) return;
		doPlay(vpath, c);
	}
	
	public void play(Controls c) {
		int col = c.getViewType() == ViewController.ViewType.AUDIO ? 1 : 0;
		String playFile = getSelectedFilePath(-1, col, c);
		if (playFile == null) return;
		try {
			doPlay(playFile, c);
		}
		catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	public static GuiSession loadFromCache(String cacheDir, MainFrame mf) throws IOException {
		boolean needTry = true;
		GuiSession guiSession = null;
		while (needTry) {
			try {
				guiSession = new GuiSession(cacheDir);
				needTry = false;
			}
			catch(AVEngine.RequiredComponentMissing rcm) {
				mf.report(rcm);
				JFileChooser jfc = new JFileChooser();
				String dialogTitle = "Select the Folder Containing " + rcm.getComponent(),
						buttonTitle = "Select";
				jfc.setDialogTitle(dialogTitle);
				jfc.setDialogType(JFileChooser.CUSTOM_DIALOG);
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setFileFilter(jfc.getAcceptAllFileFilter());
				jfc.setMultiSelectionEnabled(false);
				if (JFileChooser.APPROVE_OPTION == jfc.showDialog(mf, buttonTitle)) {
					File dir = jfc.getSelectedFile();
					Config cfg = new Config(cacheDir);
					StringBuilder newPath = new StringBuilder(dir.getPath());
					String path = cfg.getProperty(Config.SEARCH_PATH);
					if (path.length() > 0) {
						newPath.append(File.pathSeparator);
						newPath.append(path);
					}
					cfg.setProperty(Config.SEARCH_PATH, newPath.toString());
					cfg.store(cacheDir);
				}
				else {
					System.exit(4);
				}
			}
			catch(Exception e) {
				mf.report(e);
				System.exit(4);
			}
		}
		return guiSession;
	}
}
