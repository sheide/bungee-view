package edu.cmu.cs.bungee.client.query.tetrad;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;

public abstract class Explanation implements PerspectiveObserver {

	/**
	 * Importance of weight changes in the null model compared to Divergence
	 * over the null model, used to evaluate learned model.
	 */
	protected static double NULL_MODEL_ACCURACY_IMPORTANCE = 0;
	protected static final double EDGE_COST = 0.06;
	private static final int MAX_CANDIDATES = 20;

	/**
	 * 0 Print candidate facets, duration, & number of edges
	 * 
	 * 1 Print improvement for each guess
	 * 
	 * 2 Print graph of each guess
	 * 
	 * 3 Prints weights & KL for each guess
	 */
	static final int PRINT_LEVEL = 3;
	private static boolean PRINT_RSQUARED = false;
	static boolean PRINT_CANDIDATES_TO_FILE = false;

	// protected final Explanation parentModel;
	protected final GraphicalModel predicted;
	protected final Distribution observed;

	protected static int totalNumFuns = 0;

	// protected static int totalNumGrad;
	// protected static int totalNumLineSearches;

	/**
	 * @return an explanation with different facets
	 */
	abstract Explanation getAlternateExplanation(List facets);

	/**
	 * @param edges
	 * @return an explanation with different edges
	 */
	abstract Explanation getAlternateExplanation(Set edges);

	protected Explanation(List facets, Set edges, List likelyCandidates) {
		observed = Distribution.cacheCandidateDistributions(facets,
				likelyCandidates);
		predicted = new GraphicalModel(facets, edges, true, observed.totalCount);
	}

	// Explanation nullModel() {
	// return this == parentModel ? this : parentModel.nullModel();
	// }

	/**
	 * add facets to explain this null model
	 */
	public Explanation getExplanation(List candidates) {
		// Util.print("selectFacets " + maxModel.facets + "\n");
		Explanation result = FacetSelection.selectFacets(this, candidates,
				EDGE_COST);
		// Util.print("fs "+result);
		result = EdgeSelection.selectEdges(result, EDGE_COST, this);
		// Util.print("es "+result);
		// double prev = NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE;
		// if (prev != 0) {
		// NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = 0;
		// result.learnWeights(parentModel);
		// NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE = prev;
		// }
		return result;
	}

	public boolean approxEquals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Explanation other = (Explanation) obj;
		// if (parentModel == this) {
		// if (other.parentModel != other)
		// return false;
		// } else if (other.parentModel == other)
		// return false;
		// else if (!parentModel.approxEquals(other.parentModel))
		// return false;
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

	public List facets() {
		assert predicted.facets.equals(observed.facets) : predicted.facets
				+ " " + observed.facets;
		return predicted.facets;
	}

	protected int facetIndex(Perspective facet) {
		return predicted.facetIndex(facet);
	}

	protected int facetIndexOrNot(Perspective facet) {
		return predicted.facetIndexOrNot(facet);
	}

	double klDivergence() {
		double result = observed.klDivergenceFromLog(predicted
				.getLogDistribution());
		return result;
	}

	abstract double improvement(Explanation previous, double threshold1,
			List primaryFacets);

	protected double getRNormalizedWeightOrZero(Perspective cause,
			Perspective caused) {
		return getRNormalizedWeight(cause, caused);
	}

	double getRNormalizedWeight(Perspective cause, Perspective caused) {
		return getRNormalizedWeights()[facetIndex(cause)][facetIndex(caused)];
	}

	void printToFile(Explanation nullModel) {
		edu.cmu.cs.bungee.piccoloUtils.gui.Graph.printMe(buildGraph(null,
				nullModel), Util.convertForFilename(toString()));
	}

	public static List relevantFacets(Perspective popupFacet) {
		return new ArrayList(relevantFacetsSet(popupFacet));
	}

	public static Set relevantFacetsSet(Perspective popupFacet) {
		SortedSet primaryFacets1 = popupFacet.query().allRestrictions();
		primaryFacets1.add(popupFacet);
		// TopTags topTags = popupFacet.query().topTags(2 * MAX_FACETS);
		// for (Iterator it = topTags.topIterator(); it.hasNext()
		// && primaryFacets1.size() < MAX_FACETS;) {
		// TagRelevance tag = (TagRelevance) it.next();
		// Perspective p = (Perspective) tag.tag.object;
		// primaryFacets1.add(p);
		removeAncestorFacets(primaryFacets1, popupFacet);
		// }
		return primaryFacets1;
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
		// List topTags = query.topMutInf(primaryFacets, MAX_CANDIDATES);
		Set candidates = new HashSet(query.topMutInf(primaryFacets,
				MAX_CANDIDATES));
		// for (Iterator it = topTags.iterator(); it.hasNext();) {
		// Perspective tr = (Perspective) it.next();
		// if (!primaryFacets.contains(tr)) {
		// candidates.add(tr);
		//
		// // List facets = new LinkedList(primaryFacets);
		// // facets.add(tr);
		// // Collections.sort(facets);
		// // List facet = new ArrayList(1);
		// // facet.add(tr);
		// // Distribution dist = Distribution.getObservedDistribution(
		// // facets, null);
		// // Util.print(tr + " "
		// // + dist.mutualInformation(primaryFacets, facet) + " "
		// // + dist.sumCorrelation(tr));
		// }
		// }
		// result.add(query.findPerspective(402));
		if (excludePrimary)
			candidates.removeAll(primaryFacets);
		else
			candidates.addAll(primaryFacets);

		int queryCount = query.getTotalCount();
		for (Iterator it = candidates.iterator(); it.hasNext();) {
			Perspective candidate = (Perspective) it.next();
			assert candidate.getTotalCount() < queryCount : candidate + " "
					+ primaryFacets;
			if (candidate.getNameIfPossible() == null)
				query.importFacet(candidate.getID());
		}

		ArrayList result = new ArrayList(candidates);
		Collections.sort(result);
		Util.print("candidateFacets " + primaryFacets + " => " + result);
		return result;
	}

	// List primaryFacets() {
	// return parentModel == this ? facets() : parentModel.primaryFacets();
	// }
	//
	// List parentFacets() {
	// return parentModel.facets();
	// }
	//
	// Set nonPrimaryFacets() {
	// Set nonPrimary = new HashSet(facets());
	// nonPrimary.removeAll(primaryFacets());
	// return nonPrimary;
	// }

	private double[] Rs;

	private double[] getRs() {
		if (Rs == null) {
			Rs = new double[nFacets()];
			for (int cause = 0; cause < nFacets(); cause++) {
				Perspective causeP = (Perspective) facets().get(cause);
				double r2 = pseudoRsquared(causeP);
				Rs[cause] = Math.sqrt(Math.abs(r2)) * Util.sgn(r2);
			}
		}
		return Rs;
	}

	/**
	 * Efron's Pseudo R-Squared see
	 * 
	 * http://www.ats.ucla.edu/stat/mult_pkg/faq/general/Psuedo_RSquareds.htm
	 * 
	 * Compares predicted, rather than do a logistic regression
	 */
	private double pseudoRsquared(Perspective caused) {
		// PRINT_RSQUARED=predicted.nEdges()==0&&caused.getID()==150&&nFacets()==
		// 4;
		double residuals = 0;
		double baseResiduals = 0;
		int causedIndex = facetIndex(caused);
		List causeds = new ArrayList(1);
		causeds.add(caused);
		double[] marginal = observed.getMarginal(causeds);
		double unconditional = marginal[1] / (marginal[0] + marginal[1]);
		if (PRINT_RSQUARED) {
			for (Iterator it = facets().iterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (p != caused) {
					String name = p.getNameIfPossible() + "";
					System.out.print(name.substring(0, Math.min(name.length(),
							5))
							+ " ");
				}
			}
			Util
					.print("                observed pred residua uncondi off_nrg  on_nrg   delta");
		}
		int nStates = observed.nStates();
		double[] pred = predicted.getDistribution();
		double[] obs = observed.getDistribution();
		for (int offState = 0; offState < nStates; offState++) {
			if (Util.getBit(offState, causedIndex) == 0) {
				int onState = Util.setBit(offState, causedIndex, true);
				double obsOn = obs[onState];
				double obsOff = obs[offState];
				double predictedTotal = pred[onState] + pred[offState];
				double yHat = predictedTotal > 0 ? pred[onState]
						/ predictedTotal : 0.5;
				double residual = yHat * yHat * obsOff + (1 - yHat)
						* (1 - yHat) * obsOn;
				residuals += residual;
				assert !Double.isNaN(residuals) : residual + " " + yHat + " "
						+ Util.valueOfDeep(predicted);

				// Util.print(yHat * yHat * obsOff + " " + (1 - yHat) * (1 -
				// yHat)
				// * obsOn);

				// We're calculating this state-by-state for the benefit of
				// printing
				double baseResidual = unconditional * unconditional * obsOff
						+ (1 - unconditional) * (1 - unconditional) * obsOn;
				baseResiduals += baseResidual;

				// Util.print(unconditional * unconditional * obsOff);
				// Util.print((1 - unconditional) * (1 - unconditional) *
				// obsOn);

				if (PRINT_RSQUARED) {
					for (int i = 0; i < facets().size(); i++) {
						if (i != causedIndex)
							System.out
									.print(Util.getBit(offState, i) + "     ");
					}
					Util.print(formatInt(obsOn)
							+ " / "
							+ formatInt(obsOn + obsOff)
							+ " = "
							+ formatPercent(obsOn / (obsOn + obsOff))
							+ " "
							+ formatPercent(yHat)
							+ " "
							+ formatResidual(residual)
							+ " "
							+ formatResidual(baseResidual)
							+ " "
							+ formatDouble(Math.log(pred[offState] / pred[0]))
							+ " "
							+ formatDouble(Math.log(pred[onState] / pred[0]))
							+ " "
							+ formatDouble(Math.log(pred[onState]
									/ pred[offState])) + " "
							+ formatDouble(pred[offState]) + " "
							+ formatDouble(pred[onState]));
				}

			}

		}
		// double base = pseudoRsquaredDenom(caused);
		// assert Math.abs(params.yCount() - sumYhat) < 1 : params.yCount() +
		// " "
		// + sumYhat + report + "\n" + yHats();
		double Rsquare = 1 - residuals / baseResiduals;
		if (PRINT_RSQUARED || Double.isNaN(Rsquare)) {
			Util.print("pseudoR="
					+ formatDouble(Math.sqrt(Math.abs(Rsquare))
							* Util.sgn(Rsquare))
					+ " "
					+ caused
					+ " unconditional="
					+ formatPercent(unconditional)
					+ " "
					+ formatResidual(residuals)
					+ "/"
					+ formatResidual(baseResiduals)
					+ "\n"
					+ observed.klDivergence(pred)
					+ " "
					+ observed
							.klDivergence(observed.independenceDistribution())
					+ " " + predicted + " " + predicted.getEdges(false) + "\n");
		}

		// When trying to model a larger distribution, it can end up predicting
		// the primary facets worse than the unconditional average
		assert Rsquare <= 1.0001
				&& (predicted.nEdges() > 0 || Rsquare + 0.1 > 0) : this
				+ " Rsquare=" + Rsquare + " unconditional=" + unconditional
				+ " " + Util.valueOfDeep(predicted) + " residuals=" + residuals
				+ " baseResiduals=" + baseResiduals;

		assert !Double.isNaN(Rsquare) : residuals + " " + baseResiduals;
		assert !Double.isInfinite(Rsquare) : causedIndex + " " + predicted
				+ " " + residuals + " / " + baseResiduals + " "
				+ Util.valueOfDeep(obs);
		Rsquare = Math.max(Rsquare, 0);
		return Rsquare;
	}

	private static final StringAlign align = new StringAlign(7,
			StringAlign.JUST_RIGHT);

	private static final DecimalFormat countFormat = new DecimalFormat(
			"###,###");

	private static final DecimalFormat percentFormat = new DecimalFormat("###%");

	private static final DecimalFormat doubleFormat = new DecimalFormat(
			" #0.000;-#0.000");

	private static String formatPercent(double x) {
		return (new StringAlign(4, StringAlign.JUST_RIGHT)).format(x,
				percentFormat);
		// return percentFormat.format(x);
	}

	private static String formatDouble(double x) {
		String format = align.format(x, doubleFormat);
		// return StringAlign.format(format, null, 6, StringAlign.JUST_LEFT)
		// .toString();
		return format;
	}

	private String formatResidual(double x) {
		if (Double.isNaN(x))
			return "NaN";
		return formatInt(10000 * x / observed.totalCount);
	}

	private String formatInt(double dx) {
		int x = (int) Math.round(dx * observed.totalCount);
		return align.format(x, countFormat);
	}

	private double[][] causedRs;

	protected double[][] getRNormalizedWeights() {
		if (causedRs == null) {
			causedRs = new double[nFacets()][];
			for (int i = 0; i < causedRs.length; i++) {
				causedRs[i] = new double[nFacets()];
			}
			for (int caused = 0; caused < nFacets(); caused++) {
				Perspective causedP = (Perspective) facets().get(caused);
				double sum = 0;
				for (int cause = 0; cause < nFacets(); cause++) {
					if (caused != cause && predicted.hasEdge(cause, caused)) {
						Perspective causeP = (Perspective) facets().get(cause);

						// double w = getRNormalizedWeight1(causeP, causedP);
						double w = predicted.getWeight(causeP, causedP);

						causedRs[cause][caused] = w;
						sum += Math.abs(w);
					}
				}
				if (sum > 0) {
					double correction = R(causedP) / sum;
					for (int cause = 0; cause < nFacets(); cause++) {
						causedRs[cause][caused] *= correction;
					}
				}
			}
			// Util.print("GRNW "+this+" "+Util.valueOfDeep(causedRs));
		}
		return causedRs;
	}

	// /**
	// * I read in some article that averaging over all the orders for adding
	// * edges is the best way to determine which causes are momst important.
	// * However, it doesn't correspond to how important causes are in the
	// current
	// * model, so it's misleading to display.
	// */
	// private double getRNormalizedWeight1(Perspective cause, Perspective
	// caused) {
	// int nPerm = 0;
	// double sumR2 = 0;
	// Collection otherCauses = new LinkedList(predicted.getCauses(caused));
	// otherCauses.remove(cause);
	// for (Iterator combIt = new Util.CombinationIterator(otherCauses); combIt
	// .hasNext();) {
	// Collection x = (Collection) combIt.next();
	//
	// Set edges = predicted.getEdges(false);
	// edges.removeAll(GraphicalModel.getEdgesTo(x, caused));
	// Explanation withModel = x.isEmpty() ? this
	// : getAlternateExplanation(edges);
	// double with = withModel.R(caused);
	// edges.remove(GraphicalModel.getEdge(cause, caused));
	// Explanation withoutModel = getAlternateExplanation(edges);
	// double without = withoutModel.R(caused);
	//
	// Util.print(" averageSumR2 " + without + " => " + with + " " + x
	// + " + " + cause + " => " + caused + " " + withoutModel
	// + " " + withModel);
	//
	// // If lbfgs works better for one model than another, just hope
	// // the errors average out.
	// if (with > without)
	// sumR2 += with - without;
	// nPerm++;
	// }
	// double result = Util.sgn(predicted.getWeight(cause, caused)) * sumR2
	// / nPerm;
	//
	// Util.print("averageSumR2 " + predicted.nEdges() + " " + result + " + "
	// + cause + " => " + caused + " " + this);
	//
	// assert -1 <= result && result <= 1 : sumR2 + " " + nPerm;
	// return result;
	// }

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
		for (Iterator it = facets().iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (baseModel.hasEdge(p, p)) {
				predicted.setWeight(p, p, baseModel.getWeight(p, p));
			}
		}
		predicted.resetWeights();
	}

	/**
	 * dKL/dw = 0 implies observedOn/observedOff = expEOn/expEOff.
	 * 
	 * Only expEOn depends on w, which can be written wOld+deltaW
	 * 
	 * Since this factor occurs in every term,
	 * 
	 * observedOn/observedOff * expEOff/expEOn(wOld) = exp(deltaW)
	 */
	boolean optimizeWeight(int[][] edges) {
		// Util.print(Util.valueOfDeep(observed.getDistribution()));
		double kl = klDivergence();
		boolean isChange = false;
		for (int edge = 0; edge < edges.length; edge++) {
			int[] foo = edges[edge];
			int cause = foo[0];
			int caused = foo[1];
			double delta = Math.log(observed.odds(cause, caused)
					/ predicted.odds(cause, caused));
			// Util.print(" "+observed.odds(cause,
			// caused)+" "+predicted.odds(cause, caused));
			double w = predicted.getWeight(cause, caused);
			isChange |= predicted.setWeight(cause, caused, Util.constrain(w
					+ delta, -GraphicalModel.MAX_WEIGHT,
					GraphicalModel.MAX_WEIGHT));
			// Util.print(" => "+predicted.odds(cause, caused));
			double newkl = klDivergence();
			// Util.print(cause + " " + caused + " " + kl + " " + newkl + " " +
			// w
			// + " " + delta + " "
			// + Util.valueOfDeep(predicted.getWeights()) + " "
			// + Util.valueOfDeep(predicted.getDistribution()));
			assert newkl <= kl * 1.0001 || Math.abs(newkl - kl) < 1e-7 : cause
					+ " " + caused + " " + kl + " " + newkl + " " + w + " "
					+ delta + " " + Util.valueOfDeep(predicted.getWeights())
					+ " " + Util.valueOfDeep(predicted.getDistribution());
			kl = newkl;
		}
		if (isChange)
			predicted.resetWeights();
		return isChange;
	}

	// String printGraph(boolean primaryOnly) {
	// Util.print("obs: " + observed);
	// return predicted.printGraph(observed, primaryOnly ? primaryFacets()
	// : null);
	// }

	protected int nFacets() {
		return predicted.nFacets();
	}

	protected Perspective getFacet(int i) {
		return predicted.getFacet(i);
	}

	void printTable(Perspective p) {
		boolean prev = PRINT_RSQUARED;
		PRINT_RSQUARED = true;
		pseudoRsquared(p);
		PRINT_RSQUARED = prev;
	}

	// protected void printGraphAndNull() {
	// Explanation nullModel = nullModel();
	// if (nullModel != this) {
	// Util.print("\nbuildGraph\n" + nullModel.buildGraph(null));
	// nullModel.printGraphAndNull();
	// }
	// Util.print("\nbuildGraph\n" + buildGraph(null));
	// printGraph();
	// if (parentModel == this) {
	// Util.print("(Null Model = this)");
	// }
	// }

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(Util.shortClassName(this)).append(" ");

		// buf.append(primaryFacets()).append(" + ").append(
		// nonPrimaryFacets());
		buf.append(facets());

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

	// public Graph buildGraph(PerspectiveObserver redrawer) {
	// return predicted.buildGraph(observed, parentModel, redrawer);
	// }

	public Graph buildGraph(PerspectiveObserver redrawer, Explanation nullModel) {
		return predicted.buildGraph(getRs(), getRNormalizedWeights(),
				klDivergence(), nullModel, redrawer);
	}

	protected String printGraph() {
		return predicted.printGraph(getRs(), getRNormalizedWeights(),
				klDivergence());
	}

	double R(Perspective facet) {
		return getRs()[facetIndex(facet)];
	}

	/*
	 * This is just for the sake of files. PopupSummary is the redrawer for
	 * client graphs.
	 */
	public void redraw() {
		// Util.print("Explanation.redraw " + this);
		// printToFile();
	}

	public abstract Graph buildGraph(PerspectiveObserver redrawer,
			Perspective popupFacet);
}
