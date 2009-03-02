package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;

public class FacetSelection extends GreedySubset {
	private final Map explanations = new HashMap();
	private final Explanation nullModel;
	private final double edgeThreshold;
	private final List candidates;

	static Explanation selectFacets(Explanation nullModel, double threshold) {
		FacetSelection search = new FacetSelection(nullModel, Explanation
				.candidateFacets(nullModel.facets(), true), threshold);

		search.cacheCandidates();
		Set addedFacets = search.selectVariables();
		// Util.print("FacetSelection => " + addedFacets);
		Explanation result = search.lookupExplanation(addedFacets, nullModel);
//		Util.print("gggg");
//		result.printGraph(false);

		return result;
	}

	FacetSelection(Explanation nullModel, List candidates, double edgeThreshold) {
		super(edgeThreshold * nullModel.nFacets(), candidates, GreedySubset.ADD);
		this.candidates = Collections.unmodifiableList(candidates);
		this.nullModel = nullModel;
		this.edgeThreshold = edgeThreshold;
	}

	// double eval() {
	// Explanation explanation = explanation0
	// .getAlternateExplanation(currentGuess,null,null);
	// Util.print("\neval " + explanation);
	// return explanation.improvement(explanation0.nullModel);
	// return 0;
	// }

	double improvement(Object toggledFacet, double threshold1) {
		Explanation previous = lookupExplanation(previousGuess(toggledFacet),
				nullModel);
		Explanation current = lookupExplanation(currentGuess, previous);
		boolean isAdding = isAdding(toggledFacet);
		Explanation smaller = isAdding ? previous : current;
		Explanation larger = isAdding ? current : previous;
		threshold = edgeThreshold * smaller.nFacets();
//		current.printGraph(false);
		double result = (isAdding ? 1 : -1)
				* smaller.improvement(larger, threshold1);

//		double weightSpaceChange = larger.predicted.weightSpaceChange(
//				smaller.predicted, smaller.primaryFacets(), smaller.observed, larger.observed,
//				true);
//		double[] correlations = new double[nullModel.primaryFacets().size()];
//		int corrIndex = 0;
//		Perspective nonPrim = (Perspective) toggledFacet;
//		for (Iterator it = nullModel.primaryFacets().iterator(); it.hasNext();) {
//			Perspective prim = (Perspective) it.next();
//			assert (prim != nonPrim);
//			correlations[corrIndex++] = larger.observed.getChiSq(nonPrim, prim)
//					.correlation();
//		}
//		List toggledFacets = new ArrayList(1);
//		toggledFacets.add(toggledFacet);
//		Util.print(toggledFacet + ", " + weightSpaceChange
//
//		+ ", correlations, " + Util.join(correlations)
//
//		// +", mutInf, "
//				// + larger.observed.mutualInformation(toggledFacets, smaller
//				// .facets())
//
//				);
		
//current.printToFile();
		return result;
	}

	List facetList(Collection addedFacets) {
		List result = new LinkedList(nullModel.facets());
		result.addAll(addedFacets);
		Collections.sort(result);
		return result;
	}

	Explanation lookupExplanation(Set addedFacets, Explanation base) {
		List allFacetList = Collections.unmodifiableList(facetList(addedFacets));
		Explanation result = (Explanation) explanations.get(allFacetList);
		if (result == null) {
			if (base == null)
				base = nullModel;
			result = base.getAlternateExplanation(allFacetList, candidates);
			explanations.put(allFacetList, result);
		}
		return result;
	}

	void cacheCandidates() {
		assert candidates.size() > 0;
		Distribution.cacheCandidates(facetList(currentGuess), candidates);
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		cacheCandidates();
//		lookupExplanation(currentGuess, null).printToFile();
		// lookupExplanation(currentGuess, null).printGraph();
	}
}
