/* 

 Created on Mar 4, 2005

 Bungee View lets you search, browse, and data-mine an image collection.  
 Copyright (C) 2006  Mark Derthick

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.  See gpl.html.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 You may also contact the author at 
 mad@cs.cmu.edu, 
 or at
 Mark Derthick
 Carnegie-Mellon University
 Human-Computer Interaction Institute
 Pittsburgh, PA 15213

 */

package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;

final class Summary extends LazyContainer implements MouseDoc {

	// static int updateCount = 0;

	/**
	 * Horizontal space between buttons
	 */
	private final static int BUTTON_MARGIN = 8;

	Bungee art;

	Query q;

	double queryW;

	QueryViz queryViz;

	APText label;

	private TextNfacets summaryText;

	ClearButton clear;

	private BookmarkButton bookmark;

	private APText clearMessage;

	private EllipsisButton ellipsis;

	private Boundary boundary;

	private RestrictButton restrict;

	ColorKey colorKey;

	PopupSummary facetDesc;

	RankComponentHeights rankComponentHeights = new RankComponentHeights(0, 0,
			0, 0);

	final List ranks;

	/**
	 * The drop-down menu on rank labels.
	 */
	PerspectiveList perspectiveList;

	Summary(Bungee _art) {
		super();
		art = _art;
		// setPaint(Bungee.summaryBG);
		queryViz = new QueryViz(this);
		addChild(queryViz);
		ranks = new ArrayList(art.query.nAttributes);

		label = art.oneLineLabel();
		label.setScale(2.0);
		label.setTextPaint(Bungee.summaryFG);
		label.setPickable(false);
		label.setText("Search");
		addChild(label);

		summaryText = new TextNfacets(art, Bungee.summaryFG, false);
		summaryText.setWrapText(false);
		summaryText.setWrapOnWordBoundaries(false);
		// summaryText.setTextPaint(summaryTextColor);
		summaryText.setPaint(Bungee.summaryBG);
		// summaryText.setFont(Art.font);
		// summaryText.setConstrainWidthToTextWidth(false);
		// summaryText.setConstrainHeightToTextHeight(false);
		// summaryText.setPickable(false);
		summaryText.addInputEventListener(new SummaryTextHover());
		addChild(summaryText);

		clearMessage = art.oneLineLabel();
		// clearMessage.setPaint(Bungee.summaryFG);
		clearMessage.setTextPaint(Bungee.helpColor);
		clearMessage.setPickable(false);
		clearMessage
				.setText(" (To clear a single filter, click on the tag again.) ");

		ellipsis = new EllipsisButton();
		ellipsis.setVisible(false);
		addChild(ellipsis);

		clear = new ClearButton();
		// clear.addInputEventListener(new ClearQueryHandler());
		clear.setVisible(false);
		addChild(clear);

		if (true || art.jnlpClipboardService != null) {
			bookmark = new BookmarkButton();
			bookmark.setVisible(false);
			addChild(bookmark);
		}

		restrict = new RestrictButton();
		restrict.setVisible(false);
		addChild(restrict);

		// highlighter = new Highlighter(this);
		// highlighter.start();

		// barHelp = new Help(art);

		// colorKey = new ColorKey();
		// addChild(colorKey);

		facetDesc = new PopupSummary(art);
		addChild(facetDesc);

		setPickable(false);
	}

	void init() {

		q = art.query;
		synchronizeWithQuery();

		// for (int i = 0; i < ranks.size(); i++) {
		// Rank r = ((Rank) ranks.get(i));
		// addChild(r);
		// }

		// fetcher = new Fetcher();
		// fetcher.start();

		boundary = new Boundary(this, false);
		addChild(boundary);
	}

	void delayedInit() {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			r.delayedInit();
		}
	}

	// void validate(double _w, double _h, double _minW, double _maxW) {
	// maxW = _maxW;
	// minW = _minW;
	// validate(_w, _h);
	// }

	void validate(double _w, double _h) {
		// if (_w != w || _h != h) {
		validateInternal(_w, _h, true);
		// redrawLabels();
		// }
	}

	void validateInternal(double _w, double _h, boolean recomputeQueryW) {
		// Util.print("Summary.validateInternal " + _w + " " + _h + " " + queryW
		// + " " + recomputeQueryW + " " + minWidth(recomputeQueryW));
		assert _w == (int) _w;
		assert _h == (int) _h;
		w = _w; // - art.lineH; // Leave room for rightmost rotated label; No,
		// use grid margin.
		h = _h;
		// setBounds(0, 0, _w, h);
		boundary.margin = art.grid.margin_size() / 2;
		setTextSize();
		double summaryTextY = label.getGlobalBounds().getHeight();
		double buttonY = summaryTextY + /* 1.5 * */art.lineH + 4;
		// Place this now so getTopMargin works
		clear.setOffset(queryW, buttonY);

		{
			// computeRankComponentHeights depends on queryW, because we don't
			// want
			// labels running off the left edge of the window.
			// But if something else constrains label height to be smaller, make
			// queryW smaller, too.
			if (recomputeQueryW) {
				// double minW = clear.getWidth() + restrict.getWidth()
				// + bookmark.getWidth() + 2 * BUTTON_MARGIN;
				queryW = queryViz.maxWidth();
			}
			computeRankComponentHeights();
			if (recomputeQueryW) {

				// don't use constrain, because it will gag if _w * 0.22 > _w -
				// minW
				queryW = Math.round(Math.min(queryW, Math.max(_w * 0.22,
						rankComponentHeights.labelsH() - art.lineH)));
				// if (updatedQueryW != queryW) {
				// queryW = updatedQueryW;
				// computeRankComponentHeights();
				// }
			}
		}
		label.setOffset(queryW, 0);

		double ellipsisX = w - ellipsis.minWidth();
		ellipsis.setOffset(ellipsisX, summaryTextY - 2); // -2 corrects for
		// border, and makes
		// text line up

		summaryText.setOffset(queryW, summaryTextY);
		summaryText.setBounds(0, 0, ellipsisX - BUTTON_MARGIN - queryW,
				art.lineH);
		summaryText.layoutBestFit();

		clear.setOffset(queryW, buttonY);
		clearMessage.setOffset(queryW, clear.getYOffset());

		if (bookmark != null) {
			bookmark.setOffset(w - bookmark.minWidth(), clear.getYOffset());

			restrict.setOffset(
					(clear.getMaxX() + bookmark.getXOffset() - restrict
							.minWidth()) / 2, clear.getYOffset());
		} else
			restrict.setOffset(clear.getMaxX() + BUTTON_MARGIN, clear
					.getYOffset());

		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			r.validate(w);
		}

		// boundary.setMinX(minW);
		// boundary.setMaxX(maxW);
		boundary.validate();

		queryViz.validate(queryW, h);
		// colorKey.fit();
		if (perspectiveList != null)
			perspectiveList.validate();

		validateColorKey();
	}

	void validateColorKey() {
		if (colorKey != null)
			colorKey.removeFromParent();
		double topMargin = summaryText.getYOffset();
		colorKey = new ColorKey(queryW - Rank.LABEL_RIGHT_MARGIN
				- Rank.LABEL_LEFT_MARGIN, clear.getMaxY() - topMargin);
		colorKey.setOffset(Rank.LABEL_LEFT_MARGIN, topMargin);
		colorKey.setVisible(art.query.isRestricted());
		colorKey.setPickable(false);
		addChild(colorKey);
	}

	void setTextSize() {
		// Util.print("Summary.setTextSize " + art.font + " " +
		// label.getGlobalScale());
		label.setFont(art.font);
		// label.setHeight(art.lineH);

		ellipsis.setFont(art.font);
		clear.setFont(art.font);
		bookmark.setFont(art.font);
		restrict.setFont(art.font);
		// colorKey.setSize();
	}

	double internalH() {
		return h - getTopMargin() - getBottomMargin();
	}

	double getTopMargin() {
		// Limit summary to 3 lines

		// // Can't use clear.getMaxY because we use this to determine
		// return label.getScale() * art.lineH + clear.getScale() * art.lineH
		// + art.lineH /2;

		assert !Double.isNaN(clear.getMaxY());
		assert !Double.isNaN(art.lineH);
		return clear.getMaxY() + art.lineH / 2;
	}

	double getBottomMargin() {
		assert !Double.isNaN(queryViz.getBottomMargin());
		assert !Double.isNaN(art.lineH);
		return queryViz.getBottomMargin() + art.lineH / 2;
	}

	public void updateBoundary(Boundary boundary1) {
		assert boundary1 == boundary;
		if (art.getShowBoundaries()) {
			// System.out.println("Grid.updateBoundary " + boundary.centerX());
			validateInternal(boundary1.center(), h, false);
			art.updateSummaryBoundary();
		}
	}

	public void enterBoundary(Boundary boundary1) {
		if (!art.getShowBoundaries()) {
			boundary1.exit();
		}
	}

	void updateQueryBoundary() {
		queryW = queryViz.getWidth();
		validateInternal(w, h, false);
	}

	public double minWidth() {
		return minWidth(false);
	}

	double widgetWidth() {
		return clear.minWidth() + restrict.minWidth() + bookmark.minWidth() + 2
				* BUTTON_MARGIN;
	}

	double minWidth(boolean recomputeQueryW) {
		// TextButton b = clear;
		// if (bookmark != null)
		// b = bookmark;
		// if (restrict != null)
		// b = restrict;
		return widgetWidth()
				+ (recomputeQueryW ? queryViz.minWidth() : queryViz.getWidth());
	}

	public double maxWidth() {
		return w + art.grid.w - art.grid.minWidth();
	}

	public double minHeight() {
		return art.lineH * (10 + art.query.nAttributes);
	}

	void setFeatures() {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			r.setFeatures();
		}
		if (!art.getShowTagLists())
			hidePerspectiveList();
	}

	// private void initColors() {
	// colors = new Color[nColors];
	// for (int i = 0; i < nColors; i++) {
	// colors[i] = Color.getHSBColor(((i * nColors / 2 - 3 * i) % nColors)
	// / (float) nColors, 0.25f, 0.5f);
	// }
	// }

	void setDescription() {
		Markup description = q.descriptionVerbPhrase();
		// if (summaryText.getText() == null
		// || !summaryText.getText().equals(description)) {
		// Util.print("Summary.setDescription");
		summaryText.setContent(description);
		summaryText.layoutBestFit();
		// summaryText.setTextPaint(q.isRestricted() ? summaryTextColor : FG);

		setEllipsisVisibility();

		boolean buttonsVisible = q.isRestricted();
		clear.setNumFilters();
		clear.setVisible(buttonsVisible || q.isRestrictedData());
		if (bookmark != null) {
			bookmark.setVisible(buttonsVisible);
		}
		if (restrict != null) {
			restrict.setVisible(buttonsVisible);
		}
	}

	void setEllipsisVisibility() {
		boolean incomplete = false;
		if (summaryText.getWidth() > 0)
			incomplete = summaryText.isIncomplete();
		// Util.print("setEllipsisVisibility " + incomplete);
		ellipsis.setVisible(incomplete);
		// summaryText.setPickable(incomplete);
	}

	int nRanks() {
		return ranks.size();
	}

	double marginH() {
		return rankComponentHeights.marginH();
	}

	double selectedFoldH() {
		return rankComponentHeights.foldH();
	}

	double selectedFrontH() {
		return rankComponentHeights.frontH();
	}

	double selectedLabelH() {
		return rankComponentHeights.labelsH();
	}

	static final class DesiredSize {

		final double min, max, ratioToDeselectedH;

		DesiredSize(double _min, double _max, double _ratioToDeselectedH) {
			min = _min;
			max = _max;
			ratioToDeselectedH = _ratioToDeselectedH;
			assert min >= 0;
			assert max >= min;
			assert ratioToDeselectedH >= 0;
		}

		DesiredSize scale(double scale) {
			return new DesiredSize(min * scale, max * scale, ratioToDeselectedH
					* scale);
		}
	}

	static final class RankComponentHeights {
		private final double fold, front, labels, margin;

		RankComponentHeights(double _fold, double _front, double _labels,
				double _margin) {
			fold = _fold;
			front = _front;
			labels = _labels;
			margin = _margin;
			assert fold >= 0;
			assert front >= 0;
			assert labels >= 0;
			assert margin >= 0;
		}

		double selectedH() {
			return fold + front + labels;
		}

		double deselectedH(Summary summary) {
			int nDeselectedRanks = Math.max(2, summary.nRanks()) - 1;
			double internalH = Math.max(0, summary.internalH());
			double deselectedH = Math.floor((internalH - selectedH())
					/ nDeselectedRanks);

			assert selectedH() + nDeselectedRanks * deselectedH <= internalH : (selectedH() + nDeselectedRanks
					* deselectedH)
					+ " "
					+ internalH
					+ " "
					+ nDeselectedRanks
					+ " "
					+ deselectedH;

			return deselectedH - margin;
		}

		double foldH() {
			return fold;
		}

		double frontH() {
			return front;
		}

		double labelsH() {
			return labels;
		}

		double marginH() {
			return margin;
		}

		static RankComponentHeights lerp(double zeroToOne,
				RankComponentHeights start, RankComponentHeights end) {
			assert zeroToOne >= 0 && zeroToOne <= 1 : zeroToOne;
			return new RankComponentHeights(PNode.lerp(zeroToOne, start.fold,
					end.fold), PNode.lerp(zeroToOne, start.front, end.front),
					PNode.lerp(zeroToOne, start.labels, end.labels), PNode
							.lerp(zeroToOne, start.margin, end.margin));
		}

		public String toString() {
			return "<RankComponentHeights fold=" + fold + "; front=" + front
					+ "; labels=" + labels + "; margin=" + margin + ">";
		}

		public boolean equals(Object o) {
			if (!(o instanceof RankComponentHeights))
				return false;
			RankComponentHeights h = (RankComponentHeights) o;
			return h.fold == fold && h.front == front && h.labels == labels
					&& h.margin == margin;
		}

		public int hashCode() {
			int result = 17;
			// we know that components are really integers
			result = 37 * result + (int) fold;
			result = 37 * result + (int) front;
			result = 37 * result + (int) labels;
			result = 37 * result + (int) margin;
			return result;
		}
	}

	void computeRankComponentHeights() {
		computeRankComponentHeights(Bungee.rankAnimationMS);
	}

	void computeRankComponentHeights(long duration) {
		// Any bigger than this, and the text would run off the screen to
		// the left
		final double maxSelectedLabelH = queryW + art.lineH;
		final double minSelectedLabelH = Math.min(maxSelectedLabelH,
				6 * art.lineH);
		computeRankComponentHeights(new DesiredSize(0,
				Double.POSITIVE_INFINITY, 1.5), new DesiredSize(0,
				Double.POSITIVE_INFINITY, 3), new DesiredSize(
				minSelectedLabelH, maxSelectedLabelH, 7), new DesiredSize(1,
				Double.POSITIVE_INFINITY, 0.2));
		layoutChildrenWhenNeeded(duration);
	}

	/**
	 * Sets rankComponentHeights (fold, front, and labels selected/deselected
	 * heights and margin).
	 * 
	 * Called when height, number of ranks, or queryW change.
	 * 
	 * All ratios are to the combined deselected+margin height
	 */
	void computeRankComponentHeights(DesiredSize desiredFoldH,
			DesiredSize desiredFrontH, DesiredSize desiredLabelsH,
			DesiredSize desiredMarginH) {
		if (h > 0) {
			// Util.print("setDeselectedHeights " + h + " " + ranks.size());
			int nDeselectedRanks = Math.max(2, nRanks()) - 1;
			DesiredSize[] desired = { desiredFoldH, desiredFrontH,
					desiredLabelsH, desiredMarginH.scale(nDeselectedRanks) };

			double unallocated = internalH();
			double selectedToDeselectedRatio = 0;
			for (int i = 0; i < desired.length; i++) {
				selectedToDeselectedRatio += desired[i].ratioToDeselectedH;
			}

			for (int i = 0; i < desired.length; i++) {

				// Convert the selected rank components into an equivalent
				// number of
				// deselectedRankH's
				// and combine with them, so you can divide into internalH and
				// get
				// the deselected rank height
				double equivNranks = nDeselectedRanks
						+ selectedToDeselectedRatio;
				double deselectedH = unallocated / equivNranks;

				DesiredSize size = desired[i];

				double componentH = size.ratioToDeselectedH * deselectedH;
				if (componentH < size.min) {
					// recompute with this as a constant
					unallocated -= size.min;
					selectedToDeselectedRatio -= size.ratioToDeselectedH;
				} else if (componentH > size.max) {
					// recompute with this as a constant
					unallocated -= size.max;
					selectedToDeselectedRatio -= size.ratioToDeselectedH;
				}
			}
			double equivNranks = nDeselectedRanks + selectedToDeselectedRatio;
			double deselectedH = unallocated / equivNranks;
			double[] sizes = new double[desired.length];
			for (int i = 0; i < desired.length; i++) {
				DesiredSize size = desired[i];
				sizes[i] = Math.round(Util.constrain(size.ratioToDeselectedH
						* deselectedH, size.min, size.max));
			}
			rankComponentHeights = new RankComponentHeights(sizes[0], sizes[1],
					sizes[2], Math.floor(sizes[3] / nDeselectedRanks));
		} else {
			rankComponentHeights = new RankComponentHeights(0, 0, 0, 0);
		}

		// Util.print("computeRankComponentHeights return "
		// + rankComponentHeights);
		// }
	}

	void layoutChildrenWhenNeeded() {
		layoutChildrenWhenNeeded(Bungee.rankAnimationMS);
	}

	PActivity rankAnimator;

	// call Rank.prepareAnimation and then animate to new heights
	void layoutChildrenWhenNeeded(long duration) {
		double deselectedRankH = rankComponentHeights.deselectedH(this);
		// Util.print("\nSummary.layoutChildrenWhenNeeded " + ranks.size() + " "
		// + deselectedRankH);
		if (deselectedRankH > 0) {
			double selectedRankH = rankComponentHeights.selectedH();
			double margin = rankComponentHeights.marginH();
			double yOffset = getTopMargin();
			for (Iterator it = ranks.iterator(); it.hasNext();) {
				Rank r = ((Rank) it.next());
				// r.updateHeights();
				double rH = r.isConnected ? selectedRankH : deselectedRankH;
				r.prepareAnimation(yOffset, rH);
				// r.animateToBounds(0.0, yOffset, w, desiredH,
				// ConditionalAnimationPNode.animationMS);
				// Util.print("Summary: Rank " + r.getName() + " " + yOffset + "
				// "
				// + desiredH + " " + r.getHeight());
				yOffset += rH + margin;
			}
			if (duration > 0) {
				rankAnimator = new PInterpolatingActivity(duration,
						Bungee.rankAnimationStep) {
					protected void activityFinished() {
						super.activityFinished();
						rankAnimator = null;
					}

					public void setRelativeTargetValue(float zeroToOne) {
						// Util.print("Summary.animateRank");
						for (Iterator it = ranks.iterator(); it.hasNext();) {
							Rank r = ((Rank) it.next());
							r.animateRank(zeroToOne);
						}
					}
				};
				addActivity(rankAnimator);
			} else {
				for (Iterator it = ranks.iterator(); it.hasNext();) {
					Rank r = ((Rank) it.next());
					r.animateRank(1);
				}
			}
		}
	}

	void restrict() {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			r.restrict();
		}
	}

	void updateData() {
		// Util.print("Summary.updateData " + art.query.isQueryValid() + " " +
		// art.query);
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			r.updateData();
			if (r.getParent() == null) {
				// all its PVs have zero totalChildTotalCount, so rank has been
				// removed.
				// start over so you don't get concurrent modification error.
				updateData();
				break;
			}
		}

		PInterpolatingActivity dataAnimator = new PInterpolatingActivity(
				Bungee.dataAnimationMS, Bungee.dataAnimationStep) {

			// void activityStarted() {
			// super.activityStarted();
			// Util.print("animation activityStarted");
			// }

			public void setRelativeTargetValue(float zeroToOne) {
				// updateCount++;
				// Util.print("Summary.animateData");
				for (int i = 0; i < nRanks(); i++) {
					Rank r = (Rank) ranks.get(i);
					r.animateData(zeroToOne);
				}
			}
		};
		addActivity(dataAnimator);

		if (perspectiveList != null)
			perspectiveList.updateData();
		if (colorKey != null)
			colorKey.setVisible(art.query.isRestricted());
		if (bookmark != null)
			bookmark.showDisabledState();
		if (restrict != null)
			restrict.showDisabledState();
	}

	// void redrawLabels() {
	// Rank r = connectedRank();
	// if (r != null)
	// r.redrawLabels();
	// }

	void updateSelections(Set facets) {
		// Util.print("Summary.updateSelections " + facets);
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			PerspectiveViz[] ps = r.perspectives;
			boolean inRank = false;
			for (Iterator fit = facets.iterator(); fit.hasNext() && !inRank;) {
				Perspective facet = (Perspective) fit.next();
				for (int j = 0; j < ps.length && !inRank; j++) {
					Perspective pvP = ps[j].p;
					inRank = (pvP == facet || pvP == facet.getParent());
				}
			}
			if (inRank)
				r.updatePerspectiveSelections(facets);
		}
		summaryText.updateSelections(facets);
	}

	PerspectiveViz lookupPV(Perspective facet) {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			PerspectiveViz[] ps = r.perspectives;
			for (int j = 0; j < ps.length; j++) {
				if (ps[j].p == facet)
					return ps[j];
			}
		}
		return null;
	}

	PerspectiveViz findPerspectiveViz(String facetName) {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			PerspectiveViz[] ps = r.perspectives;
			for (int j = 0; j < ps.length; j++) {
				if (ps[j].p.getName().equals(facetName))
					return ps[j];
			}
		}
		return null;
	}

	void synchronizeWithQuery() {
		synchronizeWithQuery(null);
	}

	void synchronizeWithQuery(Perspective toConnect) {
		// Util.print("summary.updateAllData " + isTotal);
		synchronizePerspectives(q.displayedPerspectives(), toConnect); // Start
		// animating
		// PerspectiveViz adds
		// and
		// deletes.
		// queryViz.positionSearchBox(h);
		queryViz.synchronizeWithQuery();
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			// r.updateHeights();
			r.updateNameLabels();
		}
		// highlightFacet = -1; // invalidate cached values.
	}

	/**
	 * @param facets
	 *            facets for which highlighting has changed
	 */
	void highlightFacet(Set facets) {
		updateSelections(facets);
		if (perspectiveList != null)
			perspectiveList.highlightFacet();
	}

	void highlightCluster(Set facets) {
		// if (facet != null) {
		queryViz.highlightCluster(facets);
		summaryText.updateSelections(facets);
	}

	void clearQuery() {
		art.printUserAction(Bungee.BUTTON, "Clear", 0);
		boolean isMultipleFilters = q.nFilters(false, true, false, false) > 1;
		art.clearQuery();
		if (isMultipleFilters) {
			addChild(clearMessage);
		}
	}

	// static Color colorForIndex(int index) {
	// return colors[index % nColors];
	// }

	// int numNonZombies() {
	// int result = 0;
	// Iterator it = ranks.iterator();
	// while (it.hasNext()) {
	// Rank r = (Rank) it.next();
	// if (r.isZombie()) {
	// if (true || r.getHeight() == 0.0) {
	// //Util.print("clean zombie rank " + r.getName());
	// removeChild(r);
	// it.remove();
	// }
	// } else
	// result++;
	// }
	// return result;
	// }

	private Set previousRestrictions = new HashSet();

	void synchronizeSelections(Set previousRestrictions2) {
		// for (Iterator it = previousRestrictions2.iterator(); it.hasNext();) {
		// Perspective p = (Perspective) it.next();
		//
		// assert p.getParent() != null : p;
		updateSelections(previousRestrictions2);
	}

	// void synchronizePerspectives() {
	// synchronizePerspectives(q.displayedPerspectives());
	// }

	void synchronizePerspectives(Collection queryPerspectives,
			Perspective toConnect) {
		// Util.print("synchronizePerspectives " + art.highlightedFacet + " " +
		// queryPerspectives);

		// out with the old.
		// Perspective highlight = art.highlightedFacet;
		for (Iterator pit = ranks.iterator(); pit.hasNext();) {
			Rank r = (Rank) pit.next();
			PerspectiveViz[] ps = r.perspectives;
			PerspectiveViz[] remove = null;
			boolean removeAll = true;
			for (int i = 0; i < ps.length; i++) {
				PerspectiveViz pv = ps[i];
				if (queryPerspectives.contains(pv.p))
					removeAll = false;
				else {
					remove = (PerspectiveViz[]) Util.push(remove, pv,
							PerspectiveViz.class);
					// if (highlight != null && highlight.hasAncestor(pv.p)) {
					// art.highlightFacet(null, 0);
					// }
				}
			}
			if (removeAll) {
				// Util.print("Remove rank " + r);
				removeChild(r);
				pit.remove();
			} else {
				// Util.print("Remove pvs " + remove);
				r.removePerspectives(remove);
			}
		}

		// in with the new.
		for (Iterator iter = queryPerspectives.iterator(); iter.hasNext();) {
			Perspective p = (Perspective) iter.next();
			if (lookupPV(p) == null) {
				Rank rank = addPerspective(p);
				rank.delayedInit();
				// if (defaultConnect != null) {
				// defaultConnect = p;
				// // forceRedraw = false;
				// }
			}
		}

		if (toConnect != null)
			connectToPerspective(toConnect);
		// else if (connectedPerspective() == null)
		// // If we removed the connected rank above, it might have had
		// perspective list showing
		// hidePerspectiveList();
		setDescription();
		queryViz.synchronizeWithQuery();
		computeRankComponentHeights();

		synchronizeSelections(previousRestrictions);
		synchronizeSelections(previousRestrictions = q.allRestrictions());
	}

	Rank findRank(PerspectiveViz pv) {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			if (r.usesPerspective(pv))
				return r;
		}
		return null;
	}

	Rank getChildRank(Rank parent) {
		if (parent != null) {
			for (Iterator it = ranks.iterator(); it.hasNext();) {
				Rank r = (Rank) it.next();
				if (r.parentRank == parent)
					return r;
			}
		}
		return null;
	}

	Rank addPerspective(Perspective p) {
		// Util.print("Summary.addPerspective " + p);
		PerspectiveViz pv = new PerspectiveViz(p, this);
		Rank parentRank = findRank(pv.parentPV);
		Rank result = getChildRank(parentRank);
		if (result == null) {
			result = new Rank(this, parentRank);
			result.validate(w);
			addChild(result);
			// Util.print("addRank for " + p);
			if (parentRank == null)
				ranks.add(result);
			else
				ranks.add(ranks.indexOf(parentRank) + 1, result);
		}
		result.addPerspective(pv);
		return result;
	}

	PerspectiveViz[] getChildPVs(PerspectiveViz parent) {
		PerspectiveViz[] children = null;
		Rank r = getChildRank(parent.rank);
		if (r != null) {
			PerspectiveViz[] ps = r.perspectives;
			for (int j = 0; j < ps.length; j++) {
				if (ps[j].parentPV == parent)
					children = (PerspectiveViz[]) Util.push(children, ps[j],
							PerspectiveViz.class);
			}
		}
		return children;
	}

	void flagConnectedRank(Rank r) {
		// Util.print("connectToRank " + r.getName() + " " + r.isConnected + " "
		// + connectedRank());
		Rank current = connectedRank();
		if (r != current) {
			// Util.print("summary.flagConnectedRank " + r + " old was "
			// + connectedRank());
			hidePerspectiveList();
			if (current != null) {
				current.setConnected(false);
			}
			if (r != null)
				r.setConnected(true);
			queryViz.setSearchVisibility(r != null);
			art.setHelpVisibility();
		}
	}

	Rank connectedRank() {
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			if (r.isConnected)
				return r;
		}
		return null;
	}

	Perspective connectedPerspective() {
		Perspective result = null;
		Rank r = connectedRank();
		if (r != null)
			result = r.perspectives[0].p;
		return result;
	}

	void connectToPerspective(Perspective p) {
		// Util.print("connectToPerspective " + p);
		PerspectiveViz pv = lookupPV(p);
		assert pv != null : p;
		Rank rank = pv.rank;
		assert rank != null : p;
		// rank.connect();
		flagConnectedRank(rank);
	}

	int nBars() {
		int result = 0;
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			result += r.nBars();
		}
		return result;
	}

	double[] pValues() {
		double[] result = null;
		for (Iterator it = ranks.iterator(); it.hasNext();) {
			Rank r = (Rank) it.next();
			result = Util.append(result, r.pValues());
		}
		assert result != null : ranks + " " + art.query;
		return result;
	}

	void setSummaryTextDoc(boolean state) {
		ellipsis.setMouseDoc(state);
		// setMouseDoc(summaryText, state);
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// // Util.print("Summary.setMouseDoc " + state + " "
	// // + (source == summaryText));
	// state = state && ellipsis.getVisible();
	// art.setMouseDoc(state);
	// }

	public void setMouseDoc(String s) {
		art.setMouseDoc(s);
	}

	// void setMouseDoc(Markup ignore1, boolean ignore2) {
	// assert Util.ignore(ignore1);
	// assert Util.ignore(ignore2);
	// assert false;
	// }

	boolean toggleSummary() {
		return summaryText.getHeight() > art.lineH ? contractSummary()
				: expandSummary();
	}

	boolean expandSummary() {
		art.printUserAction(Bungee.BUTTON, "Ellipsis", 0);
		if (summaryText.getHeight() <= art.lineH && summaryText.isIncomplete()) {
			// Util.print("expandSummary");
			summaryText.setWrapText(true);
			summaryText.setWrapOnWordBoundaries(true);
			summaryText.setTrim(-1, (int) (art.lineH / 2));
			// double summaryH =
			summaryText.layout(summaryText.getWidth(), h
					- summaryText.getYOffset());
			// summaryText.setHeight(summaryH + 10.0); // Easier to read with a
			// little extra margin at
			// the bottom
			summaryText.moveToFront();
			// summaryText.setPickable(false);
			// ellipsis.setVisible(false);
			ellipsis.setState(false);
			ellipsis.mouseDoc = "Truncate the summary";
			return true;
		}
		return false;
	}

	boolean contractSummary() {
		if (summaryText.getHeight() > art.lineH) {
			// Util.print("contractSummary");
			summaryText.setTrim(-1, -1);
			summaryText.setWrapText(false);
			summaryText.setWrapOnWordBoundaries(false);
			summaryText.setHeight(art.lineH);
			summaryText.layout();
			// ellipsis.moveToFront();
			ellipsis.setState(true);
			ellipsis.mouseDoc = "Show the rest of the summary";
			setEllipsisVisibility();
			return true;
		}
		return false;
	}

	void maybeHideTransients(PInputEvent e) {
		// Util.print(summaryText);
		Point2D p = e.getPositionRelativeTo(summaryText);
		if (p.getX() < 0 || p.getY() < 0 || p.getX() >= summaryText.getWidth()
				|| p.getY() >= summaryText.getHeight() - 10)
			contractSummary();
	}

	void mayHideTransients() {
		art.mayHideTransients();
	}

	void doHideTransients() {
		if (clearMessage.getParent() != null)
			removeChild(clearMessage);
		contractSummary();
		// colorKey.fit();
		queryViz.doHideTransients();
	}

	/**
	 * Only called by replayOps
	 */
	void clickBar(Perspective facet, int modifiers) {
		PerspectiveViz pv = lookupPV(facet.getParent());
		assert pv != null : facet.getParent();
		pv.clickBar(facet, modifiers);
	}

	void clickRank(Perspective facet, int modifiers) {
		lookupPV(facet).pickFacet(facet, modifiers);
	}

	boolean removeSearch(String string) {
		return queryViz.removeSearch(string);
	}

	void clickEllipsis() {
		ellipsis.doPick();
	}

	void clickClear() {
		clear.doPick();
	}

	boolean doneDelayedInit = false;

	protected void paint(PPaintContext ignore) {
		// Util.print("Summary.paint");
		if (!doneDelayedInit) {
			doneDelayedInit = true;
			// Creating the editor takes forever, so put it off.
			// Can't figure out a better way to make sure we're displayed.

			Runnable delayedInit = new Runnable() {

				public void run() {
					queryViz.init();
					delayedInit();
					art.selectedItem.init();
					art.grid.init();
					art.setInitialState();
				}
			};

			javax.swing.SwingUtilities.invokeLater(delayedInit);
		}
		super.paint(ignore);
	}

	protected boolean pickAfterChildren(PPickPath pickPath) {
		boolean result = super.pickAfterChildren(pickPath);
		Util.print("summary.pickAfterChildren " + result);
		return result;
	}

	void setSelectedForEdit(Perspective facet) {
		queryViz.setSelectedForEdit(facet);
	}

	void showPopup(Perspective facet) {
		if (isShowPopup(facet)) {
			// Work your way up from facet until you find an ancestor with a pv.
			// barp
			// will be a bar on it (or the same pv if facet is a facet_type)

			for (Perspective ancestor = facet, barp = facet;; barp = ancestor, ancestor = ancestor
					.getParent()) {
				assert ancestor != null : facet + " " + barp + " "
						+ lookupPV(barp) + " "
						+ art.query.displayedPerspectives() + " " + ranks;
				if (ancestor != facet || ancestor.getParent() == null) {
					PerspectiveViz pv = lookupPV(ancestor);
					if (pv != null) {
						LazyPNode anchor = lookupPV(ancestor).anchorForPopup(
								barp);
						// if (anchor != null) {
						// There a transient state where pvp has a pv, but
						// it
						// doesn't have any bars yet.
						facetDesc.setFacet(facet, false, anchor);
						// }
						break;
					}
				}
			}

			// Perspective lowestDisplayedChild = facet;
			// Perspective lowestDisplayed = facet.getParent() == null ? facet
			// : facet.getParent();
			// PerspectiveViz pv = findPerspective(lowestDisplayed);
			// while (pv == null) {
			// assert lowestDisplayed != null : facet + " "
			// + lowestDisplayedChild;
			// lowestDisplayedChild = lowestDisplayed;
			// lowestDisplayed = lowestDisplayed.getParent();
			// pv = findPerspective(lowestDisplayed);
			// }
			// LazyPNode anchor = pv.anchorForPopup(lowestDisplayedChild);
			//
			// facetDesc.setFacet(facet, pv.rank, false, anchor);
		}
	}

	void stop() {
		if (facetDesc != null) {
			// This hangs
			// facetDesc.exit();
			facetDesc = null;
		}
		hidePerspectiveList();
	}

	boolean isShowPopup(Object facet) {
		boolean state = facet != null && !art.isShowingInitialHelp();
		facetDesc.exit();
		// facetDesc.setVisible(state);
		if (state) {
			facetDesc.moveToFront();
			// moveToFront(); // Do this in Popup when it starts translating, to
			// reduce the number of summary invalidatePaints. Here, just worry
			// about grid.
			moveAheadOf(art.grid);
		} else {
			moveBehind(art.header);
		}
		return state;
	}

	// public void showObjectDesc(Object facet, boolean state) {
	// // Util.print("showObjectDesc " + desc);
	// if (!art.isShowingInitialHelp()) {
	// // facetDesc.setVisible(true);
	// if (!state)
	// facet = null;
	// // facetDesc.setVisible(false);
	// // else {
	// // // facetDesc.setContent(desc);
	// // facetDesc.update((Perspective) facet, true);
	// showPopup(facetDesc, facet);
	// // }
	// }
	// }
	//
	// public void showFacet(Perspective facet, boolean state) {
	// // Util.print("MouseDoc.showFacet " + state + " " + facet);
	// // Vector desc = null;
	// // if (state && facet != null) {
	// // desc = new Vector();
	// // if (facet.parent == null) {
	// // desc.add(Util.pluralize(art.query.genericObjectLabel));
	// // desc.add(" having ");
	// // }
	// // desc.add(facet);
	// // desc.add(facetInfo(facet));
	// // }
	// showObjectDesc(facet, state);
	// }

	void showCluster(Cluster cluster) {
		// Util.print("MouseDoc.showFacet " + state + " " + facet);
		// Vector desc = null;
		// if (state && facet != null) {
		// // StringBuffer buf = new StringBuffer();
		// // facet.facetNames(buf);
		// // buf.append(facetInfo(facet));
		// desc = new Vector();
		// desc.add(facet);
		// desc.add(facetInfo(facet));
		// // desc.add(buf.toString());
		// }
		// showObjectDesc(facet, state);
		if (isShowPopup(cluster)) {
			// facetDesc.setAnchor(null);
			facetDesc.setCluster(cluster);
		}
	}

	void showMedianArrowPopup(Perspective facet) {
		if (isShowPopup(facet)) {
			PerspectiveViz pv = lookupPV(facet);
			assert pv != null;
			// facetDesc.setAnchor(pv);
			facetDesc.setFacet(facet, true, pv.medianArrow);
		}
	}

	void togglePerspectiveList(Perspective _selected) {
		if (perspectiveList == null || perspectiveList.isHidden())
			// ensurePerspectiveList is slow so don't call it just to hide it
			ensurePerspectiveList(_selected);
		perspectiveList.toggle();
	}

	Perspective handleArrow(Perspective arrowFocus, int key, int modifiers) {
		return ensurePerspectiveList(arrowFocus).handleArrow(key, modifiers);
	}

	PerspectiveList ensurePerspectiveList(Perspective _selected) {
		if (perspectiveList == null) {
			perspectiveList = new PerspectiveList(art);
		}
		perspectiveList.setSelected(_selected);
		return perspectiveList;
	}

	Perspective listedPerspective() {
		return perspectiveList != null ? perspectiveList.listedPerspective()
				: null;
	}

	void hidePerspectiveList() {
		// Util.print("hidePerspectiveList");
		// Util.printStackTrace();
		if (perspectiveList != null && !perspectiveList.isHidden())
			perspectiveList.toggle();
	}

	// void expandColorKey() {
	// colorKey.expand();
	// }

	final class ColorKey extends LazyPNode {

		final ColorKeyHover colorKeyHover = new ColorKeyHover();

		ColorKey(double w, double h) {
			Color[][] colors = { Markup.INCLUDED_COLORS,
					Markup.POSITIVE_ASSOCIATION_COLORS,
					Markup.UNASSOCIATED_COLORS,
					Markup.NEGATIVE_ASSOCIATION_COLORS, Markup.EXCLUDED_COLORS };
			String[] msgs = {
					"Tag required by filters",
					"Tag associated with filters; Items with this tag are more likely than others to satisfy the filters",
					"Unassociated tag; There is no statistically significant association between this tag and the filters",
					"Tag negatively associated with filters; Items with this tag are less likely than others to satisfy the filters",
					"Tag prohibited by filters" };
			int nColors = colors.length;
			if (!art.getIsShortcuts() && !art.getShowCheckboxes())
				nColors--;
			APText label1 = art.oneLineLabel();
			label1.setTextPaint(Bungee.summaryFG);
			label1.setText("Color Key");
			label.setPickable(false);
			addChild(label1);
			if (label1.getMaxX() > w)
				label1.setScale(w / label1.getMaxX());
			double buttonW = Math.round(label1.getMaxX() / nColors);
			// double y = 1.5 * art.lineH;
			// double buttonH = Math.round(h - y);
			double buttonH = clear.getHeight();
			double y = h - buttonH;
			for (int i = 0; i < nColors; i++) {
				PNode node = new ColorKeyKey(colors[i][0], colors[i][1],
						msgs[i], buttonW, buttonH);
				node.setOffset(i * buttonW, y);
				addChild(node);
			}
		}

		private class ColorKeyKey extends LazyPNode {
			Color color;
			Color highlight;
			String msg;

			ColorKeyKey(Color _color, Color _highlight, String _msg, double w,
					double h) {
				color = _color;
				highlight = _highlight;
				msg = _msg;
				setBounds(0, 0, w, h);
				setPaint(color);
				addInputEventListener(colorKeyHover);
			}

			void enter() {
				setPaint(highlight);
				APText text = art.oneLineLabel();
				text.setPaint(Bungee.summaryBG);
				text.setTextPaint(Bungee.helpColor);
				text.setText(msg);
				text.setOffset(getX(), getMaxY() - text.getHeight());
				addChild(text);
			}

			void exit() {
				setPaint(color);
				removeAllChildren();
			}
		}

		private final class ColorKeyHover extends MyInputEventHandler {

			// private ColorKey colorKey;

			ColorKeyHover() {
				super(ColorKeyKey.class);
				// colorKey = _colorKey;
			}

			protected boolean enter(PNode node) {
				// Util.print("SummaryTextHover.enter " + node);
				((ColorKeyKey) node).enter();
				return true;
			}

			protected boolean exit(PNode node, PInputEvent e) {
				((ColorKeyKey) node).exit();
				return true;
			}
		}

	}

	final class SummaryTextHover extends MyInputEventHandler {

		// private Summary summary;

		SummaryTextHover() {
			super(TextNfacets.class);
			// summary = _summary;
		}

		// LazyPNode getSource(LazyPNode node) {
		// if (node == null)
		// return null;
		// LazyPNode parent = node.getParent();
		// if (parent instanceof Summary)
		// // Could be ellipsis, summaryText, or one of its children.
		// return node;
		// else
		// return getSource(parent);
		// }

		protected boolean click(PNode node) {
			return expandSummary();
			// ((Summary) parent).setMouseDoc(node, false);
		}

		protected boolean enter(PNode node) {
			// Util.print("SummaryTextHover.enter " + node);
			setSummaryTextDoc(true);
			return true;
		}

		protected boolean exit(PNode node, PInputEvent e) {
			setSummaryTextDoc(false);
			maybeHideTransients(e);
			return true;
		}

		protected void mayHideTransients(PNode node) {
			Summary.this.mayHideTransients();
		}
	}

	final class EllipsisButton extends SummaryButton {

		private static final String label1 = "...";

		EllipsisButton() {
			// super("...", art.font, 0, 0, art.getStringWidth("...") + 2,
			// art.lineH + 2, null, 1.5f, color, Bungee.summaryBG);
			super(label1);
			mouseDoc = "Show the rest of the summary";
			// ((PText) child).setTextPaint(color);
		}

		public void doPick() {
			toggleSummary();
		}

		public void exit() {
			Summary summary = (Summary) getParent();
			summary.contractSummary();
		}

		public void mayHideTransients(PNode node) {
			// ((Summary) getParent()).mayHideTransients();
		}

	}

	final class ClearButton extends SummaryButton {

		private static final String label1 = " Clear  ";

		ClearButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(" Unrestrict "),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Remove all text and category filters";

			// nFiltersLabel = art.oneLineLabel();
			// nFiltersLabel.setTextPaint(color);
			// nFiltersLabel.setJustification(Component.CENTER_ALIGNMENT);
			// // nFiltersLabel.setScale(1.0 / scale);
			// nFiltersLabel.setText(label);
			// nFiltersLabel.setOffset(Math.round((child.getWidth() -
			// nFiltersLabel
			// .getWidth()) / 2.0), 0.0);
			// child.addChild(nFiltersLabel);

			// setScale(scale);
		}

		public void doPick() {
			if (getText().equals(label1))
				clearQuery();
			else
				art.setDatabase(art.dbName);
		}

		void setNumFilters() {
			if (!q.isRestricted() && q.isRestrictedData()) {
				mouseDoc = "Revert to exploring the entire database";
				setText(" Unrestrict  ");
			} else {
				mouseDoc = "Remove " + q.describeNfilters();
				setText(label1);
			}
		}
	}

	final class BookmarkButton extends SummaryButton {

		private static final String label1 = " Bookmark  ";

		BookmarkButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(label1),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Copy a URL for your current query to the system Clipboard";
			setDisabledMessage("There are no results to bookmark");
			if (art.informedia != null) {
				setText("New Video Set");
				mouseDoc = "Save these results as an Informedia video set";
				setDisabledMessage("There are no results to save");
			}
		}

		public boolean isEnabled() {
			return art.query.getOnCount() > 0;
		}

		public void doPick() {
			if (art.informedia != null)
				art.saveVideoSet();
			else
				art.copyBookmark();
		}

		public void mayHideTransients(PNode node) {
			Summary.this.mayHideTransients();
		}

	}

	final class RestrictButton extends SummaryButton {

		private static final String label1 = " Restrict  ";

		RestrictButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(label1),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Explore within the current results";
			setDisabledMessage("There are no results to restrict to");
		}

		public boolean isEnabled() {
			return art.query.getOnCount() > 0;
		}

		public void doPick() {
			art.restrict();
		}

		public void mayHideTransients(PNode node) {
			Summary.this.mayHideTransients();
		}

	}

	class SummaryButton extends BungeeTextButton {

		SummaryButton(String text) {
			super(text, Bungee.summaryBG,
					Util.brighten(Bungee.summaryFG, 0.6f), Summary.this.art,
					null);
		}

	}

	boolean keyPress(char key) {
		Rank r = connectedRank();
		if (r != null)
			return r.keyPress(key);
		return false;
	}

	// final class Fetcher extends QueueThread {
	//
	// public Fetcher() {
	// super("Fetcher", null, true, 0);
	// // queue = new LinkedList();
	// // setPriority(getPriority() - 2);
	// }
	//
	// public void process(Object o) {
	// PerspectiveViz pv = (PerspectiveViz) o;
	// Query q = pv.p.getQuery();
	// q.prefetchData(pv.p);
	// javax.swing.SwingUtilities.invokeLater(pv.getDoValidate());
	// }
	//
	// }

}