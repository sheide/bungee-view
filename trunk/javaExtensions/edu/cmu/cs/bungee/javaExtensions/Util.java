/* 

 Created on Mar 4, 2005

 The Bungee View applet lets you search, browse, and data-mine an image collection.  
 Copyright (C) 2006  Mark Derthick

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.  See gpl.html.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 You may also contact the author at 
 mad@cs.cmu.edu, 
 or at
 Mark Derthick
 Carnegie-Mellon University
 Human-Computer Interaction Institute
 Pittsburgh, PA 15213

 */

package edu.cmu.cs.bungee.javaExtensions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * misc static functions on numbers, strings, images, treating arrays as sets
 * 
 */
public final class Util {

	private Util() {
		// Disallow instantiation
	}

	/**
	 * Stick this in an assert to suppress compiler warnings: assert
	 * ignore(ignore);
	 * 
	 * @param ignore
	 * @return true
	 */
	public static boolean ignore(Object ignore) {
		return ignore == null || true;
	}

	/**
	 * @param ignore
	 *            a variable for which to ignore never-read warnings
	 * @return true
	 */
	public static boolean ignore(boolean ignore) {
		return ignore || true;
	}

	/**
	 * @param ignore
	 *            a variable for which to ignore never-read warnings
	 * @return true
	 */
	public static boolean ignore(int ignore) {
		return ignore == 0 || ignore != 0;
	}

	/**
	 * @param ignore
	 *            a variable for which to ignore never-read warnings
	 * @return true
	 */
	public static boolean ignore(double ignore) {
		return ignore == 0 || ignore != 0;
	}

	/**
	 * @param n
	 * @return -1, 0, or 1 depending on signum of n
	 */
	public static int sgn(int n) {
		if (n < 0)
			return -1;
		else if (n > 0)
			return 1;
		else
			return 0;
	}

	/**
	 * @param n
	 * @return -1, 0, or 1 depending on signum of n
	 */
	public static int sgn(double n) {
		if (n < 0)
			return -1;
		else if (n > 0)
			return 1;
		else
			return 0;
	}

	/**
	 * @param x
	 * @param y
	 * @return minimum of x and y
	 */
	public static double min(double x, double y) {
		if (x < y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @param y
	 * @return minimum of x and y
	 */
	public static float min(float x, float y) {
		if (x < y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @param y
	 * @return minimum of x and y
	 */
	public static int min(int x, int y) {
		if (x < y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @param y
	 * @return maximum of x and y
	 */
	public static double max(double x, double y) {
		if (x > y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @param y
	 * @return maximum of x and y
	 */
	public static float max(float x, float y) {
		if (x > y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @param y
	 * @return maximum of x and y
	 */
	public static int max(int x, int y) {
		if (x > y)
			return x;
		else
			return y;
	}

	/**
	 * @param x
	 * @return (int) (x + 0.5)
	 */
	public static int round(double x) {
		assert x >= 0;
		return (int) (x + 0.5);
	}

	// public static float blend(float zeroToOne, float start, float end) {
	// return Math.round(start + zeroToOne * (end - start));
	// }

	/**
	 * @param x1
	 * @param x2
	 * @param zeroToOne
	 * @return x + zeroToOne * (x2 - x)
	 */
	public static double interpolate(double x1, double x2, float zeroToOne) {
		if (zeroToOne == 1.0f)
			// avoid roundoff errors
			return x2;
		return x1 + zeroToOne * (x2 - x1);
	}

	/**
	 * @param val
	 * @param minv
	 * @param maxv
	 * @return the int in the range [minv, maxv] closest to val
	 */
	public static int constrain(int val, int minv, int maxv) {
		assert minv <= maxv : minv + " " + maxv;
		return Math.min(Math.max(val, minv), maxv);
	}

	/**
	 * @param val
	 * @param minv
	 * @param maxv
	 * @return the float in the range [minv, maxv] closest to val
	 */
	public static float constrain(float val, float minv, float maxv) {
		assert minv <= maxv : minv + " " + maxv;
		return Math.min(Math.max(val, minv), maxv);
	}

	/**
	 * @param val
	 * @param minv
	 * @param maxv
	 * @return the double in the range [minv, maxv] closest to val
	 */
	public static double constrain(double val, double minv, double maxv) {
		assert minv <= maxv : minv + " " + maxv;
		// print(val + " " + minv + "-" + maxv);
		return Math.min(Math.max(val, minv), maxv);
	}

	public static float[] getHSBcomponents(Color color) {
		float[] result = new float[3];
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(),
				result);
		return result;
	}

	private static final float colorComponentChangeFactor = 2.0f;

	/**
	 * @param color
	 * @return Increases brightness. If factor < 1 it will get dimmer.
	 */
	public static Color fade(Color color) {
		return brighten(color, 1.0f / colorComponentChangeFactor);
	}

	/**
	 * @param color
	 * @return Increases brightness. If factor < 1 it will get dimmer.
	 */
	public static Color brighten(Color color) {
		return brighten(color, colorComponentChangeFactor);
	}

	/**
	 * @param color
	 * @param factor
	 * @return Multiplies brightness by factor. If factor < 1 it will get
	 *         dimmer.
	 */
	public static Color brighten(Color color, float factor) {
		if (factor <= 0.0)
			factor = colorComponentChangeFactor;
		float[] hsb = getHSBcomponents(color);
		return Color.getHSBColor(hsb[0], hsb[1], Math
				.min(1.0f, hsb[2] * factor));
	}

	/**
	 * @param color
	 * @return Increases brightness, and decreases saturation. If factor < 1 it
	 *         will get darker.
	 */
	public static Color lighten(Color color) {
		return lighten(color, colorComponentChangeFactor);
	}

	/**
	 * @param color
	 * @param factor
	 * @return Multiplies brightness by factor, and divides saturation by
	 *         factor. If factor < 1 it will get darker.
	 */
	public static Color lighten(Color color, float factor) {
		if (factor <= 0.0)
			factor = colorComponentChangeFactor;
		float[] hsb = getHSBcomponents(color);
		return Color.getHSBColor(hsb[0], Math.min(1.0f, hsb[1] / factor), Math
				.min(1.0f, hsb[2] * factor));
	}

	/**
	 * @param s
	 * @return nOccurrences(s, '\n') + 1
	 */
	public static int nLines(String s) {
		return nOccurrences(s, '\n') + 1;
	}

	static int nthOccurrenceIndex(String s, char c, int n) {
		// -1 means either n=0 or there are fewer than n occurrences
		int index = -1;
		while (n > 0 && (index = s.indexOf(c, index + 1)) >= 0)
			n -= 1;
		return index;
	}

	public static String subLines(String s, int firstLine, int nLines) {
		int firstChar = nthOccurrenceIndex(s, '\n', firstLine) + 1;
		if (firstChar <= 0 && firstLine > 0)
			return "";
		int lastChar = nthOccurrenceIndex(s, '\n', firstLine + nLines);
		if (lastChar >= firstChar)
			return s.substring(firstChar, lastChar);
		else if (firstChar > 0)
			return s.substring(firstChar);
		else
			return s;
	}

	/**
	 * @param n
	 * @return format n with commas separating thousands, millions, etc
	 */
	public static String addCommas(int n) {
		// println("addCommas " + n);
		String s = Integer.toString(n);
		int len = s.length();
		if (len >= 4) {
			for (int i = len - 3; i > 0; i = i - 3)
				s = s.substring(0, i) + "," + s.substring(i);
		}
		return s;
	}

	/**
	 * @param s
	 * @return add s or es
	 */
	public static String pluralize(String s) {
		// Util.print("pluraize " + s);
		if (s.charAt(s.length() - 1) != 's')
			s += "s";
		else
			s += "es";
		return s;
	}

	/**
	 * add s or es
	 * 
	 * @param s
	 */
	public static void pluralize(StringBuffer s) {
		if (s.charAt(s.length() - 1) != 's')
			s.append("s");
		else
			s.append("es");
	}

	/**
	 * @param alpha
	 * @return c-A for A, etc
	 */
	public static char controlChar(char alpha) {
		char result = (char) (alpha - 64);
		assert Character.isISOControl(result) : alpha;
		return result;
	}

	/**
	 * {a, e, i, o, u, A, E, I, O, U}
	 */
	public static final char[] vowels = { 'a', 'e', 'i', 'o', 'u', 'A', 'E',
			'I', 'O', 'U' };

	/**
	 * @param noun
	 * @return "a" or "an"
	 */
	public static String indefiniteArticle(String noun) {
		String result = " a ";
		char c = noun.charAt(0);
		if (Util.isMember(vowels, c))
			result = " an ";
		// Util.print("indef " + noun + " " + result);
		return result;
	}

	/**
	 * @param a
	 * @param connective
	 * @return public static String (["sex", "lies", "videotape"], " and ")
	 *         returns "sex, lies, and videotape".
	 */
	public static String arrayToEnglish(Object[] a, String connective) {
		String result = null;
		int len = a.length;
		if (len == 1)
			result = (String) a[0];
		else if (len > 0) {
			Arrays.sort(a);
			if (len <= 2)
				result = join(a, connective);
			else {
				StringBuffer buf = new StringBuffer(len * 20);
				for (int i = 0; i < a.length - 1; i++) {
					buf.append(a[i]);
					buf.append(", ");
				}
				buf.append(connective);
				buf.append(a[a.length - 1]);
				result = buf.toString();
			}
		}
		return result;
	}

	public static int nOccurrences(String s, char c) {
		int n = 0;
		int index = -1;
		while ((index = s.indexOf(c, index + 1)) >= 0)
			n += 1;
		return n;
	}

	public static int nOccurrences(int[] a, int i) {
		int n = 0;
		for (int j = 0; j < a.length; j++)
			if (a[j] == i)
				n++;
		return n;
	}

	static final Pattern semicolonPattern = Pattern.compile(";");

	/**
	 * @param s
	 * @return more efficient than s.split(";")
	 */
	public static String[] splitSemicolon(String s) {
		return semicolonPattern.split(s);
	}

	static final Pattern commaPattern = Pattern.compile(",");

	/**
	 * @param s
	 * @return more efficient than s.split(",")
	 */
	public static String[] splitComma(String s) {
		assert s != null;
		return commaPattern.split(s);
	}

	/**
	 * @param s
	 * @param regExp
	 * @return s.split(regExp) converted to int[]
	 */
	public static int[] splitInts(String s, String regExp) {
		String[] strings = s.split(regExp);
		int[] ints = new int[strings.length];
		for (int i = 0; i < strings.length; i++)
			ints[i] = Integer.parseInt(strings[i]);
		return ints;
	}

	public static String intsToString(int[] ints) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < ints.length; i++) {
			b.append(ints[i]);
			if (i < ints.length - 1) {
				b.append(",");
			}
		}
		return b.toString();
	}

	// /**
	// * @deprecated
	// */
	// public static Object[] union(Object[] a1, Object[] a2, Class type) {
	// if (a1 == null || a1.length == 0)
	// return a2;
	// else if (a2 == null || a2.length == 0)
	// return a1;
	// else {
	// int n = 0;
	// for (int i = 0; i < a2.length; i++)
	// n += nOccurrences(a1, a2[i]);
	// if (n == 0) {
	// return append(a1, a2, type);
	// } else {
	// Object[] a = (Object[]) java.lang.reflect.Array.newInstance(
	// type, a1.length + a2.length - n);
	// int index = a1.length;
	// System.arraycopy(a1, 0, a, 0, index);
	// for (int i = 0; i < a2.length; i++) {
	// if (!isMember(a1, a2[i]))
	// a[index++] = a2[i];
	// }
	// return a;
	// }
	// }
	// }
	//
	// public static int[] union(int[] a1, int[] a2) {
	// assert !hasDuplicates(a1);
	// if (a1 == null || a1.length == 0)
	// return a2;
	// else if (a2 == null || a2.length == 0)
	// return a1;
	// else {
	// int n = 0;
	// for (int i = 0; i < a2.length; i++)
	// n += nOccurrences(a1, a2[i]);
	// if (n == 0) {
	// return append(a1, a2);
	// } else {
	// int[] a = new int[a1.length + a2.length - n];
	// int index = a1.length;
	// System.arraycopy(a1, 0, a, 0, index);
	// for (int i = 0; i < a2.length; i++) {
	// if (!isMember(a1, a2[i]))
	// a[index++] = a2[i];
	// }
	// return a;
	// }
	// }
	// }

	/**
	 * @deprecated
	 */
	public static Object[] append(Object[] a1, Object[] a2, Class type) {
		if (a1 == null || a1.length == 0)
			return a2;
		else if (a2 == null || a2.length == 0)
			return a1;
		else {
			Object[] a = (Object[]) java.lang.reflect.Array.newInstance(type,
					a1.length + a2.length);
			System.arraycopy(a1, 0, a, 0, a1.length);
			System.arraycopy(a2, 0, a, a1.length, a2.length);
			return a;
		}
	}

	/**
	 * @deprecated
	 */
	public static String[] append(String[] a1, String[] a2) {
		if (a1 == null)
			return a2;
		else if (a2 == null)
			return a1;
		else {
			String[] a = new String[a1.length + a2.length];
			System.arraycopy(a1, 0, a, 0, a1.length);
			System.arraycopy(a2, 0, a, a1.length, a2.length);
			return a;
		}
	}

	/**
	 * @deprecated
	 */
	public static int[] append(int[] a1, int[] a2) {
		if (a1 == null)
			return a2;
		else if (a2 == null)
			return a1;
		else {
			int[] a = new int[a1.length + a2.length];
			System.arraycopy(a1, 0, a, 0, a1.length);
			System.arraycopy(a2, 0, a, a1.length, a2.length);
			return a;
		}
	}

	/**
	 * @deprecated
	 */
	public static double[] append(double[] a1, double[] a2) {
		if (a1 == null)
			return a2;
		else if (a2 == null)
			return a1;
		else {
			double[] a = new double[a1.length + a2.length];
			System.arraycopy(a1, 0, a, 0, a1.length);
			System.arraycopy(a2, 0, a, a1.length, a2.length);
			return a;
		}
	}

	public static Object[] copy(Object[] a, Class type) {
		if (a == null)
			return a;
		Object[] a2 = (Object[]) Array.newInstance(type, a.length);
		System.arraycopy(a, 0, a2, 0, a.length);
		return a2;
	}

	public static boolean hasDuplicates(int[] a) {
		if (a != null)
			for (int i = 0; i < a.length; i++) {
				if (nOccurrences(a, a[i]) > 1)
					return true;
			}
		return false;
	}

	public static boolean hasDuplicates(Object[] a) {
		if (a != null) {
			int n = a.length;
			if (n < 20) {
				for (int i = 0; i < n; i++) {
					if (nOccurrences(a, a[i]) > 1)
						return true;
				}
			} else {
				Hashtable t = new Hashtable();
				for (int i = 0; i < n; i++) {
					if (t.get(a[i]) != null)
						return true;
					else
						t.put(a[i], a);
				}
			}
		}
		return false;
	}

	// public static Object[] setIntersection(Object[] a1, Object[] p, Class
	// type) {
	// int n = 0;
	// if (p != null && a1 != null) {
	// for (int i = 0; i < p.length; i++)
	// n += nOccurrences(a1, p[i]);
	// }
	// if (n == 0)
	// return null;
	// Object[] a = (Object[]) java.lang.reflect.Array.newInstance(type, n);
	// int j = 0;
	// for (int i = 0; i < a1.length; i++)
	// if (isMember(p, a1[i]))
	// Array.set(a, j++, a1[i]);
	// return a;
	// }

	public static int intersectionCardinalilty(int[] a1, int[] a2) {
		int n = 0;
		if (a2 != null && a1 != null) {
			for (int i = 0; i < a2.length; i++)
				if (isMember(a1, a2[i]))
					n++;
		}
		return n;
	}

	public static int intersectionCardinaliltySorted(int[] a1, int[] a2) {
		assert !hasDuplicates(a1);
		assert !hasDuplicates(a2);
		int n = 0;
		int index = 0;
		if (a2 != null && a1 != null) {
			int a1l = a1.length;
			int a2l = a2.length;
			for (int i = 0; i < a2l; i++) {
				assert i == 0 || a2[i - 1] < a2[i];
				int elt2 = a2[i];
				int elt1 = elt2 + 1;
				while (index < a1l && (elt1 = a1[index]) < elt2) {
					index++;
					assert index == a1l || a1[index - 1] < a1[index];
				}
				if (elt1 == elt2)
					n++;
			}
		}
		return n;
	}

	public static int intersectionCardinalilty(Object[] a1, Object[] a2) {
		int n = 0;
		if (a2 != null && a1 != null) {
			for (int i = 0; i < a2.length; i++)
				if (isMember(a1, a2[i]))
					n++;
		}
		return n;
	}

	// public static boolean intersects(Object[] a1, Object[] a2) {
	// if (a2 != null && a1 != null) {
	// for (int i = 0; i < a2.length; i++)
	// if (isMember(a1, a2[i]))
	// return true;
	// }
	// return false;
	// }
	//
	// public static int[] setIntersection(int[] a1, int[] a2) {
	// int n = intersectionCardinalilty(a1, a2);
	// int[] a = new int[n];
	// int j = 0;
	// for (int i = 0; i < a1.length; i++)
	// if (isMember(a2, a1[i]))
	// a[j++] = a1[i];
	// return a;
	// }
	//
	// public static int[] setIntersectionSorted(int[] a1, int[] a2) {
	// int n = intersectionCardinaliltySorted(a1, a2);
	// int[] a = new int[n];
	// int j = 0;
	// int index = 0;
	// int a1l = a1.length;
	// int a2l = a2.length;
	// for (int i = 0; i < a1l; i++) {
	// int elt1 = a1[i];
	// int elt2 = elt1 + 1;
	// while (index < a2l && (elt2 = a2[index]) < elt1) {
	// index++;
	// }
	// if (elt1 == elt2)
	// a[j++] = elt1;
	// }
	// return a;
	// }

	/**
	 * @deprecated
	 */
	public static Object[] setDifference(Object[] a1, Object[] a2, Class type) {
		if (a1 == null || a2 == null)
			return a1;
		int n = intersectionCardinalilty(a1, a2);
		if (n == 0)
			return a1;
		Object[] a = (Object[]) java.lang.reflect.Array.newInstance(type,
				a1.length - n);
		int j = 0;
		for (int i = 0; i < a1.length; i++)
			if (!isMember(a2, a1[i]))
				Array.set(a, j++, a1[i]);
		return a;
	}

	public static int[] setDifference(int[] a1, int[] a2) {
		if (a1 == null || a2 == null)
			return a1;
		int n = intersectionCardinalilty(a1, a2);
		if (n == 0)
			return a1;
		int[] a = new int[a1.length - n];
		int j = 0;
		for (int i = 0; i < a1.length; i++)
			if (!isMember(a2, a1[i]))
				a[j++] = a1[i];
		return a;
	}

	// public static int[] setDifferenceSorted(int[] a1, int[] a2) {
	// if (a1 == null || a2 == null)
	// return a1;
	// int n = intersectionCardinaliltySorted(a1, a2);
	// if (n == 0)
	// return a1;
	// int j = 0;
	// int index = 0;
	// int a1l = a1.length;
	// int a2l = a2.length;
	// int[] a = new int[a1l - n];
	// for (int i = 0; i < a1l; i++) {
	// int elt1 = a1[i];
	// int elt2 = elt1 + 1;
	// while (index < a2l && (elt2 = a2[index]) < elt1) {
	// index++;
	// }
	// if (elt1 != elt2)
	// a[j++] = elt1;
	// }
	// return a;
	// }
	//
	// public static int[] symmetricSetDifference(int[] a1, int[] a2) {
	// return append(setDifference(a1, a2), setDifference(a2, a1));
	// }
	//
	// public static Object[] symmetricSetDifference(Object[] a1, Object[] a2,
	// Class type) {
	// return append(setDifference(a1, a2, type), setDifference(a2, a1, type),
	// type);
	// }

	// public static int nOccurrences(Object[] a1, Object p) {
	// int result = 0;
	// for (int i = 0; i < a1.length; i++)
	// if (a1[i] == p)
	// result++;
	// return result;
	// }

	public static int nOccurrences(Object[] a1, Object p) {
		int result = 0;
		if (a1 != null) {
			for (int i = 0; i < a1.length; i++)
				if (equalsNullOK(a1[i], p))
					result++;
		}
		return result;
	}

	// public static int[] delete(int[] a1, int p) {
	// int n = nOccurrences(a1, p);
	// if (n == 0)
	// return a1;
	// int[] a = new int[a1.length - n];
	// int j = 0;
	// for (int i = 0; i < a1.length; i++)
	// if (a1[i] != p)
	// a[j++] = a1[i];
	// return a;
	// }

	/**
	 * @deprecated
	 */
	public static Object delete(Object[] a1, Object p, Class type) {
		if (a1 == null)
			return a1;
		int n = nOccurrences(a1, p);
		if (n == 0)
			return a1;
		Object a = java.lang.reflect.Array.newInstance(type, a1.length - n);
		int j = 0;
		for (int i = 0; i < a1.length; i++)
			if (a1[i] != p)
				Array.set(a, j++, a1[i]);
		return a;
	}

	/**
	 * @deprecated
	 */
	public static Object deleteIndex(Object[] a1, int index, Class type) {
		int oldN = a1.length;
		assert index >= 0;
		assert index < oldN;
		Object a = java.lang.reflect.Array.newInstance(type, oldN - 1);
		System.arraycopy(a1, 0, a, 0, index);
		System.arraycopy(a1, index + 1, a, index, oldN - index - 1);
		return a;
	}

	// public static int[] deleteIndex(int[] a1, int index) {
	// int oldN = a1.length;
	// assert index >= 0;
	// assert index < oldN;
	// int[] a = new int[oldN - 1];
	// System.arraycopy(a1, 0, a, 0, index);
	// System.arraycopy(a1, index + 1, a, index, oldN - index - 1);
	// return a;
	// }
	//
	// public static Object deleteEquals(Object[] a1, Object p, Class type) {
	// int n = nOccurrences(a1, p);
	// if (n == 0)
	// return a1;
	// Object[] a = (Object[]) java.lang.reflect.Array.newInstance(type,
	// a1.length - n);
	// int j = 0;
	// for (int i = 0; i < a1.length; i++) {
	// if (!equalsNullOK(a1[i], p))
	// Array.set(a, j++, a1[i]);
	// }
	// return a;
	// }

	/**
	 * @deprecated
	 */
	public static Object push(Object[] a1, Object p, Class type) {
		int a1_length = a1 == null ? 0 : a1.length;
		Object a = java.lang.reflect.Array.newInstance(type, a1_length + 1);
		Array.set(a, 0, p);
		if (a1_length > 0)
			System.arraycopy(a1, 0, a, 1, a1_length);
		return a;
	}

	/**
	 * @deprecated
	 */
	public static Object endPush(Object[] a1, Object p, Class type) {
		int a1_length = a1 == null ? 0 : a1.length;
		Object a = java.lang.reflect.Array.newInstance(type, a1_length + 1);
		Array.set(a, a1_length, p);
		if (a1_length > 0)
			System.arraycopy(a1, 0, a, 0, a1_length);
		return a;
	}

	/**
	 * @deprecated
	 */
	public static int[] push(int[] a1, int n) {
		int[] a;
		if (a1 == null) {
			a = new int[1];
		} else {
			a = new int[a1.length + 1];
			System.arraycopy(a1, 0, a, 1, a1.length);
		}
		a[0] = n;
		return a;
	}

	// public static char[] push(char[] a1, char n) {
	// char[] a;
	// if (a1 == null) {
	// a = new char[1];
	// } else {
	// a = new char[a1.length + 1];
	// System.arraycopy(a1, 0, a, 1, a1.length);
	// }
	// a[0] = n;
	// return a;
	// }
	//
	// public static int[] endPush(int[] a1, int n) {
	// if (a1 == null)
	// return push(a1, n);
	// int[] a = new int[a1.length + 1];
	// a[a1.length] = n;
	// System.arraycopy(a1, 0, a, 0, a1.length);
	// return a;
	// }
	//
	// public static char[] endPush(char[] a1, char n) {
	// if (a1 == null)
	// return push(a1, n);
	// char[] a = new char[a1.length + 1];
	// a[a1.length] = n;
	// System.arraycopy(a1, 0, a, 0, a1.length);
	// return a;
	// }
	//	
	//
	// public static int[] subArray(int[] a, int start) {
	// return subArray(a, start, a.length - 1);
	// }

	/**
	 * subArray includes the end'th element, so end should be less than a.length
	 * 
	 * @deprecated
	 */
	public static int[] subArray(int[] a, int start, int end) {
		assert start <= end;
		assert end < a.length;
		int[] result = new int[end - start + 1];
		System.arraycopy(a, start, result, 0, end - start + 1);
		return result;
	}

	// // subArray includes the end'th element, so end should be less than
	// a.length
	// public static float[] subArray(float[] a, int start, int end) {
	// assert start <= end;
	// assert end < a.length;
	// float[] result = new float[end - start + 1];
	// System.arraycopy(a, start, result, 0, end - start + 1);
	// return result;
	// }

	/**
	 * @deprecated
	 */
	public static Object[] subArray(Object[] a, int start, Class type) {
		return subArray(a, start, a.length - 1, type);
	}

	/**
	 * subArray includes the end'th element, so end should be less than a.length
	 * 
	 * @deprecated
	 */
	public static Object[] subArray(Object[] a, int start, int end, Class type) {
		assert start <= end + 1 : start + " " + end;
		assert end < a.length : end + " " + a.length;
		if (start == 0 && end == a.length - 1)
			return a;
		else {
			Object[] result = (Object[]) java.lang.reflect.Array.newInstance(
					type, end - start + 1);
			System.arraycopy(a, start, result, 0, end - start + 1);
			return result;
		}
	}

	public static AffineTransform scaleNtranslate(double scaleX, double scaleY,
			double x, double y) {
		AffineTransform result = AffineTransform.getTranslateInstance(x, y);
		result.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
		return result;
	}

	private static int member(int[] a, int elt) {
		if (a != null)
			for (int i = 0; i < a.length; i++) {
				if (a[i] == elt)
					return i;
			}
		return -1;
	}

	private static int member(char[] a, char elt) {
		if (a != null)
			for (int i = 0; i < a.length; i++) {
				if (a[i] == elt)
					return i;
			}
		return -1;
	}

	public static boolean isMember(int[] a, int elt) {
		return member(a, elt) >= 0;
	}

	public static boolean isMember(char[] a, char elt) {
		return member(a, elt) >= 0;
	}

	public static boolean isMember(Object[] a, Object elt) {
		return member(a, elt) >= 0;
	}

	public static int member(Object[] a, Object elt) {
		return member(a, elt, 0);
	}

	public static int member(Object[] a, Object elt, int start) {
		if (a != null) {
			for (int i = start; i < a.length; i++) {
				if (equalsNullOK(a[i], elt))
					return i;
			}
		}
		return -1;
	}

	/**
	 * @param arg1
	 * @param arg2
	 * @return Whether both args are null, or the args are equal
	 */
	public static boolean equalsNullOK(Object arg1, Object arg2) {
		return arg1 == null ? arg2 == null : arg1.equals(arg2);
	}

	/**
	 * Opposite of split; concatenates STRINGLIST using DELIMITER as the
	 * separator. The separator is only added between strings, so there will be
	 * no separator at the beginning or end.
	 * <p>
	 * 
	 * @param stringList
	 *            The list of strings that will to be put together
	 * @param delimiter
	 *            The string to put between the strings of stringList
	 * @return string that has DELIMITER put between each of the elements of
	 *         stringList
	 */
	public static String join(Object[] stringList, String delimiter) {
		String result = null;
		int len = stringList == null ? 0 : stringList.length;
		if (len > 0) {
			StringBuffer buf = new StringBuffer(len * 20);
			synchronized (stringList) {
				for (int i = 0; i < len - 1; i++) {
					buf.append(stringList[i]);
					buf.append(delimiter);
				}
				if (len > 0)
					buf.append(stringList[len - 1]);
			}
			result = buf.toString();
		}
		return result;
	}

	public static String join(int[] stringList, String delimiter) {
		String result = null;
		int len = stringList == null ? 0 : stringList.length;
		if (len > 0) {
			StringBuffer buf = new StringBuffer(len * 20);
			synchronized (stringList) {
				for (int i = 0; i < len - 1; i++) {
					buf.append(stringList[i]);
					buf.append(delimiter);
				}
				if (len > 0)
					buf.append(stringList[len - 1]);
			}
			result = buf.toString();
		}
		return result;
	}

	public static String join(float[] stringList, String delimiter) {
		String result = null;
		int len = stringList.length;
		if (len > 0) {
			StringBuffer buf = new StringBuffer(len * 20);
			synchronized (stringList) {
				for (int i = 0; i < len - 1; i++) {
					buf.append(stringList[i]);
					buf.append(delimiter);
				}
				if (len > 0)
					buf.append(stringList[len - 1]);
			}
			result = buf.toString();
		}
		return result;
	}

	public static String join(int[] s) {
		return join(s, ", ");
	}

	public static String join(float[] s) {
		return join(s, ", ");
	}

	public static String join(Object[] s) {
		return join(s, ", ");
	}

	// public final static DecimalFormat twoPlaces = new DecimalFormat("0.00");

	public static Color getHSBColor(float h, float s, float b, float alpha) {
		Color c = Color.getHSBColor(h, s, b);
		return new Color(c.getRed(), c.getGreen(), c.getBlue(),
				(int) (alpha * 255 + 0.5));
	}

	public static void printDeep(Object a) {
		System.out.println(valueOfDeep(a));
	}

	public static String valueOfDeep(Object a) {
		return valueOfDeep(a, ", ");
	}

	public static String valueOfDeep(Object a, String separator) {
		StringBuffer buf = new StringBuffer();
		valueOfDeepInternal(a, buf, separator);
		return buf.toString();
	}

	static void valueOfDeepInternal(Object a, StringBuffer buf, String separator) {
		if (a == null)
			buf.append("<null>");
		else if (isArray(a)) {
			buf.append("[");
			if (a instanceof Object[]) {
				Object[] ar = (Object[]) a;
				for (int i = 0; i < ar.length; i++) {
					valueOfDeepInternal(ar[i], buf, separator);
					if (i < (ar.length - 1))
						buf.append(separator);
				}
			} else if (a instanceof float[]) {
				float[] ar = (float[]) a;
				for (int i = 0; i < ar.length; i++) {
					buf.append(ar[i]);
					if (i < (ar.length - 1))
						buf.append(separator);
				}
			} else if (a instanceof double[]) {
				double[] ar = (double[]) a;
				for (int i = 0; i < ar.length; i++) {
					buf.append(ar[i]);
					if (i < (ar.length - 1))
						buf.append(separator);
				}
			} else if (a instanceof char[]) {
				char[] ar = (char[]) a;
				for (int i = 0; i < ar.length; i++) {
					buf.append(ar[i]);
					if (i < (ar.length - 1))
						buf.append(separator);
				}
			} else if (a instanceof int[]) {
				int[] ar = (int[]) a;
				for (int i = 0; i < ar.length; i++) {
					buf.append(ar[i]);
					if (i < (ar.length - 1))
						buf.append(separator);
				}
			} else {
				System.err.println("Can't find match for " + a.getClass());
				buf.append(a);
			}
			buf.append("]");
		} else if (a instanceof Collection) {
			buf.append("<");
			for (Iterator iterator = ((Collection) a).iterator(); iterator
					.hasNext();) {
				valueOfDeepInternal(iterator.next(), buf, separator);
				if (iterator.hasNext())
					buf.append(separator);
			}
			buf.append(">");
		} else {
			buf.append(a);
		}
	}

	private static boolean isArray(Object target) {
		Class targetClass = target.getClass();
		return targetClass.isArray();
	}

	/**
	 * Save a few keystrokes.
	 */
	public static void print(Object o) {
		// printStackTrace();
		if (o == null)
			System.out.println("<null>");
		else
			System.out.println(o.toString());
	}

	public static void print(int o) {
		System.out.println(Integer.toString(o));
	}

	public static void print(double o) {
		System.out.println(Double.toString(o));
	}

	public static void print(float o) {
		System.out.println(Float.toString(o));
	}

	public static void print(byte o) {
		System.out.println(Byte.toString(o));
	}

	public static void print(short o) {
		System.out.println(Short.toString(o));
	}

	public static void print(long o) {
		System.out.println(Long.toString(o));
	}

	public static void print(char o) {
		System.out.println(Character.toString(o));
	}

	public static void print(boolean o) {
		System.out.println(Boolean.toString(o));
	}

	public static void printStackTrace() {
		(new RuntimeException(
				"Relax.  There's no error, we're just printing the stack trace"))
				.printStackTrace();
	}

	public static void err(Object o) {
		if (o == null)
			System.err.println("<null>");
		else
			System.err.println(o.toString());
	}

	// The Eclipse debugger isn't showing me a stack trace for assert
	// violations.
	// Just add -enableassertions java vm option
	// public static void assertTrue(boolean prop, String s) {
	// if (!prop) {
	// System.out.println(s);
	// Error e = new Error();
	// e.printStackTrace();
	// throw e;
	// }
	// }

	public static String fetch(URL a_url) throws IOException {
		BufferedReader dis = null;
		try {
			URLConnection uc = a_url.openConnection();
			StringBuffer sb = new StringBuffer();
			dis = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			while (true) {
				String s = dis.readLine();
				if (s == null)
					break;
				sb.append(s + "\n");
			}
			return sb.toString();
		} catch (IOException e) {
			System.out.println("Unable to fetch " + a_url + "(IOexception)");
			return "no document";
		} finally {
			if (dis != null)
				dis.close();
		}
	}

	public static String describeImage(RenderedImage image) {
		StringBuffer buf = new StringBuffer();
		buf.append("\n" + image.getWidth() + "x" + image.getHeight()
				+ " pixels.\n");
		ColorModel cm = image.getColorModel();
		int nComponents = cm.getNumComponents();
		buf.append(cm.getNumColorComponents() + " color components; "
				+ nComponents + " total components [");
		buf.append(join(cm.getComponentSize()));
		ColorSpace cs = cm.getColorSpace();
		String s;
		switch (cs.getType()) {
		case ColorSpace.TYPE_RGB:
			s = "TYPE_RGB";
			break;
		case ColorSpace.TYPE_GRAY:
			s = "TYPE_GRAY";
			break;
		case ColorSpace.TYPE_HSV:
			s = "TYPE_HSV";
			break;
		default:
			s = "Unknown type";
		}
		buf.append("] has alpha: " + cm.hasAlpha() + "; ColorSpace type: " + s
				+ "; pixelSize: " + cm.getPixelSize());
		return buf.toString();
	}

	static {
		// Unless images are large, it's better not to cache in files
		ImageIO.setUseCache(false);
	}

	// Converts image to be compatible with monitor
	public static BufferedImage toCompatibleImage(BufferedImage image,
			int transparency) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage result = graphicsConfiguration.createCompatibleImage(w,
				h, transparency);
		return copy(result, image);
	}

	// Converts image to be compatible with monitor
	public static BufferedImage toCompatibleImage(BufferedImage image) {
		return toCompatibleImage(image, getTransparency(image));
	}

	// result will use the given color model
	public static BufferedImage convertImage(BufferedImage image,
			ColorModel colorModel) {
		int w = image.getWidth();
		int h = image.getHeight();
		WritableRaster raster = colorModel.createCompatibleWritableRaster(w, h);
		BufferedImage result = new BufferedImage(colorModel, raster, false,
				null);
		return copy(result, image);
	}

	public static int getTransparency(BufferedImage image) {
		return image.getColorModel().getTransparency();
	}

	// assumes src is same size as tgt
	public static BufferedImage copy(BufferedImage tgt, BufferedImage src) {
		Graphics2D g2 = tgt.createGraphics();
		g2.drawImage(src, 0, 0, null);
		g2.dispose();
		return tgt;
	}

	// Tested buffering. Reads 1000bytes/call for jpeg (200 for png, 100 for
	// gif)
	public static BufferedImage read(InputStream in) throws IOException {
		BufferedImage image1 = ImageIO.read(in);
		if (image1 == null)
			throw new IOException("ImageIO.read fails");
		BufferedImage image2 = null;
		try {
			image2 = toCompatibleImage(image1);
			return image2;
		} finally {
			if (image1 != image2)
				image1.flush();
		}
	}

	public static BufferedImage read(URL url) throws IOException {
		if (url == null)
			throw new IOException("null url");
		InputStream in = null;
		try {
			in = url.openStream();
			if (in == null)
				throw new IOException("Can't open connection to "
						+ url.toExternalForm());
			return read(new BufferedInputStream(in));
		} finally {
			if (in != null)
				in.close();
		}
	}

	public static BufferedImage resize(Image image, int w, int h,
			boolean alwaysCopy) {
		int originalW = image.getWidth(null);
		int originalH = image.getHeight(null);
		w = min(w, originalW);
		h = min(h, originalH);
		if (alwaysCopy || w != originalW || h != originalH
				|| !(image instanceof BufferedImage)) {
			BufferedImage resized = createCompatibleImage(w, h);
			Graphics2D g = (Graphics2D) resized.getGraphics();
			// g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			// RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			// RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			boolean finished = g.drawImage(image, 0, 0, w, h, null);
			g.dispose();
			image.flush();
			assert finished;
			assert resized.getWidth() == w : resized.getWidth() + " " + w;
			assert resized.getHeight() == h : resized.getHeight() + " " + h;
			image = resized;
		}
		return (BufferedImage) image;
	}

	public static BufferedImage rotate(BufferedImage image, double theta) {
		boolean swap = Math.round(2.0 * theta / Math.PI) % 2 == 1;
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		double delta = swap ? (w - h) / 2.0 : 0.0;
		AffineTransform at = AffineTransform
				.getTranslateInstance(-delta, delta);
		at.rotate(theta, w / 2.0, h / 2.0);
		BufferedImage rotated = swap ? createCompatibleImage(h, w)
				: createCompatibleImage(w, h);
		Graphics2D g = (Graphics2D) rotated.getGraphics();
		// g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		// RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		// RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, at, null);
		g.dispose();
		return rotated;
	}

	public static BufferedImage readCompatibleImage(InputStream blobStream) {
		BufferedImage result = null;
		try {
			ImageInputStream iis = ImageIO.createImageInputStream(blobStream);
			ImageReader reader = (ImageReader) ImageIO.getImageReaders(iis)
					.next();
			reader.setInput(iis);
			int w = reader.getWidth(0);
			int h = reader.getHeight(0);
			ImageTypeSpecifier type = reader.getRawImageType(0);
			if (type == null || type.getNumBands() == 3) {
				// print(reader.getFormatName());
				ImageReadParam param = reader.getDefaultReadParam();
				result = createCompatibleImage(w, h);
				param.setDestination(result);
				result = reader.read(0, param);
			} else
				result = resize(reader.read(0), w, h, true);
			iis.close();
			blobStream.close();
			// print(describeImage(result));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void writeImage(BufferedImage thumbImage, int quality,
			String outFile) throws ImageFormatException, IOException {
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(outFile));
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
		quality = Math.max(0, Math.min(quality, 100));
		param.setQuality(quality / 100.0f, false);
		encoder.setJPEGEncodeParam(param);
		encoder.encode(thumbImage);
		out.close();
	}

	private static final GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration();

	public static BufferedImage createCompatibleImage(int w, int h) {
		return graphicsConfiguration.createCompatibleImage(w, h,
				Transparency.OPAQUE);
	}

	public static BufferedImage createCompatibleAlphaImage(int w, int h) {
		return graphicsConfiguration.createCompatibleImage(w, h,
				Transparency.BITMASK);
	}

	public static BufferedReader getReader(String filename) {
		return getReader(new File(filename));
	}

	public static BufferedReader getReader(File file) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
		} catch (FileNotFoundException e) {
			System.err.println("Can't find file " + file);
		}
		return in;
	}

	public static BufferedWriter getWriter(String filename) {
		return getWriter(new File(filename));
	}

	public static BufferedWriter getWriter(File file) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			System.err.println("Can't find file " + file);
			e.printStackTrace();
		}
		return out;
	}

	public static OutputStream getOutputStream(File file) {
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Can't find file " + file);
			e.printStackTrace();
		}
		return out;
	}
	
	public static String readFile(String filename) {
		return readFile(new File(filename));
	}

	public static String readFile(File f) {
		return ReaderToString(getReader(f));
	}

	public static String readURL(String URL) throws IOException {
		URL url = new URL(URL);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		return ReaderToString(in);
	}

	static String ReaderToString(BufferedReader in) {
		String result = null;
		if (in != null) {
			StringBuffer buf = new StringBuffer();
			String line;
			try {
				while ((line = in.readLine()) != null) {
					if (buf.length() > 0)
						buf.append("\n");
					buf.append(line);
				}
				in.close();
				result = buf.toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static boolean writeFile(File f, String s) {
		BufferedWriter out = getWriter(f);
		if (out != null) {
			try {
				out.write(s);
				out.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static String commonPrefix(String s1, String s2,
			boolean caseSensitive) {
		String prefix = s1;
		for (int i = 0; i < prefix.length(); i++) {
			if (s2.length() >= i) {
				char char1 = prefix.charAt(i);
				char char2 = s2.charAt(i);
				if (!caseSensitive) {
					char1 = Character.toUpperCase(char1);
					char2 = Character.toUpperCase(char2);
				}
				if (char1 == char2)
					continue;
			}
			prefix = prefix.substring(0, i);
			break;
		}
		return prefix;
	}

	public static Object[] reverse(Object[] b) {
		for (int left = 0, right = b.length - 1; left < right; left++, right--) {
			// exchange the first and last
			Object temp = b[left];
			b[left] = b[right];
			b[right] = temp;
		}
		return b;
	}

	public static Object max(Object[] a, Comparator descendentOnCountComparator) {
		if (a == null || a.length == 0)
			return null;
		Object result = a[0];
		for (int i = 1; i < a.length; i++)
			if (descendentOnCountComparator.compare(result, a[i]) > 0)
				result = a[i];
		return result;
	}

	public static boolean isControlDown(int modifiers) {
		return (modifiers & InputEvent.CTRL_DOWN_MASK) != 0;
	}

	public static boolean isShiftDown(int modifiers) {
		return (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
	}

	public static boolean isAltDown(int modifiers) {
		return (modifiers & InputEvent.ALT_DOWN_MASK) != 0;
	}

	public static Iterator arrayIterator(Object[] array, int start,
			int nElements) {
		return new ArrayIterator(array, start, nElements);
	}

	private static class ArrayIterator implements Iterator {

		private final Object[] array;
		private int index;
		private final int lastIndexPlusOne;

		ArrayIterator(Object[] _array, int start, int nElements) {
			array = _array;
			index = start;
			lastIndexPlusOne = start + nElements;
		}

		public boolean hasNext() {
			return index < lastIndexPlusOne;
		}

		public Object next() {
			return array[index++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}
}