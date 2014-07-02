/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Iterator;
//import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import com.newasptech.postslate.AVClip;
import com.newasptech.postslate.Config;
import com.newasptech.postslate.audio.Event;

class SyncPanel extends BasePanel {
	@SuppressWarnings("unused")
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.FileViewPanel");
	private static final long serialVersionUID = 1L;
	private ButtonGroup audioEventOrder = new ButtonGroup();
	private JSpinner spnCandidates = new JSpinner();
	private JList<Event> lstVideoClapTime = null;
	private JList<Event> lstAudioClapTime = null;
	private JRadioButton rdbtnScore = null;
	private JRadioButton rdbtnTime = null;
	private WaveGraphPanel panelGraphVideo = null;
	private WaveGraphPanel panelGraphAudio = null;

	/**
	 * Create the panel.
	 */
	public SyncPanel(MainFrame f, GuiSession m) {
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
				},
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
		
		spnCandidates.setModel(new SpinnerNumberModel(10, 10, getSession().getConfig().ivalue(Config.SCAN_EVENTS), 1));
		spnCandidates.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateEventLists();
				/*
				try {
					getSession().startWaveGraphs(-1);
				}
				catch(Exception ex) {
					report(ex);
				}
				*/
			}
		});
		spnCandidates.setToolTipText("How many clap-candidate items to show in the combo boxes?");
		add(spnCandidates, "4, 2");
		
		JLabel lblCandidate = new JLabel("candidates");
		add(lblCandidate, "6, 2, 3, 1");
		
		JLabel lblSortBy = new JLabel("Sort by");
		add(lblSortBy, "10, 2");
		
		rdbtnTime = new JRadioButton("time");
		rdbtnTime.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateEventLists();
			}
		});
		
		rdbtnScore = new JRadioButton("score");
		rdbtnScore.setSelected(true);
		rdbtnScore.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateEventLists();
			}
		});
		rdbtnScore.setToolTipText("Order the clap-candidate items by a score that represents their likelihood of being the real clap.");
		add(rdbtnScore, "12, 2, 3, 1, left, default");
		audioEventOrder.add(rdbtnScore);
		rdbtnTime.setToolTipText("Order the clap-candidate items by time.");
		add(rdbtnTime, "16, 2, 5, 1, left, default");
		audioEventOrder.add(rdbtnTime);
		
		JLabel lblGraphCamera = new JLabel("Camera Audio");
		add(lblGraphCamera, "2, 4, 3, 1");
		
		JLabel lblClapTime = new JLabel("Clap occurs at");
		add(lblClapTime, "12, 4, 7, 1");
		
		panelGraphVideo = new WaveGraphPanel(getSession());
		add(panelGraphVideo, "2, 6, 9, 1, fill, fill");
		
		lstVideoClapTime = new JList<Event>();
                lstVideoClapTime.setModel(new DefaultListModel<Event>());
                lstVideoClapTime.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lstVideoClapTime.setToolTipText("The time at which the clap event occurs, in seconds, with respect to the beginning of the Video source clip.");
                JScrollPane scrVideoClapTime = new JScrollPane(lstVideoClapTime);
		add(scrVideoClapTime, "12, 5, 9, 2, fill, default");
		
		JLabel lblExternal = new JLabel("External Audio");
		add(lblExternal, "2, 8, 3, 1");
		
		panelGraphAudio = new WaveGraphPanel(getSession());
		add(panelGraphAudio, "2, 10, 9, 1, fill, fill");
		
		lstAudioClapTime = new JList<Event>();
                lstAudioClapTime.setModel(new DefaultListModel<Event>());
                lstAudioClapTime.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lstAudioClapTime.setToolTipText("The time at which the clap event occurs, in seconds, with respect to the beginning of the Audio source clip.");
                JScrollPane scrAudioClapTime = new JScrollPane(lstAudioClapTime);
		add(scrAudioClapTime, "12, 9, 9, 2, fill, default");
	}
	
	public JList<Event> getVideoClapList() {
		return lstVideoClapTime;
	}
	
	public JList<Event> getAudioClapList() {
		return lstAudioClapTime;
	}
	
	private ListSelectionListener audioClapListener = null, videoClapListener = null;
	private void disableListSelectionListeners() {
		if (audioClapListener != null)
			lstAudioClapTime.removeListSelectionListener(audioClapListener);
		if (videoClapListener != null)
			lstVideoClapTime.removeListSelectionListener(videoClapListener);
		audioClapListener = null;
		videoClapListener = null;
	}
	
	private void enableListSelectionListeners() {
		audioClapListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int idx = lstAudioClapTime.isSelectedIndex(e.getFirstIndex()) ? e.getFirstIndex() : e.getLastIndex();
					setNewClapPosition(false, lstAudioClapTime.getModel().getElementAt(idx));
				}
			}
		};
		lstAudioClapTime.addListSelectionListener(audioClapListener);
		videoClapListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int idx = lstVideoClapTime.isSelectedIndex(e.getFirstIndex()) ? e.getFirstIndex() : e.getLastIndex();
					setNewClapPosition(true, lstVideoClapTime.getModel().getElementAt(idx));
				}
			}
		};
		lstVideoClapTime.addListSelectionListener(videoClapListener);
	}
	
	public void setClips(AVClip vclip, AVClip aclip) {
		videoClip = vclip;
		audioClip = aclip;
		updateEventLists();
	}
	
	private void setNewClapPosition(boolean isVideo, Event clap) {
		if (isVideo)
			videoClip = getSession().setClapEvent(videoClip, audioClip, isVideo, clap);
		else
			audioClip = getSession().setClapEvent(videoClip, audioClip, isVideo, clap);
	}
	private AVClip videoClip = null, audioClip = null;
	
	private void updateEventLists() {
		int eventCount = (Integer)getMainFrame().controls().getCandidates().getValue();
		boolean scoreOrder = audioEventOrder.isSelected(rdbtnScore.getModel());
		disableListSelectionListeners();
		updateEventList(videoClip, lstVideoClapTime, eventCount, scoreOrder);
		updateEventList(audioClip, lstAudioClapTime, eventCount, scoreOrder);
		enableListSelectionListeners();
	}
	
	private static void updateEventList(AVClip clip, JList<Event> lst, int eventCount, boolean scoreOrder) {
		DefaultListModel<Event> mdl = (DefaultListModel<Event>)lst.getModel();
		mdl.removeAllElements();
		if (clip == null) return;
		for (Iterator<Event> pEvent = clip.getEvents(eventCount, scoreOrder).iterator(); pEvent.hasNext();) {
			mdl.addElement(pEvent.next());
		}
		lst.setSelectedIndex(eventIndex(clip.getOffset(), lst));
	}
	
	private static int eventIndex(float clapPos, JList<Event> lst) {
		int idx = -1, i = 0;
		for (i = 0; i != lst.getModel().getSize(); ++i) {
            if (lst.getModel().getElementAt(i).getTime() == clapPos) {
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
