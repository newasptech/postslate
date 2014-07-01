/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
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
	/** The "left margin", so to speak: The offset by which the start of the raw clip should be shifted
	 *  within the graph. (This allows the two graphs to be aligned on their clap positions.)  */
	private float graphStartOffset;
	/** The span of time represented by the entire width of the graph. */
	private float graphTimeSpan;
	private float[] clapCandidates;
	/** The offset of the clap, relative to the start of the raw clip, not the trimmed clip. */
	private int clapPosIdx;
	private Map<Rectangle, Float> clapTickMap = null;
	private WaveRenderer.HorizDim hdim = null;
	private int graphEndInPixels = 0;
	
	public WaveGraphPanel() {
		super();
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					handleLBClick(e);
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				handleMouseOver(e);
			}
		});
	}
	
	public void nullifyLoader() {
		if (wavLoader != null) {
			wavLoader.cancel();
			wavLoader = null;
			repaint();
		}		
	}
	
	public void prepareGraph(AsyncLoader _wavLoader, float _trimStartTime, float _trimDuration,
			float _graphStartOffset, float _graphTimeSpan, float[] _clapCandidates, int _clapPosIdx) {
		nullifyLoader();
		wavLoader = _wavLoader;
		trimStartTime = _trimStartTime;
		trimDuration = _trimDuration;
		graphStartOffset = _graphStartOffset;
		graphTimeSpan = _graphTimeSpan;
		clapCandidates = _clapCandidates;
		clapPosIdx = _clapPosIdx;
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
        	hdim = new WaveRenderer.HorizDim(getWidth(), trimStartTime,
        			trimDuration, graphStartOffset, graphTimeSpan, clapCandidates, clapPosIdx);
        	_l.log(Level.FINE, hdim.toString());
        	BufferedImage image = gr.renderWaveform(hdim, getHeight());
        	g.drawImage(image, 0, 0, null);
        	clapTickMap = gr.getClapTickMap();
        	graphEndInPixels = gr.getGraphWidthInPixels() + hdim.getGraphStartInPixels();
        	System.runFinalization();
        	System.gc();
        	_l.log(Level.FINE, "After garbage collection, total available memory is " + Runtime.getRuntime().freeMemory());
    	}
    	catch(Exception e) {
    		_l.log(Level.SEVERE, "Caught an exception while generating the graph", e);
    	}
    }
	
	private void handleLBClick(MouseEvent e) {
		_l.log(Level.FINE, "Received a left-mouse click at x=" + e.getX() + ", y=" + e.getY());
		if (clapTickMap == null) return;
		for (Iterator<Rectangle> pR = clapTickMap.keySet().iterator(); pR.hasNext();) {
			Rectangle r = pR.next();
			if (r.contains(e.getX(), e.getY())) {
				_l.log(Level.FINE, "Point (" + e.getX() + ", " + e.getY() + ") corresponds to time " + clapTickMap.get(r));
				// WIP
			}
		}
	}
	
	private void handleMouseOver(MouseEvent e) {
		_l.log(Level.FINE, "Received a mouse-over event at (" + e.getX() + ", " + e.getY() + ")");
		if (hdim == null) return;
		if (e.getX() < hdim.getGraphStartInPixels() || e.getX() >= graphEndInPixels) {
			_l.log(Level.FINE, "This position is off the graph");
		}
		else {
			float t = hdim.getTimeStep() * (e.getX() - hdim.getGraphStartInPixels());
			_l.log(Level.FINE, "This position corresponds to time "+ t);
			// WIP
		}
	}
}