/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

import com.newasptech.postslate.audio.wave.AsyncLoader;
import com.newasptech.postslate.audio.wave.WaveRenderer;
import com.newasptech.postslate.audio.wave.WaveBuffer;

class WaveGraphPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.gui.WaveGraphPanel");
	private AsyncLoader wavLoader = null;
	
	/** The offset, relative to the start of the clip, representing the trimmed start time. */
	private float trimStartTime;
	/** The duration of the trimmed clip */
	private float trimDuration;
	/** The offset of the clap, relative to the start of the raw clip, not the trimmed clip. */
	private float clapPos;
	/** The "left margin", so to speak: The offset by which the start of the raw clip should be shifted
	 *  within the graph. (This allows the two graphs to be aligned on their clap positions.)  */
	private float graphStartOffset;
	/** The span of time represented by the entire width of the graph. */
	private float graphTimeSpan;
	
	public void nullifyLoader() {
		if (wavLoader != null) {
			wavLoader.cancel();
			wavLoader = null;
			repaint();
		}		
	}
	
	public void prepareGraph(AsyncLoader _wavLoader, float _trimStartTime, float _trimDuration,
			float _clapPos,	float _graphStartOffset, float _graphTimeSpan) {
		nullifyLoader();
		wavLoader = _wavLoader;
		trimStartTime = _trimStartTime;
		trimDuration = _trimDuration;
		clapPos = _clapPos;
		graphStartOffset = _graphStartOffset;
		graphTimeSpan = _graphTimeSpan;
		repaint();
	}

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (wavLoader == null) return;
        if (wavLoader.isRunning()) {
        	repaint();
        	return;
        }
    	try {
        	WaveBuffer b = wavLoader.getBuffer();
        	WaveRenderer gr = new WaveRenderer(b);
        	WaveRenderer.HorizDim hdim = new WaveRenderer.HorizDim(getWidth(), trimStartTime,
        			trimDuration, clapPos, graphStartOffset, graphTimeSpan);
        	_l.log(Level.FINE, hdim.toString());
        	BufferedImage image = gr.renderWaveform(hdim, getHeight());
        	g.drawImage(image, 0, 0, null);
        	System.runFinalization();
        	System.gc();
        	_l.log(Level.FINE, "After garbage collection, total available memory is " + Runtime.getRuntime().freeMemory());
    	}
    	catch(Exception e) {
    		_l.log(Level.SEVERE, "Caught an exception while generating the graph", e);
    	}
    }
}