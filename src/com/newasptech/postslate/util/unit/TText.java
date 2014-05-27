/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util.unit;

import static org.junit.Assert.*;
import org.junit.Test;

import com.newasptech.postslate.util.Text;

public class TText {
	@Test
	public void testTokenizeCommand() {
		String Q = Text.Q;
		String[] paths = new String[]{ "exec",
				"/path/to/exec",
				Q + "/path/with space/exec" + Q,
				"/path/" + Q + "with space" + Q + "/sub/exec",
				"/path/" + Q + "with one space" + Q + "/and/" + Q + "yet another" + Q + "/sub/exec"
				};
		for (String ip : paths) {
			String op = ip.replaceAll(Q, "");
			String[] iparts = new String[]{ip, "arg0", "arg1" };
			StringBuilder cmd = new StringBuilder();
			for (String p : iparts) {
				cmd.append(p);
				cmd.append(" ");
			}
			String[] oparts = Text.tokenizeCommand(cmd.toString());
			assertTrue(op.contentEquals(oparts[0]));
			for (int i = 1; i != iparts.length; ++i)
				assertTrue(iparts[i].contentEquals(oparts[i]));
		}
	}
}
