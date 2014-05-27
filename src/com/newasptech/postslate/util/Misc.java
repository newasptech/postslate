/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util;

import java.util.Collection;

public class Misc {

	/** Convert a collection of Integer to an array of int */
	public static int[] toPrim(Collection<Integer> cInt) {
		Integer[] iarray = cInt.toArray(new Integer[]{});
		int[] iout = new int[iarray.length];
		for (int i = 0; i != iarray.length; ++i) {
			iout[i] = iarray[i];
		}
		return iout;
	}

	public static int[] range(int value) {
		int[] iarray = new int[value];
		for (int i = 0; i != value; ++i)
			iarray[i] = i;
		return iarray;
	}
	
	public static String osName() {
		return System.getProperty("os.name").toLowerCase();
	}
	
	public static boolean isMac() {
		return (osName().indexOf("mac") >= 0);
	}
	
	public static boolean isWindows() {
		return (osName().indexOf("windows") >= 0);
	}
	
	public static boolean isLinux() {
		return (osName().indexOf("linux") >= 0);
	}
}
