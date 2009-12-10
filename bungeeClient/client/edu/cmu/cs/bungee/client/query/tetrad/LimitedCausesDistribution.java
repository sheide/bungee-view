package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;

class LimitedCausesDistribution extends Distribution {

	private final ArrayList<List<ItemPredicate>> causes;

	protected static Distribution getInstance(List<ItemPredicate> facets,
			int[] counts, Set<List<ItemPredicate>> edges) {
		Distribution result;
		boolean fullyConnected = edges.size() < facets.size()
				* (facets.size() - 1) / 2;
		if (fullyConnected) {
			result = Distribution.ensureDist(facets, counts);
		} else {
			CacheDistArgs arg = new CacheDistArgs(facets, edges);
			result = cachedDistributions.get(arg);
			if (result == null) {
				result = new LimitedCausesDistribution(facets, counts, edges);
				cachedDistributions.put(arg, result);
			}
		}
		return result;
	}

	private LimitedCausesDistribution(List<ItemPredicate> facets, int[] counts,
			Set<List<ItemPredicate>> edges) {
		super(facets, counts);
		// int nFacets = nFacets();
		causes = new ArrayList<List<ItemPredicate>>(nFacets);
		for (int i = 0; i < nFacets; i++) {
			Perspective caused = (Perspective) facets.get(i);
			List<ItemPredicate> causs = new LinkedList<ItemPredicate>();
			for (Iterator<List<ItemPredicate>> it = edges.iterator(); it
					.hasNext();) {
				List<ItemPredicate> edge = it.next();
				if (edge.get(0) == caused) {
					causs.add(edge.get(1));
				} else if (edge.get(1) == caused) {
					causs.add(edge.get(0));
				}
			}
			causes.set(i, Collections.unmodifiableList(causs));
			// assert causes[i].size() > 0 : facets + " " + edges;
		}
		assert checkCauses() : Util.valueOfDeep(causes) + " " + edges;
	}

	@Override
	public Distribution getMarginalDistribution(List<ItemPredicate> subFacets) {
		if (subFacets.equals(facets))
			return this;
		Set<List<ItemPredicate>> subedges = getEdgesAmong(subFacets);
		return getInstance(subFacets, getMarginalCounts(subFacets), subedges);
	}

	private Set<List<ItemPredicate>> getEdgesAmong(List<ItemPredicate> subFacets) {
		HashSet<List<ItemPredicate>> result = new HashSet<List<ItemPredicate>>();
		for (Iterator<ItemPredicate> it = subFacets.iterator(); it.hasNext();) {
			ItemPredicate caused = it.next();
			for (Iterator<ItemPredicate> it2 = getCauses(caused).iterator(); it2
					.hasNext();) {
				ItemPredicate cause = it2.next();
				if (subFacets.contains(cause) && cause.compareTo(caused) < 0) {
					List<ItemPredicate> edgeList = new ArrayList<ItemPredicate>(
							2);
					edgeList.add(cause);
					edgeList.add(caused);
					result.add(edgeList);
				}
			}
		}
		// Util.print("getEdges " + result);
		// assert result.size() > 0 : subFacets + " " + this;
		return result;
	}

	protected List<ItemPredicate> getCauses(Perspective caused) {
		assert checkCauses() : this;
		return causes.get(facetIndex(caused));
	}

	private boolean checkCauses() {
		for (Iterator<ItemPredicate> it1 = facets.iterator(); it1.hasNext();) {
			Perspective caused = (Perspective) it1.next();
			for (Iterator<ItemPredicate> it = causes.get(facetIndex(caused))
					.iterator(); it.hasNext();) {
				ItemPredicate cause = it.next();
				if (!causes.get(facetIndex(cause)).contains(caused))
					return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "<" + Util.shortClassName(this) + " " + facets + " "
				+ Util.valueOfDeep(getCounts()) + " nEdges=" + nEdges() + "\n"
				+ causesDesc() + ">";
	}

	private int nEdges() {
		return getEdgesAmong(facets).size();
	}

	private String causesDesc() {
		StringBuffer buf = new StringBuffer();
		for (Iterator<ItemPredicate> it1 = facets.iterator(); it1.hasNext();) {
			Perspective caused = (Perspective) it1.next();
			buf.append(caused).append(" <= ").append(getCauses(caused)).append(
					"\n");
		}
		return buf.toString();
	}

}
