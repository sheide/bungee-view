package edu.cmu.cs.bungee.client.query.tetrad;

import java.sql.ResultSet;
import java.sql.SQLException;
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

import JSci.maths.statistics.ChiSq2x2;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

class Distribution {

	// private static final int MAX_CACHED_DIST_SIZE = 10;
	protected final List facets;

	protected final double[] distribution;
	protected int[] counts;
	protected final int totalCount;

	/**
	 * Sum(observed) p*log(p) for normalization of KL divergence, so it only has
	 * to compute p*log(q)
	 */
	private double pLogP = Double.NaN;
	protected final int nFacets;

	protected static Map cachedDistributions = new HashMap();

	protected static Distribution createAndCache(List facets, int[] counts) {
		assert !cachedDistributions.containsKey(facets);
		Distribution result = new Distribution(facets, counts);
		cachedDistributions.put(result.facets, result);
		return result;
	}

	/**
	 * This is only for immutable Distributions
	 */
	protected Distribution(List facets, int[] counts) {
		this.facets = computeFacets(facets);
		this.nFacets = facets.size();
		totalCount = Util.sum(counts);
		this.counts = counts;
		assert totalCount > 0;
		this.distribution = counts2dist(counts);
		assert checkDist(this.distribution);
		// Util.print("**new dist " + facets+" "+Util.valueOfDeep(counts));
	}

	/**
	 * This is only for GraphicalModels
	 */
	protected Distribution(List facets, int count) {
		this.facets = computeFacets(facets);
		this.nFacets = facets.size();
		this.distribution = new double[nStates()];
		totalCount = count;
		assert totalCount > 0;
	}

	static protected Distribution ensureDist(List facets, int[] counts) {
		// List args = new ArrayList(facets.size() + counts.length);
		// args.addAll(facets);
		// for (int i = 0; i < counts.length; i++) {
		// args.add(new Integer(counts[i]));
		// }
		Distribution result = (Distribution) cachedDistributions.get(facets);
		if (result == null) {
			result = createAndCache(facets, counts);
			// xxresult = getDistFromDB(facets, null);
		}
		assert result.facets.equals(facets);
		assert Arrays.equals(counts, result.counts);
		return result;
	}

	private static Distribution ensureDist(List facets) {
		Distribution result = (Distribution) cachedDistributions.get(facets);
		for (Iterator it = cachedDistributions.entrySet().iterator(); it
				.hasNext()
				&& result == null;) {
			Map.Entry entry = (Map.Entry) it.next();
			// Util.print("in cache "+cachedFacets);
			if (((List) entry.getKey()).containsAll(facets)) {
				result = ((Distribution) entry.getValue())
						.getMarginalDistribution(facets);
			}
		}
		if (result == null)
			result = getDistFromDB(facets, null);
		return result;
	}

	private Distribution getMarginalDistribution(List subFacets) {
		return ensureDist(subFacets, getMarginalCounts(subFacets));
	}

	static Distribution cacheCandidateDistributions(List facets,
			List likelyCandidates) {
		Collection candidates = new HashSet();
		if (likelyCandidates != null)
			candidates.addAll(likelyCandidates);
		candidates.removeAll(facets);
		int[] fCounts = null;
		for (Iterator it = cachedDistributions.entrySet().iterator(); it
				.hasNext()
				&& (candidates.size() > 0 || fCounts == null);) {
			Map.Entry entry = (Map.Entry) it.next();
			List cachedFacets = (List) entry.getKey();
			// Util.print("in cache "+cachedFacets);
			if (cachedFacets.containsAll(facets)) {
				candidates.removeAll(cachedFacets);
				if (fCounts == null) {
					fCounts = ((Distribution) entry.getValue())
							.getMarginalCounts(facets);
				}
			}
		}
		if (candidates.size() > 0) {
			try {
				ResultSet[] rss = query(facets).onCountMatrix(facets,
						candidates, fCounts == null);
				if (fCounts == null) {
					fCounts = getDistFromDB(facets, rss[0]).getCounts();
				}
				ResultSet rs = rss[1];
				int[] counts = null;
				int prevFacet = -1;
				List allFacets = null;
				int[] facetsIndexes = null;
				int candidateIndex = -1;
				int nFacets = facets.size() + 1;
				while (rs.next()) {
					int facet = rs.getInt(1);
					assert facet > 0 : MyResultSet.valueOfDeep(rs,
							MyResultSet.SNMINT_INT_INT, 300);
					if (facet != prevFacet) {
						if (prevFacet > 0) {
							createAndCache(allFacets, counts);
						}
						prevFacet = facet;
						counts = new int[1 << nFacets];
						allFacets = new ArrayList(nFacets);
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
					counts[state] = fCounts[substate] - count;
					counts[Util.setBit(state, candidateIndex, true)] = count;
					// Util.print("zz " + facet + " " + state + " " + count);
				}
				createAndCache(allFacets, counts);
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return ensureDist(facets);
	}

	private static Distribution getDistFromDB(List facets, ResultSet rs) {
		if (rs == null)
			rs = query(facets).onCountMatrix(facets, null, true)[0];
		int nFacets = facets.size();
		int[] counts1 = new int[1 << nFacets];
		try {
			while (rs.next()) {
				assert rs.getInt(1) == 0;
				int state = rs.getInt(2);
				assert state > 0;
				int count = rs.getInt(3);
				counts1[state] = count;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		counts1[0] = query(facets).getTotalCount() - Util.sum(counts1);
		Distribution result = createAndCache(facets, counts1);
		return result;
	}

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

	private static double[] counts2dist(int[] counts) {
		double total = Util.sum(counts);
		double[] result = new double[counts.length];
		for (int state = 0; state < counts.length; state++) {
			result[state] = counts[state] / total;
		}
		return result;
	}

	private static List computeFacets(List facets) {
		assert !Util.hasDuplicates(facets.toArray()) : facets;
		List result = new ArrayList(facets);
		Collections.sort(result);
		assert result.equals(facets) : facets;
		return Collections.unmodifiableList(result);
	}

	protected static boolean checkDist(double[] dist) {
		double sum = Util.kahanSum(dist);
		assert Math.abs(sum - 1) < 1e-10 : Util.valueOfDeep(dist);
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

	// private static String tetradName(Perspective p) {
	// String name = p.getNameIfPossible();
	// if (name == null)
	// name = "p";
	// else
	// name = predicateName(name);
	// name += "_" + p.getID();
	// return name;
	// }
	//
	// static String predicateName(String facetName) {
	// facetName = facetName.replaceAll("\n\r\\/:*?\"<>| ", "_");
	// String result = "F" + facetName.toLowerCase();
	// result = result.replaceAll("\\W", "_");
	// result = result.replaceAll("_+", "_");
	// return result;
	// }

	protected double[] getMarginal(List marginals) {
		// Util.print("getMarginal " + marginals + " " + facets() + " "
		// + Util.valueOfDeep(distribution));
		assert marginals.size() <= nFacets : marginals + " " + facets;
		assert facets().containsAll(marginals);
		return getMarginal(getMarginIndexes(marginals));
	}

	private double[] getMarginal(int[] indexes) {
		if (indexes.length == nFacets && isSequence(indexes))
			return getDistribution();
		// assert indexes.length < facets.size() :
		// "Differently ordered facets: "
		// + facets + " " + Util.valueOfDeep(indexes);
		double[] result = new double[1 << indexes.length];
		int nStates = nStates();
		for (int state = 0; state < nStates; state++) {
			// Util.print("getMarginal " + Util.valueOfDeep(indexes) + " " +
			// state
			// + " " + getDistribution()[state] + " "
			// + getSubstate(state, indexes));
			result[getSubstate(state, indexes)] += getDistribution()[state];
		}
		assert checkDist(result);
		return result;
	}

	protected int[] getMarginalCounts(List marginals) {
		int[] counts1 = getCounts();
		if (marginals.equals(facets))
			return counts1;
		int[] indexes = getMarginIndexes(marginals);
		int[] result = new int[1 << indexes.length];
		int nStates = nStates();
		for (int state = 0; state < nStates; state++) {
			result[getSubstate(state, indexes)] += counts1[state];
		}
		assert Util.sum(result) == totalCount : totalCount + " "
				+ Util.valueOfDeep(result);
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
		assert checkDist(distribution);
		// assert Math.abs(distribution[0] - 0.469015143) > 0.0000001 : facets
		// + " " + Util.valueOfDeep(distribution);
		return distribution;
	}

	protected int[] getCounts() {
		if (counts == null) {
			counts = new int[distribution.length];
			int sum = 0;
			int maxState = 0;
			int maxValue = 0;
			for (int substate = 0; substate < counts.length; substate++) {
				int count = (int) Math.round(distribution[substate]
						* totalCount);
				counts[substate] = count;
				sum += count;
				if (count > maxValue) {
					maxValue = count;
					maxState = substate;
				}
			}
			// correct for any rounding errors
			counts[maxState] += totalCount - sum;
		}
		return counts;
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

	List getCauses(Perspective caused) {
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
	private static int getSubstate(int state, int[] indexes) {
		int result = 0;
		for (int index = 0; index < indexes.length; index++) {
			result = Util.setBit(result, index, Util.isBit(state,
					indexes[index]));
		}
		return result;
	}

	private static int setSubstate(int state, int substate, int[] indexes) {
		for (int subvar = 0; subvar < indexes.length; subvar++) {
			state = Util.setBit(state, indexes[subvar], Util.isBit(substate,
					subvar));
		}
		// Util.print("setState " + substate + " " + Util.valueOfDeep(indexes)
		// + " " + state);
		return state;
	}

	double klDivergenceFromLog(double[] logPredicted) {
		double[] obs = getDistribution();
		int nStates = nStates();
		double[] addends = new double[nStates + 1];
		for (int state = 0; state < nStates; state++) {
			// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			addends[state] = obs[state] * logPredicted[state];
		}
		addends[nStates] = -pLogP();// Double.NEGATIVE_INFINITY;
		// Arrays.sort(addends);
		// addends[0]=-pLogP();
		// Util.print(Util.valueOfDeep(addends));
		double sum = -Util.kahanSum(addends);
		// if (NonAlchemyModel.printInterval[0] > 0) {
		// Util.testSum(addends, sum);
		// }
		if (Double.isNaN(sum) || sum < -1e15)
			Util.print("KLdivergence " + sum + "\n"
					+ Util.valueOfDeep(exp(logPredicted)) + "\n"
					+ Util.valueOfDeep(obs));
		return Math.max(0, sum);
	}

	double klDivergence(double[] predicted) {
		double[] obs = getDistribution();
		int nStates = nStates();
		double[] addends = new double[nStates + 1];
		for (int state = 0; state < nStates; state++) {
			// assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			double p = obs[state];
			if (p > 0) {
				addends[state] = -p * Math.log(predicted[state]);
				// assert !(Double.isNaN(sum) || Double.isInfinite(sum)) : sum
				// + " " + p + " " + predicted[state] + "\n"
				// + Util.valueOfDeep(predicted) + "\n"
				// + Util.valueOfDeep(obs);
			}
		}
		addends[nStates] = pLogP();
		double sum = Util.kahanSum(addends);
		if (Double.isNaN(sum) || Double.isInfinite(sum))
			Util.print("KLdivergence " + sum + "\n"
					+ Util.valueOfDeep(predicted) + "\n"
					+ Util.valueOfDeep(obs));
		return sum;
	}

	double[] independenceDistribution() {
		double[] marginals = new double[nFacets];
		int[] index = new int[1];
		for (int i = 0; i < marginals.length; i++) {
			index[0] = i;
			marginals[i] = getMarginal(index)[0];
		}
		double[] result = new double[nStates()];
		for (int state = 0; state < result.length; state++) {
			double p = 1;
			for (int i = 0; i < marginals.length; i++) {
				p *= Util.isBit(state, i) ? marginals[i] : 1 - marginals[i];
			}
			result[state] = p;
		}
		assert checkDist(result);
		return result;
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
	private double pLogP() {
		if (Double.isNaN(pLogP)) {
			pLogP = 0;
			pLogP = -klDivergence(getDistribution());
			// double[] obs = getDistribution();
			// for (int state = 0; state < nStates(); state++) {
			// // assert predicted[i] > 0 : Util.valueOfDeep(predicted);
			// double p = obs[state];
			// if (p > 0)
			// pLogP += p * Math.log(p);
			// }
		}
		// assert pLogP >= 0 : pLogP + " " + this;
		return pLogP;
	}

	private double[][] odds;

	/**
	 * @param causeNode
	 * @param causedNode
	 * @return the odds of both nodes being on
	 */
	double odds(int causeNode, int causedNode) {
		if (odds == null) {
			// int nFacets = nFacets();
			odds = new double[nFacets][];
			for (int i = 0; i < odds.length; i++) {
				odds[i] = new double[nFacets];
			}
			decacheOdds();
		}
		if (odds[causeNode][causedNode] < 0) {
			double[] dist = getDistribution();
			double pOn = 0;
			for (int state = 0; state < dist.length; state++) {
				if (Util.isBit(state, causeNode)
						&& Util.isBit(state, causedNode)) {
					pOn += dist[state];
				}
			}
			odds[causeNode][causedNode] = pOn == 1 ? Double.POSITIVE_INFINITY
					: pOn / (1 - pOn);
			odds[causedNode][causeNode] = odds[causeNode][causedNode];
		}
		return odds[causeNode][causedNode];
	}

	protected void decacheOdds() {
		if (odds != null) {
			for (int i = 0; i < odds.length; i++) {
				Arrays.fill(odds[i], -1);
			}
		}
	}

	protected void decacheOdds(int cause, int caused) {
		if (odds != null) {
			odds[cause][caused] = -1;
			odds[caused][cause] = -1;
		}
	}

	private static Query query(Collection facets) {
		// We allow facets to be empty, as long as we're not primary
		Query query = ((Perspective) Util.some(facets)).query();
		return query;
	}

	// double mutualInformation(List facets1, List facets2) {
	// assert facets.containsAll(facets1);
	// assert facets.containsAll(facets2);
	// Set allFacets = new HashSet(facets1);
	// allFacets.addAll(facets2);
	// assert allFacets.size() == facets1.size() + facets2.size();
	// Distribution dist;
	// if (allFacets.size() == facets().size()) {
	// dist = this;
	// } else {
	// List facetList = new ArrayList(allFacets);
	// dist = getMarginalDistribution(facetList);
	// }
	// Distribution dist1 = getMarginalDistribution(facets1);
	// Distribution dist2 = getMarginalDistribution(facets2);
	// // Util.print("mutInf " + facets1 + ": " + dist1.entropy() + " " +
	// // facets2
	// // + ": " + dist2.entropy() + " both: " + dist.entropy());
	// return dist1.entropy() + dist2.entropy() - dist.entropy();
	// }

	// double sumCorrelation(Perspective facet) {
	// double result = 0;
	// int n = 0;
	// for (Iterator it = facets().iterator(); it.hasNext();) {
	// Perspective facet2 = (Perspective) it.next();
	// if (facet2 != facet) {
	// result += Math.abs(getChiSq(facet, facet2).correlation());
	// // Util.print("sumCorrelation " + facet2 + " " + facet + " "
	// // + getChiSq(facet, facet2).correlation() + " "
	// // + getChiSq(facet, facet2).printTable());
	// n++;
	// }
	// }
	// return result / n;
	// }

	ChiSq2x2 getChiSq(Perspective marginal1, Perspective marginal2) {
		List marginals = new ArrayList(2);
		marginals.add(marginal1);
		marginals.add(marginal2);
		int[] marginal = getMarginalCounts(marginals);
		ChiSq2x2 result = ChiSq2x2.getInstance(null, totalCount, marginal[0]
				+ marginal[1], marginal[0] + marginal[2], marginal[0]);
		return result;
	}

	// double entropy() {
	// double result = 0;
	// int nStates = nStates();
	// for (int state = 0; state < nStates; state++) {
	// result -= ChiSq2x2.nLogn(distribution[state]);
	// }
	// return result;
	// }

	/**
	 * Can't use Perspective.stdDev, because deeply nested facets have
	 * totalCount = -1
	 * 
	 * @return standard deviation of binary variable p, over the whole database
	 */
	protected static double stdDev(Perspective p) {
		List facets1 = new ArrayList(1);
		facets1.add(p);
		Distribution dist = Distribution.ensureDist(facets1, null);
		double n = dist.totalCount;
		double count = dist.getDistribution()[1];
		double stdDev = Math.sqrt(count * (n - count) / (n * (n - 1)));
		assert stdDev >= 0 : count + " " + n + " " + p;
		return stdDev;
	}

	// private double[][] causedRs;
	//
	// // private HashMap rCache = new HashMap();
	//
	// // This could cache more (lists & combinations); maybe compute for all
	// // cause/caused pairs at once.
	// protected double averageSumR2(Perspective cause, Perspective caused,
	// Distribution predicted) {
	// int causeNode = facetIndex(cause);
	// int causedNode = facetIndex(caused);
	// if (causedRs == null) {
	// // int nFacets = nFacets();
	// causedRs = new double[nFacets][];
	// for (int i = 0; i < causedRs.length; i++) {
	// causedRs[i] = new double[nFacets];
	// Arrays.fill(causedRs[i], Double.NaN);
	// }
	// }
	// double result = causedRs[causeNode][causedNode];
	// if (Double.isNaN(result)) {
	// int nPerm = 0;
	// double sumR2 = 0;
	// Collection otherCauses = new LinkedList(getCauses(caused));
	// otherCauses.remove(cause);
	// for (Iterator combIt = new Util.CombinationIterator(otherCauses); combIt
	// .hasNext();) {
	// Collection x = (Collection) combIt.next();
	// List otherCausesComb = new ArrayList(x.size() + 1);
	// otherCausesComb.addAll(x);
	// otherCausesComb.add(caused);
	// Collections.sort(otherCausesComb);
	// List allCausesComb = new ArrayList(otherCausesComb.size() + 1);
	// allCausesComb.addAll(otherCausesComb);
	// allCausesComb.add(cause);
	// Collections.sort(allCausesComb);
	// assert !Util.hasDuplicates(allCausesComb.toArray()) : allCausesComb
	// + " "
	// + otherCausesComb
	// + " "
	// + cause
	// + " "
	// + caused
	// + " " + otherCauses;
	// double without = R(otherCausesComb, caused, predicted);
	// double with = R(allCausesComb, caused, predicted);
	//
	// // Util.print("averageSumR2 " + x + " + " + cause + " => "
	// // + caused + " " + prevR2 + " " + R2 + " " + this);
	//
	// // If we compute R with Logistic Regression, the coefficients
	// // (and hence the graph labels) won't be symmetric:(
	// //
	// // If we compute R from the two (different) predicted
	// // distributions, R may not increase monotonically with more
	// // variables.
	// //
	// // assert R2 + 2e-6 > prevR2 : cause + " => " + caused + " <= "
	// // + getMarginalDistribution(otherCausesComb) + " "
	// // + prevR2 + "; "
	// // + getMarginalDistribution(allCausesComb) + " " + R2
	// // + " " + this;
	//
	// Util.print("averageSumR2 " + without + " => " + with + " "
	// + otherCausesComb + " + " + cause);
	//
	// // If lbfgs works better for one model than another, just hope
	// // the errors average out.
	// // if (R2 > prevR2)
	// sumR2 += with - without;
	// nPerm++;
	// }
	// result = sumR2 / nPerm;
	// assert 0 <= result && result <= 1 : sumR2 + " " + nPerm;
	// causedRs[causeNode][causedNode] = result;
	// }
	// return result;
	// }
	//
	// private double R(List causes, Perspective caused, Distribution predicted)
	// {
	// if (causes.isEmpty())
	// return 0;
	// if (!causes.contains(caused)) {
	// List causesAndCaused = new ArrayList(causes.size() + 1);
	// causesAndCaused.addAll(causes);
	// causesAndCaused.add(caused);
	// Collections.sort(causesAndCaused);
	// causes = causesAndCaused;
	// }
	// return getMarginalDistribution(causes).R(predicted.getMarginal(causes),
	// caused);
	//
	// }

	// void printCounts() {
	// double[] marginals = new double[nFacets()];
	// int[] marginal = new int[1];
	// int index = 0;
	// for (Iterator it = facets.iterator(); it.hasNext();) {
	// Perspective p = (Perspective) it.next();
	// System.out.print(p.getName().substring(0, 5) + " ");
	// marginal[0] = index;
	// marginals[index++] = getMarginal(marginal)[1];
	// }
	// System.out.print("observed  if independent");
	// Util.print("");
	//
	// for (int state = 0; state < nStates(); state++) {
	// double obs = getDistribution()[state];
	// double indep = 1;
	// for (int i = 0; i < facets.size(); i++) {
	// System.out.print(Util.getBit(state, i) + "     ");
	// indep *= Util.isBit(state, i) ? marginals[i] : 1 - marginals[i];
	// }
	// Util.print(formatInt(obs) + " " + formatInt(indep));
	// }
	// }

	int nFacets() {
		return nFacets;
	}

	protected int nStates() {
		return 1 << nFacets;
	}

	protected Perspective getFacet(int i) {
		return (Perspective) facets.get(i);
	}

	public String toString() {
		return "<" + Util.shortClassName(this) + " " + facets + " "
				+ Util.valueOfDeep(getCounts()) + ">";
	}

}
