/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio.wave;

import java.io.InputStream;
import java.io.IOException;

/** SampleFrame holds a set of values, one per channel, from a single point in time,
 *  as read from a WAV file.  You can access the samples either as raw values,
 *  normalized signed integers, or normalized floating-point numbers.*/
public class SampleFrame extends Reader {

    private FormatHeader header;
    private int[] rawValues = null;

    public SampleFrame(InputStream _istr, int chunkLen, FormatHeader _header) {
        super(_istr, chunkLen);
        header = _header;
        if (header.getCompressionCode() > 0 && header.getCompressionCode() != FormatHeader.COMPRESSION_PCM) {
            StringBuffer err = new StringBuffer("Unsupported compression code: ");
            err.append(header.getCompressionCode());
            throw new Error(err.toString());
        }
        if (header.getBytesPerSample() < 1 || header.getBytesPerSample() > 4) {
            StringBuffer err = new StringBuffer("Cannot handle bytes-per-sample value of ");
            err.append(header.getBytesPerSample());
            throw new Error(err.toString());
        }
    }

    /** Return each channel value from the frame as an N-byte integer value,
     * just as it was unpacked from its little-endian encoding. */
    public int[] getRaw() throws IOException {
        if (rawValues == null) {
            rawValues = new int[header.getNumChannels()];
            for (int i=0; i != header.getNumChannels(); ++i) {
                rawValues[i] = readLEInt(header.getBytesPerSample());
            }
            finish();
        }
        return rawValues;
    }

    /** Return each channel sample as a signed, N-byte integer value cast
     *  as a 4-byte int.  This means taking the raw sample value and converting
     *  it from a two's-complement binary to a signed integer in the range
     *  [-2^(N*8-1), 2^(N*8-1)-1].  Use the getRangeMin()/getRangeMax()
     *  methods of the FormatHeader object to obtain the values of these bounds. */
    public int[] getNormIntAmplitudes() throws IOException {
        getRaw();
        int[] nis = new int[header.getNumChannels()];
        for (int i = 0; i != header.getNumChannels(); ++i) {
            if (rawValues[i] > header.getNIThreshold()) {
                nis[i] = rawValues[i] - header.getNIShift();
            }
            else {
                nis[i] = rawValues[i];
            }
        }
        return nis;
    }

    /** Return each channel's wave amplitude as a float normalized to the
     * 	range [-1.0, 1.0] */
    public float[] getNormFloatAmplitudes() throws IOException {
        int[] iamp = getNormIntAmplitudes();
        float[] famp = new float[header.getNumChannels()];
        for (int i = 0; i != header.getNumChannels(); ++i) {
            if (iamp[i] >= 0) {
                famp[i] = (float)1.0 * iamp[i] / header.getRangeMax();
            }
            else {
                famp[i] = (float)-1.0 * iamp[i] / header.getRangeMin();
            }
        }
        return famp;
    }
}
