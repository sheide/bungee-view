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

	private Map explanations = new HashMap();
	private Set committedEdges;
	private Explanation maxModel;

	static Explanation selectEdges(Explanation explanation, double threshold) {
		/**
		 * @param isCoreEdges
		 *            Once core edges are removed, removing others will have no
		 *            effect. So first remove any non-core edges you can, and
		 *            then remove only core edges.
		 */
		Set startCoreEdges = candidateEdges(explanation, true);
		Set startNonCoreEdges = candidateEdges(explanation, false);
		EdgeSelection nonCoreSearch = new EdgeSelection(threshold,
				startNonCoreEdges, startCoreEdges, explanation);
		Set nonCoreEdges = nonCoreSearch.selectVariables();
//		Util.print("EdgeSelection nc => " + nonCoreEdges.size() + " "
//				+ nonCoreEdges);
		Explanation intermediateExplanation = nonCoreSearch.lookupExplanation(
				nonCoreEdges, explanation);
		EdgeSelection coreSearch = new EdgeSelection(threshold, startCoreEdges,
				nonCoreEdges, intermediateExplanation);
		Set coreEdges = coreSearch.selectVariables();
//		Util.print("EdgeSelection => " + coreEdges.size() + " " + coreEdges);
		return coreSearch.lookupExplanation(coreEdges, intermediateExplanation);
	}

	EdgeSelection(double threshold, Set candidateEdges, Set committedEdges,
			Explanation base) {
		super(threshold, candidateEdges, GreedySubset.REMOVE);
		addAllVariables();
		this.committedEdges = Collections.unmodifiableSet(committedEdges);
		this.maxModel = base;
		base.predicted.edgesFixed = true;
		cacheExplanation(allEdges(candidateEdges), base);
		// explanations.put(allEdges(candidateEdges), base);
		// Util.print("EdgeSelection " + base + " " + threshold);
		// base.printGraph();
	}

	static Set candidateEdges(Explanation explanation, boolean isCoreEdges) {
		List primaryFacets = explanation.primaryFacets();
		Set result = explanation.predicted.getEdges();
		for (Iterator it = result.iterator(); it.hasNext();) {
			List candidateEdge = (List) it.next();
			if (isCoreEdges != (primaryFacets.containsAll(candidateEdge))) {
				it.remove();
			}
		}
		return result;
	}

	// void add(Object candidate) {
	// // Util.print("EdgeSelection.add " + candidate);
	// explanation0.predicted.addEdge((Perspective) ((List) candidate).get(0),
	// (Perspective) ((List) candidate).get(1));
	// super.add(candidate);
	// }
	//
	// void remove(Object candidate) {
	// // Util.print("EdgeSelection.remove " + candidate);
	// explanation0.predicted.removeEdge((Perspective) ((List) candidate)
	// .get(0), (Perspective) ((List) candidate).get(1));
	// super.remove(candidate);
	// }

	// double eval() {
	// return explanation0.predicted.weightSpaceChange(explanation0.predicted,
	// explanation0.nullModel.primaryFacets());
	// }

	double improvement(Object toggledEdge, double threshold1) {
		Explanation previous = lookupExplanation(previousGuess(toggledEdge),
				null);
		Explanation current = lookupExplanation(currentGuess, previous);
		// Util.print("es.improv "+toggledEdge+" "+current+" "+previous);
		// current.printGraph(false);
		double result = isAdding(toggledEdge) ? previous.improvement(current,
				threshold1) : -current.improvement(previous, threshold1);

		return result;
	}

	// double improvement(Object toggledEdge) {
	// Set addedEdges = new HashSet(currentGuess);
	// addedEdges.add(toggledEdge);
	// Explanation larger = lookupExplanation(addedEdges, null);
	// addedEdges.remove(toggledEdge);
	// Explanation smaller = lookupExplanation(addedEdges, larger);
	// double result = -smaller.improvement(larger);
	// // Util.print("EdgeSelection.improvement " + result
	// // + (currentGuess.contains(toggledEdge) ? " + " : " - ")
	// // + toggledEdge + " " + currentGuess);
	// return result;
	// }

	Set allEdges(Collection addedEdges) {
		Set result = new HashSet(committedEdges);
		result.addAll(addedEdges);
		return result;
	}

	Explanation lookupExplanation(Set addedEdges, Explanation base) {
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
		expl.predicted.edgesFixed = true;
		List args = new ArrayList(3);
		args.add(expl);
		args.add(expl.toString());
		args.add(edges.size() + "");
		explanations.put(Collections.unmodifiableSet(edges), args);

	}

	private Explanation edges2Explanation(Set edges) {
		List args = (List) explanations.get(edges);
		Explanation result = args == null ? null : (Explanation) args.get(0);
		assert result == null || edges.size() == result.predicted.nEdges() : result.predicted.edgesFixed
				+ " " + args + " " + edges.size();
		return result;
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		// lookupExplanation(currentGuess, null).printToFile();
	}
}
