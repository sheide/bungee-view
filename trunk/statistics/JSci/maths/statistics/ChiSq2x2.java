package JSci.maths.statistics;

import edu.cmu.cs.bungee.javaExtensions.IntHashtable;

public class ChiSq2x2 {

	boolean isPValid;

	/**
	 * True if table00 > expected00
	 */
	boolean isPositive;

	double chiSq;

	double p;

	public void setChiSq2x2(int total, int row0, int col0, int table00)
			throws OutOfRangeException {
		setChiSq2x2(total, row0, col0, table00, 6);
	}

	public void setChiSq2x2(int total, int row0, int col0, int table00,
			int fisherThreshold) throws OutOfRangeException {
		assert total > 0;
		assert row0 > 0;
		assert col0 > 0;
		assert table00 >= 0;
		if (row0 == 0 || col0 == 0 || row0 == total || col0 == total) {
			chiSq = 0.0;
			p = 1.0;
			isPValid = true;
		} else {
			isPositive = row0 * col0 < table00 * total;
			int row1 = total - row0;
			int col1 = total - col0;
			if (Math.min(row0, row1) * Math.min(col0, col1) < fisherThreshold
					* total) {
				assert row1 > 0 : total + " " + row0 + " " + col0 + " " + table00;
				assert col1 > 0 : total + " " + row0 + " " + col0 + " " + table00;
				int table01 = row0 - table00;
				int table10 = col0 - table00;
				int table11 = row1 - table10;
				assert row0 >= table00 : total + " " + row0 + " " + col0 + " " + table00;
				assert col0 >= table00 : total + " " + row0 + " " + col0 + " " + table00;
				assert row1 >= table10 : total + " " + row0 + " " + col0 + " " + table00;
				chiSq = -1.0;
				try {
					FisherExactTest test = new FisherExactTest(table00,
							table01, table10, table11);
					p = test.getTwoTailPrimitive();
					isPValid = true;
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
				assert expected00 >= 0.0;
				assert expected10 >= 0.0;
				assert expected01 >= 0.0;
				assert expected11 >= 0.0;
				chiSq = 0.0;
				double diff = Math.abs(expected00 - dTable00 * dTotal) - 0.5;
				// Don't try integer math, because you'll get overflows
				if (diff > 0)
					chiSq = diff * diff / expected00;
				
				diff = Math.abs(expected01 - table01 * dTotal) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected01;
				
				diff = Math.abs(expected10 - table10 * dTotal) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected10;
				
				diff = Math.abs(expected11 - table11 * dTotal) - 0.5;
				if (diff > 0)
					chiSq += diff * diff / expected11;
				
				assert chiSq >= 0 : chiSq;
				isPValid = false;
			}
		}
	}

	static final IntHashtable thresholds = new IntHashtable();

	static final ChiSqrDistribution dist = new ChiSqrDistribution(1);

	public int significant(double pValue) {
		assert pValue < 0.5 : pValue;
		if (chiSq < 0) {
			if (p > pValue) {
				return 0;
			}
		} else {
			int key = (int) (10000 * pValue);
			Double threshold = (Double) thresholds.get(key);
			if (threshold == null) {
				threshold = new Double(dist.inverse(1.0 - 2.0 * pValue));
				thresholds.put(key, threshold);
				// Util.print("chiSq threshold " + degFreedom + " " + p + " => "
				// + threshold);
			}
			if (chiSq < threshold.doubleValue())
				return 0;
		}
		if (isPositive)
			return 1;
		else
			return -1;
	}

	public double pValue() {
		if (!isPValid) {
			assert chiSq >= 0;
			p = 1.0 - dist.cumulative(chiSq);
			isPValid = true;
		}
		assert p >= 0 && p <= 1 : p;
		return p;
	}

	public double chiSqValue() {
		assert chiSq >= 0;
		return chiSq;
	}

	public String toString() {
		return "chiSq=" + chiSq + "; p=" + p;
	}

	public void reset() {
		chiSq = -1.0;
		p = 1.0;
		isPValid = true;		
	}

}
