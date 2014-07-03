1. Introduction

Postslate - what is it good for?

Postslate is a tool to assist in synchronizing clips of video with corresponding
audio clips captured by a separate recorder. It achieves this by making use of
the "clap" sound produced by a slate.  (If this means nothing to you, you
probably don't need to use postslate.)

In its current form, postslate can be useful if:
- the camera video is in a form usable by FFMPEG
- the video clips include audio (albeit inferior) captured by a camera mic.
- the externally-recorded audio clips are in WAV format

The general approach of postslate, in synchronizing audio with video clips, is
to locate the clap points in the audio streams from both the camera and the
external sound recorder.  Once synchronized, the camera video and external audio
are (non-destructively) "trimmed" and "merged" together in a single output file,
which can be used as a source for editing.  Merging involves repackaging
streams without transcoding them, so there is no loss of quality.

Please note:  By design, Postslate NEVER modifies your source clips.
It only reads them.

2. Installation

Postslate is written in Java and should work on virtually any modern system.

Postslate requires a Java virtual machine, version 6 or higher.  There are many
JRE distributions freely available.  If you don't already have one, please see
http://en.wikipedia.org/wiki/List_of_Java_virtual_machines .

Before installing Postslate, you need to make sure that the following packages
are installed:

Package			URL
FFMPEG version 2.x	http://ffmpeg.org/download.html
MPV			http://mpv.io/installation/

If you want to modify Postslate, you should also have the Ant build tool
( http://ant.apache.org ), or else be prepared to use your own tool--
e.g., Eclipse.  Overall, compiling the Java code for Postslate is about as basic
as it gets.

The easiest installation is simple to unpack the distribution file (i.e.,
postslate-*.tar.gz) and to launch it using the included postslate script
(Mac/Linux/Unix).  On Windows, run postslate.bat.

From the Mac/Linux/Unix/Cygwin command line, you can also use the commands
	./configure [--prefix=path]
	make
	make install
	
familiar to many users of free software.

3. Use

To get started, please see the Postslate tutorial at
	https://github.com/newasptech/postslate/wiki/Quick-Tutorial

4. Copyright

Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
See the accompanying LICENSE.txt file for copyright licensing info.
