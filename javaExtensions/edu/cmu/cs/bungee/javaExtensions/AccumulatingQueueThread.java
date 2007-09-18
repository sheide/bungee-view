package edu.cmu.cs.bungee.javaExtensions;

public class AccumulatingQueueThread extends QueueThread {

		public AccumulatingQueueThread(String name, int deltaPriority) {
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
				result = queue.toArray();
				assert result != null;
				queue.clear();
			}
			return result;
		}


}
