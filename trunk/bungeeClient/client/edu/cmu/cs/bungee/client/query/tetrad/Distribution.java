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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import JSci.maths.statistics.ChiSq2x2;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

public class Distribution {

	// private static final int MAX_CACHED_DIST_SIZE = 10;
	protected final List<ItemPredicate> facets;

	protected final double[] distribution;
	protected int[] counts;
	protected final int totalCount;

	/**
	 * Sum(observed) p*log(p) for normalization of KL divergence, so it only has
	 * to compute p*log(q)
	 */
	private double pLogP = Double.NaN;
	protected final int nFacets;

	protected static Map<CacheDistArgs, Distribution> cachedDistributions = new HashMap<CacheDistArgs, Distribution>();
	protected static Map<List<ItemPredicate>, Set<ItemPredicate>> cacheDAG = new HashMap<List<ItemPredicate>, Set<ItemPredicate>>();

	protected static Distribution createAndCache(List<ItemPredicate> facets,
			int[] counts) {
		assert !cachedDistributions.containsKey(facets) : facets + " "
				+ cachedDistributions.get(facets) + "\n" + cachedDistributions;
		Distribution result = new Distribution(facets, counts);
		// Util.print("createAndCache " + cachedDistributions.size() + " "
		// + facets.size() + " " + result);

		// if (result.facets.size() == 2 && ((Perspective)
		// result.facets.get(0)).getID() == 73
		// && ((Perspective) result.facets.get(0)).getID() == 150) {
		// Util.print(result.facets);
		// Util.printStackTrace();
		// }

		cachedDistributions.put(new CacheDistArgs(result.facets), result);
		addSubsets(result.facets);
		return result;
	}

	static class CacheDistArgs {

		final List<ItemPredicate> facets;
		final Set<List<ItemPredicate>> edges;

		CacheDistArgs(List<ItemPredicate> facets,
				final Set<List<ItemPredicate>> edges) {
			this.facets = Collections.unmodifiableList(facets);
			this.edges = Collections.unmodifiableSet(edges);
		}

		CacheDistArgs(List<ItemPredicate> facets) {
			this.facets = Collections.unmodifiableList(facets);
			edges = null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((edges == null) ? 0 : edges.hashCode());
			result = prime * result
					+ ((facets == null) ? 0 : facets.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheDistArgs other = (CacheDistArgs) obj;
			if (edges == null) {
				if (other.edges != null)
					return false;
			} else if (!edges.equals(other.edges))
				return false;
			if (facets == null) {
				if (other.facets != null)
					return false;
			} else if (!facets.equals(other.facets))
				return false;
			return true;
		}
	}

	private static void addSubsets(List<ItemPredicate> facets) {
		if (facets.size() > 1)
			for (Iterator<ItemPredicate> it = facets.iterator(); it.hasNext();) {
				ItemPredicate p = it.next();
				List<ItemPredicate> remaining = new ArrayList<ItemPredicate>(
						facets);
				remaining.remove(p);
				Set<ItemPredicate> larger = cacheDAG.get(remaining);
				if (larger == null) {
					larger = new TreeSet<ItemPredicate>();
					cacheDAG.put(remaining, larger);
				}
				larger.add(p);
				addSubsets(remaining);
			}
	}

	/**
	 * This is only for immutable Distributions
	 */
	public Distribution(List<ItemPredicate> facets, int[] counts) {
		this.facets = computeFacets(facets);
		this.nFacets = facets.size();
		totalCount = Util.sum(counts);
		this.counts = counts;
		assert totalCount > 0;
		this.distribution = counts2dist(counts);
		try {
			assert checkDist(this.distribution);
		} catch (Error e) {
			throw new IllegalArgumentException("While constructing " + this, e);
		}
		// Util.print("**new dist " + facets+" "+Util.valueOfDeep(counts));
	}

	/**
	 * This is only for GraphicalModels
	 */
	protected Distribution(List<ItemPredicate> facets, int count) {
		this.facets = computeFacets(facets);
		this.nFacets = facets.size();
		this.distribution = new double[nStates()];
		totalCount = count;
		assert totalCount > 0;
	}

	/**
	 * The cache should be a member of Query, but for now we just keep one
	 * global cache.
	 */
	public static void decacheDistributions() {
		// Util.print("decacheDistributions");
		cachedDistributions.clear();
	}

	static protected Distribution ensureDist(List<ItemPredicate> facets,
			int[] counts) {
		// List args = new ArrayList(facets.size() + counts.length);
		// args.addAll(facets);
		// for (int i = 0; i < counts.length; i++) {
		// args.add(new Integer(counts[i]));
		// }
		Distribution result = cachedDistributions.get(facets);
		if (result == null) {
			result = createAndCache(facets, counts);
			// xxresult = getDistFromDB(facets, null);
		}
		assert result.facets.equals(facets);
		assert Arrays.equals(counts, result.counts) : facets + " "
				+ Util.valueOfDeep(counts) + " " + result;
		return result;
	}

	static Distribution findLargerDist(List<ItemPredicate> facets) {
		Distribution result = cachedDistributions.get(facets);
		if (result == null) {
			SortedSet<ItemPredicate> additions = (SortedSet<ItemPredicate>) cacheDAG
					.get(facets);
			if (additions != null) {
				List<ItemPredicate> larger = new LinkedList<ItemPredicate>(
						facets);
				larger.add(additions.first());
				Collections.sort(larger);
				result = findLargerDist(larger);
			}
		}
		return result;
	}

	static Distribution ensureDist(List<ItemPredicate> facets) {
		Distribution result = cachedDistributions.get(facets);
		if (result == null) {
			Distribution largerDist = findLargerDist(facets);
			if (largerDist != null)
				result = largerDist.getMarginalDistribution(facets);
		}
		if (result == null)
			result = getDistFromDB(facets, null);
		return result;
	}

	public Distribution getMarginalDistribution(List<ItemPredicate> subFacets) {
		int[] marginalCounts = getMarginalCounts(subFacets);
		if (cachedDistributions.get(facets) == this) {
			return ensureDist(subFacets, marginalCounts);
		} else
			return new Distribution(subFacets, marginalCounts);

	}

	static Distribution cacheCandidateDistributions(List<ItemPredicate> facets,
			List<ItemPredicate> likelyCandidates) {
		// Util.print("cacheCandidateDistributions " + facets + " "
		// + likelyCandidates);
		Collection<ItemPredicate> candidates = new HashSet<ItemPredicate>();
		if (likelyCandidates != null)
			candidates.addAll(likelyCandidates);
		candidates.removeAll(facets);
		int[] fCounts = null;

		SortedSet<ItemPredicate> additions = (SortedSet<ItemPredicate>) cacheDAG
				.get(facets);
		if (additions != null) {
			candidates.removeAll(additions);
			fCounts = ensureDist(facets).getCounts();
		} else {
			Distribution d = cachedDistributions.get(facets);
			if (d != null)
				fCounts = d.getCounts();
		}

		// for (Iterator it = cachedDistributions.entrySet().iterator(); it
		// .hasNext()
		// && (candidates.size() > 0 || fCounts == null);) {
		// Map.Entry entry = (Map.Entry) it.next();
		// List cachedFacets = (List) entry.getKey();
		// // Util.print("in cache "+cachedFacets);
		// if (cachedFacets.containsAll(facets)) {
		// candidates.removeAll(cachedFacets);
		// if (fCounts == null) {
		// fCounts = ((Distribution) entry.getValue())
		// .getMarginalCounts(facets);
		// }
		// }
		// }
		if (candidates.size() > 0) {
			try {
				Query query = query(facets);
				ResultSet[] rss = query.onCountMatrix(facets, candidates,
						fCounts == null);
				assert rss.length == 2 : fCounts + " " + rss.length;
				if (fCounts == null) {
					fCounts = getDistFromDB(facets, rss[0]).getCounts();
				}
				int nonZeroStateCount = 0;
				int[] counts = null;
				int prevFacet = -1;
				List<ItemPredicate> allFacets = null;
				int[] facetsIndexes = null;
				int candidateIndex = -1;
				int nFacets = facets.size() + 1;
				ResultSet rs = rss[1];

				// Util.print(MyResultSet.valueOfDeep(rs,
				// MyResultSet.SNMINT_INT_INT, 500));
				// Util.print(Util.valueOfDeep(fCounts));

				while (rs.next()) {
					int facet = rs.getInt(1);
					assert facet > 0 : MyResultSet.valueOfDeep(rs,
							MyResultSet.SNMINT_INT_INT, 300);
					if (facet != prevFacet) {
						if (prevFacet > 0) {
							computeNcache(fCounts, query, nonZeroStateCount,
									counts, prevFacet, allFacets,
									candidateIndex);
						}
						prevFacet = facet;
						nonZeroStateCount = 0;
						counts = new int[1 << nFacets];
						allFacets = new ArrayList<ItemPredicate>(nFacets);
						allFacets.addAll(facets);
						Perspective p = query.findPerspective(facet);
						assert !allFacets.contains(p) : allFacets + " " + p;
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
					assert substate > 0;
					int state = setSubstate(0, substate, facetsIndexes);
					int count = rs.getInt(3);
					// Util.print(state + " " + Util.setBit(state,
					// candidateIndex, true)+" "+count + " "
					// + query.findPerspective(facet));
					assert count <= fCounts[substate] : facets + " "
							+ query.findPerspective(facet) + " "
							+ Util.valueOfDeep(fCounts) + " " + substate + " "
							+ count;
					nonZeroStateCount += count;
					counts[state] = fCounts[substate] - count;
					counts[Util.setBit(state, candidateIndex, true)] = count;
					// Util.print("zz " + facet + " " + state + " " +
					// count);
				}
				rs.close();

				// Util.print(facets + " " + allFacets + " "
				// + Util.valueOfDeep(fCounts) + " "
				// + Util.valueOfDeep(counts));
				// createAndCache(allFacets, counts);

				if (prevFacet > 0)
					computeNcache(fCounts, query, nonZeroStateCount, counts,
							prevFacet, allFacets, candidateIndex);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return ensureDist(facets);
	}

	private static void computeNcache(int[] fCounts, Query query,
			int nonZeroStateCount, int[] counts, int prevFacet,
			List<ItemPredicate> allFacets, int candidateIndex) {
		Perspective p = query.findPerspectiveIfPossible(prevFacet);
		int totalCount2 = p == null ? -1 : p.getTotalCount();
		if (totalCount2 < 0)
			// In case isRestrictedData and p is deeply nested
			getDistFromDB(allFacets, null);
		else {
			int zeroCount = totalCount2 - nonZeroStateCount;
			assert zeroCount >= 0;
			counts[0] = fCounts[0] - zeroCount;
			assert counts[0] >= 0 : p + " " + totalCount2 + "-"
					+ nonZeroStateCount + "=" + zeroCount + "\n"
					+ Util.valueOfDeep(counts) + "\n"
					+ Util.valueOfDeep(fCounts);
			counts[Util.setBit(0, candidateIndex, true)] = zeroCount;
			// Util.print("cd " + fCounts[0] + " "
			// + nonZeroStateCount + " "
			// + p.getTotalCount() + " " + p);

			// Util.print(facets + " " + allFacets + " "
			// + Util.valueOfDeep(fCounts) + " "
			// + Util.valueOfDeep(counts));

			Distribution dist = createAndCache(allFacets, counts);
			assert dist.totalCount == query.getTotalCount() : dist;
		}
	}

	private static Distribution getDistFromDB(List<ItemPredicate> facets,
			ResultSet rs) {
		if (rs == null)
			rs = query(facets).onCountMatrix(facets, null, true)[0];
		int nFacets = facets.size();
		int[] counts1 = new int[1 << nFacets];
		try {
			while (rs.next()) {
				assert rs.getInt(1) == 0;
				int state = rs.getInt(2);
				assert state > 0 : "Too many facets (" + facets.size() + ")? "
						+ facets;
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

	private static List<ItemPredicate> computeFacets(List<ItemPredicate> facets) {
		assert !Util.hasDuplicates(facets.toArray()) : facets;
		List<ItemPredicate> result = new ArrayList<ItemPredicate>(facets);
		Collections.sort(result);
		assert result.equals(facets) : facets;
		return Collections.unmodifiableList(result);
	}

	protected static boolean checkDist(double[] dist) {
		for (int i = 0; i < dist.length; i++) {
			double d = dist[i];
			assert d >= 0 && d <= 1 : Util.valueOfDeep(dist);
		}
		double sum = Util.kahanSum(dist);
		assert Math.abs(sum - 1) < 1e-10 : Util.valueOfDeep(dist);

		// int nFacets = (int) Util.log2(dist.length);
		// int[] indexes = new int[1];
		// for (int i = 0; i < nFacets; i++) {
		// indexes[0] = i;
		// double p = getMarginal(indexes, dist)[1];
		// assert p > 0 : i + " " + Util.valueOfDeep(dist);
		// }

		return true;
	}

	protected List<ItemPredicate> facets() {
		return facets;
	}

	protected int facetIndex(ItemPredicate p2) {
		assert facets.contains(p2) : p2 + " " + facets;
		return facetIndexOrNot(p2);
	}

	protected int facetIndexOrNot(ItemPredicate p2) {
		return facets.indexOf(p2);
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

	protected double[] getMarginal(List<ItemPredicate> marginals) {
		// Util.print("getMarginal " + marginals + " " + facets() + " "
		// + Util.valueOfDeep(distribution));
		assert marginals.size() <= nFacets : marginals + " " + facets;
		assert facets().containsAll(marginals);
		return getMarginal(getMarginIndexes(marginals));
	}

	private double[] getMarginal(int[] indexes) {
		return getMarginal(indexes, getDistribution());
	}

	private static double[] getMarginal(int[] indexes, double[] dist) {
		int nStates = dist.length;
		int nMargFacets = indexes.length;
		int nMargStates = 1 << nMargFacets;
		if (nMargStates == nStates && isSequence(indexes))
			return dist;
		// assert indexes.length < facets.size() :
		// "Differently ordered facets: "
		// + facets + " " + Util.valueOfDeep(indexes);
		double[] result = new double[nMargStates];
		for (int state = 0; state < nStates; state++) {
			// Util.print("getMarginal " + Util.valueOfDeep(indexes) + " " +
			// state
			// + " " + getDistribution()[state] + " "
			// + getSubstate(state, indexes));
			result[getSubstate(state, indexes)] += dist[state];
		}
		assert checkDist(result);
		return result;
	}

	public int[] getMarginalCounts(List<ItemPredicate> marginals) {
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

	public double[] getDistribution() {
		assert checkDist(distribution);
		// assert Math.abs(distribution[0] - 0.469015143) > 0.0000001 : facets
		// + " " + Util.valueOfDeep(distribution);
		return distribution;
	}

	public int[] getCounts() {
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

	public Distribution getConditionalDistribution(
			List<ItemPredicate> falseFacets, List<ItemPredicate> trueFacets) {
		assert !Util.hasDuplicates(falseFacets);
		assert !Util.hasDuplicates(trueFacets);
		assert facets.containsAll(falseFacets);
		assert facets.containsAll(trueFacets);
		assert !(new HashSet<ItemPredicate>(falseFacets)).removeAll(trueFacets);
		List<ItemPredicate> condFacets = new ArrayList<ItemPredicate>(facets);
		condFacets.removeAll(falseFacets);
		condFacets.removeAll(trueFacets);
		int[] condCounts = new int[1 << condFacets.size()];
		getCounts();
		int falseMask = 0;
		int[] falseIndexes = getMarginIndexes(falseFacets);
		for (int i = 0; i < falseIndexes.length; i++) {
			falseMask = Util.setBit(falseMask, falseIndexes[i], true);
		}
		int trueMask = 0;
		int[] trueIndexes = getMarginIndexes(trueFacets);
		for (int i = 0; i < trueIndexes.length; i++) {
			trueMask = Util.setBit(trueMask, trueIndexes[i], true);
		}
		int[] condIndexes = getMarginIndexes(condFacets);
		int total = 0;
		for (int state = 0; state < counts.length; state++) {
			if ((state & falseMask) == 0 && (state & trueMask) == trueMask) {
				condCounts[getSubstate(state, condIndexes)] = counts[state];
				total += counts[state];
			}
		}
		Distribution result = total > 0 ? new Distribution(condFacets,
				condCounts) : null;
		return result;
	}

	protected int[] getMarginIndexes(List<ItemPredicate> marginals) {
		return getMarginIndexes(facets, marginals);
	}

	private static int[] getMarginIndexes(List<ItemPredicate> facets,
			List<ItemPredicate> marginals) {
		int[] result = new int[marginals.size()];
		int index = 0;
		for (Iterator<ItemPredicate> it = marginals.iterator(); it.hasNext();) {
			ItemPredicate facet = it.next();
			assert facets.contains(facet) : facet + " " + facets;
			result[index++] = facets.indexOf(facet);
		}
		return result;
	}

	List<ItemPredicate> getCauses(ItemPredicate caused) {
		List<ItemPredicate> result = new LinkedList<ItemPredicate>(facets());
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

	private static Query query(Collection<ItemPredicate> facets) {
		// We allow facets to be empty, as long as we're not primary
		Query query = ((ItemPredicate) Util.some(facets)).query();
		return query;
	}

	double mutualInformation(List<ItemPredicate> facets1,
			List<ItemPredicate> facets2) {
		assert facets.containsAll(facets1) : facets1 + " " + facets;
		assert facets.containsAll(facets2) : facets2 + " " + facets;
		List<ItemPredicate> allFacets = new LinkedList<ItemPredicate>(facets1);
		allFacets.addAll(facets2);
		assert allFacets.size() == new HashSet<ItemPredicate>(allFacets).size();
		Distribution dist;
		if (allFacets.size() == facets().size()) {
			dist = this;
		} else {
			Collections.sort(allFacets);
			dist = getMarginalDistribution(allFacets);
		}
		Distribution dist1 = getMarginalDistribution(facets1);
		Distribution dist2 = getMarginalDistribution(facets2);
		// Util.print("mutInf " + facets1 + ": " + dist1.entropy() + " " +
		// facets2
		// + ": " + dist2.entropy() + " both: " + dist.entropy());
		return dist1.entropy() + dist2.entropy() - dist.entropy();
	}

	/**
	 * @return the multivariate mutual information of all the facets
	 */
	double mutualInformation() {
		assert nFacets() > 1;

		List<ItemPredicate> first = new ArrayList<ItemPredicate>(1);
		first.add(facets.get(0));
		List<ItemPredicate> rest = new LinkedList<ItemPredicate>(facets);
		rest.remove(0);
		if (facets.size() == 2) {
			return mutualInformation(first, rest);
		} else {
			Distribution cond = getMarginalDistribution(first);
			Distribution restD = getMarginalDistribution(rest);
			Distribution fals = getConditionalDistribution(first, emptyIPlist);
			Distribution tru = getConditionalDistribution(emptyIPlist, first);
			// Util.print("mutInf " + facets1 + ": " + dist1.entropy() + " " +
			// facets2
			// + ": " + dist2.entropy() + " both: " + dist.entropy());

			// see http://en.wikipedia.org/wiki/Multivariate_mutual_information
			return restD.mutualInformation() - cond.distribution[0]
					* fals.mutualInformation() - cond.distribution[1]
					* tru.mutualInformation();
		}
	}

	static final List<ItemPredicate> emptyIPlist = new ArrayList<ItemPredicate>(
			0);

	double conditionalMutualInformation(List<ItemPredicate> facets1,
			List<ItemPredicate> facets2, List<ItemPredicate> condition) {
		// Util.print("cmi " + facets1 + " " + facets2 + " " + condition);
		if (condition.isEmpty())
			return mutualInformation(facets1, facets2);
		assert facets.containsAll(facets1) : facets1 + " " + facets;
		assert facets.containsAll(facets2) : facets2 + " " + facets;
		assert facets.containsAll(condition) : condition + " " + facets;
		List<ItemPredicate> allFacets = new LinkedList<ItemPredicate>(facets1);
		allFacets.addAll(facets2);
		allFacets.addAll(condition);
		assert allFacets.size() == new HashSet<ItemPredicate>(allFacets).size();
		Distribution all;
		Collections.sort(allFacets);
		if (allFacets.size() == facets().size()) {
			all = this;
		} else {
			all = getMarginalDistribution(allFacets);
		}
		Distribution cond = getMarginalDistribution(condition);
		LinkedList<ItemPredicate> temp1 = new LinkedList<ItemPredicate>(
				allFacets);
		temp1.removeAll(facets1);
		Distribution no1 = getMarginalDistribution(temp1);
		LinkedList<ItemPredicate> temp2 = new LinkedList<ItemPredicate>(
				allFacets);
		temp2.removeAll(facets2);
		Distribution no2 = getMarginalDistribution(temp2);
		// Util.print("mutInf " + facets1 + ": " + dist1.entropy() + " " +
		// facets2
		// + ": " + dist2.entropy() + " both: " + dist.entropy());

		// see http://en.wikipedia.org/wiki/Conditional_mutual_information
		return no1.entropy() + no2.entropy() - all.entropy() - cond.entropy();
	}

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

	double correlationHacked(Explanation nullModel) {
		List<ItemPredicate> prim = nullModel.facets();
		List<ItemPredicate> nonPrim = new LinkedList<ItemPredicate>(facets());
		nonPrim.removeAll(prim);
		if (nonPrim.size() != 1)
			return Double.NaN;
		ItemPredicate candidate = nonPrim.get(0);

		double sum = 0;
		double sumSq = 0;
		int n = 0;
		for (Iterator<ItemPredicate> it = prim.iterator(); it.hasNext();) {
			ItemPredicate p = it.next();
			double corr = Math.abs(getChiSq(p, candidate).correlation());
			sum += corr;
			sumSq += corr * corr;
			n++;
		}
		double result = sum * sum - sumSq;
		return result;
	}

	ChiSq2x2 getChiSq(ItemPredicate marginal1, ItemPredicate marginal2) {
		List<ItemPredicate> marginals = new ArrayList<ItemPredicate>(2);
		marginals.add(marginal1);
		marginals.add(marginal2);
		int[] marginal = getMarginalCounts(marginals);
		ChiSq2x2 result = ChiSq2x2.getInstance(null, totalCount, marginal[0]
				+ marginal[1], marginal[0] + marginal[2], marginal[0], this);
		return result;
	}

	double entropy() {
		double result = 0;
		int nStates = nStates();
		for (int state = 0; state < nStates; state++) {
			result -= ChiSq2x2.nLogn(distribution[state]);
		}
		return result;
	}

	/**
	 * Can't use Perspective.stdDev, because deeply nested facets have
	 * totalCount = -1
	 * 
	 * @return standard deviation of binary variable p, over the whole database
	 */
	protected static double stdDev(ItemPredicate p) {
		List<ItemPredicate> facets1 = new ArrayList<ItemPredicate>(1);
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

	public List<ItemPredicate> getFacets() {
		return facets;
	}

	protected ItemPredicate getFacet(int i) {
		return facets.get(i);
	}

	@Override
	public String toString() {
		return "<" + Util.shortClassName(this) + " " + facets + " "
				+ Util.valueOfDeep(getCounts()) + ">";
	}

	public int getTotalCount() {
		return totalCount;
	}

}
