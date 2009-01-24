package edu.cmu.cs.bungee.client.query.tetrad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.Util;

public class AlchemyModel extends Explanation {
	private static String CONNECTIVE = "^";
	private static double PENALTY = 0.000001;
	static final int MOD = 50;
	private static final boolean IS_STRUCTURE_LEARNING = false;
	private static final String DIRECTORY = "C:\\Documents and Settings\\mad\\workspace\\bungeeDBscripts\\";
	final JDBCSample jdbc;

	Explanation getLikeExplanation() {
		return new AlchemyModel(facets(), predicted.getEdges(), this, null,
				jdbc);
	}

	Explanation getAlternateExplanation(List facets) {
		return new AlchemyModel(facets, null, this, null, jdbc);
	}

	Explanation getAlternateExplanation(List facets, List candidateFacets) {
		return new AlchemyModel(facets, null, this, candidateFacets, jdbc);
	}

	Explanation getAlternateExplanation(Set edges) {
		return new AlchemyModel(facets(), edges, this, null, jdbc);
	}

	protected AlchemyModel(List facets, Set edges, Explanation base,
			List candidateFacets, JDBCSample jdbc2) {
		super(facets, edges, base, candidateFacets);
		jdbc = jdbc2;
	}

	protected static JDBCSample createJdbc(String dbName) {
		JDBCSample jdbc1 = null;
		try {
			jdbc1 = new JDBCSample("jdbc:mysql://localhost/", dbName, "p5",
					"p5pass");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return jdbc1;
	}

	// To see distribution:
	//
	// Add to bv-out.mln:
	//
	// item = {I,J,K,L,M}
	//
	// C:/Downloads/alchemy/bin/infer -i bv-out.mln -r bv.infer -e bv-test.db -q
	// has_property

	private void runAlchemy() {
		writeMLNfile();
		writeDBfile();
		try {
			String[] cmd = {
					"C:/cygwin/bin/sh",
					"-c",
					IS_STRUCTURE_LEARNING ?

					"C:/Downloads/alchemy/bin/learnstruct "
							+ "-i bv.mln -o bv-out.mln -t bv.db -maxNumPredicates 2 "
							+ "-startFromEmptyMLN " +
							// "-noAddUnitClauses true "+
							"-penalty " + PENALTY + " -maxVars 1 > C:/hello"

							:

							"C:/Downloads/alchemy/bin/learnwts " + "-noPrior "
							// +
									// "-gConvThresh 1e-10 -queryEvidence "
									+ "-g -i bv.mln -o bv-out.mln -t bv.db -noAddUnitClauses > C:/hello"

			};
			Runtime
					.getRuntime()
					.exec(
							cmd,
							null,
							new File(
									"C:\\Documents and Settings\\mad\\workspace\\bungeeDBscripts"))
					.waitFor();
			// "C:\\Downloads\\alchemy\\bin\\learnstruct -i bv.mln -o bv-out.mln -t bv.db"
			// +
			// " -maxNumPredicates 2 -startFromEmptyMLN true -penalty 0.000001 -maxVars 1"
			// ,
			//
			// new File(
			// "C:\\Documents and Settings\\mad\\workspace\\bungeeDBscripts"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeMLNfile() {
		StringBuffer buf = new StringBuffer();

		writeItems(buf);

		buf.append("has_property(item, facet)\n");

		// buf.append(Util.join(MLN, "\n"));

		// {
		// Iterator it1 = primaryFacets.iterator();
		// buf.append("has_property(i, "
		// + predicateName((Perspective) it1.next()) + ")\n");
		// }

		for (Iterator it1 = facets().iterator(); it1.hasNext();) {
			Perspective p1 = (Perspective) it1.next();
			buf
					.append("has_property(i, " + Distribution.tetradName(p1)
							+ ")\n");

			// buf.append("has_property(i, " + tetradName(p1) + "dup)\n");
			// buf.append("has_property(i, " + tetradName(p1)
			// + ") ^ has_property(i, " + tetradName(p1) + "dup)\n");
		}

		for (Iterator it = predicted.getEdges().iterator(); it.hasNext();) {
			List edge = (List) it.next();
			Perspective cause = (Perspective) edge.get(0);
			Perspective caused = (Perspective) edge.get(1);
			if (cause.compareTo(caused) > 0) {
				buf.append("has_property(i, " + Distribution.tetradName(cause)

				+ ") " + CONNECTIVE + " has_property(i, "

				// + ") ^ has_property(i, "

						+ Distribution.tetradName(caused) + ")\n");
			}
		}
		Util.writeFile(new File(DIRECTORY + "bv.mln"), buf.toString());
	}

	private void writeItems(StringBuffer buf) {
		buf.append("item = {");
		try {
			ResultSet rs = jdbc
					.SQLquery("SELECT CONCAT('I', record_num), record_num"
							+ " FROM item WHERE MOD(record_num, " + MOD
							+ ") = 0");
			boolean isFirst = true;
			while (rs.next()) {
				if (isFirst)
					isFirst = false;
				else
					buf.append(",");
				buf.append(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		buf.append("}\n");
	}

	private void writeDBfile() {
		// Util.print("writeDBfile " + primaryFacets);
		int[] facetIDs = new int[nFacets()];
		int i = 0;
		for (Iterator it = facets().iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			facetIDs[i++] = facet.getID();
		}
		try {
			ResultSet rs = jdbc
					.SQLquery("SELECT CONCAT(name, '_', facet.facet_id), record_num"
							+ " FROM facet INNER JOIN item_facet USING (facet_id)"
							+ " WHERE facet.facet_id IN("
							+ Util.join(facetIDs)
							+ ") AND MOD(record_num, " + MOD + ") = 0");
			BufferedWriter writer = Util.getWriter(DIRECTORY + "bv.db");
			while (rs.next()) {
				writer.write("has_property(I");
				writer.write(Integer.toString(rs.getInt(2)));
				writer.write(", ");
				writer.write(Distribution.predicateName(rs.getString(1)));
				writer.write(")\n");

				// if (Math.random() > 0.01) {
				// writer.write("has_property(I");
				// writer.write(Integer.toString(rs.getInt(2)));
				// writer.write(", ");
				// writer.write(predicateName(rs.getString(1) + "dup"));
				// writer.write(")\n");
				// }
			}
			writer.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final Pattern rule = Pattern
			.compile("(?m)^(-?\\d+(?:\\.\\d+))(?:\\s*(!?))has_property\\((\\w+),(\\w+)\\)(?:\\s*v\\s*(!?)has_property\\((\\w+),(\\w+)\\))?");
	private static final Pattern id = Pattern.compile("F.*?(\\d+)");

	private Perspective lookupFacet(Query query, String facet) {
		Perspective result = null;
		Matcher m = id.matcher(facet);
		if (m.matches()) {
			int facetID = Integer.parseInt(m.group(1));
			result = query.findPerspective(facetID);
		}
		return result;
	}

	protected void learnWeights(Explanation base) {
		// Util.print("\nAlchemy: " + facets);
		Query query = Distribution.query(facets());
		runAlchemy();
		String output = Util.readFile(DIRECTORY + "bv-out.mln");
		// Util.print("zzz\n" + output);
		Matcher m = rule.matcher(output);
		while (m.find()) {
			double weight = Double.parseDouble(m.group(1));
			boolean negated1 = m.group(2).length() > 0;
			if (negated1)
				// Convert unary clauses and conjunctions to positive; leave
				// implications negative
				weight = -weight;
			String var1 = m.group(3);
			Perspective facet1 = lookupFacet(query, m.group(4));
			// Util.print(negated1 + " " + var1 + " " + facet1+" "+weight);
			if (m.group(5) != null) {
				boolean negated2 = m.group(5).length() > 0;
				String var2 = m.group(6);
				Perspective facet2 = lookupFacet(query, m.group(7));
				// Util.print(negated2 + " " + var2 + " " + facet2);
				assert var1.equals(var2);
				// assert !negated1;
				assert negated2;
				if (facet1 != facet2) {
					predicted.setWeight(facet1, facet2, weight);
				}
			} else if (facet1 != null) {
				predicted.setWeight(facet1, facet1, weight);
			}
		}
	}

	// void setWeight(Perspective cause, Perspective caused, double weight) {
	// // super.setWeight(cause, caused, weight / 2);
	// // if (CONNECTIVE.equals("^"))
	// super.setWeight(caused, cause, cause == caused ? weight : weight / 2);
	// }
	//
	// double linkEnergy(int state, int causeNode, int causedNode) {
	// return Util.isBit(state, causedNode) && Util.isBit(state, causeNode) ?
	// predicted.getWeight(
	// causeNode, causedNode)
	// : 0;
	// }
	//
	// double linkExpEnergy(int state, int causeNode, int causedNode) {
	// return Util.isBit(state, causedNode) && Util.isBit(state, causeNode) ?
	// predicted.getExpWeight(
	// causeNode, causedNode)
	// : 1;
	// }
	//
	// double linkExpEnergy(int state, int causeNode, int causedNode) {
	// double result = 1;
	// if (Util.isBit(state, causedNode)) {
	// if (causeNode == causedNode) {
	// result = getExpWeight(causeNode, causedNode);
	// } else if (Util.isBit(state, causeNode)) {
	// result = getExpWeight(causeNode, causedNode);
	// }
	// }
	// Util.print("linkExpEnergy " + state + " " + causeNode + " "
	// + causedNode + " " + result + " old="
	// + Math.exp(linkEnergy(state, causeNode, causedNode)));
	// return result;
	// }
	//
	// EnergyBasedModel selectFacets(Distribution maxModel) {
	// double bestEval = Double.NEGATIVE_INFINITY;
	// EnergyBasedModel best = null;
	// Set candidateFacets = new HashSet(maxModel.facets);
	// candidateFacets.removeAll(facets());
	// Set currentGuess = new HashSet(facets());
	// for (Iterator it = candidateFacets.iterator(); it.hasNext();) {
	// Perspective candidateFacet = (Perspective) it.next();
	// currentGuess.add(candidateFacet);
	// EnergyBasedModel candidateExplanation = getAlternateExplanation(
	// currentGuess, null, maxModel);
	// double eval = candidateExplanation.improvement(this);
	//
	// // Util
	// // .print("selectFacets eval " + eval + " "
	// // + candidateExplanation+"\n");
	// if (eval > bestEval) {
	// bestEval = eval;
	// best = candidateExplanation;
	// }
	// currentGuess.remove(candidateFacet);
	// }
	// Util.print("selectFacets BEST " + bestEval + " " + best);
	// best.printGraph(true);
	// if (bestEval < NODE_COST)
	// return this;
	// else
	// return best.selectFacets(maxModel);
	// }
	//
	// /**
	// * @param isCoreEdges
	// * Once core edges are removed, removing others will have no
	// * effect. So first remove any non-core edges you can, and then
	// * remove only core edges.
	// * @return
	// */
	// EnergyBasedModel selectEdges(boolean isCoreEdges, Distribution maxModel)
	// {
	// double bestEval = Double.NEGATIVE_INFINITY;
	// EnergyBasedModel best = null;
	// Set candidateEdges = getEdges();
	// Set currentGuess = getEdges();
	// for (Iterator it = candidateEdges.iterator(); it.hasNext();) {
	// List candidateEdge = (List) it.next();
	// if (isCoreEdges == (primaryFacets().containsAll(candidateEdge))) {
	// currentGuess.remove(candidateEdge);
	// EnergyBasedModel candidateExplanation = getAlternateExplanation(
	// facets(), currentGuess, maxModel);
	// double eval = -improvement(candidateExplanation);
	// Util.print("selectEdges eval " + eval + " " + candidateEdge);
	// if (eval > bestEval) {
	// bestEval = eval;
	// best = candidateExplanation;
	// }
	// currentGuess.add(candidateEdge);
	// }
	// }
	// // Util.print("selectEdges " + bestEval + " - " + best);
	// if (bestEval > -EDGE_COST)
	// return best.selectEdges(isCoreEdges, maxModel);
	// else if (isCoreEdges)
	// return this;
	// else
	// return selectEdges(true, maxModel);
	// }
	//
	// double[] getWeights() {
	// int argIndex = 0;
	// double[] result = new double[getNumArguments()];
	// // List wts = new LinkedList();
	// for (Iterator it1 = facets().iterator(); it1.hasNext();) {
	// Perspective p1 = (Perspective) it1.next();
	// result[argIndex++] = getWeight(p1, p1);
	// // wts.add(p1);
	// }
	//
	// for (Iterator it = getEdges().iterator(); it.hasNext();) {
	// List edge = (List) it.next();
	// // wts.add(edge);
	// Perspective cause = (Perspective) edge.get(0);
	// Perspective caused = (Perspective) edge.get(1);
	// // if (cause.compareTo(caused) > 0)
	// result[argIndex++] = getWeight(cause, caused);
	//
	// }
	// // Util.print("getWeights " + wts);
	// return result;
	// }
	//
	// void setWeights(double[] argument) {
	// int argIndex = 0;
	// for (Iterator it1 = facets().iterator(); it1.hasNext();) {
	// Perspective p1 = (Perspective) it1.next();
	// setWeight(p1, p1, argument[argIndex++]);
	// }
	//
	// for (Iterator it = getEdges().iterator(); it.hasNext();) {
	// List edge = (List) it.next();
	// Perspective cause = (Perspective) edge.get(0);
	// Perspective caused = (Perspective) edge.get(1);
	// // if (cause.compareTo(caused) > 0)
	// setWeight(cause, caused, argument[argIndex++]);
	// }
	// }
	//
	// public double evaluate(double[] argument) {
	// // Util.print("evaluate " + Util.valueOfDeep(argument));
	// setWeights(argument);
	// return
	// getObservedDistribution().KLdivergence(logPredictedDistribution());
	// }
	//
	// public double getLowerBound(int n) {
	// return -100;
	// }
	//
	// public int getNumArguments() {
	// return nFacets() + getEdges().size();
	// }
	//
	// public double getUpperBound(int n) {
	// return 100;
	// }
	//
	// public void computeGradient(double[] argument, double[] gradient) {
	// evaluate(argument, gradient);
	// }
	//
	// public double evaluate(double[] argument, double[] gradient) {
	// double result = evaluate(argument);
	// double[] observed = getObservedDistribution();
	// double[] predicted = predictedDistribution();
	// int argIndex;
	// for (argIndex = 0; argIndex < facets.size(); argIndex++) {
	// gradient[argIndex] = 0;
	// for (int state = 0; state < predicted.length; state++) {
	// if (Util.isBit(state, argIndex)) {
	// gradient[argIndex] += predicted[state] - observed[state];
	// }
	// }
	// }
	//
	// for (Iterator it = getEdges().iterator(); it.hasNext();) {
	// List edge = (List) it.next();
	// Perspective cause = (Perspective) edge.get(0);
	// Perspective caused = (Perspective) edge.get(1);
	// int causeIndex = facets.indexOf(cause);
	// int causedIndex = facets.indexOf(caused);
	// gradient[argIndex] = 0;
	// for (int state = 0; state < predicted.length; state++) {
	// if (Util.isBit(state, causeIndex)
	// && Util.isBit(state, causedIndex)) {
	// gradient[argIndex] += predicted[state] - observed[state];
	// }
	// }
	// argIndex++;
	// }
	// return result;
	// }
}
