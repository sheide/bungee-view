package edu.cmu.cs.bungee.javaExtensions;

import java.io.Serializable;
import java.util.Comparator;

public abstract class ValueComparator implements Comparator, Serializable {

	public int compare(Object data1, Object data2) {
		return value(data2) - value(data1);
	}

	public boolean equals(Object data1, Object data2) {
		return value(data1) == value(data2);
	}

	public abstract int value(Object data);

}
