/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate;

import java.io.File;

public interface AVEngine {
	static enum MetaKey { CODEC_TYPE, DURATION };
	static enum MetaValue { CODEC_TYPE_VIDEO, CODEC_TYPE_AUDIO, CODEC_TYPE_DATA };
	
	/** Read an AV file and return metadata about its streams */
	StreamMetaList streamMeta(File f);
	
	String metaKeyName(MetaKey key);
	String metaValueName(MetaValue value);
	
	/** Given video and audio AV file clips, extract the referenced clips and package
	 *  them, without transcoding, into a new container. */
	File repackage(AVDirRef vdir, AVClip vinput, AVDirRef adir, AVClip ainput, String outputPath);

	/** Given a set of AV file clips, extract the referenced clips; transcode
	 * and repackage them into a new container, retaining the same bitrate as
	 * the source streams. */
	File transcode(AVDirRef vdir, AVClip vinput, AVDirRef adir, AVClip ainput, String videoCodec,
			String audioCodec, String outputPath);
	
	/** Play a set of AV file clips */
	void play(AVDirRef inputDir, AVClip input, int width, int height);
	
	/** Return a list of output container types */
	String[] outputFormats();
	
	/** Check to make sure that the components/libraries needed by the Engine are present and working. */
	void check() throws RequiredComponentMissing, OptionalComponentMissing, ComponentCheckFailed;
	class ComponentCheckFailed extends Exception {
		private static final long serialVersionUID = 1L;
		private String component;
		public ComponentCheckFailed(String name, String errMsg) {
			super(errMsg);
			component = name;
		}
		public String getComponent() {
			return component;
		}
	}
	class RequiredComponentMissing extends Exception {
		private static final long serialVersionUID = 1L;
		private String component;
		public RequiredComponentMissing(String name, String url, String extraPath, String errMsg) {
			super(message(name, url, extraPath, errMsg));
			component = name;
		}
		public String getComponent() { return component; }
		private static String message(String name, String url, String extraPath, String errMsg) {
			StringBuilder msg = new StringBuilder("The ");
			msg.append(name);
			msg.append(" executable could not be run.\nIt may be missing, or possibly incorrectly installed.\n\n");
			msg.append(errMsg);
			msg.append("\n\nThe list of executable search paths is as follows:");
			if (extraPath != null && extraPath.length() > 0) {
				for (String p : extraPath.split(File.pathSeparator)) {
					msg.append("\n");
					msg.append(p);
				}
			}
			for (String p : System.getenv("PATH").split(File.pathSeparator)) {
				msg.append("\n");
				msg.append(p);
			}
			msg.append("\n\nTo download and install this component, please visit\n");
			msg.append(url);
			return msg.toString();
		}
	}
	class OptionalComponentMissing extends Exception {
		private static final long serialVersionUID = 1L;
		public OptionalComponentMissing(String name, String url, String extra) {
			super("The " + name + " package is needed by Postslate.\nPlease refer to " + url + "\n" + extra);
		}
	}
}
