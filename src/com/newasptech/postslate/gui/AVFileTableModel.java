package com.newasptech.postslate.gui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.newasptech.postslate.AVPair;

public class AVFileTableModel extends AbstractTableModel {
	private final static long serialVersionUID = 1L;
	List<AVPair> avPairs = null;
	public void setFiles(List<AVPair> _avPairs) {
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
		AVPair p = null;
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
		return GuiSession.STAG_MATCH;
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
	public AVPair getPairAt(int row) {
		if (row >= 0 && row < avPairs.size())
			return avPairs.get(row);
		return null;
	}
	public static final String CAMERA_CLIP_HEADER = "Camera", EXTAUDIO_CLIP_HEADER = "Ext. Audio";
}