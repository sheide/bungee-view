package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;

public class EdgeSelection extends GreedySubset {

	private final Map explanations = new HashMap();
	private final Set committedEdges;
	private final Explanation maxModel;
	private final Explanation nullModel;

	protected static Explanation selectEdges(Explanation explanation,
			double threshold,Explanation nullModel) {
		/**
		 * @param isCoreEdges
		 *            Once core edges are removed, removing others will have no
		 *            effect. So first remove any non-core edges you can, and
		 *            then remove only core edges.
		 */
		Set startCoreEdges = candidateEdges(explanation, true, nullModel);
		Set startNonCoreEdges = candidateEdges(explanation, false, nullModel);
		EdgeSelection nonCoreSearch = new EdgeSelection(threshold,
				startNonCoreEdges, startCoreEdges, explanation, nullModel);
		Set nonCoreEdges = nonCoreSearch.selectVariables();
//		if (Explanation.PRINT_LEVEL > 0)
//			Util.print("non-core EdgeSelection => " + nonCoreEdges.size() + " "
//					+ nonCoreEdges);
		Explanation intermediateExplanation = nonCoreSearch.lookupExplanation(
				nonCoreEdges, explanation);
		EdgeSelection coreSearch = new EdgeSelection(threshold, startCoreEdges,
				nonCoreEdges, intermediateExplanation, nullModel);
		Set coreEdges = coreSearch.selectVariables();
//		if (Explanation.PRINT_LEVEL > 0)
//			Util.print("core EdgeSelection => " + coreEdges.size() + " "
//					+ coreEdges);
		return coreSearch.lookupExplanation(coreEdges, intermediateExplanation);
	}

	private EdgeSelection(double threshold, Set candidateEdges,
			Set committedEdges, Explanation base,Explanation nullModel) {
		super(threshold, candidateEdges, GreedySubset.REMOVE);
		addAllVariables();
		this.committedEdges = Collections.unmodifiableSet(committedEdges);
		this.maxModel = base;
		this.nullModel=nullModel;
		// base.predicted.edgesFixed = true;
		cacheExplanation(allEdges(candidateEdges), base);
		// explanations.put(allEdges(candidateEdges), base);
		// Util.print("EdgeSelection " + base + " " + threshold);
		// base.printGraph();
	}

	private static Set candidateEdges(Explanation explanation,
			boolean isCoreEdges, Explanation nullModel) {
		List primaryFacets = nullModel.facets();
		Set result = explanation.predicted.getEdges(false);
		for (Iterator it = result.iterator(); it.hasNext();) {
			List candidateEdge = (List) it.next();
			if (isCoreEdges != (primaryFacets.containsAll(candidateEdge))) {
				it.remove();
			}
		}
		return result;
	}

	protected double improvement(Object toggledEdge, double threshold1) {
		Explanation previous = lookupExplanation(previousGuess(toggledEdge),
				null);
		Explanation current = lookupExplanation(currentGuess, previous);
		// Util.print("es.improv "+toggledEdge+" "+current+" "+previous);
		if(Explanation.PRINT_CANDIDATES_TO_FILE)
		 current.printToFile(nullModel);
		double result = -current.improvement(previous, threshold1, nullModel.facets());

		return result;
	}

	private Set allEdges(Collection addedEdges) {
		Set result = new HashSet(committedEdges);
		result.addAll(addedEdges);
		return result;
	}

	private Explanation lookupExplanation(Set addedEdges, Explanation base) {
		Set allEdges = Collections.unmodifiableSet(allEdges(addedEdges));
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

	private void cacheExplanation(Set edges, Explanation expl) {
		assert edges.size() == expl.predicted.nEdges();
		// expl.predicted.edgesFixed = true;
		List args = new ArrayList(3);
		args.add(expl);
		args.add(expl.toString());
		args.add(edges.size() + "");
		explanations.put(Collections.unmodifiableSet(edges), args);

	}

	private Explanation edges2Explanation(Set edges) {
		List args = (List) explanations.get(edges);
		Explanation result = args == null ? null : (Explanation) args.get(0);
		assert result == null || edges.size() == result.predicted.nEdges() : args
				+ " " + edges.size();
		return result;
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		if (Explanation.PRINT_LEVEL > 1) {
			Explanation best = lookupExplanation(currentGuess, null);
			best.printGraph();
			best.printToFile(nullModel);
		}
	}
}
