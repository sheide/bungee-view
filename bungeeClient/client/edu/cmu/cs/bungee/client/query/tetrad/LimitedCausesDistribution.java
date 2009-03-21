package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;

class LimitedCausesDistribution extends Distribution {

	private final List[] causes;

	protected static Distribution getInstance(List facets, int[] counts,
			Set edges) {
		Distribution result;
		boolean fullyConnected = edges.size() < facets.size()
				* (facets.size() - 1) / 2;
		if (fullyConnected) {
			result = Distribution.ensureDist(facets, counts);
		} else {
			List args = new ArrayList(2);
			args.add(Collections.unmodifiableList(facets));
			args.add(Collections.unmodifiableSet(edges));
			result = (Distribution) cachedDistributions.get(args);
			if (result == null) {
				result = new LimitedCausesDistribution(facets, counts, edges);
				cachedDistributions.put(args,
						result);
			}
		}
		return result;
	}

	private LimitedCausesDistribution(List facets, int[] counts, Set edges) {
		super(facets, counts);
//		int nFacets = nFacets();
		causes = new List[nFacets];
		for (int i = 0; i < nFacets; i++) {
			Perspective caused = (Perspective) facets.get(i);
			List causs = new LinkedList();
			for (Iterator it = edges.iterator(); it.hasNext();) {
				List edge = (List) it.next();
				if (edge.get(0) == caused) {
					causs.add(edge.get(1));
				} else if (edge.get(1) == caused) {
					causs.add(edge.get(0));
				}
			}
			causes[i] = Collections.unmodifiableList(causs);
			// assert causes[i].size() > 0 : facets + " " + edges;
		}
		assert checkCauses() : Util.valueOfDeep(causes) + " " + edges;
	}

	protected Distribution getMarginalDistribution(List subFacets) {
		if (subFacets.equals(facets))
			return this;
		Set subedges = getEdgesAmong(subFacets);
		return getInstance(subFacets, getMarginalCounts(subFacets), subedges);
	}

	private Set getEdgesAmong(List subFacets) {
		HashSet result = new HashSet();
		for (Iterator it = subFacets.iterator(); it.hasNext();) {
			Perspective caused = (Perspective) it.next();
			for (Iterator it2 = getCauses(caused).iterator(); it2.hasNext();) {
				Perspective cause = (Perspective) it2.next();
				if (subFacets.contains(cause) && cause.compareTo(caused) < 0) {
					List edgeList = new ArrayList(2);
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

	protected List getCauses(Perspective caused) {
		assert checkCauses() : this;
		return causes[facetIndex(caused)];
	}

	private boolean checkCauses() {
		for (Iterator it1 = facets.iterator(); it1.hasNext();) {
			Perspective caused = (Perspective) it1.next();
			for (Iterator it = causes[facetIndex(caused)].iterator(); it
					.hasNext();) {
				Perspective cause = (Perspective) it.next();
				if (!causes[facetIndex(cause)].contains(caused))
					return false;
			}
		}
		return true;
	}

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
		for (Iterator it1 = facets.iterator(); it1.hasNext();) {
			Perspective caused = (Perspective) it1.next();
			buf.append(caused).append(" <= ").append(getCauses(caused)).append(
					"\n");
		}
		return buf.toString();
	}

}
