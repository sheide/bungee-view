package edu.cmu.cs.bungee.javaExtensions;

import java.util.Iterator;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;

public abstract class DefaultLabeled<V> implements Labeled<V> {

	private int[] cumCounts;
	private Hashtable indexes;

	// public abstract int count(Object o);
	//
	// public abstract void drawLabel(Object o, int from, int to);

	private void ensureInitted() {
		if (cumCounts == null) {
			updateCounts();
		}		
	}
	
	public void updateCounts() {
			int n = 0;
			for (Iterator<V> it = getChildIterator(); it.hasNext();) {
				it.next();
				n++;
			}
			indexes = new Hashtable();
			cumCounts = new int[n];
			int i = 0;
			int cum = 0;
			for (Iterator<V> it = getChildIterator(); it.hasNext();) {
				V o = it.next();
				cum += count(o);
				indexes.put(o, new Integer(i));
				cumCounts[i++] = cum;
			}
//			Util.print("ensureInitted cumCount: " + cum);
		}

	public Iterator<V> cumCountChildIterator(int minCount, int maxCount) {
		return getChildIterator();
	}

	public int cumCountInclusive(V o) {
		ensureInitted();
		return cumCounts[((Integer) indexes.get(o)).intValue()];
	}

	public Iterator<V> getChildIterator(V from, V to) {
		return new ChildIterator(from, to);
	}

	private class ChildIterator implements Iterator<V> {
		final Iterator<V> base;
		V next;
		final V to;
		private boolean seenTo = false;

		ChildIterator(V from, V to) {
			base = getChildIterator();
			this.to = to;
			while (base.hasNext() && next != from && !seenTo) {
				next = base.next();
				if (next == to) {
					seenTo = true;
					next = null;
				}
			}
		}

		public boolean hasNext() {
			if (next == null && !seenTo) {
				if (base.hasNext()) {
					next = base.next();
					if (next == to)
						seenTo = true;
				}
			}
			return next != null;
		}

		public V next() {
			if (hasNext()) {
				V result = next;
				next = null;
				return result;
			} else {
				throw new IllegalAccessError();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public int priority(V o) {
		return count(o);
	}

}
