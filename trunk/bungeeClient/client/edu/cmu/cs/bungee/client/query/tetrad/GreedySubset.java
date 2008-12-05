package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Find the best concise subset of variables, where it's worth adding a variable
 * if it contributes more than threshold to the evaluation function.
 * 
 */
public abstract class GreedySubset {
	Set variables;
	Set currentGuess;
	double threshold;
	Object lastToggledVariable;

	GreedySubset(double threshold, Collection variables) {
		super();
		this.currentGuess = new HashSet();
		this.threshold = threshold;
		this.variables = new HashSet(variables);
	}

	void addAllVariables() {
		currentGuess.addAll(variables);
	}

	Set selectVariables() {
		// Util.print("\nselectVariables: base eval=" + eval());
		while (maybeUpdate(false) || maybeUpdate(true)) {
			// Keep updating until at local optimum
		}
		Util.print(Util.shortClassName(this) + " => " + currentGuess + "\n");
		return currentGuess;
	}

	private boolean maybeUpdate(boolean isAdd) {
		Object candidate = bestCandidate(isAdd);
		boolean result = candidate != null && candidate != lastToggledVariable;
		if (result) {
			toggle(candidate);
			lastToggledVariable = candidate;
			newBest(candidate);
		}
		return result;
	}

	private void toggle(Object candidate) {
		if (isAdding(candidate))
			currentGuess.remove(candidate);
		else
			currentGuess.add(candidate);
	}

	protected Set previousGuess(Object candidate) {
		toggle(candidate);
		Set result = new HashSet(currentGuess);
		toggle(candidate);
		return result;
	}

	protected boolean isAdding(Object candidate) {
		return currentGuess.contains(candidate);
	}

	// void add(Object candidate) {
	// currentGuess.add(candidate);
	// }
	//
	// void remove(Object candidate) {
	// currentGuess.remove(candidate);
	// }

	private Object bestCandidate(boolean isAdd) {
		double bestEval = Double.NEGATIVE_INFINITY;
		Object best = null;
		for (Iterator it = variables.iterator(); it.hasNext();) {
			Object variable = it.next();
			if (isAdd != currentGuess.contains(variable)) {
				toggle(variable);
				double eval = eval(variable);
//				 Util.print("eval " + eval + " " + variable);
				if (eval > bestEval) {
//					 Util.print("new best eval " + eval + " " + variable);
					bestEval = eval;
					best = variable;
				}
				toggle(variable);
			}
		}
		boolean win = bestEval > (isAdd ? threshold : -threshold);
//		 Util.print("BEST Candidate " + bestEval + (isAdd ? " + " : " - ")
//		 + (win ? best : "[" + best + "]"));
		if (!win)
			best = null;
		return best;
	}

	protected void newBest(Object candidate) {
		Util.print("NEW GUESS " + improvement(candidate) + " "
				+ (isAdding(candidate) ? "+ " : "- ") + candidate + " => "
				+ currentGuess);
	}

	double eval(Object candidate) {
		double result = improvement(candidate);
		boolean isAdd = isAdding(candidate);
		boolean win = result > (isAdd ? threshold : -threshold);
		Util.print(Util.shortClassName(this) + ".improvement "
				+ (win ? "" + result : "[" + result + "]")
				+ (isAdd ? " + " : " - ") + candidate + " " + currentGuess);
		return result;
	}

	/**
	 * @param candidate
	 * @return how much adding/removing candidate improves on the previous set.
	 *         (If candidate helps, and we're removing, improvement should be
	 *         negative.)
	 */
	abstract double improvement(Object candidate);
}
