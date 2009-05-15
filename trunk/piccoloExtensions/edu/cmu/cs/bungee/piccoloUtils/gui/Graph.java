package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.image.codec.jpeg.ImageFormatException;

import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.cmu.cs.bungee.javaExtensions.graph.Node;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

public class Graph extends LazyPPath {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int ARROW_SIZE = 8;
	private static final double MARGIN = 6.0;
	Font font;
	edu.cmu.cs.bungee.javaExtensions.graph.Graph abstractGraph;
	private APText title;
	public Paint borderColor = Color.white;
	public Paint textColor = Color.white;
	public Paint bgColor = Color.black;
	public String label;
	// private FontRenderContext frc;
	private Map nodeObjectMap = new HashMap();

	public Graph(edu.cmu.cs.bungee.javaExtensions.graph.Graph abstractGraph,
			Font font) {
		// System.out.println("ff " + abstractGraph);
		assert abstractGraph != null;
		this.abstractGraph = abstractGraph;
		if (font == null)
			font = PText.DEFAULT_FONT;
		this.font = font;
		// this.frc = frc;
		if (abstractGraph.getNumNodes() > 0) {
			abstractGraph.layout();
		}
		draw();
		setWidth(0);
	}

	private StringAlign nEdgesFormat = new StringAlign(2,
			StringAlign.JUST_RIGHT);
	private DecimalFormat edgesFormat = new DecimalFormat("#");

	public void printMe(String status) {
		String baseName = "C:\\Documents and Settings\\mad\\Desktop\\Bungee\\Misc\\InfluenceDiagrams\\"
				+ abstractGraph.label
				+ " "
				+ status
				+ " "
				+ nEdgesFormat.format(abstractGraph.getNumDirectedEdges(),
						edgesFormat) + " ";
		File jpgFile = edu.cmu.cs.bungee.javaExtensions.Util.uniquifyFilename(
				baseName, ".jpg");
		printMe(jpgFile);
	}

	public void printMe(File file) {
		abstractGraph.layout();
		draw();

		if (getBounds().isEmpty())
			setWidth(0);
		try {
			edu.cmu.cs.bungee.piccoloUtils.gui.Util.savePNodeAsJPEG(this, file,
					72, 85);
		} catch (ImageFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printMe(
			edu.cmu.cs.bungee.javaExtensions.graph.Graph graph1, String status) {
		Graph pnode = new Graph(graph1, null);
		pnode.printMe(status);
	}

	public void redraw() {
		draw();
		// printMe("Final");
	}

	public void setLabel(String label) {
		this.label = label;
		if (title == null) {
			title = APText.oneLineLabel(font);
			addChild(title);
		}
		title.setTextPaint(textColor);
		title.setText(label);
		title.setOffset(Math.round(getX() + 2 * MARGIN), Math.round(getY() + 2
				* MARGIN));
	}

	private void draw() {
		removeAllChildren();
		// System.out.println("draw");
		setPaint(bgColor);
		setStrokePaint(borderColor);
		if (label != null) {
			setLabel(label);
		}
		Map nodeMap = new HashMap();
		for (Iterator it = abstractGraph.getNodes().iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			nodeMap.put(node, addPNode(node));
		}
		for (Iterator it = abstractGraph.getEdges().iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			List nodes = edge.getNodes();
			Node node1 = (Node) nodes.get(0);
			Node node2 = (Node) nodes.get(1);
			Point2D end1 = edge.getEndpoint(((APText) nodeMap.get(node1))
					.getFullBounds());
			Point2D end2 = edge.getEndpoint(((APText) nodeMap.get(node2))
					.getFullBounds());

			Arrow arrow = new Arrow(Color.white, ARROW_SIZE, 1);
			arrow.addLabels(edge.getLabels(), font);
			int type1 = edge.getEndpoint(node1).type;
			arrow.setVisible(Arrow.LEFT_TAIL, type1 == Edge.CIRCLE);
			int type2 = edge.getEndpoint(node2).type;
			arrow.setVisible(Arrow.RIGHT_TAIL, type2 == Edge.CIRCLE);
			arrow.setVisible(Arrow.LEFT_HEAD, type1 == Edge.ARROW);
			arrow.setVisible(Arrow.RIGHT_HEAD, type2 == Edge.ARROW);
			arrow.setEndpoints(end1.getX(), end1.getY(), end2.getX(), end2
					.getY());
			addChild(arrow);
		}
		// setWidth(0);
		// printMe("Fianl", graph1);
	}

	private APText addPNode(Node node) {
		APText pNode = APText.oneLineLabel(font);
		if (node.object != null)
			nodeObjectMap.put(node.object, pNode);
		pNode.setTextPaint(textColor);
		pNode.setText(Util.truncateText(node.getLabel(), 100, font));
		pNode.setOffset(Math.rint(node.getCenterX() - pNode.getWidth() / 2),
				Math.rint(node.getCenterY() - pNode.getHeight() / 2));
		// pNode.setPaint(Color.white);
		// pNode.setScale(0.9);
		// pNode.setTextPaint(art().facetTextColor(facet));
		addChild(pNode);
		return pNode;
	}

	public APText getNode(Object object) {
		return (APText) nodeObjectMap.get(object);
	}

	public Collection getNodeObjects() {
		return Collections.unmodifiableCollection(nodeObjectMap.keySet());
	}

	public boolean setWidth(double w) {
		double Y_MARGIN = 8;
		double outlineW = MARGIN;
		reset();
		PBounds pb = getFullBounds();
		double xMargin = Math.max(Y_MARGIN, (w - pb.getWidth()) / 2) - outlineW
				/ 2;
		double yMargin = Y_MARGIN;
		float x0 = (float) (pb.getX() - xMargin);
		float y0 = (float) (pb.getY() - yMargin);
		float x1 = (float) (x0 + pb.getWidth() + 2 * xMargin);
		float y1 = (float) (y0 + pb.getHeight() + 2 * yMargin);
		float[] xs = { x0, x1, x1, x0, x0 };
		float[] ys = { y0, y0, y1, y1, y0 };
		// setBounds(x0, y0, x1, y1);
		setStroke(LazyPPath.getStrokeInstance((int) outlineW));
		setPathToPolyline(xs, ys);
		if (title != null)
			title.setOffset(Math.round(x0 + 2 * outlineW), Math.round(y0 + 2
					* outlineW));
		return true;
	}

	public int getNumEdges() {
		return abstractGraph.getEdges().size();
	}

}
