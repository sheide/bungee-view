package JSci.maths.statistics;

import java.text.DecimalFormat;

import edu.cmu.cs.bungee.javaExtensions.StringAlign;

//facet ~facet
//_ a ____ b __ query
//_ c ____ d _ ~query
/**
 * A bunch of statistics that can be computed on a 2 x 2 table. Remembers that
 * object that generated the table, for use by TopTags.
 * 
 * @author mad
 * 
 */
public class ChiSq2x2 {

	static final int FISHER_THRESHOLD = 6;

	static final ChiSqrDistribution dist = new ChiSqrDistribution(1);

	public final Object object;
	private Object printObject;

	// total=0 marks this as having no useful information
	private final int total;
	private final int row0;
	private final int col0;
	private final int table00;
	private double pvalue = -2;
	private int pvalueSign = -2;
	private double chiSq = -2;
	private double percentageRatio = -2;
	private double oddsRatio = -2;

	public int total() {
		return total;
	}

	public int col0() {
		return col0;
	}

	public int row0() {
		return row0;
	}

	public int col1() {
		return total - col0;
	}

	public int row1() {
		return total - row0;
	}

	public int table00() {
		return table00;
	}

	public int table01() {
		return row0 - table00;
	}

	public int table10() {
		return col0 - table00;
	}

	public int table11() {
		return row1() - table10();
	}

	public String toString() {
		return "<ChiSq2x2 " + (pvalueSign > 0 ? "+" : "-") + " " + pvalue + ">";
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
	 * 
	 * @throws OutOfRangeException
	 */
	public static ChiSq2x2 getInstance(Object facet2, int total, int row0,
			int col0, int table00) {
		assert total >= 0;
		assert row0 >= 0;
		assert col0 >= 0;
		assert table00 >= 0;
		assert row0 >= table00 : total + " " + row0 + " " + col0 + " "
				+ table00;
		assert col0 >= table00 : total + " " + row0 + " " + col0 + " "
				+ table00;
		assert row0 <= total : total + " " + row0 + " " + col0 + " " + table00;
		assert col0 <= total : total + " " + row0 + " " + col0 + " " + table00;
		return new ChiSq2x2(facet2, total, row0, col0, table00);
	}

	public static ChiSq2x2 getInstance(Object facet2) {
		return new ChiSq2x2(facet2, 0, 0, 0, 0);
	}

	private ChiSq2x2(Object facet2, int total2, int row02, int col02,
			int table002) {
		if (row02 == 0 || col02 == 0 || row02 == total2 || col02 == total2) {
			pvalue = 1;
			pvalueSign = 1;
			percentageRatio = 1;
			chiSq = 0;
		}
		object = facet2;
		total = total2;
		row0 = row02;
		col0 = col02;
		table00 = table002;
	}

	/**
	 * @return the p-value in [0, POSITIVE_INFINITY]
	 */
	public double pvalue() {
		if (pvalue == -2) {
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
				assert row1 >= table10 : total + " " + row0 + " " + col0 + " "
						+ table00;
				try {
					FisherExactTest test = new FisherExactTest(table00,
							table01, table10, table11);
					pvalue = test.getTwoTailPrimitive();
				} catch (Exception e) {
					throw new OutOfRangeException("Bad table: " + table00 + " "
							+ table01 + " " + table10 + " " + table11 + "\n"
							+ total + " " + row0 + " " + col0 + " " + table00);
				}
			} else {
				pvalue = 1.0 - dist.cumulative(chiSq());
			}
		}
		return pvalue;
	}

	public int sign() {
		if (pvalueSign == -2)
			pvalueSign = (row0 * (long) col0 < table00 * (long) total) ? 1 : -1;
		return pvalueSign;
	}

	public double chiSq() {
		cache();
		return chiSq;
	}

	/**
	 * @return hack to reduce the effect of sample size (and thus increase the
	 *         effect of effect size).
	 */
	public double myCramersPhi() {
		return chiSq() * sign() / Math.sqrt(total);
	}

	/**
	 * @return p/q, where p=a/(a+c) and q = b/(b+d), or a(b+d)/b(a+c)
	 */
	public double percentageRatio() {
		cache();
		return percentageRatio;
	}

	/**
	 * @return (p/(1-p))/(q/(1-q)), where p=a/(a+c) and q = b/(b+d), or ad/bc
	 */
	public double oddsRatio() {
		cache();
		return oddsRatio;
	}

	public double facetOnPercent() {
		return table00 / (double) col0;
	}

	public double parentOnPercent() {
		return row0 / (double) total;
	}

	public double siblingOnPercent() {
		return (row0 - table00) / (double) (total - col0);
	}

	public double correlation() {
		return sampleCovariance() / sampleVariance(ROW) / sampleVariance(COL);
	}

	/**
	 * @return Cov(X,Y) = E(X*Y) - E(X)*E(Y)
	 * 
	 *         see http://en.wikipedia.org/wiki/Covariance
	 */
	public double sampleCovariance() {
		double dTotal = total;
		return (table00 - col0 * (row0 / dTotal)) / (dTotal - 1);
	}

	public static final int ROW = 1;
	public static final int COL = 2;

	public double sampleVariance(int whichVar) {
		double nOn = whichVar == ROW ? row0() : col0();
		double nOff = whichVar == ROW ? row1() : col1();
		double dTotal = total;
		return 2 * nOn * nOff / (dTotal * (dTotal - 1));
	}

	/**
	 * @return Measure of effect size. Phi eliminates sample size by dividing
	 *         chi-square by n, the sample size, and taking the square root.
	 * 
	 *         see <a * href=
	 *         "http://www.people.vcu.edu/~pdattalo/702SuppRead/MeasAssoc/NominalAssoc.html"
	 *         >Discussion * of effect size< /a>
	 * 
	 */
	public double phi() {

		double dTotal = total;
		double dRow0 = row0;
		double dCol0 = col0;
		double dTable00 = table00;
		double dRow1 = dTotal - dRow0;
		double dCol1 = dTotal - dCol0;
		double table01 = dRow0 - dTable00;
		double table10 = dCol0 - dTable00;
		double table11 = dRow1 - table10;

		assert dRow0 <= dTotal;
		assert dRow1 <= dTotal;
		assert dCol0 <= dTotal;
		assert dCol1 <= dTotal;

		return (dTable00 * table11 - table01 * table10)
				/ Math.sqrt(dRow0 * dRow1 * dCol0 * dCol1);
	}

	private void cache() {
		if (chiSq == -2) {
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

			chiSq = 0.0;
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
			pvalueSign = (dTable00 < expected00) ? -1 : 1;

			assert !(col0 == 0 || dCol1 == 0 || row0 == 0);
			if (table01 == 0)
				percentageRatio = Double.POSITIVE_INFINITY;
			else
				// cast to double to avoid overflow
				percentageRatio = (dTable00 * dCol1) / (table01 * dCol0);
			assert percentageRatio >= 0 : printTable();

			if (table01 == 0 || table10 == 0)
				oddsRatio = Double.POSITIVE_INFINITY;
			else
				// cast to double to avoid overflow
				oddsRatio = (dTable00 * table11) / (table01 * table10);
			assert oddsRatio >= 0 : dTable00 + " " + table01 + " " + dCol1
					+ " " + col0;
		}
	}

	public String printTable() {
		int a = table00;
		int itotal = total;
		int col1 = itotal - col0;
		int row1 = itotal - row0;
		int b = row0 - a;
		int c = col0 - a;
		int d = col1 - b;
		DecimalFormat f = new DecimalFormat(" ###,##0");
		StringAlign align8right = new StringAlign(8, StringAlign.JUST_RIGHT);
		StringBuffer buf = new StringBuffer();
		buf.append("\n").append(align8right.format(a, f)).append(
				align8right.format(b, f)).append(align8right.format(row0, f));
		buf.append("\n").append(align8right.format(c, f)).append(
				align8right.format(d, f)).append(align8right.format(row1, f));
		buf.append("\n").append(align8right.format(col0, f)).append(
				align8right.format(col1, f)).append(
				align8right.format(itotal, f));
		return buf.toString();
	}

	private static final DecimalFormat doubleFormat = new DecimalFormat(
			"0.00E0");
	private static final StringAlign align = new StringAlign(13,
			StringAlign.JUST_LEFT);

	public static String statisticsHeading() {
		return align.format("myCramersPhi") + align.format("Chi square")
				+ align.format("p-value") + align.format("Facet Percent")
				+ align.format("Sibling Percent")
				+ align.format("Percentage Ratio") + align.format("Tag");
	}

	public void setPrintObject(Object o) {
		printObject = o;
	}

	public StringBuffer statisticsLine(StringBuffer buf) {
		// if (p.getNameIfPossible() == null)
		// p.getName(mouseDoc);
		if (buf == null)
			buf = new StringBuffer();
		buf.append(align.format(myCramersPhi(), doubleFormat));
		buf.append(align.format(chiSq(), doubleFormat));
		buf.append(align.format(pvalue(), doubleFormat));
		buf.append(align.format(facetOnPercent(), doubleFormat));
		buf.append(align.format(siblingOnPercent(), doubleFormat));
		buf.append(align.format(percentageRatio(), doubleFormat));
		Object o = printObject == null ? object : printObject;
		buf.append(o);
		return buf;
	}
}
