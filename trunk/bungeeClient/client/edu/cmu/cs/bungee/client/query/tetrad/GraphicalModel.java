package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;
import edu.cmu.cs.bungee.javaExtensions.graph.Node;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph.GraphWeigher;

public class GraphicalModel extends Distribution {
	// protected final List facets;
	// protected final int[] counts;

	// /**
	// * Encodes the current edges. Big bug that current code ignores edges
	// among
	// * auxilliary variables.
	// */
	// private final Map incoming = new HashMap();

	final boolean isSymmetric;

	/**
	 * {node1, node2) => weight
	 */
	protected final double[][] weights;

	/**
	 * Non-existent edges are coded
	 */
	final double[][] expWeights;

	GraphicalModel(List facets, Set edges, boolean isSymmetric) {
		super(facets, null);
		this.isSymmetric = isSymmetric;
		weights = new double[nFacets()][];
		expWeights = new double[nFacets()][];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = new double[nFacets()];
			expWeights[i] = new double[nFacets()];
			for (int j = 0; j < expWeights.length; j++) {
				expWeights[i][j] = -1;
			}
		}
		if (edges == null)
			edges = allEdges(facets, facets);
		addEdges(edges);
		// Util.print("GraphicalModel " + getEdges());
		// for (Iterator it = facets.iterator(); it.hasNext();) {
		// Perspective facet = (Perspective) it.next();
		// addEdge(facet, facet);
		// }
	}

	protected boolean hasEdge(Perspective cause, Perspective caused) {
		int causeNode = facetIndexOrNot(cause);
		if (causeNode < 0)
			return false;
		int causedNode = facetIndexOrNot(caused);
		if (causedNode < 0)
			return false;
		return expWeights[causeNode][causedNode] >= 0;
	}

	protected boolean hasEdge(int causeNode, int causedNode) {
		return expWeights[causeNode][causedNode] >= 0;
	}

	protected double getExpWeight(int i, int j) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		double result = expWeights[i][j];
		assert result >= 0;
		// Util.print("getExpWeight " + i + " " + j + " " + result + " old="
		// + Math.exp(getWeight(i, j)));
		return result;
	}

	protected double getWeight(Perspective cause, Perspective caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		return getWeight(facetIndex(cause), facetIndex(caused));
	}

	protected double getWeightOrZero(Perspective cause, Perspective caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		return hasEdge(cause, caused) ? getWeight(facetIndex(cause),
				facetIndex(caused)) : 0;
	}

	protected double getWeight(int cause, int caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		assert expWeights[cause][caused] >= 0 : cause + " " + caused + " "
				+ facets() + " " + Util.valueOfDeep(expWeights);
		double result = weights[cause][caused];
		return result;
	}

	double getRNormalizedWeight(Distribution observedDistribution,
			Perspective cause, Perspective caused) {
		double stdDevNormalizedWeight = getStdDevNormalizedWeight(cause, caused);
		// treat zero specially to avoid returning NaN
		double result = stdDevNormalizedWeight == 0 ? 0
				: stdDevNormalizedWeight
						* observedDistribution.R(getDistribution(), caused)
						/ sumStdDevNormalizedWeights(caused);

		// Util.print("getRNormalizedWeight stdDevNormalizedWeight="
		// + stdDevNormalizedWeight + " R="
		// + observedDistribution.R(getDistribution(), caused) + " Z="
		// + sumStdDevNormalizedWeights(caused));

		assert !Double.isNaN(result) : stdDevNormalizedWeight + " "
				+ sumStdDevNormalizedWeights(caused);
		return result;
	}

	double getStdDevNormalizedWeight(Perspective cause, Perspective caused) {
		return getWeight(cause, caused) * cause.stdDev();
	}

	// double getWeight(List edge) {
	// Double weight = (Double) weights.get(edge);
	// return weight == null ? 0 : weight.doubleValue();
	// }

	// double getExpWeight(List edge) {
	// Double expWeight = (Double) expWeights.get(edge);
	// // assert expWeight == null || expWeight.doubleValue() > 0 : edge + " "
	// // + expWeight.doubleValue();
	// return expWeight == null ? 1 : expWeight.doubleValue();
	// }

	void setWeights(double[] argument) {
		int argIndex = 0;
		for (Iterator it1 = facets().iterator(); it1.hasNext();) {
			Perspective p1 = (Perspective) it1.next();
			setWeight(p1, p1, argument[argIndex++]);
		}

		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			setWeight(edge[0], edge[1], argument[argIndex++]);
		}
	}

	void setWeight(Perspective cause, Perspective caused, double weight) {
		setWeight(facetIndex(cause), facetIndex(caused), weight);
	}

	void setWeight(int cause, int caused, double weight) {
		// Util.print("setWeight " + cause + " => " + caused + " " + weight);
		assert !Double.isInfinite(weight);
		assert !Double.isNaN(weight);
		distribution = null;
		weights[cause][caused] = weight;
		double expWeight = Math.exp(weight);
		expWeights[cause][caused] = expWeight;
		if (isSymmetric) {
			weights[caused][cause] = weight;
			expWeights[caused][cause] = expWeight;
		}
	}

	List getEdge(Perspective cause, Perspective caused) {
		List edge = new ArrayList(2);
		edge.add(cause);
		edge.add(caused);
		return edge;
	}

	// void addEdge(Perspective cause, Perspective caused, double weight) {
	// addEdge(cause, caused);
	// setWeight(cause, caused, weight);
	// }
	//
	// void addEdge(Perspective cause, Perspective caused) {
	// // Util.print("addEdge " + cause + " " + caused);
	// assert hasFacet(cause);
	// assert hasFacet(caused);
	//
	// List causes = getCauses(caused);
	// assert !causes.contains(cause) : cause + " " + caused;
	// causes.add(cause);
	//
	// // We're doing symmetric edges now
	// causes = getCauses(cause);
	// assert !causes.contains(caused) : caused + " " + cause;
	// causes.add(caused);
	// }

	void addEdge(Perspective cause, Perspective caused) {
		addEdge(facetIndex(cause), facetIndex(caused));
	}

	void addEdge(int cause, int caused) {
		// Util.print("addEdge " + cause + " " + caused + " " + isSymmetric);
		assert expWeights[cause][caused] < 0;
		expWeights[cause][caused] = 1;
		weights[cause][caused] = 0;
		if (isSymmetric) {
			expWeights[caused][cause] = 1;
			weights[caused][cause] = 0;
		}
	}

	// void removeEdge(Perspective cause, Perspective caused) {
	// // Util.print("removeEdge " + cause + " " + caused);
	// assert hasFacet(cause);
	// assert hasFacet(caused);
	//
	// List causes = getCauses(caused);
	// assert causes.contains(cause);
	// causes.remove(cause);
	//
	// causes = getCauses(cause);
	// assert causes.contains(caused);
	// causes.remove(caused);
	//
	// }

	void removeEdge(Perspective cause, Perspective caused) {
		removeEdge(facetIndex(cause), facetIndex(caused));
	}

	void removeEdge(int cause, int caused) {
		// Util.print("removeEdge " + cause + " " + caused);
		assert expWeights[cause][caused] >= 0;
		expWeights[cause][caused] = -1;
		if (isSymmetric) {
			expWeights[caused][cause] = -1;
		}
	}

	protected List getCauses(Perspective caused) {
		List result = new LinkedList();
		int causedNode = facetIndex(caused);
		if (causedNode >= 0) {
			for (int causeNode = 0; causeNode < nFacets(); causeNode++) {
				if (causeNode != causedNode
						&& expWeights[causeNode][causedNode] >= 0)
					result.add(getFacet(causeNode));
			}
		}
		// Util.print("getCauses " + caused + " " + causes);
		return result;
	}

	EdgeIterator getEdgeIterator() {
		return new EdgeIterator();
	}

	class EdgeIterator implements Iterator {

		// current edge
		int cause = 0;
		int caused = -1;

		// next edge
		int nextCause = -1;
		int nextCaused = -1;

		public boolean hasNext() {
			if (nextCause < 0) {
				nextCause = cause;
				nextCaused = caused + 1;
				for (; nextCause < nFacets(); nextCause++) {
					for (; nextCaused < nFacets(); nextCaused++) {
						if (nextCause != nextCaused
								&& expWeights[nextCause][nextCaused] >= 0
								&& (!isSymmetric || nextCause < nextCaused))
							return true;
					}
					nextCaused = 0;
				}
			}
			// Util.print("EdgeIterator " + nextCause + " " + nFacets() + " "
			// + cause + " " + caused + " " + nextCaused);
			return nextCause < nFacets();
		}

		public Object next() {
			if (hasNext()) {
				cause = nextCause;
				caused = nextCaused;
				nextCause = -1;
				int[] edge = { cause, caused };
				// Util.print("next " + Util.valueOfDeep(edge));
				return edge;
			} else {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			if (caused > 0) {
				removeEdge(cause, caused);
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * @return distance in displayed parameter space from nullModel
	 */
	double weightSpaceChange(GraphicalModel reference, Collection primaryFacets) {
		double delta = 0;
		// double initial = 0;
		for (Iterator causeIt = primaryFacets.iterator(); causeIt.hasNext();) {
			Perspective cause = (Perspective) causeIt.next();
			for (Iterator causedIt = primaryFacets.iterator(); causedIt
					.hasNext();) {
				Perspective caused = (Perspective) causedIt.next();
				int compare = cause.compareTo(caused);
				if (compare < 0 || !isSymmetric && compare > 0) {

					// double weight0 = reference.getRNormalizedWeight(cause,
					// caused);
					// double weight = getRNormalizedWeight(cause, caused);

					// Don't normalize, because a single strong predictor will
					// change the proportions, but not the underlying
					// dependency.
					double weight0 = reference.getWeightOrZero(cause, caused);
					double weight = getWeightOrZero(cause, caused);

					double diff = weight - weight0;

//					Util.print("weightSpaceChange " + Math.abs(diff) + " "
//							+ cause + " " + caused + " nullW=" + weight0
//							+ " W=" + weight);

					delta += diff * diff;
				}
			}
		}
		// Util.print("weightSpaceChange " + Math.sqrt(delta));
		// printGraph(false);
		// Util.print("");
		// reference.printGraph(false);
		// Util.print("weightSpaceChange done\n");
		assert !Double.isNaN(delta);
		return Math.sqrt(delta);
	}

	protected static String formatWeight(double weight) {
		if (Double.isNaN(weight))
			return "?";
		return Integer.toString((int) Math.rint(100 * weight));
	}

	// protected static String tetradName(Perspective p) {
	// String name = p.getNameIfPossible();
	// if (name == null)
	// name = "p";
	// else
	// name = predicateName(name);
	// name += "_" + p.getID();
	// return name;
	// }
	//
	// protected static String predicateName(String facetName) {
	// facetName = facetName.replaceAll("\n\r\\/:*?\"<>| ", "_");
	// String result = "F" + facetName.toLowerCase();
	// result = result.replaceAll("\\W", "_");
	// result = result.replaceAll("_+", "_");
	// return result;
	// }
	//
	// private int[] getCounts() {
	// Util.print("getCounts " + this);
	// int nFacets = facets.size();
	// int[] counts1 = new int[1 << nFacets];
	// ResultSet rs = query().onCountMatrix(facets);
	// try {
	// while (rs.next()) {
	// counts1[rs.getInt(1)] = rs.getInt(2);
	// }
	// rs.close();
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	//
	// // Util.print("\n" + nFacets + " primaryFacets\n "
	// // + Util.join(facets, "\n "));
	// // List others = new LinkedList(facets);
	// // others.removeAll(facets);
	// // Util.print(others.size() + " other facets of interest\n "
	// // + Util.join(others, "\n "));
	//
	// // Util.print("counts: " + Util.valueOfDeep(counts));
	//
	// // The low order bit represents the first facet (popupFacet)
	// // Perspective popupFacet1 = (Perspective) facets.get(0);
	// // int countInUniverse = getCountInUniverse(counts1, 0);
	// // assert countInUniverse == popupFacet1.getTotalCount() : popupFacet1
	// // + " " + popupFacet1.getTotalCount() + " " + countInUniverse;
	// // }
	// return counts1;
	// }

	public double[] getDistribution() {
		double[] p = distribution;
		if (p == null) {
			// Util.print("predictedDistribution " + this);
			int nFacets = facets.size();
			double z = 0;
			p = new double[1 << nFacets];
			for (int state = 0; state < p.length; state++) {
				// p[state] = Math.exp(energy(state));
				p[state] = expEnergy(state);
				// assert p[state] > 0 : state;
				z += p[state];
			}
			for (int state = 0; state < p.length; state++) {
				p[state] /= z;
				// Util.print("state " + state + " " + energy(state) + " "
				// + p[state]);
			}
			distribution = p;
		}
		return super.getDistribution();
	}

	protected double[] logPredictedDistribution() {
		int nFacets = facets.size();
		double z = 0;
		double[] logP = new double[1 << nFacets];
		for (int state = 0; state < logP.length; state++) {
			// p[state] = Math.exp(energy(state));
			logP[state] = energy(state);
			// assert p[state] > 0 : state;
			z += expEnergy(state);
		}
		double logZ = Math.log(z);
		for (int state = 0; state < logP.length; state++) {
			logP[state] -= logZ;
			// Util.print("state " + state + " " + energy(state) + " "
			// + p[state]);
		}

		// Util.print("logZ=" + logZ);
		// double[] old = getDistribution();
		// for (int state = 0; state < logP.length; state++) {
		// Util.print(state + " " + energy(state) + " old=" + old[state]
		// + " new=" + Math.exp(logP[state]) + " log=" + logP[state]);
		// }

		return logP;
	}

	private double expEnergy(int state) {
		int nFacets = nFacets();
		double result = 1;
		for (int i = 0; i < nFacets; i++) {
			for (int j = 0; j <= i; j++) {
				result *= linkExpEnergy(state, j, i);
				// assert result > 0 : this + " " + state + " " + i + " " +
				// j+linkExpEnergy(state, j, (Perspective) facets.get(j),
				// i, caused);
				// Util.print("energy " + state + " " +
				// caused
				// + " " + weight);
			}
		}
		return result;
	}

	private double energy(int state) {
		int nFacets = nFacets();
		double sum = 0;
		for (int i = 0; i < nFacets; i++) {
			for (int j = 0; j <= i; j++) {
				sum += linkEnergy(state, i, j);
				// Util
				// .print("energy " + state + " " + i + " " + j + " "
				// + linkEnergy(state, i, j));
			}
		}
		return sum;
	}

	// double biasEnergy(int state, int node) {
	// return Util.isBit(state, node) ? getWeight(node, node) : 0;
	// }
	//
	// double biasExpEnergy(int state, int node) {
	// return Util.isBit(state, node) ? getExpWeight(node, node) : 1;
	// }

	double linkEnergy(int state, int causeNode, int causedNode) {
		return hasEdge(causeNode, causedNode) && Util.isBit(state, causedNode)
				&& Util.isBit(state, causeNode) ? getWeight(causeNode,
				causedNode) : 0;
	}

	double linkExpEnergy(int state, int causeNode, int causedNode) {
		return hasEdge(causeNode, causedNode) && Util.isBit(state, causedNode)
				&& Util.isBit(state, causeNode) ? getExpWeight(causeNode,
				causedNode) : 1;
	}

	// protected double[] getMarginal(List marginals) {
	// return getMarginal(predictedDistribution(), getMarginIndexes(marginals));
	// }
	//
	// protected static double[] getMarginal(double[] dist, int[] indexes) {
	// double[] result = new double[1 << indexes.length];
	// for (int state = 0; state < dist.length; state++) {
	// result[getSubstate(state, indexes)] += dist[state];
	// }
	// return result;
	// }
	//
	// protected int[] getMarginalCounts(List marginals) {
	// return getMarginalCounts(counts, getMarginIndexes(marginals));
	// }
	//
	// protected static int[] getMarginalCounts(int[] dist, int[] indexes) {
	// int[] result = new int[1 << indexes.length];
	// for (int state = 0; state < dist.length; state++) {
	// result[getSubstate(state, indexes)] += dist[state];
	// }
	// return result;
	// }
	//
	// protected static double[] getMarginal(int[] dist, int[] indexes) {
	// double[] result = new double[1 << indexes.length];
	// int count = 0;
	// for (int state = 0; state < dist.length; state++) {
	// count += dist[state];
	// result[getSubstate(state, indexes)] += dist[state];
	// }
	// for (int i = 0; i < result.length; i++) {
	// result[i] /= count;
	// }
	// return result;
	// }
	//
	// int[] getMarginIndexes(List marginals) {
	// int[] result = new int[marginals.size()];
	// int index = 0;
	// for (Iterator it = marginals.iterator(); it.hasNext();) {
	// Object facet = it.next();
	// result[index++] = facets.indexOf(facet);
	// }
	// return result;
	// }

	// static int getSubstate(int state, int[] indexes) {
	// int result = 0;
	// for (int index = 0; index < indexes.length; index++) {
	// result = Util.setBit(result, index, Util.isBit(state,
	// indexes[index]));
	// }
	// return result;
	// }
	//
	// static int setSubstate(int state, int substate, int[] indexes) {
	// for (int subvar = 0; subvar < indexes.length; subvar++) {
	// state = Util.setBit(state, indexes[subvar], Util.isBit(substate,
	// subvar));
	// }
	// // Util.print("setState " + substate + " " + Util.valueOfDeep(indexes)
	// // + " " + state);
	// return state;
	// }
	//
	// static double KLdivergence(double[] observed, double[] logPredicted) {
	// double sum = 0;
	// for (int i = 0; i < observed.length; i++) {
	// // assert predicted[i] > 0 : Util.valueOfDeep(predicted);
	// sum -= observed[i] * logPredicted[i];
	// }
	// if (Double.isNaN(sum))
	// Util.print("KLdivergence " + sum + " "
	// + Util.valueOfDeep(logPredicted) + "\n"
	// + Util.valueOfDeep(observed));
	// return sum;
	// }

	// static double KLdivergence(double[] observed, double[] predicted) {
	// double sum = 0;
	// for (int i = 0; i < observed.length; i++) {
	// // assert predicted[i] > 0 : Util.valueOfDeep(predicted);
	// if (observed[i] > 0) {
	// if (predicted[i] == 0)
	// sum = Double.POSITIVE_INFINITY;
	// else
	// sum += observed[i] * Math.log(observed[i] / predicted[i]);
	// }
	// }
	// if (Double.isNaN(sum))
	// Util.print("KLdivergence " + sum + " "
	// + Util.valueOfDeep(predicted) + "\n"
	// + Util.valueOfDeep(observed));
	// return sum;
	// }

	// double quality() {
	// // Util.print(buildGraph());
	// return weightSpaceChange(nullModel) * WEIGHT_SPACE_IMPORTANCE
	// - (1 - WEIGHT_SPACE_IMPORTANCE)
	// * KLdivergence(nullModel.getDistribution(false), getMarginal());
	// }
	//
	// Query query() {
	// // We allow facets to be empty, as long as we're not primary
	// Query query = ((Perspective) Util.some(primaryFacets())).query();
	// return query;
	// }

	void addEdges(Set edges) {
		// Util.print("addEdges " + edges);
		for (Iterator it = edges.iterator(); it.hasNext();) {
			List edge = (List) it.next();
			addEdge((Perspective) edge.get(0), (Perspective) edge.get(1));
		}
	}

	void removeEdges(Set edges) {
		for (Iterator it = edges.iterator(); it.hasNext();) {
			List edge = (List) it.next();
			removeEdge((Perspective) edge.get(0), (Perspective) edge.get(1));
		}
	}

	void clearEdges() {
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			removeEdge(edge[0], edge[1]);
		}
	}

	public Graph buildGraph(Distribution observedDistForNormalization,
			Explanation nullModel) {
		Graph graph = new Graph((GraphWeigher) null);
		Map nodeMap = new HashMap();
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			ensureNode(observedDistForNormalization, graph, nodeMap, p);
		}
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			// ensureNode(observedDistForNormalization, graph, nodeMap, cause);
			Perspective caused = getFacet(edge[1]);
			// ensureNode(observedDistForNormalization, graph, nodeMap, caused);
			addEdge(observedDistForNormalization, graph, nodeMap, cause,
					caused, nullModel);
			if (isSymmetric)
				addEdge(observedDistForNormalization, graph, nodeMap, caused,
						cause, nullModel);
		}
		assert !graph.getNodes().isEmpty() : printGraph(observedDistForNormalization);
		return graph;
	}

	protected String printGraph(Distribution observedDistForNormalization) {
		Util.print("printGraph " + this);
		for (Iterator it = facets().iterator(); it.hasNext();) {
			Perspective caused = (Perspective) it.next();
			Util.print(getWeight(caused, caused) + " " + caused);
		}
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			Perspective caused = getFacet(edge[1]);
			double weight = getWeight(cause, caused);
			// if (weight != 0 && cause.compareTo(caused) > 0)
			Util
					.print(weight
							+ " "
							+ cause
							+ " => "
							+ caused
							+ (observedDistForNormalization != null ? " ("
									+ getRNormalizedWeight(
											observedDistForNormalization,
											cause, caused)
									+ " "
									+ getRNormalizedWeight(
											observedDistForNormalization,
											caused, cause) + ")" : ""));
		}
		Util.print("");
		return ""; // suitable for assert messages
	}

	private double sumStdDevNormalizedWeights(Perspective caused) {
		List causes = getCauses(caused);
		double sumWeights = 0;
		for (Iterator it2 = causes.iterator(); it2.hasNext();) {
			Perspective cause = (Perspective) it2.next();
			sumWeights += Math.abs(getStdDevNormalizedWeight(cause, caused));
		}
		return sumWeights;
	}

	private Node ensureNode(Distribution observedDistForNormalization,
			Graph graph, Map nodeMap, Perspective facet) {
		assert graph != null;
		assert facet != null;
		Node result = (Node) nodeMap.get(facet);
		if (result == null) {
			String label = facet.toString();
			if (observedDistForNormalization != null)
				label = formatWeight(observedDistForNormalization.R(
						getDistribution(), facet))
						+ " " + label;
			// Prefix with space so edge line doesn't merge with any minus sign
			result = graph.addNode(facet, " "+label);
			nodeMap.put(facet, result);
		}
		assert result != null : facet;
		return result;
	}

	private void addEdge(Distribution observedDistForNormalization,
			Graph graph, Map nodeMap, Perspective cause, Perspective caused,
			Explanation nullModel) {
		// Util.print("addRule " + negLiteral + " " + posLiteral);
		Node posNode = ensureNode(observedDistForNormalization, graph, nodeMap,
				caused);
		// if (cause != null) {
		Node negNode = ensureNode(observedDistForNormalization, graph, nodeMap,
				cause);
		// Util.print("addEdge " + posNode + " " + negNode);
		Edge edge = graph.getEdge(posNode, negNode);
		if (edge == null)
			edge = graph.addEdge((String) null, posNode, negNode);
		String label = formatWeight(getRNormalizedWeight(
				observedDistForNormalization, cause, caused));
		if (nullModel.facets().contains(cause)
				&& nullModel.facets().contains(caused))
			label = formatWeight(nullModel.getRNormalizedWeight(cause, caused))
					+ " > " + label;
		edge.setLabel("      " + label + "      ", posNode);
		edge
				.setLabel(formatWeight(getWeight(cause, caused)),
						Edge.CENTER_LABEL);
		// }
	}

	// double R(Perspective caused) {
	// double pseudoRsquared = pseudoRsquared(caused);
	// return Math.sqrt(Math.abs(pseudoRsquared)) * Util.sgn(pseudoRsquared);
	// // double sumR = 0;
	// // List causes = getCauses(caused);
	// // for (Iterator causeIt = causes.iterator(); causeIt.hasNext();) {
	// // Perspective cause = (Perspective) causeIt.next();
	// // sumR += Math.abs(getWeight(cause, caused));
	// // }
	// // return sumR;
	// }

	/**
	 * @param causes
	 * @param causeds
	 * @return [[cause1, caused1], ... Does not return biases
	 */
	static Set allEdges(Collection causes, Collection causeds) {
		// Util.print("addEdges " + causes+" "+causeds);
		HashSet result = new HashSet();
		for (Iterator it1 = causeds.iterator(); it1.hasNext();) {
			Perspective caused = (Perspective) it1.next();
			for (Iterator it2 = causes.iterator(); it2.hasNext();) {
				Perspective cause = (Perspective) it2.next();
				if (caused.compareTo(cause) > 0) {
					List edge = new ArrayList(2);
					edge.add(cause);
					edge.add(caused);
					result.add(edge);
				}
			}
		}
		return result;
	}

	// Set getEdges() {
	// // Util.print("addEdges " + causes+" "+causeds);
	// HashSet result = new HashSet();
	// for (Iterator it1 = facets.iterator(); it1.hasNext();) {
	// Perspective caused = (Perspective) it1.next();
	// for (Iterator it2 = getCauses(caused).iterator(); it2.hasNext();) {
	// Perspective cause = (Perspective) it2.next();
	// assert caused != cause;
	// if (cause.compareTo(caused) > 0) {
	// List edge = new ArrayList(2);
	// edge.add(cause);
	// edge.add(caused);
	// result.add(edge);
	// }
	// }
	// }
	// // Util.print("getEdges " + result);
	// return result;
	// }

	Set getEdges() {
		// Util.print("addEdges " + causes+" "+causeds);
		HashSet result = new HashSet();
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			Perspective caused = getFacet(edge[1]);
			assert caused != cause;
			List edgeList = new ArrayList(2);
			edgeList.add(cause);
			edgeList.add(caused);
			result.add(edgeList);
		}
		// Util.print("getEdges " + result);
		return result;
	}

	// public String toString() {
	// StringBuffer buf = new StringBuffer();
	// buf.append("<").append(Util.shortClassName(this))//.append(" quality=").
	// // append(quality()
	// // )
	// .append(" ").append(facets);
	//
	// if (false) {
	// for (Iterator it1 = facets().iterator(); it1.hasNext();) {
	// Perspective p1 = (Perspective) it1.next();
	// for (Iterator it2 = facets().iterator(); it2.hasNext();) {
	// Perspective p2 = (Perspective) it2.next();
	// if (p1 != p2) {
	// double weight = getWeight(p1, p2);
	// if (weight != 0) {
	// buf.append("\n").append(weight).append(" (")
	// .append(getStdDevNormalizedWeight(p1, p2))
	// .append(") ").append(p1).append(" ---- ")
	// .append(p2);
	// }
	// }
	// }
	// }
	// }
	//
	// return buf.toString();
	// }

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(Util.shortClassName(this)).append(" ").append(
				facets);
		if (false) {
			buf.append(" nEdges=").append(nEdges());
		} else {
			buf.append(getEdges());
		}
		buf.append(" ").append(Util.valueOfDeep(getMarginalCounts(facets)))
				.append(">");
		return buf.toString();
	}

	public int nEdges() {
		int result = 0;
		for (int cause = 0; cause < nFacets(); cause++) {
			for (int caused = 0; caused < nFacets(); caused++) {
				if (cause != caused && expWeights[cause][caused] >= 0
						&& (!isSymmetric || cause < caused))
					result++;
			}
		}
		return result;
	}

	// private static final StringAlign align = new StringAlign(7,
	// StringAlign.JUST_RIGHT);
	//
	// static final DecimalFormat countFormat = new DecimalFormat("###,###");
	//
	// static final DecimalFormat percentFormat = new DecimalFormat("###%");
	//
	// static final DecimalFormat doubleFormat = new DecimalFormat(
	// " #0.000;-#0.000");
	//
	// static String formatPercent(double x) {
	// return (new StringAlign(4, StringAlign.JUST_RIGHT)).format(x,
	// percentFormat);
	// // return percentFormat.format(x);
	// }
	//
	// static String formatDouble(double x) {
	// String format = align.format(x, doubleFormat);
	// // return StringAlign.format(format, null, 6, StringAlign.JUST_LEFT)
	// // .toString();
	// return format;
	// }
	//
	// String formatResidual(double x) {
	// return formatInt((int) (10000 * x / query().getTotalCount()));
	// }
	//
	// static String formatInt(int x) {
	// return align.format(x, countFormat);
	// }

	// /**
	// * Efron's Pseudo R-Squared see http://www.ats.ucla.edu/stat/mult_pkg/faq
	// * /general/Psuedo_RSquareds.htm
	// */
	// double pseudoRsquared(double[] predicted, Perspective caused) {
	// // Use int's to aid debugging
	// double residuals = 0;
	// double baseResiduals = 0;
	// int causedIndex = facets.indexOf(caused);
	// int[] causeds = { causedIndex };
	// double[] marginal = getMarginal(counts, causeds);
	// double unconditional = marginal[1] / (marginal[0] + marginal[1]);
	// if (PRINT_RSQUARED) {
	// for (Iterator it = facets.iterator(); it.hasNext();) {
	// Perspective p = (Perspective) it.next();
	// if (p != caused)
	// System.out.print(p.getName().substring(0, 5) + " ");
	// }
	// System.out
	// .print(
	// "                observed pred residua uncondi off_nrg  on_nrg   delta");
	// Util.print("");
	// }
	// for (int offState = 0; offState < counts.length; offState++) {
	// if (Util.getBit(offState, causedIndex) == 0) {
	// int onState = Util.setBit(offState, causedIndex, true);
	// int obsOn = counts[onState];
	// int obsOff = counts[offState];
	// double yHat = predicted[onState]
	// / (predicted[onState] + predicted[offState]);
	// double residual = yHat * yHat * obsOff + (1 - yHat)
	// * (1 - yHat) * obsOn;
	// residuals += residual;
	//
	// // Util.print(yHat * yHat * obsOff + " " + (1 - yHat) * (1 -
	// // yHat)
	// // * obsOn);
	//
	// // We're calculating this state-by-state for the benefit of
	// // printing
	// double baseResidual = unconditional * unconditional * obsOff
	// + (1 - unconditional) * (1 - unconditional) * obsOn;
	// baseResiduals += baseResidual;
	//
	// // Util.print(unconditional * unconditional * obsOff);
	// // Util.print((1 - unconditional) * (1 - unconditional) *
	// // obsOn);
	//
	// if (PRINT_RSQUARED) {
	// for (int i = 0; i < facets.size(); i++) {
	// if (i != causedIndex)
	// System.out
	// .print(Util.getBit(offState, i) + "     ");
	// }
	// Util.print(formatInt(obsOn)
	// + " / "
	// + formatInt(obsOn + obsOff)
	// + " = "
	// + formatPercent(obsOn / (double) (obsOn + obsOff))
	// + " "
	// + formatPercent(yHat)
	// + " "
	// + formatResidual(residual)
	// + " "
	// + formatResidual(baseResidual)
	// + " "
	// + formatDouble(Math.log(predicted[offState]
	// / predicted[0]))
	// + " "
	// + formatDouble(Math.log(predicted[onState]
	// / predicted[0]))
	// + " "
	// + formatDouble(Math.log(predicted[onState]
	// / predicted[offState])) + " "
	// + formatDouble(predicted[offState]) + " "
	// + formatDouble(predicted[onState]));
	// }
	//
	// }
	// }
	// // double base = pseudoRsquaredDenom(caused);
	// // assert Math.abs(params.yCount() - sumYhat) < 1 : params.yCount() +
	// // " "
	// // + sumYhat + report + "\n" + yHats();
	// double Rsquare = 1 - residuals / baseResiduals;
	// if (PRINT_RSQUARED) {
	// Util.print("pseudoR="
	// + formatDouble(Math.sqrt(Math.abs(Rsquare))
	// * Util.sgn(Rsquare)) + " " + caused
	// + " unconditional=" + formatPercent(unconditional) + " "
	// + formatResidual(residuals) + "/"
	// + formatResidual(baseResiduals) + "\n");
	// }
	//
	// // Sometimes Alchemy returns a model that predicts a node worse than the
	// // unconditional average
	// // assert Rsquare >= -0.01 && Rsquare <= 1.01 : printGraph(false) + " "
	// // + Rsquare + " " + base + " " + residuals + " "
	// // + caused.getOnCount() + " " + sumYhat;
	// Rsquare = Math.max(0, Rsquare);
	//
	// return Rsquare;
	// }

	// private static double yHat(int index, int state, double[] dist) {
	// double on = dist[Util.setBit(state, index, true)];
	// double off = dist[Util.setBit(state, index, false)];
	// double result = on / (on + off);
	// return result;
	// }

	// private double pseudoRsquaredDenom(Perspective p) {
	// double nRows = query().getTotalCount();
	// double sumY = p.getTotalCount();
	// double yMean = sumY / nRows;
	// double diff0 = yMean;
	// double diff1 = 1 - yMean;
	// double base = (nRows - sumY) * diff0 * diff0 + sumY * diff1 * diff1;
	// return base;
	// }

}
