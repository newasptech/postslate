/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import com.newasptech.postslate.audio.Event;
import com.newasptech.postslate.audio.wave.WaveBuffer;

/** Generates a waveform graph as an image. */
public class WaveRenderer {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.WaveRenderer");
	private WaveBuffer buffer;
	/** Map a 2-D rectangle covering the area of each clap tick mark to its corresponding time */
	private Map<Rectangle, Float> clapTickMap = new Hashtable<Rectangle, Float>();
	private int graphWidthInPixels = 0;
	
	public WaveRenderer(WaveBuffer _buffer) {
		buffer = _buffer;
	}
	
	public Map<Rectangle, Float> getClapTickMap() {
		return clapTickMap;
	}
	
	public int getGraphWidthInPixels() {
		return graphWidthInPixels;
	}
	
	/** Given the width of the graph as time, return a set of times where major ticks should be placed on the horizontal axis. */
	private static final int LWIDTH = 40; // estimated label width, in pixels
	private Map<Integer, Float> majorTicks(GraphHorizDim hdim) {
		Map<Integer, Float> m = new TreeMap<Integer, Float>();
		boolean needZero = true;
		if (hdim.getTrimStartAsTime() > 0.0f) {
			int h = 0;
			if (hdim.getTrimStartInPixels() < 2 * LWIDTH) {
				h = hdim.getGraphStartInPixels();
				needZero = false;
			}
			else
				h = hdim.getTrimStartInPixels() - LWIDTH;
			m.put(h, hdim.getTrimStartAsTime());
		}
		
		if (needZero) {
			int zpos = 0;
			if (hdim.getGraphStartInPixels() >= LWIDTH)
				zpos = hdim.getGraphStartInPixels() - LWIDTH;
			m.put(zpos, 0.0f);
		}
		m.put(hdim.getTrimEndInPixels() - hdim.getClapPosInPixels(hdim.getClapPosIdx()) > LWIDTH ?
				hdim.getClapPosInPixels(hdim.getClapPosIdx()) : hdim.getClapPosInPixels(hdim.getClapPosIdx()) - LWIDTH,
				hdim.getClapPosAsTime(hdim.getClapPosIdx()));
		m.put(hdim.getTrimEndInPixels() - LWIDTH,
				hdim.getTrimStartAsTime() + hdim.getTrimDuration());
		return m;
	}
	
	/** Render the graph, returning it as an image.
	 * @param hdim		horizontal dimensions for the graph image
	 * @param viewHeight	height in pixels of the graph image
	 */
	public BufferedImage renderWaveform(GraphHorizDim hdim, int viewHeight) {
		BufferedImage bufferedImage = new BufferedImage(hdim.getViewWidthInPixels(),
				viewHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bufferedImage.createGraphics();
		Color backgroundColor = new Color(255, 255, 255); // white background
		g.setPaint(backgroundColor);
		g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
		Map<Integer, Float> tickMap = majorTicks(hdim);
		int labelBase = bufferedImage.getHeight();
		Color textColor = new Color(0, 0, 0);
		g.setPaint(textColor);
		for (Iterator<Map.Entry<Integer, Float>> pEntry = tickMap.entrySet().iterator();
				pEntry.hasNext();) {
			Map.Entry<Integer, Float> entry = pEntry.next();
			Integer H = entry.getKey();
			String t = Event.TFMT.format(entry.getValue());
			_l.log(Level.FINER, "Draw text '" + t + "' at x="+H.toString()+", y="+labelBase);
			g.drawString(t, H.intValue(), labelBase);
		}

		float[][] fAmp = buffer.summarize(hdim.getTimeStep());
		int middle = viewHeight / 2, yRange = middle - 1;
		graphWidthInPixels = fAmp[0].length;
		for (int i = 0; i != graphWidthInPixels; i++) {
			int yMin = middle + (int)(yRange * fAmp[0][i]);
			int yMax = middle + (int)(yRange * fAmp[1][i]);
			for (int y = yMin; y <= yMax; ++y)
				bufferedImage.setRGB(hdim.getGraphStartInPixels() + i, y, 0);
		}
		int boundaryLineColor = (new Color(0, 0, 255)).getRGB();
		int clapLineColor = (new Color(0, 255, 0)).getRGB();
		for (int j = 0; j != viewHeight; ++j) {
			if (backgroundColor.getRGB() == bufferedImage.getRGB(hdim.getTrimStartInPixels(), j))
				bufferedImage.setRGB(hdim.getTrimStartInPixels(), j, boundaryLineColor);
			if (backgroundColor.getRGB() == bufferedImage.getRGB(hdim.getTrimEndInPixels(), j))
				bufferedImage.setRGB(hdim.getTrimEndInPixels(), j, boundaryLineColor);
			if (backgroundColor.getRGB() == bufferedImage.getRGB(hdim.getClapPosInPixels(hdim.getClapPosIdx()), j))
				bufferedImage.setRGB(hdim.getClapPosInPixels(hdim.getClapPosIdx()), j, clapLineColor);
		}
		
		Map<Integer, Float> tempTickMap = new TreeMap<Integer, Float>();
		int CLAP_TICK_HEIGHT = 10, y = 0, w = 1, h = CLAP_TICK_HEIGHT;
		for (int i = 0; i != hdim.getClapCandidates().length; ++i) {
			int x = hdim.getClapPosInPixels(i);
			if (!tempTickMap.keySet().contains(x)) {
				tempTickMap.put(x, hdim.getClapPosAsTime(i));
			}
			for (int j = 0; j != CLAP_TICK_HEIGHT; ++j) {
				bufferedImage.setRGB(x, y + j, clapLineColor);
			}
		}
		updateClapTickMap(tempTickMap, y, w, h);
		return bufferedImage;
	}
	
	private void updateClapTickMap(Map<Integer, Float> tempTickMap, int y, int w, int h) {
		clapTickMap.clear();
		for (Iterator<Map.Entry<Integer, Float>> pEntry = tempTickMap.entrySet().iterator();
				pEntry.hasNext();) {
			Map.Entry<Integer, Float> entry = pEntry.next();
			int x = entry.getKey();
			Float t = entry.getValue();
			clapTickMap.put(new Rectangle(x, y, w, h), t);
		}
	}
}