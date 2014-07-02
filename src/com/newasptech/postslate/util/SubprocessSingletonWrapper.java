package com.newasptech.postslate.util;

import java.util.logging.Level;
import java.util.logging.Logger;


public class SubprocessSingletonWrapper {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.util.SubprocessSingletonWrapper");
	private SubprocessMonitoringThread pw;
	public SubprocessSingletonWrapper() {
		_l.log(Level.FINE, "New ProcessWrapper");
		pw = null;
	}
	public SubprocessMonitoringThread get() {
		return pw;
	}
	public void set(SubprocessMonitoringThread _pw) {
		if (pw != null) {
			_l.log(Level.FINE, "Destroy previous process");
			pw.getProcess().destroy();
		}
		_l.log(Level.FINE, "Save new process");
		pw = _pw;
	}
	public boolean isRunning() {
		return (pw != null && pw.getState() != Thread.State.TERMINATED);
	}
	public void kill() {
		if (isRunning())
			pw.getProcess().destroy();
	}
}