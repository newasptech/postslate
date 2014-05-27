/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WaveBuffer {
	private List<Chunk> chunkList;
	private float duration;

	public WaveBuffer(WaveStreamReader wavReader)
			throws InterruptedException, IOException {
		chunkList = new LinkedList<Chunk>();
		float tRiffStart = 0.0f;
		for (Iterator<RiffChunk> pRiffChunk = wavReader.iterator();
				pRiffChunk.hasNext();) {
			Chunk c = new Chunk(tRiffStart, pRiffChunk.next());
			tRiffStart += c.getHeader().getPeriod() * c.getFrameCount();
			chunkList.add(c);
		}
		Chunk last = chunkList.get(chunkList.size() - 1);
		duration = last.getStartTime() + (float)(last.getFrameCount() * last.getHeader().getPeriod());
	}
	
	public float getDuration() { return duration; }

	/** Given a WaveBuffer, summarize the wave amplitudes as follows:
	 * 	1. For each sample frame, calculate the mean amplitude across all channels.
	 * 	2. For each time step, find the minimum and maximum wave amplitudes within that step.
	 * 	Return the summary as two arrays of floats:
	 * 		float[][] f = b.summarize(stepTime);
	 * 		// f[0] contains an array of minimum amplitudes, one for each time step
	 * 		// f[1] contains an array of maximum amplitudes, one for each time step
	 */
	public float[][] summarize(float stepTime) {
		int outputFrameCount = (int)(duration / stepTime), pos = 0;
		float[][] output = new float[2][outputFrameCount];
		double t = 0.0;
		float maxPos = 0.0f, minNeg = 0.0f;
		for (Iterator<Chunk> pC = chunkList.iterator(); pC.hasNext();) {
			Chunk c = pC.next();
			double period = c.getHeader().getPeriod();
			for (float ampMean : c.getData()) {
				if ((int)(t / stepTime) > pos) {
					output[0][pos] = minNeg;
					output[1][pos] = maxPos;
					maxPos = 0.0f;
					minNeg = 0.0f;
					pos++;
				}
				if (ampMean > maxPos)
					maxPos = ampMean;
				else if (ampMean < minNeg)
					minNeg = ampMean;
				t += period;
			}
		}
		if (pos < outputFrameCount) {
			output[0][pos] = minNeg;
			output[1][pos] = maxPos;
		}
		return output;
	}
	
	public class Chunk {
		private float startTime;
		private FormatHeader header;
		private float[/*framepos*/] normAmpData;
		private int frameCount;
		
		public Chunk(float _startTime, RiffChunk riffChunk)
				throws InterruptedException, IOException {
			startTime = _startTime;
			header = riffChunk.getHeader();
			frameCount = riffChunk.getData().getFrameCount();
			normAmpData = new float[frameCount];
			float numChannels = header.getNumChannels();
			int i = 0;
			for (Iterator<SampleFrame> pSampleFrame = riffChunk.getData().iterator();
					pSampleFrame.hasNext();) {
				SampleFrame sfr = pSampleFrame.next();
				float ampSum = 0.0f;
				for (float amp : sfr.getNormFloatAmplitudes())
					ampSum += amp;
				normAmpData[i++] = ampSum / numChannels;
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException();
			}
		}
		
		public float[] getData() {
			return normAmpData;
		}
		
		public float getStartTime() {
			return startTime;
		}
		
		public FormatHeader getHeader() {
			return header;
		}
		
		public int getFrameCount() {
			return frameCount;
		}
	}
}
