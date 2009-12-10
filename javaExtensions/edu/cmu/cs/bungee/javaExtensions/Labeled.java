package edu.cmu.cs.bungee.javaExtensions;

import java.util.Iterator;

public interface Labeled<V> {

	Iterator<V> getChildIterator(V from, V to);

	Iterator<V> getChildIterator();

	Iterator<V> cumCountChildIterator(int minCount, int maxCount);

	int count(V o);

	int cumCountInclusive(V o);

	void updateCounts();

	int priority(V o);

	void drawLabel(V o, int from, int to);

}
