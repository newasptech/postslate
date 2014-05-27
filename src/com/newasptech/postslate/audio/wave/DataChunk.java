/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.InputStream;
import java.util.Iterator;

public class DataChunk extends Reader implements Iterable<SampleFrame>, Iterator<SampleFrame> {
    private FormatHeader header;
    private int framePos = -1;
    private int lastFramePos;

    public DataChunk(InputStream _istr, int chunkLen, FormatHeader _header) {
        super(_istr, chunkLen);
        header = _header;
        lastFramePos = chunkLen / header.getBlockAlign() - 1;
    }
    
    public Iterator<SampleFrame> iterator() {
    	return this;
    }
    
    public void remove() {}
    
    public boolean hasNext() {
    	return (framePos < lastFramePos);
    }
    
    public SampleFrame next() {
    	framePos += 1;
    	return new SampleFrame(istr, header.getBlockAlign(), header);
    }
    
    public int getFramePos() {
    	return framePos;
    }
    
    public int getFrameCount() {
    	return lastFramePos + 1;
    }
}
