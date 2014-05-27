/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Comparator;

/** One event in an A/V file */
public class Event implements Comparable<Event>, Serializable {
	private static final long serialVersionUID = 1L;
	private float time; // offset from the beginning, in seconds
	private Data data; // supporting data
	public Event(float _time, Data _data) {
		time = _time;
		data = _data;
	}
	public float getTime() {
		return time;
	}
	public Data getData() {
		return data;
	}
	public int compareTo(Event rhs) {
		int c = getData().compareTo(rhs.getData());
		if (c != 0) {
			return c;
		}
		return Float.compare(getTime(), rhs.getTime());
	}
	public static DecimalFormat TFMT = new DecimalFormat("##0.000");
	public String toString() {
		return TFMT.format(getTime());
	}
	
	/** Interface for data associated with an AV event */
	public interface Data extends Comparable<Data>, Serializable { }
	
	public static class TimeComparator implements Comparator<Event> {
		public int compare(Event lhs, Event rhs) {
			return (Float.valueOf(lhs.getTime()).compareTo(Float.valueOf(rhs.getTime())));
		}
	}
}
