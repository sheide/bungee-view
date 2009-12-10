package edu.cmu.cs.bungee.client.viz;

import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;

import edu.cmu.cs.bungee.client.query.DisplayTree;
import edu.cmu.cs.bungee.client.query.FacetTree;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.threads.UpdateNoArgsThread;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;

final class ClusterViz extends LazyPNode implements MouseDoc,
		PickFacetTextNotifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final static int nClusters = 50;

	/**
	 * Space to the left and right of the facetTreeViz
	 */
	private static final double margin = 5;

	private final APText label;

	// private double labelH = 0;

	final Bungee art;

	final FacetTreeViz facetTreeViz;

	SortedSet<Perspective> exclude;

	// private DisplayTree excludeTree;

	// private DisplayTree stayTunedTree;

	private Clusterer clusterer;

	ClusterViz(Bungee _art) {
		art = _art;
		setPickable(false);
		setPaint(Bungee.selectedItemBG);

		label = art.oneLineLabel();
		label.scale(2.0);
		label.setTextPaint(Bungee.selectedItemFG);
		label.setPickable(false);
		double labelH = 2 * art.lineH;
		label.setText("Tag Clusters");
		addChild(label);

		facetTreeViz = new FacetTreeViz(art);
		facetTreeViz.dontHideTransients = true;
		facetTreeViz.pickFacetTextNotifier = this;
		facetTreeViz.showCheckBox = false;
		// facetTree.setOffset(leftMargin, 0);
		addChild(facetTreeViz);
		facetTreeViz.setOffset(margin, labelH + 10.0);

		// stayTunedTree = new FacetTree(null, new Perspective[0], "Finding
		// clusters...");
	}

	Clusterer ensureClusterer() {
		if (clusterer == null) {
			clusterer = new Clusterer();
			clusterer.start();
		}
		return clusterer;
	}

	void validate() {
		SelectedItem selectedItem = art.selectedItem;
		double _w = selectedItem.getWidth();
		double _h = selectedItem.getHeight();
		setOffset(selectedItem.getOffset());
		// Util.print("PerspectiveList.validate " + w + " " + h);
		setBounds(0, 0, _w, _h);
		label.setFont(art.font);
		// label.setHeight(art.lineH);
		label.setOffset(Math
				.round((_w - label.getScale() * label.getWidth()) / 2.0), 0.0);
		facetTreeViz.validate(_w - 2 * margin, _h - facetTreeViz.getYOffset());
		facetTreeViz.redraw();
	}

	void showClusters() {
		// Util.print("showClusters");
		// exclude = art.query.restrictions(true);
		validate();
		facetTreeViz.setTree(createTree());
		exclude = query().allRestrictions(true);
		update();
	}

	DisplayTree createTree() {
		DisplayTree tree = new DisplayTree(null, null, "ClusterVizTree");
		new FacetTree(tree, query().allRestrictions(true), "Tags to ignore");
		return tree;
	}

	void update() {
		if (ensureClusterer().update()) {
			// art().handleCursor(true);
		}
		redraw();
	}

	// SortedSet exclude() {
	// return art.query.allRestrictions(true);
	// }

	void redraw() {
		DisplayTree displayTree = facetTreeViz.tree;
		// if (excludeTree != null) {
		// displayTree.removeChild(excludeTree);
		// excludeTree = null;
		// }

		// Replace exclude tree
		displayTree.removeChild(0);
		// SortedSet exclude = exclude();
		FacetTree ignoreTree = new FacetTree(displayTree, exclude,
				"Tags to ignore");
		displayTree.removeChild(ignoreTree);
		ListIterator<DisplayTree> it = displayTree.childIterator();
		it.add(ignoreTree);
		while (it.hasNext()) {
			DisplayTree subtree = it.next();
			// Util.print("delete? " + exclude + " " + subtree.isMember(exclude)
			// + " " + subtree);
			if (subtree.isMember(exclude)) {
				it.remove();
			}
		}
		if (isStayTunedTree(it.previous()))
			it.remove();
		if (!ensureClusterer().isIdle()) {
			String stayTunedLabel = (displayTree.nChildren() > 1) ? "finding more clusters..."
					: "finding clusters...";
			new DisplayTree(displayTree, stayTunedLabel, null);
		}
		facetTreeViz.redraw();
		moveToFront();
	}

	boolean isStayTunedTree(DisplayTree displayTree) {
		return "finding more clusters...".equals(displayTree.treeObject())
				|| "finding clusters...".equals(displayTree.treeObject());
	}

	<V extends ItemPredicate>void highlightFacet(Set<V> highlightFacets) {
		if (facetTreeViz != null)
			facetTreeViz.highlightFacet(highlightFacets);
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// art.setMouseDoc(source, state);
	// }

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}

	// void setMouseDoc(Markup doc, boolean state) {
	// art.setMouseDoc(doc, state);
	// }

	void hide() {
		removeFromParent();
		art.selectedItem.setVisibility();
	}

	boolean isHidden() {
		return getParent() == null;
	}

	public boolean pick(FacetText node, int modifiers) {
		if (!Util.isAnyShiftKeyDown(modifiers)) {
			ItemPredicate facet = node.facet;
			if (facet != null) {
				toggleClusterExclusion((Perspective) facet);
			} else if (node.cluster != null) {
				art.toggleCluster(node.cluster);
			} else
				return false;
			return true;
		}
		return false;
	}

	void toggleClusterExclusion(Perspective facet) {
		art().printUserAction(Bungee.TOGGLE_CLUSTER_EXCLUSION, facet, 0);
		// SortedSet exclude = exclude();
		if (exclude.contains(facet))
			exclude.remove(facet);
		else
			exclude.add(facet);
		update();
	}

	public boolean highlight(FacetText node, boolean state, int modifiers) {
		// Util.print("ClusterViz.highlight " + node + " " + state + " "
		// + modifiers);
		if (!Util.isAnyShiftKeyDown(modifiers)) {
			Perspective facet = node.facet;
			if (facet != null) {
				art.highlightFacet(state ? facet : null, modifiers);
				Markup doc = Query.emptyMarkup();
				boolean excluded = exclude.contains(facet);
				if (excluded) {
					doc.add("Unexclude ");
				} else {
					doc.add("Exclude ");
				}
				doc.add(facet);
				doc.add(" from clusters");
				art.setClickDesc(doc);
			} else if (node.cluster != null)
				art.highlightCluster(state ? node.cluster : null);
			else
				return false;
			return true;
		} else
			return false;
	}

	// public boolean mouseMoved(FacetText node, int modifiers) {
	// if (modifiers == 0) {
	// Perspective facet = node.facet;
	// if (facet != null) {
	// Markup doc = Query.emptyMarkup();
	// boolean excluded = exclude.contains(facet);
	// if (excluded) {
	// doc.add("Unexclude ");
	// } else {
	// doc.add("Exclude ");
	// }
	// doc.add(facet);
	// doc.add(" from clusters");
	// art.setClickDesc(doc);
	// return true;
	// }
	// }
	// return false;
	// }

	Bungee art() {
		return art;
	}

	Query query() {
		return art.query;
	}

	void stop() {
		if (clusterer != null) {
			clusterer.exit();
			clusterer = null;
		}
	}

	final class Clusterer extends UpdateNoArgsThread {

		Clusterer() {
			super("Clusterer", 0);
		}

		@Override
		public void process() {
			String facetRestrictions = null;
			// SortedSet exclude = exclude();
			if (exclude.size() > 0) {
				StringBuffer buf = new StringBuffer(
						"LEFT JOIN ancestor a ON f.facet_id = a.facet_id AND a.ancestor_id IN (");
				for (Perspective p: exclude) {
					if (p != exclude.first())
						buf.append(", ");
					buf.append(p.getServerID());
				}
				buf.append(") WHERE a.facet_id IS NULL");
				facetRestrictions = buf.toString();
			}
			final DisplayTree tree = createTree();
			query().clusterTree(nClusters, facetRestrictions, art.pValue(),
					tree);

			Runnable doRedraw = new Runnable() {

				public void run() {
					facetTreeViz.setTree(tree);
					redraw();
					// art().handleCursor(false);
				}
			};

			javax.swing.SwingUtilities.invokeLater(doRedraw);
		}
	}
}
