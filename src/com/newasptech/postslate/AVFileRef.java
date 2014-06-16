/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.newasptech.postslate.audio.Event;

/** AVFileRef holds a reference to a media file, plus a set of metadata values for that file. */
public class AVFileRef implements Comparable<AVFileRef>, Serializable {
	private String name;
	protected StreamMetaList streamMeta = null;
	private Event[] clapEvents = null;
	private static final long serialVersionUID = 1L;
	
	public AVFileRef(File file, StreamMetaList _streamMeta) {
		name = file.getName();
		streamMeta = _streamMeta;
	}
	
	public AVFileRef(AVFileRef rhs) {
		name = rhs.getName();
		streamMeta = rhs.getMeta();
		clapEvents = rhs.getEvents();
	}
	
	public String getName() {
		return name;
	}

	public StreamMetaList getMeta() {
		return streamMeta;
	}
	
	public Event[] getEvents() {
		return clapEvents;
	}
	
	public void setEvents(Event[] _clapEvents) {
		clapEvents = _clapEvents;
	}
	
	public int compareTo(AVFileRef rhs) {
		return name.compareTo(rhs.getName());
	}
	
	public List<Event> getEvents(int eventCount, boolean scoreOrder) {
		List<Event> events = new LinkedList<Event>();
		try {
			for (int i = 0; i != eventCount; ++i) {
				events.add(getEvents()[i]);
			}
		}
		catch(ArrayIndexOutOfBoundsException aioob) {
                    // return as many events as possible
                }
		if (scoreOrder)
			Collections.sort(events);
		else {
			Collections.sort(events, new Event.TimeComparator());
		}
		return events;
	}
}
