package edu.cmu.cs.bungee.javaExtensions;

/**
 * A QueueThread where only the most recently added queue entry is relevant. The
 * queue is cleared each time that entry is processed.
 * 
 * @author mad
 * 
 */
public class UpdateThread extends QueueThread {

	/**
	 * @param name useful for debugging
	 * @param deltaPriority this thread's priority relative to the caller's priority
	 */
	public UpdateThread(String name, int deltaPriority) {
		super(name, null, true, deltaPriority);
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
			result = queue.get(queue.size() - 1);
			assert result != null;
			queue.clear();
		}
		return result;
	}

}
