package edu.cmu.cs.bungee.javaExtensions.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.PermutationIterator;
import edu.cmu.cs.bungee.javaExtensions.Util;

public class Graph {
	private static final int GRAPH_EDGE_LENGTH = 150;
	private final Map edgesLookupTable;
	private final Set edges;
	private final Map nodesTable;
	private final Map namesToNodesTable;
	private int nodeIndex;
	public String label = "Graph";

	public Graph() {
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
		// Util.print("hh "+nodesTable);
	}

	private boolean checkConsistency() {
		for (Iterator it = getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			assert edge.getNumDirections() > 0;
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

	public Node getNode(String name) {
		return (Node) namesToNodesTable.get(name);
	}

	public Node addNode(String name) {
		assert getNode(name) == null;
		Node node = new Node(name);
		ensureNode(node);
		return node;
	}

	private void ensureNode(Node node) {
		if (!hasNode(node)) {
			nodesTable.put(node, new Integer(nodeIndex++));
			namesToNodesTable.put(node.getLabel(), node);
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
		namesToNodesTable.remove(node.getLabel());
		assert checkConsistency();
	}

	public void removeNode(String label) {
		removeNode(getNode(label));
	}

	public Edge getEdge(Node node1, Node node2) {
		assert hasNode(node1) : node1 + " " + this;
		assert hasNode(node2) : node2 + " " + this;
		Set nodes = new HashSet(2);
		nodes.add(node1);
		nodes.add(node2);
		return (Edge) edgesLookupTable.get(nodes);
	}

	public Edge addEdge(String label, Node node1, Node node2) {
		assert hasNode(node1) && hasNode(node2);
		assert getEdge(node1, node2) == null;
		Edge edge = new Edge(label, node1, node2);
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

	public Set getEdges() {
		return Collections.unmodifiableSet(edges);
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
		for (Iterator it = getEdges(node).iterator(); it.hasNext();) {
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
		for (Iterator it = getEdges(node).iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			assert hasNode(edge.getDistalNode(node));
			result.add(edge.getDistalNode(node));
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

	public void union(Graph subgraph) {
		for (Iterator it = subgraph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node caused = edge.getCausedNode();
			Node cause = edge.getCausingNode();
			ensureNode(caused);
			ensureNode(cause);
			Edge oldEdge = getEdge(cause, caused);
			if (oldEdge == null) {
				addEdge(edge.getLabel(), cause, caused).setDirection(caused);
			} else
				oldEdge.addDirection(caused);

		}

	}

}
