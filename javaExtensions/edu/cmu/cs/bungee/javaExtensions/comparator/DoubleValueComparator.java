package edu.cmu.cs.bungee.javaExtensions.comparator;

import java.io.Serializable;
import java.util.Comparator;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Sorts from highest to lowest
 *
 */
public abstract class DoubleValueComparator implements Comparator, Serializable {

	public int compare(Object data1, Object data2) {
		return Util.sgn(value(data2) - value(data1));
	}

	public boolean equals(Object data1, Object data2) {
		return value(data1) == value(data2);
	}

	public abstract double value(Object data);

}
