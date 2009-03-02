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
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;
import edu.cmu.cs.bungee.javaExtensions.graph.Node;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph.GraphWeigher;

public class GraphicalModel extends Distribution {
	
	boolean edgesFixed=false;

	final boolean isSymmetric;

	/**
	 * {node1, node2) => weight
	 */
	protected final double[][] weights;

	/**
	 * Non-existent edges are coded -2; uncached are coded -1
	 */
	final double[][] expWeights;

	int[][] edgeIndexes;

	private double[] logDistribution;

	private double z = -1;

	GraphicalModel(List facets, Set edges, boolean isSymmetric, int count) {
		super(facets, count);
		this.isSymmetric = isSymmetric;
		weights = new double[nFacets()][];
		expWeights = new double[nFacets()][];
		edgeIndexes = new int[nFacets()][];
		for (int i = 0; i < weights.length; i++) {
			edgeIndexes[i] = new int[nFacets()];
			weights[i] = new double[nFacets()];
			expWeights[i] = new double[nFacets()];
			for (int j = 0; j < expWeights.length; j++) {
				expWeights[i][j] = i == j ? -1 : -2;
			}
		}
		if (edges == null)
			edges = allEdges(facets, facets);
		assert !isSymmetric || isEdgesCanonical(edges);
		addEdges(edges);
		// Util.print("GraphicalModel " + getEdges());
		// for (Iterator it = facets.iterator(); it.hasNext();) {
		// Perspective facet = (Perspective) it.next();
		// addEdge(facet, facet);
		// }
		// Util.print("jj "+Util.valueOfDeep(expWeights));
	}

	private boolean isEdgesCanonical(Set edges) {
		for (Iterator it = edges.iterator(); it.hasNext();) {
			List edge = (List) it.next();
			assert ((Perspective) edge.get(0)).compareTo(edge.get(1)) < 0 : edges
					+ " " + edge;
		}
		return true;
	}

	protected boolean hasEdge(Perspective cause, Perspective caused) {
		int causeNode = facetIndexOrNot(cause);
		if (causeNode < 0)
			return false;
		int causedNode = facetIndexOrNot(caused);
		if (causedNode < 0)
			return false;
		return expWeights[causeNode][causedNode] > -2;
	}

	protected boolean hasEdge(int causeNode, int causedNode) {
		return expWeights[causeNode][causedNode] > -2;
	}

	double getExpWeight(Perspective cause, Perspective caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		return getExpWeight(facetIndex(cause), facetIndex(caused));
	}

	protected double getExpWeightOrZero(Perspective cause, Perspective caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		return hasEdge(cause, caused) ? getExpWeight(facetIndex(cause),
				facetIndex(caused)) : 1;
	}

	protected double getExpWeight(int i, int j) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		double result = expWeights[i][j];
		if (result < 0) {
			assert result==-1:result;
			result = Math.exp(getWeight(i, j));
			expWeights[i][j] = result;
			expWeights[j][i] = result;
//			NonAlchemyModel.nExpWeight++;
		}
		assert result >= 0 : result + " " + i + " " + j + " " + getWeight(i, j);
		assert !Double.isNaN(result);
		assert !Double.isInfinite(result);
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
		assert hasEdge(cause, caused) : cause + " " + caused + " " + facets()
				+ " " + Util.valueOfDeep(expWeights);
		double result = weights[cause][caused];
		return result;
	}

	protected double getRNormalizedWeightOrZero(
			Distribution observedDistribution, Perspective cause,
			Perspective caused) {
		// Util.print("getWeight " + cause + " => " + caused + " "
		// + getWeight(getEdge(cause, caused)));
		return hasEdge(cause, caused) ? getRNormalizedWeight(
				observedDistribution, cause, caused) : 0;
	}

	double getRNormalizedWeight(Distribution observedDistribution,
			Perspective cause, Perspective caused) {
		Distribution obs = LimitedCausesDistribution.getInstance(facets,
				observedDistribution.getMarginalCounts(facets), getEdges());
		return obs.averageSumR2(cause, caused, this)
				* Util.sgn(getWeight(cause, caused));
	}

	double getStdDevNormalizedWeight(Perspective cause, Perspective caused) {
		return getWeight(cause, caused) * stdDev(cause);
	}

	/**
	 * Can't use Perspective.stdDev, because deeply nested facets have
	 * totalCount = -1
	 * 
	 * @return standard deviation of binary variable p, over the whole database
	 */
	double stdDev(Perspective p) {
		List facets1 = new ArrayList(1);
		facets1.add(p);
		Distribution dist = Distribution.getObservedDistribution(facets1, null);
		double n = dist.totalCount;
		double count = dist.getDistribution()[1];
		double stdDev = Math.sqrt(count * (n - count) / (n * (n - 1)));
		assert stdDev >= 0 : count + " " + n + " " + this;
		return stdDev;
	}

	double[] getWeights() {
		double[] result = new double[nFacets() + nEdges()];
		int argIndex = 0;
		for (int i = 0; i < nFacets(); i++) {
			result[argIndex++] = getWeight(i, i);
		}

		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			result[argIndex++] = getWeight(edge[0], edge[1]);
		}
		return result;
	}

	boolean setWeights(double[] argument) {
		boolean result = false;
		int argIndex;
		for (argIndex = 0; argIndex < nFacets(); argIndex++) {
			result = setWeight(argIndex, argIndex, argument[argIndex])
					|| result;

		}
		for (int cause = 0; cause < nFacets(); cause++) {
			for (int caused = cause + 1; caused < nFacets(); caused++) {
				if (hasEdge(cause, caused))
					result = setWeight(cause, caused, argument[argIndex++])
							|| result;
			}
		}
//		if (!result) {
//			NonAlchemyModel.nNoopSetWeights++;
//			Util.print(" noop setWeights");
//		} else {
//			NonAlchemyModel.expectedS += nStates();
//			NonAlchemyModel.expectedW += argument.length;
//			Util.print(" setWeights");
//		}
//		NonAlchemyModel.nSetWeights++;

		// for (Iterator it = getEdgeIterator(); it.hasNext();) {
		// int[] edge = (int[]) it.next();
		// setWeight(edge[0], edge[1], argument[argIndex++]);
		// }
		return result;
	}

	boolean setWeight(Perspective cause, Perspective caused, double weight) {
		return setWeight(facetIndex(cause), facetIndex(caused), weight);
	}

	boolean setWeight(int cause, int caused, double weight) {
		// Util.print("setWeight " + cause + " => " + caused + " " + weight);
		assert !edgesFixed;
		boolean result = getWeight(cause, caused) != weight;
		if (result) {
			assert !Double.isInfinite(weight);
			assert !Double.isNaN(weight);
			distribution = null;
			logDistribution = null;
			z = -1;
			odds = -1;

			weights[cause][caused] = weight;
			double expWeight = -1; // Math.exp(weight);
			// assert expWeight > 0 : weight;
			expWeights[cause][caused] = expWeight;
			if (isSymmetric) {
				weights[caused][cause] = weight;
				expWeights[caused][cause] = expWeight;
			}
//			NonAlchemyModel.nSetWeight++;
		}
		return result;
	}

	List getEdge(Perspective cause, Perspective caused) {
		List edge = new ArrayList(2);
		edge.add(cause);
		edge.add(caused);
		return edge;
	}

	void addEdge(Perspective cause, Perspective caused) {
		addEdge(facetIndex(cause), facetIndex(caused));
	}

	void addEdge(int cause, int caused) {
		// Util.print("addEdge " + cause + " " + caused + " " + isSymmetric);
		assert !edgesFixed;
		assert cause != caused;
		assert !hasEdge(cause, caused);
		expWeights[cause][caused] = 1;
		weights[cause][caused] = 0;
		if (isSymmetric) {
			expWeights[caused][cause] = 1;
			weights[caused][cause] = 0;
		}
		edgeIndexes = null;
	}

	int[][] getEdgeIndexes() {
		if (edgeIndexes == null) {
			int nFacets = nFacets();
			edgeIndexes = new int[nFacets][];
			for (int i = 0; i < nFacets; i++) {
				edgeIndexes[i] = new int[nFacets];
				for (int j = 0; j < nFacets; j++) {
					edgeIndexes[i][j] = -1;
				}
			}
			int edgeIndex = nFacets;
			for (Iterator it = getEdgeIterator(); it.hasNext();) {
				int[] edge = (int[]) it.next();
				int cause = edge[0];
				int caused = edge[1];
				if (hasEdge(cause, caused)) {
					edgeIndexes[cause][caused] = edgeIndex;
					if (isSymmetric)
						edgeIndexes[caused][cause] = edgeIndex;
					edgeIndex++;
				}
			}
		}
		return edgeIndexes;
	}

	void removeEdge(Perspective cause, Perspective caused) {
		removeEdge(facetIndex(cause), facetIndex(caused));
	}

	void removeEdge(int cause, int caused) {
		// Util.print("removeEdge " + cause + " " + caused);
		assert hasEdge(cause, caused);
		assert !edgesFixed;
		expWeights[cause][caused] = -2;
		if (isSymmetric) {
			expWeights[caused][cause] = -2;
		}
		edgeIndexes = null;
	}

	protected List getCauses(Perspective caused) {
		List result = new LinkedList();
		int causedNode = facetIndex(caused);
		if (causedNode >= 0) {
			for (int causeNode = 0; causeNode < nFacets(); causeNode++) {
				if (causeNode != causedNode && hasEdge(causeNode, causedNode))
					result.add(getFacet(causeNode));
			}
		}
		// Util.print("getCauses " + caused + " " + causes);
		return result;
	}

	/**
	 * @return [cause, caused] in this order [0, 1], [0, 2], [0, 3], [1, 2], [1,
	 *         3], [2, 3]
	 * 
	 *         i.e. for (int cause = 0; cause < nFacets; cause++) { for (int
	 *         caused = cause; caused <nFacets; caused++) {
	 * 
	 *         for xvec, these follow the bias weights
	 */
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
								&& hasEdge(nextCause, nextCaused)
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

	private int[][] stateWeights;

	int[][] stateWeights() {
		if (stateWeights == null) {
			stateWeights = new int[nStates()][];
			int nFacets = nFacets();
			for (int state = 0; state < stateWeights.length; state++) {
				int[] wts = new int[0];
				int argIndex = nFacets;
				for (int cause = 0; cause < nFacets; cause++) {
					for (int caused = cause; caused < nFacets; caused++) {
						if (hasEdge(cause, caused)) {
							if (Util.isBit(state, caused)) {
								if (cause == caused) {
									wts = Util.push(wts, cause);
								} else if (Util.isBit(state, cause)) {
									wts = Util.push(wts, argIndex);
								}
							}
							if (caused > cause)
								argIndex++;
						}
					}
				}
				stateWeights[state] = wts;
			}
			// Util.print("sw " + Util.valueOfDeep(stateWeights));
		}
		return stateWeights;
	}

	private int[][][] stateEdges;

	int[][][] stateEdges() {
		if (stateEdges == null) {
			stateEdges = new int[nStates()][][];
			int nFacets = nFacets();
			for (int state = 0; state < stateEdges.length; state++) {
				int[][] wts = new int[0][];
				for (int cause = 0; cause < nFacets; cause++) {
					for (int caused = cause; caused < nFacets; caused++) {
						if (hasEdge(cause, caused)) {
							if (Util.isBit(state, cause)
									&& Util.isBit(state, caused)) {
								int[] edge = { cause, caused };
								wts = (int[][]) Util.push(wts, edge,
										int[].class);
							}
						}
					}
				}
				stateEdges[state] = wts;
			}
			// Util.print("se " + Util.valueOfDeep(stateEdges));
		}
		return stateEdges;
	}

	/**
	 * @param smallerObserved
	 * @param observed
	 * @param fastMax
	 * @return distance in displayed parameter space from nullModel
	 */
	double weightSpaceChange(GraphicalModel smallerModel, List primaryFacets,
			Distribution smallerObserved, Distribution observed, boolean fastMax) {
		// Util.print("wsc "+primaryFacets);
		double delta2 = 0;
		// double initial = 0;
		for (Iterator causedIt = primaryFacets.iterator(); causedIt.hasNext();) {
			Perspective caused = (Perspective) causedIt.next();
			for (Iterator causeIt = primaryFacets.iterator(); causeIt.hasNext();) {
				Perspective cause = (Perspective) causeIt.next();
				if (cause.compareTo(caused) < 0) {
					double change = weightSpaceChange(smallerModel,
							smallerObserved, observed, fastMax, cause, caused);
					double smoothChange = Math.sqrt(change * change
							+ NonAlchemyModel.WEIGHT_STABILITY_SMOOTHNESS)
					// - Math
					// .sqrt(NonAlchemyModel.WEIGHT_STABILITY_SMOOTHNESS)
					;
					// Util.print("wsc " + smoothChange +" "+cause+" "+caused);
					delta2 += smoothChange;
				}
			}
		}
		// Util.print("weightSpaceChange " + this);
		// printGraph(null);
		// printGraph(false);
		// Util.print("");
		// reference.printGraph(false);
		// Util.print("weightSpaceChange done\n");
		assert !Double.isNaN(delta2);
		// double delta = Math.sqrt(delta2);
		return delta2;
	}

	double weightSpaceChange(GraphicalModel smallerModel,
			Distribution smallerObserved, Distribution observed,
			boolean fastMax, Perspective cause, Perspective caused) {
		double diff = 0;

		// Don't normalize, because a single strong predictor will
		// change the proportions, but not the underlying
		// dependency.
		double diffN = Double.POSITIVE_INFINITY;
		if (!fastMax) {
			double weight0N = smallerModel.getRNormalizedWeightOrZero(
					smallerObserved, cause, caused);
			double weightN = getRNormalizedWeightOrZero(observed, cause, caused);
			// Util.print("wsc "+weight0N+" "+weightN);
			diffN = Math.abs(weightN - weight0N);

			weight0N = smallerModel.getRNormalizedWeightOrZero(smallerObserved,
					caused, cause);
			weightN = getRNormalizedWeightOrZero(observed, caused, cause);
			// Util.print("wsc "+weight0N+" "+weightN);
			diffN += Math.abs(weightN - weight0N);
			diffN /= 2;
		}

		double diffU = Math.abs(effectiveWeight(cause, caused)
				- smallerModel.effectiveWeight(cause, caused));

		diff = Math.min(diffN, diffU);

		// if (!fastMax)
		// Util.print("weightSpaceChange " + diffN + " " + diffU + " "
		// + sigmoid(weight0U) + " " + sigmoid(weightU) + " " + weight0U
		// + " " + weightU + " " + cause + " " + caused
		// // + " norm W0=" + weight0N + " norm W=" + weightN
		// );

		return diff;
	}

	double sigmoid(double w) {
		// return w;
		return 1.0 / (1.0 + Math.exp(-w));
	}

	double effectiveWeight(Perspective cause, Perspective caused) {
		if (NonAlchemyModel.USE_SIGMOID) {
			double expw = getExpWeightOrZero(cause, caused);
			return expw / (expw + 1);
		} else {
			return getWeightOrZero(cause, caused);
		}
	}

	protected static String formatWeight(double weight) {
		if (Double.isNaN(weight))
			return "?";
		return Integer.toString((int) Math.rint(100 * weight));
	}

	public double[] getDistribution() {
		if (distribution == null) {
//			NonAlchemyModel.nGetDistribution++;
			z();
		}
		return super.getDistribution();
	}

	protected double[] logPredictedDistribution() {
		if (logDistribution == null) {
			int nStates = 1 << facets.size();
			double logZ = Math.log(z());
			logDistribution = new double[nStates];
			for (int state = 0; state < nStates; state++) {
				logDistribution[state] = energy(state) - logZ;
				// Util.print("state " + state + " " + energy(state) + " "
				// + p[state]);
			}

			// Util.print("logZ=" + logZ);
			// double[] old = getDistribution();
			// for (int state = 0; state < logP.length; state++) {
			// Util.print(state + " " + energy(state) + " old=" + old[state]
			// + " new=" + Math.exp(logP[state]) + " log=" + logP[state]);
			// }
//			NonAlchemyModel.nLogPredictedDistribution++;
		}
		return logDistribution;
	}

	/**
	 * Computes distribution as a side effect
	 */
	protected double z() {
		if (z < 0) {
			int nStates = 1 << facets.size();
			z = 0;
			distribution = new double[nStates];
			for (int state = 0; state < nStates; state++) {
				double ee = expEnergy(state);
				distribution[state] = ee;
				z += ee;
			}
			if (Double.isInfinite(z)) {
				z = 0;
				for (int state = 0; state < nStates; state++) {
					Util.print(state+" "+expEnergy(state));
					if (Double.isInfinite(distribution[state])) {
						z++;
						distribution[state] = 1;
					} else {
						distribution[state] = 0;
					}
				}
				Util.print("infinite z " + z+" "+Util.valueOfDeep(getWeights()));
			}
			// Util.print(Util.valueOfDeep(getWeights()));
			// Util.print(z);
			for (int state = 0; state < nStates; state++) {
				distribution[state] /= z;
				// Util.print("state " + state + " " + energy(state) + " "
				// + expEnergy(state) + " " + distribution[state]);
			}
			assert checkWeight(distribution);
			assert z >= 0;
//			NonAlchemyModel.nZ++;
		}
		return z;
	}

	double energy(int state) {
//		NonAlchemyModel.nEnergy++;
		int[][] edges = stateEdges()[state];
		double result = 0;
		for (int edge = 0; edge < edges.length; edge++) {
			int cause = edges[edge][0];
			int caused = edges[edge][1];
			result += getWeight(cause, caused);
		}
		return result;
	}

	double expEnergy(int state) {
//		NonAlchemyModel.nExpEnergy++;
		int[][] edges = stateEdges()[state];
		double result = 1;
		for (int edge = 0; edge < edges.length; edge++) {
			int cause = edges[edge][0];
			int caused = edges[edge][1];
			result *= getExpWeight(cause, caused);
		}
		return result;
	}

	double linkEnergy(int state, int causeNode, int causedNode) {
		return hasEdge(causeNode, causedNode) && Util.isBit(state, causedNode)
				&& Util.isBit(state, causeNode) ? getWeight(causeNode,
				causedNode) : 0;
	}

	protected Distribution getMarginalDistribution(List subFacets) {
		int nSubFacets = subFacets.size();
		if (nSubFacets == nFacets())
			return this;
		Set subedges = getEdgesAmong(subFacets);
		return LimitedCausesDistribution.getInstance(subFacets,
				getMarginalCounts(subFacets), subedges);
	}

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
			Explanation nullModel, PerspectiveObserver redrawer) {
		Graph graph = new Graph((GraphWeigher) null);
		Map nodeMap = new HashMap();
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			ensureNode(observedDistForNormalization, graph, nodeMap, p,
					redrawer);
		}
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			// ensureNode(observedDistForNormalization, graph, nodeMap, cause);
			Perspective caused = getFacet(edge[1]);
			// ensureNode(observedDistForNormalization, graph, nodeMap, caused);
			addEdge(observedDistForNormalization, graph, nodeMap, cause,
					caused, nullModel, redrawer);
			if (isSymmetric)
				addEdge(observedDistForNormalization, graph, nodeMap, caused,
						cause, nullModel, redrawer);
		}
		assert !graph.getNodes().isEmpty() : printGraph(
				observedDistForNormalization, null);
		return graph;
	}

	protected String printGraph(Distribution observedDistForNormalization,
			List facetsOfInterest) {
		Util
				.print("printGraph "
						+ this
						+ (observedDistForNormalization != null ? " KL="
								+ observedDistForNormalization
										.klDivergenceFromLog(logPredictedDistribution())
								// + " sumR="
								// + observedDistForNormalization.sumR(this)
								: ""));

		// Util.print("pred=");
		// printCounts();
		// Util.print(" obs=");
		// observedDistForNormalization.printCounts();

		for (Iterator it = facets().iterator(); it.hasNext();) {
			Perspective caused = (Perspective) it.next();
			if (facetsOfInterest == null)
				Util.print(getWeight(caused, caused) + " " + caused);
		}
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			Perspective caused = getFacet(edge[1]);
			double weight = getWeight(cause, caused);
			// if (weight != 0 && cause.compareTo(caused) > 0)
			if (facetsOfInterest == null
					|| (facetsOfInterest.contains(cause) && facetsOfInterest
							.contains(caused)))
				Util.print(weight
						+ " "
						+ cause
						+ " => "
						+ caused
						+ (observedDistForNormalization != null ? " ("
								+ getRNormalizedWeight(
										observedDistForNormalization, cause,
										caused)
								+ " "
								+ getRNormalizedWeight(
										observedDistForNormalization, caused,
										cause) + ")" : ""));
		}
		Util.print("");
		return ""; // suitable for assert messages
	}

	private Node ensureNode(Distribution observedDistForNormalization,
			Graph graph, Map nodeMap, Perspective facet,
			PerspectiveObserver redrawer) {
		assert graph != null;
		assert facet != null;
		Node result = (Node) nodeMap.get(facet);
		if (result == null) {
			String label = redrawer == null ? facet.toString() : facet
					.toString(redrawer);
			if (observedDistForNormalization != null)
				label = formatWeight(observedDistForNormalization.R(
						getDistribution(), facet))
						+ " " + label;
			// Prefix with space so edge line doesn't merge with any minus sign
			result = graph.addNode(facet, " " + label);
			nodeMap.put(facet, result);
		}
		assert result != null : facet;
		return result;
	}

	private void addEdge(Distribution observedDistForNormalization,
			Graph graph, Map nodeMap, Perspective cause, Perspective caused,
			Explanation nullModel, PerspectiveObserver redrawer) {
		// Util.print("addRule " + negLiteral + " " + posLiteral);
		Node posNode = ensureNode(observedDistForNormalization, graph, nodeMap,
				caused, redrawer);
		// if (cause != null) {
		Node negNode = ensureNode(observedDistForNormalization, graph, nodeMap,
				cause, redrawer);
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
		edge.setLabel("        " + label + "        ", posNode);
		edge
				.setLabel(formatWeight(getWeight(cause, caused)),
						Edge.CENTER_LABEL);
		// }
	}

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
				if (caused != cause) {
					// assume symmetric and canonicalize so cache lookup will
					// always work
					Perspective smaller = cause;
					Perspective larger = caused;
					if (smaller.compareTo(larger) > 0) {
						smaller = caused;
						larger = cause;
					}
					List edge = new ArrayList(2);
					edge.add(smaller);
					edge.add(larger);
					result.add(edge);
				}
			}
		}
		return result;
	}

	Set getEdges() {
		return getEdgesAmong(facets);
	}

	private Set getEdgesAmong(List prevfacets) {
		// Util.print("addEdges " + causes+" "+causeds);
		HashSet result = new HashSet();
		for (Iterator it = getEdgeIterator(); it.hasNext();) {
			int[] edge = (int[]) it.next();
			Perspective cause = getFacet(edge[0]);
			Perspective caused = getFacet(edge[1]);
			assert caused != cause;
			if (prevfacets.contains(cause) && prevfacets.contains(caused)) {
				List edgeList = new ArrayList(2);
				edgeList.add(cause);
				edgeList.add(caused);
				result.add(edgeList);
			}
		}
		// Util.print("getEdges " + result);
		return result;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(Util.shortClassName(this)).append(" ").append(
				facets);
		if (true) {
			buf.append(" nEdges=").append(nEdges());
		} else {
			buf.append(getEdges());
		}
		buf.append(" ").append(Util.valueOfDeep(getCounts())).append(">");
		return buf.toString();
	}

	public int nEdges() {
		int result = 0;
		for (int cause = 0; cause < nFacets(); cause++) {
			for (int caused = 0; caused < nFacets(); caused++) {
				if (cause != caused && hasEdge(cause, caused)
						&& (!isSymmetric || cause < caused))
					result++;
			}
		}
		return result;
	}

}
