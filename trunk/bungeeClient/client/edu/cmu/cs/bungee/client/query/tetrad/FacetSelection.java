package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.javaExtensions.Util;

public class FacetSelection extends GreedySubset<ItemPredicate> {
	private final Map<List<ItemPredicate>,Explanation> explanations = new HashMap<List<ItemPredicate>,Explanation>();
	private final Explanation nullModel;
	private final double edgeThreshold;
	private final List<ItemPredicate> candidates;

	protected static Explanation selectFacets(Explanation nullModel,
			List<ItemPredicate> candidates, double threshold) {
		FacetSelection search = new FacetSelection(nullModel, candidates,
				threshold);

		return search.selectFacetsInternal();
	}

	private Explanation selectFacetsInternal() {
		cacheCandidates();
		Set<ItemPredicate> addedFacets = selectVariables();
		Explanation result = lookupExplanation(addedFacets, nullModel);
		// Util.print("gggg");
		// result.printGraph(false);

		return result;
	}

	private FacetSelection(Explanation nullModel, List<ItemPredicate> candidates,
			double edgeThreshold) {
		super(edgeThreshold * 2 /* nullModel.nFacets() */, candidates,
				GreedySubset.ADD);
//		Util.print("FacetSelection " + nullModel);
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

	@Override
	double improvement(ItemPredicate toggledFacet, double threshold1) {
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

		// Util.print("FS.improv "+toggledFacet+" KL="+current.klDivergence());
		// larger.printTable((ItemPredicate) toggledFacet);

		if (Explanation.PRINT_CANDIDATES_TO_FILE)
			current.printToFile(nullModel);
		return result;
	}

	private List<ItemPredicate> facetList(Collection<ItemPredicate> addedFacets) {
		List<ItemPredicate> result = new LinkedList<ItemPredicate>(nullModel.facets());
		result.addAll(addedFacets);
		Collections.sort(result);
		return result;
	}

	Explanation lookupExplanation(Set<ItemPredicate> addedFacets, Explanation base) {
		List<ItemPredicate> allFacetList = facetList(addedFacets);
		Explanation result = explanations.get(allFacetList);
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

	@Override
	protected void newBest(ItemPredicate candidate) {
		super.newBest(candidate);
		if (Explanation.PRINT_LEVEL >= Explanation.IMPROVEMENT)
			Util
					.print("Best candidate rank = "
							+ candidates.indexOf(candidate));
		cacheCandidates();
		Explanation best = lookupExplanation(currentGuess, null);
		if (Explanation.PRINT_CANDIDATES_TO_FILE)
			best.printToFile(nullModel);
		if (Explanation.PRINT_LEVEL >= Explanation.GRAPH) {
			best.printGraph();
		}
	}
}
