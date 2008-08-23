package edu.cmu.cs.bungee.client.query.tetrad;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import JSci.maths.statistics.ChiSq2x2;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags.TagRelevance;
import edu.cmu.cs.bungee.client.query.tetrad.MyLinearRegressionResult;
import edu.cmu.cs.bungee.client.query.tetrad.MyLogisticRegressionResult;
import edu.cmu.cs.bungee.client.query.tetrad.MyRegressionParams;
import edu.cmu.cs.bungee.client.query.tetrad.TetradRegressionResult;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;
import edu.cmu.cs.bungee.javaExtensions.graph.Node;
import edu.cmu.tetrad.data.ColtDataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.data.Variable;
import edu.cmu.tetrad.search.IndTestChiSquare;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.PcSearch;
import edu.cmu.tetrad.util.TetradLogger;

public class Tetrad {
	static double ALPHA = 0.0;
	private static final boolean IS_LOGISTIC = true;

	/**
	 * Maximum number of candidate related Perspectives to pass to Tetrad
	 * (though popupFacet and all Query restrictions will always be included).
	 * Minimum is 1, or TopTags will barf.
	 */
	private static int MAX_FACETS = 6;

	/**
	 * Prune edges with a beta weight less than this, after graph has been
	 * computed.
	 */
	private static double MIN_CORE_EDGE_BETA = 0.05;
	private final Perspective popupFacet;
	// private Graph graph;

	private final int[] counts;

	/**
	 * The variable names in the discrete data sets for which conditional
	 * independence judgements are desired. Maps from String to Perspective
	 * 
	 */
	private final Map variables = new LinkedHashMap();
	private final Map variablesInverse = new HashMap();
	private static HashMap betaWeights = new HashMap();

	private final TetradDrawer redrawer;

	public interface TetradDrawer {

		public void drawTetradGraph(Graph graph, String status);
	}

	public static Graph getTetradGraph(Perspective facet, TetradDrawer redrawer) {
		return new Tetrad(facet, redrawer).computeGraph();
	}

	private Tetrad(Perspective facet, TetradDrawer redrawer) {
		// TetradLogger.getInstance().setLogging(true);
		// TetradLogger.getInstance().addOutputStream(System.out);
		// TetradLogger.getInstance().setForceLog(true);
		popupFacet = facet;
		this.redrawer = redrawer;

		// Add this separately, because it has to come first (to be bit 0)
		addPerspective(popupFacet);
		for (Iterator it = primaryFacets().iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			addPerspective(p);
		}
		TopTags topTags = query().topTags(2 * MAX_FACETS);
		for (Iterator it = topTags.topIterator(); it.hasNext()
				&& nFacets() < MAX_FACETS;) {
			TagRelevance tag = (TagRelevance) it.next();
			Perspective p = (Perspective) ((ChiSq2x2) tag.tag).object;
			addPerspective(p);
		}
		removeAncestorFacets();
		counts = getCounts();
		// double[] alphas = { 0, 0.001, 1 };
		// for (MAX_FACETS = 2; MAX_FACETS < 11; MAX_FACETS++) {
		// for (int i = 0; i < alphas.length; i++) {
		// ALPHA = alphas[i];
	}

	private Graph computeGraph() {
		Graph initialGraph = getInitialGraph();
		initialGraph.label=getlabel();
		printMe("Initial", initialGraph);
		Util.print("MAX_FACETS=" + MAX_FACETS + " ALPHA=" + ALPHA + " #nodes="
				+ initialGraph.getNumNodes() + " #edges="
				+ initialGraph.getNumDirectedEdges() + " score="
		// + ((int) (1000 * eval(initialGraph) + 0.5))
				);
		// }
		// }
		Graph result = new Graph();
		result.label=initialGraph.label;
		Perspective[] primary = (Perspective[]) primaryFacets().toArray(
				new Perspective[0]);
		for (int i = 0; i < primary.length; i++) {
			for (int j = i + 1; j < primary.length; j++) {
				Graph pairGraph = new Graph(initialGraph);
				for (int k = 0; k < primary.length; k++) {
					if (k != i && k != j) {
						pairGraph.removeNode(tetradName(primary[k]));
					}
					// Util.print(pairGraph);
					for (Iterator it = pairGraph.getEdges().iterator(); it
							.hasNext();) {
						Edge edge = (Edge) it.next();
						edge.setBidirectional();
					}
					removeSatelliteNodes(pairGraph);
				}
				printMe("graphForPair " + primary[i] + " " + primary[j],
						pairGraph);
				graphForPair(result, pairGraph);
			}
		}
		label(result);
		printMe("Final", result);
		return result;
	}

	private void graphForPair(Graph result, Graph pairGraph) {
		for (Iterator it = new Util.CombinationIterator(pairGraph.getEdges()); it
				.hasNext();) {
			Collection edgesToRemove = (Collection) it.next();
			Graph subgraph = new Graph(pairGraph);
			for (Iterator edgeIt = edgesToRemove.iterator(); edgeIt.hasNext();) {
				Edge edge = (Edge) edgeIt.next();
				subgraph.removeEdge(edge);
			}
			if (removeSatelliteNodes(subgraph) == 0) {
				for (Iterator it2 = new Util.CombinationIterator(subgraph
						.getEdges()); it2.hasNext();) {
					Collection forwardEdges = (Collection) it2.next();
					for (Iterator edgeIt = subgraph.getEdges().iterator(); edgeIt
							.hasNext();) {
						Edge edge = (Edge) edgeIt.next();
						int direction = forwardEdges.contains(edge) ? 0 : 1;
						edge
								.setDirection((Node) edge.getNodes().get(
										direction));
					}
					printMe("directed", subgraph);
					if (allOnPath(subgraph) && allStrong(subgraph)) {
						result.union(subgraph);
					}
				}
			}
		}
	}

	private boolean allStrong(Graph subgraph) {
		for (Iterator it = subgraph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node caused = edge.getCausedNode();
			if (getBetaWeight(subgraph, edge.getCausingNode(), caused) < MIN_CORE_EDGE_BETA)
				return false;
		}
		return true;
	}

	private boolean allOnPath(Graph subgraph) {
		Set onPath = new HashSet();
		for (Iterator it = primaryNodes(subgraph).iterator(); it.hasNext();) {
			Node primary = (Node) it.next();
			markUpstream(subgraph, primary, onPath);
		}
		return onPath.size() == subgraph.getNumDirectedEdges();
	}

	private void markUpstream(Graph graph, Node node, Set onPath) {
		for (Iterator it = graph.getUpstreamEdges(node).iterator(); it
				.hasNext();) {
			Edge edge = (Edge) it.next();
			assert edge.isArrowhead(node) : node + "\n" + graph;
			if (!onPath.contains(edge)) {
				onPath.add(edge);
				markUpstream(graph, edge.getDistalNode(node), onPath);
			}
		}
	}

	/**
	 * Remove nodes that do not lie on a path between two core nodes. Paths
	 * cannot contain colliders (even if there are intervening edges). As
	 * implemented, doesn't always reject collider paths (when there are
	 * branches.)
	 */
	private int removeSatelliteNodes(Graph graph1) {
		Collection primaryNodes = primaryNodes(graph1);
		Collection nonSatelliteNodes = new HashSet();
		nonSatelliteNodes.addAll(primaryNodes);
		for (Iterator it = primaryNodes.iterator(); it.hasNext();) {
			Node origin = (Node) it.next();
			if (graph1.hasNode(origin)) {
				Collection nodesOnPath = new HashSet();
				removeSatelliteNodesInternal(graph1, nonSatelliteNodes,
						nodesOnPath, origin, origin, false);
				// Util.print("RSN " + origin + " " + nonSatelliteNodes + "\n");
			}
		}
		int result = 0;
		for (Iterator it = (new ArrayList(graph1.getNodes())).iterator(); it
				.hasNext();) {
			Node node = (Node) it.next();
			if (!nonSatelliteNodes.contains(node)) {
				// Util.print("Removing satellite node " + node);
				graph1.removeNode(node);
				result++;
			}
		}
		return result;
	}

	private void removeSatelliteNodesInternal(Graph graph1,
			Collection nonSatelliteNodes, Collection nodesOnPath, Node origin,
			Node node, boolean seenArrow) {
		for (Iterator it = graph1.getAdjacentNodes(node).iterator(); it
				.hasNext();) {
			Node adj = (Node) it.next();
			// Util.print("RSNI " + node + " " + adj + " " + seenArrow);
			if (!nodesOnPath.contains(adj) && adj != origin
					&& (!seenArrow || graph1.getEdge(node, adj).canCause(adj))) {
				if (!seenArrow && !graph1.getEdge(node, adj).canCause(node))
					seenArrow = true;
				if (nonSatelliteNodes.contains(adj)) {
					nonSatelliteNodes.addAll(nodesOnPath);
					nodesOnPath.clear();
				} else {
					nodesOnPath.add(adj);
					removeSatelliteNodesInternal(graph1, nonSatelliteNodes,
							nodesOnPath, origin, adj, seenArrow);
					nodesOnPath.remove(adj);
				}
			}
		}
	}

	// private double eval(Graph graph2) {
	// double nEdges = 0;
	// double sumR = 0;
	// for (Iterator it = graph2.getNodes().iterator(); it.hasNext();) {
	// Node causedNode = (Node) it.next();
	// Perspective caused = lookupFacet(causedNode);
	// List causes = getCauses(graph2, caused);
	// nEdges += causes.size();
	// for (Iterator causeIt = causes.iterator(); causeIt.hasNext();) {
	// Perspective cause = (Perspective) causeIt.next();
	// sumR += getBetaWeight(graph2, cause, caused);
	// // sumR += Math.abs(getBetaWeights(cause, causes,
	// // caused, IS_LOGISTIC).beta);
	// }
	// }
	// return sumR / nEdges;
	// }

	private Query query() {
		return popupFacet.query();
	}

	private List facetsOfInterest;

	private List facetsOfInterest() {
		if (facetsOfInterest == null) {
			facetsOfInterest = Collections.unmodifiableList(new ArrayList(
					variables.values()));
		}
		return facetsOfInterest;
	}

	private int facetIndex(Perspective facet) {
		return facetsOfInterest().indexOf(facet);
	}

	private Collection primaryFacets;

	private Collection primaryFacets() {
		if (primaryFacets == null) {
			primaryFacets = query().allRestrictions();
			primaryFacets.add(popupFacet);
		}
		return primaryFacets;
	}

	private int nFacets() {
		return variables.size();
	}

	private void addPerspective(Perspective p) {
		String name = p.getNameIfPossible();
		if (name == null)
			name = "p" + p.getID();
		else
			name = name.replaceAll("\n\r\\/:*?\"<>| ", "_");

		variables.put(name, p);
		variablesInverse.put(p, name);
	}

	private Perspective lookupFacet(Node node) {
		return (Perspective) variables.get(node.getLabel());
	}

	private Collection lookupFacets(Collection nodes) {
		Collection result = new LinkedList();
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			result.add(lookupFacet(node));
		}
		return result;
	}

	private String tetradName(Perspective p) {
		return (String) variablesInverse.get(p);
	}

	private Collection primaryNodes(Graph graph1) {
		Collection primaryFacets1 = primaryFacets();
		Collection primaryNodes = new ArrayList(primaryFacets1.size());
		for (Iterator it = primaryFacets1.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			Node node = graph1.getNode(tetradName(p));
			if (node != null)
				primaryNodes.add(node);
		}
		return primaryNodes;
	}

	private void removeAncestorFacets() {
		Collection facets = new ArrayList(variables.values());
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			Collection ancestors = facet.ancestors();
			ancestors.remove(popupFacet);
			for (Iterator ancIt = ancestors.iterator(); ancIt.hasNext();) {
				Perspective ancestor = (Perspective) ancIt.next();
				if (primaryFacets().contains(facet)
						|| !primaryFacets().contains(ancestor)) {
					String ancestorName = (String) variablesInverse
							.get(ancestor);
					if (ancestorName != null) {
						variables.remove(ancestorName);
						variablesInverse.remove(ancestor);
						primaryFacets.remove(ancestor);
					}
				}
			}
		}
	}

	private Graph getInitialGraph() {
		IndependenceTest independenceTest = new IndTestChiSquare(getData(),
				ALPHA);
		// Util.print("aa "+getData().getNumRows());
		Knowledge knowledge = new Knowledge();
		List facets = facetsOfInterest();
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective caused = (Perspective) it.next();
			int tier = 0;// caused.isCausable() ? 1 : 0;
			knowledge.addToTier(tier, tetradName(caused));
			// if (false && tier == 1) {
			// for (Iterator it2 = facets.subList(facets.indexOf(caused) + 1,
			// facets.size()).iterator(); it2.hasNext();) {
			// Perspective cause = (Perspective) it2.next();
			// if (!canCause(cause, caused)) {
			// knowledge.setEdgeForbidden(tetradName(cause),
			// tetradName(caused), true);
			// knowledge.setEdgeForbidden(tetradName(caused),
			// tetradName(cause), true);
			// }
			// }
			// }
		}
		// knowledge.setTierForbiddenWithin(0, true);
		PcSearch pcSearch = new PcSearch(independenceTest, knowledge);
		// pcSearch.setDepth(5);
		edu.cmu.tetrad.graph.Graph tetradGraph = pcSearch.search();
		// Util.print(tetradGraph);
		Graph graph1 = getGraph(tetradGraph);
		// Util.print("graph: " + graph1);


		return graph1;
	}

	private Graph getGraph(edu.cmu.tetrad.graph.Graph tetradGraph) {
		Graph result = new Graph();
		Map nodeMap = new HashMap();
		for (Iterator it = tetradGraph.getNodes().iterator(); it.hasNext();) {
			edu.cmu.tetrad.graph.Node node = (edu.cmu.tetrad.graph.Node) it
					.next();
			Node resultNode = result.addNode(node.getName());
			resultNode.object = lookupFacet(resultNode);
			nodeMap.put(node, resultNode);
		}
		for (Iterator it = tetradGraph.getEdges().iterator(); it.hasNext();) {
			edu.cmu.tetrad.graph.Edge edge = (edu.cmu.tetrad.graph.Edge) it
					.next();
			Node node1 = (Node) nodeMap.get(edge.getNode1());
			Node node2 = (Node) nodeMap.get(edge.getNode2());
			result.addEdge(null, node1, node2);
		}
		return result;
	}

	private class BetaCache {
		final double beta;
		final double relativeBeta;
		final String label;

		BetaCache(double beta, double relativeBeta, String label) {
			this.beta = beta;
			this.relativeBeta = relativeBeta;
			this.label = label;
		}
	}

	private BetaCache getBetaWeights(Perspective cause, Collection otherCauses,
			Perspective caused, boolean isLogistic) {
		// Util.print("getBetaWeights "+cause+" => "+caused);
		List sortedCauses = new ArrayList(otherCauses.size() + 1); // all causes
		// (cause +
		// otherCauses
		// ) in
		// canonical
		// order
		sortedCauses.addAll(otherCauses);
		if (!otherCauses.contains(cause))
			sortedCauses.add(cause);
		Collections.sort(sortedCauses);
		assert !sortedCauses.contains(caused) : caused + " " + sortedCauses;

		List args = new ArrayList(sortedCauses.size() + 1); // sortedCauses
		// followed by
		// caused
		args.addAll(sortedCauses);
		// Collections.sort(args);
		// List sortedCauses = new ArrayList(args); // causes in canonical order
		args.add(caused);
		BetaCache[] cached = (BetaCache[]) betaWeights.get(args);
		if (cached == null) {
			cached = new BetaCache[sortedCauses.size()];
			MyRegressionParams params = getRegressionParams(sortedCauses,
					caused);
			assert params.nRows() == nRows() : Util.valueOfDeep(params.counts);
			try {
				TetradRegressionResult regression = isLogistic ? MyLogisticRegressionResult
						.getInstance(params)
						: MyLinearRegressionResult.getInstance(params);
				double[] coefs = regression.getCoefs();
				double sumCoefs = 0;
				for (int i = 0; i < params.regressorNames.length; i++) {
					assert params.regressorNames[i]
							.equals(tetradName((Perspective) args.get(i))) : Util
							.valueOfDeep(params.regressorNames)
							+ " " + sortedCauses + " " + args;
					sumCoefs += Math.abs(coefs[i + 1]);
				}

				double rSquared = regression.pseudoRsquared();

				// Scaling is confusing, because "strength" between two facets
				// will
				// change if you add an unrelated predictor, as the maxRsquared
				// may
				// now
				// be much higher. Thus prune based on scaled beta, but label
				// with unscaled beta.
				//
				// double maxRsquared = regression.maxRsquared();
				// return formatWeight(coef) + " * " +
				// formatWeight(Math.sqrt(rSquared))
				// + " / " + formatWeight(Math.sqrt(maxRsquared));

				double r = Math.sqrt(rSquared);
				double maxR = 1;
				// // try {
				// maxR = Math.sqrt(regression.maxRsquared());
				// // } catch (AssertionError e) {
				// // Util.print("Ignoring AssertionError getting maxR for "
				// // + cause + " => " + caused + " " + otherCauses);
				// // }

				for (int i = 0; i < cached.length; i++) {
					double coef = coefs[i + 1] / sumCoefs;
					double beta = r * coef;
					String label = formatWeight(coef) + " * " + formatWeight(r)
					/* + " / " + formatWeight(maxR) */;
					cached[i] = new BetaCache(beta, beta / maxR, label);
				}

				betaWeights.put(args, cached);
				// Util.print("beta     "
				// + Util.valueOfDeep(params.regressorNames) + "-->"
				// + tetradName(caused) + " " + r + " = "
				// + Util.valueOfDeep(cached));
			} catch (AssertionError e) {
				Util.print("Ignoring AssertionError in getBetaWeights for "
						+ cause + " => " + caused + otherCauses);
				// e.printStackTrace();
				BetaCache foo = new BetaCache(0, 0, "Bogus");
				for (int i = 0; i < cached.length; i++) {
					cached[i] = foo;
				}
				throw (e);
			}
		}
		int index = args.indexOf(cause);
		return cached[index];
	}

	private double getBetaWeight(Graph graph, Node cause, Node caused) {
		Collection otherCauses = graph.getCauses(caused);
		return getBetaWeights(lookupFacet(cause), lookupFacets(otherCauses),
				lookupFacet(caused), IS_LOGISTIC).beta;
	}

	private String getBetaLabel(Graph graph, Node cause, Node caused) {
		Collection otherCauses = graph.getCauses(caused);
		return getBetaWeights(lookupFacet(cause), lookupFacets(otherCauses),
				lookupFacet(caused), IS_LOGISTIC).label;
	}

	private void label(Graph graph) {
		for (Iterator it = graph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			edge.setLabel(getBetaLabel(graph, edge));
		}
	}

	private String getBetaLabel(Graph graph, Edge edge) {
		StringBuffer buf = new StringBuffer();
		for (Iterator it = edge.getNodes().iterator(); it.hasNext();) {
			Node caused = (Node) it.next();
			if (edge.canCause(caused)) {
				if (buf.length() > 0) {
					buf.append(" / ");
				}
				buf
						.append(
								getBetaLabel(graph, edge.getDistalNode(caused),
										caused)).append(
								caused.getLabel().charAt(0));
			}
		}
		return buf.toString();
	}

	private String formatWeight(double weight) {
		if (Double.isNaN(weight))
			return "?";
		return Integer.toString((int) Math.rint(100 * weight));
	}

	private MyRegressionParams getRegressionParams(Collection causes,
			Perspective caused) {
		int[] xIndexes = new int[causes.size()];
		int yIndex = facetIndex(caused);
		String[] regressorNames = new String[causes.size()];
		int xIndexesIndex = 0;
		for (Iterator it = causes.iterator(); it.hasNext();) {
			Perspective cause = (Perspective) it.next();
			regressorNames[xIndexesIndex] = tetradName(cause);
			xIndexes[xIndexesIndex] = facetIndex(cause);
			xIndexesIndex++;
		}
		int nVars = regressorNames.length;
		assert nVars > 0 : caused;

		int nSubstates = 2 << nVars;
		int[] substateCounts = new int[nSubstates];
		for (int state = 0; state < counts.length; state++) {
			int substate = 0;
			for (int j = 0; j < nVars; j++) {
				substate = Util.setBit(substate, j + 1, Util.isBit(state,
						xIndexes[j]));
			}
			substate = Util.setBit(substate, 0, Util.isBit(state, yIndex));
			substateCounts[substate] += counts[state];
		}
		return new MyRegressionParams(substateCounts, regressorNames,
				tetradName(caused));
	}

	private int[] getCounts() {
		// if (counts == null) {
		int[] counts1 = new int[1 << nFacets()];
		ResultSet rs = query().onCountMatrix(facetsOfInterest());
		try {
			while (rs.next()) {
				counts1[rs.getInt(1)] = rs.getInt(2);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		Util.print("primaryFacets (" + primaryFacets().size() + ") \n "
				+ Util.join(primaryFacets(), "\n "));
		List others = new LinkedList(facetsOfInterest());
		others.removeAll(primaryFacets());
		Util.print("facetsOfInterest (" + others.size() + ") \n "
				+ Util.join(others, "\n "));
		// Util.print("counts: " + Util.valueOfDeep(counts));

		// The low order bit represents the first facet (popupFacet)
		assert getCountInUniverse(counts1, 0) == popupFacet.getTotalCount() : popupFacet
				+ " "
				+ popupFacet.getTotalCount()
				+ " "
				+ getCountInUniverse(counts1, 0);
		// }
		return counts1;
	}

	private int getCountInUniverse(int[] counts1, int varIndex) {
		assert varIndex >= 0 && varIndex < nFacets();
		int total = 0;
		for (int j = 0; j < counts1.length; j++) {
			if (Util.isBit(j, varIndex))
				total += counts1[j];
		}
		return total;
	}

	private int nRows() {
		return query().getTotalCount();
	}

	private ColtDataSet getData() {
		boolean USE_MULTIPLIERS = true;
		List categories = new ArrayList(2);
		categories.add("0");
		categories.add("1");
		int nCols = nFacets();
		List vars = new ArrayList(nCols);
		for (Iterator it = facetsOfInterest().iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			String name = tetradName(facet);
			Variable variable = new DiscreteVariable(name, categories);
			((DiscreteVariable) variable).setAccommodateNewCategories(false);
			vars.add(variable);
		}
		ColtDataSet data1 = new ColtDataSet(USE_MULTIPLIERS ? nNonZeroStates()
				: nRows(), vars);
		int row = 0;
		for (int condition = 0; condition < counts.length; condition++) {
			// double[] sum = new double[nCols];
			for (int item = 0; item < counts[condition]; item++) {
				for (int col = 0; col < nCols; col++) {
					int colValue = Util.getBit(condition, col);
					// multiplier may work for chi square independence test
					data1.setInt(row, col, colValue);
					// sum[col] += data1.getDouble(row, col);
				}
				row++;
				if (USE_MULTIPLIERS) {
					data1.setMultiplier(row - 1, counts[condition]);
					break;
				}
			}
			// Util.print("sums: " + condition + " " + counts[condition]);
		}
		// Util.print("getData " + row + " " + counts.length + " "
		// + data1.getNumRows());
		return data1;

	}

	private int nNonZeroStates() {
		int result = 0;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0)
				result++;
		}
		return result;
	}


	// /**
	// *
	// * There are 2**(nFacets - 2) subsets of the other variables, and for
	// every
	// * value assignment to every such subsets, we want to compute the odds
	// ratio
	// * for caused given cause versus caused given not cause.
	// *
	// * The set bits in the binary representation of conditioningVars
	// determines
	// * the subset.
	// *
	// * The subset is make explicit in substateIndexes (and its complement in
	// * marginIndexes).
	// *
	// * substate ranges over the possible value assignments for each subset.
	// *
	// * The odds ratio is computed from the marginal counts for each of the
	// four
	// * assignments to cause and caused, superimposed on the substate.
	// *
	// * @param cause
	// * conditioning variable for odds ratio test
	// * @param caused
	// * the variable to compute the odds ratio for
	// * @return whether Pr(p2|p) is nearly equal to Pr(p2) for every state of
	// * every other variable.
	// */
	// private boolean canCause(Perspective cause, Perspective caused) {
	// Perspective[] facets = (Perspective[]) facetsOfInterest().toArray(
	// new Perspective[nFacets()]);
	// int causeIndex = Util.member(facets, cause);
	// int causedIndex = Util.member(facets, caused);
	// boolean causes = false;
	// double bestScore = 0;
	//
	// int nConditioningVars = nFacets() - 2;
	// // int bestState = 0;
	// int nSubsets = 1 << nConditioningVars;
	// for (int conditioningVars = 0; conditioningVars < nSubsets && !causes;
	// conditioningVars++) {
	// int[] marginIndexes = new int[0];
	// int[] substateIndexes = new int[0];
	// for (int var = 0; var < nConditioningVars; var++) {
	// int varIndex = var;
	// if (varIndex >= Math.min(causeIndex, causedIndex))
	// varIndex++;
	// if (varIndex >= Math.max(causeIndex, causedIndex))
	// varIndex++;
	// if (Util.isBit(conditioningVars, var)) {
	// marginIndexes = Util.push(marginIndexes, varIndex);
	// } else {
	// substateIndexes = Util.push(substateIndexes, varIndex);
	// }
	// }
	// // Util.print("cc " + causeIndex + " " + causedIndex + " "
	// // + conditioningVars + " "
	// // + Util.valueOfDeep(substateIndexes));
	// int nSubstateVars = nConditioningVars - marginIndexes.length;
	// int nSubstates = 1 << nSubstateVars;
	// for (int substate = 0; substate < nSubstates && !causes; substate++) {
	// int state = setSubstate(0, substate, substateIndexes);
	// double a = marginCounts(Util.setBit(Util.setBit(state,
	// causeIndex, true), causedIndex, true), marginIndexes);
	// double b = marginCounts(Util.setBit(state, causeIndex, true),
	// marginIndexes);
	// double c = marginCounts(Util.setBit(state, causedIndex, true),
	// marginIndexes);
	// double d = marginCounts(state, marginIndexes);
	// if (a + b > 10 && c + d > 10 && a + c > 10 && b + d > 10) {
	// // double oddsRatio = a * d / b / c;
	// double total = a + b + c + d;
	// double row0 = a + b;
	// double col0 = a + c;
	// ChiSq2x2 chisq = ChiSq2x2.getInstance(null, (int) total,
	// (int) row0, (int) col0, (int) a);
	// // double score = chisq.correlationPercent();
	// double score = Math.abs(chisq.correlation());
	//
	// // Util.print("odds (" + state + ") "
	// // + Util.valueOfDeep(marginIndexes) + " p="
	// // + (a / col0) + "; q=" + (a / row0) + "; odds="
	// // + oddsRatio + "\n correlation "
	// // + chisq.correlation() + " " + correlationRatio
	// // + "\npredicted a = " + (row0 * col0 / total) + " "
	// // + a + " " + b + " " + c + " " + d);
	//
	// // ChiSq2x2 maxCorrelationChisq = ChiSq2x2.getInstance(null,
	// // (int) (a + b + c + d), (int) (a + b),
	// // (int) (a + c), (int) Math.min(a + b, a + c));
	// // Util.print(" max correlation "
	// // + maxCorrelationChisq.correlation());
	// // Util.print(" entropies: " + chisq.entropy(ChiSq2x2.ROW)
	// // + " " + chisq.entropy(ChiSq2x2.COL) + " "
	// // + chisq.entropy() + " mutInf: " + chisq.mutInf()
	// // + " max mut inf  " + maxCorrelationChisq.mutInf());
	// if (score > bestScore) {
	// bestScore = score;
	// // bestState = state;
	// // if (absOdds > 2)
	// // causes = true;
	// }
	// }
	// }
	// }
	// int relevance = (primaryFacets().contains(cause) ? 1 : 0)
	// + (primaryFacets().contains(caused) ? 1 : 0);
	// double[] thresholds = { MIN_NONCORE_EDGE_CORRELATION,
	// MIN_CORE_NONCORE_EDGE_CORRELATION, MIN_CORE_EDGE_CORRELATION };
	// double threshold = thresholds[relevance];
	// causes = bestScore > threshold;
	// Util.print("Can Cause " + cause + " => " + caused + " " + +bestScore
	// + " " + causes);
	// return causes;
	// }
	//
	// private int marginCounts(int state, int[] marginIndexes) {
	// int sum = 0;
	// for (int i = 0; i < 1 << marginIndexes.length; i++) {
	// for (int j = 0; j < marginIndexes.length; j++) {
	// state = Util.setBit(state, marginIndexes[j], Util.isBit(i, j));
	// // Util.print("hh "+i+" "+j+" "+state+" "+Util.isBit(i, j));
	// }
	// sum += counts[state];
	// }
	// // Util.print("marginCounts " + state + " "
	// // + Util.valueOfDeep(marginIndexes) + " " + sum);
	// return sum;
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
	// private double getChainedBeta(Graph graph1, Perspective cause,
	// Perspective caused, Collection previousNodes) {
	// Edge edge = getEdge(graph1, cause, caused);
	// Node causedNode = graph1.getNode(tetradName(caused));
	// Perspective arrowTail = cause;
	// Perspective arrowHead = caused;
	// if (!edge.canCause(causedNode)) {
	// arrowTail = caused;
	// arrowHead = cause;
	// }
	// Node causeNode = edge.getDistalNode(causedNode);
	// assert !previousNodes.contains(causeNode);
	// assert !previousNodes.contains(causedNode);
	// double result = Math.abs(getBetaWeights(arrowTail, getCauses(graph1,
	// arrowHead), arrowHead, IS_LOGISTIC).relativeBeta);
	// if (!primaryFacets().contains(cause)) {
	// double subBeta = 0;
	// Collection subPrev = new ArrayList(previousNodes.size() + 1);
	// subPrev.addAll(previousNodes);
	// subPrev.add(causedNode);
	// Collection subCauses = getAdjacentFacets(graph1, cause);
	// for (Iterator it = subCauses.iterator(); it.hasNext();) {
	// Perspective subCause = (Perspective) it.next();
	// Node subCauseNode = graph1.getNode(tetradName(subCause));
	// if (!subPrev.contains(subCauseNode)) {
	// subBeta += getChainedBeta(graph1, subCause, cause, subPrev);
	// }
	// }
	// result *= subBeta;
	// }
	// // Util.print(" getChainedBeta " + causeNode + " => " + causedNode + " "
	// // + previousNodes + " " + result);
	// assert result >= 0 && result <= 1 : result;
	// return result;
	// }
	//
	// private Edge getEdge(Graph graph1, Perspective cause, Perspective caused)
	// {
	// Node causeNode = graph1.getNode(tetradName(cause));
	// Node causedNode = graph1.getNode(tetradName(caused));
	// return graph1.getEdge(causeNode, causedNode);
	// }

	// private Collection getAdjacentCoreNodes(Graph graph1, Node node) {
	// Collection primaryFacets1 = primaryFacets();
	// Collection result = graph1.getAdjacentNodes(node);
	// for (Iterator it2 = result.iterator(); it2.hasNext();) {
	// Node n = (Node) it2.next();
	// if (!primaryFacets1.contains(lookupFacet(n))) {
	// it2.remove();
	// }
	// }
	// return result;
	// }
	//
	// private void removeUninterestingNodes(Graph graph1) {
	// removeNonadjacentNodes(graph1);
	// removeSatelliteNodes(graph1);
	// }
	//
	// /**
	// * Remove non-primary nodes with no outgoing edges
	// */
	// private void removeNonadjacentNodes(Graph graph1) {
	// Collection primaryFacets1 = primaryFacets();
	// for (Iterator it = graph1.getNodes().iterator(); it.hasNext();) {
	// Node node = (Node) it.next();
	// if (!primaryFacets1.contains(lookupFacet(node))) {
	// boolean isAdjacent = false;
	// for (Iterator adjIt = graph1.getEdges(node).iterator(); adjIt
	// .hasNext()
	// && !isAdjacent;) {
	// Edge edge = (Edge) adjIt.next();
	// isAdjacent = edge.canCause(edge.getDistalNode(node));
	// }
	// if (!isAdjacent) {
	// graph1.removeNode(node);
	// // Util.print("Removing nonadjacent node " + node);
	// }
	// }
	// }
	// }

//	private String label;

	private String getlabel() {
//		if (label == null) {
			StringBuffer base = new StringBuffer();
			for (Iterator it = primaryFacets().iterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (base.length() > 0)
					base.append(" ");
				base.append(tetradName(p));
			}
			base.append(" ").append(
					formatWeight(MIN_CORE_EDGE_BETA))/*
													 * .append("_").append(
													 * formatWeight
													 * (MIN_CORE_NONCORE_EDGE_BETA
													 * )).append("_")
													 * .append(formatWeight
													 * (MIN_NONCORE_EDGE_BETA))
													 */.append("_").append(
					MAX_FACETS).append(" ");
			return base.toString();
//		}
//		return label;
	}

	static final char CONTROL_Q = 17;
	// static final char CONTROL_W = 23;
	// static final char CONTROL_E = 5;
	static final char CONTROL_R = 18;

	public static boolean handleKey(char c, int modifiers) {
		boolean result = true;
		boolean shift = Util.isShiftDown(modifiers);
		double factor = shift ? 1.2 : 1 / 1.2;
		switch (c) {
		case CONTROL_Q:
			MIN_CORE_EDGE_BETA *= factor;
			break;
		// case CONTROL_W:
		// MIN_CORE_NONCORE_EDGE_BETA *= factor;
		// break;
		// case CONTROL_E:
		// MIN_NONCORE_EDGE_BETA *= factor;
		// break;
		case CONTROL_R:
			MAX_FACETS += shift ? 1 : -1;
			break;

		default:
			result = false;
		}
		return result;
	}

	private void printMe(String status, Graph graph) {
		if (redrawer != null)
			redrawer.drawTetradGraph(graph, status);
	}

}

//class PerspectiveParentPrinter {
//	private final Perspective facet;
//
//	private static final StringAlign align = new StringAlign(13,
//			StringAlign.JUST_LEFT);
//
//	PerspectiveParentPrinter(Perspective p) {
//		facet = p;
//	}
//
//	public String toString() {
//		String result = align.format(facet);
//		if (facet.getParent() != null) {
//			result += " => " + facet.getParent();
//		}
//		return result;
//	}
//
//}
