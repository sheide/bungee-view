package edu.cmu.cs.bungee.javaExtensions.graph;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Edge<NodeObjectType extends Comparable<NodeObjectType>> {

	// private static final int NEITHER = 0;
	// private static final int FORWARD = 1;
	// private static final int BACKWARD = 2;
	// private static final int BIDIRECTIONAL = FORWARD | BACKWARD;

	// left is nodes[0]; right is nodes[1]
	public static final int LEFT_LABEL = 0;
	public static final int CENTER_LABEL = 1;
	public static final int RIGHT_LABEL = 2;

	private String labels[];
	// private int orientation = BIDIRECTIONAL;
	// private final Node<NodeObjectType> node1;
	// private final Node<NodeObjectType> node2;
	private final Endpoint endpoint1;
	private final Endpoint endpoint2;
	private final List<Node<NodeObjectType>> nodes;

	// private int x1, y1, x2, y2;

	Edge(String[] labels, Node<NodeObjectType> node1, Node<NodeObjectType> node2) {
		super();
		this.labels = labels;
		this.endpoint1 = new Endpoint(node1);
		this.endpoint2 = new Endpoint(node2);
		List<Node<NodeObjectType>> nodes1 = new ArrayList<Node<NodeObjectType>>(2);
		nodes1.add(node1);
		nodes1.add(node2);
		nodes = Collections.unmodifiableList(nodes1);
	}

	 @SuppressWarnings("unchecked")
	Node<NodeObjectType> getNode1() {
		return (Node<NodeObjectType>) endpoint1.node;
	}

	 @SuppressWarnings("unchecked")
	Node<NodeObjectType> getNode2() {
		return (Node<NodeObjectType>) endpoint2.node;
	}

	boolean hasNode(Node<NodeObjectType> node) {
		return node == getNode1() || node == getNode2();
	}

	// private int getMask(Node<NodeObjectType> caused) {
	// assert hasNode(caused);
	// return caused == getNode1() ? 1 : 2;
	// }

	// boolean isNull() {
	// return orientation == NEITHER;
	// }

	public void setBidirectional() {
		endpoint1.type = ARROW;
		endpoint2.type = ARROW;
	}

	public void removeDirection(Node<NodeObjectType> caused) {
		// if (edge.getNode2().getName().equals("p2772")
		// && edge.getNode1().getName().equals(
		// "lithograph"))
		// return false;

		// String name = getDistalNode(caused) + "--" + caused;
		// // Util.print("Removing weak edge " + name);
		// printMe(name);

		assert hasNode(caused);
		assert canCause(caused);
		getEndpoint(caused).type = NONE;
		// orientation &= ~getMask(caused);
	}

	public Endpoint getEndpoint(Node<NodeObjectType> caused) {
		assert hasNode(caused);
		return caused == getNode1() ? endpoint1 : endpoint2;
	}

	public void addDirection(Node<NodeObjectType> caused) {
		assert hasNode(caused);
		// if (!canCause(caused))
		// System.out.println("addDirection "+this);
		getEndpoint(caused).type = ARROW;
		// orientation |= getMask(caused);
	}

	public void setDirection(Node<NodeObjectType> caused) {
		assert hasNode(caused);
		getEndpoint(caused).type = ARROW;
		getEndpoint(getDistalNode(caused)).type = NONE;
		// orientation = getMask(caused);
		// System.out.println("setDirection "+this);
	}

	public List<Node<NodeObjectType>> getNodes() {
		return nodes;
	}

	public Node<NodeObjectType> getDistalNode(Node<NodeObjectType> proximalNode) {
		assert proximalNode == getNode1() || proximalNode == getNode2();
		return proximalNode == getNode1() ? getNode2() : getNode1();
	}

	public boolean canCause(Node<NodeObjectType> caused) {
		return getEndpoint(caused).type == ARROW;
		// return (orientation & getMask(caused)) > 0;
	}

	public String getLabel(int position) {
		return labels[position];
	}

	public void setLabel(String label, int position) {
		// assert label.indexOf('@') == -1;
		this.labels[position] = label;
	}

	public void setLabel(String label, Node<NodeObjectType> node) {
		this.labels[getPosition(node)] = label;
	}

	private int getPosition(Node<NodeObjectType> node) {
		if (node == getNode1())
			return LEFT_LABEL;
		else if (node == getNode2())
			return RIGHT_LABEL;
		assert false;
		return -1;
	}

	public String[] getLabels() {
		return labels;
	}

	public int getNumDirections() {
		return (canCause(getNode1()) ? 1 : 0) + (canCause(getNode2()) ? 1 : 0);
		// switch (orientation) {
		// case NEITHER:
		// return 0;
		// case FORWARD:
		// case BACKWARD:
		// return 1;
		// case BIDIRECTIONAL:
		// return 2;
		// default:
		// assert false;
		// return -1;
		// }
	}

	// public void setEndpoint(int x, int y, Node<NodeObjectType> node) {
	// assert hasNode(node);
	// if (node == node1) {
	// x1 = x;
	// y1 = y;
	// } else {
	// x2 = x;
	// y2 = y;
	// }
	// }
	//
	// public int getX(Node<NodeObjectType> node) {
	// assert hasNode(node);
	// return node == node1 ? x1 : x2;
	// }
	//
	// public int getY(Node<NodeObjectType> node) {
	// assert hasNode(node);
	// return node == node1 ? y1 : y2;
	// }

	public Point2D getEndpoint(Node<NodeObjectType> node, Font font, FontRenderContext frc) {
		assert hasNode(node);
		Rectangle2D rect = node.getRectangle(font, frc);
		return getEndpoint(rect);
	}

	public Point2D getEndpoint(Rectangle2D rect) {
		Line2D centerToCenterLine = getCenterToCenterLine();
		assert rect.contains(centerToCenterLine.getP1()) != rect
				.contains(centerToCenterLine.getP2()) : rect + " "
				+ centerToCenterLine.getP1() + " " + centerToCenterLine.getP2();
		double x1 = rect.getX();
		double y1 = rect.getY();
		double x2 = x1 + rect.getWidth();
		double y2 = y1 + rect.getHeight();
		Point2D result = getIntersection(centerToCenterLine, new Line2D.Double(
				x1, y1, x2, y1));
		if (result == null)
			result = getIntersection(centerToCenterLine, new Line2D.Double(x2,
					y1, x2, y2));
		if (result == null)
			result = getIntersection(centerToCenterLine, new Line2D.Double(x1,
					y2, x2, y2));
		if (result == null)
			result = getIntersection(centerToCenterLine, new Line2D.Double(x1,
					y1, x1, y2));
		assert result != null : rect + " (" + centerToCenterLine.getX1() + ", "
				+ centerToCenterLine.getY1() + ") ("
				+ centerToCenterLine.getX2() + ", "
				+ centerToCenterLine.getY2() + ")";
		// Util.print("dd " + rect + " " + result);
		assert result.getX() >= x1 - 0.000001 : result.getX() + " " + x1;
		assert result.getX() <= x2 + 0.000001 : result.getX() + " " + x2;
		assert result.getY() >= y1 - 0.000001 : result.getY() + " " + y1;
		assert result.getY() <= y2 + 0.000001 : result.getY() + " " + y2;
		return result;
	}

	/**
	 * @param edge2
	 * @return whether the edges cross
	 * 
	 *         see <a
	 *         href="http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/"
	 *         >Intersection point of two lines< /a>
	 */
	Point2D getIntersection(Edge<NodeObjectType> edge2) {
		// if (getNodes().contains(edge2.getNode1())
		// || getNodes().contains(edge2.getNode2()))
		// return null;
		if (minX() >= edge2.maxX() || maxX() <= edge2.minX()
				|| minY() >= edge2.maxY() || maxY() <= edge2.minY())
			return null;
		return getIntersection(getCenterToCenterLine(), edge2
				.getCenterToCenterLine());
	}

	int maxX() {
		return Math.max(getNode1().getCenterX(), getNode2().getCenterX());
	}

	int minX() {
		return Math.min(getNode1().getCenterX(), getNode2().getCenterX());
	}

	int maxY() {
		return Math.max(getNode1().getCenterY(), getNode2().getCenterY());
	}

	int minY() {
		return Math.min(getNode1().getCenterY(), getNode2().getCenterY());
	}

	private Double getCenterToCenterLine() {
		Node<NodeObjectType> node11 = getNode1();
		Node<NodeObjectType> node12 = getNode2();
		int x11 = node11.getCenterX();
		int y11 = node11.getCenterY();
		int x12 = node12.getCenterX();
		int y12 = node12.getCenterY();
		// Util.print("Gcc "+x11+" "+y11+" "+x12+" "+y12+" ");
		return new Line2D.Double(x11, y11, x12, y12);
	}

	private Point2D getIntersection(Line2D edge1, Line2D edge2) {
		// Find intersection of the lines determined by each edge's line segment
		double x11 = edge1.getX1();
		double y11 = edge1.getY1();
		double x12 = edge1.getX2();
		double y12 = edge1.getY2();
		double x21 = edge2.getX1();
		double y21 = edge2.getY1();
		double x22 = edge2.getX2();
		double y22 = edge2.getY2();

		double denom = (y22 - y21) * (x12 - x11) - (x22 - x21) * (y12 - y11);
		double numeratorA = (x22 - x21) * (y11 - y21) - (y22 - y21)
				* (x11 - x21);
		double numeratorB = (x12 - x11) * (y11 - y21) - (y12 - y11)
				* (x11 - x21);
		Point2D.Double result = null;
		if (denom == 0) {
			if (numeratorA == 0) {
				// lines are coincident
				assert numeratorB == 0;
				if (x21 == x22) {
					if (between(y11, y21, y12)) {
						result = new Point2D.Double(x21, y21);
					} else if (between(y11, y22, y12)) {
						result = new Point2D.Double(x22, y22);
					}
				} else {
					if (between(x11, x21, x12)) {
						result = new Point2D.Double(x21, y21);
					} else if (between(x11, x22, x12)) {
						result = new Point2D.Double(x22, y22);
					}
				}
			} else {
				// lines are parallel. result remains null.
			}
		} else {
			double ua = numeratorA / denom;
			if (ua > 0 && ua < 1) {
				double ub = numeratorB / denom;
				if (ub > 0 && ub < 1) {
					result = new Point2D.Double(x11 + ua * (x12 - x11), y11
							+ ua * (y12 - y11));
				}
			}

			// } else if (between(numeratorA, 0, denom)
			// && between(numeratorB, 0, denom)) {
			// double ua = numeratorA / denom;
			// result = new Point2D.Double(x11 + ua * (x12 - x11), y11 + ua
			// * (y12 - y11));
		}
		return result;
	}

	private boolean between(double x, double x1, double x2) {
		return x > Math.min(x1, x2) && x < Math.max(x1, x2);
	}

	public boolean isArrowhead(Node<NodeObjectType> node) {
		return canCause(node) && !canCause(getDistalNode(node));
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		String connector = " " + endpoint1.symbol("<") + "-"
				+ endpoint2.symbol(">") + " ";

		Node<NodeObjectType> caused = isArrowhead(getNode1()) ? getNode1() : getNode2();
		// String connector = isArrowhead(caused) ? " --> " : " --- ";

		buf.append(getDistalNode(caused).getLabel()).append(connector).append(
				caused.getLabel());
		return buf.toString();
	}

	public Node<NodeObjectType> getCausedNode() {
		if (isArrowhead(getNode1()))
			return getNode1();
		else if (isArrowhead(getNode2()))
			return getNode2();
		assert false;
		return null;
	}

	public Node<NodeObjectType> getCausingNode() {
		if (isArrowhead(getNode1()))
			return getNode2();
		else if (isArrowhead(getNode2()))
			return getNode1();
		assert false;
		return null;
	}

	public final static int ARROW = 1;
	public final static int CIRCLE = 2;
	public final static int NONE = 3;

	public static class Endpoint {
		public int type;
		final Node<?> node;

		Endpoint(Node<?> node) {
			this.type = NONE;
			this.node = node;
		}

		Endpoint(int type, Node<?> node) {
			assert type == ARROW || type == CIRCLE || type == NONE;
			this.type = type;
			this.node = node;
		}

		String symbol(String arrowSymbol) {
			switch (type) {
			case ARROW:
				return arrowSymbol;
			case CIRCLE:
				return "o";
			case NONE:
				return "-";

			default:
				assert false;
				return "?";
			}
		}
	}

}
