/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.gui;

/** Horizontal dimensions for the waveform graph */
public class GraphHorizDim {
	private int viewWidth;
	private float trimStartTime;
	private float trimDuration;
	private float graphStartOffset;
	private float viewWidthAsTime;
	private float[] clapCandidates;
	private int clapPosIdx;
	private float timeStep;
	private int clapPosH, trimStartH, trimEndH, horizOffset;
	
	/**
	 * @param _viewWidth graph width in pixels
	 * @param _trimStartTime the amount of time, in seconds, to be trimmed off the beginning of the clip
	 * @param _trimDuration the duration of the clip, in seconds, after trimming
	 * @param _graphStartOffset the offset of the left edge of the graph, as a time in seconds 
	 * @param _viewWidthAsTime the span of time represented by the entire width of the graph
	 * @param _clapCandidates an array of candidate clap positions, as time offsets from the beginning of the clip
	 * @param _clapPosIdx the 0-based index of the selected clap candidate
	 *  */
	public GraphHorizDim(int _viewWidth, float _trimStartTime, float _trimDuration, float _graphStartOffset, float _viewWidthAsTime, float[] _clapCandidates, int _clapPosIdx) {
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
	
	/** Return the graph's width in pixels. */
	public int getViewWidthInPixels() { return viewWidth; }
	
	/** Return the time interval represented by the space between the left edge of the graph
	 *  and the beginning of the waveform. */
	public float getGraphStartAsTime() { return graphStartOffset; }
	
	/** The amount of time to be trimmed from the start in order to sync. */
	public float getTrimStartAsTime() { return trimStartTime; }
	
	/** The duration of the clip, after trimming. */
	public float getTrimDuration() { return trimDuration; }
	
	/** The time interval spanned by the entire graph. */
	public float getViewWidthAsTime() { return viewWidthAsTime; }
	
	/** The list of possible clap times. */
	public float[] getClapCandidates() { return clapCandidates; }
	
	/** The 0-based index of the clap time */
	public int getClapPosIdx() { return clapPosIdx; }

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