/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/** AsyncLoader is a wrapper for WaveBuffer that loads a WAV file using a background thread.
 *  Example:
 *  
 *  AsyncLoader wavLoader = new AsyncLoader(FileInputStream(wavFilePath));
 *  // go do something else
 *  WaveBuffer b = wavLoader.getBuffer(); // safe to call
 *  
 *  */
public class AsyncLoader extends Thread {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.FileViewPanel");
	private InputStream istr;
	private boolean finished = false;
	private WaveBuffer buffer = null;
	private Exception cachedException = null;
	private File toDelete = null;
	
	public AsyncLoader(String path, boolean deleteWhenFinished) throws FileNotFoundException {
		istr = new FileInputStream(path);
		if (deleteWhenFinished) {
			toDelete = new File(path);
			toDelete.deleteOnExit();
		}
	}
	
	public AsyncLoader(InputStream _istr) {
		_l.log(Level.FINE, "Start new AsyncLoader");
		istr = _istr;
		start();
	}

	public void run() {
		try {
			WaveStreamReader wavReader = new WaveStreamReader(istr);
			buffer = new WaveBuffer(wavReader);
		}
		catch(Exception e) {
			cachedException = e;
		}
		_l.log(Level.FINE, "Finished buffering WAV stream");
	}
	
	public WaveBuffer getBuffer() throws Exception {
		if (!finished) {
			join();
			finished = true;
			if (cachedException != null)
				throw cachedException;
		}
		return buffer;
	}
	
	public boolean isRunning() {
		return (!finished && getState() != State.TERMINATED); 
	}
	
	public void cancel() {
		_l.log(Level.FINE, "Pre-interrupt AsyncLoader");
		if (getState() != Thread.State.TERMINATED)
			try {
				interrupt();
			}
			catch(Exception e) {
				_l.log(Level.FINE, "Exception thrown from interrupt() call", e);
			}
		_l.log(Level.FINE, "Post-interrupt AsyncLoader");
	}
}
