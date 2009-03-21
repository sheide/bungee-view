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
	static final int ADD_AND_REMOVE = 0;
	static final int ADD = 1;
	static final int REMOVE = 2;
	private Set variables;
	protected Set currentGuess;
	protected double threshold;
	private Object lastToggledVariable;
	private final int mode;

	protected GreedySubset(double threshold, Collection variables, int mode) {
		super();
		assert mode == ADD_AND_REMOVE || mode == ADD || mode == REMOVE;
		this.currentGuess = new HashSet();
		this.threshold = threshold;
		this.variables = new HashSet(variables);
		this.mode = mode;
	}

	protected void addAllVariables() {
		currentGuess.addAll(variables);
	}

	protected Set selectVariables() {
		// Util.print("\nselectVariables: base eval=" + eval());
		while (maybeUpdate(false) || maybeUpdate(true)) {
			// Keep updating until at local optimum
		}
		// Util.print(Util.shortClassName(this) + " => " + currentGuess + "\n");
		return currentGuess;
	}

	private boolean maybeUpdate(boolean isAdd) {
		Object candidate = mayUpdate(isAdd) ? bestCandidate(isAdd) : null;
		boolean result = candidate != null && candidate != lastToggledVariable;
		if (result) {
			toggle(candidate);
			lastToggledVariable = candidate;
			newBest(candidate);
		}
		return result;
	}

	private boolean mayUpdate(boolean isAdd) {
		return mode == ADD_AND_REMOVE || (isAdd == (mode == ADD));
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

	private Object bestCandidate(boolean isAdd) {
		double bestEval = Double.NEGATIVE_INFINITY;
		Object best = null;
		for (Iterator it = variables.iterator(); it.hasNext();) {
			Object variable = it.next();
			if (isAdd != currentGuess.contains(variable)) {
				toggle(variable);
				double eval = eval(variable, bestEval);
				// Util.print("eval " + eval + (isAdd?" + ":" - ") + variable);
				if (eval > bestEval) {
					// Util.print("new best eval " + eval + " " + variable);
					bestEval = eval;
					best = variable;
				}
				toggle(variable);
			}
		}
		boolean win = bestEval > (isAdd ? threshold : -threshold);
		// Util.print("BEST Candidate " + bestEval + (isAdd ? " + " : " - ")
		// + (win ? best : "[" + best + "]"));
		if (!win)
			best = null;
		return best;
	}

	protected void newBest(Object candidate) {
		if (Explanation.PRINT_LEVEL > 0) {
			Util.print("NEW GUESS "
					+ improvement(candidate, Double.NEGATIVE_INFINITY) + " "
					+ (isAdding(candidate) ? "+ " : "- ") + candidate + " => "
					+ currentGuess);
		}
	}

	private double eval(Object candidate, double bestEval) {
		boolean isAdd = isAdding(candidate);
		double thresh = isAdd ? threshold : -threshold;
		double result = improvement(candidate, Math.max(thresh, bestEval));

		if (Explanation.PRINT_LEVEL > 0) {
			boolean win = result > thresh;
			Util.print(Util.shortClassName(this) + ".improvement "
					+ (win ? result + " > " : "[" + result + "] < ") + thresh
					+ (isAdd ? " + " : " - ") + candidate + " " + currentGuess);
			if (Explanation.PRINT_LEVEL > 1)
				Util.print("");
		}

		return result;
	}

	/**
	 * @param candidate
	 * @param bestEval
	 * @return how much adding/removing candidate improves on the previous set.
	 *         (If candidate helps, and we're removing, improvement should be
	 *         negative.)
	 */
	abstract double improvement(Object candidate, double bestEval);
}
