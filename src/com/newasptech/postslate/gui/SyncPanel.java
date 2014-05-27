/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import com.newasptech.postslate.AVClip;
import com.newasptech.postslate.audio.Event;

class SyncPanel extends BasePanel {
	//private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.FileViewPanel");
	private static final long serialVersionUID = 1L;
	private ButtonGroup audioEventOrder = new ButtonGroup();
	private JSpinner spnCandidates = new JSpinner();
	private JComboBox<Event> cboVideoClapTime = null;
	private JComboBox<Event> cboAudioClapTime = null;
	private JRadioButton rdbtnScore = null;
	private JRadioButton rdbtnTime = null;
	private WaveGraphPanel panelGraphVideo = null;
	private WaveGraphPanel panelGraphAudio = null;

	/**
	 * Create the panel.
	 */
	public SyncPanel(MainFrame f, Backend m) {
		super(f, m);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("25dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("25dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("25dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("10dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("5dlu"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.MIN_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.MIN_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("max(15dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.MIN_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("max(15dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
		JLabel lblShow = new JLabel("Show");
		add(lblShow, "2, 2");
		
		spnCandidates.setModel(new SpinnerNumberModel(10, 0, 200, 1));
		spnCandidates.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateEventLists();
			}
		});
		spnCandidates.setToolTipText("How many clap-candidate items to show in the combo boxes?");
		add(spnCandidates, "4, 2");
		
		JLabel lblCandidate = new JLabel("candidates");
		add(lblCandidate, "6, 2, 3, 1");
		
		JLabel lblSortBy = new JLabel("Sort by");
		add(lblSortBy, "12, 2");
		
		rdbtnScore = new JRadioButton("score");
		rdbtnScore.setSelected(true);
		rdbtnScore.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateEventLists();
			}
		});
		rdbtnScore.setToolTipText("Order the clap-candidate items by a score that represents their likelihood of being the real clap.");
		add(rdbtnScore, "14, 2, 3, 1, left, default");
		audioEventOrder.add(rdbtnScore);
		
		rdbtnTime = new JRadioButton("time");
		rdbtnTime.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateEventLists();
			}
		});
		rdbtnTime.setToolTipText("Order the clap-candidate items by time.");
		add(rdbtnTime, "20, 2, 3, 1, left, default");
		audioEventOrder.add(rdbtnTime);
		
		JLabel lblGraphCamera = new JLabel("Camera Audio");
		add(lblGraphCamera, "2, 4, 3, 1");
		
		JLabel lblClapTime = new JLabel("Clap occurs at");
		add(lblClapTime, "12, 4, 7, 1");
		
		panelGraphVideo = new WaveGraphPanel();
		add(panelGraphVideo, "2, 6, 9, 1, fill, fill");
		
		cboVideoClapTime = new JComboBox<Event>();
		cboVideoClapTime.setToolTipText("The time at which the clap event occurs, in seconds, with respect to the beginning of the Video source clip.");
		add(cboVideoClapTime, "12, 6, 9, 1, fill, default");
		
		JLabel lblExternal = new JLabel("External Audio");
		add(lblExternal, "2, 8, 3, 1");
		
		panelGraphAudio = new WaveGraphPanel();
		add(panelGraphAudio, "2, 10, 9, 1, fill, fill");
		
		cboAudioClapTime = new JComboBox<Event>();
		cboAudioClapTime.setToolTipText("The time at which the clap event occurs, in seconds, with respect to the beginning of the Audio source clip.");
		add(cboAudioClapTime, "12, 10, 9, 1, fill, default");
	}
	
	private ItemListener audioClapListener = null, videoClapListener = null;
	private void disableComboItemListeners() {
		if (audioClapListener != null)
			cboAudioClapTime.removeItemListener(audioClapListener);
		if (videoClapListener != null)
			cboVideoClapTime.removeItemListener(videoClapListener);
		audioClapListener = null;
		videoClapListener = null;
	}
	
	private void enableComboItemListeners() {
		audioClapListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
			    if (e.getStateChange() == ItemEvent.SELECTED) {
			    	setNewClapPosition(false, (Event)e.getItem());
			    }
			}
		};
		cboAudioClapTime.addItemListener(audioClapListener);
		videoClapListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
			    if (e.getStateChange() == ItemEvent.SELECTED) {
			    	setNewClapPosition(true, (Event)e.getItem());
			    }
			}
		};
		cboVideoClapTime.addItemListener(videoClapListener);
	}
	
	public void setClips(AVClip vclip, AVClip aclip) {
		videoClip = vclip;
		audioClip = aclip;
		updateEventLists();
	}
	
	private void setNewClapPosition(boolean isVideo, Event clap) {
		if (isVideo)
			videoClip = getBackend().setClapEvent(videoClip, audioClip, isVideo,
					clap, getMainFrame().controls());
		else
			audioClip = getBackend().setClapEvent(videoClip, audioClip, isVideo,
					clap, getMainFrame().controls());
	}
	private AVClip videoClip = null, audioClip = null;
	
	private void updateEventLists() {
		int eventCount = (Integer)getMainFrame().controls().getCandidates().getValue();
		boolean scoreOrder = audioEventOrder.isSelected(rdbtnScore.getModel());
		disableComboItemListeners();
		updateEventList(videoClip, cboVideoClapTime, eventCount, scoreOrder);
		updateEventList(audioClip, cboAudioClapTime, eventCount, scoreOrder);
		enableComboItemListeners();
	}
	
	private static void updateEventList(AVClip clip, JComboBox<Event> cbo, int eventCount, boolean scoreOrder) {
		cbo.removeAllItems();
		if (clip == null) return;
		for (Iterator<Event> pEvent = clip.getEvents(eventCount, scoreOrder).iterator(); pEvent.hasNext();) {
			cbo.addItem(pEvent.next());
		}
		cbo.setSelectedIndex(eventIndex(clip.getOffset(), cbo));
	}
	
	private static int eventIndex(float clapPos, JComboBox<Event> cbo) {
		int idx = -1, i = 0;
		for (i = 0; i != cbo.getItemCount(); ++i) {
			if (cbo.getItemAt(i).getTime() == clapPos) {
				idx = i;
				break;
			}
		}
		return idx;
	}
	
	public JSpinner getCandidates() { return spnCandidates; }
	public WaveGraphPanel getVideoGraphPanel() { return panelGraphVideo; }
	public WaveGraphPanel getAudioGraphPanel() { return panelGraphAudio; }
	public boolean getOrderByScore() { return audioEventOrder.isSelected(rdbtnScore.getModel()); }
	public void setOrderByScore(boolean b) { rdbtnScore.setSelected(b); }
}
