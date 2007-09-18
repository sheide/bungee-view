package edu.cmu.cs.bungee.javaExtensions;


public class UpdateThread extends QueueThread {

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
