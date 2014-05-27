/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormatHeader extends Reader {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.audio.wave.FormatHeader");
    
    // header values
    private short compressionCode;
    private short numChannels;
    private int sampleRate;
    private int avgBytesPerSecond;
    private short blockAlign;
    private short sigBitsPerSample;
    private short extraFormatBytes = 0;
    private byte[] extraFormatData = null;
    // derived values, calculated once for efficiency
    private short bytesPerSample;
    private int rangeMin;
    private int rangeMax;
    private int threshold;
    private int shift;
    private double period;
    public static final int COMPRESSION_PCM = 1;

    public FormatHeader(InputStream _istr, int _chunkLen)
    		throws IOException {
        super(_istr, _chunkLen);
        compressionCode = readli16();
        numChannels = readli16();
        sampleRate = readli32();
        avgBytesPerSecond = readli32();
        blockAlign = readli16();
        sigBitsPerSample = readli16();
        if (getChunkDataSize() > 16) {
        	extraFormatBytes = readli16();
        	addToChunkDataSize(extraFormatBytes);
        	extraFormatData = new byte[extraFormatBytes];
        	istr.read(extraFormatData);
        }
        if ((sigBitsPerSample % BITS_PER_BYTE) == 0) {
            bytesPerSample = (short)(sigBitsPerSample / BITS_PER_BYTE);
        }
        else {
            bytesPerSample = (short)(1 + sigBitsPerSample / BITS_PER_BYTE);
        }
        int v = (int)Math.pow(2, sigBitsPerSample - 1);
        rangeMin = -v;
        rangeMax = v - 1;
        if (bytesPerSample == 1) {
            threshold = -1;
            shift = v;
        }
        else {
            threshold = rangeMax;
            shift = (int)Math.pow(2, sigBitsPerSample);
        }
        period = 1.0 / sampleRate;
        finish();
        _l.log(Level.FINE, toString());
    }
    public short getCompressionCode() { return compressionCode; }
    public short getNumChannels() { return numChannels; }
    public int getSampleRate() { return sampleRate; }
    public int getAvgBytesPerSecond() { return avgBytesPerSecond; }
    public short getBlockAlign() { return blockAlign; }
    public short getSigBitsPerSample() { return sigBitsPerSample; }
    public short getExtraFormatBytes() { return extraFormatBytes; }
    public byte[] getExtraFormatData() { return extraFormatData; }
    public short getBytesPerSample() { return bytesPerSample; }
    public int getRangeMin() { return rangeMin; }
    public int getRangeMax() { return rangeMax; }
    public int getNIThreshold() { return threshold; }
    public int getNIShift() { return shift; }
    public double getPeriod() { return period; }
    public String toString() {
        StringBuffer s = new StringBuffer("chunkID=fmt");
        s.append(" chunkDataSize=");
        s.append(getChunkDataSize());
        s.append(" compressionCode=");
        s.append(compressionCode);
        s.append(" sampleRate=");
        s.append(sampleRate);
        s.append(" avgBytesPerSecond=");
        s.append(avgBytesPerSecond);
        s.append(" blockAlign=");
        s.append(blockAlign);
        s.append(" sigBitsPerSample=");
        s.append(sigBitsPerSample);
        s.append(" extraFormatBytes=");
        s.append(extraFormatBytes);
        return s.toString();
    }
}
