package edu.cmu.cs.bungee.javaExtensions.graph;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.List;

public class Edge {

	private static final int NEITHER = 0;
	private static final int FORWARD = 1;
	private static final int BACKWARD = 2;
	private static final int BIDIRECTIONAL = FORWARD | BACKWARD;
	private String label;
	private int orientation = BIDIRECTIONAL;
	private final Node node1;
	private final Node node2;

	// private int x1, y1, x2, y2;

	Edge(String label, Node node1, Node node2) {
		super();
		this.label = label;
		this.node1 = node1;
		this.node2 = node2;
	}

	boolean hasNode(Node node) {
		return node == node1 || node == node2;
	}

	private int getMask(Node caused) {
		assert hasNode(caused);
		return caused == node1 ? 1 : 2;
	}

	boolean isNull() {
		return orientation == NEITHER;
	}

	public void setBidirectional() {
		orientation = BIDIRECTIONAL;
	}

	public void removeDirection(Node caused) {
		// if (edge.getNode2().getName().equals("p2772")
		// && edge.getNode1().getName().equals(
		// "lithograph"))
		// return false;

		// String name = getDistalNode(caused) + "--" + caused;
		// // Util.print("Removing weak edge " + name);
		// printMe(name);

		assert hasNode(caused);
		assert canCause(caused);
		orientation &= ~getMask(caused);
	}

	public void addDirection(Node caused) {
		assert hasNode(caused);
		orientation |= getMask(caused);
	}

	public void setDirection(Node caused) {
		assert hasNode(caused);
		orientation = getMask(caused);
	}

	public List getNodes() {
		List nodes = new ArrayList(2);
		nodes.add(node1);
		nodes.add(node2);
		return nodes;
	}

	public Node getDistalNode(Node proximalNode) {
		assert proximalNode == node1 || proximalNode == node2;
		return proximalNode == node1 ? node2 : node1;
	}

	public boolean canCause(Node caused) {
		return (orientation & getMask(caused)) > 0;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getNumDirections() {
		switch (orientation) {
		case NEITHER:
			return 0;
		case FORWARD:
		case BACKWARD:
			return 1;
		case BIDIRECTIONAL:
			return 2;
		default:
			assert false;
			return -1;
		}
	}

	// public void setEndpoint(int x, int y, Node node) {
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
	// public int getX(Node node) {
	// assert hasNode(node);
	// return node == node1 ? x1 : x2;
	// }
	//
	// public int getY(Node node) {
	// assert hasNode(node);
	// return node == node1 ? y1 : y2;
	// }

	public Point2D getEndpoint(Node node, Font font, FontRenderContext frc) {
		assert hasNode(node);
		Rectangle2D rect = node.getRectangle(font, frc);
		return getEndpoint(rect);
	}

	public Point2D getEndpoint(Rectangle2D rect) {
		Line2D centerToCenterLine = getCenterToCenterLine();
		assert rect.contains(centerToCenterLine.getP1()) != rect
				.contains(centerToCenterLine.getP2()) : rect + " "
				+ centerToCenterLine.getP1()+ " "
				+ centerToCenterLine.getP2();
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
//		Util.print("dd " + rect + " " + result);
		assert result.getX() >= x1 : result.getX() + " " + x1;
		assert result.getX() <= x2 : result.getX() + " " + x2;
		assert result.getY() >= y1 : result.getY() + " " + y1;
		assert result.getY() <= y2 : result.getY() + " " + y2;
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
	Point2D getIntersection(Edge edge2) {
		return getIntersection(getCenterToCenterLine(), edge2
				.getCenterToCenterLine());
	}

	private Double getCenterToCenterLine() {
		Node node11 = node1;
		Node node12 = node2;
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
		} else if (between(numeratorA, 0, denom)
				&& between(numeratorB, 0, denom)) {
			double ua = numeratorA / denom;
			result = new Point2D.Double(x11 + ua * (x12 - x11), y11 + ua
					* (y12 - y11));
		}
		return result;
	}

	private boolean between(double x, double x1, double x2) {
		return x > Math.min(x1, x2) && x < Math.max(x1, x2);
	}

	public boolean isArrowhead(Node node) {
		return canCause(node) && !canCause(getDistalNode(node));
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Node caused = isArrowhead(node1) ? node1 : node2;
		String connector = isArrowhead(caused) ? " --> " : " --- ";
		buf.append(getDistalNode(caused).getLabel()).append(connector).append(
				caused.getLabel());
		return buf.toString();
	}

	public Node getCausedNode() {
		if (isArrowhead(node1))
			return node1;
		else if (isArrowhead(node2))
			return node2;
		assert false;
		return null;
	}

	public Node getCausingNode() {
		if (isArrowhead(node1))
			return node2;
		else if (isArrowhead(node2))
			return node1;
		assert false;
		return null;
	}

}
