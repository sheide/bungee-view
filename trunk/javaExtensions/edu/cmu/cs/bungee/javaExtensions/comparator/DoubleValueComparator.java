package edu.cmu.cs.bungee.javaExtensions.comparator;

import java.util.Comparator;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Sorts from highest to lowest
 *
 */
public abstract class DoubleValueComparator<V> implements Comparator<V>{

	public int compare(V data1, V data2) {
		return Util.sgn(value(data2) - value(data1));
	}

	public boolean equals(V data1, V data2) {
		return value(data1) == value(data2);
	}

	public abstract double value(V data);

}
