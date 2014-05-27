/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** FileMatchSet pairs Video and Audio files. Each entry identifies the synchronized,
 *  overlapping portion of a video file and its independently-recorded audio counterpart. */
public class MatchBox implements Serializable {
	private static final long serialVersionUID = 1L;
	private SortedSet<AVClip> stagAudio = new TreeSet<AVClip>();
	private SortedSet<AVClip> stagVideo = new TreeSet<AVClip>();
	/** associate video files with audio files */
	private Map<AVClip, AVClip> vaMatch = new TreeMap<AVClip, AVClip>();
	/** reverse-association map */
	private Map<AVClip, AVClip> avMatch = new TreeMap<AVClip, AVClip>();
	
	public Map<AVClip, AVClip> getVAMatches() {
		return Collections.unmodifiableMap(vaMatch);
	}
	
	public boolean hasVideo(AVClip video) {
		return has(video, stagVideo, vaMatch);
	}
	
	public boolean hasAudio(AVClip audio) {
		return has(audio, stagAudio, avMatch);
	}
	
	private static boolean has(AVClip fref, SortedSet<AVClip> stag, Map<AVClip, AVClip> match) {
		return (stag.contains(fref) || match.containsKey(fref));
	}
	
	public boolean isStag(AVClip fref, AVDirRef.Type t) {
		return (t==AVDirRef.Type.VIDEO) ? isStagVideo(fref) : isStagAudio(fref); 
	}
	
	public boolean isStagVideo(AVClip fref) {
		return stagVideo.contains(fref);
	}
	
	public boolean isStagAudio(AVClip fref) {
		return stagAudio.contains(fref);
	}
	
	public boolean hasMatch(AVClip clip, AVDirRef.Type t) {
		return (t==AVDirRef.Type.VIDEO) ? hasMatchForVideo(clip) : hasMatchForAudio(clip); 
	}
	
	public boolean hasMatchForVideo(AVClip vclip) {
		return vaMatch.containsKey(vclip);
	}
	
	public boolean hasMatchForAudio(AVClip aclip) {
		return avMatch.containsKey(aclip);
	}
	
	public AVClip matchForVideo(AVClip vclip) {
		return vaMatch.get(vclip);
	}
	
	public AVClip getMatchedVideo(AVClip vclip) {
		return getKey(vclip, vaMatch, avMatch);
	}
	
	public AVClip getMatchedAudio(AVClip aclip) {
		return getKey(aclip, avMatch, vaMatch);
	}
	
	private static AVClip getKey(AVClip clip, Map<AVClip, AVClip> mapFwd, Map<AVClip, AVClip> mapRev) {
		return mapRev.get(mapFwd.get(clip));
	}
		
	public AVClip matchForAudio(AVClip aclip) {
		return avMatch.get(aclip);
	}
	
	public void addStag(AVClip clip, AVDirRef.Type t) {
		if (t == AVDirRef.Type.VIDEO)
			addStagVideo(clip);
		else
			addStagAudio(clip);
	}
	
	public void addStagVideo(AVClip video) {
		assert(video != null);
		assert(!vaMatch.containsKey(video));
		assert(!stagVideo.contains(video));
		stagVideo.add(video);
	}
	
	public void addStagAudio(AVClip audio) {
		assert(audio != null);
		assert(!avMatch.containsKey(audio));
		assert(!stagAudio.contains(audio));
		stagAudio.add(audio);
	}
	
	public void addMatch(AVClip video, AVClip audio) {
		assert(video != null);
		assert(audio != null);
		assert(!stagVideo.contains(video));
		assert(!stagAudio.contains(audio));
		assert(!vaMatch.containsKey(video));
		assert(!avMatch.containsKey(audio));
		vaMatch.put(video, audio);
		avMatch.put(audio, video);
	}
	
	/** Remove any match or stag entry containing the file, whether it is video or audio. */
	public void remove(AVClip clip) {
		removeVideo(clip);
		removeAudio(clip);
	}

	/** Remove any match containing a video file, plus any entry in the stag set. */
	public void removeVideo(AVClip video) {
		remove(video, stagVideo, vaMatch, avMatch);
	}
	
	/** Remove any match containing an audio file, plus any entry in the stag set. */
	public void removeAudio(AVClip audio) {
		remove(audio, stagAudio, avMatch, vaMatch);
	}
	
	private static void remove(AVClip fref, SortedSet<AVClip> stag,
			Map<AVClip, AVClip> matchF, Map<AVClip, AVClip> matchR) {
		assert(fref != null);
		if (stag.contains(fref)) {
			assert(!matchF.containsKey(fref));
			stag.remove(fref);
		}
		else if (matchF.containsKey(fref)) {
			AVClip fmatch = matchF.get(fref);
			assert(matchR.containsKey(fmatch));
			matchF.remove(fref);
			matchR.remove(fmatch);
		}
	}
}