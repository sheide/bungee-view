package edu.cmu.cs.bungee.javaExtensions.comparator;

import java.util.Comparator;

public abstract class ObjectValueComparator<V extends Comparable<V>> implements Comparator<V>{

	public int compare(V data1, V data2) {
		return value(data1).compareTo(value(data2));
	}

	public boolean equals(V data1, V data2) {
		return value(data1) == value(data2);
	}

	public abstract Comparable<Object> value(V data);

}
