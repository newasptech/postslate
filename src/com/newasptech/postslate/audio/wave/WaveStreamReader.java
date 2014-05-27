/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaveStreamReader extends Reader implements Iterable<RiffChunk>, Iterator<RiffChunk> {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.WaveStreamReader");

    public WaveStreamReader(InputStream _istr) throws IOException, NotWave {
        super(_istr, 0);
        getNext();
    }
    
    public Iterator<RiffChunk> iterator() {
    	return this;
    }

    public void remove() {
        throw new Error("Not Implemented");
    }
    
    private RiffChunk current = null, last = null;
    private void fetchNext() throws IOException, NotWave {
    	ChunkHeader riffHeader = readChunkHeader();
    	if (riffHeader.type().equals("RIFF")) {
    		String riffTypeID = readString(4);
    		if (riffTypeID.equals("WAVE")) {
    			current = new RiffChunk(istr, riffHeader.length());
    		}
    		else throw new NotWave("Expected WAVE, but got " + riffTypeID);
    	}
    	else throw new NotWave("Expected RIFF, but got " + riffHeader.type());
    }
    
    private void getNext() throws IOException, NotWave {
    	if (last != null) {
    		last.finish();
    		last = null;
    	}
    	if (current == null) {
    		fetchNext();
    	}
    }
    
    public boolean hasNext() {
    	boolean retval = false;
    	try {
    		getNext();
    		retval = (current != null);
    	}
    	catch(EOFException eof) {
    		_l.log(Level.FINER, "EOF");
    	}
    	catch(Exception e) {
    		_l.log(Level.SEVERE, "Unexpected exception", e);
    	}
    	return retval;
    }
    
    public RiffChunk next() {
    	RiffChunk retval = null;
    	try {
    		getNext();
    		retval = current;
    		last = current;
    	}
    	catch(EOFException eof) {
    		_l.log(Level.FINER, "EOF");
    	}
    	catch(Exception e) {
    		_l.log(Level.SEVERE, "Unexpected exception", e);
    	}
    	current = null;
    	return retval;
    }
    
    public class NotWave extends Exception {
    	private static final long serialVersionUID = 1L;
    	public NotWave(String msg) {
    		super(msg);
    	}
    }
    
    public static void main(String[] args) {
    	DecimalFormat tFmt = new DecimalFormat("###,##0.000000");
    	DecimalFormat aFmt = new DecimalFormat(" ###,##0.000000;-###,##0.000000");
    	for(String path : args) {
    		try {
				System.out.println(path);
    			InputStream f = new FileInputStream(path);
    			WaveStreamReader wavReader = new WaveStreamReader(f);
    			float tRiffStart = (float)0.0;
    			for (Iterator<RiffChunk> pRiffChunk = wavReader.iterator();
    					pRiffChunk.hasNext();) {
    				RiffChunk riffChunk = pRiffChunk.next();
    				FormatHeader header = riffChunk.getHeader();
    				System.out.println(header.toString());
    				int framePos = 0;
    				for (Iterator<SampleFrame> pSampleFrame = riffChunk.getData().iterator();
    						pSampleFrame.hasNext();) {
    					SampleFrame sfr = pSampleFrame.next();
    					StringBuffer s = new StringBuffer();
    					s.append(tFmt.format(tRiffStart + header.getPeriod() * framePos++));
    					float[] fAmp = sfr.getNormFloatAmplitudes();
    					for (float fa: fAmp) {
    						s.append("\t");
    						s.append(aFmt.format(fa));
    					}
    					System.out.println(s.toString());
    				}
    				tRiffStart += header.getPeriod() * framePos;
    			}
    		}
    		catch(Exception e) {
    			e.printStackTrace(System.err);
    		}
    	}
    }
}
