package com.newasptech.postslate.gui;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;

import com.newasptech.postslate.ViewController;
import com.newasptech.postslate.audio.Event;

public class Controls {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.Controls");
	private MainFrame mainFrame;
	private FileViewPanel panelFileView;
	private PreviewPanel panelPreviewCtl;
	private MergePanel panelMerge;
	private SyncPanel panelSync;
	private JPanel panelView;
	
	public Controls(MainFrame _mainFrame, FileViewPanel _panelFileView, PreviewPanel _panelPreviewCtl,
			MergePanel _panelMerge, SyncPanel _panelSync, JPanel _panelView) {
		mainFrame = _mainFrame;
		panelFileView = _panelFileView;
		panelPreviewCtl = _panelPreviewCtl;
		panelMerge = _panelMerge;
		panelSync = _panelSync;
		panelView = _panelView;
	}
	
	public JCheckBox getAutoView() { return panelPreviewCtl.getAutoView(); }
	public DirectorySelectionAdapter getCameraPath() { return panelFileView.getCameraPath(); }
	public DirectorySelectionAdapter getExtAudioPath() { return panelFileView.getExtAudioPath(); }
	public JTable getFileList() { return panelFileView.getFileList(); }
	public ViewController.ViewType getViewType() { return panelPreviewCtl.getViewType(); }
	public void setViewType(ViewController.ViewType vt) { panelPreviewCtl.setViewType(vt); }
	public JSpinner getCandidates() { return panelSync.getCandidates(); }
	public WaveGraphPanel getVideoGraphPanel() { return panelSync.getVideoGraphPanel(); }
	public WaveGraphPanel getAudioGraphPanel() { return panelSync.getAudioGraphPanel(); }
	public SyncPanel getSyncPanel() { return panelSync; }
	public JComboBox<String> getMergeFormat() { return panelMerge.getMergeFormat(); }
	public JSpinner getVideoShift() { return panelMerge.getVideoShift(); }
	public boolean getOrderByScore() { return panelSync.getOrderByScore(); }
	public DirectorySelectionAdapter getMergePath() { return panelMerge.getMergePath(); }
	public JCheckBox getRetainAudio() { return panelMerge.getRetainAudio(); }
	public JCheckBox getRetainVideo() { return panelMerge.getRetainVideo(); }
	public JCheckBox getRetainData() { return panelMerge.getRetainData(); }
	public JCheckBox getSeparate() { return panelMerge.getSeparate(); }
	public JPanel getViewPanel() { return panelView; }
	public JList<Event> getVideoClapList() { return panelSync.getVideoClapList(); }
	public JList<Event> getAudioClapList() { return panelSync.getAudioClapList(); }

	public Properties asProperties() {
		Properties p = new Properties();
		p.setProperty(AUTO_VIEW, Boolean.toString(getAutoView().isSelected()));
		p.setProperty(CAMERA_PATH, getCameraPath().getTextField().getText());
		p.setProperty(VIEW_TYPE, getViewType().toString());
		p.setProperty(CANDIDATES, getCandidates().getValue().toString());
		p.setProperty(MERGE_FORMAT, (String)getMergeFormat().getSelectedItem());
		p.setProperty(VIDEO_SHIFT, ((Float)getVideoShift().getValue()).toString());
		p.setProperty(ORDER_BY_SCORE, Boolean.toString(getOrderByScore()));
		p.setProperty(MERGE_PATH, getMergePath().getTextField().getText());
		p.setProperty(RETAIN_AUDIO, Boolean.toString(getRetainAudio().isSelected()));
		p.setProperty(RETAIN_VIDEO, Boolean.toString(getRetainVideo().isSelected()));
		p.setProperty(RETAIN_DATA, Boolean.toString(getRetainData().isSelected()));
		p.setProperty(SEPARATE, Boolean.toString(getSeparate().isSelected()));
		p.setProperty(WINDOW_WIDTH, (new Integer(mainFrame.getWidth())).toString());
		p.setProperty(WINDOW_HEIGHT, (new Integer(mainFrame.getHeight())).toString());
		return p;
	}
	
	public void set(Properties p) {
		try {
			getAutoView().setSelected(new Boolean(p.getProperty(AUTO_VIEW)));
			getCameraPath().getTextField().setText(p.getProperty(CAMERA_PATH));
			setViewType(ViewController.ViewType.fromString(p.getProperty(VIEW_TYPE)));
			getCandidates().setValue(new Integer(p.getProperty(CANDIDATES)));
			getMergeFormat().setSelectedItem(p.getProperty(MERGE_FORMAT));
			getVideoShift().setValue(new Float(p.getProperty(VIDEO_SHIFT)));
			panelSync.setOrderByScore(new Boolean(p.getProperty(ORDER_BY_SCORE)));
			getMergePath().getTextField().setText(p.getProperty(MERGE_PATH));
			getRetainAudio().setSelected(new Boolean(p.getProperty(RETAIN_AUDIO)));
			getRetainVideo().setSelected(new Boolean(p.getProperty(RETAIN_VIDEO)));
			getRetainData().setSelected(new Boolean(p.getProperty(RETAIN_DATA)));
			getSeparate().setSelected(new Boolean(p.getProperty(SEPARATE)));
			mainFrame.setSize(new Integer(p.getProperty(WINDOW_WIDTH)),
					new Integer(p.getProperty(WINDOW_HEIGHT)));
		}
		catch(Exception ex) {
			_l.log(Level.WARNING, "Could not fully load properties", ex);
		}
	}
	
	public static final String AUTO_VIEW = "com.newasptech.postslate.gui.autoview";
	public static final String CAMERA_PATH = "com.newasptech.postslate.gui.camera_path";
	public static final String VIEW_TYPE = "com.newasptech.postslate.gui.view_type";
	public static final String CANDIDATES = "com.newasptech.postslate.gui.candidates";
	public static final String MERGE_FORMAT = "com.newasptech.postslate.gui.merge_format";
	public static final String VIDEO_SHIFT = "com.newasptech.postslate.gui.video_shift";
	public static final String ORDER_BY_SCORE = "com.newasptech.postslate.gui.order_by_score";
	public static final String MERGE_PATH = "com.newasptech.postslate.gui.merge_path";
	public static final String RETAIN_AUDIO = "com.newasptech.postslate.gui.retain_audio";
	public static final String RETAIN_VIDEO = "com.newasptech.postslate.gui.retain_video";
	public static final String RETAIN_DATA = "com.newasptech.postslate.gui.retain_data";
	public static final String SEPARATE = "com.newasptech.postslate.gui.separate";
	public static final String WINDOW_HEIGHT = "com.newasptech.postslate.gui.window_height";
	public static final String WINDOW_WIDTH = "com.newasptech.postslate.gui.window_width";
}