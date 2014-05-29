/*
 * Copyright (C) 2014 New Aspect Technologies, Inc. All rights reserved.
 * Distribution and use are permitted under the terms of the LICENSE.txt
 * file included with the source code of this project.
 */
package com.newasptech.postslate.util;

import java.io.File;
import java.io.InputStream;
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
		Capture cout = new Capture(p.getInputStream());
		cout.start();
		Capture cerr = new Capture(p.getErrorStream());
		cerr.start();
		p.getOutputStream().close();
		status = p.waitFor();
		out = cout.output();
		err = cerr.output();
	}
	
	public void run(int timeout) throws InterruptedException, IOException {
		if (_l.isLoggable(Level.FINER))
			_l.log(Level.FINER, "exec: " + Text.join(args));
		Process p = Runtime.getRuntime().exec(args);
		Capture cout = new Capture(p.getInputStream());
		cout.start();
		Capture cerr = new Capture(p.getErrorStream());
		cerr.start();
		p.getOutputStream().close();
		ProcessWrapper pw = new ProcessWrapper(p);
		pw.start();
		try {
			pw.join(timeout);
		}
		catch(InterruptedException ie) { }
		try {
			status = p.exitValue();
		}
		catch(IllegalThreadStateException itse) {
			p.destroy();
			status = p.waitFor();
		}
		out = cout.output(timeout);
		err = cerr.output(timeout);
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
	
	class ProcessWrapper extends Thread {
		private Process p;
		public ProcessWrapper(Process _p) {
			p = _p;
		}
		public Process getProcess() {
			return p;
		}
		public void run() {
			try {
				p.waitFor();
			}
			catch(InterruptedException ie) { }
		}
	}
	
	class Capture extends Thread {
		private InputStream istr;
		private StringBuilder osb = new StringBuilder();
		private IOException caught = null;
		static private final int BLOCK_SIZE = 1024;
		
		public Capture(InputStream _istr) {
			super();
			istr = _istr;
		}
		
		public void run() {
			byte outputChunk[] = new byte[BLOCK_SIZE];
			try {
				int bytesRead = 0;
				boolean more = true;
				while (more) {
					bytesRead = istr.read(outputChunk, 0, BLOCK_SIZE);
					switch(bytesRead) {
					case -1:
						more = false;
						break;
					case 0:
						break;
					default:
						osb.append(new String(outputChunk, 0, bytesRead));
					}
				}
			}
			catch(IOException e) {
				caught = e;
			}
			finally {
				try {
					istr.close();
				}
				catch(Exception x) {}
			}
		}
		
		public void finish() throws InterruptedException, IOException {
			join();
			if (caught != null) throw caught;
		}
		
		public void finish(int timeout) throws InterruptedException, IOException {
			join(timeout);
			if (caught != null) throw caught;
		}
		
		public String output() throws InterruptedException, IOException {
			finish();
			return osb.toString();
		}
		
		public String output(int timeout) throws InterruptedException, IOException {
			finish(timeout);
			return osb.toString();
		}
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
