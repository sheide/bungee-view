package edu.cmu.cs.bungee.client.query.tetrad;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pal.mathx.ConjugateGradientSearch;

import JSci.maths.statistics.ChiSq2x2;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;

class Distribution {

	private static final boolean PRINT_RSQUARED = false;
	// private static final int MAX_CACHED_DIST_SIZE = 10;
	protected final List facets;

	protected double[] distribution;
	protected final int totalCount;

	/**
	 * Sum(observed) p*log(p) for normalization of KL divergence, so it only has
	 * to compute p*log(q)
	 */
	private double pLogP = Double.NaN;

	private static Map cachedDistributions = new HashMap();

	boolean approxEquals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Distribution other = (Distribution) obj;
		for (int i = 0; i < getDistribution().length; i++) {
			double a = getDistribution()[i];
			double b = other.getDistribution()[i];
			if (Math.abs(a - b) > 0.001) {
				return false;
			}
		}
		if (facets == null) {
			if (other.facets != null)
				return false;
		} else if (!facets.equals(other.facets))
			return false;
		if (totalCount != other.totalCount)
			return false;
		return true;
	}

	static Distribution getObservedDistribution(List facets,
			List likelyCandidates) {
		Distribution larger = null;
		for (Iterator it = cachedDistributions.entrySet().iterator(); it
				.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			List cachedFacets = (List) entry.getKey();
			// Util.print("in cache "+cachedFacets);
			if (cachedFacets.containsAll(facets)) {
				larger = (Distribution) entry.getValue();
			}
		}
		if (larger == null) {
			if (likelyCandidates != null) {
				larger = cacheCandidates(facets, likelyCandidates);
			} else {
				larger = getInstance(facets);
			}
		}
		Distribution result = larger.getMarginalDistribution(facets);
		return result;
	}

	static Distribution cacheCandidates(List facets, List likelyCandidates) {
		// Util.print("fcounts " + Util.valueOfDeep(fCounts) + " "
		// + Util.valueOfDeep(counts2dist(fCounts)));
		Distribution larger = null;
		Collection candidates = new HashSet(likelyCandidates);
		candidates.removeAll(facets);
		int[] fCounts = null;

		for (Iterator it = cachedDistributions.entrySet().iterator(); it
				.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			List cachedFacets = (List) entry.getKey();
			// Util.print("in cache "+cachedFacets);
			if (cachedFacets.containsAll(facets)) {
				candidates.removeAll(cachedFacets);
				larger = (Distribution) entry.getValue();
				if (fCounts == null)
					fCounts = larger.getMarginalCounts(facets);
			}
		}
		assert fCounts != null;
		if (candidates.size() > 0) {
			try {
				ResultSet rs = query(facets).onCountMatrix(facets, candidates);
				int nFacets = facets.size();
				int[] counts = new int[1 << nFacets];
				int prevFacet = -1;
				List allFacets = new ArrayList(facets);
				int[] facetsIndexes = getMarginIndexes(facets, facets);
				int candidateIndex = -1;
				while (rs.next()) {
					int facet = rs.getInt(1);
					assert facet > 0 : MyResultSet.valueOfDeep(rs,
							MyResultSet.SNMINT_INT_INT, 300);
					if (facet != prevFacet) {

						if (prevFacet > 0) {
							larger = getInstance(allFacets, counts);
							// if (prevFacet == 0)
							// fCounts = counts;
						}

						prevFacet = facet;
						counts = new int[1 << (nFacets + 1)];
						allFacets = new ArrayList(nFacets + 1);
						allFacets.addAll(facets);
						Perspective p = query(facets).findPerspective(facet);
						allFacets.add(p);
						Collections.sort(allFacets);
						facetsIndexes = getMarginIndexes(allFacets, facets);
						candidateIndex = allFacets.indexOf(p);
						for (int substate = 0; substate < fCounts.length; substate++) {
							int state = setSubstate(0, substate, facetsIndexes);
							counts[state] = fCounts[substate];
						}
					}
					int substate = rs.getInt(2);
					int state = setSubstate(0, substate, facetsIndexes);
					int count = rs.getInt(3);
					if (candidateIndex >= 0) {
						counts[state] = fCounts[substate] - count;
						state = Util.setBit(state, candidateIndex, true);
					}
					counts[state] = count;
					// Util.print("zz " + facet + " " + state + " " + count );
				}
				larger = getInstance(allFacets, counts);
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// List toCache = new LinkedList(facets);
		// for (Iterator it = likelyCandidates.iterator();
		// it.hasNext();) {
		// Perspective p = (Perspective) it.next();
		// if (!toCache.contains(p)) {
		// toCache.add(p);
		// }
		// if (toCache.size() >= MAX_CACHED_DIST_SIZE) {
		// larger = cacheDist(toCache);
		// toCache = new LinkedList(facets);
		// }
		// }
		// if (toCache.size() > facets.size())
		// larger = cacheDist(toCache);
		return larger;
	}

	// private static Distribution cacheIt(List allFacets, int[] counts) {
	// Distribution larger = Distribution.getInstance(allFacets, counts);
	// // Util.print("caching " + larger);
	// cachedDistributions.put(allFacets, larger);
	// return larger;
	// }

	// static Distribution cacheDist(List facets) {
	// Collections.sort(facets);
	// Distribution larger = Distribution.getObservedDistribution(facets);
	// // Util.print("caching " + larger);
	// cachedDistributions.put(facets, larger);
	// return larger;
	// }
	//
	// static Distribution getObservedDistribution(List facets) {
	// Distribution result = new Distribution(facets,
	// counts2dist(getCounts(facets)));
	// result.getDistribution(); // check that sum = 1
	// return result;
	// }

	static double[] counts2dist(int[] counts) {
		double total = Util.sum(counts);
		double[] result = new double[counts.length];
		for (int state = 0; state < counts.length; state++) {
			result[state] = counts[state] / total;
		}
		return result;
	}

	// static Distribution getObservedDistribution(List facets,
	// Distribution maxModel) {
	// Distribution result;
	// if (maxModel == null)
	// result = getObservedDistribution(facets);
	// else
	// result = new Distribution(facets, maxModel.getMarginal(facets));
	// result.getDistribution(); // check that sum = 1
	// return result;
	// }

	protected Distribution(List facets, int[] distribution) {
		this.facets = computeFacets(facets);
		totalCount = Util.sum(distribution);
		assert totalCount > 0;
		this.distribution = counts2dist(distribution);
		assert checkWeight(this.distribution);
	}

	protected Distribution(List facets, int count) {
		this.facets = computeFacets(facets);
		totalCount = count;
		assert totalCount > 0;
	}

	private static List computeFacets(List facets) {
		assert !Util.hasDuplicates(facets.toArray()) : facets;
		List result = new ArrayList(facets);
		Collections.sort(result);
		assert result.equals(facets) : facets;
		return Collections.unmodifiableList(result);
	}

	static protected Distribution getInstance(List facets, int[] counts) {
		List args = new ArrayList(facets.size() + counts.length);
		args.addAll(facets);
		for (int i = 0; i < counts.length; i++) {
			args.add(new Integer(counts[i]));
		}
		Distribution result = (Distribution) cachedDistributions.get(args);
		if (result == null) {
			result = new Distribution(facets, counts);
			cachedDistributions.put(Collections.unmodifiableList(args), result);
		}
		return result;
	}

	protected Distribution getMarginalDistribution(List subFacets) {
		if (subFacets.equals(facets))
			return this;
		return getInstance(subFacets, getMarginalCounts(subFacets));
	}

	private static Distribution getInstance(List facets) {
		// Util.print("cacheIt " + facets2);

		// Util.print("getCounts " + facets);
		int nFacets = facets.size();
		int[] counts1 = new int[1 << nFacets];
		ResultSet rs = query(facets).onCountMatrix(facets, null);
		try {
			while (rs.next()) {
				counts1[rs.getInt(2)] = rs.getInt(3);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return getInstance(facets, counts1);
	}

	protected static boolean checkWeight(double[] array) {
		double weight = Util.sum(array);
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

	protected double[] getMarginal(List marginals) {
		// Util.print("getMarginal " + marginals + " " + facets() + " "
		// + Util.valueOfDeep(distribution));
		assert marginals.size() <= nFacets() : marginals + " " + facets();
		assert facets().containsAll(marginals);
		return getMarginal(getMarginIndexes(marginals));
	}

	protected double[] getMarginal(int[] indexes) {
		if (indexes.length == facets.size() && isSequence(indexes))
			return getDistribution();
		// assert indexes.length < facets.size() :
		// "Differently ordered facets: "
		// + facets + " " + Util.valueOfDeep(indexes);
		double[] result = new double[1 << indexes.length];
		for (int state = 0; state < nStates(); state++) {
			// Util.print("getMarginal " + Util.valueOfDeep(indexes) + " " +
			// state
			// + " " + getDistribution()[state] + " "
			// + getSubstate(state, indexes));
			result[getSubstate(state, indexes)] += getDistribution()[state];
		}
		assert checkWeight(result);
		return result;
	}

	private static boolean isSequence(int[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] != i)
				return false;
		}
		return true;
	}

	double[] getDistribution() {
		assert checkWeight(distribution);
		// assert Math.abs(distribution[0] - 0.469015143) > 0.0000001 : facets
		// + " " + Util.valueOfDeep(distribution);
		return distribution;
	}

	protected int[] getCounts() {
		return getMarginalCounts(facets);
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

	protected int[] getMarginIndexes(List marginals) {
		return getMarginIndexes(facets, marginals);
	}

	private static int[] getMarginIndexes(List facets, List marginals) {
		int[] result = new int[marginals.size()];
		int index = 0;
		for (Iterator it = marginals.iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			assert facets.contains(facet) : facet + " " + facets;
			result[index++] = facets.indexOf(facet);
		}
		return result;
	}

	protected List getCauses(Perspective caused) {
		List result = new LinkedList(facets());
		result.remove(caused);
		return result;
	}

	/**
	 * @param state
	 * @param indexes
	 *            bits of state to extract
	 * @return the substate (from 0 to 1 << indexes.length)
	 */
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

	double klDivergenceFromLog(double[] logPredicted) {
		double sum = pLogP();
		double[] obs = getDistribution();
		for (int state = 0; state < nStates(); state++) {
			// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			sum -= obs[state] * logPredicted[state];
		}
		if (Double.isNaN(sum))
			Util.print("KLdivergence " + sum + " "
					+ Util.valueOfDeep(exp(logPredicted)) + "\n"
					+ Util.valueOfDeep(obs));
		return sum;
	}

	private double[] exp(double[] a) {
		double[] result = new double[a.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Math.exp(a[i]);
		}
		return result;
	}

	/**
	 * cache P*log(P) because that is independent of the weights
	 */
	double pLogP() {
		if (Double.isNaN(pLogP)) {
			pLogP = 0;
			double[] obs = getDistribution();
			for (int state = 0; state < nStates(); state++) {
				// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
				double p = obs[state];
				if (p > 0)
					pLogP += p * Math.log(p);
			}
		}
		// assert pLogP >= 0 : pLogP + " " + this;
		return pLogP;
	}

	double klDivergence(double[] predicted) {
		double sum = pLogP();
		double[] obs = getDistribution();
		for (int state = 0; state < nStates(); state++) {
			// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			double p = obs[state];
			if (ConjugateGradientSearch.vvv) {
				Util.print("kl " + state + " " + p + " " + predicted[state]);
			}
			if (p > 0) {
				sum -= p * Math.log(predicted[state]);
//				assert !(Double.isNaN(sum) || Double.isInfinite(sum)) : sum
//						+ " " + p + " " + predicted[state] + "\n"
//						+ Util.valueOfDeep(predicted) + "\n"
//						+ Util.valueOfDeep(obs);
			}
		}
		 if (Double.isNaN(sum) || Double.isInfinite(sum))
		 Util.print("KLdivergence " + sum + " "
		 + Util.valueOfDeep(predicted) + "\n"
		 + Util.valueOfDeep(obs));
		if (ConjugateGradientSearch.vvv)
			Util.print("kl " + sum);
		return sum;
	}

	protected double odds = -1;

	/**
	 * @param causeNode
	 * @param causedNode
	 * @return the odds of both nodes being on
	 */
	double odds(int causeNode, int causedNode) {
		if (odds < 0) {
			double[] dist = getDistribution();
			double pOn = 0;
			for (int state = 0; state < dist.length; state++) {
				if (Util.isBit(state, causeNode)
						&& Util.isBit(state, causedNode)) {
					pOn += dist[state];
				}
			}
			// pOn = Math.max(1e-8, pOn);
			pOn = Util.constrain(pOn, 1e-8, 1 - 1e-8);
			odds = pOn / (1 - pOn);
		}
		return odds;
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

	protected static Query query(Collection facets) {
		// We allow facets to be empty, as long as we're not primary
		Query query = ((Perspective) Util.some(facets)).query();
		return query;
	}

	double mutualInformation(List facets1, List facets2) {
		assert facets.containsAll(facets1);
		assert facets.containsAll(facets2);
		Set allFacets = new HashSet(facets1);
		allFacets.addAll(facets2);
		assert allFacets.size() == facets1.size() + facets2.size();
		Distribution dist;
		if (allFacets.size() == facets().size()) {
			dist = this;
		} else {
			List facetList = new ArrayList(allFacets);
			dist = getMarginalDistribution(facetList);
		}
		Distribution dist1 = getMarginalDistribution(facets1);
		Distribution dist2 = getMarginalDistribution(facets2);
		// Util.print("mutInf " + facets1 + ": " + dist1.entropy() + " " +
		// facets2
		// + ": " + dist2.entropy() + " both: " + dist.entropy());
		return dist1.entropy() + dist2.entropy() - dist.entropy();
	}

	double sumCorrelation(Perspective facet) {
		double result = 0;
		int n = 0;
		for (Iterator it = facets().iterator(); it.hasNext();) {
			Perspective facet2 = (Perspective) it.next();
			if (facet2 != facet) {
				result += Math.abs(getChiSq(facet, facet2).correlation());
				// Util.print("sumCorrelation " + facet2 + " " + facet + " "
				// + getChiSq(facet, facet2).correlation() + " "
				// + getChiSq(facet, facet2).printTable());
				n++;
			}
		}
		return result / n;
	}

	ChiSq2x2 getChiSq(Perspective marginal1, Perspective marginal2) {
		List marginals = new ArrayList(2);
		marginals.add(marginal1);
		marginals.add(marginal2);
		int[] marginal = getMarginalCounts(marginals);
		ChiSq2x2 result = ChiSq2x2.getInstance(null, totalCount, marginal[0]
				+ marginal[1], marginal[0] + marginal[2], marginal[0]);
		return result;
	}

	// double entropy(Distribution d) {
	// Set allFacets = new HashSet(facets());
	// allFacets.addAll(d.facets());
	// return getObservedDistribution(new ArrayList(allFacets)).entropy();
	// }

	double entropy() {
		double result = 0;
		for (int state = 0; state < nStates(); state++) {
			result -= ChiSq2x2.nLogn(distribution[state]);
		}
		return result;
	}

	// double sumR(Distribution predictedDistribution) {
	// double result = 0;
	// double[] marginal = predictedDistribution.getMarginal(facets);
	// // Util.print("sumR\n" + predictedDistribution + "\n"
	// // + Util.valueOfDeep(marginal) + "\n"
	// // + Util.valueOfDeep(getDistribution()));
	// for (Iterator it = facets.iterator(); it.hasNext();) {
	// Perspective p = (Perspective) it.next();
	// result += R(marginal, p);
	// }
	// assert !Double.isNaN(result);
	// return result;
	// }

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
	 * Efron's Pseudo R-Squared see
	 * 
	 * http://www.ats.ucla.edu/stat/mult_pkg/faq/general/Psuedo_RSquareds.htm
	 * 
	 * Compares predicted, rather than do a logistic regression
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
					System.out.print(p.getName().substring(0,
							Math.min(p.getName().length(), 5))
							+ " ");
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
				double predictedTotal = predicted[onState]
						+ predicted[offState];
				double yHat = predictedTotal > 0 ? predicted[onState]
						/ predictedTotal : 0.5;
				double residual = yHat * yHat * obsOff + (1 - yHat)
						* (1 - yHat) * obsOn;
				residuals += residual;
				assert !Double.isNaN(residuals) : residual + " " + yHat + " "
						+ Util.valueOfDeep(predicted);

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
		if (PRINT_RSQUARED || Double.isNaN(Rsquare)) {
			Util.print("pseudoR="
					+ formatDouble(Math.sqrt(Math.abs(Rsquare))
							* Util.sgn(Rsquare)) + " " + caused
					+ " unconditional=" + formatPercent(unconditional) + " "
					+ formatResidual(residuals) + "/"
					+ formatResidual(baseResiduals) + "\n");
		}

		// When trying to model a larger distribution, it can end up predicting
		// the primary facets worse than the unconditional average
		// assert Rsquare >= -0.0001 && Rsquare <= 1.0001 : this + " Rsquare="
		// + Rsquare + " unconditional=" + unconditional + " "
		// + Util.valueOfDeep(predicted) + " residuals=" + residuals
		// + " baseResiduals=" + baseResiduals;
		// Rsquare = Math.max(0, Rsquare);

		assert !Double.isNaN(Rsquare) : residuals + " " + baseResiduals;
		assert !Double.isInfinite(Rsquare) : causedIndex + " "
				+ Util.valueOfDeep(predicted) + " " + residuals + " / "
				+ baseResiduals + " " + Util.valueOfDeep(predicted);
		return Rsquare;
	}

	double[][] causedRs;
	private HashMap rCache = new HashMap();

	double averageSumR2(Perspective cause, Perspective caused,
			Distribution predicted) {
		int causeNode = facetIndex(cause);
		int causedNode = facetIndex(caused);
		if (causedRs == null) {
			causedRs = new double[nFacets()][];
			for (int i = 0; i < causedRs.length; i++) {
				causedRs[i] = new double[nFacets()];
				Arrays.fill(causedRs[i], Double.NaN);
			}
		}
		double result = causedRs[causeNode][causedNode];
		if (Double.isNaN(result)) {
			int nPerm = 0;
			double sumR2 = 0;
			Collection otherCauses = new LinkedList(getCauses(caused));
			otherCauses.remove(cause);
			for (Iterator combIt = new Util.CombinationIterator(otherCauses); combIt
					.hasNext();) {
				Collection x = (Collection) combIt.next();
				List otherCausesComb = new ArrayList(x.size() + 1);
				otherCausesComb.addAll(x);
				otherCausesComb.add(caused);
				Collections.sort(otherCausesComb);
				List allCausesComb = new ArrayList(otherCausesComb.size() + 1);
				allCausesComb.addAll(otherCausesComb);
				allCausesComb.add(cause);
				Collections.sort(allCausesComb);
				double prevR2 = R(otherCausesComb, caused, predicted);
				double R2 = R(allCausesComb, caused, predicted);

				// Util.print("averageSumR2 " + x + " + " + cause + " => "
				// + caused + " " + prevR2 + " " + R2 + " " + this);

				// If we compute R with Logistic Regression, the coefficients
				// (and hence the graph labels) won't be symmetric:(
				//
				// If we compute R from the two (different) predicted
				// distributions, R may not increase monotonically with more
				// variables.
				//
				// assert R2 + 2e-6 > prevR2 : cause + " => " + caused + " <= "
				// + getMarginalDistribution(otherCausesComb) + " "
				// + prevR2 + "; "
				// + getMarginalDistribution(allCausesComb) + " " + R2
				// + " " + this;

				if (R2 > prevR2)
					sumR2 += R2 - prevR2;
				nPerm++;
			}
			result = sumR2 / nPerm;
			causedRs[causeNode][causedNode] = result;
		}
		return result;
	}

	// This ignores the learned weights, and thus any contribution of weight
	// stability.
	/**
	 * Do a logistic regression on caused using all and only its causes.
	 * 
	 */
	double R(Perspective caused) {
		double result = 0;
		Double cached = (Double) rCache.get(caused);
		if (false && cached != null)
			result = cached.doubleValue();
		else {
			List causes = getCauses(caused);
			if (causes.size() > 0) {
				assert !causes.contains(caused) : caused + " " + this;
				assert !Util.hasDuplicates(causes) : caused + " " + this;
				assert causes.size() > 0 : caused + " " + this;

				int nCauses = causes.size();
				List prevfacets = new ArrayList(nCauses + 1);
				prevfacets.addAll(causes);
				prevfacets.add(0, caused);
				// prevfacets.addAll(facets);
				// Collections.sort(prevfacets);
				// prevfacets.add(0, caused);
				// List causeds = new ArrayList(1);
				// causeds.add(caused);
				// Util.print("aa " + allEdges(prevfacets, causeds) + " " +
				// prevfacets
				// + " " + causeds);
				// Explanation expl = NonAlchemyModel.getInstance(prevfacets,
				// // getEdgesAmong(prevfacets)
				// allEdges(prevfacets, causeds));
				// double pseudoRsquared = expl.pseudoRsquared(caused);
				// if (pseudoRsquared < 0)
				// expl.predicted.printGraph(null, null);
				//				
				String[] regressorNames = new String[nCauses];
				int i = 0;
				for (Iterator it = causes.iterator(); it.hasNext();) {
					Perspective cause = (Perspective) it.next();
					regressorNames[i++] = tetradName(cause);
				}
				MyRegressionParams params = new MyRegressionParams(
						getMarginalCounts(prevfacets), regressorNames,
						tetradName(caused));
				double pseudoRsquared = MyLogisticRegressionResult.getInstance(
						params).pseudoRsquared();

				result = Math.sqrt(Math.abs(pseudoRsquared))
						* Util.sgn(pseudoRsquared);

				// result = new LogisticRegression(caused, this).R();

				rCache.put(caused, new Double(result));

				Util.print("R " + caused + " <= " + result + " " + this + "\n");
			}
		}
		return result;
	}

	double R(List causes, Perspective caused, Distribution predicted) {
		if (causes.isEmpty())
			return 0;
		if (!causes.contains(caused)) {
			List causesAndCaused = new ArrayList(causes.size() + 1);
			causesAndCaused.addAll(causes);
			causesAndCaused.add(caused);
			Collections.sort(causesAndCaused);
			causes = causesAndCaused;
		}
		return getMarginalDistribution(causes).R(predicted.getMarginal(causes),
				caused);

	}

	void printCounts() {
		double[] marginals = new double[nFacets()];
		int[] marginal = new int[1];
		int index = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			System.out.print(p.getName().substring(0, 5) + " ");
			marginal[0] = index;
			marginals[index++] = getMarginal(marginal)[1];
		}
		System.out.print("observed  if independent");
		Util.print("");

		for (int state = 0; state < nStates(); state++) {
			double obs = getDistribution()[state];
			double indep = 1;
			for (int i = 0; i < facets.size(); i++) {
				System.out.print(Util.getBit(state, i) + "     ");
				indep *= Util.isBit(state, i) ? marginals[i] : 1 - marginals[i];
			}
			Util.print(formatInt(obs) + " " + formatInt(indep));
		}
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
		if (Double.isNaN(x))
			return "NaN";
		return formatInt(10000 * x / totalCount);
	}

	String formatInt(double dx) {
		int x = (int) Math.round(dx * totalCount);
		return align.format(x, countFormat);
	}

	int nFacets() {
		return facets.size();
	}

	int nStates() {
		return 1 << nFacets();
	}

	protected Perspective getFacet(int i) {
		return (Perspective) facets.get(i);
	}

	public String toString() {
		return "<" + Util.shortClassName(this) + " " + facets + " "
				+ Util.valueOfDeep(getCounts()) + ">";
	}

}
