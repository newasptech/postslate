package com.newasptech.postslate;

import com.newasptech.postslate.audio.wave.WaveStreamReader;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ProgressMonitor;

/** A Session contains the top-level entities used inside one Postslate process. */
public class Session {
	@SuppressWarnings("unused")
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.Session");
	private Config config = null;
	private Cache cache = null;
	private AVEngine avEngine = null;

	public Session(String cacheDir)
			throws IOException, AVEngine.ComponentCheckFailed, AVEngine.OptionalComponentMissing,
			AVEngine.RequiredComponentMissing {
		config = new Config(cacheDir);
		initLogging();
		cache = new Cache(cacheDir);
		avEngine = new AVEngineFFmpegMPV(config);
		avEngine.check();
	}
	
	public Config getConfig() {
		return config;
	}
	
	public Cache getCache() {
		return cache;
	}
	
	public AVEngine getAVEngine() {
		return avEngine;
	}
	
	public Workspace getWorkspaceFromScan(String vdir, String vspec, String adir,
			String aspec, boolean update, ProgressMonitor m) throws Exception {
		AVDirRef vdref = new AVDirRef(AVDirRef.Type.VIDEO, vdir, vspec, adir);
		AVDirRef adref = new AVDirRef(AVDirRef.Type.AUDIO, adir, aspec, vdir);
		vdref.save(cache, update);
		adref.save(cache, update);
		return new Workspace(vdref, adref, true, this, m);
	}
	
	public Workspace getWorkspaceForPath(String path)
			throws ClassNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		return new Workspace(path, this, null);
	}
	
	public Workspace getWorkspaceForFile(File f)
			throws ClassNotFoundException, InterruptedException, IOException, WaveStreamReader.NotWave {
		assert(f.isFile());
		return new Workspace(f.getParentFile().getCanonicalPath(), this, null);
	}

	private void initLogging() {
		Level logLevel = config.bvalue(Config.DEBUG) ? Level.FINE : Level.WARNING;
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(logLevel);
		Logger l = Logger.getLogger("com.newasptech.postslate");
		l.setLevel(logLevel);
		l.addHandler(ch);
		l.setUseParentHandlers(false);
	}
}
