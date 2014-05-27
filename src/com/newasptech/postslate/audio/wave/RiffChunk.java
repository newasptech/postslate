/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RiffChunk extends Reader {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.RiffChunk");
    private FormatHeader fmtReader = null;
    private DataChunk datReader = null;
    private int posInsideChunk = 4; /* start after the WAVE tag */

    public RiffChunk(InputStream _istr, int chunkLen) throws IOException {
        super(_istr, chunkLen);
        _l.log(Level.FINE, "Begin RIFF chunk of length " + chunkLen);
        int dataByteLen = -1;
        ByteArrayInputStream dataByteStr = null;
        while (posInsideChunk < chunkLen) {
        	ChunkHeader chunkHead = nextChunkHeader();
        	if (chunkHead.type().equals("fmt ")) {
                fmtReader = new FormatHeader(istr, chunkHead.length());
                posInsideChunk += chunkHead.length();
        	}
            else if (chunkHead.type().equals("data")) {
                dataByteLen = adjustedReadLen(chunkHead.length());
                byte[] dataBytes = new byte[dataByteLen];
                istr.read(dataBytes);
                assert(dataByteStr == null); // for now, assuming only one chunk of WAVE data per RIFF chunk
                dataByteStr = new ByteArrayInputStream(dataBytes);
                posInsideChunk += dataByteLen;
            }
            else {
            	_l.log(Level.FINE, "Skip chunk of type '" + chunkHead.type() + "' and length " + chunkHead.length());
            	istr.skip(chunkHead.length());
            	posInsideChunk += chunkHead.length();
            }
        }
        if (fmtReader != null && dataByteStr != null)
        	datReader = new DataChunk(dataByteStr, dataByteLen, fmtReader);
        finish();
    }
    
    private ChunkHeader nextChunkHeader() throws IOException {
        int HEADERLEN = 8;
        ChunkHeader chunkHead = readChunkHeader();
        posInsideChunk += HEADERLEN;
        _l.log(Level.FINE, chunkHead.toString());
    	return chunkHead;
    }
    
    public int adjustedReadLen(int readLen) {
        if (0 != (readLen % 2)) {
            readLen += 1;
        }
    	return readLen;
    }

    public FormatHeader getHeader() {
        return fmtReader;
    }
    
    public DataChunk getData() {
        return datReader;
    }
}
