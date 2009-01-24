package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;

public abstract class Explanation {

	/**
	 * Importance of weight changes in the null model compared to accuracy over
	 * the null model, used to evaluate learned model.
	 */
	protected static double WEIGHT_SPACE_IMPORTANCE = 0.1;
	// protected static final double NODE_COST = 0.1;
	protected static final double EDGE_COST = 0.005;
	private static final int MAX_CANDIDATES = 16;
	protected final Explanation nullModel;

	protected GraphicalModel predicted;
	protected Distribution observed;

	public boolean approxEquals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Explanation other = (Explanation) obj;
		if (nullModel == this) {
			if (other.nullModel != other)
				return false;
		} else if (other.nullModel == other)
			return false;
		else if (!nullModel.approxEquals(other.nullModel))
			return false;
		if (!observed.approxEquals(other.observed))
			return false;
		if (!predicted.approxEquals(other.predicted))
			return false;
		return true;
	}

	// /**
	// * @return a clone, using this as the null mmodel, presumably to add
	// facets
	// */
	// abstract Explanation getLikeExplanation();

	// /**
	// * @param facets
	// * @return an explanation with different facets
	// */
	// abstract Explanation getAlternateExplanation(List facets);

	/**
	 * @return an explanation with different facets
	 */
	abstract Explanation getAlternateExplanation(List facets,
			List likelyCandidates);

	/**
	 * @param edges
	 * @return an explanation with different edges
	 */
	abstract Explanation getAlternateExplanation(Set edges);

	protected Explanation(List facets, Set edges, Explanation base,
			List likelyCandidates) {
		this.nullModel = base != null ? base.nullModel : this;
		predicted = new GraphicalModel(facets, edges, true);
		observed = Distribution.getObservedDistribution(facets,
				likelyCandidates);

		// Distribution test = Distribution.getObservedDistribution(facets);
		// assert observed.facets.equals(test.facets);
		// double[] obsdistribution = observed.getDistribution();
		// double[] testdistribution = test.getDistribution();
		// for (int i = 0; i < obsdistribution.length; i++) {
		// assert obsdistribution[i] == testdistribution[i]
		// || Math.abs((obsdistribution[i] - testdistribution[i])
		// / obsdistribution[i]) < 0.0000001 : obsdistribution[i]
		// + " " + testdistribution[i] + " " + observed + " " + test;
		// }

		learnWeights(base);
		// cache();
	}

	/**
	 * optimize weights of predicted model to match observed and nullModel
	 * 
	 * @param base
	 *            initialize weights based on this model
	 */
	abstract void learnWeights(Explanation base);

	List facets() {
		assert predicted.facets.equals(observed.facets) : predicted.facets
				+ " " + observed.facets;
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

	double unnormalizedKLdivergence() {
		return observed.unnormalizedKLdivergence(predicted
				.logPredictedDistribution());
	}

	double KLdivergence() {
		return observed.KLdivergence(predicted.getDistribution());
	}

	double improvement(Explanation largerModel, double threshold1) {
		// double largerModelAccuracy = sumR(largerModel);
		// double accuracy = sumR(this);
		// double largerModelAccuracy = nullModel.observed
		// .KLdivergence(largerModel.predicted.getMarginal(nullModel
		// .facets()));
		// double accuracy = nullModel.unnormalizedKLdivergence();
		double largerModelAccuracy = nullModel.observed
				.KLdivergence(largerModel.predicted.getMarginal(nullModel
						.facets()));
		double accuracy = nullModel.KLdivergence();

		// assert largerModel.nFacets() == nFacets()
		// || accuracy - 0.0001 < largerModelAccuracy : this
		// + " "
		// + accuracy
		// + " "
		// + Util.valueOfDeep(predicted.getMarginal(nullModel.facets()))
		// + printGraph(true)
		// + " "
		// + largerModel
		// + " "
		// + largerModelAccuracy
		// + " "
		// + Util.valueOfDeep(largerModel.predicted.getMarginal(nullModel
		// .facets())) + " "
		// + Util.valueOfDeep(nullModel.observed.getDistribution())
		// + largerModel.printGraph(true);

		assert largerModel.nFacets() > nFacets()
				|| accuracy - 0.00001 < largerModelAccuracy : this + " "
				+ accuracy + " " + largerModel + " " + largerModelAccuracy;

		double deltaSumR = largerModelAccuracy - accuracy;

		// Util.print("grad "+primaryFacets());
		// printToFile();
		double weightSpaceChange = largerModel.predicted.weightSpaceChange(
				predicted, primaryFacets(), observed, largerModel.observed,
				true);
		// Util.print("fastImprov " + weightSpaceChange + " " + threshold1);
//		 double fastWeighSpaceChange = weightSpaceChange;

		if (weightSpaceChange * WEIGHT_SPACE_IMPORTANCE + deltaSumR > threshold1) {
			weightSpaceChange = largerModel.predicted.weightSpaceChange(
					predicted, primaryFacets(), observed, largerModel.observed,
					false);

			// Util.print(" slowImprov " + weightSpaceChange + " " +
			// threshold1);
		}

		// largerModel.printGraph(false);
//		 Util
//		 .print("                                               improvement "
//		 + weightSpaceChange
//		 + "("
//		 + fastWeighSpaceChange
//		 + ") deltaSumR "
//		 + largerModelAccuracy
//		 + " - "
//		 + accuracy + " = " + deltaSumR + " " + largerModel);

		return weightSpaceChange * WEIGHT_SPACE_IMPORTANCE - deltaSumR;
	}

	void printToFile() {
		edu.cmu.cs.bungee.piccoloUtils.gui.Graph.printMe(buildGraph(null), Util
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

	static List candidateFacets(List primaryFacets, boolean excludePrimary) {
		Query query = ((Perspective) Util.some(primaryFacets)).query();
		List topTags = query.topMutInf(primaryFacets, MAX_CANDIDATES);
		Set candidates = new HashSet(MAX_CANDIDATES);
		for (Iterator it = topTags.iterator(); it.hasNext();) {
			Perspective tr = (Perspective) it.next();
			if (!primaryFacets.contains(tr)) {
				candidates.add(tr);

				// List facets = new LinkedList(primaryFacets);
				// facets.add(tr);
				// Collections.sort(facets);
				// List facet = new ArrayList(1);
				// facet.add(tr);
				// Distribution dist = Distribution.getObservedDistribution(
				// facets, null);
				// Util.print(tr + " "
				// + dist.mutualInformation(primaryFacets, facet) + " "
				// + dist.sumCorrelation(tr));
			}
		}
		// result.add(query.findPerspective(402));
		if (excludePrimary)
			candidates.removeAll(primaryFacets);
		else
			candidates.addAll(primaryFacets);
		Util.print("candidateFacets " + primaryFacets + " => " + candidates);
		ArrayList result = new ArrayList(candidates);
		Collections.sort(result);
		return result;
	}

	Set nonPrimaryFacets() {
		Set nonPrimary = new HashSet(facets());
		nonPrimary.removeAll(primaryFacets());
		return nonPrimary;
	}

	List primaryFacets() {
		return nullModel.facets();
	}

	boolean isPrimaryFacet(Perspective p) {
		return primaryFacets().contains(p);
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

	void initializeWeights(Explanation base) {
		GraphicalModel baseModel = base.predicted;
		for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			int causeNode = edge[0];
			int causedNode = edge[1];
			Perspective cause = (Perspective) facets().get(causeNode);
			Perspective caused = (Perspective) facets().get(causedNode);
			if (baseModel.hasEdge(cause, caused)) {
				predicted.setWeight(causeNode, causedNode, baseModel.getWeight(
						cause, caused));
			}
		}
	}

	void optimizeWeight(Explanation base) {
		int argIndex = 0;
		double[] observedDistribution = observed.getDistribution();
		double maxDelta = 0;
		int maxCause = -1;
		int maxCaused = -1;
		for (int bit = 0; bit < nFacets(); bit++) {
			double delta = computeDelta(observedDistribution, bit, bit);
			if (Math.abs(delta) > Math.abs(maxDelta)) {
				maxDelta = delta;
				maxCause = bit;
				maxCaused = bit;
			}
		}

		GraphicalModel baseModel = base == null ? null : base.predicted;
		for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			int causeNode = edge[0];
			int causedNode = edge[1];
			Perspective cause = (Perspective) facets().get(causeNode);
			Perspective caused = (Perspective) facets().get(causedNode);
			double delta = computeDelta(observedDistribution, causeNode,
					causedNode);
			if (baseModel != null && baseModel.hasEdge(cause, caused)) {
//				delta += NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE
//						* (baseModel.getWeight(cause, caused) - (predicted
//								.getWeight(cause, caused) + delta));
				delta=0;
			}
			if (Math.abs(delta) > Math.abs(maxDelta)) {
				maxDelta = delta;
				maxCause = causeNode;
				maxCaused = causedNode;
			}
			argIndex++;
		}
		if (maxCause >= 0) {
			// Util.print("ff "+maxCause+" "+maxCaused+" "+maxDelta);
			predicted.setWeight(maxCause, maxCaused, predicted.getWeight(
					maxCause, maxCaused)
					+ maxDelta);
		}
		// Util.print(Util.valueOfDeep(getObservedDistribution()));
		// Util.print("getInitialWeights " + Util.valueOfDeep(result) + " base="
		// + base);
		// Util.print("getweights="+Util.valueOfDeep(predicted.getWeights()));
	}

	private double computeDelta(double[] observedDistribution, int causeNode,
			int causedNode) {
		double expOn = 0, expOff = 0, obs = 0;
		for (int state = 0; state < observedDistribution.length; state++) {
			if (Util.isBit(state, causeNode) && Util.isBit(state, causedNode)) {
				expOn += predicted.expEnergy(state);
				obs += observedDistribution[state];
			} else {
				expOff += predicted.expEnergy(state);
			}
		}
		// Util.print("hh "+expOn+" "+expOff+" "+obs);
		obs = Math.max(0.00001, obs);
		double delta = -Math.log(expOn * (1 - obs) / obs / expOff);
		return delta;
	}

	// /**
	// * Clip to avoid infinities
	// */
	// private double logOfRatio(double num, double denom) {
	// double ratio = denom == 0 ? 100 : num == 0 ? 0.01 : num / denom;
	// return Math.log(ratio);
	// }

	/**
	 * add facets to explain this null model
	 */
	public Explanation getExplanation() {
		long start = (new Date()).getTime();
		NonAlchemyModel.totalNumFuns = 0;
		// Util.print("selectFacets " + maxModel.facets + "\n");
		Explanation result = FacetSelection.selectFacets(this, EDGE_COST);
		result = EdgeSelection.selectEdges(result, EDGE_COST);
		double prev = NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE;
		NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = 0;
		result.learnWeights(nullModel);
		NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = prev;
		long duration = (new Date()).getTime() - start;
		result.printGraphAndNull();
		Util.print("getExplanation duration=" + (duration / 1000)
				+ " Function Evaluations: " + NonAlchemyModel.totalNumFuns
				+ "\n");
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

	// Explanation selectFacets(Distribution maxModel) {
	// double bestEval = Double.NEGATIVE_INFINITY;
	// Explanation best = null;
	// Set candidateFacets = new HashSet(maxModel.facets);
	// assert candidateFacets.containsAll(facets());
	// candidateFacets.removeAll(facets());
	// List currentGuess = new LinkedList(facets());
	// for (Iterator it = candidateFacets.iterator(); it.hasNext();) {
	// Perspective candidateFacet = (Perspective) it.next();
	// currentGuess.add(candidateFacet);
	// Explanation candidateExplanation = getAlternateExplanation(
	// currentGuess, maxModel);
	// assert candidateExplanation != this && candidateExplanation != best;
	// double eval = improvement(candidateExplanation);
	//
	// // Util.print("selectFacets eval " + eval + " " +
	// // candidateExplanation
	// // + "\n");
	// if (eval > bestEval) {
	// bestEval = eval;
	// best = candidateExplanation;
	// }
	// currentGuess.remove(candidateFacet);
	// }
	// if (best != null) {
	// Util.print("selectFacets BEST " + bestEval + " " + best);
	// best.printToFile();
	// }
	// // best.printGraph();
	// if (bestEval >= NODE_COST) {
	// best = best.selectFacets(maxModel);
	// } else {
	// best = this;
	// }
	// return best;
	// }

	String printGraph(boolean primaryOnly) {
		Util.print("obs: " + observed);
		return predicted.printGraph(observed, primaryOnly ? primaryFacets()
				: null);
	}

	// /**
	// * @param isCoreEdges
	// * Once core edges are removed, removing others will have no
	// * effect. So first remove any non-core edges you can, and then
	// * remove only core edges.
	// */
	// Explanation selectEdges(boolean isCoreEdges, Distribution maxModel) {
	// double bestEval = Double.NEGATIVE_INFINITY;
	// Explanation best = null;
	// List bestEdge = null;
	// Set candidateEdges = predicted.getEdges();
	// Set currentGuess = predicted.getEdges();
	// for (Iterator it = candidateEdges.iterator(); it.hasNext();) {
	// List candidateEdge = (List) it.next();
	// if (isCoreEdges == (primaryFacets().containsAll(candidateEdge))) {
	// currentGuess.remove(candidateEdge);
	// Explanation candidateExplanation = getAlternateExplanation(currentGuess);
	// double eval = -candidateExplanation.improvement(this);
	// // Util.print("selectEdges eval " + eval + " " + candidateEdge);
	// if (eval > bestEval) {
	// bestEval = eval;
	// best = candidateExplanation;
	// bestEdge = candidateEdge;
	// }
	// currentGuess.add(candidateEdge);
	// }
	// }
	// if (best != null) {
	// Util.print("selectEdges BEST " + bestEval + " - " + bestEdge);
	// best.printToFile();
	// }
	// // best.printGraph();
	// if (bestEval > -EDGE_COST) {
	// best = best.selectEdges(isCoreEdges, maxModel);
	// } else if (!isCoreEdges)
	// best = selectEdges(true, maxModel);
	// else
	// best = this;
	// return best;
	// }

	protected int nFacets() {
		return predicted.nFacets();
	}

	protected Perspective getFacet(int i) {
		return predicted.getFacet(i);
	}

	private void printGraphAndNull() {
		if (nullModel != this) {
			Util.print("\nbuildGraph\n" + nullModel.buildGraph(null));
			nullModel.printGraph(false);
		}
		Util.print("\nbuildGraph\n" + buildGraph(null));
		printGraph(false);

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

	public Graph buildGraph(PerspectiveObserver redrawer) {
		return predicted.buildGraph(observed, nullModel, redrawer);
	}
}
