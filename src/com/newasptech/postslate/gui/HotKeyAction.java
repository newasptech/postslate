package com.newasptech.postslate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import com.newasptech.postslate.ViewController;
import com.newasptech.postslate.audio.Event;

public class HotKeyAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.HotKeyAction");
	public static final String NEXT_CLIP = "NextClip", PREV_CLIP = "PrevClip",
			NEXT_VIDEO_CLAP = "NextVideoClap", PREV_VIDEO_CLAP = "PrevVideoClap",
			NEXT_AUDIO_CLAP = "NextAudioClap", PREV_AUDIO_CLAP = "PrevAudioClap",
			PLAY = "Play", PLAY_CLAP = "PlayClap", PLAY_FULL = "PlayFull",
			PLAY_VIDEO = "PlayVideo", PLAY_AUDIO = "PlayAudio";
	private GuiSession guiSession;
	private Controls controls;
	public HotKeyAction(String name, GuiSession _session, Controls _controls) {
		super(name);
		guiSession = _session;
		controls = _controls;
	}
	public void actionPerformed(ActionEvent e) {
		String n = (String)getValue(NAME);
		if (n.contentEquals(NEXT_CLIP)) {
			_l.fine("Next clip");
			changeClip(true);
		}
		else if (n.contentEquals(PREV_CLIP)) {
			_l.fine("Previous clip");
			changeClip(false);
		}
		else if (n.contentEquals(NEXT_VIDEO_CLAP)) {
			_l.fine("Next video clap");
			adjustClap(true, 1);
		}
		else if (n.contentEquals(PREV_VIDEO_CLAP)) {
			_l.fine("Previous video clap");
			adjustClap(true, -1);
		}
		else if (n.contentEquals(NEXT_AUDIO_CLAP)) {
			_l.fine("Next audio clap");
			adjustClap(false, 1);
		}
		else if (n.contentEquals(PREV_AUDIO_CLAP)) {
			_l.fine("Previous audio clap");
			adjustClap(false, -1);
		}
		else if (n.contentEquals(PLAY)) {
			_l.fine("Play");
			guiSession.play();
		}
		else if (n.contentEquals(PLAY_CLAP)) {
			_l.fine("Select play clap");
			controls.setViewType(ViewController.ViewType.CLAP);
		}
		else if (n.contentEquals(PLAY_FULL)) {
			_l.fine("Select play full");
			controls.setViewType(ViewController.ViewType.FULL);
		}
		else if (n.contentEquals(PLAY_VIDEO)) {
			_l.fine("Select play video");
			controls.setViewType(ViewController.ViewType.VIDEO);
		}
		else if (n.contentEquals(PLAY_AUDIO)) {
			_l.fine("Select play audio");
			controls.setViewType(ViewController.ViewType.AUDIO);
		}
	}
	private void adjustClap(boolean isVideo, int delta) {
		JList<Event> l = isVideo ? controls.getSyncPanel().getVideoClapList() 
				: controls.getSyncPanel().getAudioClapList();
		int newIdx = l.getSelectedIndex() + delta;
		if (newIdx >= 0 && newIdx < l.getModel().getSize())
			l.setSelectedIndex(newIdx);
	}
	private void changeClip(boolean next) {
		JTable flist = controls.getFileList();
		int newRow = flist.getSelectedRow() + (next ? 1 : -1);
		if (newRow >= 0 && newRow < flist.getRowCount())
			flist.changeSelection(newRow, 0, false, true);
	}
	public static void attachToPanel(JPanel panel, GuiSession guiSession, Controls controls) {
		InputMap im = panel.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.ALT_MASK, false),
				NEXT_CLIP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.ALT_MASK, false),
				PREV_CLIP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK, false),
				NEXT_VIDEO_CLAP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.ALT_MASK, false),
				PREV_VIDEO_CLAP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK, false),
				NEXT_AUDIO_CLAP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK, false),
				PREV_AUDIO_CLAP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, ActionEvent.ALT_MASK, false),
				PLAY);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK, false),
				PLAY_CLAP);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK, false),
				PLAY_FULL);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK, false),
				PLAY_VIDEO);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK, false),
				PLAY_AUDIO);
		ActionMap am = panel.getActionMap();
		for (String tag : getActionTags()) {
			am.put(tag, new HotKeyAction(tag, guiSession, controls));
		}
	}
	public static String[] getActionTags() {
		return new String[]{ NEXT_CLIP, PREV_CLIP, NEXT_VIDEO_CLAP, PREV_VIDEO_CLAP,
				NEXT_AUDIO_CLAP, PREV_AUDIO_CLAP, PLAY, PLAY_CLAP, PLAY_FULL, PLAY_VIDEO, PLAY_AUDIO };
	}
}