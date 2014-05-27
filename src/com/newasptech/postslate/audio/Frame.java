/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;

import java.text.DecimalFormat;

public class Frame {
	private static DecimalFormat T_FMT = new DecimalFormat("###,##0.000000");
	private static DecimalFormat A_FMT = new DecimalFormat(" ###,##0.000000;-###,##0.000000");
	/** Time, as an offset from the beginning of the file */
	private float time;
	/** Waveform values, one per channel, normalized to the range [-1.0, 1.0] */
	private float[] normAmplitudes;
	public Frame(float _time, float[] _normAmplitudes) {
		time = _time;
		normAmplitudes = _normAmplitudes;
	}
	public float getTime() {
		return time;
	}
	public float getNormalizedAmplitude(int channel) {
		return normAmplitudes[channel];
	}
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(T_FMT.format(time));
		for (int i=0; i != normAmplitudes.length; ++i) {
			s.append("\t");
			s.append(A_FMT.format(normAmplitudes[i]));
		}
		return s.toString();
	}
}
