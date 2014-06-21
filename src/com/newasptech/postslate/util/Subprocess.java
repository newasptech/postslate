/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Subprocess {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.util.Subprocess");
	private static final String EXTRAPATH_DEFAULT = "";
	private String[] args;
	private String extraPath = EXTRAPATH_DEFAULT;
	private String out;
	private String err;
	int status;

	/** Constructs a Subprocess, given an array of arguments.  Use Text.tokenizeCommand() to split
	 * a command containing, for example, quoted whitespace. */
	public Subprocess(String[] _args) {
		args = _args;
	}
	
	public Subprocess(String[] _args, String _extraPath) {
		args = _args;
		extraPath = _extraPath;
		adjustExecutable();
	}
	
	public String[] getArgs() {
		return args;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getOut() {
		return out;
	}
	
	public String getErr() {
		return err;
	}
	
	/** Convert a timeout in floating-point seconds to the timeout value
	 *  expected by the methods below. */
	public static int timeout(float timeoutInSec) {
		int MILLISECONDS_PER_SECOND = 1000;
		return (int)timeoutInSec * MILLISECONDS_PER_SECOND;
	}
	
	/** If the executable does not contain a directory and an extra search path is specified,
	 * search to see if the executable exists in one of the extra directories and explicitly
	 * add its path.  (If the executable cannot already be found using the contents of the
	 * PATH environment variable, then modifying it in the call to Runtime.exec() does not help.) */
	private void adjustExecutable() {
		String executable = args[0];
		if (extraPath != null && extraPath.length() > 0
				&& executable.indexOf(System.getProperty("file.separator").charAt(0)) < 0) {
			for (String p : extraPath.split(File.pathSeparator)) {
				File f = new File(p + System.getProperty("file.separator") + executable);
				if (f.isFile() && f.canExecute()) {
					args[0] = f.getPath();
					break;
				}
			}
		}
	}
	
	public void run() throws InterruptedException, IOException {
		if (_l.isLoggable(Level.FINER))
			_l.log(Level.FINER, "exec: " + Text.join(args));
		Process p = Runtime.getRuntime().exec(args);
		StreamCaptureThread cout = new StreamCaptureThread(p.getInputStream());
		cout.start();
		StreamCaptureThread cerr = new StreamCaptureThread(p.getErrorStream());
		cerr.start();
		p.getOutputStream().close();
		status = p.waitFor();
		out = cout.output();
		err = cerr.output();
	}
	
	public void run(int timeout) throws InterruptedException, IOException {
		SubprocessMonitoringThread pw = start();
		try {
			pw.join(timeout);
		}
		catch(InterruptedException ie) { }
		Process p = pw.getProcess();
		try {
			status = p.exitValue();
		}
		catch(IllegalThreadStateException itse) {
			p.destroy();
			status = p.waitFor();
		}
		out = pw.getStdout().output(timeout);
		err = pw.getStderr().output(timeout);
	}
	
	public SubprocessMonitoringThread start() throws IOException {
		if (_l.isLoggable(Level.FINER))
			_l.log(Level.FINER, "exec: " + Text.join(args));
		Process p = Runtime.getRuntime().exec(args);
		StreamCaptureThread cout = new StreamCaptureThread(p.getInputStream());
		cout.start();
		StreamCaptureThread cerr = new StreamCaptureThread(p.getErrorStream());
		cerr.start();
		p.getOutputStream().close();
		SubprocessMonitoringThread pw = new SubprocessMonitoringThread(p, cout, cerr);
		pw.start();
		return pw;
	}
	
	public static void check(String[] _args, String _extraPath)
			throws InterruptedException, IOException, NonzeroExit {
		Subprocess s = new Subprocess(_args, _extraPath);
		s.run();
		if (s.getStatus() != 0) {
			if (_l.isLoggable(Level.FINER))
				_l.log(Level.FINER, "exec status: " + s.getStatus());
			throw new NonzeroExit(s.getArgs(), s.getStatus());
		}
	}
	
	public static void check(String[] _args)
			throws InterruptedException, IOException, NonzeroExit {
		check(_args, EXTRAPATH_DEFAULT);
	}
	
	public static String checkOutput(String[] _args, String _extraPath)
			throws InterruptedException, IOException, NonzeroExit {
		Subprocess s = new Subprocess(_args, _extraPath);
		s.run();
		if (s.getStatus() != 0) {
			if (_l.isLoggable(Level.FINER))
				_l.log(Level.FINER, "exec status: " + s.getStatus());
			throw new NonzeroExit(s.getArgs(), s.getStatus());
		}
		return s.getOut();
	}
	
	public static String checkOutput(String[] _args)
			throws InterruptedException, IOException, NonzeroExit {
		return checkOutput(_args, EXTRAPATH_DEFAULT);
	}
	
	public static class NonzeroExit extends RuntimeException {
    	private static final long serialVersionUID = 1L;
    	public NonzeroExit(String[] args, int status) {
			super("Process exited with status " + status);
		}
	}
	
	/** Given the name of a program to run, modify it by adding ".exe" if needed. */
	public static String execName(String execStem) {
		if (Misc.isWindows())
			return execStem + ".exe";
		return execStem;
	}
}
