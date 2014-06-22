package com.newasptech.postslate;

public class TrimValues {
	private float videoStart, audioStart, duration;

	/** Given the length and clap positions for each member of a video/audio pair,
	 *  calculate the parameters needed to trim the clips.
	 *  @param vClipLen video clip length
	 *  @param _vClapPos video clap position, as an offset from the beginning
	 *  @param aClipLen audio clip length
	 *  @param aClapPos audio clap position, as n offset from the beginning
	 *   */
	public TrimValues(float vClipLen, float _vClapPos, float vShift, float aClipLen, float aClapPos) {
		assert(vClipLen > _vClapPos);
		assert(aClipLen > aClapPos);
		float vClapPos = _vClapPos + vShift;
		float preClapLen = Math.min(vClapPos, aClapPos),
				postClapLen = Math.min(vClipLen - vClapPos, aClipLen - aClapPos);
		videoStart = vClapPos - preClapLen;
		audioStart = aClapPos - preClapLen;
		duration = preClapLen + postClapLen;
	}

	public float getVideoStart() { return videoStart; }
	public float getAudioStart() { return audioStart; }
	public float getDuration() { return duration; }
}
