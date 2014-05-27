/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

/** Class to connect a JTextField containing a directory path with a "builder" button
 * such that when the button is clicked, a file-selection dialog pops up that allows
 * the user to select one directory, and after the user approves the selection, the
 * text of the path to that directory is used to update the text field. */
class DirectorySelectionAdapter extends MouseAdapter {
	private JButton button;
	private JTextField textField;
	private Component parent;
	DirectorySelectionAdapter(JTextField _textField, JButton _button, Component _parent) {
		button = _button;
		textField = _textField;
		parent = _parent;
		button.addMouseListener(this);
	}
	
	public JTextField getTextField() {
		return textField;
	}
	
	public boolean hasEntry() {
		return (textField.getText().length() > 0);
	}
	
	public File getFile() {
		return new File(textField.getText());
	}
	
	public boolean isOk() {
		File f = getFile();
		return (f.exists() && f.isDirectory());
	}
	
	public boolean isWritable() {
		File f = getFile();
		return (f.exists() && f.isDirectory() && f.canWrite());
	}

	public void mouseClicked(MouseEvent e) {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Select Video Directory");
		jfc.setDialogType(JFileChooser.CUSTOM_DIALOG);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setFileFilter(jfc.getAcceptAllFileFilter());
		jfc.setMultiSelectionEnabled(false);
		if (hasEntry() && isOk())
			jfc.setCurrentDirectory(getFile());
		if (JFileChooser.APPROVE_OPTION == jfc.showDialog(parent, "Select")) {
			File dir = jfc.getSelectedFile();
			textField.setText(dir.getAbsolutePath());
			for (KeyListener kl : textField.getKeyListeners()) {
				kl.keyTyped(null);
			}
		}
	}
}