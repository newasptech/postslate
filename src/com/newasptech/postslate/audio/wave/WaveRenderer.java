/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

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
	private Map<Integer, Float> majorTicks(HorizDim hdim) {
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
	
	/** Render a graph of a wave file, returning it as an image.
	 * @param hdim		horizontal dimensions for the graph image
	 * @param viewHeight	height in pixels of the graph image
	 */
	public BufferedImage renderWaveform(HorizDim hdim, int viewHeight) {
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
	
	/** Horizontal dimensions for the graph */
	public static class HorizDim {
		private int viewWidth;
		private float trimStartTime;
		private float trimDuration;
		private float graphStartOffset;
		private float viewWidthAsTime;
		private float[] clapCandidates;
		private int clapPosIdx;
		public int getViewWidthInPixels() { return viewWidth; }
		public float getGraphStartAsTime() { return graphStartOffset; }
		public float getTrimStartAsTime() { return trimStartTime; }
		public float getTrimDuration() { return trimDuration; }
		public float getViewWidthAsTime() { return viewWidthAsTime; }
		public float[] getClapCandidates() { return clapCandidates; }
		public int getClapPosIdx() { return clapPosIdx; }
		/** Constructor
		 * @param _viewWidth graph width in pixels
		 * @param _trimStartTime the amount of time, in seconds, to be trimmed off the beginning of the clip
		 * @param _trimDuration the duration of the clip, in seconds, after trimming
		 * @param _graphStartOffset the offset of the left edge of the graph, as a time in seconds 
		 * @param _viewWidthAsTime the span of time represented by the entire width of the graph
		 * @param _clapCandidates an array of candidate clap positions, as time offsets from the beginning of the clip
		 * @param _clapPosIdx the 0-based index of the selected clap candidate
		 *  */
		public HorizDim(int _viewWidth, float _trimStartTime, float _trimDuration, float _graphStartOffset, float _viewWidthAsTime, float[] _clapCandidates, int _clapPosIdx) {
			viewWidth = _viewWidth;
			trimStartTime = _trimStartTime;
			trimDuration = _trimDuration;
			graphStartOffset = _graphStartOffset;
			viewWidthAsTime = _viewWidthAsTime;
			clapCandidates = _clapCandidates;
			clapPosIdx = _clapPosIdx;
			timeStep = viewWidthAsTime / (viewWidth - 1);
			trimStartH = (int)((graphStartOffset + trimStartTime) / timeStep);
			trimEndH = (int)((graphStartOffset + trimStartTime + trimDuration) / timeStep);
			horizOffset = (int)(graphStartOffset / timeStep);
		}
		private float timeStep;
		private int clapPosH, trimStartH, trimEndH, horizOffset;
		/** Return the timespan represented by one pixel in the graph. */
		public float getTimeStep() { return timeStep; }
		public float getClapPosAsTime(int idx) {
			if (idx < 0 || idx >= clapCandidates.length)
				return 0.0f;
			return clapCandidates[idx];
		}
		/** Return the horizontal position of the clap, in pixels, with respect to the left edge of the graph */
		public int getClapPosInPixels(int idx) { return (int)(((graphStartOffset + getClapPosAsTime(idx)) / timeStep)); }
		/** Return the position of the trimmed start time, in pixels from the left edge of the graph */
		public int getTrimStartInPixels() { return trimStartH; }
		/** Return the position of the trimmed end time, in pixels from the left edge of the graph */
		public int getTrimEndInPixels() { return trimEndH; }
		/** Return the position of the waveform, in pixels from the left edge of the graph */
		public int getGraphStartInPixels() { return horizOffset; }
		public String toString() {
			return String.format("viewWidth = %d, horizOffset = %d, trimStartH = %d, clapPosH = %d, " + 
					"trimEndH = %d, viewWidthAsTime = %f, graphStartOffset = %f, trimStartTime = %f, " +
					"clapPos = %f, trimDuration = %f, timeStep = %f",
					viewWidth, horizOffset, trimStartH, clapPosH, trimEndH, viewWidthAsTime, graphStartOffset,
					trimStartTime, getClapPosAsTime(getClapPosIdx()), trimDuration, timeStep);
		}
	}
}