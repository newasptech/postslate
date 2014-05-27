/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.audio;

public interface FileReader extends Iterable<Frame> {
	short getNumChannels();
	float getSampleRate();
}
