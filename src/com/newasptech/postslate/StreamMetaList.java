/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** A list of AV stream metadata collections--e.g., one for each stream within
 *  an audio/video container file */
public class StreamMetaList extends LinkedList<StreamMeta> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public StreamMetaList() {
		super();
	}
	
	public StreamMetaList(Collection<StreamMeta> rhs) {
		super(rhs);
	}
	
	public int findFirstIndex(String key, String value) {
		int i = 0;
		for (Iterator<StreamMeta> p = iterator(); p.hasNext(); ++i) {
			StreamMeta m = p.next();
			if (m.matches(key, value))
				return i;
		}
		return -1;
	}

	public StreamMeta findFirst(String key, String value) {
		StreamMeta smeta = null;
		for (Iterator<StreamMeta> p = iterator(); p.hasNext();) {
			StreamMeta pmeta = p.next();
			if (pmeta.matches(key, value)) {
				smeta = pmeta;
				break;
			}
		}
		return smeta;
	}
	
	public List<Integer> findAllIndices(Hashtable<String, String> kvfilter) {
		int i = 0;
		List<Integer> matches = new LinkedList<Integer>();
		for (Iterator<StreamMeta> psm = iterator(); psm.hasNext(); ++i) {
			for (Iterator<String> pKey = kvfilter.keySet().iterator(); pKey.hasNext();) {
				String key = pKey.next();
				if (psm.next().matches(key, kvfilter.get(key))) {
					matches.add(Integer.valueOf(i));
					break;
				}
			}
		}
		return matches;
	}
	
	public List<Integer> findAllIndices(String key, String value) {
		Hashtable<String, String> kvfilter = new Hashtable<String, String>();
		kvfilter.put(key, value);
		return findAllIndices(kvfilter);
	}
	
	/** Find all streams where key0=value0 OR key1=value1 OR ... */
	public StreamMetaList findAll(Hashtable<String, String> kvfilter) {
		List<StreamMeta> matches = new LinkedList<StreamMeta>();
		for (Iterator<StreamMeta> psm = iterator(); psm.hasNext();) {
			StreamMeta pmeta = psm.next();
			for (Iterator<String> pKey = kvfilter.keySet().iterator(); pKey.hasNext();) {
				String key = pKey.next();
				if (pmeta.matches(key, kvfilter.get(key))) {
					matches.add(pmeta);
					break;
				}
			}
		}
		return new StreamMetaList(matches);
	}
	
	public StreamMetaList findAll(String key, String value) {
		Hashtable<String, String> kvfilter = new Hashtable<String, String>();
		kvfilter.put(key, value);
		return findAll(kvfilter);
	}
}
