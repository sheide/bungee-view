package edu.cmu.cs.bungee.client.query.tetrad;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.QuerySQL;
import edu.cmu.cs.bungee.client.query.tetrad.GraphicalModel.SimpleEdge;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;

public abstract class Explanation implements PerspectiveObserver {

	// /**
	// * Importance of weight changes in the null model compared to Divergence
	// * over the null model, used to evaluate learned model.
	// */
	// protected static final double NULL_MODEL_ACCURACY_IMPORTANCE = 0;
	public static final double DEFAULT_EDGE_COST = 0.06;
	double edgeCost = DEFAULT_EDGE_COST;

	// If this is too large (1000), onCountMatrix command is too long for
	// server.
	private static final int MAX_CANDIDATES = 20;

	/**
	 * Print nothing
	 */
	static final int NOTHING = 0;

	/**
	 * Print candidate facets, duration, number of edges. and scores
	 */
	static final int STATISTICS = 1;

	/**
	 * Print improvement for each guess
	 */
	static final int IMPROVEMENT = 2;

	/**
	 * Print graph of each guess
	 */
	static final int GRAPH = 3;

	/**
	 * Prints weights & KL for each guess
	 */
	static final int WEIGHTS = 4;

	static final int PRINT_LEVEL = NOTHING;

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
	abstract Explanation getAlternateExplanation(
			List<ItemPredicate> allFacetList);

	/**
	 * @param edges
	 * @return an explanation with different edges
	 */
	abstract Explanation getAlternateExplanation(Set<SimpleEdge> edges);

	protected Explanation(List<ItemPredicate> facets, Set<SimpleEdge> edges,
			List<ItemPredicate> likelyCandidates) {
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
	public Explanation getExplanation(List<ItemPredicate> candidates) {
		// Util.print("selectFacets " + maxModel.facets + "\n");
		Explanation result = FacetSelection.selectFacets(this, candidates,
				edgeCost);
		// Util.print("fs "+result);
		result = EdgeSelection.selectEdges(result, edgeCost, this);

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
	 */
	abstract void learnWeights();

	public List<ItemPredicate> facets() {
		assert predicted.facets.equals(observed.facets) : predicted.facets
				+ " " + observed.facets;
		return predicted.facets;
	}

	protected int facetIndex(ItemPredicate p2) {
		return predicted.facetIndex(p2);
	}

	protected int facetIndexOrNot(ItemPredicate facet) {
		return predicted.facetIndexOrNot(facet);
	}

	double klDivergence() {
		double result = observed.klDivergenceFromLog(predicted
				.getLogDistribution());
		return result;
	}

	abstract double improvement(Explanation previous, double threshold1,
			List<ItemPredicate> primaryFacets);

	protected double getRNormalizedWeightOrZero(ItemPredicate cause,
			ItemPredicate caused) {
		return getRNormalizedWeight(cause, caused);
	}

	double getRNormalizedWeight(ItemPredicate cause, ItemPredicate caused) {
		return getRNormalizedWeights()[facetIndex(cause)][facetIndex(caused)];
	}

	void printToFile(Explanation nullModel) {
		edu.cmu.cs.bungee.piccoloUtils.gui.Graph.printMe(buildGraph(null,
				nullModel, true), Util.convertForFilename(toString()));
	}

	public static List<ItemPredicate> relevantFacets(Perspective popupFacet) {
		SortedSet<Perspective> primaryFacets1 = popupFacet.query()
				.allRestrictions();
		primaryFacets1.add(popupFacet);
		// TopTags topTags = popupFacet.query().topTags(2 * MAX_FACETS);
		// for (Iterator it = topTags.topIterator(); it.hasNext()
		// && primaryFacets1.size() < MAX_FACETS;) {
		// TagRelevance tag = (TagRelevance) it.next();
		// Perspective p = (Perspective) tag.tag.object;
		// primaryFacets1.add(p);
		removeAncestorFacets(primaryFacets1, popupFacet);
		// }

		SortedSet<ItemPredicate> combos = Perspective.coalesce(primaryFacets1,
				true);
		// Util.print("rf " + combos);
		return new ArrayList<ItemPredicate>(combos);
	}

	private static void removeAncestorFacets(Collection<Perspective> primary,
			Perspective popupFacet) {
		for (Iterator<Perspective> it = new ArrayList<Perspective>(primary)
				.iterator(); it.hasNext();) {
			Perspective facet = it.next();
			Collection<Perspective> ancestors = facet.ancestors();
			// Never remove popupFacet
			ancestors.remove(popupFacet);
			primary.removeAll(ancestors);
		}
	}

	static <F extends ItemPredicate> List<ItemPredicate> candidateFacets(
			List<F> primaryFacets, boolean excludePrimary) {
		Query query = ((ItemPredicate) Util.some(primaryFacets)).query();
		List<Perspective> topMut = query.topMutInf(QuerySQL.itemPredsSQLexpr(
				null, primaryFacets, true, "?").toString()
		// Query.getItemPredicateIDs(primaryFacets)
				, MAX_CANDIDATES);
		List<ItemPredicate> result = new LinkedList<ItemPredicate>();
		for (Perspective p : topMut) {
			result.add(p);
		}
		if (excludePrimary)
			result.removeAll(primaryFacets);
		else
			result.addAll(primaryFacets);

		int queryCount = query.getTotalCount();
		for (Iterator<ItemPredicate> it = result.iterator(); it.hasNext();) {
			ItemPredicate candidate = it.next();
			int totalCount = candidate.getTotalCount();
			if (totalCount == queryCount)
				it.remove();
			else if (candidate.getNameIfPossible() == null)
				query.importFacet(((Perspective) candidate).getID());
		}

		if (PRINT_LEVEL >= Explanation.STATISTICS) {
			Util.print("candidateFacets " + primaryFacets + " => " + result);
		}
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

	protected double[] Rs;

	protected double[] getRs() {
		if (Rs == null) {
			Rs = new double[nFacets()];
			for (int cause = 0; cause < nFacets(); cause++) {
				ItemPredicate causeP = facets().get(cause);
				double r2 = pseudoRsquared(causeP);
				Rs[cause] = Math.sqrt(Math.abs(r2)) * Util.sgn(r2);
			}
			// Util.print("getRs " + Util.valueOfDeep(Rs) + " " + this);
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
	private double pseudoRsquared(ItemPredicate p2) {
		// PRINT_RSQUARED=predicted.nEdges()==0&&caused.getID()==150&&nFacets()==
		// 4;
		double residuals = 0;
		double baseResiduals = 0;
		int causedIndex = facetIndex(p2);
		List<ItemPredicate> causeds = new ArrayList<ItemPredicate>(1);
		causeds.add(p2);
		double[] marginal = observed.getMarginal(causeds);
		if (marginal[1] == 0 || marginal[0] == 0)
			return 0;
		double unconditional = marginal[1] / (marginal[0] + marginal[1]);
		if (PRINT_RSQUARED) {
			for (Iterator<ItemPredicate> it = facets().iterator(); it.hasNext();) {
				ItemPredicate p = it.next();
				if (p != p2) {
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
					+ p2
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

	private static final int RAW_WEIGHT = 1;
	private static final int MEAN_DELTA_R = 2;
	private static final int MARGINAL_DELTA_R = 3;

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
				ItemPredicate causedP = facets().get(caused);
				double sum = 0;
				for (int cause = 0; cause < nFacets(); cause++) {
					if (caused != cause && predicted.hasEdge(cause, caused)) {
						ItemPredicate causeP = facets().get(cause);

						double w = computeRNormalizedWeight(causeP, causedP);
						// double w = predicted.getWeight(causeP, causedP);

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

	private double computeRNormalizedWeight(ItemPredicate cause,
			ItemPredicate caused) {
		double result = Double.NaN;
		switch (RAW_WEIGHT) {
		case RAW_WEIGHT:
			result = predicted.getWeight(cause, caused);
			break;
		case MEAN_DELTA_R:
			result = meanDeltaR(cause, caused);
			break;
		case MARGINAL_DELTA_R:
			result = marginalDeltaR(cause, caused);
			break;

		default:
			assert false;
			break;
		}
		return result;
	}

	/**
	 * Let's try adding this link last.
	 */
	private double marginalDeltaR(ItemPredicate cause, ItemPredicate caused) {
		double with = R(caused);
		Set<SimpleEdge> edges = predicted.getEdges(false);
		edges.remove(GraphicalModel.getEdge(cause, caused));
		Explanation withoutModel = getAlternateExplanation(edges);
		double without = withoutModel.R(caused);

		// Util.print(" averageSumR2 " + without + " => " + with + " + " + cause
		// + " => " + caused + " " + withoutModel + " " + this);

		// If lbfgs works better for one model than another, just hope
		// the errors average out.
		double result = Util.sgn(predicted.getWeight(cause, caused))
				* Math.max(0, with - without);

		// Util.print("averageSumR2 " + predicted.nEdges() + " " + result +
		// " + "
		// + cause + " => " + caused + " " + this);

		assert -1 <= result && result <= 1 : with + " " + without;
		return result;
	}

	/**
	 * I read in some article that averaging over all the orders for adding
	 * edges is the best way to determine which causes are momst important.
	 * However, it doesn't correspond to how important causes are in the current
	 * model, so it's misleading to display.
	 */
	private double meanDeltaR(ItemPredicate cause, ItemPredicate caused) {
		int nPerm = 0;
		double sumR2 = 0;
		Collection<ItemPredicate> otherCauses = new LinkedList<ItemPredicate>(
				predicted.getCauses(caused));
		otherCauses.remove(cause);
		for (Iterator<List<ItemPredicate>> combIt = new Util.CombinationIterator<ItemPredicate>(
				otherCauses); combIt.hasNext();) {
			Collection<ItemPredicate> x = combIt.next();

			Set<SimpleEdge> edges = predicted.getEdges(false);
			edges.removeAll(GraphicalModel.getEdgesTo(x, caused));
			Explanation withModel = x.isEmpty() ? this
					: getAlternateExplanation(edges);
			double with = withModel.R(caused);
			edges.remove(GraphicalModel.getEdge(cause, caused));
			Explanation withoutModel = getAlternateExplanation(edges);
			double without = withoutModel.R(caused);

			Util.print(" averageSumR2 " + without + " => " + with + " " + x
					+ " + " + cause + " => " + caused + " " + withoutModel
					+ " " + withModel);

			// If lbfgs works better for one model than another, just hope
			// the errors average out.
			if (with > without)
				sumR2 += with - without;
			nPerm++;
		}
		double result = Util.sgn(predicted.getWeight(cause, caused)) * sumR2
				/ nPerm;

		Util.print("averageSumR2 " + predicted.nEdges() + " " + result + " + "
				+ cause + " => " + caused + " " + this);

		assert -1 <= result && result <= 1 : sumR2 + " " + nPerm;
		return result;
	}

	void initializeWeights(Explanation base) {
		GraphicalModel baseModel = base.predicted;
		for (Iterator<int[]> it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = it.next();
			int causeNode = edge[0];
			int causedNode = edge[1];
			ItemPredicate cause = facets().get(causeNode);
			ItemPredicate caused = facets().get(causedNode);
			if (baseModel.hasEdge(cause, caused)) {
				predicted.setWeight(causeNode, causedNode, baseModel.getWeight(
						cause, caused));
			}
		}
		for (Iterator<ItemPredicate> it = facets().iterator(); it.hasNext();) {
			ItemPredicate p = it.next();
			if (baseModel.hasEdge(p, p)) {
				predicted.setWeight(p, p, baseModel.getWeight(p, p));
			}
		}
		predicted.resetWeights();
	}

	// void randomizeWeights() {
	// for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
	// int[] edge = (int[]) it.next();
	// int causeNode = edge[0];
	// int causedNode = edge[1];
	// predicted.setWeight(causeNode, causedNode, Math.random()-0.5);
	// }
	// for (Iterator it = facets().iterator(); it.hasNext();) {
	// ItemPredicate p = (ItemPredicate) it.next();
	// predicted.setWeight(p, p, Math.random()-0.5);
	// }
	// predicted.resetWeights();
	// }

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

	protected ItemPredicate getFacet(int i) {
		return predicted.getFacet(i);
	}

	void printTable(ItemPredicate p) {
		boolean prev = PRINT_RSQUARED;
		PRINT_RSQUARED = true;
		pseudoRsquared(p);
		PRINT_RSQUARED = prev;
	}

	boolean printStats(Explanation nullModel) {
		boolean result = nFacets() > nullModel.nFacets();
		// Util.print(result+" "+this+" "+nullModel);
		if (result) {
			List<ItemPredicate> primaryFacets = nullModel.facets();
			// Query query = ((ItemPredicate) Util.some(primaryFacets)).query();
			// Perspective facet1 = (Perspective) primaryFacets.get(0);
			// Perspective facet2 = (Perspective) primaryFacets.get(1);
			//
			List<ItemPredicate> x = new LinkedList<ItemPredicate>(facets());
			x.removeAll(primaryFacets);
			Perspective p = (Perspective) x.get(0);
			//
			// query.clear();
			// if (facet1.getParent() != null)
			// query.toggleFacet(facet1, 0);
			// if (facet2.getParent() != null)
			// query.toggleFacet(facet2, 0);
			// Perspective parent = p.getParent();
			// result = parent != null;
			// if (result) {
			// parent.displayAncestors();
			// query.updateOnItems(null, 0);
			// query.updateData(false);

			// Try I(X;Y) - I(X;Y|Z)

			// = H(X) + H(Y) - H(X,Y) - H(X,Z) - H(Y,Z) + H(X,Y,Z) + H(Z)

			// Distribution dXZ = distForFacets(facet1, p, null);
			// Distribution dYZ = distForFacets(facet2, p, null);
			// Distribution dXY = distForFacets(facet1, facet2, null);
			// Distribution dX = distForFacets(facet1, null, null);
			// Distribution dY = distForFacets(facet2, null, null);
			// Distribution dZ = distForFacets(p, null, null);
			// Distribution dXYZ = distForFacets(facet1, facet2, p);
			// double deltaMutInf = dX.entropy() + dY.entropy() + dZ.entropy()
			// - dXY.entropy() - dXZ.entropy() - dYZ.entropy()
			// + dXYZ.entropy();

			Util.print(// relevance(p, parent) + "\t"+
					// relevance(p, query) + "\t" +
					observed.correlationHacked(nullModel) + "\t"
							+ (-interactionInformationHacked(nullModel)) + "\t"
							+ improvement(nullModel, 0, primaryFacets) + "\t"
							+ p + "\t" + primaryFacets);
			// Util.print(observed);
			// }
			// query.clear();
		}
		return result;
	}

	// see http://en.wikipedia.org/wiki/Interaction_information
	double interactionInformation() {
		double result = 0;
		int n = facets().size();
		for (Iterator<List<ItemPredicate>> it = new Util.CombinationIterator<ItemPredicate>(
				facets()); it.hasNext();) {
			List<ItemPredicate> subfacets = it.next();
			int m = subfacets.size();
			if (m > 0) {
				double h = Distribution.ensureDist(subfacets).entropy();
				if ((n - m) % 2 == 0)
					result -= h;
				else
					result += h;
			}
		}
		return result;
	}

	double interactionInformation(Explanation nullModel) {
		List<ItemPredicate> prim = nullModel.facets();
		if (prim.size() != 2)
			return Double.NaN;
		List<ItemPredicate> nonPrim = new LinkedList<ItemPredicate>(facets());
		nonPrim.removeAll(prim);

		double result = -observed.entropy()
				- Distribution.ensureDist(nonPrim).entropy()
				+ Distribution.ensureDist(prim).entropy();
		List<ItemPredicate> prim1 = new ArrayList<ItemPredicate>(1);
		prim1.add(prim.get(0));
		List<ItemPredicate> prim2 = new ArrayList<ItemPredicate>(1);
		prim2.add(prim.get(1));
		result -= Distribution.ensureDist(prim1).entropy()
				+ Distribution.ensureDist(prim2).entropy();
		nonPrim.add(prim.get(0));
		Collections.sort(nonPrim);
		result += Distribution.ensureDist(nonPrim).entropy();
		nonPrim.remove(prim.get(0));
		nonPrim.add(prim.get(1));
		Collections.sort(nonPrim);

		result += Distribution.ensureDist(nonPrim).entropy();
		return result;
	}

	double interactionInformationHacked(Explanation nullModel) {
		List<ItemPredicate> prim = nullModel.facets();
		if (prim.size() != 2)
			return Double.NaN;
		List<ItemPredicate> nonPrim = new LinkedList<ItemPredicate>(facets());
		nonPrim.removeAll(prim);

		// Util.print("ii " + observed.entropy() + " " + observed + " "
		// + Distribution.ensureDist(nonPrim).entropy() + " "
		// + Distribution.ensureDist(nonPrim));
		double result = -observed.entropy()
				- Distribution.ensureDist(nonPrim).entropy();
		nonPrim.add(prim.get(0));
		Collections.sort(nonPrim);
		result += Distribution.ensureDist(nonPrim).entropy();
		// Util.print("ii2 " + Distribution.ensureDist(nonPrim).entropy() + " "
		// + Distribution.ensureDist(nonPrim));
		nonPrim.remove(prim.get(0));
		nonPrim.add(prim.get(1));
		Collections.sort(nonPrim);

		result += Distribution.ensureDist(nonPrim).entropy();
		// Util.print("ii3 " + Distribution.ensureDist(nonPrim).entropy() + " "
		// + Distribution.ensureDist(nonPrim));
		return result;
	}

	// private static Distribution distForFacets(Perspective X, Perspective Y,
	// Perspective Z) {
	// List l = new LinkedList();
	// if (X != null)
	// l.add(X);
	// if (Y != null)
	// l.add(Y);
	// if (Z != null)
	// l.add(Z);
	// Collections.sort(l);
	// return Distribution.ensureDist(l);
	// }

	protected double getRNormalizedWeightsTest(Perspective causeP,
			Perspective causedP) {
		double sum = 0;
		for (int cause = 0; cause < nFacets(); cause++) {
			ItemPredicate causeP1 = facets().get(cause);
			if (causedP != causeP1 && predicted.hasEdge(causeP1, causedP)) {

				double w = predicted.getWeight(causeP1, causedP);
				sum += Math.abs(w);
			}
		}
		return Math.abs(predicted.getWeight(causeP, causedP)) / sum;
	}

	// private static double relevance(Perspective p, ItemPredicate universe) {
	// ChiSq2x2 c = ChiSq2x2.getInstance(p, universe.getTotalCount(), universe
	// .getOnCount(), p.getTotalCount(), p.getOnCount(), universe);
	//
	// // ChiSq2x2 expected = c.expected();
	// // double chiSq = c.chiSq();// + "\n" + chiSq + "\t" + chiSq /
	// // c.total()
	// // + " " + c.correlation() + "\n" + expected.printTable() + "\n");
	//
	// double myChi = c.myCramersPhi() / Math.sqrt(universe.getTotalCount());
	// // Util.print(p + c.printTable() + "\n" + myChi);
	// Perspective.TopTags.TagRelevance tagRelevance = new
	// Perspective.TopTags.TagRelevance(
	// c, myChi);
	// return tagRelevance.relevanceScore();
	// }

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

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(Util.shortClassName(this)).append(" ");

		// buf.append(primaryFacets()).append(" + ").append(
		// nonPrimaryFacets());
		buf.append(facets());

		if (false) {
			for (Iterator<int[]> it = predicted.getEdgeIterator(); it.hasNext();) {
				int[] edge = it.next();
				int causeNode = edge[0];
				int causedNode = edge[1];
				if (predicted.hasEdge(causeNode, causedNode)) {
					double weight = predicted.getWeight(causeNode, causedNode);
					ItemPredicate p1 = facets().get(causeNode);
					ItemPredicate p2 = facets().get(causedNode);
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

	public Graph<ItemPredicate> buildGraph(PerspectiveObserver redrawer,
			Explanation nullModel, boolean debug) {
		return predicted.buildGraph(getRs(), getRNormalizedWeights(),
				klDivergence(), nullModel, redrawer, debug);
	}

	protected String printGraph() {
		return predicted.printGraph(getRs(), getRNormalizedWeights(),
				klDivergence());
	}

	double R(ItemPredicate facet) {
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

	public abstract Graph<ItemPredicate> buildGraph(PerspectiveObserver redrawer,
			Perspective popupFacet, boolean debug);

	// public void writeVennMasterFile(Collection primaryFacets) {
	// try {
	// BufferedWriter writer = Util.getWriter("c:\\\\temp.list");
	// List facets = facets();
	// for (Iterator it = facets.iterator(); it.hasNext();) {
	// Perspective p = (Perspective) it.next();
	// printTable(p);
	// }
	// int[] counts = observed.counts;
	// for (int i = 0; i < counts.length; i++) {
	// int count = counts[i];
	// List primaryNames = new LinkedList();
	// String suffix = "";
	// for (int j = 0; j < facets.size(); j++) {
	// if (Util.isBit(i, j)) {
	// Perspective p = (Perspective) facets.get(j);
	// String name = edu.cmu.cs.bungee.client.query.tetrad.Tetrad
	// .computeTetradName(p, null);
	// if (primaryFacets.contains(p)) {
	// primaryNames.add(name);
	// } else {
	// suffix += name;
	// }
	// }
	// }
	// String itemPrefix = "item" + i + "-";
	// for (int j = 0; j < count; j++) {
	// if (Math.random() < 0.1) {
	// for (Iterator it2 = primaryNames.iterator(); it2
	// .hasNext();) {
	// String name = (String) it2.next();
	// writer.write(itemPrefix + j + "\t" + name + suffix
	// + "\n");
	//
	// }
	//
	// }
	// }
	// }
	// writer.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	public Distribution getObservedDestribution() {
		return observed;
	}

	public Distribution getPredictedDestribution() {
		return predicted;
	}

	public int nUsedFacets() {
		return predicted.nUsedFacets();
	}

	public List<ItemPredicate> unusedFacets() {
		return predicted.unusedFacets();
	}
}
