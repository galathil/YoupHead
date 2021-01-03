package app;

import org.tinylog.Logger;

public class YHThread implements Runnable{

	private int entry;
	
	public YHThread(int entry) {
		this.entry=entry;
	}
	
	@Override
	public void run() {
		try {
			YoupHead.downloadPage(entry);
		} catch (Exception e) {
			Logger.error(e);
		}
	}

}
