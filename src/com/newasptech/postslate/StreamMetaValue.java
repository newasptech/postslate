/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.Serializable;

/** Represents one metadata value for a stream */
public class StreamMetaValue implements Serializable {
	private static final long serialVersionUID = 1L;
	private String strValue = null;
	public StreamMetaValue(String _strValue) {
		super();
		strValue = _strValue;
	}
	public String value() {
		return strValue;
	}
	public int iValue() {
		return Integer.decode(strValue);
	}
	public float fValue() {
		return new Float(strValue);
	}
}
