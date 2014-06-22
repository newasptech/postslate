package com.newasptech.postslate;

public class AVPair {
	private AVClip video;
	private AVClip audio;
	public AVPair(AVClip _video, AVClip _audio) {
		video = _video;
		audio = _audio;
	}
	public AVClip video() { return video; }
	public AVClip audio() { return audio; }
}