package JSci.maths.statistics;

/**
 * Just contains one static method for computing a Chi Squared p-value for a 2 x
 * 2 table.
 * 
 * @author mad
 * 
 */
public class ChiSq2x2 {

	static final int FISHER_THRESHOLD = 6;

	static final ChiSqrDistribution dist = new ChiSqrDistribution(1);

	private ChiSq2x2() {
		// Prevent instantiation
	}

	/**
	 * @param total
	 *            sum of all 4 table entries
	 * @param row0
	 *            sum of first row
	 * @param col0
	 *            sum of first column
	 * @param table00
	 *            first entry
	 * @return the p-value, except that if table00 is less than the expected
	 *         value, the arithmetic inverse of the p-value is returned.
	 * @throws OutOfRangeException
	 */
	public static double pValue(int total, int row0, int col0, int table00)
			throws OutOfRangeException {
		assert total > 0;
		assert row0 > 0;
		assert col0 > 0;
		assert table00 >= 0;
		double p = 1.0;
		if (!(row0 == 0 || col0 == 0 || row0 == total || col0 == total)) {
			int row1 = total - row0;
			int col1 = total - col0;
			if (Math.min(row0, row1) * Math.min(col0, col1) < FISHER_THRESHOLD
					* total) {
				assert row1 > 0 : total + " " + row0 + " " + col0 + " "
						+ table00;
				assert col1 > 0 : total + " " + row0 + " " + col0 + " "
						+ table00;
				int table01 = row0 - table00;
				int table10 = col0 - table00;
				int table11 = row1 - table10;
				assert row0 >= table00 : total + " " + row0 + " " + col0 + " "
						+ table00;
				assert col0 >= table00 : total + " " + row0 + " " + col0 + " "
						+ table00;
				assert row1 >= table10 : total + " " + row0 + " " + col0 + " "
						+ table00;
				try {
					FisherExactTest test = new FisherExactTest(table00,
							table01, table10, table11);
					p = test.getTwoTailPrimitive();
				} catch (Exception e) {
					throw new OutOfRangeException("Bad table: " + table00 + " "
							+ table01 + " " + table10 + " " + table11 + "\n"
							+ total + " " + row0 + " " + col0 + " " + table00);
				}
			} else {
				double dTotal = total;
				double dRow0 = row0;
				double dCol0 = col0;
				double dTable00 = table00;
				double dRow1 = dTotal - dRow0;
				double dCol1 = dTotal - dCol0;
				double table01 = dRow0 - dTable00;
				double table10 = dCol0 - dTable00;
				double table11 = dRow1 - table10;
				double expected00 = dRow0 * dCol0 / dTotal;
				double expected01 = dRow0 * dCol1 / dTotal;
				double expected10 = dRow1 * dCol0 / dTotal;
				double expected11 = dRow1 * dCol1 / dTotal;

				assert expected00 >= 0.0 && expected00 <= dRow0
						&& expected00 <= dCol0;
				assert expected10 >= 0.0 && expected10 <= dRow1
						&& expected10 <= dCol0;
				assert expected01 >= 0.0 && expected01 <= dRow0
						&& expected01 <= dCol1;
				assert expected11 >= 0.0 && expected11 <= dRow1
						&& expected11 <= dCol1;
				assert dRow0 <= dTotal;
				assert dRow1 <= dTotal;
				assert dCol0 <= dTotal;
				assert dCol1 <= dTotal;

				double chiSq = 0.0;
				double diff = Math.abs(expected00 - dTable00) - 0.5;
				// Don't try integer math, because you'll get overflows
				if (diff > 0)
					chiSq = diff * diff / expected00;

				diff = Math.abs(expected01 - table01) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected01;

				diff = Math.abs(expected10 - table10) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected10;

				diff = Math.abs(expected11 - table11) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected11;

				// This was an attempt to color only bars that are much higher
				// or lower than the mean.
				// It doesn't seem to do what I want.
				// if (dTotal > PRACTICAL_SIGNIFICANCE_TOTAL) {
				// chiSq *= PRACTICAL_SIGNIFICANCE_TOTAL / dTotal;
				// }

				assert chiSq >= 0 : chiSq;
				p = 1.0 - dist.cumulative(chiSq);
			}
		}
		return p;
	}
	
	public static class SignedPValue {
		public double magnitude = 1.0;
		public boolean sign = true;
		
		public String toString() {
			return "<SignedPValue " + (sign ? "+":"-") + " " + magnitude + ">";
		}
	}

//	public static double[] signedPvalue(int total, int row0, int col0, int table00) {
//		double[] result = {pValue(total, row0, col0, table00), row0 * col0 < table00 * total ? 1 : -1};
//		return result;
//	}

	public static SignedPValue signedPvalue(int total, int row0, int col0, int table00) {
		SignedPValue result = new SignedPValue();
		result.magnitude = pValue(total, row0, col0, table00);
		result.sign = row0 * (long) col0 < table00 * (long) total;
		return result;
	}
}
