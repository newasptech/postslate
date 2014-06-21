package com.newasptech.postslate.util;

import java.io.IOException;
import java.io.InputStream;

public class StreamCaptureThread extends Thread {
	private InputStream istr;
	private StringBuilder osb = new StringBuilder();
	private IOException caught = null;
	static private final int BLOCK_SIZE = 1024;
	
	public StreamCaptureThread(InputStream _istr) {
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