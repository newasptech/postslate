/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.newasptech.postslate.Cache;
import com.newasptech.postslate.Cmd;
import com.newasptech.postslate.util.Misc;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MainFrame extends JFrame {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.MainFrame");
	private static final long serialVersionUID = 1L;
	private JPanel contentPane = null;
	private ProgressMonitor progressBar = null;
	private FileViewPanel panelFileView = null;
	private SyncPanel panelSync = null;
	private PreviewPanel panelPreviewCtl = null;
	private MergePanel panelMerge = null;
	private JPanel panelView = null;
	private File propertiesFile;
	private GuiSession guiSession;

	/**
	 * Create the frame.
	 */
	public MainFrame(String cacheDir) throws FileNotFoundException, IOException {
		StringBuffer guiPropertiesFile = new StringBuffer(Cache.getUsableDirectory(Cache.location(cacheDir)).getAbsolutePath());
		guiPropertiesFile.append(System.getProperty("file.separator"));
		guiPropertiesFile.append("gui.properties");
		propertiesFile = new File(guiPropertiesFile.toString());
		
		setTitle("Postslate");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1100, 700);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("min(50dlu;default)"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("min(250dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("min(100dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("min(100dlu;default)"),
				FormFactory.RELATED_GAP_ROWSPEC,
				}));

		guiSession = GuiSession.loadFromCache(cacheDir, this);
		panelFileView = new FileViewPanel(this, guiSession);
		contentPane.add(panelFileView, "2, 2, 1, 5, fill, fill");
		
		panelMerge = new MergePanel(this, guiSession);
		contentPane.add(panelMerge, "2, 8, fill, fill");
		
		panelPreviewCtl = new PreviewPanel(this, guiSession);
		contentPane.add(panelPreviewCtl, "4, 2, fill, fill");
		
		panelView = new JPanel();
		panelView.setBackground(new Color(10, 10, 10)); // black background
		contentPane.add(panelView, "4, 4, fill, fill");
		
		panelSync = new SyncPanel(this, guiSession);
		contentPane.add(panelSync, "4, 6, 1, 3, fill, fill");
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (e.getID() == WindowEvent.WINDOW_CLOSING) {
					try {
						_l.log(Level.FINE, "Store GUI control values to " + propertiesFile.toString());
						controls().asProperties().store(new FileOutputStream(propertiesFile), "");
					}
					catch(Exception ex) {
						report(ex);
					}
					dispose();
				}
			}
		});
		HotKeyAction.attachToPanel(contentPane, guiSession, controls());
		
		Image windowIcon = slateImage(); 
		setIconImage(windowIcon);
		setupMenu();
		
		if (propertiesFile.exists()) {
			Properties p = new Properties();
			_l.log(Level.FINE, "Load GUI control values from " + propertiesFile.toString());
			p.load(new FileInputStream(propertiesFile));
			controls().set(p);
		}
	}
	
	public void dispose() {
		if (guiSession != null) {
			guiSession.cleanup();
		}
		super.dispose();
	}
	
	public static Image slateImage() {
		return Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource("img/slate.png"));
	}
	
	class MenuActionListener implements ActionListener {
		private JFrame mainFrame;
		public MenuActionListener(JFrame _mainFrame) {
			mainFrame = _mainFrame;
		}
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().contentEquals("About")) {
				int msgType = JOptionPane.DEFAULT_OPTION;
				Icon icon = new ImageIcon(slateImage());
				JOptionPane.showMessageDialog(mainFrame, ABOUT, ABOUT_TITLE, msgType, icon);
			}
			else if (e.getActionCommand().contentEquals("Quit")) {
				mainFrame.dispose();
			}
		}
	}

	private void setupMenu() {
		ActionListener menuListener = new MenuActionListener(this);
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu(MainFrame.APPLICATION_NAME);
		bar.add(menu);
		JMenuItem menuItem = new JMenuItem("About");
		menuItem.addActionListener(menuListener);
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
		menuItem.addActionListener(menuListener);
		menu.add(menuItem);
		
		setJMenuBar(bar);
	}
	
	public void report(Exception e) {
		_l.log(Level.SEVERE, "Report error", e);
		JOptionPane.showMessageDialog(this, e.getMessage(), "An error occurred",
				JOptionPane.ERROR_MESSAGE);
	}
	
	public ProgressMonitor getProgressMonitor(int min, int max, String msg) {
		if (progressBar != null)
			throw new RuntimeException("progressBar already exists");
		progressBar = new PSProgressMonitor(this, msg, min, max);
		return progressBar;
	}
	
	protected void finishProgressMonitor() {
		assert(progressBar != null);
		progressBar = null;
	}
	
	public class PSProgressMonitor extends ProgressMonitor {
		public PSProgressMonitor(MainFrame mainFrame, String msg, int min, int max) {
			super(mainFrame, msg, "", min, max);
		}
		public void close() {
			finishProgressMonitor();
			super.close();
		}
	}
	
	public Controls controls() { return new Controls(this, panelFileView, panelPreviewCtl,
			panelMerge, panelSync, panelView); }

	private static void realMain(String[] args) throws Exception {
		LongOpt[] longopts = new LongOpt[] {
			new LongOpt("cache", LongOpt.REQUIRED_ARGUMENT, null, Cmd.OPT_CACHE)
		};
		Getopt g = new Getopt("com.newasptech.postslate.gui.MainFrame", args,
				new String(new char[]{Cmd.OPT_CACHE, ':', Cmd.OPT_HELP}), longopts);
		g.setOpterr(false);
		int c;
		String cacheDir = null;
		while ((c = g.getopt()) != -1) {
			switch (c)
			{
			case Cmd.OPT_CACHE:
				cacheDir = g.getOptarg();
				break;
			default:
				Cmd.main(args);
			}
		}
		if (g.getOptind() < args.length) {
			Cmd.main(args);
		}
		else {
			FrameRunner fr = new FrameRunner(cacheDir);
			EventQueue.invokeLater(fr);
		}
	}
	
	public static final String APPLICATION_NAME = "Postslate";
	static class FrameRunner implements Runnable {
		private String cacheDir;
		public FrameRunner(String _cacheDir) throws Exception {
			cacheDir = _cacheDir;
			preGuiInit();
		}
		public void run() {
			try {
				MainFrame mainFrame = new MainFrame(cacheDir);
				mainFrame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		private void preGuiInit()
				throws ClassNotFoundException, IllegalAccessException, InstantiationException, UnsupportedLookAndFeelException {
			// set the name of the System menu on OSX
			if (Misc.isMac()) {
				// take the menu bar off the jframe
				System.setProperty("apple.laf.useScreenMenuBar", "true");
			}
			// set the look and feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
	}

	public static final String VERSION="0.2.0";
	public static final String ABOUT_TITLE = "About Postslate",
			ABOUT = "Postslate - a tool to synchronize slate claps from\n" +
			"separately-recorded audio and video clips.\n" +
			"\n" +
			"Version " + VERSION +
			"\n" +
			"Copyright (C) 2014 by New Aspect Technologies, Inc.\n" +
			"All righs reserved.\n" +
			"\n" +
			"This software and its source code are made available\n" +
			"under the terms of the GNU Public License, version 3.\n" +
			"See the LICENSE.txt file included with the source code,\n" +
			"or visit http://www.gnu.org/licenses/gpl.html\n" +
			"";
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			realMain(args);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
			System.exit(2);
		}
	}
}
