/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Text {

	/** Concatenate strings from an array to make a delimiter-separated list. */
	public static String join(String[] parts, String delim) {
		StringBuffer b = new StringBuffer();
		boolean afterFirst = false;
		for (String s: parts) {
			if (afterFirst)
				b.append(delim);
			b.append(s);
			afterFirst = true;
		}
		return b.toString();
	}
	
	public static String join(String[] parts) {
		return join(parts, " ");
	}

	public static final String Q = "\"";
	private static final Pattern P = Pattern.compile("(([^\\s" + Q + "]*" + Q + "[^" + Q + "]*" + Q + ")+\\S*)|[^\\s" + Q + "]+");
	public static String[] tokenizeCommand(String c) {
		List<String> tokens = new LinkedList<String>();
		Matcher m = P.matcher(c);
		int pos = 0;
		while (m.find(pos)) {
			String g1 = m.group(1);
			if (g1 != null)
				tokens.add(g1.replace(Q, ""));
			else
				tokens.add(m.group());
			pos = m.end();
		}
		return tokens.toArray(new String[]{});
	}
}
