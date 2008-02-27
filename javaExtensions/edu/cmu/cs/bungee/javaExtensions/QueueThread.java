package edu.cmu.cs.bungee.javaExtensions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A thread that maintains a FIFO queue of objects on which it calls process.
 * 
 * @author mad
 *
 */
public class QueueThread extends Thread {

	// private Object object;

	 List queue;

	private final boolean unique;

	private boolean processing = false;

	/**
	 * @param name useful for debugging
	 * @param deltaPriority this thread's priority relative to the caller's priority
	 */
	public QueueThread(String name, int deltaPriority) {
		this(name, null, true, deltaPriority);
	}

	/**
	 * @param name useful for debugging
	 * @param init_queue initial queue
	 * @param _unique if true, don't add duplicates to queue
	 * @param deltaPriority this thread's priority relative to the caller's priority
	 */
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

	/**
	 *  put o on the queue, unless unique and it's already there.
	 * @param o
	 * @return whether anything was added to the queue.
	 */
	final public synchronized boolean add(Object o) {
		boolean result = !unique || !queue.contains(o);
		if (result) {
			queue.add(o);
			notify();
		}
		return result;
	}

	/**
	 *  remove o from the queue
	 * @param o
	 * @return whether anything was removed from the queue.
	 */
	final public synchronized boolean remove(Object o) {
		boolean result = queue.contains(o);
		if (result) {
			queue.remove(o);
		}
		return result;
	}

	 synchronized Object get() {
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

	/**
	 * @return true iff our queue is empty (and we haven't exit'ed). Might or
	 *         might not be processing the last queue entry.
	 */
	final public boolean isUpToDate() {
		boolean result = queue != null && queue.isEmpty();
		// Util.print("isUpToDate return " + result);
		return result;
	}

	/**
	 * @return true if nowhere to go and nothing to do.
	 */
	final public boolean isIdle() {
		return !processing && queue != null && queue.isEmpty();
	}

	/**
	 * Clear the queue and stop accepting add's to it. Any current process call will complete.
	 */
	 public synchronized void exit() {
		if (queue != null) {
			Util.print("...exiting " + getName() + " priority=" + getPriority());
			queue = null;
			notify();
		}
	}
	
	/**
	 * Called when this Thread starts.
	 */
	public void init() {
		// Override this
//		testThreadProblems();
	}
	
	private void testThreadProblems() {
		try {
			long delay = (long) (Math.random()*2000);
			Util.print(this + " " + delay);
			sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	final public void run() {
		init();
		Object o;
		while ((o = get()) != null) {
			try {
				processing = true;
//				testThreadProblems();
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

	/**
	 * This is the whole point of QueueThread. Override this to process each queue addition.
	 * 
	 * @param o arbitrary argument from the queue.
	 */
	public void process(Object o) {
		assert Util.ignore(o);
		Util.err("Should override QueueThread.process");
	}
}
