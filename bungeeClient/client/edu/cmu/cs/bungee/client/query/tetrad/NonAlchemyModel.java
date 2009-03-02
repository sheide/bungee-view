package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.Util;

import pal.mathx.ConjugateGradientSearch;
import pal.mathx.MFWithGradient;
import pal.mathx.MultivariateFunction;

import lbfgs.LBFGS;
import lbfgs.LBFGS.ExceptionWithIflag;

public class NonAlchemyModel extends Explanation implements
// MultivariateFunction
		MFWithGradient {

	// static int nSetWeights = 0;
	// static int nNoopSetWeights = 0;
	// static int nSetWeight = 0;
	// static int nExpWeight = 0;
	// static int nLogPredictedDistribution = 0;
	// static int nGetDistribution = 0;
	// static int expectedW = 0;
	// static int expectedS = 0;
	static int totalNumGrad;
	// static int nEnergy;
	// static int nExpEnergy;
	// static int nZ;
	// static int nOW;
	// static int nEvalNgrad;
	// static int nNoOpGrad;
	// static int nNoOpEval;
	private double cachedEval = Double.NaN;
	private double[] cachedGradient;
	//
	// void stats() {
	// Util.print("nSetWeights " + nSetWeights);
	// Util.print("nNoopSetWeights " + nNoopSetWeights);
	// Util.print("nSetWeight " + nSetWeight);
	// Util.print("nExpWeight " + nExpWeight);
	// Util.print("nLogPredictedDistribution " + nLogPredictedDistribution);
	// Util.print("nGetDistribution " + nGetDistribution);
	// Util.print("expectedW " + expectedW);
	// Util.print("expectedS " + expectedS);
	// Util.print("nEnergy " + nEnergy);
	// Util.print("nExpEnergy " + nExpEnergy);
	// Util.print("nZ " + nZ);
	// Util.print("nOW " + nOW);
	// Util.print("nEvalNgrad " + nEvalNgrad);
	// Util.print("nNoOpGrad " + nNoOpGrad);
	// Util.print("nNoOpEval " + nNoOpEval);
	// }

	private static final int BURN_IN = 100;// 500;
	/**
	 * Importance of weight changes in the null model compared to accuracy over
	 * the full model, used during learnWeights.
	 */
	static double WEIGHT_STABILITY_IMPORTANCE = 0;// 1e-2;
	static final double WEIGHT_STABILITY_SMOOTHNESS = 1e-9;
	static final boolean USE_SIGMOID = true;

	private static Map explanations = new HashMap();

	static Explanation getInstance(List facets, Set edges, Explanation base,
			List likelyCandidates) {
		if (edges == null)
			edges = GraphicalModel.allEdges(facets, facets);
		Object args = args(base == null ? facets : base.facets(), facets, edges);
		Explanation prev = (Explanation) explanations.get(args);
		if (prev == null)
			prev = new NonAlchemyModel(facets, edges, base, likelyCandidates);
		// if (base != null)
		// Util.print("gi " + prev + " " + " " + prev.nullModel + " " + base
		// + " " + base.nullModel);
		// assert Double.isNaN(((NonAlchemyModel)prev).cachedEval);
		return prev;
	}

	static Explanation getInstance(List facets, Set edges) {
		// Util.print("Expl.getInst " + facets);
		Explanation prev = null;
		if (edges == null)
			edges = GraphicalModel.allEdges(facets, facets);
		List args = args(facets, facets, edges);
		args = args.subList(1, 4);
		for (Iterator it = explanations.entrySet().iterator(); it.hasNext()
				&& prev == null;) {
			Map.Entry entry = (Map.Entry) it.next();
			List prevArgs = (List) entry.getKey();
			// if (args.equals(prevArgs.subList(1, 3)))
			if (args.equals(prevArgs.subList(1, 4)))
				prev = (Explanation) entry.getValue();
		}
		if (prev == null)
			prev = new NonAlchemyModel(facets, edges, null, null);
		return prev;
	}

	private void cache() {
		Object args = args(parentModel.facets(), facets(), predicted.getEdges());
		Explanation prev = (Explanation) explanations.get(args);
		if (prev == null) {
			// Util.print("caching " + facets());
			explanations.put(args, this);
		} else if (!approxEquals(prev)) {
			printGraphAndNull();
			prev.printGraphAndNull();
			assert false;
		}
	}

	private static List args(List baseFacets, List facets, Set edges) {
		List args = new ArrayList(4);
		List nf = new ArrayList(baseFacets);
		Collections.sort(nf);
		args.add(nf);
		List f = new ArrayList(facets);
		Collections.sort(f);
		args.add(f);
		Set e = new HashSet(edges);
		args.add(e);
		args.add(new Double(NULL_MODEL_ACCURACY_IMPORTANCE));
		return Collections.unmodifiableList(args);
	}

	// public static Explanation getNullExplanation(Perspective popupFacet) {
	// return new NonAlchemyModel(Explanation.primaryFacets(popupFacet), null,
	// null, null);
	// }

	public static Explanation getExplanation(Perspective popupFacet) {
		Explanation result = null;
		List primaryFacets = Explanation.primaryFacets(popupFacet);
		if (primaryFacets.size() > 1) {
			Explanation nullModel = NonAlchemyModel.getInstance(primaryFacets,
					null, null, null /*
									 * Explanation.candidateFacets(primaryFacets,
									 * false)
									 */);
			result = nullModel.getExplanation();
		}
		return result;
	}

	Explanation getAlternateExplanation(List facets, List likelyCandidates) {
		return NonAlchemyModel
				.getInstance(facets, null, this, likelyCandidates);
	}

	Explanation getAlternateExplanation(Set edges) {
		// Util.print("getAlternateExplanation " + this + " " + edges);
		return NonAlchemyModel.getInstance(facets(), edges, this, null);
	}

	protected NonAlchemyModel(List facets, Set edges, Explanation base,
			List likelyCandidates) {
		super(facets, edges, base, likelyCandidates);
		learnWeights(base);
		cache();
	}

	static double epsilon = 1E-12;

	static int totalNumFuns = 0;

	protected void learnWeights(Explanation base) {
		boolean debug = false;
		long start = debug ? new Date().getTime() : 0;

		if (base != null)
			initializeWeights(base);

		if (debug)
			Util.print("\ninitial weights  KL "
					+
					// observed.unnormalizedKLdivergence(predicted
					// .logPredictedDistribution())
					klDivergence() + " "
					+ Util.valueOfDeep(predicted.getWeights()));

		for (int i = 0; i < BURN_IN; i++) {
			if (!optimizeWeight(base))
				break;
			// double fx = observed.unnormalizedKLdivergence(predicted
			// .logPredictedDistribution());
			// if (Math.abs(fx - prev) < epsilon)
			// break;
			// prev = fx;
		}
		double[] sw = predicted.getWeights();

		if (debug) {
			double kl = klDivergence();
			Util.print("burned-in weights " + (new Date().getTime() - start)
					+ "ms, " + BURN_IN + " iterations, KL " + kl + " "
					+ Util.valueOfDeep(sw));
		}

		 double[] weights = cgSearch(sw);
//		double[] weights = lbfgsSearch(sw);

		setWeights(weights);
	}

	private double[] cgSearch(double[] sw) {
		ConjugateGradientSearch search = new ConjugateGradientSearch();
		double[] weights = search.findMinimumArgs(this, sw, epsilon, epsilon);

		// if (debug) {
		// Util.print("final weights  " + (new Date().getTime() - start)
		// + "ms, " + (search.numFun + BURN_IN) + " iterations, KL "
		// + klDivergence() + " " + Util.valueOfDeep(weights));
		// Util.print("wsi " + NonAlchemyModel.WEIGHT_STABILITY_IMPORTANCE
		// + "\n" + this + " " + base);
		// }

		totalNumFuns += search.numFun + BURN_IN;
		totalNumGrad += search.numGrad;
		return weights;
	}

	private double[] lbfgsSearch(double[] sw) {
		boolean useDiag = false;
		double[] grad = new double[sw.length];
		double[] diag = new double[sw.length];
		double f = evaluate(sw, grad);
		if (useDiag)
			compute2ndGradient(diag);
		int[] iFlag = { 0 };
		int[] printInterval = { -1, 3 };
		// if (nFacets()>6)printInterval[0]=1;
		try {
			LBFGS.lbfgs(sw.length, 3, sw, f, grad, useDiag, diag,
					printInterval, 1e-8, epsilon, iFlag);
			while (iFlag[0] > 0) {
				for (int i = 0; i < sw.length; i++) {
					sw[i] = Util.constrain(sw[i], -100, 100);
				}
				f = evaluate(sw, grad);
				if (useDiag)
					compute2ndGradient(diag);
				LBFGS.lbfgs(sw.length, 3, sw, f, grad, useDiag, diag,
						printInterval, 1e-8, epsilon, iFlag);
			}
		} catch (ExceptionWithIflag e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (iFlag[0] < 0) {
			Util.err("LBFGS barfed: " + iFlag[0]);
		}

		totalNumFuns += LBFGS.nfevaluations() + BURN_IN;
		totalNumGrad += LBFGS.nfevaluations();
		return sw;
	}

	boolean setWeights(double[] argument) {
		boolean result = predicted.setWeights(argument);
		if (result) {
			cachedEval = Double.NaN;
			getCachedGradient()[0] = Double.NaN;
		}
		return result;
	}

	public double evaluate(double[] argument) {
		setWeights(argument);
		if (!Double.isNaN(cachedEval))
			return cachedEval;
		// predicted.printGraph(getObservedDistribution());
		// Util.print("eval "+nullModel.facets()+" "+nullModel+" "+this);

		double result = klDivergence();
		if (WEIGHT_STABILITY_IMPORTANCE > 0) {
			double change = predicted.weightSpaceChange(parentModel.predicted,
					parentModel.facets(), null, null, true);
			change = Math.log(Math.E + change * WEIGHT_STABILITY_IMPORTANCE);
			result *= change;
		}
		cachedEval = result;
		// if (nFacets()>6)
		// Util.print("evaluate " + cachedEval+" "+Util.valueOfDeep(argument));
		return result;
	}

	public double getLowerBound(int n) {
		return -100;
	}

	public double getUpperBound(int n) {
		return 100;
	}

	public void computeGradient(double[] argument, double[] gradient) {
		setWeights(argument);
		computeGradient(gradient);
	}

	public double evaluate(double[] argument, double[] gradient) {
		// nEvalNgrad++;
		// Util.print("evalNgrad");
		double result = evaluate(argument);
		computeGradient(gradient);
		return result;
	}

	private double[] getCachedGradient() {
		if (cachedGradient == null) {
			cachedGradient = new double[getNumArguments()];
			cachedGradient[0] = Double.NaN;
		}
		return cachedGradient;
	}

	private void computeGradient(double[] gradient) {
		if (!Double.isNaN(getCachedGradient()[0])) {
			System.arraycopy(getCachedGradient(), 0, gradient, 0,
					gradient.length);
			return;
		}
		double[] predictedDistribution = predicted.getDistribution();
		double[] observedDistribution = observed.getDistribution();
		Arrays.fill(gradient, 0);

		int[][] sw = predicted.stateWeights();
		for (int state = 0; state < sw.length; state++) {
			double err = predictedDistribution[state]
					- observedDistribution[state];
			int[] weights = sw[state];
			for (int w = 0; w < weights.length; w++) {
				gradient[weights[w]] += err;
			}
		}
		System.arraycopy(gradient, 0, getCachedGradient(), 0, gradient.length);
		// // Util.print("cg "+Util.valueOfDeep(gradient));
	}

	public void compute2ndGradient(double[] argument, double[] gradient) {
		setWeights(argument);
		compute2ndGradient(gradient);
	}

	private void compute2ndGradient(double[] gradient) {
		double denom = predicted.z() * predicted.z();
		int w = 0;
		for (int cause = 0; cause < nFacets(); cause++) {
			for (int caused = cause; caused < nFacets(); caused++) {
				if (predicted.hasEdge(cause, caused)) {
					double expEon = 0;
					double expEoff = 0;
					for (int state = 0; state < predicted.nStates(); state++) {
						double expE = predicted.expEnergy(state);
						if (Util.isBit(state, cause)
								&& Util.isBit(state, caused)) {
							expEon += expE;
						} else {
							expEoff += expE;
						}
					}
					gradient[w++] = expEon * expEoff / denom;
				}
			}
		}
		Util.print("c2g " + Util.valueOfDeep(gradient));
	}

	static class CompareCount implements Comparator {

		public int compare(Object data1, Object data2) {
			int result = Util.sgn(value(data1) - value(data2));
			if (result == 0)
				result = Util.sgn(id(data1) - id(data2));
			return result;
		}

		private int id(Object data) {
			return ((Perspective) data).getID();
		}

		private double value(Object data) {
			return ((Perspective) data).getTotalCount();
		}
	}

	public static void test(Query query, int nCandidates) {
		if (nCandidates <= 0)
			return;
		int minCount = 0;
		SortedSet topPerspectives = new TreeSet(new CompareCount());
		for (int i = 1; i <= query.nAttributes; i++) {
			Perspective facetType = query.findPerspective(i);
			for (Iterator it = facetType.getChildIterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				int count = p.getTotalCount();
				if (count > minCount) {
					topPerspectives.add(p);
					if (topPerspectives.size() > nCandidates)
						topPerspectives.remove(topPerspectives.first());
					if (topPerspectives.size() >= nCandidates)
						minCount = ((Perspective) topPerspectives.first())
								.getTotalCount();
					// Util.print("tp "+" "+count+" "+p);
				}
			}
		}
		int[] counts = new int[50];
		for (Iterator it = topPerspectives.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			Explanation result = null;
			List primaryFacets = Explanation.primaryFacets(p);
			List candidateFacets = Explanation.candidateFacets(primaryFacets,
					true);
			Util.print(p + " " + p.getTotalCount() + " " + primaryFacets + " "
					+ candidateFacets);
			for (Iterator it2 = candidateFacets.iterator(); it2.hasNext();) {
				Perspective candidate = (Perspective) it2.next();
				List pair = new ArrayList(2);
				pair.add(p);
				pair.add(candidate);
				Collections.sort(pair);
				Util.print(pair);
				Explanation nullModel = NonAlchemyModel.getInstance(pair, null,
						null, null);
				result = nullModel.getExplanation();
				int nEdges = result.predicted.nEdges();
				counts[nEdges]++;
				if (nEdges > 1)
					result.printToFile();
			}
		}
		Util.print("nEdges distribution: " + Util.valueOfDeep(counts));
	}

}