package com.newasptech.postslate.util;


public class SubprocessMonitoringThread extends Thread {
	private Process p;
	private StreamCaptureThread cout, cerr;
	public SubprocessMonitoringThread(Process _p, StreamCaptureThread _cout, StreamCaptureThread _cerr) {
		p = _p;
		cout = _cout;
		cerr = _cerr;
	}
	public Process getProcess() {
		return p;
	}
	public StreamCaptureThread getStdout() {
		return cout;
	}
	public StreamCaptureThread getStderr() {
		return cerr;
	}
	public void run() {
		try {
			p.waitFor();
		}
		catch(InterruptedException ie) { }
	}
}