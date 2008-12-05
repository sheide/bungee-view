package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;

import pal.mathx.ConjugateGradientSearch;
import pal.mathx.MFWithGradient;
import pal.mathx.MachineAccuracy;

public class NonAlchemyModel extends Explanation implements MFWithGradient {

	// public static Explanation getNullExplanation(Perspective popupFacet) {
	// return new NonAlchemyModel(Explanation.primaryFacets(popupFacet), null,
	// null, null);
	// }

	public static Explanation getExplanation(Perspective popupFacet) {
		Explanation result = null;
		List primaryFacets = Explanation.primaryFacets(popupFacet);
		if (primaryFacets.size() > 1) {
			Distribution maxModel = Distribution
					.getObservedDistribution(Explanation.candidateFacets(
							primaryFacets, false));
			Explanation nullModel = new NonAlchemyModel(primaryFacets, null,
					null, maxModel);
			result = nullModel.getExplanation(maxModel);
		}
		return result;
	}

	Explanation getLikeExplanation() {
		return new NonAlchemyModel(facets(), predicted.getEdges(), this,
				observed);
	}

	// Explanation getAlternateExplanation(List facets) {
	// return new NonAlchemyModel(facets, null, this, null);
	// }

	Explanation getAlternateExplanation(List facets, Distribution maxModel) {
		return new NonAlchemyModel(facets, null, this, maxModel);
	}

	Explanation getAlternateExplanation(Set edges) {
		return new NonAlchemyModel(facets(), edges, this, observed);
	}

	protected NonAlchemyModel(List facets, Set edges, Explanation base,
			Distribution maxModel) {
		super(facets, edges, base, maxModel);
	}

	protected void learnWeights(Explanation base) {

		// predicted.setWeights(getInitialWeights(base));
		// Util.print("\nlearnWeights: " + this);

		ConjugateGradientSearch search = new ConjugateGradientSearch();
		double[] weights = search.findMinimumArgs(this,
				getInitialWeights(base), MachineAccuracy.EPSILON, 0.0001);
		predicted.setWeights(weights);
	}

	public double evaluate(double[] argument) {
		// Util.print("evaluate " + Util.valueOfDeep(argument));
		predicted.setWeights(argument);
		// predicted.printGraph(getObservedDistribution());
		return observed.KLdivergence(predicted.logPredictedDistribution());
	}

	public double getLowerBound(int n) {
		return -100;
	}

	public double getUpperBound(int n) {
		return 100;
	}

	public void computeGradient(double[] argument, double[] gradient) {
		evaluate(argument, gradient);
	}

	public double evaluate(double[] argument, double[] gradient) {
		double result = evaluate(argument);
		int argIndex;
		for (argIndex = 0; argIndex < nFacets(); argIndex++) {
			gradient[argIndex] = 0;
			for (int state = 0; state < predicted.nStates(); state++) {
				if (Util.isBit(state, argIndex)) {
					gradient[argIndex] += predicted.getDistribution()[state]
							- observed.getDistribution()[state];
				}
			}
		}

		for (Iterator it = predicted.getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			gradient[argIndex] = 0;
			for (int state = 0; state < predicted.nStates(); state++) {
				if (Util.isBit(state, edge[0]) && Util.isBit(state, edge[1])) {
					gradient[argIndex] += predicted.getDistribution()[state]
							- observed.getDistribution()[state];
				}
			}
			argIndex++;
		}
		return result;
	}
}
