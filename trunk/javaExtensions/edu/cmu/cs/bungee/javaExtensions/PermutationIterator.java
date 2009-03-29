/**
 * 
 */
package edu.cmu.cs.bungee.javaExtensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PermutationIterator implements Iterator {

	private final Object[] objects;
	private final IntegerPermutationGenerator integerPermGenerator;
	private int nRemainingPerms;
	private final List perm;

	public PermutationIterator(List collection) {
		objects = collection.toArray();
		int nObjects = objects.length;
		integerPermGenerator = new IntegerPermutationGenerator(nObjects);
		nRemainingPerms = factorial(nObjects);
		perm = new ArrayList(nObjects);
		for (int i = 0; i < nObjects; i++) {
			perm.add("foo");
		}
	}

	public boolean hasNext() {
		return nRemainingPerms > 0;
	}

	/*
	 * Value is a List ordered the same way as constuctor argument
	 */
	public Object next() {
		nRemainingPerms--;
		int[] integerPerm = integerPermGenerator.next();
		for (int i = 0; i < integerPerm.length; i++) {
			perm.set(i, objects[integerPerm[i]]);
		}
		return Collections.unmodifiableList(perm);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static int factorial(int n) {
		if (n <= 1) // base case
			return 1;
		else
			return n * factorial(n - 1);
	}

}