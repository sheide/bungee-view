package JSci.maths.statistics;

import edu.cmu.cs.bungee.javaExtensions.*;

public class ChiSqr {

	static final IntHashtable dists = new IntHashtable();

	static final IntHashtable thresholds = new IntHashtable();

	static ChiSqrDistribution getDist(int degFreedom) {
		ChiSqrDistribution result = (ChiSqrDistribution) dists.get(degFreedom);
		if (result == null) {
			result = new ChiSqrDistribution(degFreedom);
			dists.put(degFreedom, result);
		}
		return result;
	}

	public static boolean significant(int[][] table, double p) {
		assert p < 0.5;
		boolean result;
		double chiSqr = chiSqr(table);
		int nRows = table.length;
		int nCols = table[0].length;
		int degFreedom = (nRows - 1) * (nCols - 1);
		if (chiSqr < 0) {
			assert degFreedom == 1;
			double fisher = FisherExact(table);
//			if (fisher < 0.5)
//				fisher = 1.0 - fisher;
			fisher *= 2.0; // We want the 2-sided p value
			result = fisher < p;
		} else {
			int key = (int) (10000 * (degFreedom + p));
			Double threshold = (Double) thresholds.get(key);
			if (threshold == null) {
				ChiSqrDistribution dist = getDist(degFreedom);
				threshold = new Double(dist.inverse(1.0 - 2.0 * p));
				thresholds.put(key, threshold);
//				Util.print("chiSq threshold " + degFreedom + " " + p + " => "
//						+ threshold);
			}
			result = chiSqr >= threshold.doubleValue();
		}
		// if (result)
		// pValue(table);
		return result;
	}

	public static double pValue(int[][] table) {
		// int[][] foo = {{6,17,13,9,5},{13,5,7,16,9}};
		// table = foo;
		double chiSqr = chiSqr(table);
		assert chiSqr != Double.POSITIVE_INFINITY;
		// if (chiSqr == Double.POSITIVE_INFINITY)
		// return 0;
		int nRows = table.length;
		int nCols = table[0].length;
		int degFreedom = (nRows - 1) * (nCols - 1);
		double p;
		if (chiSqr < 0) {
//			chiSqr = -chiSqr;
			assert degFreedom == 1;
			p = FisherExact(table);
//			Util.print("Fisher raw=" + p);
//			if (p > 0.5)
//				p = 1.0 - p;
			p *= 2.0; // We want the 2-sided p value
			// chiSqr = -chiSqr - 1;
			// double cum = chiSqr == Double.POSITIVE_INFINITY ? 0
			// : 1.0 - getDist(degFreedom).cumulative(chiSqr);
//			Util.print("Fisher=" + p);
		} else {
			ChiSqrDistribution dist = getDist(degFreedom);
			p = 1.0 - dist.cumulative(chiSqr);
//			Util.print("chiSqr (" + (chiSqr) + ") => "
//					+ getDist(degFreedom).cumulative(chiSqr));
		}
//		Util.print(Util.valueOfDeep(table) + " " + p);
		assert p >= 0 && p <= 1 : p;
		return p;
	}

	static double chiSqr(int[][] table) {
		int nRows = table.length;
		int nCols = table[0].length;
		int[] rowSums = new int[nRows];
		int[] colSums = new int[nCols];
		double grandSum = 0;
		for (int row = 0; row < nRows; row++) {
//			int rowSum = 0;
			for (int col = 0; col < nCols; col++) {
				int n = table[row][col];
				assert n >= 0 : Util.valueOfDeep(table);
				rowSums[row] += n;
				colSums[col] += n;
			}
			grandSum += rowSums[row];
		}
//		for (int col = 0; col < nCols; col++) {
//			int colSum = 0;
//			for (int row = 0; row < nRows; row++) {
//				colSum += table[row][col];
//			}
//			colSums[col] = colSum;
//		}
		// Util.print(grandSum + " " + Util.valueOfDeep(colSums));
		// Util.print(Util.valueOfDeep(rowSums));
		// double minExpected = grandSum;
		double grandDiff = 0;
		for (int row = 0; row < nRows; row++) {
			int rowSum = rowSums[row];
			for (int col = 0; col < nCols; col++) {
				double expected = rowSum * colSums[col] / grandSum;
				if (expected < 6.0)
					return -1.0;
				// 0.5 is Yates' correction for continuity
				double diff = Math.max(0.0,
						Math.abs(expected - table[row][col]) - 0.5);
				// Util.print(table[row][col] + " " + expected + " " + (diff *
				// diff / expected));
				grandDiff += diff * diff / expected;
			}
		}
		// Util.print(Math.round(grandDiff));
		// if (minExpected < 6)
		// grandDiff = -1 - grandDiff; // not good approximation
		return grandDiff;
	}

	static double FisherExact(int[][] table) {
		FisherExactTest test = new FisherExactTest(table[0][0], table[0][1],
				table[1][0], table[1][1]);
		return test.getTwoTail().doubleValue() / 2;
	}

	// a b a+b
	// c d c+d
	// a+c b+d n
	//
	// a+b c+d n
	// p = ( a ) ( c ) / (a+c)
	//
	// = (a+b)!(c+d)!(a+c)!(b+d)!
	// ------------------------
	// n!a!b!c!d!
	//
	// = (a+b) * ... * (b+1) (c+d) * ... * (d+1) (a+c) * ... * (c+1)
	// ------------------- * ------------------- * -------------------
	// a * ... * 1 1 n * ... * (b+d+1)
	//
	// assert a < b, c, d
	//
	// static double FisherExact(int[][] table) {
	// // Util.printDeep(table);
	// int a = Math.min(Math.min(table[0][0], table[0][1]), Math.min(
	// table[1][0], table[1][1]));
	// int b, c, d;
	// if (a == table[0][0]) {
	// b = table[0][1];
	// c = table[1][0];
	// d = table[1][1];
	// } else if (a == table[0][1]) {
	// b = table[0][0];
	// d = table[1][0];
	// c = table[1][1];
	// } else if (a == table[1][0]) {
	// b = table[1][1];
	// c = table[1][0];
	// d = table[0][1];
	// } else {
	// b = table[0][1];
	// c = table[1][0];
	// d = table[0][0];
	// }
	//
	// double p = 0.0;
	// for (int i = 0; i <= a; i++) {
	// int delta = a - i;
	// p += FisherExactInternal(i, b + delta, c + delta, d - delta);
	// }
	// return p;
	// }
	//
	// static double FisherExactInternal(int a, int b, int c, int d) {
	// assert a>=0 && b>=0 && c>=0 && d>=0;
	// int n = a + b + c + d;
	// double p = 1;
	// for (int i = a + b; i > b; i--)
	// p *= i;
	// for (int i = a; i > 1; i--)
	// p /= i;
	// for (int i = a + c; i > c; i--)
	// p *= i;
	// if (c > b + 1) {
	// for (int i = n; i > c + d; i--)
	// p /= i;
	// for (int i = b + d; i > d; i--)
	// p *= i;
	// } else {
	// for (int i = n; i > b + d; i--)
	// p /= i;
	// for (int i = c + d; i > d; i--)
	// p *= i;
	// }
	// assert p >= 0 && p <= 1 : p + " " + a + " " + b + " " + c + " " + d;
	// return p;
	// }
}
