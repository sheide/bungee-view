package edu.cmu.cs.bungee.client.viz;

import java.sql.ResultSet;
import java.util.ListIterator;

import edu.cmu.cs.bungee.client.query.DisplayTree;
import edu.cmu.cs.bungee.client.query.FacetTree;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolo.PNode;


public class ClusterViz extends LazyPNode implements MouseDoc,
		PickFacetTextNotifier {

	private static final long serialVersionUID = -445422230767837264L;

	final static int nClusters = 50;

	private static double margin = 5;

	private final APText label;

	private double labelH = 0;

	final Bungee art;

	private FacetTreeViz facetTree;

//	private Perspective[] exclude;

//	private DisplayTree excludeTree;

//	private DisplayTree stayTunedTree;

	private Clusterer clusterer;

	ClusterViz(Bungee _art) {
		art = _art;
		setPickable(false);
		setPaint(Bungee.selectedItemBG);

		label = art.oneLineLabel();
		label.scale(2.0);
		label.setTextPaint(Bungee.selectedItemFG);
		label.setPickable(false);
		labelH = 2 * art.lineH;
		label.setText("Clusters");
		addChild(label);

		facetTree = new FacetTreeViz(art);
		facetTree.dontHideTransients = true;
		facetTree.pickFacetTextNotifier = this;
		facetTree.showCheckBox = false;
		// facetTree.setOffset(leftMargin, 0);
		addChild(facetTree);
		facetTree.setOffset(margin, labelH + 10.0);

//		stayTunedTree = new FacetTree(null, new Perspective[0], "Finding clusters...");
	}
	
	Clusterer ensureClusterer() {
		if (clusterer == null) {
			clusterer = new Clusterer();
			clusterer.start();			
		}
		return clusterer;
	}

	void validate(double _w, double _h) {
		// Util.print("PerspectiveList.validate " + w + " " + h);
		setBounds(0, 0, _w, _h);
		label.setFont(art.font);
		label.setHeight(art.lineH);
		label.setOffset(Math
				.round((_w - label.getScale() * label.getWidth()) / 2.0), 0.0);
		facetTree.validate(_w - 2 * margin, _h - facetTree.getYOffset());
		facetTree.redraw();
	}

	// void showClusters() {
	// moveToFront();
	// FacetTree tree = new FacetTree();
	// Perspective[] associates = art.query.associates();
	// // Util.print(associates.length + " Associates:");
	// // PrintArray.printArray(associates);
	// Perspective[] previous = null;
	// for (int i = 0; i < associates.length; i++) {
	// if (!Util.isMember(previous, associates[i])) {
	// FacetTree child = new FacetTree(associates[i], art);
	// if (child.cluster != null) {
	// tree.children.add(child);
	// previous = (Perspective[]) Util.append(previous,
	// child.cluster.facets, Perspective.class);
	// }
	// }
	// }
	// facetTree.setTree(tree);
	// facetTree.redraw();
	// }

	void showClusters() {
//		exclude = art.query.restrictions(true);
		createTree(null);
		update();
	}

	void createTree(ResultSet[] rs) {
		DisplayTree tree = new DisplayTree(null, "ClusterVizTree");
		new FacetTree(tree, art.query.restrictions(true), "Values to ignore");
		if (rs != null) {
			for (int i = 0; i < rs.length; i++) {
//				Util.print("");
				new FacetTree(tree, rs[i], art.query);
			}
		}
		facetTree.setTree(tree);
	}

	void update() {
		if (ensureClusterer().update()) {
//			art().handleCursor(true);			
		}
		redraw();
	}
	
	Perspective[] exclude() {
		return art.query.restrictions(true);
	}

	void redraw() {
		DisplayTree displayTree = facetTree.tree;
//		if (excludeTree != null) {
//			displayTree.removeChild(excludeTree);
//			excludeTree = null;
//		}
		Perspective[] exclude = exclude();
		ListIterator it = displayTree.childIterator();
		
		// Replace exclude tree
		it.next();
		it.set(new FacetTree(displayTree, exclude, "Values to ignore"));
		
			while (it.hasNext()) {
				DisplayTree subtree = (DisplayTree) it.next();
				if (subtree.isMember(exclude))
					it.remove();
			}	
			if (isStayTunedTree((DisplayTree) it.previous()))
				it.remove();
		if (!ensureClusterer().isIdle()) {
			String stayTunedLabel = (displayTree.nChildren() > 1) ? "finding more clusters..." : "finding clusters...";
			new DisplayTree(displayTree, "Stay Tuned", stayTunedLabel);
		}
		facetTree.redraw();
		moveToFront();
	}
	
	boolean isStayTunedTree(DisplayTree displayTree) {
		return "Stay Tuned".equals(displayTree.treeObject());
	}

	void showFacet(Object highlightFacet) {
		if (facetTree != null)
			facetTree.showFacet(highlightFacet);
	}

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	void hide() {
		removeFromParent();
	}

	boolean isHidden() {
		return getParent() == null;
	}

	public boolean pick(FacetText node, int modifiers) {
		if (modifiers == 0) {
			ItemPredicate facet = node.facet;
			if (facet != null) {
				Perspective[] exclude = exclude();
				if (Util.isMember(exclude, facet))
					exclude = (Perspective[]) Util.delete(exclude, facet,
							Perspective.class);
				else
					exclude = (Perspective[]) Util.push(exclude, facet,
							Perspective.class);
				update();
			} else if (node.cluster != null) {
				art.toggleCluster(node.cluster);
			} else
				return false;
			return true;
		}
		return false;
	}

	public boolean highlight(FacetText node, boolean state, int modifiers) {
//		Util.print("ClusterViz.highlight " + node + " " + state + " "
//				+ modifiers);
		if (modifiers == 0) {
			Perspective facet = node.facet;
			if (facet != null) {
				art.highlightFacet(facet, state, modifiers);
				Markup doc = Query.emptyMarkup();
				boolean excluded = Util.isMember(exclude(), facet);
				if (excluded) {
					doc.add("Unexclude ");
				} else {
					doc.add("Exclude ");
				}
				doc.add(facet);
				doc.add(" from clusters");
				art.setClickDesc(doc);
			} else if (node.cluster != null)
				art.highlightCluster(node.cluster, state);
			else
				return false;
			return true;
		} else
			return false;
	}

	public boolean mouseMoved(FacetText node, int modifiers) {
		if (modifiers == 0) {
			Perspective facet = node.facet;
			if (facet != null) {
				Markup doc = Query.emptyMarkup();
				boolean excluded = Util.isMember(exclude(), facet);
				if (excluded) {
					doc.add("Unexclude ");
				} else {
					doc.add("Exclude ");
				}
				doc.add(facet);
				doc.add(" from clusters");
				art.setClickDesc(doc);
				return true;
			}
		}
		return false;
	}
	
	Bungee art() {
		return art;
	}

	class Clusterer extends UpdateNoArgsThread {

//		ResultSet[] rs;

		public Clusterer() {
			super("Clusterer", 0);
		}

		public void process() {
			String facetRestrictions = null;
			Perspective[] exclude = exclude();
			if (exclude.length > 0) {
				StringBuffer buf = new StringBuffer(
						"LEFT JOIN ancestor a ON f.facet_id = a.facet_id AND a.ancestor_id IN (");
				for (int i = 0; i < exclude.length; i++) {
					buf.append(exclude[i].getID());
					if (i < exclude.length - 1)
						buf.append(", ");
				}
				buf.append(") WHERE a.facet_id IS NULL");
				facetRestrictions = buf.toString();
			}
			final ResultSet[] rs = art.query
					.clusterRS(nClusters, facetRestrictions, art.pValue());


			Runnable doRedraw = new Runnable() {

				public void run() {
					createTree(rs);
					redraw();
//					art().handleCursor(false);
				}
			};
			
			javax.swing.SwingUtilities.invokeLater(doRedraw);
		}
	}
}
