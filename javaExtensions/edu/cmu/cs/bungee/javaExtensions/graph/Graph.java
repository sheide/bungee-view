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

public class Graph {
	public interface GraphWeigher {
		double weight(Graph graph, Node cause, Node caused);

		double threshold();
	}

	private static final int GRAPH_EDGE_LENGTH = 250;
	private final Map edgesLookupTable;
	private final Set edges;
	private final Map nodesTable;
	private final Map namesToNodesTable;
	private int nodeIndex;
	private GraphWeigher weigher;
	public String label = "Graph";

	public Graph(GraphWeigher weigher) {
		this.weigher = weigher;
		edgesLookupTable = new HashMap();
		edges = new HashSet();
		nodesTable = new HashMap();
		namesToNodesTable = new HashMap();
	}

	public Graph(Graph copyFrom) {
		edgesLookupTable = new HashMap(copyFrom.edgesLookupTable);
		edges = new HashSet(copyFrom.edges);
		nodesTable = new HashMap(copyFrom.nodesTable);
		namesToNodesTable = new HashMap(copyFrom.namesToNodesTable);
		nodeIndex = copyFrom.nodeIndex;
		label = copyFrom.label;
		weigher = copyFrom.weigher;
		// Util.print("hh "+nodesTable);
	}

	private boolean checkConsistency() {
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			// assert edge.getNumDirections() > 0;
			for (Iterator it2 = edge.getNodes().iterator(); it2.hasNext();) {
				Node node = (Node) it2.next();
				assert hasNode(node);
			}
		}
		return true;
	}

	private int nodeIndex(Node node) {
		Integer index = (Integer) nodesTable.get(node);
		return index == null ? -1 : index.intValue();
	}

	public boolean hasNode(Node node) {
		return nodeIndex(node) > -1;
	}

	public Node getNode(Object object) {
		return (Node) namesToNodesTable.get(object);
	}

	public Node addNode(Object object, String name) {
		assert getNode(name) == null;
		Node node = new Node(object, name);
		ensureNode(node);
		return node;
	}

	public Node addNode(Node node) {
		assert !hasNode(node);
		ensureNode(node);
		return node;
	}

	private void ensureNode(Node node) {
		if (!hasNode(node)) {
			nodesTable.put(node, new Integer(nodeIndex++));
			assert node.object != null;
			namesToNodesTable.put(node.object, node);
			assert checkConsistency();
		}
	}

	public void removeNode(Node node) {
		assert hasNode(node);
		assert hasNode(node);
		for (Iterator it = getEdges(node).iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
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

	public Edge getEdge(Node node1, Node node2) {
		assert node1 != null;
		assert node2 != null;
		Set nodes = new HashSet(2);
		nodes.add(node1);
		nodes.add(node2);
		return getEdge(nodes);
	}

	public Edge getEdge(Set nodes) {
		assert nodes.size() == 2;
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			assert hasNode(node) : node + " " + this;
		}
		return (Edge) edgesLookupTable.get(nodes);
	}

	public Edge addEdge(String label1, Node node1, Node node2) {
		String[] labels = { null, label1, null };
		return addEdge(labels, node1, node2);
	}

	public Edge addEdge(String[] labels, Node node1, Node node2) {
		assert hasNode(node1) && hasNode(node2);
		assert getEdge(node1, node2) == null;
		if (labels == null)
			labels = new String[3];
		assert labels.length == 3;
		Edge edge = new Edge(labels, node1, node2);
		Set nodes = new HashSet(2);
		nodes.add(node1);
		nodes.add(node2);
		edgesLookupTable.put(nodes, edge);
		edges.add(edge);
		assert checkConsistency();
		return edge;
	}

	public void removeEdge(Edge edge) {
		assert edges.contains(edge);
		Set nodes = new HashSet(edge.getNodes());
		edgesLookupTable.remove(nodes);
		edges.remove(edge);
		assert checkConsistency();
	}

	public int getNumDirectedEdges() {
		int result = 0;
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			result += edge.getNumDirections();
		}
		return result;
	}

	public int getNumEdges() {
		return edges.size();
	}

	public Set getEdges() {
		// too slow
		// return Collections.unmodifiableSet(edges);
		return edges;
	}

	public Set getEdges(Node node) {
		assert hasNode(node);
		Set result = new HashSet();
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.hasNode(node))
				result.add(edge);
		}
		return result;
	}

	public Iterator getNodeEdgeIterator(Node node) {
		return new NodeEdgeIterator(node);
	}

	public class NodeEdgeIterator implements Iterator {
		private final Node node;
		private final Iterator edgeIterator;

		private Edge next;

		NodeEdgeIterator(Node node) {
			this.node = node;
			edgeIterator = getEdges().iterator();
			peek();
		}

		private void peek() {
			next = null;
			while (edgeIterator.hasNext() && next == null) {
				Edge edge = (Edge) edgeIterator.next();
				if (edge.hasNode(node))
					next = edge;
			}
		}

		public boolean hasNext() {
			return next != null;
		}

		public Object next() {
			Edge result = next;
			peek();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Set getUpstreamEdges(Node node) {
		assert hasNode(node);
		Set result = new HashSet();
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.hasNode(node) && edge.canCause(node))
				result.add(edge);
		}
		return result;
	}

	public Set getCauses(Node node) {
		assert hasNode(node);
		Set result = new HashSet();
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.hasNode(node) && edge.canCause(node))
				result.add(edge.getDistalNode(node));
		}
		return result;
	}

	public void pruneNullEdges() {
		Collection weak = new LinkedList();
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.getNumDirections() == 0)
				weak.add(edge);
		}
		for (Iterator it = weak.iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
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
	private boolean moreCaused(Node leftNode, Node rightNode) {
		int causeScore = netCauses(leftNode);
		int causedScore = netCauses(rightNode);
		boolean result = causeScore == causedScore ? nodeIndex(leftNode) < nodeIndex(rightNode)
				: causeScore < causedScore;
		// Util.print("moreCaused " + graph1.getNodes().get(causeIndex) + " => "
		// + graph1.getNodes().get(causedIndex) + " = " + result);
		return result;
	}

	private int netCauses(Node node) {
		int score = 0;
		for (Iterator it = getNodeEdgeIterator(node); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.canCause(node))
				score++;
			Node distal = edge.getDistalNode(node);
			if (edge.canCause(distal))
				score--;
		}
		// Util.print("netCauses " + node + " " + score);
		return score;
	}

	/**
	 * Place node centers in a circle, ordered to minimize edge crossings.
	 */
	public void layout() {
		List nodes = new ArrayList(getNodes());
		int nNodes = getNumNodes();
		assert nNodes > 0;
		List bestPerm = null;
		int bestEdgeCrossings = Integer.MAX_VALUE;
		for (PermutationIterator it = new PermutationIterator(nodes); it
				.hasNext();) {
			List perm = (List) it.next();
			if (perm == null)
				break;
			if (perm.get(0) == nodes.get(0)
					&& (nNodes < 3 || moreCaused((Node) perm.get(nNodes - 1),
							(Node) perm.get(1)))) {
				// Canonicalize by starting at 0 and preferring arrows that
				// point to the right.
				arrangeInCircle(0, GRAPH_EDGE_LENGTH, GRAPH_EDGE_LENGTH, perm);
				int edgeCrossings = edgeCrossings();
				if (edgeCrossings < bestEdgeCrossings) {
					bestEdgeCrossings = edgeCrossings;
					bestPerm = new ArrayList(perm);
				}
			}
		}
		// assert isPerm(bestPerm);
		arrangeInCircle(0, GRAPH_EDGE_LENGTH, GRAPH_EDGE_LENGTH, bestPerm);
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
	private void arrangeInCircle(int centerx, int centery, int radius, List perm) {

		double rad = 6.28 / getNumNodes();
		double phi = .75 * 6.28; // start from 12 o'clock.

		for (Iterator it = perm.iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			int centerX = centerx + (int) (radius * Math.cos(phi));
			int centerY = centery + (int) (radius * Math.sin(phi));

			node.setCenterX(centerX);
			node.setCenterY(centerY);

			phi += rad;
		}
	}

	public Set getNodes() {
		return Collections.unmodifiableSet(nodesTable.keySet());
	}

	private int edgeCrossings() {
		int nCrosses = 0;
		List edges1 = new ArrayList(getEdges());
		int index = 0;
		for (Iterator it1 = edges1.iterator(); it1.hasNext();) {
			index++;
			Edge edge1 = (Edge) it1.next();
			for (Iterator it2 = edges1.subList(index, edges1.size()).iterator(); it2
					.hasNext();) {
				Edge edge2 = (Edge) it2.next();
				if (edge1.getIntersection(edge2) != null)
					nCrosses++;
			}
		}
		return nCrosses;
	}

	public Set getAdjacentNodes(Node node) {
		assert hasNode(node);
		Set result = new HashSet();
		for (Iterator it = getNodeEdgeIterator(node); it.hasNext();) {
			Edge edge = (Edge) it.next();
			assert hasNode(edge.getDistalNode(node));
			result.add(edge.getDistalNode(node));
		}
		return result;
	}

	public void union(Graph subgraph) {
		for (Iterator it = subgraph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node caused = edge.getCausedNode();
			Node cause = edge.getCausingNode();
			ensureNode(caused);
			ensureNode(cause);
			Edge myEdge = getEdge(cause, caused);
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
	public boolean contains(Graph subgraph) {
		if (subgraph.getNumEdges() > getNumEdges())
			return false;
		for (Iterator it = subgraph.getEdges().iterator(); it.hasNext();) {
			Edge subedge = (Edge) it.next();
			List nodes = subedge.getNodes();
			Node node1 = (Node) nodes.get(0);
			if (!hasNode(node1))
				return false;
			Node node2 = (Node) nodes.get(1);
			if (!hasNode(node2))
				return false;
			Edge edge = getEdge(node1, node2);
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
				for (Iterator it = getEdges().iterator(); it.hasNext();) {
					Edge edge = (Edge) it.next();
					for (Iterator nodeIt = edge.getNodes().iterator(); nodeIt
							.hasNext();) {
						Node caused = (Node) nodeIt.next();
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
	public int removeNonpathEdges(Node primary1, Node primary2) {
		Collection pathEdges = pathEdges(primary1, primary2, 0);
		return retainAllEdges(pathEdges);
	}

	private int retainAllEdges(Collection pathEdges) {
		int result = 0;
		for (Iterator it = (new ArrayList(getEdges())).iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
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
	public int removeNonpathEdges(Node[] primaryNodes) {
		Collection pathEdges = new HashSet();
		for (int i = 0; i < primaryNodes.length; i++) {
			Node primary1 = primaryNodes[i];
			for (int j = i + 1; j < primaryNodes.length; j++) {
				Node primary2 = primaryNodes[j];
				pathEdges.addAll(pathEdges(primary1, primary2, 0));
			}
		}
		//Util.print("removeNPE "+Util.valueOfDeep(primaryNodes)+" "+pathEdges);
		int result = retainAllEdges(pathEdges);
		Collection nodesToRemove = new LinkedList();
		for (Iterator it = getNodes().iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			if (!Util.isMember(primaryNodes, node)
					&& getEdges(node).size() == 0)
				nodesToRemove.add(node);
		}
		for (Iterator it = nodesToRemove.iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			removeNode(node);
		}
		return result;
	}

	public boolean allOnPath(Node primary1, Node primary2, double threahold) {
		return pathEdges(primary1, primary2, threahold).size() == getNumEdges();
	}

	/**
	 * downstream means in the direction of the arrow
	 * 
	 * @return all edges in graph1 that lie on a colliderless path between two
	 *         primary nodes
	 */
	private Collection pathEdges(Node primary1, Node primary2, double threshold) {
		Collection pathEdges = new HashSet();
		Collection nodeStack = new HashSet();
		nodeStack.add(primary1);
		pathEdgesInternal(pathEdges, primary2, nodeStack, primary1, false,
				threshold, 1);
		return pathEdges;
	}

	/**
	 * Depth-first sesarch, backtracking if we're in a loop, or meet a collider.
	 * If we reach a goal node, add the current path to pathEdges
	 */
	private boolean pathEdgesInternal(Collection pathEdges, Node goalNode,
			Collection nodeStack, Node node, boolean downstreamOnly,
			double threshold, double strength) {
		boolean result = false;
		for (Iterator it = getNodeEdgeIterator(node); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node adj = edge.getDistalNode(node);
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

	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			buf.append(edge).append("\n");
		}
		return buf.toString();
	}

}
