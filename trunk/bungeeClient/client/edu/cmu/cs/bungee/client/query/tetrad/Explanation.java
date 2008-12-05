package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags.TagRelevance;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;

public abstract class Explanation {

	protected static final double WEIGHT_SPACE_IMPORTANCE = 0.1;
	protected static final double NODE_COST = 0.1;
	protected static final double EDGE_COST = 0.05;
	private static final int MAX_CANDIDATES = 10;
	protected final Explanation nullModel;

	protected GraphicalModel predicted;
	protected Distribution observed;

	/**
	 * @return a clone, using this as the null mmodel, presumably to add facets
	 */
	abstract Explanation getLikeExplanation();

	// /**
	// * @param facets
	// * @return an explanation with different facets
	// */
	// abstract Explanation getAlternateExplanation(List facets);

	/**
	 * @param facets
	 * @return an explanation with different facets
	 */
	abstract Explanation getAlternateExplanation(List facets,
			Distribution maxModel);

	/**
	 * @param edges
	 * @return an explanation with different edges
	 */
	abstract Explanation getAlternateExplanation(Set edges);

	protected Explanation(List facets, Set edges, Explanation base,
			Distribution maxModel) {
		this.nullModel = base != null ? base.nullModel : this;
		predicted = new GraphicalModel(facets, edges, true);
		observed = Distribution.getObservedDistribution(facets, maxModel);
		learnWeights(base);
	}

	/**
	 * optimize weights of predicted model to match observed and nullModel
	 * 
	 * @param base
	 *            initialize weights based on this model
	 */
	abstract void learnWeights(Explanation base);

	List facets() {
		assert predicted.facets.equals(observed.facets);
		return predicted.facets;
	}

	// void fullyConnect() {
	// assert nullModel != null;
	// for (Iterator it1 = facets.iterator(); it1.hasNext();) {
	// Object facet1 = it1.next();
	// for (Iterator it2 = facets.iterator(); it2.hasNext();) {
	// Object facet2 = it2.next();
	// if (facet1 != facet2) {
	// addEdge((Perspective) facet1, (Perspective) facet2);
	// }
	// }
	// }
	// }

	protected int facetIndex(Perspective facet) {
		return predicted.facetIndex(facet);
	}

	protected int facetIndexOrNot(Perspective facet) {
		return predicted.facetIndexOrNot(facet);
	}

	double improvement(Explanation largerModel) {
		double largerModelAccuracy = sumR(largerModel);
		double accuracy = sumR(this);

		assert largerModel.nFacets() == nFacets()
				|| accuracy + 0.00001 > largerModelAccuracy : this + " "
				+ accuracy + " " + largerModel + " " + largerModelAccuracy;

		assert largerModel.nFacets() > nFacets()
				|| accuracy - 0.00001 < largerModelAccuracy : this + " "
				+ accuracy + " " + largerModel + " " + largerModelAccuracy;

		double deltaSumR = largerModelAccuracy - accuracy;

		// printToFile();
		double weightSpaceChange = largerModel.predicted.weightSpaceChange(
				predicted, primaryFacets());

		// Util.print("weightSpaceChange " + weightSpaceChange + " deltaSumR "
		// + largerModelAccuracy + " - " + accuracy + " = " + deltaSumR
		// + " " + largerModel);

		return weightSpaceChange * WEIGHT_SPACE_IMPORTANCE + deltaSumR;
	}

	void printToFile() {
		edu.cmu.cs.bungee.piccoloUtils.gui.Graph.printMe(buildGraph(), Util
				.convertForFilename(toString()));
	}

	double sumR(Explanation largerModel) {
		// Util.print("sumR graph");
		// largerModel.printGraph();
		return nullModel.observed.sumR(largerModel.predicted);
	}

	static List primaryFacets(Perspective popupFacet) {
		Set primaryFacets1 = popupFacet.query().allRestrictions();
		primaryFacets1.add(popupFacet);
		// TopTags topTags = popupFacet.query().topTags(2 * MAX_FACETS);
		// for (Iterator it = topTags.topIterator(); it.hasNext()
		// && primaryFacets1.size() < MAX_FACETS;) {
		// TagRelevance tag = (TagRelevance) it.next();
		// Perspective p = (Perspective) tag.tag.object;
		// primaryFacets1.add(p);
		// primaryFacets1 = removeAncestorFacets(primaryFacets1, popupFacet);
		// }
		return new ArrayList(primaryFacets1);
	}

	static List candidateFacets(Collection primaryFacets, boolean excludePrimary) {
		Query query = ((Perspective) Util.some(primaryFacets)).query();
		TopTags topTags = query.topTags(MAX_CANDIDATES);
		Set result = new HashSet(MAX_CANDIDATES);
		for (Iterator it = topTags.topIterator(); it.hasNext();) {
			TagRelevance tr = (TagRelevance) it.next();
			result.add(tr.tag.object);
		}
		if (excludePrimary)
			result.removeAll(primaryFacets);
		else
			result.addAll(primaryFacets);
		return new ArrayList(result);
	}

	Set nonPrimaryFacets() {
		Set nonPrimary = new HashSet(facets());
		nonPrimary.removeAll(primaryFacets());
		return nonPrimary;
	}

	List primaryFacets() {
		return nullModel.facets();
	}

	double pseudoRsquared(Perspective caused) {
		// Util.print("\npseudoRsquared " + caused + " " + this + "\n"
		// + printGraph(false));
		assert observed.facets.equals(predicted.facets);
		return observed.pseudoRsquared(predicted.getDistribution(), caused);
	}

	double getRNormalizedWeight(Perspective cause, Perspective caused) {
		// Util.print("\npseudoRsquared " + caused + " " + this + "\n"
		// + printGraph(false));
		assert observed.facets.equals(predicted.facets);
		return predicted.getRNormalizedWeight(observed, cause, caused);
	}

	// private int[] getPredictedCounts() {
	// double[] predicted = getDistribution(true);
	// int[] result = new int[predicted.length];
	// int count = count();
	// for (int i = 0; i < result.length; i++) {
	// result[i] = (int) Math.round(predicted[i] * count);
	// }
	// return result;
	// }
	//
	// private int count() {
	// int count = 0;
	// for (int i = 0; i < counts.length; i++) {
	// count += counts[i];
	// }
	// return count;
	// }

	// double predictivity(Perspective caused) {
	// double result = nullModel.observed.pseudoRsquared(predicted
	// .getMarginal(primaryFacets()), caused);
	// // Util.print("");
	// // printGraph(false);
	// // Util.print("predictivity " + caused + " " + result + " " + this);
	// return result;
	// }

	// double[] getInitialWeights() {
	// // Util.print("getInitialWeights " + predicted + " "
	// // + getObservedDistribution());
	// int argIndex = 0;
	// double[] result = new double[getNumArguments()];
	// double[] observedDistribution = observed.getDistribution();
	// for (int bit = 0; bit < nFacets(); bit++) {
	// int state = Util.setBit(0, bit, true);
	// result[argIndex++] = logOfRatio(observedDistribution[state],
	// observedDistribution[0]);
	// }
	//
	// for (Iterator it = predicted.getEdges().iterator(); it.hasNext();) {
	// List edge = (List) it.next();
	// // wts.add(edge);
	// Perspective cause = (Perspective) edge.get(0);
	// Perspective caused = (Perspective) edge.get(1);
	// double[] marginal = observed.getMarginal(edge);
	// double logOfRatio = logOfRatio(marginal[3], marginal[0]);
	// // double logOfRatio = logOfRatio(marginal[3], 1 - marginal[3]);
	// // Util.print(Util.valueOfDeep(marginal) + " " + logOfRatio);
	// result[argIndex++] = logOfRatio - result[facetIndex(caused)]
	// - result[facetIndex(cause)];
	//
	// }
	// // Util.print(Util.valueOfDeep(getObservedDistribution()));
	// // Util.print("getInitialWeights " + Util.valueOfDeep(result));
	// return result;
	// }

	public int getNumArguments() {
		return nFacets() + predicted.nEdges();
	}

	double[] getInitialWeights(Explanation base) {
		// assert base == null || facets().containsAll(base.facets());

		int argIndex = 0;
		double[] result = new double[getNumArguments()];
		double[] observedDistribution = observed.getDistribution();
		for (int bit = 0; bit < nFacets(); bit++) {
			int state = Util.setBit(0, bit, true);
			result[argIndex++] = logOfRatio(observedDistribution[state],
					observedDistribution[0]);
		}

		GraphicalModel baseModel = base == null ? null : base.predicted;
		for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			int causeNode = edge[0];
			int causedNode = edge[1];
			Perspective cause = (Perspective) facets().get(causeNode);
			Perspective caused = (Perspective) facets().get(causedNode);
			if (baseModel != null && baseModel.hasEdge(cause, caused)) {
				result[argIndex++] = baseModel.getWeight(cause, caused);
			} else {
				double[] marginal = observed.getMarginal(edge);
				double logOfRatio = logOfRatio(marginal[3], 1 - marginal[3]);
				// double logOfRatio = logOfRatio(marginal[3], marginal[0]);
				// double logOfRatio = logOfRatio(marginal[3], 1 - marginal[3]);
				// Util.print(Util.valueOfDeep(marginal) + " " + logOfRatio);
				result[argIndex++] = logOfRatio - result[causedNode]
						- result[causeNode];
			}
		}
		// Util.print(Util.valueOfDeep(getObservedDistribution()));
		// Util.print("getInitialWeights " + Util.valueOfDeep(result));
		return result;
	}

	/**
	 * Clip to avoid infinities
	 */
	private double logOfRatio(double num, double denom) {
		double ratio = denom == 0 ? 100 : num == 0 ? 0.01 : num / denom;
		return Math.log(ratio);
	}

	/**
	 * add facets to explain this null model
	 */
	public Explanation getExplanation(Distribution maxModel) {
		long start = (new Date()).getTime();
		// Util.print("selectFacets " + maxModel.facets + "\n");
		Explanation result = FacetSelection.selectFacets(this, maxModel,
				NODE_COST);
		result = EdgeSelection.selectEdges(result, EDGE_COST);
		long duration = (new Date()).getTime() - start;
		result.printGraphAndNull();
		Util.print("getExplanation duration=" + (duration / 1000));
		return result;
	}

	// /**
	// * add facets to explain this null model
	// */
	// public Explanation getExplanation(Distribution maxModel) {
	// // printGraph(false);
	// // pseudoRsquared(getFacet(0));
	// // if (true)
	// // return null;
	// // Util.print("getExplanation");
	// long start = (new Date()).getTime();
	// // Util.print("selectFacets " + maxModel.facets + "\n");
	// Explanation result = selectFacets(maxModel);
	// result = result.selectEdges(false, maxModel);
	// long duration = (new Date()).getTime() - start;
	// result.printGraphAndNull();
	// Util.print("getExplanation duration=" + (duration / 1000));
	// return result;
	// }

	Explanation selectFacets(Distribution maxModel) {
		double bestEval = Double.NEGATIVE_INFINITY;
		Explanation best = null;
		Set candidateFacets = new HashSet(maxModel.facets);
		assert candidateFacets.containsAll(facets());
		candidateFacets.removeAll(facets());
		List currentGuess = new LinkedList(facets());
		for (Iterator it = candidateFacets.iterator(); it.hasNext();) {
			Perspective candidateFacet = (Perspective) it.next();
			currentGuess.add(candidateFacet);
			Explanation candidateExplanation = getAlternateExplanation(
					currentGuess, maxModel);
			assert candidateExplanation != this && candidateExplanation != best;
			double eval = improvement(candidateExplanation);

			// Util.print("selectFacets eval " + eval + " " +
			// candidateExplanation
			// + "\n");
			if (eval > bestEval) {
				bestEval = eval;
				best = candidateExplanation;
			}
			currentGuess.remove(candidateFacet);
		}
		if (best != null) {
			Util.print("selectFacets BEST " + bestEval + " " + best);
			best.printToFile();
		}
		// best.printGraph();
		if (bestEval >= NODE_COST) {
			best = best.selectFacets(maxModel);
		} else {
			best = this;
		}
		return best;
	}

	void printGraph() {
		predicted.printGraph(observed);
	}

	/**
	 * @param isCoreEdges
	 *            Once core edges are removed, removing others will have no
	 *            effect. So first remove any non-core edges you can, and then
	 *            remove only core edges.
	 */
	Explanation selectEdges(boolean isCoreEdges, Distribution maxModel) {
		double bestEval = Double.NEGATIVE_INFINITY;
		Explanation best = null;
		List bestEdge = null;
		Set candidateEdges = predicted.getEdges();
		Set currentGuess = predicted.getEdges();
		for (Iterator it = candidateEdges.iterator(); it.hasNext();) {
			List candidateEdge = (List) it.next();
			if (isCoreEdges == (primaryFacets().containsAll(candidateEdge))) {
				currentGuess.remove(candidateEdge);
				Explanation candidateExplanation = getAlternateExplanation(currentGuess);
				double eval = -candidateExplanation.improvement(this);
				// Util.print("selectEdges eval " + eval + " " + candidateEdge);
				if (eval > bestEval) {
					bestEval = eval;
					best = candidateExplanation;
					bestEdge = candidateEdge;
				}
				currentGuess.add(candidateEdge);
			}
		}
		if (best != null) {
			Util.print("selectEdges BEST " + bestEval + " - " + bestEdge);
			best.printToFile();
		}
		// best.printGraph();
		if (bestEval > -EDGE_COST) {
			best = best.selectEdges(isCoreEdges, maxModel);
		} else if (!isCoreEdges)
			best = selectEdges(true, maxModel);
		else
			best = this;
		return best;
	}

	protected int nFacets() {
		return predicted.nFacets();
	}

	protected Perspective getFacet(int i) {
		return predicted.getFacet(i);
	}

	private void printGraphAndNull() {
		if (nullModel != this) {
			Util.print("\nbuildGraph\n" + nullModel.buildGraph());
			nullModel.printGraph();
		}
		Util.print("\nbuildGraph\n" + buildGraph());
		printGraph();

	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(Util.shortClassName(this))//.append(" quality=").
				// append(quality()
				// )
				.append(" ").append(primaryFacets()).append(" + ").append(
						nonPrimaryFacets());

		if (false) {
			for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
				int[] edge = (int[]) it.next();
				int causeNode = edge[0];
				int causedNode = edge[1];
				if (predicted.hasEdge(causeNode, causedNode)) {
					double weight = predicted.getWeight(causeNode, causedNode);
					Perspective p1 = (Perspective) facets().get(causeNode);
					Perspective p2 = (Perspective) facets().get(causedNode);
					buf.append("\n").append(weight).append(" (").append(
							predicted.getStdDevNormalizedWeight(p1, p2))
							.append(") ").append(p1).append(" ---- ")
							.append(p2);
				}
			}
		} else {
			buf.append(" nEdges=").append(predicted.nEdges());
		}

		buf.append(">");
		return buf.toString();
	}

	public Graph buildGraph() {
		return predicted.buildGraph(observed, nullModel);
	}
}
