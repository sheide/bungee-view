package edu.cmu.cs.bungee.client.query.tetrad;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;

class Distribution {
	private static final boolean PRINT_RSQUARED = false;
	protected final List facets;

	protected double[] distribution;
	protected final int totalCount;

	static Distribution getObservedDistribution(List facets) {
		double total = query(facets).getTotalCount();
		int[] counts = getCounts(facets);
		double[] distribution = new double[counts.length];
		for (int state = 0; state < counts.length; state++) {
			distribution[state] = counts[state] / total;
		}
		Distribution result = new Distribution(facets, distribution);
		result.getDistribution(); // check that sum = 1
		return result;
	}

	static Distribution getObservedDistribution(List facets,
			Distribution maxModel) {
		Distribution result;
		if (maxModel == null)
			result = getObservedDistribution(facets);
		else
			result = new Distribution(facets, maxModel.getMarginal(facets));
		result.getDistribution(); // check that sum = 1
		return result;
	}

	protected Distribution(List facets, double[] distribution) {
		this.facets = new ArrayList(facets);
		this.distribution = distribution;
		totalCount = query(facets).getTotalCount();
		assert totalCount > 0;
		assert distribution == null || getDistribution() != null; // check that
		// sum = 1
	}

	private static boolean checkWeight(double[] array) {
		double weight = 0;
		for (int i = 0; i < array.length; i++) {
			weight += array[i];
		}
		assert Math.abs(weight - 1) < 0.0001 : Util.valueOfDeep(array);
		return true;
	}

	protected List facets() {
		return facets;
	}

	protected int facetIndex(Perspective facet) {
		assert facets.contains(facet) : facet + " " + facets;
		return facetIndexOrNot(facet);
	}

	protected int facetIndexOrNot(Perspective facet) {
		return facets.indexOf(facet);
	}

	// private boolean hasFacet(Perspective facet) {
	// return facets.contains(facet);
	// }

	static String tetradName(Perspective p) {
		String name = p.getNameIfPossible();
		if (name == null)
			name = "p";
		else
			name = predicateName(name);
		name += "_" + p.getID();
		return name;
	}

	static String predicateName(String facetName) {
		facetName = facetName.replaceAll("\n\r\\/:*?\"<>| ", "_");
		String result = "F" + facetName.toLowerCase();
		result = result.replaceAll("\\W", "_");
		result = result.replaceAll("_+", "_");
		return result;
	}

	private static int[] getCounts(List facets) {
		// Util.print("getCounts " + facets);
		int nFacets = facets.size();
		int[] counts1 = new int[1 << nFacets];
		ResultSet rs = query(facets).onCountMatrix(facets);
		try {
			while (rs.next()) {
				counts1[rs.getInt(1)] = rs.getInt(2);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Util.print("\n" + nFacets + " primaryFacets\n "
		// + Util.join(facets, "\n "));
		// List others = new LinkedList(facets);
		// others.removeAll(facets);
		// Util.print(others.size() + " other facets of interest\n "
		// + Util.join(others, "\n "));

		// Util.print("counts: " + Util.valueOfDeep(counts));

		// The low order bit represents the first facet (popupFacet)
		// Perspective popupFacet1 = (Perspective) facets.get(0);
		// int countInUniverse = getCountInUniverse(counts1, 0);
		// assert countInUniverse == popupFacet1.getTotalCount() : popupFacet1
		// + " " + popupFacet1.getTotalCount() + " " + countInUniverse;
		// }
		return counts1;
	}

	protected double[] getMarginal(List marginals) {
		return getMarginal(getMarginIndexes(marginals));
	}

	protected double[] getMarginal(int[] indexes) {
		if (indexes.length == facets.size()) {
			assert isSequence(indexes);
			return getDistribution();
		}
		assert indexes.length < facets.size() : "Differently ordered facets: "
				+ facets + " " + Util.valueOfDeep(indexes);
		double[] result = new double[1 << indexes.length];
		for (int state = 0; state < nStates(); state++) {
			// Util.print("getMarginal " + marginals + " " + state + " "
			// + getDistribution()[state] + " "
			// + getSubstate(state, indexes));
			result[getSubstate(state, indexes)] += getDistribution()[state];
		}
		assert checkWeight(result);
		return result;
	}

	private static boolean isSequence(int[] array) {
		for (int i = 0; i < array.length; i++) {
			assert array[i] == i;
		}
		return true;
	}

	public double[] getDistribution() {
		assert checkWeight(distribution);
		return distribution;
	}

	protected int[] getMarginalCounts(List marginals) {
		double[] marginal = getMarginal(marginals);
		double totalCount2 = totalCount;
		int[] result = new int[marginal.length];
		for (int substate = 0; substate < result.length; substate++) {
			result[substate] = (int) Math.round(marginal[substate]
					* totalCount2);
		}
		return result;
	}

	int[] getMarginIndexes(List marginals) {
		int[] result = new int[marginals.size()];
		int index = 0;
		for (Iterator it = marginals.iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			result[index++] = facetIndex(facet);
		}
		return result;
	}

	static int getSubstate(int state, int[] indexes) {
		int result = 0;
		for (int index = 0; index < indexes.length; index++) {
			result = Util.setBit(result, index, Util.isBit(state,
					indexes[index]));
		}
		return result;
	}

	static int setSubstate(int state, int substate, int[] indexes) {
		for (int subvar = 0; subvar < indexes.length; subvar++) {
			state = Util.setBit(state, indexes[subvar], Util.isBit(substate,
					subvar));
		}
		// Util.print("setState " + substate + " " + Util.valueOfDeep(indexes)
		// + " " + state);
		return state;
	}

	double KLdivergence(double[] logPredicted) {
		double sum = 0;
		for (int state = 0; state < nStates(); state++) {
			// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			sum -= getDistribution()[state] * logPredicted[state];
		}
		if (Double.isNaN(sum))
			Util.print("KLdivergence " + sum + " "
					+ Util.valueOfDeep(logPredicted) + "\n"
					+ Util.valueOfDeep(getDistribution()));
		return sum;
	}

	// static double KLdivergence(double[] observed, double[] predicted) {
	// double sum = 0;
	// for (int i = 0; i < observed.length; i++) {
	// // assert predicted[i] > 0 : Util.valueOfDeep(predicted);
	// if (observed[i] > 0) {
	// if (predicted[i] == 0)
	// sum = Double.POSITIVE_INFINITY;
	// else
	// sum += observed[i] * Math.log(observed[i] / predicted[i]);
	// }
	// }
	// if (Double.isNaN(sum))
	// Util.print("KLdivergence " + sum + " "
	// + Util.valueOfDeep(predicted) + "\n"
	// + Util.valueOfDeep(observed));
	// return sum;
	// }

	// double quality() {
	// // Util.print(buildGraph());
	// return weightSpaceChange(nullModel) * WEIGHT_SPACE_IMPORTANCE
	// - (1 - WEIGHT_SPACE_IMPORTANCE)
	// * KLdivergence(nullModel.getDistribution(false), getMarginal());
	// }

	static Query query(Collection facets) {
		// We allow facets to be empty, as long as we're not primary
		Query query = ((Perspective) Util.some(facets)).query();
		return query;
	}

	double sumR(Distribution predictedDistribution) {
		double result = 0;
		double[] marginal = predictedDistribution.getMarginal(facets);
//		Util.print("sumR\n" + predictedDistribution + "\n"
//				+ Util.valueOfDeep(marginal) + "\n"
//				+ Util.valueOfDeep(getDistribution()));
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			result += R(marginal, p);
		}
		assert !Double.isNaN(result);
		return result;
	}

	double R(double[] predictedDistribution, Perspective caused) {
		double pseudoRsquared = pseudoRsquared(predictedDistribution, caused);
		return Math.sqrt(Math.abs(pseudoRsquared)) * Util.sgn(pseudoRsquared);
		// double sumR = 0;
		// List causes = getCauses(caused);
		// for (Iterator causeIt = causes.iterator(); causeIt.hasNext();) {
		// Perspective cause = (Perspective) causeIt.next();
		// sumR += Math.abs(getWeight(cause, caused));
		// }
		// return sumR;
	}

	// double pseudoRsquared(Distribution predictedDistribution, Perspective
	// caused) {
	// return pseudoRsquared(predictedDistribution.getDistribution(), caused);
	// }

	/**
	 * Efron's Pseudo R-Squared see http://www.ats.ucla.edu/stat/mult_pkg/faq
	 * /general/Psuedo_RSquareds.htm
	 */
	double pseudoRsquared(double[] predicted, Perspective caused) {
		assert predicted != null;
		assert getDistribution() != null;
		assert predicted.length == getDistribution().length;
		assert checkWeight(predicted);
		double residuals = 0;
		double baseResiduals = 0;
		int causedIndex = facetIndex(caused);
		List causeds = new ArrayList(1);
		causeds.add(caused);
		double[] marginal = getMarginal(causeds);
		double unconditional = marginal[1] / (marginal[0] + marginal[1]);
		if (PRINT_RSQUARED) {
			for (Iterator it = facets.iterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (p != caused)
					System.out.print(p.getName().substring(0, 5) + " ");
			}
			System.out
					.print("                observed pred residua uncondi off_nrg  on_nrg   delta");
			Util.print("");
		}
		for (int offState = 0; offState < nStates(); offState++) {
			if (Util.getBit(offState, causedIndex) == 0) {
				int onState = Util.setBit(offState, causedIndex, true);
				double obsOn = getDistribution()[onState];
				double obsOff = getDistribution()[offState];
				double yHat = predicted[onState]
						/ (predicted[onState] + predicted[offState]);
				double residual = yHat * yHat * obsOff + (1 - yHat)
						* (1 - yHat) * obsOn;
				residuals += residual;

				// Util.print(yHat * yHat * obsOff + " " + (1 - yHat) * (1 -
				// yHat)
				// * obsOn);

				// We're calculating this state-by-state for the benefit of
				// printing
				double baseResidual = unconditional * unconditional * obsOff
						+ (1 - unconditional) * (1 - unconditional) * obsOn;
				baseResiduals += baseResidual;

				// Util.print(unconditional * unconditional * obsOff);
				// Util.print((1 - unconditional) * (1 - unconditional) *
				// obsOn);

				if (PRINT_RSQUARED) {
					for (int i = 0; i < facets.size(); i++) {
						if (i != causedIndex)
							System.out
									.print(Util.getBit(offState, i) + "     ");
					}
					Util.print(formatInt(obsOn)
							+ " / "
							+ formatInt(obsOn + obsOff)
							+ " = "
							+ formatPercent(obsOn / (obsOn + obsOff))
							+ " "
							+ formatPercent(yHat)
							+ " "
							+ formatResidual(residual)
							+ " "
							+ formatResidual(baseResidual)
							+ " "
							+ formatDouble(Math.log(predicted[offState]
									/ predicted[0]))
							+ " "
							+ formatDouble(Math.log(predicted[onState]
									/ predicted[0]))
							+ " "
							+ formatDouble(Math.log(predicted[onState]
									/ predicted[offState])) + " "
							+ formatDouble(predicted[offState]) + " "
							+ formatDouble(predicted[onState]));
				}

			}
		}
		// double base = pseudoRsquaredDenom(caused);
		// assert Math.abs(params.yCount() - sumYhat) < 1 : params.yCount() +
		// " "
		// + sumYhat + report + "\n" + yHats();
		double Rsquare = 1 - residuals / baseResiduals;
		if (PRINT_RSQUARED) {
			Util.print("pseudoR="
					+ formatDouble(Math.sqrt(Math.abs(Rsquare))
							* Util.sgn(Rsquare)) + " " + caused
					+ " unconditional=" + formatPercent(unconditional) + " "
					+ formatResidual(residuals) + "/"
					+ formatResidual(baseResiduals) + "\n");
		}

		// When trying to model a larger distribution, it can end up predicting
		// the primary facets worse than the unconditional average
//		assert Rsquare >= -0.0001 && Rsquare <= 1.0001 : this + " Rsquare="
//				+ Rsquare + " unconditional=" + unconditional + " "
//				+ Util.valueOfDeep(predicted) + " residuals=" + residuals
//				+ " baseResiduals=" + baseResiduals;
		// Rsquare = Math.max(0, Rsquare);

		assert !Double.isNaN(Rsquare);
		return Rsquare;
	}

	private static final StringAlign align = new StringAlign(7,
			StringAlign.JUST_RIGHT);

	static final DecimalFormat countFormat = new DecimalFormat("###,###");

	static final DecimalFormat percentFormat = new DecimalFormat("###%");

	static final DecimalFormat doubleFormat = new DecimalFormat(
			" #0.000;-#0.000");

	static String formatPercent(double x) {
		return (new StringAlign(4, StringAlign.JUST_RIGHT)).format(x,
				percentFormat);
		// return percentFormat.format(x);
	}

	static String formatDouble(double x) {
		String format = align.format(x, doubleFormat);
		// return StringAlign.format(format, null, 6, StringAlign.JUST_LEFT)
		// .toString();
		return format;
	}

	String formatResidual(double x) {
		return formatInt(10000 * x / totalCount);
	}

	String formatInt(double dx) {
		int x = (int) Math.round(dx * totalCount);
		return align.format(x, countFormat);
	}

	int nFacets() {
		return facets.size();
	}

	public int nStates() {
		return 1 << nFacets();
	}

	protected Perspective getFacet(int i) {
		return (Perspective) facets.get(i);
	}

	public String toString() {
		return "<" + Util.shortClassName(this) + " " + facets + " "
				+ Util.valueOfDeep(getMarginalCounts(facets)) + ">";
	}

}
