/**
 * 
 */
package edu.cmu.cs.bungee.javaExtensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PermutationIterator<V> implements Iterator<List<V>> {

	private final ArrayList<V> objects;
	private final IntegerPermutationIter integerPermGenerator;
	private int nRemainingPerms;
	private final List<V> perm;

	public PermutationIterator(List<V> collection) {
		objects = new ArrayList<V>(collection);
		int nObjects = objects.size();
		integerPermGenerator = new IntegerPermutationIter(nObjects);
		nRemainingPerms = factorial(nObjects);
		perm = new ArrayList<V>(nObjects);
		for (int i = 0; i < nObjects; i++) {
			perm.add(null);
		}
	}

	public boolean hasNext() {
		return nRemainingPerms > 0;
	}

	/*
	 * Value is a List ordered the same way as constuctor argument
	 */
	public List<V> next() {
		nRemainingPerms--;
		int[] integerPerm = integerPermGenerator.next();
		for (int i = 0; i < integerPerm.length; i++) {
			perm.set(i, objects.get(integerPerm[i]));
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