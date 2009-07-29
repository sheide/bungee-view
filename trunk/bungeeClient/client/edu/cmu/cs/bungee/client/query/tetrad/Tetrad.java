package edu.cmu.cs.bungee.client.query.tetrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.tetrad.MyLinearRegressionResult;
import edu.cmu.cs.bungee.client.query.tetrad.MyLogisticRegressionResult;
import edu.cmu.cs.bungee.client.query.tetrad.MyRegressionParams;
import edu.cmu.cs.bungee.client.query.tetrad.TetradRegressionResult;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph;
import edu.cmu.cs.bungee.javaExtensions.graph.Node;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph.GraphWeigher;
import edu.cmu.tetrad.data.ColtDataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.data.Variable;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.search.IndTestChiSquare;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.PcSearch;
import edu.cmu.tetrad.search.PcdSearch;
import edu.cmu.tetrad.util.TetradLogger;

public class Tetrad implements GraphWeigher {
	static double ALPHA = 0.0;
	private static final boolean IS_LOGISTIC = true;

	/**
	 * Maximum number of candidate related Perspectives to pass to Tetrad
	 * (though popupFacet and all Query restrictions will always be included).
	 * Minimum is 1, or TopTags will barf.
	 */
	private static int MAX_FACETS = 0;

	/**
	 * Prune edges with a beta weight less than this, after graph has been
	 * computed.
	 */
	static double MIN_BETA = 0.0;
	// private final Perspective popupFacet;
	// private Graph graph;

	final int[] counts;
	private List facetsOfInterest;
	final Collection primaryFacets;

	/**
	 * The variable names in the discrete data sets for which conditional
	 * independence judgements are desired. Maps from String to Perspective
	 * 
	 */
	private final Map variables = new LinkedHashMap();
	private final Map variablesInverse = new HashMap();
	private final HashMap betaWeights = new HashMap();

	private final PerspectiveObserver redrawer;
	private final TetradPrinter printer;

	public interface TetradPrinter {

		public void drawTetradGraph(Graph graph, String status);
	}

	public static Graph getTetradGraph(Perspective facet,
			TetradPrinter printer, PerspectiveObserver redrawer) {
		Set primary = new HashSet(facet.query().allRestrictions());
		primary.add(facet);
		Set other = new HashSet();

		List candidates = Explanation.candidateFacets(new ArrayList(primary),
				false);
		for (Iterator it = candidates.iterator(); it.hasNext()
				&& other.size() < MAX_FACETS;) {
			Perspective p = (Perspective) it.next();
			other.add(p);
			removeAncestorFacets(primary, other, facet);
		}

		// TopTags topTags = facet.query().topTags(2 * MAX_FACETS);
		// for (Iterator it = topTags.topIterator(); it.hasNext()
		// && other.size() < MAX_FACETS;) {
		// TagRelevance tag = (TagRelevance) it.next();
		// Perspective p = (Perspective) tag.tag.object;
		// other.add(p);
		// removeAncestorFacets(primary, other, facet);
		// }

		return getTetradGraph(facet, primary, other, printer, redrawer);
	}

	public static Graph getTetradGraph(Perspective popupFacet,
			Collection primaryFacets, Collection allFacets, TetradPrinter printer,
			PerspectiveObserver redrawer) {

		Tetrad tetrad = new Tetrad(popupFacet, primaryFacets, allFacets,
				printer, redrawer);
		// printer = null;
		Graph tetradGraph = tetrad.computeGraph();
		if (printer != null) {
			printer.drawTetradGraph(tetradGraph, "tetrad");
		}
		return tetradGraph;
		// return new Koller(tetrad).getKollerGraph(printer);
	}

	Tetrad(Perspective popupFacet, Collection primaryFacets, Collection allFacets,
			TetradPrinter printer, PerspectiveObserver redrawer) {
		boolean logTetrad = true;
		if (logTetrad) {
			TetradLogger.getInstance().setLogging(true);
			TetradLogger.getInstance().addOutputStream(System.out);
			TetradLogger.getInstance().setForceLog(true);
		}

		primaryFacets.add(popupFacet);
		if (!allFacets.containsAll(primaryFacets))
			// Don't barf on unmodifiable collections if it's already OK
			allFacets.addAll(primaryFacets);

		this.primaryFacets = primaryFacets;
		this.redrawer = redrawer;
		this.printer = printer;

		// Add this separately, because it has to come first (to be bit 0)
		addPerspective(popupFacet);
		for (Iterator it = allFacets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			addPerspective(p);
		}
		counts = getCounts();
		// double[] alphas = { 0, 0.001, 1 };
		// for (MAX_FACETS = 2; MAX_FACETS < 11; MAX_FACETS++) {
		// for (int i = 0; i < alphas.length; i++) {
		// ALPHA = alphas[i];
	}

	Graph computeGraph() {
		long start = (new Date()).getTime();
		Graph initialGraph = getInitialGraph();
		initialGraph.label = getlabel();

		// printMe("Initial", initialGraph);
		// Util.print("MAX_FACETS=" + MAX_FACETS + " ALPHA=" + ALPHA
		// + "\ninitial nodes=" + initialGraph.getNumNodes() + " #edges="
		// + initialGraph.getNumDirectedEdges() + " averageR="
		// + ((int) (100 * averageR(initialGraph) + 0.5)));

		Graph result = initialGraph;

		// Graph result = new Graph(this);
		// result.label = initialGraph.label;
		// Node[] primary = primaryNodes(initialGraph);
		// for (int i = 0; i < primary.length; i++) {
		// result.addNode(primary[i]);
		// }
		// for (int i = 0; i < primary.length - 1; i++) {
		// for (int j = i + 1; j < primary.length; j++) {
		// Graph pairGraph = new Graph(initialGraph);
		// for (int k = 0; k < primary.length; k++) {
		// if (k != i && k != j) {
		// pairGraph.removeNode(primary[k]);
		// }
		// }
		// setBidirectional(pairGraph);
		// pairGraph.removeNonpathEdges(primary[i], primary[j]);
		// // Util.print("after pairGraph\n" + pairGraph);
		// printMe("graphForPair " + primary[i].getLabel() + " "
		// + primary[j].getLabel(), pairGraph);
		// graphForPair(result, pairGraph, primary[i], primary[j]);
		// }
		// }
		// result.pruneWeakEdges();

		// label(result);
		printMe("Final", result);
		long duration = (new Date()).getTime() - start;
		Util.print("final nodes=" + result.getNumNodes() + " #edges="
				+ result.getNumDirectedEdges() + " averageR="
				+ ((int) (100 * averageR(result) + 0.5)) + " # regressions="
				+ nnn + " duration=" + duration);
		return result;
	}

	// private void graphForPair(Graph result, Graph pairGraph, Node primary1,
	// Node primary2) {
	// for (Iterator it = new Util.CombinationIterator(pairGraph.getEdges()); it
	// .hasNext();) {
	// Collection edgesToRemove = (Collection) it.next();
	// // Util.print("etr "+edgesToRemove);
	// Graph subgraph = new Graph(pairGraph);
	// for (Iterator edgeIt = edgesToRemove.iterator(); edgeIt.hasNext();) {
	// Edge edge = (Edge) edgeIt.next();
	// subgraph.removeEdge(edge);
	// }
	// setBidirectional(subgraph);
	// // printMe("etr", subgraph);
	// if (subgraph.allOnPath(primary1, primary2, 0)) {
	// for (Iterator it2 = new Util.CombinationIterator(subgraph
	// .getEdges()); it2.hasNext();) {
	// Collection forwardEdges = (Collection) it2.next();
	// for (Iterator edgeIt = subgraph.getEdges().iterator(); edgeIt
	// .hasNext();) {
	// Edge edge = (Edge) edgeIt.next();
	// int direction = forwardEdges.contains(edge) ? 0 : 1;
	// edge
	// .setDirection((Node) edge.getNodes().get(
	// direction));
	// }
	// if (!result.contains(subgraph)
	// && subgraph.allOnPath(primary1, primary2, 0)
	// && subgraph.allOnPath(primary1, primary2, MIN_BETA)) {
	// // The first two conjuncts are just for efficiency
	// printMe("directed", subgraph);
	// result.union(subgraph);
	// printMe("result", result);
	// }
	// }
	// }
	// }
	// }

	// private boolean allStrong(Graph subgraph) {
	// for (Iterator it = subgraph.getEdges().iterator(); it.hasNext();) {
	// Edge edge = (Edge) it.next();
	// Node caused = edge.getCausedNode();
	// if (Math
	// .abs(getBetaWeight(subgraph, edge.getCausingNode(), caused)) <
	// MIN_CORE_EDGE_BETA)
	// return false;
	// }
	// return true;
	// }

	// private boolean allOnPath(Graph subgraph) {
	// Set onPath = new HashSet();
	// for (Iterator it = primaryNodes(subgraph).iterator(); it.hasNext();) {
	// Node primary = (Node) it.next();
	// markUpstream(subgraph, primary, onPath);
	// }
	// boolean result = onPath.size() == subgraph.getNumDirectedEdges();
	// if (result)
	// Util.print("ii "+primaryNodes(subgraph)+" "+onPath+"\n"+subgraph+"\n\n");
	// return result;
	// }
	//
	// private void markUpstream(Graph graph, Node node, Set onPath) {
	// for (Iterator it = graph.getUpstreamEdges(node).iterator(); it
	// .hasNext();) {
	// Edge edge = (Edge) it.next();
	// assert edge.isArrowhead(node) : node + "\n" + graph;
	// if (!onPath.contains(edge)) {
	// onPath.add(edge);
	// markUpstream(graph, edge.getDistalNode(node), onPath);
	// Util.print(node+" is caused by "+edge);
	// }
	// }
	// }

	// /**
	// * downstream means in the direction of the arrow
	// *
	// * @return all edges in graph1 that lie on a colliderless path between two
	// * primary nodes
	// */
	// private Collection pathEdges(Graph graph1, Node primary1, Node primary2,
	// double threshold) {
	// Collection pathEdges = new HashSet();
	// Collection pathNodes = new HashSet();
	// Collection seenNodes = new HashSet();
	// Collection downhillPathNodes = new HashSet();
	// seenNodes.add(primary1);
	// downhillPathNodes.add(primary1);
	// pathEdgesInternal(graph1, downhillPathNodes, primary1,
	// pathNodes, pathEdges, primary2, seenNodes, primary1, false,
	// false, threshold, 1);
	// return pathEdges;
	// }
	//
	// /**
	// * Depth-first sesarch, backtracking if we're in a loop, or meet a
	// collider.
	// * If we reach a goal node, add the current path to pathEdges
	// */
	// /**
	// * @param graph1
	// * Graph with either all directed edges or all undirected edges
	// * @param downhillPathNodes
	// * There is a downhill path from these nodes to goalNode
	// * @param pathNodes
	// * All nodes of pathEdges
	// * @param pathEdges
	// * There is a path from these edges to goalNode
	// * @param goalNode
	// * @param seenNodes
	// * These nodes have already been visited
	// * @param node
	// * current node in the depth-first search
	// * @param downstreamPath
	// * ???
	// * @param downstreamOnly
	// * whether the current search path already includes a downstream
	// * edge
	// * @param threshold
	// * minimum strength of paths whose edges get added to pathEdges
	// * @param strength
	// * cumulative product of edges on the current search path
	// * @return whether there is a path from node to goalNode
	// */
	// private boolean pathEdgesInternal(Graph graph1,
	// Collection downhillPathNodes, Node origin,
	// Collection pathNodes, Collection pathEdges, Node goalNode,
	// Collection seenNodes, Node node, boolean downstreamPath,
	// boolean downstreamOnly, double threshold, double strength) {
	// boolean result = false;
	// for (Iterator it = graph1.getNodeEdgeIterator(node); it.hasNext();) {
	// Edge edge = (Edge) it.next();
	// Node adj = edge.getDistalNode(node);
	// // Util.print("RSNI " + node + " " + adj + " " + seenArrow);
	// boolean isDownstream = edge.canCause(adj);
	// if (!downstreamOnly || isDownstream) {
	// boolean add = goalNode == adj;
	// if (!add && seenNodes.contains(adj)) {
	// add = pathNodes.contains(adj)
	// && (!(downstreamOnly ||isDownstream)|| downhillPathNodes
	// .contains(adj));
	// } else if (!add) {
	// seenNodes.add(adj);
	// double beta = threshold > 0 ? Math
	// .abs(getBetaWeight(graph1, edge.getCausingNode(),
	// edge.getCausedNode())) : 1;
	// double substrength = strength * beta;
	// if (substrength >= threshold) {
	// add = pathEdgesInternal(graph1, downhillPathNodes,
	// origin, pathNodes, pathEdges,
	// goalNode, seenNodes, adj, !isDownstream
	// && (downstreamPath || downhillPathNodes
	// .contains(node)),
	// downstreamOnly || isDownstream, threshold,
	// substrength);
	// }
	// }
	// if (add) {
	// pathEdges.add(edge);
	// if (node != origin) {
	// pathNodes.add(node);
	// if (isDownstream) {
	// downhillPathNodes.add(node);
	// propagateUphill(graph1, seenNodes,
	// downhillPathNodes, node);
	// }
	// }
	// result = true;
	// }
	// }
	// }
	// return result;
	// }
	//
	// private void propagateUphill(Graph graph1, Collection seenNodes,
	// Collection downhillPathNodes, Node node) {
	// for (Iterator it = graph1.getNodeEdgeIterator(node); it.hasNext();) {
	// Edge edge = (Edge) it.next();
	// Node adj = edge.getDistalNode(node);
	// // Util.print("RSNI " + node + " " + adj + " " + seenArrow);
	// boolean isUpstream = edge.canCause(node);
	// if (isUpstream && seenNodes.contains(adj)
	// && !downhillPathNodes.contains(adj)) {
	// downhillPathNodes.add(adj);
	// propagateUphill(graph1, seenNodes, downhillPathNodes, adj);
	// }
	// }
	// }

	private double averageR(Graph graph2) {
		double nEdges = 0;
		double sumR = 0;
		for (Iterator it = graph2.getNodes().iterator(); it.hasNext();) {
			Node caused = (Node) it.next();
			Set causes = graph2.getCauses(caused);
			nEdges += causes.size();
			for (Iterator causeIt = causes.iterator(); causeIt.hasNext();) {
				Node cause = (Node) causeIt.next();
				sumR += Math.abs(getBetaWeight(graph2, cause, caused));
			}
		}
		// Util.print("averageR "+nEdges+" "+sumR);
		return sumR / nEdges;
	}

	double R(Graph graph2, Node caused) {
		double sumR = 0;
		Set causes = graph2.getCauses(caused);
		for (Iterator causeIt = causes.iterator(); causeIt.hasNext();) {
			Node cause = (Node) causeIt.next();
			sumR += Math.abs(getBetaWeight(graph2, cause, caused));
		}
		return sumR;
	}

	// private Query query() {
	// return popupFacet.query();
	// }

	List facetsOfInterest() {
		if (facetsOfInterest == null) {
			facetsOfInterest = Collections.unmodifiableList(new ArrayList(
					variables.values()));
		}
		return facetsOfInterest;
	}

	private int facetIndex(Perspective facet) {
		return facetsOfInterest().indexOf(facet);
	}

	private int nnn;

	// Collection primaryFacets() {
	// if (primaryFacets == null) {
	// primaryFacets = query().allRestrictions();
	// primaryFacets.add(popupFacet);
	// }
	// return primaryFacets;
	// }

	private int nFacets() {
		return variables.size();
	}

	private void addPerspective(Perspective p) {
		String name = computeTetradName(p, redrawer);

		variables.put(name, p);
		variablesInverse.put(p, name);
	}

	public static String computeTetradName(Perspective p,
			PerspectiveObserver redrawer1) {
		String name = p.getName(redrawer1);
		if (name == null)
			name = "p" + p.getServerID();
		else
			name = name.replaceAll("\n\r\\/:*?\"<>| ", "_");
		return name;
	}

	// private void setBidirectional(Graph graph) {
	// for (Iterator it = graph.getEdges().iterator(); it.hasNext();) {
	// Edge edge = (Edge) it.next();
	// edge.setBidirectional();
	// }
	// }

	private Perspective lookupFacet(Node node) {
		return (Perspective) node.object;
	}

	private Perspective lookupFacet(String label) {
		return (Perspective) variables.get(label);
	}

	private List lookupFacets(Collection nodes) {
		List result = new ArrayList(nodes.size());
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			result.add(lookupFacet(node));
		}
		return result;
	}

	String tetradName(Perspective p) {
		return (String) variablesInverse.get(p);
	}

	// Node[] primaryNodes(Graph graph1) {
	// Collection primaryFacets1 = primaryFacets;
	// Collection primaryNodes = new ArrayList(primaryFacets1.size());
	// for (Iterator it = primaryFacets1.iterator(); it.hasNext();) {
	// Perspective p = (Perspective) it.next();
	// Node node = graph1.getNode(p);
	// if (node != null)
	// primaryNodes.add(node);
	// }
	// return (Node[]) primaryNodes.toArray(new Node[0]);
	// }

	private static void removeAncestorFacets(Collection primary,
			Collection facets, Perspective popupFacet) {
		// Util.print("removeAncestorFacets " + primary + " " + facets);
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			Collection ancestors = facet.ancestors();
			// Never remove popupFacet
			ancestors.remove(popupFacet);
			for (Iterator ancIt = ancestors.iterator(); ancIt.hasNext();) {
				Perspective ancestor = (Perspective) ancIt.next();
				if (primary.contains(facet) || !primary.contains(ancestor)) {
					primary.remove(ancestor);
					if (facets.remove(ancestor)) {
						// Avoid concurrent modification of facets
						removeAncestorFacets(primary, facets, popupFacet);
						return;
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

		// pcSearch.setDepth(5);
		edu.cmu.tetrad.graph.Graph tetradGraph = new PcSearch(
				independenceTest, knowledge).search();
		Util.print(tetradGraph);
		Graph graph1 = getGraph(tetradGraph);
		// Util.print("graph: " + graph1);

		return graph1;
	}

	private Graph getGraph(edu.cmu.tetrad.graph.Graph tetradGraph) {
		Graph result = new Graph(this);
		Map nodeMap = new HashMap();
		for (Iterator it = tetradGraph.getNodes().iterator(); it.hasNext();) {
			edu.cmu.tetrad.graph.Node node = (edu.cmu.tetrad.graph.Node) it
					.next();
			Node resultNode = result.addNode(lookupFacet(node.getName()), node
					.getName());
			nodeMap.put(node, resultNode);
		}
		for (Iterator it = tetradGraph.getEdges().iterator(); it.hasNext();) {
			edu.cmu.tetrad.graph.Edge edge = (edu.cmu.tetrad.graph.Edge) it
					.next();
			edu.cmu.tetrad.graph.Node node1 = edge.getNode1();
			Node resultNode1 = (Node) nodeMap.get(node1);
			edu.cmu.tetrad.graph.Node node2 = edge.getNode2();
			Node resultNode2 = (Node) nodeMap.get(node2);
			Edge resultEdge = result.addEdge((String) null, resultNode1,
					resultNode2);

			if (edge.getProximalEndpoint(node1) == Endpoint.ARROW) {
				resultEdge.addDirection(resultNode1);
			}
			if (edge.getProximalEndpoint(node2) == Endpoint.ARROW) {
				resultEdge.addDirection(resultNode2);
			}
		}
		return result;
	}

	private class BetaCache {
		final double coef;
		final double beta; // this is now normalized coef
		final double relativeBeta; // this is now beta (coef * r)
		final String label;

		BetaCache(double coef, double beta, double relativeBeta, String label) {
			this.coef = coef;
			this.beta = beta;
			this.relativeBeta = relativeBeta;
			this.label = label;
		}

		public double getBeta() {
			return relativeBeta;
		}

		public double getCoef() {
			return coef;
		}

		public String getLabel() {
			return label;
		}

		public String toString() {
			return "<BetaCache " + coef + ">";
		}
	}

	private BetaCache getBetaWeights(Perspective cause, List otherCauses,
			Perspective caused, boolean isLogistic) {
		// Util.print("getBetaWeights "+cause+" => "+caused);
		int index = otherCauses.indexOf(cause) + 1;
		return getBetas(cause, otherCauses, caused, isLogistic)[index];
	}

	private List getBetaArgs(Perspective cause, Collection otherCauses,
			Perspective caused) {
		assert cause == null || otherCauses.contains(cause);
		// assert otherCauses.size() == (new HashSet(otherCauses)).size();
		Collection sortedCauses = otherCauses; // new
		// ArrayList(otherCauses.size()
		// + 1); // all causes
		// (cause +
		// otherCauses
		// ) in
		// canonical
		// order
		// sortedCauses.addAll(otherCauses);
		// if (!otherCauses.contains(cause))
		// sortedCauses.add(cause);
		// too slow
		// Collections.sort(sortedCauses);
		assert !sortedCauses.contains(caused) : caused + " " + sortedCauses;

		List args = new ArrayList(sortedCauses.size() + 1); // sortedCauses
		// followed by
		// caused
		args.addAll(sortedCauses);
		// Collections.sort(args);
		// List sortedCauses = new ArrayList(args); // causes in canonical order
		args.add(caused);
		return args;
	}

	private BetaCache[] getBetas(Perspective cause, Collection otherCauses,
			Perspective caused, boolean isLogistic) {
		List args = getBetaArgs(cause, otherCauses, caused);
		BetaCache[] cached = (BetaCache[]) betaWeights.get(args);
		if (cached == null) {
			cached = new BetaCache[otherCauses.size() + 1];
			MyRegressionParams params = getRegressionParams(otherCauses, caused);
			// assert params.nRows() == nRows() :
			// Util.valueOfDeep(params.counts);
			try {
				nnn++;
				TetradRegressionResult regression = isLogistic ? MyLogisticRegressionResult
						.getInstance(params)
						: MyLinearRegressionResult.getInstance(params);
				double[] coefs = regression.getCoefs();
				double sumCoefs = 0;
				for (int i = 0; i < params.regressorNames.length; i++) {
					// assert params.regressorNames[i]
					// .equals(tetradName((Perspective) args.get(i))) : Util
					// .valueOfDeep(params.regressorNames)
					// + " " + sortedCauses + " " + args;
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
				// double maxR = 1;
				// // try {
				// maxR = Math.sqrt(regression.maxRsquared());
				// // } catch (AssertionError e) {
				// // Util.print("Ignoring AssertionError getting maxR for "
				// // + cause + " => " + caused + " " + otherCauses);
				// // }

				cached[0] = new BetaCache(coefs[0], 0, 0, null);
				for (int i = 1; i < cached.length; i++) {
					double coef = coefs[i] / sumCoefs;
					double beta = r * coef;
					String label = formatWeight(coef) + " * " + formatWeight(r)
					/* + " / " + formatWeight(maxR) */;
					cached[i] = new BetaCache(coefs[i], coef, beta, label);
				}

				betaWeights.put(args, cached);
				// Util.print("beta     "
				// + Util.valueOfDeep(params.regressorNames) + "-->"
				// + tetradName(caused) + " " + r + " = "
				// + Util.valueOfDeep(cached));
			} catch (AssertionError e) {
				Util.print("Ignoring AssertionError in getBetaWeights for "
						+ cause + " => " + caused + otherCauses);
				Util.print(params.getDiscreteData(7));
				// e.printStackTrace();
				BetaCache foo = new BetaCache(0, 0, 0, "Bogus");
				for (int i = 0; i < cached.length; i++) {
					cached[i] = foo;
				}
				betaWeights.put(args, cached);
				throw (e);
			}
		}
		return cached;
	}

	private double getBetaWeight(Graph graph, Node cause, Node caused) {
		Collection otherCauses = graph.getCauses(caused);
		return getBetaWeights(lookupFacet(cause), lookupFacets(otherCauses),
				lookupFacet(caused), IS_LOGISTIC).getBeta();
	}

	double getWeight(List otherCauses, Perspective cause, Perspective caused) {
		return getBetaWeights(cause, otherCauses, caused, IS_LOGISTIC)
				.getBeta();
	}

	String getLabel(List otherCauses, Perspective cause, Perspective caused) {
		return getBetaWeights(cause, otherCauses, caused, IS_LOGISTIC)
				.getLabel();
	}

	double getBias(Collection otherCauses, Perspective caused) {
		// Tetrad barfs on regression with no inputs, and we don't really care
		// inn that case anyway
		double result = otherCauses.isEmpty() ? 1 : getBetas(null, otherCauses,
				caused, IS_LOGISTIC)[0].getCoef();
		return result;
	}

	// private String getBetaLabel(Graph graph, Node cause, Node caused) {
	// Collection otherCauses = graph.getCauses(caused);
	// return getBetaWeights(lookupFacet(cause), lookupFacets(otherCauses),
	// lookupFacet(caused), IS_LOGISTIC).label;
	// }

	void label(Graph graph) {
		for (Iterator it = graph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			List nodes = edge.getNodes();
			Node node1 = (Node) nodes.get(0);
			Node node2 = (Node) nodes.get(1);
			double wt1 = edge.canCause(node1) ? getBetaWeight(graph, node2,
					node1) : 0;
			double wt2 = edge.canCause(node2) ? getBetaWeight(graph, node1,
					node2) : 0;
			edge.setLabel("   " + formatWeight(wt1) + "   ", Edge.LEFT_LABEL);
			edge.setLabel("   " + formatWeight(wt2) + "   ", Edge.RIGHT_LABEL);
		}
		for (Iterator it = graph.getNodes().iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			String label = formatWeight(R(graph, node)) + " " + node.getLabel();
			node.setLabel(label);
		}
	}

	// private String getBetaLabel(Graph graph, Edge edge) {
	// StringBuffer buf = new StringBuffer();
	// for (Iterator it = edge.getNodes().iterator(); it.hasNext();) {
	// Node caused = (Node) it.next();
	// if (edge.canCause(caused)) {
	// if (buf.length() > 0) {
	// buf.append(" / ");
	// }
	// buf
	// .append(
	// getBetaLabel(graph, edge.getDistalNode(caused),
	// caused)).append(
	// caused.getLabel().charAt(0));
	// }
	// }
	// return buf.toString();
	// }

	static String formatWeight(double weight) {
		if (Double.isNaN(weight))
			return "?";
		return Integer.toString((int) Math.rint(100 * weight));
	}

	private MyRegressionParams getRegressionParams(Collection causes,
			Perspective caused) {
		// assert causes.size() == (new HashSet(causes)).size();
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
		return getCountsInternal(facetsOfInterest());
	}

	static int[] getCountsInternal(List facetsOfInterest2) {
		List sorted = new ArrayList(facetsOfInterest2);
		Collections.sort(sorted);
		return Distribution.ensureDist(sorted).getMarginalCounts(
				facetsOfInterest2);

		// // if (counts == null) {
		// int nFacets = facetsOfInterest2.size();
		// int[] counts1 = new int[1 << nFacets];
		// Perspective popupFacet1 = (Perspective) facetsOfInterest2.get(0);
		// Query query = popupFacet1.query();
		// ResultSet rs = query.onCountMatrix(facetsOfInterest2,null, true)[0];
		// try {
		// while (rs.next()) {
		// counts1[rs.getInt(1)] = rs.getInt(2);
		// }
		// rs.close();
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }
		//
		// Util.print("\n" + nFacets + " primaryFacets\n "
		// + Util.join(facetsOfInterest2, "\n "));
		// List others = new LinkedList(facetsOfInterest2);
		// others.removeAll(facetsOfInterest2);
		// Util.print(others.size() + " other facets of interest\n "
		// + Util.join(others, "\n "));
		// // Util.print("counts: " + Util.valueOfDeep(counts));
		//
		// // The low order bit represents the first facet (popupFacet)
		// int countInUniverse = getCountInUniverse(counts1, 0);
		// assert countInUniverse == popupFacet1.getTotalCount() : popupFacet1
		// + " " + popupFacet1.getTotalCount() + " " + countInUniverse;
		// // }
		// return counts1;
	}

	// private static int getCountInUniverse(int[] counts1, int varIndex) {
	// assert varIndex >= 0 && 1 << varIndex < counts1.length;
	// int total = 0;
	// for (int j = 0; j < counts1.length; j++) {
	// if (Util.isBit(j, varIndex))
	// total += counts1[j];
	// }
	// return total;
	// }

	private int nRows() {
		// return query().getTotalCount();
		int result = 0;
		for (int i = 0; i < counts.length; i++) {
			result += counts[i];
		}
		return result;
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

	// private String label;

	private String getlabel() {
		// if (label == null) {
		StringBuffer base = new StringBuffer();
		for (Iterator it = primaryFacets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (base.length() > 0)
				base.append(" ");
			base.append(tetradName(p));
		}
		base.append(" ").append(formatWeight(MIN_BETA))/*
														 * .append("_").append (
														 * formatWeight (
														 * MIN_CORE_NONCORE_EDGE_BETA
														 * ) ).append("_")
														 * .append( formatWeight
														 * (
														 * MIN_NONCORE_EDGE_BETA
														 * ))
														 */.append("_").append(
				MAX_FACETS).append(" ");
		return base.toString();
		// }
		// return label;
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
			MIN_BETA *= factor;
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
		if (result)
			Util.print("MAX_FACETS=" + MAX_FACETS + ", MIN_BETA=" + MIN_BETA);
		return result;
	}

	private int printIndex;

	private void printMe(String status, Graph graph) {
		if (false && printer != null) {
			label(graph);
			printer.drawTetradGraph(graph, Util.extensionFormat
					.format(printIndex++)
					+ " " + status);
		}
	}

	public double threshold() {
		return MIN_BETA;
	}

	public double weight(Graph graph, Node cause, Node caused) {
		return getBetaWeight(graph, cause, caused);
	}

}

// class PerspectiveParentPrinter {
// private final Perspective facet;
//
// private static final StringAlign align = new StringAlign(13,
// StringAlign.JUST_LEFT);
//
// PerspectiveParentPrinter(Perspective p) {
// facet = p;
// }
//
// public String toString() {
// String result = align.format(facet);
// if (facet.getParent() != null) {
// result += " => " + facet.getParent();
// }
// return result;
// }
//
// }
