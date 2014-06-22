/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

//import java.util.logging.Level;
//import java.util.logging.Logger;

import javax.swing.JPanel;

class BasePanel extends JPanel {
	//private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.BasePanel");
	private static final long serialVersionUID = 1L;
	private MainFrame mainFrame = null;
	private GuiSession guiSession = null;

	public BasePanel() {
		super();
	}

	public BasePanel(MainFrame f, GuiSession m) {
		super();
		mainFrame = f;
		guiSession = m;
	}
	
	public MainFrame getMainFrame() {
		return mainFrame;
	}
	
	public GuiSession getSession() {
		return guiSession;
	}
	
	public void report(Exception ex) {
		getMainFrame().report(ex);
	}
}
