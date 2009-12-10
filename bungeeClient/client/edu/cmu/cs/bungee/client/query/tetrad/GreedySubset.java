package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Find the best concise subset of variables, where it's worth adding a variable
 * if it contributes more than threshold to the evaluation function.
 * 
 */
public abstract class GreedySubset<V> {
	static final int ADD_AND_REMOVE = 0;
	static final int ADD = 1;
	static final int REMOVE = 2;
	private Set<V> variables;
	protected Set<V> currentGuess;
	protected double threshold;
	private V lastToggledVariable;
	private final int mode;

	protected GreedySubset(double threshold, Collection<V> variables, int mode) {
		super();
		assert variables.size() > 0;
		assert mode == ADD_AND_REMOVE || mode == ADD || mode == REMOVE;
		this.currentGuess = new HashSet<V>();
		this.threshold = threshold;
		this.variables = new HashSet<V>(variables);
		this.mode = mode;
	}

	protected void addAllVariables() {
		currentGuess.addAll(variables);
	}

	protected Set<V> selectVariables() {
		// Util.print("\nselectVariables: base eval=" + eval());
		while (maybeUpdate(false) || maybeUpdate(true)) {
			// Keep updating until at local optimum
		}
		if (Explanation.PRINT_LEVEL >= Explanation.IMPROVEMENT)
			Util.print(Util.shortClassName(this) + " => #"
					+ currentGuess.size() + " " + currentGuess + "\n");
		return currentGuess;
	}

	private boolean maybeUpdate(boolean isAdd) {
		V candidate = mayUpdate(isAdd) ? bestCandidate(isAdd) : null;
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

	private void toggle(V candidate) {
		if (isAdding(candidate))
			currentGuess.remove(candidate);
		else
			currentGuess.add(candidate);
	}

	protected Set<V> previousGuess(V candidate) {
		toggle(candidate);
		Set<V> result = new HashSet<V>(currentGuess);
		toggle(candidate);
		return result;
	}

	protected boolean isAdding(V candidate) {
		return currentGuess.contains(candidate);
	}

	private V bestCandidate(boolean isAdd) {
		double bestEval = Double.NEGATIVE_INFINITY;
		V best = null;
		List<Eval> evals = new LinkedList<Eval>();
		for (Iterator<V> it = variables.iterator(); it.hasNext();) {
			V variable = it.next();
			if (isAdd != currentGuess.contains(variable)) {
				toggle(variable);
				double eval = eval(variable, bestEval);
				evals.add(new Eval(eval, variable));
				// Util.print("eval " + eval + (isAdd?" + ":" - ") + variable);
				if (eval > bestEval) {
					// Util.print("new best eval " + eval + " " + variable);
					bestEval = eval;
					best = variable;
				}
				toggle(variable);
			}
		}

		double thresh = isAdd ? threshold : -threshold;

		if (Explanation.PRINT_LEVEL == Explanation.IMPROVEMENT) {
			Util.print("\n" + Util.shortClassName(this)
					+ ".improvement: threshold = " + thresh
					+ (isAdd ? "; adding to " : "; removing from ")
					+ currentGuess);
			Collections.sort(evals);
			for (Iterator<Eval> it = evals.iterator(); it.hasNext();) {
				Eval e = it.next();
				double result = e.eval;
				boolean win = result > thresh;
				Util.print((win ? "  " : " [") + result + (win ? " \t" : "]\t")
						+ e.object);
			}
		}

		boolean win = bestEval > thresh;
		// Util.print("BEST Candidate " + bestEval + (isAdd ? " + " : " - ")
		// + (win ? best : "[" + best + "]"));
		if (!win)
			best = null;
		return best;
	}

	private class Eval implements Comparable<Eval> {
		double eval;
		Object object;

		Eval(double eval, Object object) {
			this.eval = eval;
			this.object = object;
		}

		public int compareTo(Eval arg0) {
			return Util.sgn(arg0.eval - eval);
		}
	}

	protected void newBest(V candidate) {
		if (Explanation.PRINT_LEVEL >= Explanation.IMPROVEMENT) {
			Util.print("NEW GUESS "
					+ improvement(candidate, Double.NEGATIVE_INFINITY) + " "
					+ (isAdding(candidate) ? "+ " : "- ") + candidate + " => "
					+ currentGuess);
		}
	}

	private double eval(V candidate, double bestEval) {
		boolean isAdd = isAdding(candidate);
		double thresh = isAdd ? threshold : -threshold;
		double result = improvement(candidate, Math.max(thresh, bestEval));

		if (Explanation.PRINT_LEVEL >= Explanation.WEIGHTS) {
			boolean win = result > thresh;
			Util.print(Util.shortClassName(this) + ".improvement "
					+ (win ? result + " > " : "[" + result + "] < ") + thresh
					+ (isAdd ? " + " : " - ") + candidate + " " + currentGuess);
			if (Explanation.PRINT_LEVEL >= Explanation.WEIGHTS)
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
	abstract double improvement(V candidate, double bestEval);
}
