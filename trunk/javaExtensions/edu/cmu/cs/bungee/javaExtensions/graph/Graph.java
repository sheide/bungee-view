package edu.cmu.cs.bungee.javaExtensions.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.PermutationIterator;
import edu.cmu.cs.bungee.javaExtensions.Util;

//import edu.cmu.cs.bungee.javaExtensions.Util;

public class Graph<NodeObjectType extends Comparable<NodeObjectType>> {
	public interface GraphWeigher<NodeObjectType extends Comparable<NodeObjectType>> {
		double weight(Graph<NodeObjectType> graph, Node<NodeObjectType> cause, Node<NodeObjectType> caused);

		double threshold();
	}

	private static final int GRAPH_EDGE_LENGTH = 250;
	public float labelW = GRAPH_EDGE_LENGTH * 0.7f;
	private final Map<Set<Node<NodeObjectType>>, Edge<NodeObjectType>> edgesLookupTable;
	private final Set<Edge<NodeObjectType>> edges;
	private final Map<Node<NodeObjectType>, Integer> nodesTable;
	private final Map<NodeObjectType, Node<NodeObjectType>> namesToNodesTable;
	private int nodeIndex;
	private GraphWeigher<NodeObjectType> weigher;
	public String label = "Graph";

	public Graph(GraphWeigher<NodeObjectType> weigher) {
		this.weigher = weigher;
		edgesLookupTable = new HashMap<Set<Node<NodeObjectType>>, Edge<NodeObjectType>>();
		edges = new HashSet<Edge<NodeObjectType>>();
		nodesTable = new HashMap<Node<NodeObjectType>, Integer>();
		namesToNodesTable = new HashMap<NodeObjectType, Node<NodeObjectType>>();
	}

	public Graph(Graph<NodeObjectType> copyFrom) {
		edgesLookupTable = new HashMap<Set<Node<NodeObjectType>>, Edge<NodeObjectType>>(
				copyFrom.edgesLookupTable);
		edges = new HashSet<Edge<NodeObjectType>>(copyFrom.edges);
		nodesTable = new HashMap<Node<NodeObjectType>, Integer>(
				copyFrom.nodesTable);
		namesToNodesTable = new HashMap<NodeObjectType, Node<NodeObjectType>>(
				copyFrom.namesToNodesTable);
		nodeIndex = copyFrom.nodeIndex;
		label = copyFrom.label;
		weigher = copyFrom.weigher;
		// Util.print("hh "+nodesTable);
	}

	private boolean checkConsistency() {
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			// assert edge.getNumDirections() > 0;
			for (Iterator<Node<NodeObjectType>> it2 = edge.getNodes()
					.iterator(); it2.hasNext();) {
				Node<NodeObjectType> node = it2.next();
				assert hasNode(node);
			}
		}
		return true;
	}

	private int nodeIndex(Node<NodeObjectType> node) {
		Integer index = nodesTable.get(node);
		return index == null ? -1 : index.intValue();
	}

	public boolean hasNode(Node<NodeObjectType> node) {
		return nodeIndex(node) > -1;
	}

	public Node<NodeObjectType> getNode(String nodeLabel) {
		return namesToNodesTable.get(nodeLabel);
	}

	public Node<NodeObjectType> addNode(NodeObjectType object, String name) {
		assert getNode(name) == null;
		Node<NodeObjectType> node = new Node<NodeObjectType>(object, name);
		ensureNode(node);
		return node;
	}

	public Node<NodeObjectType> addNode(Node<NodeObjectType> node) {
		assert !hasNode(node);
		ensureNode(node);
		return node;
	}

	private void ensureNode(Node<NodeObjectType> node) {
		if (!hasNode(node)) {
			nodesTable.put(node, new Integer(nodeIndex++));
			assert node.object != null;
			namesToNodesTable.put(node.object, node);
			assert checkConsistency();
		}
	}

	public void removeNode(Node<NodeObjectType> node) {
		assert hasNode(node);
		assert hasNode(node);
		for (Iterator<Edge<NodeObjectType>> it = getEdges(node).iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			removeEdge(edge);
		}
		nodesTable.remove(node);
		assert node.object != null;
		namesToNodesTable.remove(node.object);
		assert checkConsistency();
	}

	public void removeNode(String label1) {
		removeNode(getNode(label1));
	}

	public Edge<NodeObjectType> getEdge(Node<NodeObjectType> node1,
			Node<NodeObjectType> node2) {
		assert node1 != null;
		assert node2 != null;
		Set<Node<NodeObjectType>> nodes = new HashSet<Node<NodeObjectType>>(2);
		nodes.add(node1);
		nodes.add(node2);
		return getEdge(nodes);
	}

	public Edge<NodeObjectType> getEdge(Set<Node<NodeObjectType>> nodes) {
		assert nodes.size() == 2;
		for (Iterator<Node<NodeObjectType>> it = nodes.iterator(); it.hasNext();) {
			Node<NodeObjectType> node = it.next();
			assert hasNode(node) : node + " " + this;
		}
		return edgesLookupTable.get(nodes);
	}

	public Edge<NodeObjectType> addEdge(String label1,
			Node<NodeObjectType> node1, Node<NodeObjectType> node2) {
		String[] labels = { null, label1, null };
		return addEdge(labels, node1, node2);
	}

	public Edge<NodeObjectType> addEdge(String[] labels,
			Node<NodeObjectType> node1, Node<NodeObjectType> node2) {
		assert hasNode(node1) && hasNode(node2);
		assert getEdge(node1, node2) == null;
		if (labels == null)
			labels = new String[3];
		assert labels.length == 3;
		Edge<NodeObjectType> edge = new Edge<NodeObjectType>(labels, node1,
				node2);
		Set<Node<NodeObjectType>> nodes = new HashSet<Node<NodeObjectType>>(2);
		nodes.add(node1);
		nodes.add(node2);
		edgesLookupTable.put(nodes, edge);
		edges.add(edge);
		assert checkConsistency();
		return edge;
	}

	public void removeEdge(Edge<NodeObjectType> edge) {
		assert edges.contains(edge);
		Set<Node<NodeObjectType>> nodes = new HashSet<Node<NodeObjectType>>(
				edge.getNodes());
		edgesLookupTable.remove(nodes);
		edges.remove(edge);
		assert checkConsistency();
	}

	public int getNumDirectedEdges() {
		int result = 0;
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			result += edge.getNumDirections();
		}
		return result;
	}

	public int getNumEdges() {
		return edges.size();
	}

	public Set<Edge<NodeObjectType>> getEdges() {
		// too slow
		// return Collections.unmodifiableSet(edges);
		return edges;
	}

	public Set<Edge<NodeObjectType>> getEdges(Node<NodeObjectType> node) {
		assert hasNode(node);
		Set<Edge<NodeObjectType>> result = new HashSet<Edge<NodeObjectType>>();
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (edge.hasNode(node))
				result.add(edge);
		}
		return result;
	}

	public Iterator<Edge<NodeObjectType>> getNodeEdgeIterator(
			Node<NodeObjectType> node) {
		return new NodeEdgeIterator(node);
	}

	public class NodeEdgeIterator implements Iterator<Edge<NodeObjectType>> {
		private final Node<NodeObjectType> node;
		private final Iterator<Edge<NodeObjectType>> edgeIterator;

		private Edge<NodeObjectType> next;

		NodeEdgeIterator(Node<NodeObjectType> node) {
			this.node = node;
			edgeIterator = getEdges().iterator();
			peek();
		}

		private void peek() {
			next = null;
			while (edgeIterator.hasNext() && next == null) {
				Edge<NodeObjectType> edge = edgeIterator.next();
				if (edge.hasNode(node))
					next = edge;
			}
		}

		public boolean hasNext() {
			return next != null;
		}

		public Edge<NodeObjectType> next() {
			Edge<NodeObjectType> result = next;
			peek();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Set<Edge<NodeObjectType>> getUpstreamEdges(Node<NodeObjectType> node) {
		assert hasNode(node);
		Set<Edge<NodeObjectType>> result = new HashSet<Edge<NodeObjectType>>();
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (edge.hasNode(node) && edge.canCause(node))
				result.add(edge);
		}
		return result;
	}

	public Set<Node<NodeObjectType>> getCauses(Node<NodeObjectType> node) {
		assert hasNode(node);
		Set<Node<NodeObjectType>> result = new HashSet<Node<NodeObjectType>>();
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (edge.hasNode(node) && edge.canCause(node))
				result.add(edge.getDistalNode(node));
		}
		return result;
	}

	public void pruneNullEdges() {
		Collection<Edge<NodeObjectType>> weak = new LinkedList<Edge<NodeObjectType>>();
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (edge.getNumDirections() == 0)
				weak.add(edge);
		}
		for (Iterator<Edge<NodeObjectType>> it = weak.iterator(); it.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			removeEdge(edge);
		}
	}

	/**
	 * @param leftNode
	 *            index of proposed left node
	 * @param rightNode
	 *            index of proposed right node
	 * @return whether this left/right layout is better than the reverse
	 */
	private boolean moreCaused(Node<NodeObjectType> leftNode,
			Node<NodeObjectType> rightNode) {
		int causeScore = netCauses(leftNode);
		int causedScore = netCauses(rightNode);
		boolean result = causeScore == causedScore ? nodeIndex(leftNode) < nodeIndex(rightNode)
				: causeScore < causedScore;
		// Util.print("moreCaused " + graph1.getNodes().get(causeIndex) + " => "
		// + graph1.getNodes().get(causedIndex) + " = " + result);
		return result;
	}

	private int netCauses(Node<NodeObjectType> node) {
		int score = 0;
		for (Iterator<Edge<NodeObjectType>> it = getNodeEdgeIterator(node); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (edge.canCause(node))
				score++;
			Node<NodeObjectType> distal = edge.getDistalNode(node);
			if (edge.canCause(distal))
				score--;
		}
		// Util.print("netCauses " + node + " " + score);
		return score;
	}

	private int nMoreCaused(List<Node<NodeObjectType>> nodes) {
		int result = 0;
		for (int causeIndex = 0; causeIndex < nodes.size(); causeIndex++) {
			Node<NodeObjectType> cause = nodes.get(causeIndex);
			for (int causedIndex = causeIndex + 1; causedIndex < nodes.size(); causedIndex++) {
				Node<NodeObjectType> caused = nodes.get(causedIndex);
				if (moreCaused(cause, caused))
					result++;
			}
		}
		return result;
	}

	/**
	 * Place node centers in a circle, ordered to minimize edge crossings.
	 */
	@SuppressWarnings("unchecked")
	public void layout() {
		// long start = new Date().getTime();
		int nNodes = getNumNodes();
		assert nNodes > 0;
		List<Node<NodeObjectType>> nodes = new ArrayList<Node<NodeObjectType>>(
				getNodes());

		// use centerY as scratchpad for "moreCaused"
		for (Iterator<Node<NodeObjectType>> it = nodes.iterator(); it.hasNext();) {
			Node<NodeObjectType> node = it.next();
			node.setCenterY(netCauses(node) * nNodes + nodeIndex(node));
		}

		Collections.sort(nodes);
		// Util.print("layout " + nodes);
		Edge<NodeObjectType>[] edges1 = getEdges().toArray(new Edge[0]);
		List<Node<NodeObjectType>> bestPerm = null;
		int bestEdgeCrossings = Integer.MAX_VALUE;
		for (PermutationIterator<Node<NodeObjectType>> it = new PermutationIterator<Node<NodeObjectType>>(
				nodes); it.hasNext();) {
			List<Node<NodeObjectType>> perm = it.next();
			if (perm == null)
				break;
			if (perm.get(0) == nodes.get(0) && (nNodes < 3 ||

			// moreCaused((Node<NodeObjectType>) perm.get(1),
			// (Node<NodeObjectType>) perm.get(nNodes - 1))
					perm.get(1).getCenterY() < perm.get(nNodes - 1)
							.getCenterY()

					)) {

				// Canonicalize by starting at 0 and preferring arrows that
				// point to the right.

				// arrangeInCircle(0, GRAPH_EDGE_LENGTH, GRAPH_EDGE_LENGTH,
				// perm);
				// int edgeCrossings = edgeCrossings();

				int edgeCrossings = edgeCrossings(perm, edges1);

				// Util.print("layout " + edgeCrossings + "/" + getNumEdges()
				// + " " + perm);
				if (edgeCrossings < bestEdgeCrossings
						|| edgeCrossings == bestEdgeCrossings
						&& nMoreCaused(perm) > nMoreCaused(bestPerm)) {
					bestEdgeCrossings = edgeCrossings;
					bestPerm = new ArrayList<Node<NodeObjectType>>(perm);
				}
			}
		}
		// assert isPerm(bestPerm);
		arrangeInCircle(0, GRAPH_EDGE_LENGTH, GRAPH_EDGE_LENGTH, bestPerm);
		// Util.print("best layout " + bestPerm);
		// Util.print("layout took " + (new Date().getTime() - start) + " ms; "
		// + bestEdgeCrossings + " edge crossings");
	}

	public int getNumNodes() {
		return nodesTable.size();
	}

	// private boolean isPerm(int[] permutation) {
	// for (int i = 0; i < permutation.length; i++) {
	// assert Util.isMember(permutation, i) : Util
	// .valueOfDeep(permutation);
	// }
	// return true;
	// }

	/**
	 * Arranges the nodes in the graph clockwise in a circle, starting at 12
	 * o'clock.
	 * 
	 * 
	 * @param centerx
	 * @param centery
	 * @param radius
	 *            The radius of the circle in pixels; a good default is 150.
	 */
	private void arrangeInCircle(int centerx, int centery, int radius,
			List<Node<NodeObjectType>> perm) {

		double rad = 6.28 / getNumNodes();
		double phi = .75 * 6.28; // start from 12 o'clock.

		for (Iterator<Node<NodeObjectType>> it = perm.iterator(); it.hasNext();) {
			Node<NodeObjectType> node = it.next();
			int centerX = centerx + (int) (radius * Math.cos(phi));
			int centerY = centery + (int) (radius * Math.sin(phi));

			node.setCenterX(centerX);
			node.setCenterY(centerY);

			phi += rad;
		}
	}

	public Set<Node<NodeObjectType>> getNodes() {
		return Collections.unmodifiableSet(nodesTable.keySet());
	}

//	private static Edge[] emptyEdges = new Edge[0];

	// private static Node<NodeObjectType>[] emptyNodes = new
	// Node<NodeObjectType>[0];

	private int edgeCrossings(List<Node<NodeObjectType>> perm,
			Edge<NodeObjectType>[] edges1) {
		for (int i = 0; i < perm.size(); i++) {
			Node<NodeObjectType> node1 = perm.get(i);
			// use centerX as a scratchpad to record order
			node1.setCenterX(i);
		}
		int nCrosses = 0;
		for (int i = 0; i < edges1.length; i++) {
			Edge<NodeObjectType> edge1 = edges1[i];
			int edge1node1 = edge1.getNode1().getCenterX();
			int edge1node2 = edge1.getNode2().getCenterX();
			double edge1min = Math.min(edge1node1, edge1node2);
			double edge1max = Math.max(edge1node1, edge1node2);
			for (int j = i + 1; j < edges1.length; j++) {
				Edge<NodeObjectType> edge2 = edges1[j];
				int between1 = edgeCrossingsInternal(edge2.getNode1()
						.getCenterX(), edge1min, edge1max);
				if (between1 != 0) {
					int between2 = edgeCrossingsInternal(edge2.getNode2()
							.getCenterX(), edge1min, edge1max);
					if (between1 * between2 < 0)
						nCrosses++;
				}
			}
		}
		return nCrosses;
	}

	// 1 if min<node1<max; 0 if node1==min || node1==max; -1 otherwise
	private int edgeCrossingsInternal(double node1, double min, double max) {
		return node1 > min ? (node1 > max ? -1 : node1 == max ? 0 : 1)
				: (node1 == min ? 0 : -1);
	}

	// private int edgeCrossings() {
	// int nCrosses = 0;
	// // int index = 0;
	// Edge[] edges1 = (Edge[]) getEdges().toArray(emptyEdges);
	// for (int i = 0; i < edges1.length; i++) {
	// // index++;
	// Edge edge1 = edges1[i];
	// for (int j = i + 1; j < edges1.length; j++) {
	// Edge edge2 = edges1[j];
	// if (edge1.getIntersection(edge2) != null)
	// nCrosses++;
	// }
	// }
	//
	// // List edges1 = new ArrayList(getEdges());
	// // for (Iterator it1 = edges1.iterator(); it1.hasNext();) {
	// // index++;
	// // Edge edge1 = (Edge) it1.next();
	// // for (Iterator it2 = edges1.subList(index, edges1.size()).iterator();
	// // it2
	// // .hasNext();) {
	// // Edge edge2 = (Edge) it2.next();
	// // if (edge1.getIntersection(edge2) != null)
	// // nCrosses++;
	// // }
	// // }
	//
	// return nCrosses;
	// }

	public Set<Node<NodeObjectType>> getAdjacentNodes(Node<NodeObjectType> node) {
		assert hasNode(node);
		Set<Node<NodeObjectType>> result = new HashSet<Node<NodeObjectType>>();
		for (Iterator<Edge<NodeObjectType>> it = getNodeEdgeIterator(node); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			assert hasNode(edge.getDistalNode(node));
			result.add(edge.getDistalNode(node));
		}
		return result;
	}

	public void union(Graph<NodeObjectType> subgraph) {
		for (Iterator<Edge<NodeObjectType>> it = subgraph.getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			Node<NodeObjectType> caused = edge.getCausedNode();
			Node<NodeObjectType> cause = edge.getCausingNode();
			ensureNode(caused);
			ensureNode(cause);
			Edge<NodeObjectType> myEdge = getEdge(cause, caused);
			if (myEdge == null) {
				myEdge = addEdge(edge.getLabels(), cause, caused);
				myEdge.setDirection(caused);
			} else
				myEdge.addDirection(caused);

		}

	}

	/**
	 * @param subgraph
	 * @return whether all subgraph's [directed] edges are present in this Graph
	 */
	public boolean contains(Graph<NodeObjectType> subgraph) {
		if (subgraph.getNumEdges() > getNumEdges())
			return false;
		for (Iterator<Edge<NodeObjectType>> it = subgraph.getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> subedge = it.next();
			List<Node<NodeObjectType>> nodes = subedge.getNodes();
			Node<NodeObjectType> node1 = nodes.get(0);
			if (!hasNode(node1))
				return false;
			Node<NodeObjectType> node2 = nodes.get(1);
			if (!hasNode(node2))
				return false;
			Edge<NodeObjectType> edge = getEdge(node1, node2);
			if (edge == null)
				return false;
			if (subedge.canCause(node1) && !edge.canCause(node1))
				return false;
			if (subedge.canCause(node2) && !edge.canCause(node2))
				return false;
		}
		return true;
	}

	// Might be worthwhile to prune in order of strength
	public void pruneWeakEdges() {
		if (weigher != null) {
			int result = 1;
			while (result > 0) {
				// Unlikely but possible that pruning weakens another edge
				result = 0;
				for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
						.hasNext();) {
					Edge<NodeObjectType> edge = it.next();
					for (Iterator<Node<NodeObjectType>> nodeIt = edge
							.getNodes().iterator(); nodeIt.hasNext();) {
						Node<NodeObjectType> caused = nodeIt.next();
						if (edge.canCause(caused)) {
							double beta = Math.abs(weigher.weight(this, edge
									.getDistalNode(caused), caused));
							// Util.print("prune? "+edge+" "+beta);
							if (beta < weigher.threshold()) {
								edge.removeDirection(caused);
								result++;
							}
						}
					}
				}
				pruneNullEdges();
			}
		}
	}

	/**
	 * Makes graph1 undirected, and removes nodes that do not lie on a path
	 * between two core nodes.
	 */
	public int removeNonpathEdges(Node<NodeObjectType> primary1,
			Node<NodeObjectType> primary2) {
		Collection<Edge<NodeObjectType>> pathEdges = pathEdges(primary1,
				primary2, 0);
		return retainAllEdges(pathEdges);
	}

	private int retainAllEdges(Collection<Edge<NodeObjectType>> pathEdges) {
		int result = 0;
		for (Iterator<Edge<NodeObjectType>> it = (new ArrayList<Edge<NodeObjectType>>(
				getEdges())).iterator(); it.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			if (!pathEdges.contains(edge)) {
				// Util.print("Removing satellite node " + node);
				removeEdge(edge);
				result++;
			}
		}
		return result;
	}

	/**
	 * Makes graph1 undirected, and removes nodes that do not lie on a path
	 * between two core nodes.
	 */
	public int removeNonpathEdges(Node<NodeObjectType>[] primaryNodes) {
		Collection<Edge<NodeObjectType>> pathEdges = new HashSet<Edge<NodeObjectType>>();
		for (int i = 0; i < primaryNodes.length; i++) {
			Node<NodeObjectType> primary1 = primaryNodes[i];
			for (int j = i + 1; j < primaryNodes.length; j++) {
				Node<NodeObjectType> primary2 = primaryNodes[j];
				pathEdges.addAll(pathEdges(primary1, primary2, 0));
			}
		}
		// Util.print("removeNPE "+Util.valueOfDeep(primaryNodes)+" "+pathEdges);
		int result = retainAllEdges(pathEdges);
		Collection<Node<NodeObjectType>> nodesToRemove = new LinkedList<Node<NodeObjectType>>();
		for (Iterator<Node<NodeObjectType>> it = getNodes().iterator(); it
				.hasNext();) {
			Node<NodeObjectType> node = it.next();
			if (!Util.isMember(primaryNodes, node)
					&& getEdges(node).size() == 0)
				nodesToRemove.add(node);
		}
		for (Iterator<Node<NodeObjectType>> it = nodesToRemove.iterator(); it
				.hasNext();) {
			Node<NodeObjectType> node = it.next();
			removeNode(node);
		}
		return result;
	}

	public boolean allOnPath(Node<NodeObjectType> primary1,
			Node<NodeObjectType> primary2, double threahold) {
		return pathEdges(primary1, primary2, threahold).size() == getNumEdges();
	}

	/**
	 * downstream means in the direction of the arrow
	 * 
	 * @return all edges in graph1 that lie on a colliderless path between two
	 *         primary nodes
	 */
	private Collection<Edge<NodeObjectType>> pathEdges(
			Node<NodeObjectType> primary1, Node<NodeObjectType> primary2,
			double threshold) {
		Collection<Edge<NodeObjectType>> pathEdges = new HashSet<Edge<NodeObjectType>>();
		Collection<Node<NodeObjectType>> nodeStack = new HashSet<Node<NodeObjectType>>();
		nodeStack.add(primary1);
		pathEdgesInternal(pathEdges, primary2, nodeStack, primary1, false,
				threshold, 1);
		return pathEdges;
	}

	/**
	 * Depth-first sesarch, backtracking if we're in a loop, or meet a collider.
	 * If we reach a goal node, add the current path to pathEdges
	 */
	private boolean pathEdgesInternal(
			Collection<Edge<NodeObjectType>> pathEdges,
			Node<NodeObjectType> goalNode,
			Collection<Node<NodeObjectType>> nodeStack,
			Node<NodeObjectType> node, boolean downstreamOnly,
			double threshold, double strength) {
		boolean result = false;
		for (Iterator<Edge<NodeObjectType>> it = getNodeEdgeIterator(node); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			Node<NodeObjectType> adj = edge.getDistalNode(node);
			// Util.print("RSNI " + node + " " + adj + " " + seenArrow);
			boolean isDownstream = edge.canCause(adj);
			// It's always OK to go downstream, but once you have, you can't go
			// up again
			if (!nodeStack.contains(adj) && (!downstreamOnly || isDownstream)) {
				double beta = threshold > 0 && weigher != null ? Math
						.abs(weigher.weight(this, edge.getCausingNode(), edge
								.getCausedNode())) : 1;
				double substrength = strength * beta;
				if (substrength >= threshold) {
					boolean add = goalNode == adj;
					if (!add) {
						nodeStack.add(adj);
						add = pathEdgesInternal(pathEdges, goalNode, nodeStack,
								adj, downstreamOnly || isDownstream, threshold,
								substrength);
						nodeStack.remove(adj);
					}
					if (add) {
						pathEdges.add(edge);
						result = true;
					}
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Iterator<Edge<NodeObjectType>> it = getEdges().iterator(); it
				.hasNext();) {
			Edge<NodeObjectType> edge = it.next();
			buf.append(edge).append("\n");
		}
		return buf.toString();
	}

}
