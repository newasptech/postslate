/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;

import com.newasptech.postslate.audio.wave.FormatHeader;
import com.newasptech.postslate.audio.wave.RiffChunk;
import com.newasptech.postslate.audio.wave.SampleFrame;
import com.newasptech.postslate.audio.wave.WaveStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileReaderWAV implements FileReader {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.FileReaderWAV");
	private WAVIterator wi = null;
	
	public FileReaderWAV(File fileRef)
			throws FileNotFoundException, IOException, WaveStreamReader.NotWave {
		wi = new WAVIterator(fileRef.toString());
	}

	public Iterator<Frame> iterator() {
		return wi;
	}
	
	public short getNumChannels() {
		return wi.getHeader().getNumChannels();
	}
	
	public float getSampleRate() {
		return wi.getHeader().getSampleRate();
	}
	
	class WAVIterator implements Iterator<Frame> {
		private WaveStreamReader waveReader = null;
		private Iterator<RiffChunk> pRiffChunk = null;
		private RiffChunk riffReader = null;
		private Iterator<SampleFrame> pSampleFrame = null;
		private float tRIFFStart = (float)0.0;
		
		public WAVIterator(String path)
				throws FileNotFoundException, IOException, WaveStreamReader.NotWave {
			waveReader = new WaveStreamReader(new FileInputStream(path));
			pRiffChunk = waveReader.iterator();
		}
		
		public boolean hasNext() {
			while(true) {
				if (riffReader == null) {
					if (pRiffChunk.hasNext()) {
						riffReader = pRiffChunk.next();
						pSampleFrame = riffReader.getData().iterator();
					}
					else {
						return false;
					}
				}
				if (pSampleFrame.hasNext()) {
					return true;
				}
				tRIFFStart += timeOffsetInRIFFChunk();
				riffReader = null;
			}
		}
		
		private float timeOffsetInRIFFChunk() {
			return (float)(riffReader.getData().getFramePos()*riffReader.getHeader().getPeriod());			
		}
		
		public Frame next() {
			Frame af = null;
			try {
				float[] fAmp = pSampleFrame.next().getNormFloatAmplitudes();
				af = new Frame(tRIFFStart+timeOffsetInRIFFChunk(), fAmp);
			}
			catch(Exception e) {
				_l.log(Level.SEVERE, "Unexpected exception", e);
			}
			return af;
		}
		
		public void remove() {}
		
		public FormatHeader getHeader() {
			return riffReader.getHeader();
		}
	}
}
