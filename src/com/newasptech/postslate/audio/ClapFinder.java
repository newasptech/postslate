/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.newasptech.postslate.Config;

public class ClapFinder {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.ClapFinder");
	public static final float CHUNK_SHIFT_THRESHOLD = (float)0.75;
	private float typicalClapDuration;
		
	public ClapFinder(float _typicalClapDuration) {
		typicalClapDuration = _typicalClapDuration;
	}
	
	public Event[] findClaps(FileReader afr, short maxEvents, short quantizeFactor) {
		List<Event> clapEvents = new LinkedList<Event>();
		float advanceIntervalStd = quantizeFactor * typicalClapDuration;
		float advanceInterval = advanceIntervalStd;
		float thisChunkStart = (float)0.0, nextChunkStart = advanceIntervalStd;
		float chunkMaxSlope = (float)0.0, timeAtMaxSlope = (float)0.0;
		Frame lastFrame = null;
		for (Iterator<Frame> pFrame = afr.iterator(); pFrame.hasNext();) {
			Frame frame = pFrame.next();
			if (frame.getTime() >= nextChunkStart) {
				if (timeAtMaxSlope >= (thisChunkStart + advanceIntervalStd * CHUNK_SHIFT_THRESHOLD)
						&& advanceInterval == advanceIntervalStd) {
					advanceInterval /= 2.0;
				}
				else {
					clapEvents.add(new Event(timeAtMaxSlope, new PercussiveSound(chunkMaxSlope)));
					advanceInterval = advanceIntervalStd;
					chunkMaxSlope = (float)0.0;
				}
				thisChunkStart += advanceInterval; nextChunkStart += advanceInterval;
			}
			if (lastFrame != null) {
				for (int i = 0; i != afr.getNumChannels(); ++i) {
					float slope = afr.getSampleRate() * (frame.getNormalizedAmplitude(i) - lastFrame.getNormalizedAmplitude(i));
					if (slope > chunkMaxSlope) {
						chunkMaxSlope = slope;
						timeAtMaxSlope = lastFrame.getTime();
					}
				}
			}
			lastFrame = frame;
		}
		Collections.sort(clapEvents);
		if (maxEvents>0) {
			return clapEvents.subList(0, Math.min(clapEvents.size(), maxEvents)).toArray(new Event[]{});
		}
		Event[] retEvents = clapEvents.toArray(new Event[]{});
		_l.log(Level.FINE, String.format("Possible clap times: %.6f, %.6f, %.6f",
				retEvents[0].getTime(), retEvents[1].getTime(), retEvents[2].getTime()));
		return retEvents;
	}
	
	public static void main(String[] args) {
		try {
	    	DecimalFormat tFmt = new DecimalFormat("###,##0.000000");
	    	System.out.println("File\tClap Times");
	    	int DISPLAY_CLAP_TIMES = 3;
	    	Config cfg = new Config();
	    	ClapFinder cf = new ClapFinder(cfg.fvalue(Config.TYPICAL_CLAP_DURATION));
			for (int i=0; i != args.length; ++i) {
				FileReader r = new FileReaderWAV(new java.io.File(args[i]));
				StringBuffer msg = new StringBuffer();
				msg.append(args[i]);
				Event[] events = cf.findClaps(r, cfg.svalue(Config.SCAN_EVENTS),
						cfg.svalue(Config.QUANTIZE_FACTOR));
				for (int j=0; j != DISPLAY_CLAP_TIMES; ++j) {
					msg.append("\t");
					msg.append(tFmt.format(events[j].getTime()));
				}
				System.out.println(msg.toString());
			}
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
