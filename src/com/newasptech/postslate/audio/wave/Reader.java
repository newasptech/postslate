/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class Reader {
    protected static final String WAV_CHARSET = "US-ASCII";
    protected static final short BITS_PER_BYTE = 8;

    protected InputStream istr;
    private int chunkDataSize;

    public Reader(InputStream _istr, int _chunkDataSize) {
        istr = _istr;
        chunkDataSize = _chunkDataSize;
    }
    
    public int getChunkDataSize() {
    	return chunkDataSize;
    }
    
    protected void addToChunkDataSize(int i) {
    	chunkDataSize += i;
    }

    protected void finish() throws IOException {
        if ((chunkDataSize % 2) != 0) {
            istr.skip(1);
        }
    }

    protected boolean isEOF() throws IOException {
        return istr.available() == 0;
    }

    protected String readString(int len) throws Error, IOException {
        byte[] bytes = new byte[len];
        if (istr.read(bytes) != len) {
            throw new EOFException();
        }
        return new String(bytes, Charset.forName(WAV_CHARSET));
    }

    protected int readLEInt(int bytes) throws IOException {
        if (bytes < 1 || bytes > 4) {
            StringBuffer err = new StringBuffer("Unable to read an integer of length ");
            err.append(bytes);
            throw new Error(err.toString());
        }
        int ival = 0;
        for (int i=0; i != bytes; ++i) {
        	int byteval = istr.read();
        	if (byteval == -1) {
        		throw new EOFException();
        	}
            ival |= (int)(byteval<<(i*BITS_PER_BYTE));
        }
        return ival;
    }

    protected short readli16() throws IOException {
        return (short)readLEInt(2);
    }

    protected int readli24() throws IOException {
        return readLEInt(3);
    }

    protected int readli32() throws IOException {
        return readLEInt(4);
    }

    protected ChunkHeader readChunkHeader() throws IOException {
        return new ChunkHeader(readString(4), readLEInt(4));
    }

    public class ChunkHeader {
        private String type;
        private int length;
        public ChunkHeader(String _type, int _length) {
            type = _type;
            length = _length;
        }
        public String type() { return type; }
        public int length() { return length; }
        public String toString() {
        	return String.format("ChunkHeader: type=%s length=%d", type, length);
        }
    }

    public class Error extends RuntimeException {
    	private static final long serialVersionUID = 1L;
    	public Error(String msg) {
    		super(msg);
    	}
    }
}
