package edu.cmu.cs.bungee.client.viz;

import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags;
import edu.cmu.cs.bungee.client.query.Perspective.TopTags.TagRelevance;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolo.PNode;

public class ExtremeTags extends LazyContainer implements MouseDoc,
		PerspectiveObserver {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final double COLUMN_MARGIN = 10;

	Bungee art;

	APText label;

	private Boundary boundary;

	ExtremeTags(Bungee a) {
		super();
		art = a;

		label = art.oneLineLabel();
		label.setScale(2.0);
		// label.setConstrainWidthToTextWidth(false);
		label.setTextPaint(Bungee.summaryFG);
		label.setPickable(false);
		label.setText("Top Tags");

		boundary = new Boundary(this, false);
		addChild(boundary);

		setPickable(false);
	}

	boolean setVisibility() {
		boolean hide = art.query.getOnCount() == 0 || !art.query.isRestricted();
		// Util.print("Selected.hide " + state + " " + (state != isHidden()));
		if (hide != isHidden()) {
			if (hide)
				removeAllChildren();
			else
				addChild(label);
		}
		return !isHidden();
	}

	private boolean isHidden() {
		return label.getParent() == null;
	}

	// Since we're lying about onCount, can't rely on default FacetText redraw.
	public void redraw() {
		updateData();
	}

	void updateData() {
		if (setVisibility()) {
			removeAllChildren();
			addChild(label);
			addChild(boundary);
			double y = getTopMargin();
			int nLines = (int) ((getHeight() - y - art.lineH) / art.lineH / 2);
			TopTags topTags = art.query.topTags(nLines);
			y = updateDataInternal(y, topTags.topIterator());
			y += art.lineH;
			y = updateDataInternal(y, topTags.bottom.iterator());
		}
	}

	double maxRelevance(Perspective facet) {
		// return Math.sqrt(art.query.getTotalCount());
		return Math.sqrt(facet.parentTotalCount());
	}

	private double updateDataInternal(double y, Iterator<TagRelevance> it) {
		double numW = art.numWidth(-100);
		double nameW = getWidth() - numW - margin_size() - COLUMN_MARGIN;
		// double maxRelevance = maxRelevance();
		while (it.hasNext()) {
			TagRelevance tag = it.next();
			Perspective facet = (Perspective) tag.tag.object;
//			double relevance = tag.relevance;
//			Util.print("udi "+y+"\t"+relevance); 
			int score = (int) Math.round(tag.relevanceScore());

			FacetText text = FacetText.getFacetText(facet, art, numW, nameW,
					false, false, score, this, isUnderline(facet));
			text.setOffset(margin_size(), y);
			addChild(text);

			y += art.lineH;
		}
		return y;
	}

	private boolean isUnderline(Perspective facet) {
		assert Util.ignore(facet);
		return false;
		// return facet.getParent() != null;
	}

	private double getTopMargin() {
		return label.getMaxY() + art.lineH / 2;
	}

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}

	public void validate(double w1, double h1) {
		// Util.print("ExtremeTags.validate " + w1 + " " + minWidth());
		setBounds(0, 0, w1, h1);
		label.setFont(art.font);
		label.setOffset(margin_size(), 0);
		label.setWidth(Math.round((w1 - margin_size()) / label.getScale()));
		// boundary.margin = art.grid.margin_size() / 2;
		boundary.validate();
		updateData();
	}

	int margin_size() {
		return art.getShowCheckboxes() ? 8 : 40;
	}

	@Override
	public double minWidth() {
		return label.getWidth() * label.getScale() + margin_size();
		// return art.lineH * 5;
	}

	@Override
	public double maxWidth() {
		return w + art.grid.w - art.grid.minWidth();
	}

	@Override
	public void updateBoundary(Boundary boundary1) {
		assert boundary1 == boundary;
		if (art.getShowBoundaries()) {
			// System.out.println("Grid.updateBoundary " + boundary.centerX());
			validate(boundary1.center(), h);
			art.updateExtremesBoundary();
		}
	}

	@Override
	public void enterBoundary(Boundary boundary1) {
		if (!art.getShowBoundaries()) {
			boundary1.exit();
		}
	}

	@SuppressWarnings("unchecked")
	public void highlightFacet(Set<Perspective> facets) {
		for (Iterator<PNode> it = getChildrenIterator(); it.hasNext();) {
			PNode node = it.next();
			if (node instanceof FacetText) {
				FacetText child = (FacetText) node;
				Perspective childFacet = child.getFacet();
				if (childFacet != null && facets.contains(childFacet)) {
					child.highlightFacet();
				}
			}
		}
	}

}
