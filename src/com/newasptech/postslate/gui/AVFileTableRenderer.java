package com.newasptech.postslate.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.newasptech.postslate.AVPair;
import com.newasptech.postslate.MatchBox;

public class AVFileTableRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final Color stagBackgroundColor = new Color(192, 192, 192),
			nonstagBackgroundColor = new Color(255, 255, 255),
			matchForegroundColor = new Color(37, 141, 72), defForegroundColor = new Color(0, 0, 0);
	private GuiSession guiSession;
	public AVFileTableRenderer(GuiSession _session) {
		guiSession = _session;
	}
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		AVFileTableModel m = (AVFileTableModel)table.getModel();
		MatchBox mbox = guiSession.getWorkspace().getMatchBox();
		AVPair pair = m.getPairAt(row);
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