package edu.cmu.cs.bungee.javaExtensions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class QueueThread extends Thread {

	// private Object object;

	protected List queue;

	protected final boolean unique;
	
	protected boolean processing = false;

	public QueueThread(String name, int deltaPriority) {
		this(name, null, true, deltaPriority);
	}

	public QueueThread(String name, Object[] init_queue, boolean _unique,
			int deltaPriority) {
		unique = _unique;
		setName(name);
		if (init_queue == null)
			init_queue = new Object[0];
		queue = new LinkedList(Arrays.asList(init_queue));
		if (deltaPriority != 0)
			setPriority(getPriority() + deltaPriority);
		// object = o;
	}

	public synchronized boolean add(Object o) {
		boolean result = !unique || !queue.contains(o);
		if (result) {
			queue.add(o);
			notify();
		}
		return result;
	}

	protected synchronized Object get() {
		Object result = null;
		while (isUpToDate()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// Our wait is over
			}
		}
		if (queue != null) {
			assert !queue.isEmpty();
			result = queue.remove(0);
			assert result != null;
		}
		return result;
	}

	public boolean isUpToDate() {
		boolean result = queue != null && queue.isEmpty();
		// Util.print("isUpToDate return " + result);
		return result;
	}
	
	public boolean isIdle() {
		return !processing && queue != null && queue.isEmpty();
	}

	public synchronized void exit() {
		if (queue != null) {
			Util.print("...exiting " + getName());
			queue = null;
			notify();
		}
	}

	public void run() {
		Object o;
		while ((o = get()) != null) {
			try {
				processing = true;
				process(o);
				// }
				// if (q != null) {
				// q.nameGetter = null;
				// q = null;
				// }
			} catch (Throwable e) {
				System.err.println("Ignoring exception in " + this + ":" + e);
				e.printStackTrace();
			}
			processing = false;
		}
		exit();
	}

	// oVERRIDE THIS
	public void process(Object o) {
		assert Util.ignore(o);
		Util.err("Should override QueueThread.process");
	}
}
