package edu.cmu.cs.bungee.javaExtensions;

import java.util.Comparator;

public abstract class ObjectValueComparator implements Comparator{

	public int compare(Object data1, Object data2) {
		return value(data1).compareTo(value(data2));
	}

	public boolean equals(Object data1, Object data2) {
		return value(data1) == value(data2);
	}

	public abstract Comparable value(Object data);

}
