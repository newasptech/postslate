/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.util.Hashtable;
import java.io.Serializable;

/** A map of key-value metadata pairs describing one AV stream. */
public class StreamMeta extends Hashtable<String, StreamMetaValue> implements Serializable {
	private static final long serialVersionUID = 1L;

	public boolean matches(String key, String value) {
		if (!containsKey(key)) return false;
		return get(key).value().contentEquals(value);
	}
	public boolean matches(String key, int iValue) {
		if (!containsKey(key)) return false;
		return get(key).iValue() == iValue;
	}
	public boolean matches(String key, float fValue) {
		if (!containsKey(key)) return false;
		return get(key).fValue() == fValue;
	}
}
