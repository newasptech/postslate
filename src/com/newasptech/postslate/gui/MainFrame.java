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
import com.newasptech.postslate.AVEngine;
import com.newasptech.postslate.Cache;
import com.newasptech.postslate.Cmd;
import com.newasptech.postslate.Config;
import com.newasptech.postslate.gui.BasePanel.ViewType;
import com.newasptech.postslate.util.Misc;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MainFrame extends JFrame {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.Frame");
	private static final long serialVersionUID = 1L;
	private JPanel contentPane = null;
	private ProgressMonitor progressBar = null;
	private FileViewPanel panelFileView = null;
	private SyncPanel panelSync = null;
	private PreviewPanel panelPreview = null;
	private MergePanel panelMerge = null;
	private File propertiesFile;

	/**
	 * Create the frame.
	 */
	public MainFrame(String cacheDir) throws FileNotFoundException, IOException {
		StringBuffer guiPropertiesFile = new StringBuffer(Cache.location(cacheDir).getAbsolutePath());
		guiPropertiesFile.append(System.getProperty("file.separator"));
		guiPropertiesFile.append("gui.properties");
		propertiesFile = new File(guiPropertiesFile.toString());
		
		setTitle("Postslate");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1000, 492);
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
				RowSpec.decode("min(250dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.MIN_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.MIN_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		Backend backend = loadBackend(cacheDir);
		panelFileView = new FileViewPanel(this, backend);
		contentPane.add(panelFileView, "2, 2, 1, 5, fill, fill");
		
		panelSync = new SyncPanel(this, backend);
		contentPane.add(panelSync, "4, 2, fill, fill");
		
		panelPreview = new PreviewPanel(this, backend);
		contentPane.add(panelPreview, "4, 4, fill, fill");
		
		panelMerge = new MergePanel(this, backend);
		contentPane.add(panelMerge, "4, 6, fill, fill");
		
		if (propertiesFile.exists()) {
			Properties p = new Properties();
			_l.log(Level.FINE, "Load GUI control values from " + propertiesFile.toString());
			p.load(new FileInputStream(propertiesFile));
			controls().set(p);
		}
		
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
		
		Image windowIcon = slateImage(); 
		setIconImage(windowIcon);
		setupMenu();
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
		menu.setMnemonic(KeyEvent.VK_A);
		bar.add(menu);
		JMenuItem menuItem = new JMenuItem("About", KeyEvent.VK_A);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
		menuItem.addActionListener(menuListener);
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
		menuItem.addActionListener(menuListener);
		menu.add(menuItem);
		
		setJMenuBar(bar);
	}
	
	private Backend loadBackend(String cacheDir) throws IOException {
		boolean needTry = true;
		Backend backend = null;
		while (needTry) {
			try {
				backend = new Backend(cacheDir);
				needTry = false;
			}
			catch(AVEngine.RequiredComponentMissing rcm) {
				report(rcm);
				JFileChooser jfc = new JFileChooser();
				String dialogTitle = "Select the Folder Containing " + rcm.getComponent(),
						buttonTitle = "Select";
				jfc.setDialogTitle(dialogTitle);
				jfc.setDialogType(JFileChooser.CUSTOM_DIALOG);
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setFileFilter(jfc.getAcceptAllFileFilter());
				jfc.setMultiSelectionEnabled(false);
				if (JFileChooser.APPROVE_OPTION == jfc.showDialog(this, buttonTitle)) {
					File dir = jfc.getSelectedFile();
					Config cfg = new Config(cacheDir);
					StringBuilder newPath = new StringBuilder(dir.getPath());
					String path = cfg.getProperty(Config.SEARCH_PATH);
					if (path.length() > 0) {
						newPath.append(File.pathSeparator);
						newPath.append(path);
					}
					cfg.setProperty(Config.SEARCH_PATH, newPath.toString());
					cfg.store(cacheDir);
				}
				else {
					System.exit(4);
				}
			}
			catch(Exception e) {
				report(e);
				System.exit(4);
			}
		}
		return backend;
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
	
	public class Controls {
		public JCheckBox getAutoView() { return panelPreview.getAutoView(); }
		public DirectorySelectionAdapter getCameraPath() { return panelFileView.getCameraPath(); }
		public DirectorySelectionAdapter getExtAudioPath() { return panelFileView.getExtAudioPath(); }
		public JTable getFileList() { return panelFileView.getFileList(); }
		public PreviewPanel.ViewType getViewType() { return panelPreview.getViewType(); }
		public void setViewType(PreviewPanel.ViewType vt) { panelPreview.setViewType(vt); }
		public JSpinner getCandidates() { return panelSync.getCandidates(); }
		public WaveGraphPanel getVideoGraphPanel() { return panelSync.getVideoGraphPanel(); }
		public WaveGraphPanel getAudioGraphPanel() { return panelSync.getAudioGraphPanel(); }
		public SyncPanel getSyncPanel() { return panelSync; }
		public JTextField getMergeFormat() { return panelMerge.getMergeFormat(); }
		public JSpinner getVideoShift() { return panelMerge.getVideoShift(); }
		public boolean getOrderByScore() { return panelSync.getOrderByScore(); }
		public DirectorySelectionAdapter getMergePath() { return panelMerge.getMergePath(); }
		public JCheckBox getRetainAudio() { return panelMerge.getRetainAudio(); }
		public JCheckBox getRetainVideo() { return panelMerge.getRetainVideo(); }
		public JCheckBox getRetainData() { return panelMerge.getRetainData(); }
		public JCheckBox getSeparate() { return panelMerge.getSeparate(); }

		public Properties asProperties() {
			Properties p = new Properties();
			p.setProperty(AUTO_VIEW, Boolean.toString(getAutoView().isSelected()));
			p.setProperty(CAMERA_PATH, getCameraPath().getTextField().getText());
			p.setProperty(VIEW_TYPE, getViewType().toString());
			p.setProperty(CANDIDATES, getCandidates().getValue().toString());
			p.setProperty(MERGE_FORMAT, getMergeFormat().getText());
			p.setProperty(VIDEO_SHIFT, ((Float)getVideoShift().getValue()).toString());
			p.setProperty(ORDER_BY_SCORE, Boolean.toString(getOrderByScore()));
			p.setProperty(MERGE_PATH, getMergePath().getTextField().getText());
			p.setProperty(RETAIN_AUDIO, Boolean.toString(getRetainAudio().isSelected()));
			p.setProperty(RETAIN_VIDEO, Boolean.toString(getRetainVideo().isSelected()));
			p.setProperty(RETAIN_DATA, Boolean.toString(getRetainData().isSelected()));
			p.setProperty(SEPARATE, Boolean.toString(getSeparate().isSelected()));
			p.setProperty(WINDOW_WIDTH, (new Integer(getWidth())).toString());
			p.setProperty(WINDOW_HEIGHT, (new Integer(getHeight())).toString());
			return p;
		}
		
		public void set(Properties p) {
			try {
				getAutoView().setSelected(new Boolean(p.getProperty(AUTO_VIEW)));
				getCameraPath().getTextField().setText(p.getProperty(CAMERA_PATH));
				setViewType(ViewType.fromString(p.getProperty(VIEW_TYPE)));
				getCandidates().setValue(new Integer(p.getProperty(CANDIDATES)));
				getMergeFormat().setText(p.getProperty(MERGE_FORMAT));
				getVideoShift().setValue(new Float(p.getProperty(VIDEO_SHIFT)));
				panelSync.setOrderByScore(new Boolean(p.getProperty(ORDER_BY_SCORE)));
				getMergePath().getTextField().setText(p.getProperty(MERGE_PATH));
				getRetainAudio().setSelected(new Boolean(p.getProperty(RETAIN_AUDIO)));
				getRetainVideo().setSelected(new Boolean(p.getProperty(RETAIN_VIDEO)));
				getRetainData().setSelected(new Boolean(p.getProperty(RETAIN_DATA)));
				getSeparate().setSelected(new Boolean(p.getProperty(SEPARATE)));
				setSize(new Integer(p.getProperty(WINDOW_WIDTH)),
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
	public Controls controls() { return new Controls(); }

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

	public static final String VERSION="0.1";
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
