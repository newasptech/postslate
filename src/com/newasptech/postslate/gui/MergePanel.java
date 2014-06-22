/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
//import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import com.newasptech.postslate.Config;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JCheckBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class MergePanel extends BasePanel {
	@SuppressWarnings("unused")
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.MergePanel");
	private static final long serialVersionUID = 1L;
	private JTextField txtMergePath;
	private DirectorySelectionAdapter dsaMergePath = null;
	private JCheckBox chkRetainAudio = null;
	private JCheckBox chkRetainVideo = null;
	private JCheckBox chkRetainData = null;
	private JCheckBox chkSeparate = null;
	private JSpinner spnVShift = null;
	private JButton btnMerge = null;
	private JComboBox<String> cboMergeFormat;

	/**
	 * Create the panel.
	 */
	public MergePanel(MainFrame f, GuiSession m) {
		super(f, m);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("45dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("25dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				},
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
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
		chkRetainAudio = new JCheckBox("Camera audio");
		chkRetainAudio.setToolTipText("Copy the camera audio stream(s) to the merged output, but with stream indices higher than that of the externally-recorded audio.  When editing, you will have to remember to mute the camera audio stream.  This option can be a useful check for comparing the audio tracks; if you listen to them together, you will likely hear an echo.");
		add(chkRetainAudio, "2, 2, 3, 1");
		
		chkRetainVideo = new JCheckBox("Extra video");
		chkRetainVideo.setToolTipText("Copy any secondary video stream to the merged output");
		add(chkRetainVideo, "6, 2");
		
		chkRetainData = new JCheckBox("Data streams");
		chkRetainData.setToolTipText("Copy data streams to the merged output.");
		add(chkRetainData, "8, 2, 3, 1");
		
		chkSeparate = new JCheckBox("Separate A/V outputs");
		chkSeparate.setToolTipText("When this option is checked, synchronize and trim the external audio with the camera video, but output them a separate files of identical length.");
		add(chkSeparate, "2, 4, 5, 1");
		
		JLabel lblNewLabel = new JLabel("Shift video by constant offset of ");
		add(lblNewLabel, "2, 6, 5, 1");
		
		spnVShift = new JSpinner();
		spnVShift.setModel(new SpinnerNumberModel(new Float(0.0), new Float(-5), new Float(5), new Float(0.01)));
		spnVShift.setToolTipText("At times, it may be necessary to shift video by a constant amount with respect to audio.\n" +
				"The amount needed may vary by output container type or by which version of FFMPEG is installed on your system.\n" +
				"This value may be positive or negative.\n" +
				"If A/V sync consistently looks off, experiment with changing this field before editing many clap points.");
		add(spnVShift, "8, 6");
		
		btnMerge = new JButton("Merge All");
		btnMerge.setEnabled(false);
		btnMerge.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Thread t = new Thread() {
					public void run() {
						try {
							getBackend().getWorkspace().merge(txtMergePath.getText(), (String)cboMergeFormat.getSelectedItem(),
								chkSeparate.isSelected(), chkRetainVideo.isSelected(), chkRetainAudio.isSelected(),
								chkRetainData.isSelected(), (Float)(spnVShift.getValue()), null, null,
								getMainFrame().getProgressMonitor(0, 100, "Merging"));
						}
						catch(Exception ex) {
							report(ex);
						}		
					}
				};
				t.start();
			}
		});
		btnMerge.setToolTipText("Synchronize, trim and merge all clips shown as paired in the list.\n" +
				"To avoid the possibility of data loss, this path must be different from the source paths.");
		add(btnMerge, "2, 10");
		
		JLabel lblNewLabel_1 = new JLabel("seconds");
		add(lblNewLabel_1, "10, 6, left, default");
		
		JLabel lblNewLabel_2 = new JLabel("Output As");
		add(lblNewLabel_2, "2, 8, left, default");
		
		cboMergeFormat = new JComboBox<String>();
		add(cboMergeFormat, "4, 8, 3, 1, fill, default");
		cboMergeFormat.setToolTipText("The output container format: avi, mov, etc.");
		setMergeFormats();
		cboMergeFormat.setSelectedItem(getBackend().getConfig().getProperty(Config.MERGE_FORMAT));
		
		JLabel lblMergePath = new JLabel("to path");
		add(lblMergePath, "4, 10, right, default");
		
		txtMergePath = new JTextField();
		add(txtMergePath, "6, 10, 5, 1, fill, default");
		txtMergePath.setToolTipText("Output the synchronized/trimmed/merged clips to this path/folder.");
		txtMergePath.setColumns(10);
		txtMergePath.getDocument().addDocumentListener(new DocumentListener() {
			private void u() {
				try {
					btnMerge.setEnabled(dsaMergePath.isWritable()
						&& !(getMainFrame().controls().getCameraPath().getFile().getCanonicalPath().contentEquals(dsaMergePath.getFile().getCanonicalPath()))
						&& !(getMainFrame().controls().getExtAudioPath().getFile().getCanonicalPath().contentEquals(dsaMergePath.getFile().getCanonicalPath()))
						);
				}
				catch(IOException ioe) {
					report(ioe);
				}
			}
			public void changedUpdate(DocumentEvent e) { u(); }
			public void insertUpdate(DocumentEvent e) { u(); }
			public void removeUpdate(DocumentEvent e) { u(); }
		});
		
		JButton btnSelectMergePath = new JButton("...");
		btnSelectMergePath.setToolTipText("Click to select the output path/folder.");
		add(btnSelectMergePath, "12, 10");
		
		dsaMergePath = new DirectorySelectionAdapter(txtMergePath, btnSelectMergePath, this);
	}
	
	public JSpinner getVideoShift() {
		return spnVShift;
	}
	
	public JComboBox<String> getMergeFormat() {
		return cboMergeFormat;
	}
	
	public DirectorySelectionAdapter getMergePath() {
		return dsaMergePath;
	}
	
	public JCheckBox getRetainAudio() {
		return chkRetainAudio;
	}
	
	public JCheckBox getRetainVideo() {
		return chkRetainVideo;
	}
	
	public JCheckBox getRetainData() {
		return chkRetainData;
	}
	
	public JCheckBox getSeparate() {
		return chkSeparate;
	}
	
	private void setMergeFormats() {
		GuiSession b = getBackend();
		for (String format : b.getAVEngine().outputFormats()) {
			cboMergeFormat.addItem(format);
		}
	}
}