package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.tetrad.GraphicalModel.SimpleEdge;

public class EdgeSelection extends GreedySubset<SimpleEdge> {

	private final Map<Set<SimpleEdge>, Explanation> explanations = new HashMap<Set<SimpleEdge>, Explanation>();
	private final Set<SimpleEdge> committedEdges;
	private final Explanation maxModel;
	private final Explanation nullModel;

	protected static Explanation selectEdges(Explanation explanation,
			double threshold, Explanation nullModel) {
		if (explanation.predicted.nEdges() == 0)
			return explanation;
		/**
		 * @param isCoreEdges
		 *            Once core edges are removed, removing others will have no
		 *            effect. So first remove any non-core edges you can, and
		 *            then remove only core edges.
		 */
		Set<SimpleEdge> startCoreEdges = candidateEdges(explanation, true, nullModel);
		Set<SimpleEdge> startNonCoreEdges = candidateEdges(explanation, false, nullModel);

		Set<SimpleEdge> nonCoreEdges = startNonCoreEdges;
		Explanation intermediateExplanation = explanation;
		if (startNonCoreEdges.size() > 0) {
			EdgeSelection nonCoreSearch = new EdgeSelection(threshold,
					startNonCoreEdges, startCoreEdges, explanation, nullModel);
			nonCoreEdges = nonCoreSearch.selectVariables();
			// if (Explanation.PRINT_LEVEL > 0)
			// Util.print("non-core EdgeSelection => " + nonCoreEdges.size() +
			// " "
			// + nonCoreEdges);
			intermediateExplanation = nonCoreSearch.lookupExplanation(
					nonCoreEdges, explanation);
		}

		EdgeSelection coreSearch = new EdgeSelection(threshold, startCoreEdges,
				nonCoreEdges, intermediateExplanation, nullModel);
		Set<SimpleEdge> coreEdges = coreSearch.selectVariables();
		// if (Explanation.PRINT_LEVEL > 0)
		// Util.print("core EdgeSelection => " + coreEdges.size() + " "
		// + coreEdges);
		return coreSearch.lookupExplanation(coreEdges, intermediateExplanation);
	}

	private EdgeSelection(double threshold, Set<SimpleEdge> candidateEdges,
			Set<SimpleEdge> committedEdges, Explanation base, Explanation nullModel) {
		super(threshold, candidateEdges, GreedySubset.REMOVE);
		addAllVariables();
		this.committedEdges = Collections.unmodifiableSet(committedEdges);
		this.maxModel = base;
		this.nullModel = nullModel;
		// base.predicted.edgesFixed = true;
		cacheExplanation(allEdges(candidateEdges), base);
		// explanations.put(allEdges(candidateEdges), base);
		// Util.print("EdgeSelection " + base + " " + threshold);
		// base.printGraph();
	}

	private static Set<SimpleEdge> candidateEdges(Explanation explanation,
			boolean isCoreEdges, Explanation nullModel) {
		List<ItemPredicate> primaryFacets = nullModel.facets();
		Set<SimpleEdge> result = explanation.predicted.getEdges(false);
		for (Iterator<SimpleEdge> it = result.iterator(); it.hasNext();) {
			SimpleEdge candidateEdge = it.next();
			if (isCoreEdges != (primaryFacets.contains(candidateEdge.p1) && primaryFacets
					.contains(candidateEdge.p2))) {
				it.remove();
			}
		}
		return result;
	}

	@Override
	protected double improvement(SimpleEdge toggledEdge, double threshold1) {
		Explanation previous = lookupExplanation(previousGuess(toggledEdge),
				null);
		Explanation current = lookupExplanation(currentGuess, previous);
		// Util.print("es.improv "+toggledEdge+" "+current+" "+previous);
		if (Explanation.PRINT_CANDIDATES_TO_FILE)
			current.printToFile(nullModel);
		double result = -current.improvement(previous, threshold1, nullModel
				.facets());

		return result;
	}

	private Set<SimpleEdge> allEdges(Collection<SimpleEdge> addedEdges) {
		Set<SimpleEdge> result = new HashSet<SimpleEdge>(committedEdges);
		result.addAll(addedEdges);
		return result;
	}

	private Explanation lookupExplanation(Set<SimpleEdge> addedEdges, Explanation base) {
		Set<SimpleEdge> allEdges = Collections.unmodifiableSet(allEdges(addedEdges));
		Explanation result = edges2Explanation(allEdges);
		if (result == null) {
			if (base == null)
				base = maxModel;
			result = base.getAlternateExplanation(allEdges);
			// result.printToFile();
			cacheExplanation(allEdges, result);
		}
		return result;
	}

	private void cacheExplanation(Set<SimpleEdge> edges, Explanation expl) {
		assert edges.size() == expl.predicted.nEdges();
		// expl.predicted.edgesFixed = true;
//		List<Object> args = new ArrayList<Object>(3);
//		args.add(expl);
//		args.add(expl.toString());
//		args.add(edges.size() + "");
		explanations.put(Collections.unmodifiableSet(edges), expl);

	}

	private Explanation edges2Explanation(Set<SimpleEdge> edges) {
		Explanation result = explanations.get(edges);
//		Explanation result = args == null ? null : (Explanation) args.get(0);
		assert result == null || edges.size() == result.predicted.nEdges() : result
				+ " " + edges.size();
		return result;
	}

	@Override
	protected void newBest(SimpleEdge candidate) {
		super.newBest(candidate);
		Explanation best = lookupExplanation(currentGuess, null);
		if (Explanation.PRINT_CANDIDATES_TO_FILE)
			best.printToFile(nullModel);
		if (Explanation.PRINT_LEVEL >= Explanation.GRAPH) {
			best.printGraph();
		}
	}
}
