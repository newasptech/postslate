/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import com.newasptech.postslate.audio.Event;

public class WaveRenderer {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.WaveRenderer");
	private WaveBuffer buffer;
	
	public WaveRenderer(WaveBuffer _buffer) {
		buffer = _buffer;
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
		m.put(hdim.getTrimEndInPixels() - hdim.getClapPosInPixels() > LWIDTH ?
				hdim.getClapPosInPixels() : hdim.getClapPosInPixels() - LWIDTH,
				hdim.getClapPosAsTime());
		m.put(hdim.getTrimEndInPixels() - LWIDTH,
				hdim.getTrimStartAsTime() + hdim.getTrimDuration());
		return m;
	}
	
	/** Render a graph of a wave file, returning it as an image.
	 * @param viewHeight	height in pixels of the graph image
	 * @param hdim		horizontal dimensions for the graph image
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
		for (Iterator<Map.Entry<Integer, Float>> pEntry = tickMap.entrySet().iterator(); pEntry.hasNext();) {
			Map.Entry<Integer, Float> entry = pEntry.next();
			Integer H = entry.getKey();
			String t = Event.TFMT.format(entry.getValue());
			_l.log(Level.FINER, "Draw text '" + t + "' at x="+H.toString()+", y="+labelBase);
			g.drawString(t, H.intValue(), labelBase);
		}

		float[][] fAmp = buffer.summarize(hdim.getTimeStep());
		int middle = viewHeight / 2, yRange = middle - 1;
		for (int i = 0; i != fAmp[0].length; i++) {
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
			if (backgroundColor.getRGB() == bufferedImage.getRGB(hdim.getClapPosInPixels(), j))
				bufferedImage.setRGB(hdim.getClapPosInPixels(), j, clapLineColor);
		}
		return bufferedImage;
	}
	
	/** Horizontal dimensions for the graph */
	public static class HorizDim {
		private int viewWidth;
		private float trimStartTime;
		private float trimDuration;
		private float clapPos;
		private float graphStartOffset;
		private float viewWidthAsTime;
		public int getViewWidthInPixels() { return viewWidth; }
		public float getGraphStartAsTime() { return graphStartOffset; }
		public float getTrimStartAsTime() { return trimStartTime; }
		public float getClapPosAsTime() { return clapPos; }
		public float getTrimDuration() { return trimDuration; }
		public float getViewWidthAsTime() { return viewWidthAsTime; }
		/** Constructor
		 * @param _viewWidth graph width in pixels
		 * @param _trimStartTime the amount of time, in seconds, to be trimmed off the beginning of the clip
		 * @param _trimDuration the duration of the clip, in seconds, after trimming
		 * @param _clapPos the time offset of the clap, with respect to the beginning of the raw clip
		 * @param _graphStartOffset the offset of the left edge of the graph, as a time in seconds 
		 * @param _viewWidthAsTime the span of time represented by the entire width of the graph
		 *  */
		public HorizDim(int _viewWidth, float _trimStartTime, float _trimDuration, float _clapPos, float _graphStartOffset, float _viewWidthAsTime) {
			viewWidth = _viewWidth;
			trimStartTime = _trimStartTime;
			trimDuration = _trimDuration;
			clapPos = _clapPos;
			graphStartOffset = _graphStartOffset;
			viewWidthAsTime = _viewWidthAsTime;
			timeStep = viewWidthAsTime / (viewWidth - 1);
			clapPosH = (int)(((graphStartOffset + clapPos) / timeStep));
			trimStartH = (int)((graphStartOffset + trimStartTime) / timeStep);
			trimEndH = (int)((graphStartOffset + trimStartTime + trimDuration) / timeStep);
			horizOffset = (int)(graphStartOffset / timeStep);
		}
		private float timeStep;
		private int clapPosH, trimStartH, trimEndH, horizOffset;
		/** Return the timespan represented by one pixel in the graph. */
		public float getTimeStep() { return timeStep; }
		/** Return the horizontal position of the clap, in pixels, with respect to the left edge of the graph */
		public int getClapPosInPixels() { return clapPosH; }
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
					trimStartTime, clapPos, trimDuration, timeStep);
		}
	}
}