package edu.cmu.cs.bungee.javaExtensions.comparator;

import java.util.Comparator;

public abstract class IntValueComparator<V> implements Comparator<V> {

	public int compare(V data1, V data2) {
		return value(data2) - value(data1);
	}

	public boolean equals(V data1, V data2) {
		return value(data1) == value(data2);
	}

	public abstract int value(V data);

}
