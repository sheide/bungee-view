package edu.cmu.cs.bungee.javaExtensions.threads;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * An UpdateThread where process takes no arguments. update just means "make sure to call process when you get a chance".
 * 
 * @author mad
 *
 */
public class UpdateNoArgsThread extends UpdateThread {

	/**
	 * @param name useful for debugging
	 * @param deltaPriority this thread's priority relative to the caller's priority
	 */
	public UpdateNoArgsThread(String name, int deltaPriority) {
		super(name, deltaPriority);
	}

	/**
	 * Request that process be called.
	 * @return whether the queue was updated (won't be if there's already a request queued).
	 */
	final public synchronized boolean update() {
		return add(this);
	}

	final public void process(Object ignore) {
		assert Util.ignore(ignore);
		process();
	}

	/**
	 * Override this to carry out the queued request.
	 */
	public void process() {
		Util.err("Should override UpdateNoArgsThread.process");

	}
}
