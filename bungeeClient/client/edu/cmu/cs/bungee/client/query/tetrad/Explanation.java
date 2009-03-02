package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import pal.mathx.ConjugateGradientSearch;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;

public abstract class Explanation implements PerspectiveObserver {

	/**
	 * Importance of weight changes in the null model compared to Divergence
	 * over the null model, used to evaluate learned model.
	 */
	protected static double NULL_MODEL_ACCURACY_IMPORTANCE = 10;
	protected static double FULL_MODEL_ACCURACY_IMPORTANCE = 1000;
	// protected static final double NODE_COST = 0.1;
	protected static final double EDGE_COST = 0.05;
	private static final int MAX_CANDIDATES = 16;
	protected final Explanation parentModel;

	protected final GraphicalModel predicted;
	protected final Distribution observed;

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
		parentModel = base != null ? base : this;
		observed = Distribution.getObservedDistribution(facets,
				likelyCandidates);
		predicted = new GraphicalModel(facets, edges, true, observed.totalCount);

		// if (base != null)
		// Util.print("expl " + predicted.getEdges() + " "
		// + nullModel.predicted.getEdges());

		assert parentModel == this
				|| !facets().equals(parentModel.facets())
				|| !predicted.getEdges().equals(
						parentModel.predicted.getEdges()) : this + " "
				+ parentModel + " " + base;

		// Util.print("Explanation " + facets + " " + base + " " + edges);

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

						// Don't do this until subtype initializations have happened
//		learnWeights(base);
		// cache();
	}

	Explanation nullModel() {
		return this == parentModel ? this : parentModel.nullModel();
	}

	public boolean approxEquals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Explanation other = (Explanation) obj;
		if (parentModel == this) {
			if (other.parentModel != other)
				return false;
		} else if (other.parentModel == other)
			return false;
		else if (!parentModel.approxEquals(other.parentModel))
			return false;
		if (!observed.approxEquals(other.observed))
			return false;
		if (!predicted.approxEquals(other.predicted))
			return false;
		return true;
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

	double klDivergence() {
		if (ConjugateGradientSearch.vvv) {
			Util.print("\nkl " + Util.valueOfDeep(predicted.getWeights()));
			for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
				int[] edge = (int[]) it.next();
				Perspective cause = (Perspective) predicted.facets.get(edge[0]);
				Perspective caused = (Perspective) predicted.facets
						.get(edge[1]);
				Util.print(cause + " " + caused);
			}
		}
		double result = observed.klDivergenceFromLog(predicted
				.logPredictedDistribution());
		// assert Util.isClose(result, observed.klDivergence(predicted
		// .getDistribution()));
		return result;
	}

	double improvement(Explanation largerModel, double threshold1) {
		double largerModelNullDivergence = parentModel.observed
				.klDivergence(largerModel.predicted.getMarginal(parentFacets()));
		double nullDivergence = parentModel.klDivergence();
		double deltaNullDivergence = (largerModelNullDivergence - nullDivergence)
				* NULL_MODEL_ACCURACY_IMPORTANCE;
		assert !Double.isInfinite(deltaNullDivergence) : largerModelNullDivergence
				+ " "
				+ nullDivergence
				+ " "
				+ Util.valueOfDeep(largerModel.predicted
						.getMarginal(parentFacets()));

		assert largerModel.nFacets() > nFacets()
				|| nullDivergence + 0.00001 > largerModelNullDivergence : this
				+ "\n" + parentModel + " " + nullDivergence + "\n"
				+ largerModel + " " + largerModelNullDivergence;

		double deltaFullDivergence = Double.POSITIVE_INFINITY;
		if (largerModel.nFacets() == nFacets()) {
			double largerModelFullDivergence = largerModel.klDivergence();
			double fullDivergence = klDivergence();
			deltaFullDivergence = (fullDivergence - largerModelFullDivergence)
					* FULL_MODEL_ACCURACY_IMPORTANCE;
		}
		if (deltaFullDivergence < threshold1)
			return deltaFullDivergence;

		// Util.print("grad "+primaryFacets());
		// printToFile();
		double weightSpaceChange = largerModel.predicted
				.weightSpaceChange(predicted, parentFacets(), observed,
						largerModel.observed, true);
		// Util.print("fastImprov " + weightSpaceChange + " " + threshold1);
		double fastWeighSpaceChange = weightSpaceChange;
		assert Util.ignore(fastWeighSpaceChange);
		boolean isSlow = weightSpaceChange - deltaNullDivergence > threshold1;

		if (isSlow) {
			weightSpaceChange = largerModel.predicted.weightSpaceChange(
					predicted, parentFacets(), observed, largerModel.observed,
					false);

			// Util.print(" slowImprov " + weightSpaceChange + " " +
			// threshold1);
		}

		// Util.print("                                        "
		// + (isSlow ? "      *" : "       ")
		// + "improvement "
		// + weightSpaceChange
		// + (!isSlow ? "" : " (fast was " + fastWeighSpaceChange + " - "
		// + deltaNullDivergence + " > " + threshold1 + ")")
		// // + " nullDivergence "
		// // +
		// // nullModel.observed.KLdivergence(largerModel.predicted
		// // .getMarginal(nullModel.facets()))
		// // + " - "
		// // + nullModel.KLdivergence(
		// // + " = "
		// // + deltaNullDivergence
		// + (largerModel.nFacets() == nFacets() ? " fullDivergence "
		// + largerModel.klDivergence() + " - " + klDivergence()
		// + " = " + deltaFullDivergence : "")
		// + " predicted="
		// + Util.valueOfDeep(largerModel.predicted
		// .getMarginalCounts(primaryFacets()))
		// + " observed="
		// + Util.valueOfDeep(nullModel.observed
		// .getMarginalCounts(nullModel.facets()))
		// + " deltaNullDivergence=" + deltaNullDivergence
		// + " deltaFullDivergence=" + deltaFullDivergence);

		// printGraph(false);
		// largerModel.printGraph(false);

		return Math.min(weightSpaceChange - deltaNullDivergence,
				deltaFullDivergence);
	}

	void printToFile() {
		edu.cmu.cs.bungee.piccoloUtils.gui.Graph.printMe(buildGraph(this), Util
				.convertForFilename(toString()));
	}

	// double sumR(Explanation largerModel) {
	// // Util.print("sumR graph");
	// // largerModel.printGraph();
	// return nullModel.observed.sumR(largerModel.predicted);
	// }

	static List primaryFacets(Perspective popupFacet) {
		Set primaryFacets1 = popupFacet.query().allRestrictions();
		primaryFacets1.add(popupFacet);
		// TopTags topTags = popupFacet.query().topTags(2 * MAX_FACETS);
		// for (Iterator it = topTags.topIterator(); it.hasNext()
		// && primaryFacets1.size() < MAX_FACETS;) {
		// TagRelevance tag = (TagRelevance) it.next();
		// Perspective p = (Perspective) tag.tag.object;
		// primaryFacets1.add(p);
		removeAncestorFacets(primaryFacets1, popupFacet);
		// }
		return new ArrayList(primaryFacets1);
	}

	private static void removeAncestorFacets(Collection primary,
			Perspective popupFacet) {
		for (Iterator it = new ArrayList(primary).iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			Collection ancestors = facet.ancestors();
			// Never remove popupFacet
			ancestors.remove(popupFacet);
			primary.removeAll(ancestors);
		}
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

	List primaryFacets() {
		return Collections.unmodifiableList(parentModel == this ? facets()
				: parentModel.primaryFacets());
	}

	List parentFacets() {
		return Collections.unmodifiableList(parentModel.facets());
	}

	// boolean isPrimaryFacet(Perspective p) {
	// return parentFacets().contains(p);
	// }

	Set nonPrimaryFacets() {
		Set nonPrimary = new HashSet(facets());
		nonPrimary.removeAll(primaryFacets());
		return nonPrimary;
	}

	// double pseudoRsquared(Perspective caused) {
	// // Util.print("\npseudoRsquared " + caused + " " + this + "\n"
	// // + printGraph(false));
	// assert observed.facets.equals(predicted.facets);
	// return observed.pseudoRsquared(predicted.getDistribution(), caused);
	// }

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

	boolean optimizeWeight(Explanation base) {
		int argIndex = 0;
		double maxOdds = Double.POSITIVE_INFINITY;
		int maxCause = -1;
		int maxCaused = -1;
		for (int bit = 0; bit < nFacets(); bit++) {
			double odds = predicted.odds(bit, bit);
			if (odds < maxOdds) {
				maxOdds = odds;
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
			double odds = predicted.odds(causeNode, causedNode);
			if (baseModel != null && baseModel.hasEdge(cause, caused)) {
				// delta += NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE
				// * (baseModel.getWeight(cause, caused) - (predicted
				// .getWeight(cause, caused) + delta));
				odds = 1;
			}
			if (odds < maxOdds) {
				maxOdds = odds;
				maxCause = causeNode;
				maxCaused = causedNode;
			}
			argIndex++;
		}
		boolean result = maxCause >= 0;
		if (result) {
			// Util.print("ff "+maxCause+" "+maxCaused+" "+maxDelta);
			// NonAlchemyModel.nOW++;
			double weight = predicted.getWeight(maxCause, maxCaused)
					+ Math.log(observed.odds(maxCause, maxCaused) / maxOdds);
			assert !Double.isNaN(weight) : predicted.getWeight(maxCause,
					maxCaused)
					+ " " + maxOdds + " " + observed.odds(maxCause, maxCaused);
			predicted.setWeight(maxCause, maxCaused, weight);
		}
		// Util.print(Util.valueOfDeep(getObservedDistribution()));
		// Util.print("getInitialWeights " + Util.valueOfDeep(result) + " base="
		// + base);
		// Util.print("getweights="+Util.valueOfDeep(predicted.getWeights()));
		return result;
	}

	// private double expDelta(int causeNode, int causedNode) {
	// double[] observedDistribution = observed.getDistribution();
	// double expOn = 0, expOff = 0, obs = 0;
	// for (int state = 0; state < observedDistribution.length; state++) {
	// if (Util.isBit(state, causeNode) && Util.isBit(state, causedNode)) {
	// expOn += predicted.expEnergy(state);
	// obs += observedDistribution[state];
	// } else {
	// expOff += predicted.expEnergy(state);
	// }
	// }
	// // Util.print("hh "+expOn+" "+expOff+" "+obs);
	// obs = Math.max(0.00001, obs);
	// double result = obs * expOff / (expOn * (1 - obs));
	// return result;
	// }

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
		NonAlchemyModel.totalNumGrad = 0;
		// Util.print("selectFacets " + maxModel.facets + "\n");
		Explanation result = FacetSelection.selectFacets(this, EDGE_COST);
		// Util.print("fs "+result);
		result = EdgeSelection.selectEdges(result, EDGE_COST);
		// Util.print("es "+result);
		double prev = NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE;
		if (prev != 0) {
			NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = 0;
			result.learnWeights(parentModel);
			NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = prev;
		}
		long duration = (new Date()).getTime() - start;
		// result.printGraphAndNull();
		// ((NonAlchemyModel) result).stats();
		Util.print("getExplanation duration=" + (duration / 1000) + " nEdges="
				+ result.predicted.nEdges() + " Function Evaluations: "
				+ NonAlchemyModel.totalNumFuns + " Gradients: "
				+ NonAlchemyModel.totalNumGrad + "\n");
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

	protected void printGraphAndNull() {
		Explanation nullModel = nullModel();
		if (nullModel != this) {
			Util.print("\nbuildGraph\n" + nullModel.buildGraph(null));
			nullModel.printGraphAndNull();
		}
		Util.print("\nbuildGraph\n" + buildGraph(null));
		printGraph(false);
		if (parentModel == this) {
			Util.print("(Null Model = this)");
		}
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
		// Util.print("\n" + Util.valueOfDeep(observed.getDistribution()));
		// Util.print("\n" + Util.valueOfDeep(predicted.getDistribution()));
		// Util.print("kl: " + klDivergence());
		// printGraph(false);

		buf.append(">");
		return buf.toString();
	}

	public Graph buildGraph(PerspectiveObserver redrawer) {
		return predicted.buildGraph(observed, parentModel, redrawer);
	}

	public void redraw() {
		Util.print("Explanation.redraw "+this);
		printToFile();
	}
}
