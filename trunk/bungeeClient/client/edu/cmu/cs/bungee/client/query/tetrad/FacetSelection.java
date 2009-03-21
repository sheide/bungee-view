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

	protected static Explanation selectFacets(Explanation nullModel,
			List candidates, double threshold) {
		FacetSelection search = new FacetSelection(nullModel, candidates,
				threshold);

		search.cacheCandidates();
		Set addedFacets = search.selectVariables();
		if (Explanation.PRINT_LEVEL > 0)
			Util.print("FacetSelection => " + addedFacets);
		Explanation result = search.lookupExplanation(addedFacets, nullModel);
		// Util.print("gggg");
		// result.printGraph(false);

		return result;
	}

	private FacetSelection(Explanation nullModel, List candidates,
			double edgeThreshold) {
		super(edgeThreshold * nullModel.nFacets(), candidates, GreedySubset.ADD);
		this.candidates = Collections.unmodifiableList(candidates);
		this.nullModel = nullModel;
		this.edgeThreshold = edgeThreshold;
	}

	double improvement(Object toggledFacet, double threshold1) {
		Explanation previous = lookupExplanation(previousGuess(toggledFacet),
				nullModel);
		Explanation current = lookupExplanation(currentGuess, previous);
		boolean isAdding = isAdding(toggledFacet);
		Explanation smaller = isAdding ? previous : current;
		Explanation larger = isAdding ? current : previous;
		if (larger.nFacets() >= 7)
			return 0;
		threshold = edgeThreshold * smaller.nFacets();
		// current.printGraph(false);
		assert larger.parentModel == smaller : "\n" + larger + "\n"
				+ larger.parentModel + "\n" + smaller;
		double result = (isAdding ? 1 : -1)
				* larger.improvement(smaller, threshold1);

		// current.printToFile();
		return result;
	}

	private List facetList(Collection addedFacets) {
		List result = new LinkedList(nullModel.facets());
		result.addAll(addedFacets);
		Collections.sort(result);
		return result;
	}

	private Explanation lookupExplanation(Set addedFacets, Explanation base) {
		List allFacetList = facetList(addedFacets);
		Explanation result = (Explanation) explanations.get(allFacetList);
		if (result == null) {
			if (base == null)
				base = nullModel;
			result = base.getAlternateExplanation(allFacetList);
			explanations.put(result.facets(), result);
		}
		return result;
	}

	private void cacheCandidates() {
		assert candidates.size() > 0;
		Distribution.cacheCandidateDistributions(facetList(currentGuess),
				candidates);
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		cacheCandidates();
		if (Explanation.PRINT_LEVEL > 1) {
			Explanation best = lookupExplanation(currentGuess, null);
			best.printGraph();
			best.printToFile();
		}
	}
}
