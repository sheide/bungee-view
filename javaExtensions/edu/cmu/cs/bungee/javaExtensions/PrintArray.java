/*
 * Author: mad Created: Sunday, September 02, 2001 8:53:52 PM Modified: Sunday,
 * September 02, 2001 8:53:52 PM
 */

package edu.cmu.cs.bungee.javaExtensions;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

public class PrintArray {

	public static void printArray(Object a) {
		System.out.println(printArrayInternal(a, "", null));
	}

	public static String printArrayString(Object a) {
		return printArrayInternal(a, "", null);
	}

	public static String printArrayString(Object a, String separator) {
		return printArrayInternal(a, "", separator);
	}

	static String printArrayInternal(Object a, String s, String separator) {
		if (separator == null)
			separator = ", ";
		if (a == null)
			s += "<null>";
		else if (isArray(a)) {
			s += "[";
			if (a instanceof Object[]) {
				Object[] ar = (Object[]) a;
				for (int i = 0; i < ar.length; i++) {
					s = printArrayInternal(ar[i], s, separator);
					if (i < (ar.length - 1))
						s += separator;
				}
			} else if (a instanceof float[]) {
				float[] ar = (float[]) a;
				for (int i = 0; i < ar.length; i++) {
					s += ar[i];
					if (i < (ar.length - 1))
						s += separator;
				}
			} else if (a instanceof double[]) {
				double[] ar = (double[]) a;
				for (int i = 0; i < ar.length; i++) {
					s += ar[i];
					if (i < (ar.length - 1))
						s += separator;
				}
			} else if (a instanceof char[]) {
				char[] ar = (char[]) a;
				for (int i = 0; i < ar.length; i++) {
					s += ar[i];
					if (i < (ar.length - 1))
						s += separator;
				}
			} else if (a instanceof int[]) {
				int[] ar = (int[]) a;
				for (int i = 0; i < ar.length; i++) {
					s += ar[i];
					if (i < (ar.length - 1))
						s += separator;
				}
			} else {
				System.out.println("Can't find match for " + a.getClass());
				s += a;
			}
			s += "]";
		} else if (a instanceof Collection) {
			s += "<";
			Collection ar = (Collection) a;
			Iterator it = ar.iterator();
			while (it.hasNext()) {
				s = printArrayInternal(it.next(), s, separator);
				if (it.hasNext())
					s += separator;
			}
			s += ">";
		} else {
			s += a.toString();
		}
		return s;
	}

	static boolean isArray(Object target) {
		Class targetClass = target.getClass();
		return targetClass.isArray();
	}

	public static void describe(Object target, String intro) {
		if (target == null)
			System.out.println("<null>");
		else {
			Class targetClass = target.getClass();
			Field[] publicFields = targetClass.getFields();
			System.out.println();
			System.out.println(intro + target.toString());
			if (targetClass.isArray())
				printArray(target);
			for (int i = 0; i < publicFields.length; i++) {
				String fieldName = publicFields[i].getName();
				Class typeClass = publicFields[i].getType();
				String fieldType = typeClass.getName();
				System.out.println("Name: " + fieldName + ", Type: "
						+ fieldType);
				try {
					printArray(publicFields[i].get(target));
				} catch (IllegalAccessException e) {
					System.out.println("Exception: " + e);
				}
			}
		}
	}

}
