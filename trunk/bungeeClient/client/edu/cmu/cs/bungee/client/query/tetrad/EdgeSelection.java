package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
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
		Set startCoreEdges = candidateEdges(explanation, true);
		Set startNonCoreEdges = candidateEdges(explanation, false);
		EdgeSelection nonCoreSearch = new EdgeSelection(threshold,
				startNonCoreEdges, startCoreEdges, explanation);
		Set nonCoreEdges = nonCoreSearch.selectVariables();
		EdgeSelection coreSearch = new EdgeSelection(threshold,
				startCoreEdges, nonCoreEdges, explanation);
		Set coreEdges = coreSearch.selectVariables();
		// Util.print("EdgeSelection => " + coreEdges);
		return coreSearch.lookupExplanation(coreEdges, null);
	}

	EdgeSelection(double threshold, Set candidateEdges, Set committedEdges,
			Explanation base) {
		super(threshold, candidateEdges);
		addAllVariables();
		this.committedEdges = committedEdges;
		this.maxModel = base;
		// Util.print("edgesel " + edges);
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

	double improvement(Object toggledEdge) {
		Explanation previous = lookupExplanation(previousGuess(toggledEdge),
				null);
		Explanation current = lookupExplanation(currentGuess, previous);
		double result = isAdding(toggledEdge) ? previous.improvement(current)
				: -current.improvement(previous);
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
		Set allEdges = allEdges(addedEdges);
		Explanation result = (Explanation) explanations.get(allEdges);
		if (result == null) {
			if (base == null)
				base = maxModel;
			result = base.getAlternateExplanation(allEdges);
			explanations.put(allEdges, result);
		}
		return result;
	}

	protected void newBest(Object candidate) {
		super.newBest(candidate);
		lookupExplanation(currentGuess, null).printToFile();
	}
}
