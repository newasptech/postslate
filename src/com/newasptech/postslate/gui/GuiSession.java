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
import java.awt.Rectangle;
import java.awt.Toolkit;
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
import com.newasptech.postslate.util.Misc;

/** Class for back-end GUI-related A/V manipulation. */
class GuiSession extends Session {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.Backend");
	private Workspace workspace = null;
	private MainFrame mainFrame = null;
	public GuiSession(String cacheDir, MainFrame _mainFrame)
			throws IOException, AVEngine.ComponentCheckFailed, AVEngine.OptionalComponentMissing,
			AVEngine.RequiredComponentMissing {
		super(cacheDir);
		mainFrame = _mainFrame;
	}

	public void loadWorkspace(String path) throws Exception {
		workspace = getWorkspaceForPath(path);
		updateWorkspaceFileList();
	}
	
	public void scanNewWorkspace(String pathCamera, String pathExtAudio, String filemaskVideo,
			String filemaskAudio, boolean update, ProgressMonitor m)
			throws Exception {
		try {
			workspace = getWorkspaceFromScan(pathCamera, filemaskVideo, pathExtAudio, filemaskAudio,
					update, m);
		}
		catch(CancellationException cex) {
			_l.log(Level.INFO, "User canceled the operation");
		}
		updateWorkspaceFileList();
	}
	
	private Controls controls() {
		return mainFrame.controls();
	}
	
	public void clearWorkspace() {
		workspace = null;
		updateWorkspaceFileList();
	}
	
	public Workspace getWorkspace() {
		return workspace;
	}
	
	private void updateWorkspaceFileList() {
		JTable fl = controls().getFileList();
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
	/** Mark or unmark an A/V clip as stag. */
	public void toggleStag(int row, int col) throws Exception {
		if (STAG_MATCH.equals((String)controls().getFileList().getValueAt(row, col))) return;
		AVClipNDir cd = workspace.findClip(new File(getFilePath(row, col)));
		boolean needStag = !workspace.getMatchBox().isStag(cd.clip, cd.dir.getType());
		workspace.getMatchBox().remove(cd.clip);
		if (needStag)
			workspace.getMatchBox().addStag(cd.clip, cd.dir.getType());
		workspace.saveMatches();
		updateWorkspaceFileList();
	}
	
	public void setClapEvent(WaveGraphPanel wgPanel, float clapTime) {
		try {
			SelectedAVClipNDirPair clips = new SelectedAVClipNDirPair(-1);
			boolean videoChanged = controls().getVideoGraphPanel().equals(wgPanel);
			Event[] candidates = (videoChanged) ? clips.vClip.clip.getEvents() : clips.aClip.clip.getEvents();
			int idx = getClapIndex(candidates, clapTime);
			setClapEvent(clips.vClip != null ? clips.vClip.clip : null, clips.aClip != null ? clips.aClip.clip : null,
					videoChanged, candidates[idx]);
			(videoChanged ? controls().getVideoClapList() : controls().getAudioClapList()).setSelectedIndex(idx);
		}
		catch(Exception ex) {
			mainFrame.report(ex);
		}
	}
	
	/** Given an AVClip, change which event represents the clap. 
	 * @param vClip video clip
	 * @param aClip audio clip
	 * @param videoChanged true if the video clip needs to be changed, false otherwise
	 * @param clap an Event representing the new clap position
	 * */
	public AVClip setClapEvent(AVClip vClip, AVClip aClip, boolean videoChanged, Event clap) {
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
    				videoChanged ? getClapIndex(vClip.getEvents(), clap.getTime())
    						: controls().getVideoClapList().getSelectedIndex(),
        			new AVClipNDir(aClip, workspace.getAudioDir()),
        			videoChanged ? controls().getAudioClapList().getSelectedIndex()
        					: getClapIndex(aClip.getEvents(), clap.getTime())
    						);
        	autoPlayIfNeeded(FileViewPanel.SELECTED_ROW);
    	}
    	catch(Exception ex) {
    		_l.log(Level.SEVERE, "Caught an exception while updating matches", ex);
    	}
    	return ret;
	}
	
	private float[] getClapList(Event[] events) {
		int eventCount = Math.min(events.length, (Integer)controls().getCandidates().getValue());
		float[] cl = new float[eventCount];
		for (int i = 0; i != eventCount; ++i)
			cl[i] = events[i].getTime();
		return cl;
	}
	
	private static int getClapIndex(Event[] events, float t) {
		for (int i = 0; i < events.length; ++i) {
			if (t == events[i].getTime())
				return i;
		}
		return -1;
	}
	
	public void startWaveGraphs(int row) throws Exception {
		SelectedAVClipNDirPair clips = new SelectedAVClipNDirPair(row);
		int vClapPosIdx = clips.vClip == null ? 0 : getClapIndex(clips.vClip.clip.getEvents(), clips.vClip.clip.getOffset()),
				aClapPosIdx = clips.aClip == null ? 0 : getClapIndex(clips.aClip.clip.getEvents(), clips.aClip.clip.getOffset());
		updateWaveGraphs(clips.vClip, vClapPosIdx, clips.aClip, aClapPosIdx);
		controls().getSyncPanel().setClips(clips.vClip != null ? clips.vClip.clip : null, clips.aClip != null ? clips.aClip.clip : null);
	}
	
	private void updateWaveGraphs(AVClipNDir vc0, int vClapPosIdx, AVClipNDir acd, int aClapPosIdx)
		throws IOException {
		float vShift = (Float)controls().getVideoShift().getValue();
		WaveGraphPanel vGraphPanel = controls().getVideoGraphPanel();
		WaveGraphPanel aGraphPanel = controls().getAudioGraphPanel();
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
			File camAudio = File.createTempFile("pslm", "." + AsyncLoader.FILE_FORMAT);
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
		if (vcd != null) {
			float[] vClapCandidates = getClapList(vc0.clip.getEvents());
			vGraphPanel.prepareGraph(audioCacheCam, vTrimStart, trimDuration,
				vStartGraphOffset, graphTimeSpan, vClapCandidates, vClapPosIdx);
		}
		if (acd != null) {
			float[] aClapCandidates = getClapList(acd.clip.getEvents());
			aGraphPanel.prepareGraph(audioCacheExt, aTrimStart, trimDuration,
				aStartGraphOffset, graphTimeSpan, aClapCandidates, aClapPosIdx);
		}
	}
	
	private String getFilePath(int row, int col) {
		StringBuilder filePath = new StringBuilder(((col == FileViewPanel.VIDEO_COL) ? workspace.getVideoDir() : workspace.getAudioDir()).getPath());
		filePath.append(System.getProperty("file.separator"));
		filePath.append((String)controls().getFileList().getValueAt(row, col));
		return filePath.toString();
	}
	
	/** Return the selected video/audio file, or null if none is currently selected.
	 * @param row a row index from the file list, or -1 to use the current selection
	 * @param col the column number of the video/audio file (VIDEO_COL or AUDIO_COL)
	 * @param c Controls reference
	 *  */
	private String getSelectedFilePath(int row, int col) {
		if (row < 0) row = controls().getFileList().getSelectedRow();
		String fileName = (String)controls().getFileList().getValueAt(row, col);
		if (fileName.contentEquals(STAG_MATCH))
			return null;
		return getFilePath(row, col);
	}
	
	private ViewController newViewController() {
		return new ViewController((Float)controls().getVideoShift().getValue(),
				(String)controls().getMergeFormat().getSelectedItem(), workspace);
	}
	
	private Rectangle previewArea() {
		JPanel vp = controls().getViewPanel();
		Point lhCorner = vp.getLocationOnScreen();
		int width = vp.getWidth(), height = vp.getHeight(),
				x = (int)lhCorner.getX(), y = (int)lhCorner.getY();
		// On Mac, adjust for the fact that there is no menu bar inside the main window frame
		if (Misc.isMac())
			y -= Toolkit.getDefaultToolkit().getScreenInsets(mainFrame.getGraphicsConfiguration()).top;
		return new Rectangle(x, y, width, height);
	}
	
	public void autoPlayIfNeeded(int row) throws Exception {
		if (!controls().getAutoView().isSelected()) return;
		String vpath = getSelectedFilePath(row, FileViewPanel.VIDEO_COL),
				apath = getSelectedFilePath(row, FileViewPanel.AUDIO_COL);
		if (vpath == null || apath == null) return;
		doPlay(vpath);
	}
	
	/** Play the selected portion (clap/full/video/audio) of the clip pair selected in the file list. */
	public void play() {
		int col = controls().getViewType() == ViewController.ViewType.AUDIO ? FileViewPanel.AUDIO_COL : FileViewPanel.VIDEO_COL;
		String playFile = getSelectedFilePath(FileViewPanel.SELECTED_ROW, col);
		if (playFile == null) return;
		try {
			doPlay(playFile);
		}
		catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/** Start a preview of a "slice" of video or audio. This is used for the graph mouse-over preview.
	 * @param panel the graph panel  */
	public void startSlicePreview(WaveGraphPanel panel, float offset, float _duration) {
		float MIN_DURATION = 0.5f, duration = Math.max(_duration, MIN_DURATION);
		int row = FileViewPanel.SELECTED_ROW, col = (panel.equals(controls().getVideoGraphPanel()) ? FileViewPanel.VIDEO_COL : FileViewPanel.AUDIO_COL);
		String filePath = getSelectedFilePath(row, col);
		_l.log(Level.FINE, "Preview " + (col == FileViewPanel.VIDEO_COL ? "video" : "audio") + " panel from file" +
				filePath + ", offset " + offset + ", duration " + duration);
		Rectangle r = previewArea();
		try {
			AVClipNDir cnd = workspace.findClip(new File(filePath));
			AVClip modClip = null;
			boolean forceAudioOnly = Misc.isMac() && col == FileViewPanel.VIDEO_COL;
			if (forceAudioOnly) {
				String key = getAVEngine().metaKeyName(AVEngine.MetaKey.CODEC_TYPE);
				String value = getAVEngine().metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO);
				int[] indices = new int[]{ cnd.clip.getMeta().findFirstIndex(key, value) };
				modClip = new AVClip(cnd.clip, offset, duration, indices);
			}
			else {
				modClip = new AVClip(cnd.clip, offset, duration);
			}
			AVClipNDir cndMod = new AVClipNDir(modClip, cnd.dir);
			newViewController().view(cndMod, (int)r.getWidth(), (int)r.getHeight(),
					(int)r.getX(), (int)r.getY(), forceAudioOnly, false);
		}
		catch(Exception ex) {
			mainFrame.report(ex);
		}
	}
	
	/** Most GUI media-play calls get funneled down to this. */
	private void doPlay(String filePath) throws Exception {
		_l.log(Level.FINE, "View " + filePath + " portion " + controls().getViewType().toString());
		Rectangle r = previewArea();
		newViewController().view(new File(filePath), controls().getViewType(),
				(int)r.getWidth(), (int)r.getHeight(), (int)r.getX(), (int)r.getY());
	}
	
	/** Clean up any loose ends. Should be called on exit. */
	public void cleanup() {
		AVEngine e = getAVEngine();
		if (e != null)
			e.stopCurrent();
	}
	
	/** Get the currently-selected clips, or the clips from a given row in the file list. */
	class SelectedAVClipNDirPair {
		public AVClipNDir vClip = null, aClip = null;
		public SelectedAVClipNDirPair(int row) throws Exception {
			if (row < 0) row = controls().getFileList().getSelectedRow();
			boolean haveVideo = !(STAG_MATCH.equals((String)controls().getFileList().getValueAt(row, 0)));
			if (haveVideo) vClip = workspace.findClip(new File(getFilePath(row, 0)));
			boolean haveAudio = !(STAG_MATCH.equals((String)controls().getFileList().getValueAt(row, 1)));
			if (haveAudio) aClip = workspace.findClip(new File(getFilePath(row, 1)));
		}
	}

	public static GuiSession loadFromCache(String cacheDir, MainFrame mf) throws IOException {
		boolean needTry = true;
		GuiSession guiSession = null;
		while (needTry) {
			try {
				guiSession = new GuiSession(cacheDir, mf);
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
