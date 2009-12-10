package edu.cmu.cs.bungee.client.query.tetrad;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.tetrad.data.ColtDataSet;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.LogisticRegression;
import edu.cmu.tetrad.search.LogisticRegressionResult;
import edu.cmu.tetrad.search.RegressionResult;
import edu.cmu.tetradapp.model.DataWrapper;
import edu.cmu.tetradapp.model.RegressionParams;
import edu.cmu.tetradapp.model.RegressionRunner;

/**
 * Encapsulates the data for the regression, providing summary statistics and
 * views as arrays or ColtDataSets. y is variable 0, followed by the x vars.
 * 
 */
class MyRegressionParams {
	// final double[][] regressors;
	final String[] regressorNames;
	// final double[] target;
	final String targetName;
	final int[] counts;
	final int[] marginals;
	private final int nRows;
	private final double[][] standardizedValues;

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<MyRegressionParams ").append(
				Util.valueOfDeep(regressorNames)).append(" => ").append(
				targetName).append("\n").append(Util.valueOfDeep(marginals))
				.append(">");
		return buf.toString();
	}

	MyRegressionParams(int[] _counts, String[] _regressorNames,
			String _targetName) {
		counts = _counts;
		regressorNames = _regressorNames;
		targetName = _targetName;

		marginals = new int[nVars()];
		int _nRows = 0;
		for (int state = 0; state < counts.length; state++) {
			assert counts[state] >= 0 : Util.valueOfDeep(counts);
			int count = Math.max(0, counts[state]);
			_nRows += count;
			for (int j = 0; j < nVars(); j++) {
				if (Util.isBit(state, j))
					marginals[j] += count;
			}
		}
		nRows = _nRows;
		standardizedValues = new double[nXs()][];
		for (int var = 0; var < standardizedValues.length; var++) {
			double n = nRows;
			double count = marginals[var + 1];
			double mean = count / n;
			double stdDev = Math.sqrt(count * (n - count) / (n * (n - 1)));
			if (stdDev == 0)
				// Not sure what to do here - probably doesn't matter
				stdDev = 1;
			// Util.print("gg " + mean + " " + stdDev);
			double[] standardized = { (0 - mean) / stdDev, (1 - mean) / stdDev };
			standardizedValues[var] = standardized;
		}
		// Util.print("\ncounts " + Util.valueOfDeep(counts));
		// Util.print("regressorNames " + Util.valueOfDeep(regressorNames));
		// Util.print("marginals " + Util.valueOfDeep(marginals));
		// Util
		// .print("standardizedValues "
		// + Util.valueOfDeep(standardizedValues));
	}

	boolean isCompatibleWith(MyRegressionParams _params) {
		int _nRows = _params.nRows();
		int _nVars = _params.nVars();
		int[] _marginals = _params.marginals;
		assert _nRows == nRows() : _nRows + " " + nRows();
		assert _nVars == nVars() : _nVars + " " + nVars();
		for (int var = 0; var < nVars(); var++) {
			assert marginals[var] == _marginals[var] : Util
					.valueOfDeep(_marginals)
					+ " "
					+ Util.valueOfDeep(marginals)
					+ " "
					+ this
					+ "\n"
					+ Util.valueOfDeep(_params.counts)
					+ " "
					+ Util.valueOfDeep(counts);
		}
		return true;
	}

	int yCount() {
		return marginals[0];
	}

	/**
	 * @return form of data for linear regression.
	 */
	ColtDataSet getContinuousData() {
		List<Node> vars = new ArrayList<Node>(nVars());
		vars.add(new ContinuousVariable(targetName));
		for (int i = 0; i < regressorNames.length; i++) {
			vars.add(new ContinuousVariable(regressorNames[i]));
		}
		ColtDataSet data1 = new ColtDataSet(nRows(), vars);
		int row = 0;
		for (int state = 0; state < counts.length; state++) {
			for (int item = 0; item < counts[state]; item++) {
				for (int var = 0; var < nXs(); var++) {
					double value = standardizedValue(var, Util.getBit(state,
							var + 1));
					data1.setDouble(row, var + 1, value);
				}
				double value = Util.getBit(state, 0);
				data1.setDouble(row, 0, value);
				row++;
			}
		}
		return data1;
	}

	ColtDataSet getDiscreteData(int maxNameLength) {
		List<Node> vars = new ArrayList<Node>(nVars());
		vars.add(new DiscreteVariable(truncateName(targetName, maxNameLength)));
		for (int i = 0; i < regressorNames.length; i++) {
			vars.add(new DiscreteVariable(truncateName(regressorNames[i],
					maxNameLength)));
		}
		ColtDataSet data1 = new ColtDataSet(nNonZeroStates(), vars);
		int row = 0;
		for (int state = 0; state < counts.length; state++) {
			if (counts[state] > 0) {
				for (int var = 0; var < nXs(); var++) {
					int value = Util.getBit(state, var + 1);
					data1.setInt(row, var + 1, value);
				}
				int value = Util.getBit(state, 0);
				data1.setInt(row, 0, value);
				data1.setMultiplier(row, counts[state]);
				row++;
			}
		}
		return data1;
	}

	private String truncateName(String name, int maxNameLength) {
		if (name.length() > maxNameLength)
			return name.substring(0, maxNameLength);
		else
			return name;
	}

	private int nNonZeroStates() {
		int result = 0;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0)
				result++;
		}
		return result;
	}

	Object[] getRegressors() {
		boolean USE_MULTIPLIERS = true;
		int nCases = USE_MULTIPLIERS ? nNonZeroStates() : nRows();
		double[][] Xs = new double[nXs()][];
		double[] Ys = new double[nCases];
		int[] mults = USE_MULTIPLIERS ? new int[nCases] : null;
		for (int var = 0; var < Xs.length; var++) {
			Xs[var] = new double[nCases];
		}
		int row = 0;
		double[] sums = new double[nCases];
		for (int state = 0; state < counts.length; state++) {
			int count = counts[state];
			if (count > 0) {
				int repeats = USE_MULTIPLIERS ? 1 : count;
				int mult = USE_MULTIPLIERS ? count : 1;
				for (int item = 0; item < repeats; item++) {
					double[] rowValues = getStandardizedRow(state);
					for (int var = 0; var < Xs.length; var++) {
						Xs[var][row] = rowValues[var];
						sums[var] += rowValues[var] * mult;
					}
					Ys[row] = Util.getBit(state, 0);
					if (mults != null)
						mults[row] = mult;
					row++;
				}
			}
		}
		// assert row == nRows() : row;
		for (int var = 0; var < Xs.length; var++) {
			assert Math.abs(sums[var]) < 0.0001 : var + " " + sums[var];
		}
		Object[] result = { mults, Ys, Xs };
		return result;
	}

	double[] getStandardizedRow(int state) {
		double[] row = new double[nXs()];
		for (int var = 0; var < nXs(); var++) {
			int value = Util.getBit(state, var + 1);
			row[var] = standardizedValue(var, value);
		}
		return row;
	}

	int[] getRow(int state) {
		int[] row = new int[nXs()];
		for (int var = 0; var < nXs(); var++) {
			int value = Util.getBit(state, var + 1);
			row[var] = value;
		}
		return row;
	}

	private double standardizedValue(int var, int value) {
		return standardizedValues[var][value];
	}

	// double[][] getTarget() {
	// double[][] result = new double[2][];
	// for (int i = 0; i < result.length; i++) {
	// result[i]=new double[nRows()];
	// }
	// int row = 0;
	// for (int state = 0; state < counts.length; state++) {
	// double value = Util.getBit(state, 0);
	// result[]
	// for (int item = 0; item < counts[state]; item++) {
	// result[row++] = value;
	// }
	// }
	// return result;
	// }

	String[] getRegressorNames() {
		return regressorNames;
	}

	String getTargetName() {
		return targetName;
	}

	int nRows() {
		return nRows;
	}

	int nVars() {
		return regressorNames.length + 1;
	}

	int nXs() {
		return regressorNames.length;
	}
}

class MyLogisticRegressionResult extends TetradRegressionResult {
	private LogisticRegressionResult logisticResult;
	private String report;

	static TetradRegressionResult getInstance(MyRegressionParams _params) {
		return new MyLogisticRegressionResult(_params);
	}

	@Override
	TetradRegressionResult getLikeInstance(MyRegressionParams _params) {
		assert params.isCompatibleWith(_params) : Util
				.valueOfDeep(_params.counts)
				+ " " + Util.valueOfDeep(params.counts);
		return getInstance(_params);
	}

	MyLogisticRegressionResult(MyRegressionParams _params) {
		params = _params;
		LogisticRegression regression = new LogisticRegression();
		Object[] data = params.getRegressors();
		regression.setRegressors((double[][]) data[2]);
		regression.setMultipliers((int[]) data[0]);
		regression.setVariableNames(params.regressorNames);
		regression.setAlpha(Tetrad.ALPHA);
		regression.maxCoef = 1000;

		report = regression.regress((double[]) data[1], params.targetName,
				printReport);
		if (printReport)
			Util.print(report);
		logisticResult = regression.getResult();

		assert checkCoefs() : this + " " + Util.valueOfDeep(params.counts)
				+ "\n" + report;
	}

	@Override
	double[] getCoefsInternal() {
		return logisticResult.getCoefs();
	}

	double yHat(int state) {
		double[] zs = params.getStandardizedRow(state);
		double[] coefs = getCoefs();
		assert coefs.length == zs.length + 1 : Util.valueOfDeep(coefs) + " "
				+ zs.length;
		double logits = coefs[0];
		for (int i = 0; i < zs.length; i++) {
			logits += coefs[i + 1] * zs[i];
		}
		double result = 1 / (1 + Math.exp(-logits));
		assert !Double.isNaN(result) : logits + " " + Util.valueOfDeep(coefs)
				+ " " + Util.valueOfDeep(zs);
		// Util.print(" "+Util.valueOfDeep(zs)+" "+logits+" "+result);
		return result;
	}

	/**
	 * Efron's Pseudo R-Squared see http://www.ats.ucla.edu/stat/mult_pkg/faq
	 * /general/Psuedo_RSquareds.htm
	 */
	@Override
	double pseudoRsquared() {
		// assert false;
		double residuals = 0;
		double sumYhat = 0;
		int[] counts = params.counts;
		for (int state = 0; state < counts.length; state++) {
			double yHat = yHat(state);
			double y = Util.getBit(state, 0);
			int count = counts[state];
			sumYhat += yHat * count;
			double residual = y - yHat;
			residuals += residual * residual * count;
			if (printReport)
				Util.print("yHat count=" + count + "\tX=" + (state / 2)
						+ "\ty=" + y + " => " + yHat + "\tresidual="
						+ (residual * residual * count));

		}
		double base = pseudoRsquaredDenom();
		// assert Math.abs(params.yCount() - sumYhat) < 1 : params.yCount() +
		// " "
		// + sumYhat + report + "\n" + yHats();
		double Rsquare = 1 - residuals / base;
		if (printReport) {
			Util.print("pseudoRsquared=" + Rsquare + " ("
					+ Math.sqrt(Math.abs(Rsquare)) + ") for "
					+ Util.valueOfDeep(params.counts) + " " + residuals + "/"
					+ base + "\n");
			Util.print(Util.valueOfDeep(getCoefs()));
		}
		assert Rsquare >= -0.01 && Rsquare <= 1.01 : "Rsquare=" + Rsquare
				+ " base=" + base + " residuals=" + residuals + " yCount="
				+ params.yCount() + " sumYhat=" + sumYhat + " nRows=" + nRows()
				+ "\n" + report + "\n" + yHats();
		return Rsquare;
	}

	private String yHats() {
		StringBuffer result = new StringBuffer();
		int[] counts = params.counts;
		for (int state = 0; state < counts.length; state++) {
			double yHat = yHat(state);
			int y = Util.getBit(state, 0);
			if (counts[state] > 0)
				result.append("\nyHat count="
						+ align.format(counts[state], countFormat) + " x="
						+ Util.valueOfDeep(params.getRow(state)) + " y=" + y
						+ " => " + yHat);
		}
		return result.toString();
	}

	private static final StringAlign align = new StringAlign(7,
			StringAlign.JUST_RIGHT);

	static final DecimalFormat countFormat = new DecimalFormat("###,###");

	private double pseudoRsquaredDenom() {
		double sumY = params.yCount();
		double yMean = sumY / nRows();
		double diff0 = yMean;
		double diff1 = 1 - yMean;
		double base = (nRows() - sumY) * diff0 * diff0 + sumY * diff1 * diff1;
		return base;
	}

}

class MyLinearRegressionResult extends TetradRegressionResult {
	private RegressionResult linearResult;

	static TetradRegressionResult getInstance(MyRegressionParams _params) {
		return new MyLinearRegressionResult(_params);
	}

	@Override
	TetradRegressionResult getLikeInstance(MyRegressionParams _params) {
		assert params.isCompatibleWith(_params) : Util
				.valueOfDeep(_params.counts)
				+ " " + Util.valueOfDeep(params.counts);
		return getInstance(_params);
	}

	MyLinearRegressionResult(MyRegressionParams _params) {
		params = _params;
		ColtDataSet data2 = params.getContinuousData();
		List<Node> regressors = new ArrayList<Node>(params.regressorNames.length);
		for (int i = 0; i < params.regressorNames.length; i++) {
			regressors.add(data2.getVariable(params.regressorNames[i]));
		}

		DataWrapper dataWrapper = new DataWrapper(data2);

		RegressionParams tetradParams = new RegressionParams();
		tetradParams.setRegressorNames(params.regressorNames);
		tetradParams.setTargetName(params.targetName);
		tetradParams.setAlpha(Tetrad.ALPHA);

		RegressionRunner runner = new RegressionRunner(dataWrapper,
				tetradParams);
		runner.execute();
		linearResult = runner.getResult();
		// String report = linearResult.toString();
	}

	@Override
	double[] getCoefsInternal() {
		return linearResult.getCoef();
	}

	@Override
	double pseudoRsquared() {
		double Rsquare = linearResult.getRSquare();
		assert Rsquare >= 0 && Rsquare <= 1 : Rsquare;
		return Rsquare;
	}

}

abstract class TetradRegressionResult {
	static final boolean printReport = false;

	MyRegressionParams params;

	abstract TetradRegressionResult getLikeInstance(MyRegressionParams maxParams);

	abstract double pseudoRsquared();

	@Override
	public String toString() {
		return "<Regression " + Util.valueOfDeep(params.regressorNames)
				+ " => " + params.targetName + " "
				+ Util.valueOfDeep(getCoefsInternal()) + ">";
	}

	int nRows() {
		return params.nRows();
	}

	int nVars() {
		return params.nVars();
	}

	double[] getCoefs() {
		assert checkCoefs() : this + " " + Util.valueOfDeep(params.counts);
		return getCoefsInternal();
	}

	boolean checkCoefs() {
		double[] coefs = getCoefsInternal();
		for (int i = 0; i < coefs.length; i++) {
			if (Double.isNaN(coefs[i]))
				return false;
		}
		return true;
	}

	abstract double[] getCoefsInternal();

	// double coefScale() {
	// double result = 1;
	// // if (isLogistic()) {
	// double goalSum;
	// if (true) {
	// double rSquared = pseudoRsquared();
	// Util.print("coefScale: Rsquared=" + rSquared + "; " + this);
	// double maxRsquared = maxRsquared();
	// Util.print("rSquared = " + rSquared + "; max = " + maxRsquared);
	// goalSum = Math.sqrt(rSquared / maxRsquared);
	// }
	// // else {
	// // String[] regressorNames = params.regressorNames;
	// // int[] xIndexes = xIndexes();
	// // int[] yIndexes = { yIndex() };
	// // double xEntropy = entropy(data, xIndexes);
	// // double yEntropy = entropy(data, yIndexes);
	// // double xyEntropy = entropy(data, Util
	// // .append(xIndexes, yIndexes));
	// // double mutInf = xEntropy + yEntropy - xyEntropy;
	// // // Util.print("coefScale " + regression.params.targetName +
	// // // " "
	// // // + Util.valueOfDeep(regressorNames) + " " + xEntropy
	// // // + " " + yEntropy + " " + mutInf);
	// // goalSum = mutInf / Math.min(xEntropy, yEntropy);
	// // }
	// double sumCoefs = 0;
	// for (int i = 0; i < params.regressorNames.length; i++) {
	// sumCoefs += Math.abs(getCoefs()[i + 1]);
	// }
	// result = goalSum / sumCoefs;
	// // }
	// return result;
	// }

	/**
	 * @return the largest pseudo R squared you can get from the regression's x
	 *         variables by redistirbuting the y variable. If all regression
	 *         coefficients have the same sign, restrict the y distribution to
	 *         reflect that sign. Approximate the maximum y distribution by
	 *         cramming it all together.
	 */
	double maxRsquared() {
		// double[] target = new double[params.target.length];
		// System.arraycopy(params.target, 0, target, 0, target.length);
		// MyRegressionParams maxParams = new
		// MyRegressionParams(params.regressors,
		// params.regressorNames, target, params.targetName);

		// double[][] temp = standardizationParams(facetsOfInterest());
		// double[] means = temp[0];
		// int[] xIndexes = xIndexes(regression);
		// int yCount = (int) (means[yIndex] * getCountInUniverse(yIndex));
		// int nStates = 1 << xIndexes.length;
		boolean allNegative = true;
		boolean allPositive = true;
		for (int i = 1; i < getCoefs().length; i++) {
			if (getCoefs()[i] < 0)
				allPositive = false;
			else
				allNegative = false;
		}
		// Perspective y = (Perspective) facetsOfInterest().get(yIndex());
		double rSquared = 0;
		if (!allNegative)
			rSquared = Math.max(rSquared, maxRsquaredInternal(true));
		if (!allPositive && rSquared < 1.0)
			rSquared = Math.max(rSquared, maxRsquaredInternal(false));
		return rSquared;
	}

	private int nZeroSubstates(int startSubstate, boolean isPositive) {
		int result = 0;
		int nStates = params.counts.length;
		int nSubstates = nStates / 2;
		for (int i = startSubstate + 1; i < nSubstates; i++) {
			// substate is the state of the Xs. state is the state of all vars.
			int substate = isPositive ? nSubstates - i - 1 : i;
			int offstate = substate * 2;
			int onstate = offstate + 1;
			int count = params.counts[offstate] + params.counts[onstate];
			if (count < 2)
				result++;
		}
		return result;
	}

	private double maxRsquaredInternal(boolean isPositive) {
		int nStates = params.counts.length;
		int[] maxCounts = new int[nStates];
		int yCount = params.yCount();
		int nSubstates = nStates / 2;
		for (int i = 0; i < nSubstates; i++) {
			// substate is the state of the Xs. state is the state of all vars.
			int substate = isPositive ? nSubstates - i - 1 : i;
			int offstate = substate * 2;
			int onstate = offstate + 1;
			int count = params.counts[offstate] + params.counts[onstate];
			int onCount = Math.min(yCount
					- (nSubstates - i - 1 - nZeroSubstates(i, isPositive)),
					count);
			if (count > 1) {
				if (onCount == 0) {
					onCount++;
					// yCount--;
				} else if (onCount == count) {
					onCount--;
					// yCount++;
				}
			}
			int offCount = count - onCount;
			assert onCount >= 0 : yCount + " " + count + " " + onCount + " "
					+ i + " " + nSubstates + " "
					+ Util.valueOfDeep(params.counts);
			assert offCount >= 0;
			maxCounts[onstate] = onCount;
			maxCounts[offstate] = offCount;
			// Util.print("aa " + substate + " " + yCount + " " + onCount +
			// " "
			// + offCount);
			yCount -= onCount;
		}
		assert yCount == 0 : yCount + " " + Util.valueOfDeep(maxCounts);
		MyRegressionParams maxParams = new MyRegressionParams(maxCounts,
				params.regressorNames, params.targetName);

		TetradRegressionResult maxRegression = getLikeInstance(maxParams);
		double pseudoRsquared = maxRegression.pseudoRsquared();
		// Util.print("maxScoreInternal: isPositive=" + isPositive +
		// "; Rsquared="
		// + pseudoRsquared + "; " + maxRegression);
		return pseudoRsquared;
	}

}