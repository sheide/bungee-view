package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;

public class FacetSelection extends GreedySubset {
	private Map explanations = new HashMap();
	private final Explanation nullModel;
	private final Distribution maxModel;

	static Explanation selectFacets(Explanation nullModel,
			Distribution maxModel, double threshold) {
		FacetSelection search = new FacetSelection(nullModel, maxModel,
				threshold);
		Set addedFacets = search.selectVariables();
		// Util.print("FacetSelection => " + addedFacets);
		return search.lookupExplanation(addedFacets, nullModel);
	}

	FacetSelection(Explanation nullModel, Distribution maxModel,
			double threshold) {
		super(threshold, Explanation.candidateFacets(nullModel.facets(), true));
		this.maxModel = maxModel;
		this.nullModel = nullModel;
	}

	// double eval() {
	// Explanation explanation = explanation0
	// .getAlternateExplanation(currentGuess,null,null);
	// Util.print("\neval " + explanation);
	// return explanation.improvement(explanation0.nullModel);
	// return 0;
	// }

	double improvement(Object toggledFacet) {
		Explanation previous = lookupExplanation(previousGuess(toggledFacet),
				nullModel);
		Explanation current = lookupExplanation(currentGuess, previous);
		double result = isAdding(toggledFacet) ? previous.improvement(current)
				: -current.improvement(previous);
		return result;
	}

	List facetList(Collection addedFacets) {
		List result = new LinkedList(nullModel.facets());
		result.addAll(addedFacets);
		Collections.sort(result);
		return result;
	}

	Explanation lookupExplanation(Set addedFacets, Explanation base) {
		List facetList = facetList(addedFacets);
		Explanation result = (Explanation) explanations.get(facetList);
		if (result == null) {
			if (base == null)
				base = nullModel;
			result = base.getAlternateExplanation(facetList, maxModel);
			explanations.put(facetList, result);
		}
		return result;
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		lookupExplanation(currentGuess, null).printToFile();
	}
}
