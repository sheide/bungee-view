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

		return selectFacetsInternal(nullModel, search);
	}

	// protected static Explanation selectFacets(Explanation nullModel,
	// List candidates, int nFacets) {
	// FacetSelection search = new FacetSelection(nullModel, candidates,
	// -nFacets - 1);
	// // Util.print("sf " + nFacets);
	// return selectFacetsInternal(nullModel, search);
	// }

	private static Explanation selectFacetsInternal(Explanation nullModel,
			FacetSelection search) {
		search.cacheCandidates();
		Set addedFacets = search.selectVariables();
		Explanation result = search.lookupExplanation(addedFacets, nullModel);
		// Util.print("gggg");
		// result.printGraph(false);

		return result;
	}

	private FacetSelection(Explanation nullModel, List candidates,
			double edgeThreshold) {
		super(edgeThreshold * 2 /* nullModel.nFacets() */, candidates,
				GreedySubset.ADD);
		this.candidates = Collections.unmodifiableList(candidates);
		this.nullModel = nullModel;
		this.edgeThreshold = edgeThreshold;
	}

	private double computeThreshold() {
		if (edgeThreshold >= 0)
			return edgeThreshold * 2;// smaller.nFacets();
		else if (currentGuess.size() <= -edgeThreshold - 1)
			return 0;
		else
			return Double.POSITIVE_INFINITY;
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
		threshold = computeThreshold();
		// current.printGraph(false);
		// assert larger.parentModel == smaller : "\n" + larger + "\n"
		// + larger.parentModel + "\n" + smaller;
		double result = (isAdding ? 1 : -1)
				* larger.improvement(smaller, threshold1, nullModel.facets());

		// Util.print("FS.improv "+toggledFacet);larger.printTable((Perspective)
		// toggledFacet);

		if (Explanation.PRINT_CANDIDATES_TO_FILE)
			current.printToFile(nullModel);
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

		// List cands = new ArrayList(1);
		// cands.add(candidates.get(0));
		// ArrayList guess = new ArrayList(currentGuess);
		// Collections.sort(guess);
		// for (Iterator it = candidates.iterator(); it.hasNext();) {
		// Perspective cand = (Perspective) it.next();
		// if (!guess.contains(cand)) {
		// cands.set(0, cand);
		// Set added = new HashSet(currentGuess);
		// added.add(cand);
		// List allFacetList = facetList(added);
		// List primary = nullModel.facets();
		// Distribution all = Distribution.ensureDist(allFacetList);
		// Util.print("mut inf "
		// + all.conditionalMutualInformation(primary, cands,
		// guess) + " " + cand);
		// }
		// }

	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		if (Explanation.PRINT_LEVEL > 0)
			Util
					.print("Best candidate rank = "
							+ candidates.indexOf(candidate));
		cacheCandidates();
		if (Explanation.PRINT_LEVEL > 1) {
			Explanation best = lookupExplanation(currentGuess, null);
			best.printGraph();
			best.printToFile(nullModel);
		}
	}
}
