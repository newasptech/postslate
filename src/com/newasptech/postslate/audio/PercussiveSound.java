/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;


/** EventData for a percussive sound (e.g., the clap of a slate). */
public class PercussiveSound implements Event.Data {
	private static final long serialVersionUID = 1L;
	
	/** Slope of the waveform as of the event's time. */
	private float slope;
	
	public PercussiveSound(float _slope) {
		slope = _slope;
	}
	public float getSlope() {
		return slope;
	}
	public int compareTo(Event.Data rhs) {
		return -Float.compare(slope, ((PercussiveSound)rhs).getSlope());
	}
}
