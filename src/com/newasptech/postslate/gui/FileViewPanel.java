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
import java.awt.Point;
import java.io.File;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.newasptech.postslate.AVDirRef;
import com.newasptech.postslate.Config;
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
	/** The index of the table column containing video files. */
	public static final int VIDEO_COL = 0;
	/** The index of the table column containing audio files. */
	public static final int AUDIO_COL = 1;
	/** The row number used to indicate "use whatever row is selected" */
	public static final int SELECTED_ROW = -1;
	
	/**
	 * Create the panel.
	 */
	public FileViewPanel(MainFrame f, GuiSession m) {
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
				"Customize this by editing the settings in\n" + getSession().getConfig().saveloc(getSession().getCache().getBasedir()));
		add(cboCameraNameMask, "6, 6, fill, default");
		
		cboExtAudioNameMask = new JComboBox<AudioFileMaskOptions>();
		cboExtAudioNameMask.setModel(new DefaultComboBoxModel<AudioFileMaskOptions>(AudioFileMaskOptions.values()));
		cboExtAudioNameMask.setToolTipText("Filename filter for audio files.\n" +
				"Customize this by editing the settings in\n" + getSession().getConfig().saveloc(getSession().getCache().getBasedir()));
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
		listFiles.getSelectionModel().addListSelectionListener(new AVFileTableListener());
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
			String fileCachePath = getSession().getCache().indexForFiles(dsa.getFile().getAbsolutePath(), type);
			if ((new File(fileCachePath)).exists())
				retval = DirectoryState.SCANNED;
		}
		return retval;
	}
	
	private void changedPath(AVDirRef.Type type0, DirectorySelectionAdapter txt0, DirectorySelectionAdapter txt1) {
		Workspace w = null;
		DirectoryState state0 = getState(txt0, type0);
		_l.log(Level.FINE, "Directory " + txt0.getFile().getAbsolutePath() + " has state " + state0.toString());
		switch(state0) {
		case UNUSABLE:
		case USABLE:
			getSession().clearWorkspace();
			btnScan.setText(BTN_LABEL_SCAN);
			btnScan.setEnabled(state0 == DirectoryState.USABLE
					&& getState(txt1, type0 == AVDirRef.Type.VIDEO ? AVDirRef.Type.AUDIO : AVDirRef.Type.VIDEO) == DirectoryState.USABLE);
			break;
		case SCANNED:
			try {
				getSession().loadWorkspace(txt0.getFile().getAbsolutePath());
				w = getSession().getWorkspace();
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
	
	public JTable getFileList() { return listFiles; }
	public DirectorySelectionAdapter getCameraPath() { return dsaCameraPath; }
	public DirectorySelectionAdapter getExtAudioPath() { return dsaExtAudioPath; }
	
	private static final String BTN_LABEL_SCAN = "Scan", BTN_LABEL_RESCAN = "Re-scan";
	private void scanClicked() {
		Thread t = new Thread() {
			public void run() {
				ProgressMonitor m = getMainFrame().getProgressMonitor(0, 100, "Scanning Files");
				boolean update = btnScan.getText().equals(BTN_LABEL_RESCAN);
				try {
					getSession().scanNewWorkspace(dsaCameraPath.getFile().getAbsolutePath(),
						dsaExtAudioPath.getFile().getAbsolutePath(),
						((VideoFileMaskOptions)cboCameraNameMask.getSelectedItem()).filemask,
						((AudioFileMaskOptions)cboExtAudioNameMask.getSelectedItem()).filemask,
						update, m);
				}
				catch(Exception ex) {
					report(ex);
				}
				finally {
					m.close();
				}
				btnScan.setText(BTN_LABEL_RESCAN);
			}
		};
		t.start();		
	}

	class AVFileTableListener extends MouseAdapter implements ListSelectionListener {
		public void mouseClicked(MouseEvent e) {
			int row = listFiles.rowAtPoint(new Point(e.getX(), e.getY()));
			int col = listFiles.columnAtPoint(new Point(e.getX(), e.getY()));
			int stagOnMask = MouseEvent.CTRL_DOWN_MASK;
			try {
				if ((e.getModifiersEx() & stagOnMask) == stagOnMask)
					getSession().toggleStag(row, col);
			}
			catch(Exception ex) {
				report(ex);
			}
		}
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) return;
			int row = listFiles.getSelectedRow();
			try {
				getSession().startWaveGraphs(row);
				getSession().autoPlayIfNeeded(row);
			}
			catch(Exception ex) {
				report(ex);
			}
		}
	}
}
