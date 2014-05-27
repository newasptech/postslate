/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import javax.swing.ButtonGroup;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

class PreviewPanel extends BasePanel {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.PreviewPanel");
	private static final long serialVersionUID = 1L;
	private JCheckBox chkAutoView = null;
	private ButtonGroup viewType = new ButtonGroup();
	private JRadioButton rdbtnClap = null, rdbtnFull = null, rdbtnVideo = null, rdbtnAudio = null;
	
	/**
	 * Create the panel.
	 */
	public PreviewPanel(MainFrame f, Backend m) {
		super(f, m);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("40dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("min:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
		JButton btnView = new JButton("Play");
		btnView.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				getBackend().play(ViewType.fromString( viewType.getSelection().getActionCommand() ),
						getMainFrame().controls());
			}
		});
		btnView.setToolTipText("Preview the item selected at right.");
		add(btnView, "2, 2");
		
		rdbtnClap = new JRadioButton(ViewType.CLAP.toString().toLowerCase());
		rdbtnClap.setSelected(true);
		rdbtnClap.setToolTipText("Preview just around the sync point, where the clap ought to appear.");
		rdbtnClap.setActionCommand(rdbtnClap.getText());
		add(rdbtnClap, "4, 2, right, default");
		
		rdbtnFull = new JRadioButton(ViewType.FULL.toString().toLowerCase());
		rdbtnFull.setToolTipText("Preview the full merged clip, with video from the camera and externally-recorded audio.");
		rdbtnFull.setActionCommand(rdbtnFull.getText());
		add(rdbtnFull, "6, 2, right, default");
		
		rdbtnVideo = new JRadioButton(ViewType.VIDEO.toString().toLowerCase());
		rdbtnVideo.setToolTipText("Play just the camera video clip.");
		rdbtnVideo.setActionCommand(rdbtnVideo.getText());
		add(rdbtnVideo, "8, 2, right, default");
		
		rdbtnAudio = new JRadioButton(ViewType.AUDIO.toString().toLowerCase());
		rdbtnAudio.setToolTipText("Play just the external audio.");
		rdbtnAudio.setActionCommand(rdbtnAudio.getText());
		add(rdbtnAudio, "10, 2, right, default");
		
		chkAutoView = new JCheckBox("Auto", true);
		chkAutoView.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				boolean b = chkAutoView.isSelected();
				_l.log(Level.FINER, "chkAutoView was clicked. isSelected()="+(b?"true":"false"));
			}
		});
		chkAutoView.setToolTipText("Automatically preview when a new clip is selected, or when the sync point changes.");
		add(chkAutoView, "14, 2");

		viewType.add(rdbtnClap);
		viewType.add(rdbtnFull);
		viewType.add(rdbtnVideo);
		viewType.add(rdbtnAudio);
	}
	
	public JCheckBox getAutoView() { return chkAutoView; }

	public ViewType getViewType() {
		if (rdbtnClap.isSelected())
			return ViewType.CLAP;
		else if (rdbtnFull.isSelected())
			return ViewType.FULL;
		else if (rdbtnVideo.isSelected())
			return ViewType.VIDEO;
		return ViewType.AUDIO;
	}
	
	public void setViewType(ViewType t) {
		switch(t) {
		case CLAP: rdbtnClap.setSelected(true); break;
		case FULL: rdbtnFull.setSelected(true); break;
		case VIDEO: rdbtnVideo.setSelected(true); break;
		case AUDIO: rdbtnAudio.setSelected(true); break;
		}
	}
}