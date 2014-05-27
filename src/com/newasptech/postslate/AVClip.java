/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.util.Collection;
import java.util.LinkedList;

import com.newasptech.postslate.util.Misc;

/** An AVClip is a reference to an Audio/Video file, or a contiguous portion thereof. */
public class AVClip extends AVFileRef {
	private static final long serialVersionUID = 1L;
	public static final float INDEFINITE_DURATION = -1.0f;
	
	/** offset from the beginning of the file, in seconds */
	private float offset;
	/** duration in seconds */
	private float duration = INDEFINITE_DURATION;
	private int[] streamIndices = null;
	
	public AVClip(AVFileRef _fileRef, float _offset) {
		super(_fileRef);
		offset = _offset;
	}

	public AVClip(AVFileRef _fileRef, float _offset, float _duration) {
		super(_fileRef);
		offset = _offset;
		duration = _duration;
	}

	public AVClip(AVFileRef _fileRef, float _offset, float _duration,
			int[] _streamIndices) {
		super(_fileRef);
		offset = _offset;
		duration = _duration;
		streamIndices = _streamIndices;
	}

	public AVClip(AVFileRef _fileRef, float _offset, float _duration,
			Collection<Integer> _streamIndices) {
		super(_fileRef);
		offset = _offset;
		duration = _duration;
		streamIndices = Misc.toPrim(_streamIndices);
	}
	
	public float getOffset() {
		return offset;
	}
	
	public float getDuration() {
		return duration;
	}
	
	public StreamMetaList getMeta() {
		if (streamIndices == null)
			return streamMeta;
		LinkedList<StreamMeta> blist = new LinkedList<StreamMeta>();
		for (int i : streamIndices) {
			blist.add(streamMeta.get(i));
		}
		return new StreamMetaList(blist);
	}
	
	/** Given an AVClip, return the duration of its first stream (by codec type--e.g., video)
	 * by pulling it from the clip's metadata. */
	public static float duration(AVClip clip, AVEngine.MetaValue codecType, AVEngine e) {
		return clip.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				e.metaValueName(codecType)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue();
	}
}
