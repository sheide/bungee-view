package edu.cmu.cs.bungee.javaExtensions;

public class UpdateNoArgsThread extends UpdateThread {

	public UpdateNoArgsThread(String name, int deltaPriority) {
		super(name, deltaPriority);
	}

	public synchronized boolean update() {
		return add(this);
	}

	final public void process(Object ignore) {
		assert Util.ignore(ignore);
		process();
	}

	// oVERRIDE THIS
	public void process() {
		Util.err("Should override UpdateNoArgsThread.process");

	}
}
