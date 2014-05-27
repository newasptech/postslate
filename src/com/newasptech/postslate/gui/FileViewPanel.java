/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.ProgressMonitor;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.newasptech.postslate.AVDirRef;
import com.newasptech.postslate.Config;
import com.newasptech.postslate.MatchBox;
import com.newasptech.postslate.Workspace;

class FileViewPanel extends BasePanel {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.FileViewPanel");
	private static final long serialVersionUID = 1L;
	private JTextField txtCameraPath = null;
	private JTextField txtExtAudioPath = null;
	private JButton btnScan = null;
	private JComboBox<AudioFileMaskOptions> cboExtAudioNameMask = null;
	private JComboBox<VideoFileMaskOptions> cboCameraNameMask = null;
	private JTable listFiles = null;
	private JScrollPane listFilesScrollPane = null;
	private DirectorySelectionAdapter dsaCameraPath = null;
	private DirectorySelectionAdapter dsaExtAudioPath = null;
	
	/**
	 * Create the panel.
	 */
	public FileViewPanel(MainFrame f, Backend m) {
		super(f, m);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
		btnScan = new JButton(BTN_LABEL_SCAN);
		btnScan.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				scanClicked();
				if (!e.isConsumed())
					e.consume();
			}
		});
		btnScan.setToolTipText("Scan the source paths/folders and analyze video/audio clips found there.\n" +
				"Note: postslate will never delete or modify these files.");
		btnScan.setEnabled(false);
		add(btnScan, "6, 2");
		
		JLabel lblCameraPath = new JLabel("Camera Video Path");
		add(lblCameraPath, "2, 4");
		
		JButton btnSelectCameraPath = new JButton("...");
		btnSelectCameraPath.setToolTipText("Click to select a path/folder for camera video files.");
		add(btnSelectCameraPath, "4, 4");
		
		JLabel lblCameraNameMask = new JLabel("Name mask");
		add(lblCameraNameMask, "6, 4");
		
		JLabel lblExtAudioPath = new JLabel("External Audio Path");
		add(lblExtAudioPath, "2, 8");
		
		JButton btnSelectExtAudioPath = new JButton("...");
		btnSelectExtAudioPath.setToolTipText("Click to select a path/folder for external audio files.");
		add(btnSelectExtAudioPath, "4, 8");
		
		JLabel lblExtAudioNameMask = new JLabel("Name mask");
		add(lblExtAudioNameMask, "6, 8");
		
		txtCameraPath = new JTextField();
		add(txtCameraPath, "2, 6, 3, 1, fill, default");
		txtCameraPath.setToolTipText("The path/folder where camera video files are stored.");
		txtCameraPath.setColumns(10);
		
		txtExtAudioPath = new JTextField();
		add(txtExtAudioPath, "2, 10, 3, 1, fill, default");
		txtExtAudioPath.setToolTipText("The path/folder where external audio clips are stored.\n" +
				"This is likely a different path/folder from where the camera video is, but may be the same.");
		txtExtAudioPath.setColumns(10);
		enablePathListeners();
		
		cboCameraNameMask = new JComboBox<VideoFileMaskOptions>();
		cboCameraNameMask.setModel(new DefaultComboBoxModel<VideoFileMaskOptions>(VideoFileMaskOptions.values()));
		cboCameraNameMask.setToolTipText("Filename filter for video files.\n" +
				"Customize this by editing the settings in\n" + getBackend().getConfig().saveloc(getBackend().getCache().getBasedir()));
		add(cboCameraNameMask, "6, 6, fill, default");
		
		cboExtAudioNameMask = new JComboBox<AudioFileMaskOptions>();
		cboExtAudioNameMask.setModel(new DefaultComboBoxModel<AudioFileMaskOptions>(AudioFileMaskOptions.values()));
		cboExtAudioNameMask.setToolTipText("Filename filter for audio files.\n" +
				"Customize this by editing the settings in\n" + getBackend().getConfig().saveloc(getBackend().getCache().getBasedir()));
		add(cboExtAudioNameMask, "6, 10, fill, default");
		
		listFiles = new JTable(new AVFileTableModel());
		listFiles.setAutoCreateColumnsFromModel(true);
		listFiles.setCellSelectionEnabled(false);
		listFiles.setColumnSelectionAllowed(false);
		listFiles.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listFiles.setDragEnabled(false);
		listFiles.setRowSelectionAllowed(true);
		listFiles.setShowGrid(false);
		listFiles.addMouseListener(new AVFileTableListener());
		listFilesScrollPane = new JScrollPane(listFiles, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listFiles.setFillsViewportHeight(true);
		listFiles.getTableHeader().setReorderingAllowed(false);
		listFiles.setToolTipText("Click on a row to view waveforms and optionally preview the match.\n" +
				"Control-click on an item to toggle it as stag (i.e., not matched with another clip).");
		add(listFilesScrollPane, "2, 12, 5, 1, fill, fill");

		dsaCameraPath = new DirectorySelectionAdapter(txtCameraPath, btnSelectCameraPath, this);
		dsaExtAudioPath = new DirectorySelectionAdapter(txtExtAudioPath, btnSelectExtAudioPath, this);
	}
	
	public static enum AudioFileMaskOptions {
		WAV_FILES("WAV files", Config.FILESPEC_AUDIO_DEF),
		ALL_FILES("All files", "*");
		private String label;
		public String filemask;
		AudioFileMaskOptions(String _label, String _filemask) {
			label = _label;
			filemask = _filemask;
		}
		public String toString() { return label; }
	}
	
	public enum VideoFileMaskOptions {
		VIDEO_FILES("Video files", Config.FILESPEC_VIDEO_DEF),
		ALL_FILES("All files", "*");
		private String label;
		public String filemask;
		VideoFileMaskOptions(String _label, String _filemask) {
			label = _label;
			filemask = _filemask;
		}
		public String toString() { return label; }
	};
	
	private DocumentListener doclisExtAudio = null, doclisCameraPath = null;
	private void enablePathListeners() {
		if (doclisExtAudio == null) {
		doclisExtAudio = new DocumentListener() {
			private void u() {
				changedPath(AVDirRef.Type.AUDIO, dsaExtAudioPath, dsaCameraPath);
				
			}
			public void changedUpdate(DocumentEvent e) { u(); }
			public void insertUpdate(DocumentEvent e) { u(); }
			public void removeUpdate(DocumentEvent e) { u(); }
		};
		txtExtAudioPath.getDocument().addDocumentListener(doclisExtAudio);
		}
		if (doclisCameraPath == null) {
		doclisCameraPath = new DocumentListener() {
			private void u() {
				changedPath(AVDirRef.Type.VIDEO, dsaCameraPath, dsaExtAudioPath);
				
			}
			public void changedUpdate(DocumentEvent e) { u(); }
			public void insertUpdate(DocumentEvent e) { u(); }
			public void removeUpdate(DocumentEvent e) { u(); }
		};
		txtCameraPath.getDocument().addDocumentListener(doclisCameraPath);
		}
	}
	private void disablePathListeners() {
		if (doclisExtAudio != null) {
			txtExtAudioPath.getDocument().removeDocumentListener(doclisExtAudio);
			doclisExtAudio = null;
		}
		if (doclisCameraPath != null) {
			txtCameraPath.getDocument().removeDocumentListener(doclisCameraPath);
			doclisCameraPath = null;
		}
	}
	
	/** Given a JTextField that contains a directory path, return:
	 * null if the directory path does not exist
	 * false if the path exists, but there is no cache for that location
	 * true if the path exists and there is a cache */
	private enum DirectoryState {
		/** Directory does not exist */
		UNUSABLE,
		/** Directory exists, but has never been scanned */
		USABLE,
		/** Directory has been scanned and results are cached. */
		SCANNED
	};
	private DirectoryState getState(DirectorySelectionAdapter dsa, AVDirRef.Type type) {
		DirectoryState retval = DirectoryState.UNUSABLE;
		if (dsa.isOk()) {
			retval = DirectoryState.USABLE;
			String fileCachePath = getBackend().getCache().indexForFiles(dsa.getFile().getAbsolutePath(), type);
			if ((new File(fileCachePath)).exists())
				retval = DirectoryState.SCANNED;
		}
		return retval;
	}
	
	private static final String BTN_LABEL_SCAN = "Scan", BTN_LABEL_RESCAN = "Re-scan";
	private void scanClicked() {
		boolean update = btnScan.getText().equals(BTN_LABEL_RESCAN);
		ProgressMonitor m = getMainFrame().getProgressMonitor(0, 100, "Scanning Files");
		ScanThread t = new ScanThread(getBackend(), dsaCameraPath.getFile().getAbsolutePath(),
				dsaExtAudioPath.getFile().getAbsolutePath(),
				((VideoFileMaskOptions)cboCameraNameMask.getSelectedItem()).filemask,
				((AudioFileMaskOptions)cboExtAudioNameMask.getSelectedItem()).filemask,
				update, m, getMainFrame().controls());
		t.start();		
	}
	
	class ScanThread extends Thread {
		private Backend backend;
		private String cameraPath, extAudioPath, vFilemask, aFilemask;
		private boolean update;
		private ProgressMonitor m;
		private MainFrame.Controls c;
		public ScanThread(Backend _backend, String _cameraPath, String _extAudioPath, String _vFilemask, String _aFilemask, boolean _update, ProgressMonitor _m, MainFrame.Controls _c) {
			backend = _backend;
			cameraPath = _cameraPath;
			extAudioPath = _extAudioPath;
			vFilemask = _vFilemask;
			aFilemask = _aFilemask;
			update = _update;
			m = _m;
			c = _c;
		}
		public void run() {
			try {
				backend.scanNewWorkspace(cameraPath, extAudioPath, vFilemask, aFilemask, update, m, c);
			}
			catch(Exception ex) {
				report(ex);
			}
			finally {
				m.close();
			}
			btnScan.setText(BTN_LABEL_RESCAN);
		}
	}
	
	private void changedPath(AVDirRef.Type type0, DirectorySelectionAdapter txt0, DirectorySelectionAdapter txt1) {
		Workspace w = null;
		DirectoryState state0 = getState(txt0, type0);
		_l.log(Level.FINE, "Directory " + txt0.getFile().getAbsolutePath() + " has state " + state0.toString());
		switch(state0) {
		case UNUSABLE:
		case USABLE:
			getBackend().clearWorkspace(getMainFrame().controls());
			btnScan.setText(BTN_LABEL_SCAN);
			btnScan.setEnabled(state0 == DirectoryState.USABLE
					&& getState(txt1, type0==AVDirRef.Type.VIDEO?AVDirRef.Type.AUDIO:AVDirRef.Type.VIDEO) == DirectoryState.USABLE);
			break;
		case SCANNED:
			try {
				w = getBackend().loadWorkspace(txt0.getFile().getAbsolutePath(), getMainFrame().controls());
			}
			catch(Exception e) {
				report(e);
			}
		}
		if (w != null) {
			disablePathListeners();
			txt1.getTextField().setText((type0 == AVDirRef.Type.VIDEO ? w.getAudioDir() : w.getVideoDir()).getPath());
			enablePathListeners();
			btnScan.setText(BTN_LABEL_RESCAN);
			btnScan.setEnabled(true);
		}
	}
	
	class AVFileTableListener extends MouseAdapter {
		
		public void mouseClicked(MouseEvent e) {
			int row = listFiles.rowAtPoint(new Point(e.getX(), e.getY()));
			int col = listFiles.columnAtPoint(new Point(e.getX(), e.getY()));
			int selectOffMask = MouseEvent.CTRL_DOWN_MASK;
			int stagOnMask = MouseEvent.CTRL_DOWN_MASK;
			try {
				_l.log(Level.FINE, "getModifiersEx() == " + e.getModifiersEx() + ", selectOffMask == " + selectOffMask + ", stagOnMask ==" + stagOnMask);
				if ((e.getModifiersEx() & selectOffMask) != selectOffMask) {
					getBackend().startWaveGraphs(getMainFrame().controls(), row);
					getBackend().autoPlayIfNeeded(row, getMainFrame().controls());
				}
				else if ((e.getModifiersEx() & stagOnMask) == stagOnMask){
					getBackend().toggleStag(getMainFrame().controls(), row, col);
				}
			}
			catch(Exception ex) {
				report(ex);
			}
		}
	}
	
	public static class AVFileTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		private static final Color stagBackgroundColor = new Color(192, 192, 192),
				nonstagBackgroundColor = new Color(255, 255, 255),
				matchForegroundColor = new Color(37, 141, 72), defForegroundColor = new Color(0, 0, 0);
		private Backend backend;
		public AVFileTableRenderer(Backend _backend) {
			backend = _backend;
		}
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			AVFileTableModel m = (AVFileTableModel)table.getModel();
			MatchBox mbox = backend.getWorkspace().getMatchBox();
			Workspace.AVPair pair = m.getPairAt(row);
			if (
				(pair.video() != null && mbox.isStagVideo(pair.video()))
				|| (pair.audio() != null && mbox.isStagAudio(pair.audio()))
				) {
				setBackground(stagBackgroundColor);
				setForeground(defForegroundColor);
			}
			else {
				if (pair.video() != null && mbox.hasMatchForVideo(pair.video()))
					setForeground(matchForegroundColor);
				else
					setForeground(defForegroundColor);
				setBackground(nonstagBackgroundColor);
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}
	
	public static class AVFileTableModel extends AbstractTableModel {
		private final static long serialVersionUID = 1L;
		List<Workspace.AVPair> avPairs = null;
		public void setFiles(List<Workspace.AVPair> _avPairs) {
			avPairs = _avPairs;
		}
		public int getRowCount() {
			if (avPairs == null) return 0;
			return avPairs.size();
		}
		public int getColumnCount() {
			return 2;
		}
		public Object getValueAt(int row, int column) {
			Workspace.AVPair p = null;
			if (avPairs != null && row >= 0 && row < avPairs.size()) {
				p = avPairs.get(row);
				switch(column) {
				case 0:
					if (p.video() != null)
						return p.video().getName();
					break;
				case 1:
					if (p.audio() != null)
						return p.audio().getName();
					break;
				}
			}
			return Backend.STAG_MATCH;
		}
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
			case 0:
				return CAMERA_CLIP_HEADER;
			case 1:
				return EXTAUDIO_CLIP_HEADER;
			default:
				assert(false);
			}
			return null;
		}
		public Workspace.AVPair getPairAt(int row) {
			if (row >= 0 && row < avPairs.size())
				return avPairs.get(row);
			return null;
		}
		public static final String CAMERA_CLIP_HEADER = "Camera", EXTAUDIO_CLIP_HEADER = "Ext. Audio";
	}
	
	public JTable getFileList() { return listFiles; }
	public DirectorySelectionAdapter getCameraPath() { return dsaCameraPath; }
	public DirectorySelectionAdapter getExtAudioPath() { return dsaExtAudioPath; }
}
