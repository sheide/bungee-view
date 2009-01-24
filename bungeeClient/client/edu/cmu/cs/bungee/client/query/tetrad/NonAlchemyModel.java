package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;

import pal.mathx.ConjugateGradientSearch;
import pal.mathx.MFWithGradient;
import pal.mathx.MultivariateFunction;

public class NonAlchemyModel extends Explanation implements
// MultivariateFunction
		MFWithGradient {

	private static final int BURN_IN = 100;// 500;
	/**
	 * Importance of weight changes in the null model compared to accuracy over
	 * the full model, used during learnWeights.
	 */
	static double WEIGHT_STABILITY_IMPORTANCE = 1e-8;
	static final double WEIGHT_STABILITY_SMOOTHNESS = 1e-10;

	private static Map explanations = new HashMap();

	static Explanation getInstance(List facets, Set edges, Explanation base,
			List likelyCandidates) {
		if (edges == null)
			edges = GraphicalModel.allEdges(facets, facets);
		Object args = args(base == null ? facets : base.facets(), facets, edges);
		Explanation prev = (Explanation) explanations.get(args);
		if (prev == null)
			prev = new NonAlchemyModel(facets, edges, base, likelyCandidates);
		return prev;
	}

	static Explanation getInstance(List facets, Set edges) {
		// Util.print("Expl.getInst " + facets);
		Explanation prev = null;
		if (edges == null)
			edges = GraphicalModel.allEdges(facets, facets);
		List args = args(facets, facets, edges);
		args = args.subList(1, 3);
		for (Iterator it = explanations.entrySet().iterator(); it.hasNext()
				&& prev == null;) {
			Map.Entry entry = (Map.Entry) it.next();
			List prevArgs = (List) entry.getKey();
			// if (args.equals(prevArgs.subList(1, 3)))
			if (args.equals(prevArgs.subList(1, 3)))
				prev = (Explanation) entry.getValue();
		}
		if (prev == null)
			prev = new NonAlchemyModel(facets, edges, null, null);
		return prev;
	}

	private void cache() {
		Object args = args(nullModel.facets(), facets(), predicted.getEdges());
		Explanation prev = (Explanation) explanations.get(args);
		if (prev == null) {
			// Util.print("caching " + facets());
			explanations.put(args, this);
		} else if (!approxEquals(prev)) {
			printGraph(false);
			prev.printGraph(false);
			assert false;
		}
	}

	private static List args(List baseFacets, List facets, Set edges) {
		List args = new ArrayList(3);
		List nf = new ArrayList(baseFacets);
		Collections.sort(nf);
		args.add(nf);
		List f = new ArrayList(facets);
		Collections.sort(f);
		args.add(f);
		Set e = new HashSet(edges);
		args.add(e);
		return args;
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

	// Explanation getLikeExplanation() {
	// return new NonAlchemyModel(facets(), predicted.getEdges(), this, null);
	// }

	// Explanation getAlternateExplanation(List facets) {
	// return new NonAlchemyModel(facets, null, this, null);
	// }

	Explanation getAlternateExplanation(List facets, List likelyCandidates) {
		return NonAlchemyModel
				.getInstance(facets, null, this, likelyCandidates);
	}

	Explanation getAlternateExplanation(Set edges) {
		return NonAlchemyModel.getInstance(facets(), edges, this, null);
	}

	protected NonAlchemyModel(List facets, Set edges, Explanation base,
			List likelyCandidates) {
		super(facets, edges, base, likelyCandidates);
		cache();
	}

	static double epsilon = 1E-12;

	static int totalNumFuns = 0;

	protected void learnWeights(Explanation base) {
		boolean debug = false;
		long start = debug ? new Date().getTime() : 0;

		// predicted.setWeights(getInitialWeights(base));
		// Util.print("\nlearnWeights: "+this);
		// Util.printStackTrace();
		// if (base!=null)
		// base.printGraph(false);

		if (base != null)
			initializeWeights(base);

		if (debug)
			Util.print("\ninitial weights  KL "
					+
					// observed.unnormalizedKLdivergence(predicted
					// .logPredictedDistribution())
					observed.KLdivergence(predicted.getDistribution()) + " "
					+ Util.valueOfDeep(predicted.getWeights()));

		for (int i = 0; i < BURN_IN; i++) {
			optimizeWeight(base);
			// double fx = observed.unnormalizedKLdivergence(predicted
			// .logPredictedDistribution());
			// if (Math.abs(fx - prev) < epsilon)
			// break;
			// prev = fx;
		}
		double[] sw = predicted.getWeights();

		if (debug) {
			double KL = observed.KLdivergence(predicted.getDistribution());
			Util.print("burned-in weights " + (new Date().getTime() - start)
					+ "ms, " + BURN_IN + " iterations, KL " + KL + " "
					+ Util.valueOfDeep(sw));
		}

		ConjugateGradientSearch search = new ConjugateGradientSearch();
		double[] weights = search.findMinimumArgs(this, sw, epsilon, epsilon);

		if (debug)
			Util.print("final weights  " + (new Date().getTime() - start)
					+ "ms, " + (search.numFun + BURN_IN) + " iterations, KL "
					+ observed.KLdivergence(predicted.getDistribution()) + " "
					+ Util.valueOfDeep(weights));

		totalNumFuns += search.numFun + BURN_IN;

		// double[] initialGradient=new double[initialWeights.length];
		// computeGradient(initialWeights, initialGradient);
		// List added = new LinkedList(facets());
		// added.removeAll(base.facets());
		// if (added.size()==1) {
		// Perspective addedFacet=(Perspective) added.get(0);
		// for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
		// int[] edge= (int[]) it.next();
		// Perspective cause = getFacet(edge[0]);
		// Perspective caused = getFacet(edge[1]);
		// if
		// ((addedFacet==cause||addedFacet==caused)&&(isPrimaryFacet(caused)||
		// isPrimaryFacet(cause))) {
		// grad+=
		// }
		// }
		// Util.print("  "+)
		// }
		predicted.setWeights(weights);

		// double[] grad=new double[weights.length];
		// Util.print("learnWeights KL = "+evaluate(weights,grad));
		// Util.print("grad = "+Util.valueOfDeep(grad));
		// printGraph();
	}

	public double evaluate(double[] argument) {
		// Util.print("evaluate " + Util.valueOfDeep(argument));
		predicted.setWeights(argument);
		// predicted.printGraph(getObservedDistribution());
		// Util.print("eval "+nullModel.facets()+" "+nullModel+" "+this);
		double change = predicted.weightSpaceChange(nullModel.predicted,
				nullModel.facets(), null, null, true);
		// double smoothChange = Math.sqrt(change * change
		// + WEIGHT_STABILITY_SMOOTHNESS);
		return unnormalizedKLdivergence() + change
				* WEIGHT_STABILITY_IMPORTANCE;
	}

	public double getLowerBound(int n) {
		return -100;
	}

	public double getUpperBound(int n) {
		return 100;
	}

	public void computeGradient(double[] argument, double[] gradient) {
		predicted.setWeights(argument);
		computeGradient(gradient);
	}

	// public double evaluate2(double[] argument, double[] gradient) {
	// double result = evaluate(argument);
	// int argIndex;
	// double[] predictedDistribution = predicted.getDistribution();
	// double[] observedDistribution = observed.getDistribution();
	// for (argIndex = 0; argIndex < nFacets(); argIndex++) {
	// gradient[argIndex] = 0;
	// for (int state = 0; state < predicted.nStates(); state++) {
	// if (Util.isBit(state, argIndex)) {
	// gradient[argIndex] += predictedDistribution[state]
	// - observedDistribution[state];
	// }
	// }
	// }
	//
	// for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
	// int[] edge = (int[]) it.next();
	// gradient[argIndex] = 0;
	// for (int state = 0; state < predicted.nStates(); state++) {
	// if (Util.isBit(state, edge[0]) && Util.isBit(state, edge[1])) {
	// gradient[argIndex] += predictedDistribution[state]
	// - observedDistribution[state];
	// }
	// }
	// argIndex++;
	// }
	// return result;
	// }

	public double evaluate(double[] argument, double[] gradient) {
		double result = evaluate(argument);
		computeGradient(gradient);
		return result;
	}

	// private void computeGradient(double[] gradient) {
	// for (int i = 0; i < gradient.length; i++) {
	// gradient[i] = 0;
	// }
	// double[] observedDistribution = observed.getDistribution();
	// // int[][] edgeIndexes = predicted.getEdgeIndexes();
	// int nStates = predicted.nStates();
	// Util.print(Util.valueOfDeep(observedDistribution));
	// Util.print(Util.valueOfDeep(predicted.getWeights()));
	//
	// double[] expEnergies = new double[nStates];
	// double z = 0;
	// for (int state = 0; state < expEnergies.length; state++) {
	// expEnergies[state] = predicted.expEnergy(state);
	// z += expEnergies[state];
	// }
	//
	// int argIndex;
	// for (argIndex = 0; argIndex < nFacets(); argIndex++) {
	// double sum1 = 0;
	// double sum2 = 0;
	// for (int state = 1; state < nStates; state++) {
	// if (Util.isBit(state, argIndex)) {
	// sum1 += expEnergies[state];
	// sum2 += observedDistribution[state];
	// }
	// }
	// gradient[argIndex] = sum1 / z - sum2;
	// }
	// for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
	// int[] edge = (int[]) it.next();
	// int cause = edge[0];
	// int caused = edge[1];
	// double sum1 = 0;
	// double sum2 = 0;
	// for (int state = 1; state < predicted.nStates(); state++) {
	// if (Util.isBit(state, cause) && Util.isBit(state, caused)) {
	// sum1 += expEnergies[state];
	// sum2 += observedDistribution[state];
	// }
	// }
	// gradient[argIndex] = sum1 / z - sum2;
	//
	// // Encourage weights to stay the same
	// Perspective causeP = (Perspective) facets().get(cause);
	// Perspective causedP = (Perspective) facets().get(caused);
	// if (nullModel.predicted.hasEdge(causeP, causedP)) {
	// double w0 = nullModel.predicted.getWeight(causeP, causedP);
	// double w = predicted.getWeight(cause, caused);
	//
	// // double expW = Math.exp(w);
	// // double dw = expW / ((expW + 1) * (expW + 1));
	// // if (w > w0)
	// // dw = -dw;
	//
	// double expW = Math.exp(-w);
	// double expWplus = expW + 1;
	// double expPlusDeltaInverse = 1.0 / expWplus - 1.0
	// / (Math.exp(-w0) + 1);
	// double dw = expW
	// * expPlusDeltaInverse
	// / (expWplus * expWplus * Math.sqrt(expPlusDeltaInverse
	// * expPlusDeltaInverse
	// + WEIGHT_STABILITY_SMOOTHNESS));
	// gradient[argIndex] += dw * WEIGHT_STABILITY_IMPORTANCE;
	// }
	// argIndex++;
	// }
	// }

	private void computeGradient(double[] gradient) {
		double[] predictedDistribution = predicted.getDistribution();
		double[] observedDistribution = observed.getDistribution();
		// int[][] edgeIndexes = predicted.getEdgeIndexes();
		int nStates = predicted.nStates();
		int argIndex;
		for (argIndex = 0; argIndex < nFacets(); argIndex++) {
			gradient[argIndex] = 0;
			for (int state = 0; state < nStates; state++) {
				if (Util.isBit(state, argIndex)) {
					gradient[argIndex] += predictedDistribution[state]
							- observedDistribution[state];
				}
			}
		}
		for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			int cause = edge[0];
			int caused = edge[1];
			gradient[argIndex] = 0;
			for (int state = 0; state < predicted.nStates(); state++) {
				if (Util.isBit(state, cause) && Util.isBit(state, caused)) {
					gradient[argIndex] += predictedDistribution[state]
							- observedDistribution[state];
				}
			}

			// Encourage weights to stay the same
			Perspective causeP = (Perspective) facets().get(cause);
			Perspective causedP = (Perspective) facets().get(caused);
			if (nullModel.predicted.hasEdge(causeP, causedP)) {
				double expw0 = nullModel.predicted.getWeight(causeP, causedP);
				double expw = predicted.getWeight(cause, caused);
				double delta = expw - expw0;

				// double expW = Math.exp(w);
				// double dw = expW / ((expW + 1) * (expW + 1));
				// if (w > w0)
				// dw = -dw;

				// double expWplus = expw + 1;
				// double expPlusDeltaInverse = 1.0 / expWplus - 1.0
				// / (expw0 + 1);
				// double dw = expw
				// * expPlusDeltaInverse
				// / (expWplus * expWplus * Math.sqrt(expPlusDeltaInverse
				// * expPlusDeltaInverse
				// + WEIGHT_STABILITY_SMOOTHNESS));

				double dw = delta
						/ Math
								.sqrt(delta * delta
										+ WEIGHT_STABILITY_SMOOTHNESS);

				// Util.print("grad " + dw
				// +" "+causeP+" "+causedP+" "+gradient[argIndex]);
				// double delta=w-w0;
				// dw=delta/Math.sqrt(delta*delta+WEIGHT_STABILITY_SMOOTHNESS);
				gradient[argIndex] += dw * WEIGHT_STABILITY_IMPORTANCE;
				// if (dw==0)Util.print("ZERO "+w0+" "+w);
			}
			argIndex++;
		}
	}

	// public double evaluateNEW(double[] argument, double[] gradient) {
	// double result = evaluate(argument);
	// initializeWeights(null, gradient);
	// return result;
	// }
}
