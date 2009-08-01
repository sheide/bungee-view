/*
 * 
 * Created on Mar 4, 2005
 * 
 * Bungee View lets you search, browse, and data-mine an image
 * collection. Copyright (C) 2006 Mark Derthick
 * 
 * This program is free software; you can redistribute it and/or modify it undercents
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. See gpl.html.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * You may also contact the author at mad@cs.cmu.edu, or at Mark Derthick
 * Carnegie-Mellon University Human-Computer Interaction Institute Pittsburgh,
 * PA 15213
 *  
 */

package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.CollationKey;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.viz.Summary.RankComponentHeights;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Arrow;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;

final class PerspectiveViz extends LazyContainer implements FacetNode,
		PickFacetTextNotifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Summary summary;

	Rank rank;

	PerspectiveViz parentPV;

	Perspective p;

	private SqueezablePNode labels;

	// private LazyPNode parentRect;

	SqueezablePNode front;

	LazyPPath lightBeam = null;

	/**
	 * This will have isVisible==false unless we are the first child of the rank
	 */
	TextNfacets rankLabel;

	APText[] percentLabels;

	private LazyPNode percentLabelHotZone;

	private LazyPNode hotLine;

	private APText hotZonePopup;

	// private double percentLabelW;

	final static double PERCENT_LABEL_SCALE = 0.75;

	static final Color pvBG = new Color(0x1f2333); // Color.darkGray.darker();

	static final double epsilon = 1.0e-10;

	/**
	 * Our logical width, of which floor(leftEdge) - floor(leftEdge +
	 * visibleWidth) is visible. logicalWidth>=visibleWidth();
	 * 
	 * visibleWidth = w - epsilon bars are placed at floor(0 - logicalWidth)
	 */
	double logicalWidth = 0;

	/**
	 * offset into logicalWidth of the leftmost visible pixel. Rightmost visible
	 * pixel is leftEdge + w - epsilon 0<=leftEdge<logicalWidth-visibleWidth;
	 */
	double leftEdge = 0;

	boolean isZooming() {
		return logicalWidth > visibleWidth();
	}

	void resetLogicalBounds() {
		setLogicalBounds(0, visibleWidth());
	}

	void setLogicalBounds(double x, double w) {
		// Util.print("setLogicalBounds " + x + " " + w);
		assert x >= 0 && x + visibleWidth() <= w : x + "/" + w + " "
				+ visibleWidth();
		if (x != leftEdge || w != logicalWidth) {
			leftEdge = x;
			logicalWidth = w;
			revalidate();
		}
	}

	double visibleWidth() {
		return w - epsilon;
		// return getWidth() - 1.0e-12;
	}

	// private double rightEdge = 0;

	// /**
	// * The x midpoint of each facet (indexed by facet.index), whether or not
	// it
	// * is displayed. Length = number of facets.
	// */
	// private int[] x_mid_coords;

	int labelHprojectionW;

	/**
	 * The facet of the bar at a given x coordinate. Only the endpoints are
	 * recorded. Noone should ask for coordinates in between the endpoints.
	 */
	private Perspective[] barXs;

	/**
	 * The facet of the label at a given x coordinate. All points are recorded.
	 */
	private Perspective[] labelXs;

	Hashtable barTable = new Hashtable();

	/**
	 * Remember previous layout parameters, and don't re-layout if they are the
	 * same.
	 */
	private RankComponentHeights prevComponentHeights;

	double prevH;

	MedianArrow medianArrow;

	PerspectiveViz(Perspective _p, Summary _summary) {
		p = _p;
		summary = _summary;
		parentPV = summary.lookupPV(p.getParent());
		setPickable(false);

		// Bungee art = art();

		// Util.print("PerspectiveViz " + p.getName() + " " + p.nValues());

		front = new SqueezablePNode();
		front.setPaint(pvBG);
		// front.setStroke(LazyPPath.getStrokeInstance(0));
		front.setHeight(1);
		// front.clip = new PBounds(-1000, 0.5, 2000, 0.5);
		addChild(front);
		// front isn't visible directly, so bounds don't matter.
		// front.setBounds(0, 0, 1, 1);
		// front.addInputEventListener(Bungee.facetClickHandler);

		// parentRect = new LazyPNode();
		// parentRect.setPaint(pvBG);
		// parentRect.setBounds(0, 0.5, 1, 0.5);

		labels = new SqueezablePNode();
		labels.setVisible(false);
		addChild(labels); // add labels after front, so labels get
		// picked in
		// favor of rankLabel
		// labels.addInputEventListener(Bungee.facetClickHandler);

		rankLabel = new TextNfacets(art(), Bungee.summaryFG, true);
		rankLabel.setPickable(false);
		// rankLabel.unpickableAction = -2;
		rankLabel.setWrapText(false);
		// rankLabel.setUnderline(true);
		// rankLabel.setPaint(Color.red);

		if (p.isOrdered()) {
			Color color = Markup.UNASSOCIATED_COLORS[1];
			medianArrow = new MedianArrow(color, 7, 0);
		}

		front.addInputEventListener(Bungee.facetClickHandler);
	}

	void delayedInit() {
		if (percentLabels == null) {
			percentLabels = new APText[3];
			boolean visible = rankLabel.getVisible();
			// double percentLabelScaledW = percentLabelW * PERCENT_LABEL_SCALE;
			// double x = Math.round(-percentLabelScaledW);
			for (int i = 0; i < 3; i++) {
				percentLabels[i] = art().oneLineLabel();
				percentLabels[i].setTransparency(0);
				percentLabels[i].setVisible(visible);
				percentLabels[i].setPickable(false);
				percentLabels[i].setTextPaint(Bungee.PERCENT_LABEL_COLOR);
				percentLabels[i].setJustification(Component.RIGHT_ALIGNMENT);
				percentLabels[i].setConstrainWidthToTextWidth(false);
				// percentLabels[i].setWidth(percentLabelW);
				// percentLabels[i].setScale(percentLabelScale);
				// percentLabels[i].setXoffset(x);
				front.addChild(percentLabels[i]);
			}
			percentLabels[0].setText(formatOddsRatio(Math
					.exp(-Rank.LOG_ODDS_RANGE))
					+ "+");
			percentLabels[1].setText(formatOddsRatio(Math.exp(0)));
			percentLabels[2].setText(formatOddsRatio(Math
					.exp(Rank.LOG_ODDS_RANGE))
					+ "+");

			percentLabelHotZone = new LazyPNode();
			// percentLabelScaledW *= 0.67; // Make hot zone cover 100%, rather
			// // than up to 0.001%
			// percentLabelHotZone.setBounds(-percentLabelScaledW, 0,
			// percentLabelScaledW, 1);
			// front.addChild(percentLabelHotZone);
			percentLabelHotZone.setPickable(false);
			// percentLabelHotZone.setPaint(Color.yellow);
			percentLabelHotZone
					.addInputEventListener(new HotZoneListener(this));

			hotLine = new LazyPNode();
			hotLine.setPaint(Bungee.PERCENT_LABEL_COLOR);
			hotLine.setVisible(false);
			hotLine.setPickable(false);

			hotZonePopup = art().oneLineLabel();
			// hotZonePopup.setTransparency(0.5f);
			hotZonePopup.setPaint(Color.black);
			hotZonePopup.setTextPaint(Bungee.helpColor);
			hotZonePopup.setVisible(false);
			hotZonePopup.setPickable(false);
			addChild(hotZonePopup);
		}
	}

	String getName() {
		return p.getName();
	}

	void updateData() {
		// Util.print("PV.updateData " + p+" "+p.getTotalChildTotalCount()+"
		// "+query().isQueryValid());
		if (p.getTotalChildTotalCount() == 0) {
			query().removeRestrictionInternal(p);
			summary.synchronizeWithQuery();
		} else {
			assert rank.expectedPercentOn() >= 0;
			if (query().isQueryValid()) {
				for (Iterator it = barTable.values().iterator(); it.hasNext();) {
					Bar bar = ((Bar) it.next());
					bar.updateData();
				}
			}
			if (medianArrow != null) {
				if (p.getOnCount() > 0 && art().getShowMedian()) {
					front.addChild(medianArrow);
					layoutMedianArrow();
				} else {
					medianArrow.removeFromParent();
				}
			}
			drawLabels();
		}
	}

	void animateData(double zeroToOne) {
		for (Iterator it = barTable.values().iterator(); it.hasNext();) {
			Bar bar = ((Bar) it.next());
			bar.animateData(zeroToOne);
		}
	}

	PActivity zoomer;

	void animatePanZoom(final double goalLeftEdge, final double goalLogicalWidth) {
		// Util.print("animatePanZoom " + goalLeftEdge + " " +
		// goalLogicalWidth);
		finishPanZoom();

		assert goalLeftEdge >= 0
				&& goalLeftEdge + visibleWidth() <= goalLogicalWidth : goalLeftEdge
				+ "/" + goalLogicalWidth + " " + visibleWidth();

		zoomer = new PInterpolatingActivity(Bungee.rankAnimationMS,
				Bungee.rankAnimationStep) {
			final double startLeftEdge = leftEdge;
			final double startLogicalWidth = logicalWidth;

			public void activityFinished() {
				zoomer = null;
				super.activityFinished();
			}

			public void setRelativeTargetValue(float zeroToOne) {
				// Util.print("animatePanZoom " + zeroToOne);
				double newLeftEdge = Util.interpolate(startLeftEdge,
						goalLeftEdge, zeroToOne);
				double newLogicalWidth = Util.interpolate(startLogicalWidth,
						goalLogicalWidth, zeroToOne);
				try {
					setLogicalBounds(newLeftEdge, newLogicalWidth);
				} catch (AssertionError e) {
					Util.err("setRelativeTargetValue " + zeroToOne + " "
							+ startLeftEdge + "/" + startLogicalWidth + " "
							+ goalLeftEdge + "/" + goalLogicalWidth + " "
							+ visibleWidth());
					e.printStackTrace();
				}
			}
		};
		addActivity(zoomer);
	}

	void finishPanZoom() {
		if (zoomer != null)
			zoomer.terminate(0);
	}

	void setBarTransparencies(float zeroToOne) {
		for (Iterator it = barTable.values().iterator(); it.hasNext();) {
			Bar bar = ((Bar) it.next());
			bar.setTransparency(zeroToOne);
		}
	}

	// Called only by Rank.redraw.
	void validate(int _visibleWidth, boolean isShowRankLabels) {
		// Util.print("pv.validate " + p + " " + p.isPrefetched());
		if (p.isPrefetched() || p.getParent() == null) {
			if (p.getTotalChildTotalCount() == 0)
				// We're in the process of removing this perspective,
				// which will be done by updateData.
				return;

			// Util.print("pv.validate " + p + " " + visibleWidth + " "
			// + p.getTotalCount() + " " + p.getTotalChildTotalCount());
			setPercentLabelVisible();

			front.setWidth(_visibleWidth);
			// setWidth(_visibleWidth);
			w = _visibleWidth;
			// visibleWidth = _visibleWidth - epsilon;
			resetLogicalBounds();
			layoutLightBeam();
			rankLabel.setVisible(isShowRankLabels);
			if (!p.isPrefetched()) {
				queueDrawLetters();
			}
		} else {
			queuePrefetch();
		}
	}

	void revalidate() {
		// Util.print("pv.revalidate " + p + " " + leftEdge + " " + w
		// + "/" + logicalWidth);
		if (visibleWidth() > 0) {
			assert logicalWidth > 0 : p + " " + leftEdge + "/" + logicalWidth;
			// front.setBounds(0, 0, (int) (rightEdge - leftEdge), 1);
			drawBars();
			drawLabels();
			if (medianArrow != null) {
				// Do this after drawBars, as offset computation uses bar
				// offsets
				double logicalVisibleOffset = logicalWidth / 2 - leftEdge;
				if (logicalVisibleOffset >= 0 && logicalVisibleOffset < w) {
					medianArrow.setOffset(logicalVisibleOffset, 1.0);
				} else
					medianArrow.removeFromParent();
				if (query().isQueryValid())
					layoutMedianArrow();
			}
		}
	}

	void setFeatures() {
		facetPTexts.clear();
		if (medianArrow != null && p.guessOnCount() > 0) {
			if (art().getShowMedian())
				front.addChild(medianArrow);
			else
				medianArrow.removeFromParent();
		}
		drawLabels();
		if (art().getShowZoomLetters()) {
			drawLetters();
		} else if (letters != null) {
			letters.removeFromParent();
			letters = null;
		}
	}

	void queuePrefetch() {
		queue(getDoValidate());
	}

	void queue(Runnable runnable) {
		Query q = query();
		q.unqueuePrefetch(runnable);
		q.queuePrefetch(p);
		q.queuePrefetch(runnable);
	}

	void queueDrawLetters() {
		queue(getDoDrawLetters());
	}

	void setPercentLabelVisible() {
		if (percentLabels != null) {
			boolean isShowRankLabels = this == rank.perspectives[0];
			// percentScale.setVisible(isShowRankLabels);
			percentLabels[0].setVisible(isShowRankLabels);
			percentLabels[1].setVisible(isShowRankLabels);
			percentLabels[2].setVisible(isShowRankLabels
			// && rank.expectedPercentOn() < 1
					);
			// hotLine.setWidth(w * summary.selectedFrontH);
		}
	}

	private transient Runnable doValidate;

	Runnable getDoValidate() {
		if (doValidate == null)
			doValidate = new Runnable() {

				public void run() {
					rank.validateInternal();
				}
			};
		return doValidate;
	}

	private transient Runnable doDrawLetters;

	Runnable getDoDrawLetters() {
		if (doDrawLetters == null)
			doDrawLetters = new Runnable() {

				public void run() {
					drawLetters();
				}
			};
		return doDrawLetters;
	}

	void layoutMedianArrow() {
		if (p.isPrefetched()) {
			if (medianArrow.unconditionalMedian == null) {
				medianArrow.unconditionalMedian = p.getMedianPerspective(false);
			}
			double median = p.median(true);
			if (median >= 0.0) {
				int medianIndex = (int) median;
				double childFraction = median - medianIndex;
				Perspective medianChild = p.getNthChild(medianIndex);
				medianArrow.conditionalMedian = medianChild;
				Bar bar = (Bar) barTable.get(medianChild);
				while (bar == null && medianIndex < p.nChildren() - 1) {
					childFraction = 0.0;
					bar = (Bar) barTable.get(p.getNthChild(++medianIndex));
				}
				// Util.print("layoutMedianArrow " + medianChild + " "
				// + childFraction + " " + bar);
				if (bar != null) {
					int left = minBarPixelRaw(medianChild);
					int right = maxBarPixelRaw(medianChild);
					double x = Util.interpolate(left, right,
							(float) childFraction);
					if (x < 0 || x > visibleWidth())
						medianArrow.removeFromParent();
					// double x = bar.getXOffset() + childFraction *
					// bar.getWidth();
					double length = x - medianArrow.getXOffset();
					medianArrow.setLengthAndDirection((int) length);
					// Color color = null;
					medianArrow.updateColor(p.medianTestSignificant());
					// Util.print("median: " + p + " " + median + " " + bar + "
					// "
					// + x + " " + length);
					medianArrow.moveToFront();
				} else {
					medianArrow.removeFromParent();
				}
			}
		}
	}

	// called as a result of changing deselectedFrontH, etc
	void layoutPercentLabels() {
		if (isConnected()) {
			// double x = Math.round(-percentLabelW * PERCENT_LABEL_SCALE);

			double frontH = summary.selectedFrontH();
			double yOffset = -PERCENT_LABEL_SCALE * art().lineH / 2.0 / frontH;
			// Util.print("percent y offset = " + yOffset);
			double x = percentLabels[0].getXOffset();
			double scaleY = PERCENT_LABEL_SCALE / frontH;
			percentLabels[0].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, 1.0 + yOffset));
			percentLabels[1].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, 0.5 + yOffset));
			percentLabels[2].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, yOffset));
			// scaleMedianArrow();
			// edu.cmu.cs.bungee.piccoloUtils.gui.Util.printDescendents(
			// percentLabels[2]);
		}
	}

	void scaleMedianArrow() {
		if (medianArrow != null) {
			double scaleY = 1.0 / front.goalYscale; // 1.0 /
			// summary.selectedFrontH;
			medianArrow.setTransform(Util.scaleNtranslate(1.0, scaleY,
					medianArrow.getXOffset(), 1.0)); // -
			// medianArrow.getHeight()
			// * scaleY / 2.0));
		}
	}

	APText tempRatioLabel(double oddsRatio) {
		double frontH = summary.selectedFrontH();
		double offset = -art().lineH / 2.0 / frontH;
		double scaleY = PERCENT_LABEL_SCALE / frontH;
		double y = 1.0 - Rank.warp(oddsRatio);
		percentLabels[1].setTransform(Util.scaleNtranslate(PERCENT_LABEL_SCALE,
				scaleY, percentLabels[1].getXOffset(), y + offset));
		percentLabels[1].setText(formatOddsRatio(oddsRatio));
		percentLabels[1].setVisible(true);
		return percentLabels[1];
	}

	void hotZone(double y) {
		double effectiveY = 1.0 - y;
		// if (rank.warpPower() == 1.0) {
		// y = Math.max(y, 0.5);
		// effectiveY = 2.0 - 2.0 * y;
		// }
		double oddsRatio = Rank.unwarp(effectiveY);
		// Util.print("hotZone " + y + " " + rank.expectedPercentOn() + " " +
		// rank.warpPower() + " " + percent);
		double frontH = summary.selectedFrontH();
		double offset = -art().lineH / 2.0 / frontH;
		double scaleY = PERCENT_LABEL_SCALE / frontH;
		percentLabels[1].setTransform(Util.scaleNtranslate(PERCENT_LABEL_SCALE,
				scaleY, percentLabels[1].getXOffset(), y + offset));
		percentLabels[1].setText(formatOddsRatio(oddsRatio));
		hotLine.setVisible(true);
		hotLine.moveToFront();
		hotLine.setScale(1 / frontH);
		hotLine.setBounds(0, (int) (y * frontH), (int) ((percentLabelHotZone
				.getWidth() + w) * frontH), 1);
		hotZonePopup.setVisible(true);
		String msg = (oddsRatio > 0.666666666) ? Math.round(oddsRatio)
				+ " times as likely as others" : "1 / "
				+ Math.round(1.0 / oddsRatio) + " as likely as others";
		hotZonePopup.setText(msg);
		hotZonePopup.moveToFront();
		// Util.print(hotLine.getGlobalBounds());
		// gui.Util.printDescendents(hotLine);
	}

	static String formatOddsRatio(double ratio) {
		if (ratio == Double.POSITIVE_INFINITY)
			return "* Infinity";
		int iRatio = (int) Math.round(ratio > 1.0 ? ratio : 1.0 / ratio);
		String result = iRatio == 1 ? "=" : (ratio > 1.0 ? "* " : "/ ")
				+ iRatio;
		return result;
	}

	void loseHotZone() {
		double frontH = summary.selectedFrontH();
		double offset = -art().lineH / 2.0 / frontH;
		double scaleY = PERCENT_LABEL_SCALE / frontH;
		percentLabels[1].setTransform(Util.scaleNtranslate(PERCENT_LABEL_SCALE,
				scaleY, percentLabels[1].getXOffset(), 0.5 + offset));
		percentLabels[1].setText(formatOddsRatio(1.0));
		hotLine.setVisible(false);
		hotZonePopup.setVisible(false);
	}

	// double getFoldH() {
	// double h = getHeight();
	// double frontH = Util.min(h, summary.perspectiveSelectedFrontH);
	// double extra = h - frontH;
	// double ratio = summary.foldH / summary.perspectiveLabelH;
	// double texth = extra / (1.0 + ratio);
	// double foldH = h - frontH - texth;
	// return foldH;
	// }

	public void layoutChildren() {
		if (logicalWidth > 0) { // Make sure we've been initialized.
			// double h = getHeight();
			if (!rank.componentHeights.equals(prevComponentHeights) /*
																	 * ||
																	 * Math.abs
																	 * (h -
																	 * prevH) >
																	 * 1.0
																	 */) {
				// assert rightEdge - leftEdge == getWidth();
				// Util.print("pv.layoutChildren " + p + " " + h + " "
				// + rank.foldH() + " " + rank.frontH());
				prevComponentHeights = rank.componentHeights;
				prevH = h;
				double foldH = rank.foldH();
				front.layout(foldH, rank.frontH());
				scaleMedianArrow();
				double yScale = rank.labelsH();
				if (yScale > 0)
					// Avoid division by zero
					yScale /= summary.selectedLabelH();
				labels.layout(foldH + rank.frontH(), yScale);
				layoutLetters();
				layoutRankLabel(false);
				layoutPercentLabels();
				if (!areLabelsInited())
					drawLabels(); // In case labels just now became visible
				layoutLightBeam();
				// If a child has a light beam and we are connected, and then
				// you connect to something above us,
				// child bounds won't change, but the light beam needs to.
				PerspectiveViz[] children = getChildPVs();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						children[i].layoutLightBeam();
					}
				}
			}
		}
	}

	void layoutLetters() {
		if (letters != null) {
			double foldH = rank.foldH();
			letters.setOffset(0, foldH);
			letters.setHeight(foldH);
			letters.setY(-foldH);
			letters.setVisible(labels.getVisible());
		}
	}

	void layoutRankLabel(boolean contentChanged) {
		// Util.print("layoutRankLabel " + p + " " + rankLabel.getVisible() + "
		// "
		// + art().query.isQueryValid());
		if (rankLabel.getVisible() && rank.frontH() > 0) {
			double xScale = 1.0;
			double yScale = 1.0 / rank.frontH();
			double labelH = rank.frontH() + rank.labelsH();
			double lineH = art().lineH;
			if (labelH < lineH) {
				xScale = labelH / lineH;
				yScale *= xScale;
				labelH = lineH;
			} else
				labelH = lineH * ((int) (labelH / lineH));
			boolean boundsChanged = rankLabel.setBounds(0, 0, Math.floor(rank
					.rankLabelWdith()
					/ xScale), labelH);
			rankLabel.setTransform(Util.scaleNtranslate(xScale, yScale,
					rankLabel.getXOffset(), 0.0));
			if (contentChanged || boundsChanged) {
				rankLabel.setHeight(labelH);
				rankLabel.setWrapOnWordBoundaries(labelH > lineH);
				rankLabel.layoutBestFit();
				// Noone else should call layout, because notifier will be
				// lost!!!
				rank.hackRankLabelNotifier();
			}
		}
	}

	// /**
	// * Add this PerspectiveViz as the pickFacetTextNotifier of all rankLabel
	// * FacetTexts with an actual facet.
	// */
	// void hackRankLabelNotifier() {
	// // Util.print("hackRankLabelNotifier " + rankLabel.getChildrenCount());
	// for (int i = 0; i < rankLabel.getChildrenCount(); i++) {
	// FacetText ft = (FacetText) rankLabel.getChild(i);
	// if (ft.getFacet() == p) {
	// ft.pickFacetTextNotifier = this;
	// // art().validatePerspectiveList();
	// // break;
	// }
	// }
	// }

	// // Redrawer for rankLabel
	// public void redraw() {
	// layoutRankLabel(true);
	// }
	//
	// void draw(boolean redrawOnly) {
	// // Util.print("pv.draw " + p + " " + redrawOnly);
	// if (w > 0) {
	// // Don't do redraws before we get drawn.
	// drawBars(redrawOnly);
	// redrawLabels();
	// }
	// }
	//
	// void redrawLabels() {
	// loseLabels();
	// drawLabels();
	// }

	/**
	 * [top, bottom, topLeft, topRight, bottomLeft, bottomRight]
	 */
	private float[] prevLightBeamCoords = null;

	/**
	 * Draw the light beam shining from parentPV to this pv.
	 * 
	 * The shape has to change shape during animation, so is called in
	 * layoutChildren rather than draw.
	 */
	void layoutLightBeam() {
		if (parentPV != null) {
			Bar parentFrontRect = parentPV.lookupBar(p);
			if (parentFrontRect != null) {
				// If we select a range of parent tags, some might be so small
				// that there's not a bar for it.

				PBounds gBottomRect = front.getGlobalBounds();
				if (gBottomRect.getWidth() > 0) {
					float[] newCoords = new float[6];
					Rectangle2D lBottomRect = rank.globalToLocal(gBottomRect);
					newCoords[1] = (float) (lBottomRect.getY()
					// + front .getGlobalBounds().getHeight() / 2
					);
					newCoords[4] = (float) lBottomRect.getX();
					newCoords[5] = newCoords[4]
							+ (float) lBottomRect.getWidth();

					PBounds gTopRect = parentFrontRect.getGlobalBounds();
					Rectangle2D lTopRect = rank.globalToLocal(gTopRect);
					newCoords[0] = (float) (lTopRect.getY() + lTopRect
							.getHeight());
					newCoords[2] = (float) lTopRect.getX();
					newCoords[3] = newCoords[2] + (float) lTopRect.getWidth();

					if (!Arrays.equals(newCoords, prevLightBeamCoords)) {
						prevLightBeamCoords = newCoords;
						if (lightBeam == null) {
							lightBeam = new LazyPPath();
							lightBeam.setPickable(false);
							lightBeam.setStroke(null);
							updateLightBeamTransparency();
							rank.addChild(lightBeam);
						} else {
							lightBeam.reset();
							// lightBeam.moveToFront();
						}
						float[] Xs = { newCoords[4], newCoords[2],
								newCoords[3], newCoords[5], newCoords[4] };
						float[] Ys = { newCoords[1], newCoords[0],
								newCoords[0], newCoords[1], newCoords[1] };
						lightBeam.setPathToPolyline(Xs, Ys);
					}
				}
			} else if (lightBeam != null) {
				lightBeam.removeFromParent();
				// rank.removeChild(lightBeam);
				lightBeam = null;
			}
		}
	}

	PerspectiveViz[] getChildPVs() {
		return summary.getChildPVs(this);
	}

	// void initFold() {
	// // Util.print("initFold " + p.getName());
	// assert fold != null;
	// float center = (int) (w / 2.0);
	// Enumeration it = barTable.elements();
	// while (it.hasMoreElements()) {
	// Bar bar = (Bar) it.nextElement();
	// bar.initFold(fold, center);
	// }
	// assert percentLabels != null;
	// // fold.addChild(hLine);
	// // fold.addChild(percentLabels[2]);
	// // fold.addChild(percentLabels[3]);
	// }

	// private boolean foldInitted() {
	// return fold.getChildrenCount() > 0;
	// }

	static final boolean VERIFY_BAR_TABLES = true;

	boolean verifyBarTables() {
		if (VERIFY_BAR_TABLES) {
			Perspective prevFacet = barXs[0];
			assert prevFacet != null : printBarXs();
			boolean lastEmpty = false;
			for (int i = 1; i < barXs.length; i++) {
				if (barXs[i] == null)
					lastEmpty = true;
				else if (barXs[i] == prevFacet)
					// next x must have the next facet
					lastEmpty = false;
				else {
					assert lastEmpty == false : i + printBarXs();
					// starting new bar
					prevFacet = barXs[i];
				}
			}
			assert lastEmpty == false : printBarXs();
		}
		return true;
	}

	String printBarXs() {
		Util.print(p + " has " + barTable.size() + " bars over " + barXs.length
				+ " pixels.");
		Util.printDeep(barXs);
		ItemPredicate prevFacet = barXs[0];
		int facetStart = 0;
		for (int i = 1; i < barXs.length; i++) {
			Perspective facet = barXs[i];
			if (facet != null) {
				if (facet == prevFacet)
					i++;
				else if (facetStart != i - 1)
					Util.print("\nunterminated:");
				Util.print(prevFacet + ": " + facet.getTotalCount() + " "
						+ facet.cumCountExclusive() + "-"
						+ facet.cumCountInclusive() + " [" + facetStart + ", "
						+ (i - 1) + "]");
				prevFacet = facet;
				facetStart = i;
			}
		}
		return "";
	}

	private void setTextSize() {
		int projectionW = (int) (1.1 * art().lineH);
		if (projectionW != labelHprojectionW) {
			labelHprojectionW = projectionW;

			if (percentLabels != null) {
				double percentLabelW = art().getStringWidth("/ 100+");
				double percentLabelScaledW = percentLabelW
						* PERCENT_LABEL_SCALE;
				double x = Math.round(-percentLabelScaledW);
				for (int i = 0; i < 3; i++) {
					percentLabels[i].setWidth(percentLabelW);
					percentLabels[i].setXoffset(x);
				}

				// Make hot zone cover 100%, rather than up to 0.001%
				percentLabelScaledW = Math.round(percentLabelScaledW * 0.67);
				percentLabelHotZone.setBounds(-percentLabelScaledW, 0,
						percentLabelScaledW, 1);
				hotZonePopup.setOffset(percentLabelHotZone.getX(), summary
						.selectedFoldH()
						- 1.5 * hotZonePopup.getHeight());
			}
		}
	}

	double percentLabelW() {
		if (percentLabels == null)
			return 0;
		else
			return percentLabels[0].getWidth() * percentLabels[0].getScale();
	}

	private void drawBars() {
		// Util.print("drawBars " + p + " " + redrawOnly);
		assert p.getTotalChildTotalCount() > 0 : p;
		front.removeAllChildren();
		front.addChild(rankLabel);
		setTextSize();
		if (percentLabels != null) {
			// front.addChild(percentScale);
			for (int i = 0; i < 3; i++) {
				percentLabels[i].setFont(art().font);
				front.addChild(percentLabels[i]);
			}
			front.addChild(percentLabelHotZone);
			front.addChild(hotLine);
			// front.addChild(parentRect);
		}
		if (medianArrow != null && art().getShowMedian()) {
			front.addChild(medianArrow);
		}
		computeBars();
	}

	int maybeDrawBar(Perspective facet, double divisor, boolean forceDraw) {
		// if (forceDraw) {
		// Util.print("maybeDrawBar " + facet + " " + lookupBar(facet));
		// }
		assert facet.getParent() == p;
		int barW = 0;
		if (visibleWidth() > 0 && facet.getTotalCount() > 0
				&& (!forceDraw || lookupBar(facet) == null)) {
			// if !forceDraw, we're drawing all new bars. No need to check
			// table.
			assert p.getTotalChildTotalCount() >= facet.getTotalCount() : facet
					+ " " + facet.getTotalCount() + "/"
					+ p.getTotalChildTotalCount() + " "
					+ query().isQueryValid();
			double minX = Math.max(leftEdge, facet.cumCountExclusive()
					* divisor)
					- leftEdge;
			double maxX = Math.min(visibleWidth(), facet.cumCountInclusive()
					* divisor - leftEdge);
			assert minX >= 0 && maxX < w && maxX >= minX : "shouldn't try to draw this bar "
					+ facet
					+ " "
					+ minX
					+ "-"
					+ maxX
					+ " "
					+ leftEdge
					+ " "
					+ w + "/" + logicalWidth;
			// if (maxX > minX) {
			int iMaxX = (int) maxX;
			int iMinX = (int) minX;
			// if (facet.getID() == 157990)
			// Util.print("maybe draw bar " + p + "." + facet + " " + iMinX
			// + "-" + iMaxX + " " + forceDraw);
			assert facet.cumCountInclusive() <= p.getTotalChildTotalCount() : facet
					+ " "
					+ facet.cumCountInclusive()
					+ "/"
					+ p.getTotalChildTotalCount();
			assert iMaxX < logicalWidth : minX + " " + maxX + " "
					+ logicalWidth;
			// x_mid_coords[datum.whichChild()] = (iMaxX + iMinX) >>> 1;
			assert iMinX >= 0 : facet + " " + facet.cumCountInclusive() + " "
					+ facet.getTotalCount();
			if (barXs[iMinX] != null && barXs[iMinX] != facet) {
				iMinX += 1;
				assert iMinX >= iMaxX || barXs[iMinX] == null
						|| barXs[iMinX] == facet : this + " " + facet + " "
						+ printBarXs() + " " + minX + "-" + maxX;
			}
			if (barXs[iMaxX] != null && barXs[iMaxX] != facet) {
				iMaxX -= 1;
				assert ((int) minX) >= iMaxX || barXs[iMaxX] == null
						|| barXs[iMaxX] == facet : this + " " + facet + " "
						+ printBarXs();
			}
			if (iMinX > iMaxX && forceDraw) {
				// No space. Try to create some.
				// I turned off these first two options because drawing more
				// bars
				// was hurting performance more than it was helping the
				// user.
				// Any bar that has room for a label will be drawn in
				// initLabels;
				if (iMinX > 1
						&& (barXs[iMinX - 2] == null || barXs[iMinX - 2] == barXs[iMinX - 1])) {
					barXs[iMinX - 2] = barXs[iMinX - 1];
					iMinX -= 1;
					iMaxX = iMinX;
				} else if (iMaxX < barXs.length - 2
						&& (barXs[iMaxX + 2] == null || barXs[iMaxX + 2] == barXs[iMaxX + 1])) {
					barXs[iMaxX + 2] = barXs[iMaxX + 1];
					iMaxX += 1;
					iMinX = iMaxX;
				} else if (forceDraw) {
					iMinX = (iMinX > 1) ? iMinX - 1 : iMaxX + 1;
					iMaxX = iMinX;
					Perspective oldFacet = barXs[iMinX];
					Bar oldBar = (Bar) barTable.get(oldFacet);
					assert oldBar != null : p + " " + facet + " " + oldFacet
							+ " " + iMinX + "-" + iMaxX + " " + visibleWidth()
							+ printBarXs();
					Bar.release(oldBar);
					barTable.remove(oldFacet);
					barXs[iMinX] = facet;
					// Util.print("Removing bar " + oldFacet);
				}
			}
			if (iMinX <= iMaxX) {
				barW = iMaxX - iMinX + 1;
				// Util.print("add bar " + facet + " " + iMinX + "-" + iMaxX);
				Bar bar = Bar.getBar(this, iMinX, barW, facet);
				front.addChild(bar);
				if (query().isQueryValid()) {
					double expectedPercentOn = rank.expectedPercentOn();
					if (expectedPercentOn >= 0 && facet.getOnCount() >= 0) {
						// if (foldInitted()) {
						// bar.initFold(fold, w / 2);
						// assert forceDraw : "fold hasn't been initted on
						// initial
						// drawBars";
						// double exp = getExp();
						bar.updateData();
						if (true || !forceDraw)
							bar.animateData(1.0);
						// }
					}
				}
				barTable.put(facet, bar);
				assert validateBarX(facet, iMinX);
				assert validateBarX(facet, iMaxX);
			}
		}
		return barW;
	}

	double barWidthRatio() {
		return logicalWidth / p.getTotalChildTotalCount();
	}

	Bar lookupBar(Perspective facet) {
		Bar result = (Bar) barTable.get(facet);
		// if (result != null)
		// Util.print("lookupBar " + facet + " => " + result.facet);
		return result;
	}

	int nBars() {
		// Util.print("PV.nBars " + barTable.size());
		return barTable.size();
	}

	double[] pValues() {
		double[] result = new double[nBars()];
		int i = 0;
		for (Iterator it = barTable.keySet().iterator(); it.hasNext(); i++) {
			ItemPredicate facet = (ItemPredicate) it.next();
			result[i] = facet.pValue();
			// Util.print(p + " " + result[i]);
			// if (result[i]<0)
			// result[i] = 1.0;
		}
		return result;
	}

	private boolean validateBarX(Perspective datum, int xCoord) {
		double divisor = barWidthRatio();
		double maxX = datum.cumCountInclusive() * divisor;
		double minX = maxX - datum.getTotalCount() * divisor;
		int iMaxX = (int) (maxX - leftEdge);
		int iMinX = (int) (minX - leftEdge);
		assert xCoord >= iMinX && xCoord <= iMaxX : xCoord + " [" + iMinX
				+ ", " + iMaxX + "]";
		return true;
	}

	private FacetPText mouseNameLabel;

	void updateSelection(Set facets) {
		updateLightBeamTransparency();

		drawMouseLabel();

		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			Bar bar = lookupBar(facet);
			if (bar != null) {
				bar.updateSelection();
				// drawLabels();
			}
		}
		if (labels != null && labels.getVisible()) {
			for (int i = 0; i < labels.getChildrenCount(); i++) {
				FacetPText label = (FacetPText) labels.getChild(i);
				Perspective labelFacet = label.getFacet();
				if (labelFacet != null && facets.contains(labelFacet))
					label.setColor();
			}
		}
	}

	void updateLightBeamTransparency() {
		if (lightBeam != null) {
			// Util.print("updateLightBeamTransparency"+lightBeam.getTransparency
			// ());
			lightBeam
					.setPaint(p.isRestriction(true) ? Markup.INCLUDED_COLORS[0]
							: Color.white);
			if (art().highlightedFacets.contains(p))
				lightBeam.setTransparency(0.2f);
			else
				lightBeam.setTransparency(0.15f);
		}
	}

	/**
	 * y coordinate for numeric labels; depends on maxCount.
	 */
	double numW;

	double nameW;

	// void loseLabels() {
	// // Util.print("PV.loseLabels " + p + " " + labelXs);
	// if (labelXs != null) {
	// for (int i = 0; i < labelXs.length; i++)
	// labelXs[i] = null;
	// }
	// labels.removeAllChildren();
	// }

	private void drawLabels() {
		assert SwingUtilities.isEventDispatchThread();
		// Util.print("initLabels " + p + " "
		// + areLabelsInited() + " " + barTable.size() + " " +
		// p.getOnCount());

		// even if we're not connected, make sure stale labels don't come back
		// later. labels.getVisible() is true when the labels SqueezablePNode
		// goalYscale > 0
		labels.removeAllChildren();
		if (labels.getVisible() && barTable.size() > 0
				&& query().isQueryValid()) {
			// If there aren't any bars, x_mid_coords hasn't been initialized.
			// This is a problem inside rank.redraw after addPerspective.
			// Util.print("PV.initLabels " + p);
			if (p.isPrefetched()) {
				// summary.q.prefetchData(p);
				int maxCount = rank.maxCount();
				numW = Math.round(art().numWidth(maxCount) + 10.0);

				// The last term sets a margin between text and next Rank
				nameW = Math.floor(summary.selectedLabelH() * 1.4 - numW - 2
						* art().lineH);

				// int freeLabelXs = w + 2;
				// SortedSet allRestrictions = p.allRestrictions();
				// for (Iterator it = allRestrictions.iterator(); it.hasNext();)
				// {
				// Perspective facet = (Perspective) it.next();
				// maybeDrawLabel(facet);
				// }

				computeLabels();

				// // If sort order was really important we should synchronize
				// p.sortDataIndexByOn();
				// // Util.print("initLabels " + p.sortOrder() + " " + p);
				// for (int i = 0; i < p.nChildren() && freeLabelXs > 0; i++) {
				// Perspective child = p.getNthOnValue(i);
				// // Util.print(child + " " + child.onCount + " " + maxCount);
				// freeLabelXs -= maybeDrawLabel(child);
				// }
				// // Util.print("initLabels done " + p.sortOrder() + " " + p);

				mouseNameLabel = new FacetPText(null, 0.0, -1.0);
				mouseNameLabel.setVisible(false);
				mouseNameLabel.setPickable(false);
				labels.addChild(mouseNameLabel);
			} else {
				queuePrefetch();
			}
			drawMouseLabel();
		}
	}

	private boolean areLabelsInited() {
		return labels.getChildrenCount() > 0;
	}

	// void drawLabels() {
	// if (labels.getVisible()) {
	// // if (p.isPrefetched()) {
	// // Util.print("drawLabels "
	// // + p
	// // + " "
	// // + (mouseNameLabel == null ? false : mouseNameLabel
	// // .getVisible()));
	// initLabels();
	// drawMouseLabel();
	// // } else {
	// // summary.fetcher.add(this);
	// // }
	// }
	// }

	private int prevMidX;

	private void drawMouseLabel() {
		if (areLabelsInited() && labels.getVisible()) {
			Perspective mousedFacet = null;
			for (Iterator it = art().highlightedFacets.iterator(); it.hasNext()
					&& mousedFacet == null;) {
				Perspective facet = (Perspective) it.next();
				if (facet.getParent() == p && isPerspectiveVisible(facet)) {
					// There may be more than one. Tough.
					mousedFacet = facet;
				}
			}
			// Util.print("drawMouseLabel " + p + " " + mousedFacet);
			drawMouseLabelInternal(mousedFacet);
		}
	}

	private void drawMouseLabelInternal(Perspective v) {
		boolean state = v != null;
		int midX = state ? midLabelPixel(v, barWidthRatio()) : prevMidX;
		int iVisibleWidth = (int) visibleWidth();
		// Util.print("drawMouseLabelInternal " + p + " " + v + " " + state + "
		// " + midX);
		if (midX >= 0 && midX <= iVisibleWidth) {
			prevMidX = midX;

			int minX = Util.constrain(midX - labelHprojectionW, 0,
					iVisibleWidth);
			int maxX = Util.constrain(midX + labelHprojectionW, 0,
					iVisibleWidth);
			for (int i = minX; i <= maxX; i++) {
				FacetText label = findLabel(labelXs[i]);
				if (label != null) {
					int midLabel = midLabelPixel(labelXs[i], barWidthRatio());
					if (Math.abs(midLabel - midX) <= labelHprojectionW) {
						// Util.print("need mouseLabel? " + p + " " + v + " " +
						// " "
						// + labelXs[i].getFacet() + " "
						// + labelXs[i].getVisible() + " "
						// + mouseNameLabel.getVisible());
						if (labelXs[i] == v) {
							label.setVisible(true);
							mouseNameLabel.setVisible(false);
							mouseNameLabel.setPickable(false);
							return;
						} else
							label.setVisible(v == null);
					}
				}
			}

			if (state) {
				maybeDrawBar(v, barWidthRatio(), true);
				assert verifyBarTables();
				mouseNameLabel.setFacet(v);
				mouseNameLabel.setPTextOffset(midX, 0.0);
				labels.moveAheadOf(front);
			} else {
				front.moveAheadOf(labels);
			}
			mouseNameLabel.setVisible(state);
			mouseNameLabel.setPickable(state);
		}
	}

	FacetText findLabel(Perspective facet) {
		if (facet != null) {
			for (int i = 0; i < labels.getChildrenCount(); i++) {
				FacetText child = (FacetText) labels.getChild(i);
				if (child.facet == facet)
					return child;
			}
		}
		return null;
	}

	private int maybeDrawLabel(Perspective v) {
		int nPixelsOccluded = 0;
		if (v.getTotalCount() > 0) {
			int iVisibleWidth = (int) visibleWidth();
			int midX = midLabelPixel(v, barWidthRatio());
			assert midX >= 0 && midX < iVisibleWidth : v + " " + midX + " "
					+ iVisibleWidth;
			// Util.print("maybeDrawLabel " + v + " " + midX + " "
			// + minBarPixel(v, barWidthRatio()) + "-"
			// + maxBarPixel(v, barWidthRatio()));
			if (labelXs[midX] == null || labelXs[midX] == v) {
				maybeDrawBar(v, barWidthRatio(), true);
				assert verifyBarTables();
				if (lookupBar(v) != null) {
					FacetPText label = getFacetPText(v, 0.0, midX);
					labels.addChild(label);

					int minX = Util.constrain(midX - labelHprojectionW, 0,
							iVisibleWidth);
					int maxX = Util.constrain(midX + labelHprojectionW, 0,
							iVisibleWidth);
					for (int i = minX; i <= maxX; i++) {
						if (labelXs[i] == null)
							nPixelsOccluded++;
						labelXs[i] = v;
					}
					// Util.print("maybeDrawLabel " + v + " " + minX + " - " +
					// maxX);
				}
			}
		}
		return nPixelsOccluded;
	}

	boolean isConnected() {
		return rank.isConnected();
	}

	void connectToPerspective() {
		rank.connect();
	}

	void setConnected(boolean connected) {
		// if (!connected)
		// edu.cmu.cs.bungee.piccoloUtils.gui.Util.printDescendents(letters);
		// Util.print("PV.setConnected " + p.getName() + " " + connected);
		if (rankLabel.getVisible()) {
			float transparency = connected ? 1 : 0;
			for (int i = 0; i < 3; i++) {
				percentLabels[i].animateToTransparency(transparency,
						Bungee.dataAnimationMS);
			}
			// if (query().isQueryValid())
			// redraw100PercentLabel();
			percentLabelHotZone.setPickable(connected);
			rankLabel.setPickable(connected);
			resetLogicalBounds();
		}
	}

	// // Visibiility is determined by whether we are rank's first perspective
	// (in
	// // validate).
	// // and also by parent's visibility (the fold).
	// // Transparency and position are determined by curtainH
	// void redraw100PercentLabel() {
	// if (isConnected() && rank.totalChildTotalCount() >= 0) {
	// // Util.print("PV.redrawPercents " + p.getName() + " " + curtainH +
	// // " "
	// // + (Art.lineH / summary.foldH));
	// // assert curtainH <= 1;
	// // double foldH = summary.selectedFoldH;
	// // double lineH = summary.art.lineH;
	// double percent = rank.expectedPercentOn();
	// // percentLabels[1].setText(ResultsGrid.formatPercent(percent, null)
	// // .toString());
	// // percentLabels[1].setVisible(percent < 1);
	// }
	// }

	// void drawPercentScale() {
	// double w = percentLabelW;
	// // double exp = 1 / getExp();
	// float[] Xs = new float[12];
	// float[] Ys = new float[12];
	// int i = 0;
	// for (double y = 0; y <= 1; y += 0.1) {
	// double percent = rank.unwarp(1 - y);
	// Ys[i] = (float) (rank.warpPower() == 1 ? y / 2 + 0.5 : y);
	// Xs[i] = (float) (w * (1 - percent));
	// i++;
	// }
	// Xs[11] = Xs[10];
	// Ys[11] = Ys[0];
	// // Util.print("");
	// // Util.printDeep(Xs);
	// // Util.printDeep(Ys);
	// // gui.Util.printDescendents(percentScale);
	// percentScale.setPathToPolyline(Xs, Ys);
	// }

	// private void updatePercentLabel(APText label, float transparency, double
	// x,
	// double y) {
	// // label.moveToFront();
	// // if (transparency == 1)
	// // Util.print("updatePercentLabel " + p + " " + y);
	// boolean shouldAnimate = (isConnected() || label.getParent() != null
	// && label.getParent().getVisible())
	// && (transparency > 0 || label.getTransparency() > 0);
	// if (shouldAnimate) {
	// if (x != label.getXOffset() || y != label.getYOffset())
	// label.animateToPositionScaleRotation(x, y, percentLabelScale,
	// 0, Art.dataAnimationMS);
	// if (transparency != label.getTransparency())
	// label.animateToTransparency(transparency, Art.dataAnimationMS);
	// } else {
	// label.setOffset(x, y);
	// label.setTransparency(transparency);
	// }
	// }

	// boolean isRestricted() {
	// return p.isRestricted();
	// }

	// String[] getRestrictionNames(boolean isLocalOnly) {
	// return p.getRestrictionNames(isLocalOnly);
	// }

	// Called by Bar.highlight
	void highlightFacet(Perspective facet, int modifiers, PInputEvent e) {
		// Util.print("PV.highlightFacet " + facet);
		if (art().getIsEditing() && e.isRightMouseButton()) {
			art().setClickDesc("Set selected for edit");
		} else if (art().getIsEditing() && e.isMiddleMouseButton()) {
			art().setClickDesc("Open edit menu");
		} else if (isConnected() || facet == null) {
			art()
					.setClickDesc(
							facet != null ? facet.facetDoc(modifiers) : null);
		} else {
			highlight(facet, modifiers);
		}
		art().highlightFacet(facet, modifiers);
	}

	// Called as a result of pickFacetTextNotifier on rank label FacetTexts
	public boolean pick(FacetText node, int modifiers) {
		// Util.print("PV.pick " + p + " " + node + " " + modifiers + " "
		// + node.isPickable);
		return pickFacet(node.getFacet(), modifiers);
	}

	// Called as a result of pickFacetTextNotifier on rank label FacetTexts
	boolean pickFacet(Perspective facet, int modifiers) {
		// Skip if over checkboxes and therefore modifiers != 0 (should not
		// happen for top-level ranks)
		boolean handle = isHandlePickFacetText(facet, modifiers);
		// Util.print("PV.pick " + p + "." + facet + " " + modifiers + " "
		// + handle);
		if (handle) {
			art().printUserAction(Bungee.RANK_LABEL, facet, modifiers);
			if (!isConnected()) {
				connectToPerspective();
			} else {
				if (art().arrowFocus != null
						&& art().arrowFocus.getParent() == p)
					facet = art().arrowFocus;
				else if (p.nRestrictions() > 0)
					facet = (Perspective) p.allRestrictions().first();
				else
					facet = p.getNthChild(0);
				summary.togglePerspectiveList(facet);
			}
		}
		return handle;
	}

	// // Called as a result of pickFacetTextNotifier on rank label FacetTexts
	// public boolean mouseMoved(FacetText node, int modifiers) {
	// // Avoid calling updateItemPredicateClickDesc
	// return highlight(node, true, modifiers);
	// }
	//
	// // Called as a result of pickFacetTextNotifier on rank label FacetTexts
	// public boolean shiftKeysChanged(FacetText node, int modifiers) {
	// // Avoid calling updateItemPredicateClickDesc
	// return highlight(node, true, modifiers);
	// }

	// Called as a result of pickFacetTextNotifier on rank label FacetTexts
	public boolean highlight(FacetText node, boolean state, int modifiers) {
		Perspective facet = state ? node.getFacet() : null;
		return highlight(facet, modifiers);
	}

	boolean isHandlePickFacetText(Perspective facet, int modifiers) {
		return facet != null
				&& (!Util.isAnyShiftKeyDown(modifiers) && facet == p || !isConnected());
	}

	boolean highlight(Perspective facet, int modifiers) {
		boolean handle = isHandlePickFacetText(facet, modifiers);
		// Util.print("PV.highlight " + modifiers + " " + handle);
		if (handle) {

			// Highlight the facet, but we'll do our own mouse doc.
			art().highlightFacet(facet, modifiers);

			Markup doc = Query.emptyMarkup();
			if (!isConnected()) {
				doc.add("open category ");
				doc.add(p);
			} else if (art().getShowTagLists()) {
				if (summary.perspectiveList == null
						|| summary.perspectiveList.isHidden()) {
					doc.add("List all ");
					doc.add(p);
					doc.add(" tags");
				} else
					doc.add("Hide the list of tags");
			} else
				handle = false;
			if (handle)
				art().setClickDesc(doc);
		}
		return handle;
	}

	double frontBottomOffset() {
		return front.getYOffset()
				+ Math.min(Math.round(front.getFullBounds().getHeight()), rank
						.getHeight());
	}

	void hidePvTransients() {
		summary.mayHideTransients();
	}

	Hashtable facetPTexts = new Hashtable();

	FacetPText getFacetPText(Perspective _facet, double _y, double x) {
		FacetPText label = null;
		if (_facet != null)
			label = (FacetPText) facetPTexts.get(_facet);
		if (label == null || label.numW != numW || label.nameW != nameW) {
			label = new FacetPText(_facet, _y, x);
			if (_facet != null)
				facetPTexts.put(_facet, label);
		} else {
			label.setPTextOffset(x, _y);
			((APText) label).setText(label.art.facetLabel(_facet, numW, nameW,
					false, true, label.showCheckBox, true, label));
			label.setColor();
		}
		return label;
	}

	final class FacetPText extends FacetText {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		void setFacet(Perspective _facet) {
			facet = _facet;
			assert facet.getParent() != null;
			((APText) this).setText(art.facetLabel(facet, numW, nameW, false,
					showChildIndicator, showCheckBox, true, this));

		}

		// should be FacetText.setFacet
		void setPTextOffset(double x, double y) {
			// Util.print("FacetPText init " + facet);
			setColor();

			double offset = getWidth() / 1.4;
			x = Math.round(x - offset + (y + art.lineH) / 1.4 - 0.85
					* art.lineH);
			double _y = Math.round(y / 1.4 + offset);
			setOffset(x, _y);
		}

		FacetPText(Perspective _facet, double _y, double x) {
			super(summary.art, PerspectiveViz.this.numW,
					PerspectiveViz.this.nameW);
			setRotation(-Math.PI / 4.0);
			// setPaint(Art.summaryBG);

			showCheckBox = art.getShowCheckboxes();
			showChildIndicator = true;
			// isPickable = showCheckBox;
			setUnderline(true /* isPickable */);
			if (_facet != null) {
				setFacet(_facet);
				setPTextOffset(x, _y);
				// if (facet == art.highlightedFacet) {
				// // MouseLabel does highlighting for these, so turn it off
				// // for base label.
				// if (_facet.isRestriction())
				// setTextPaint(Art.whites[1]);
				// else
				// setTextPaint(Art.whites[0]);
				// }
			}
			// addInputEventListener(Art.facetClickHandler);
		}

		public boolean pick(int modifiers, PInputEvent e) {
			// Util.print("FacetPNode.pick " + e.getPosition() + " " +
			// e.getPositionRelativeTo(this));
			// int modifiers = e.getModifiers();
			// double x = e.getPositionRelativeTo(this).getX();
			// if (x < art.checkBoxWidth) {
			// modifiers |= InputEvent.CTRL_DOWN_MASK;
			// }
			// // Util.print(p.getFacetTypeName() + " facet picked: " + facet +
			// " "
			// // + facet.getName());
			// art.printUserAction(Art.BAR_LABEL, facet, modifiers);
			if (isConnected()) {
				super.pick(modifiers, e);
				// toggleFacet(facet, modifiers);
				// p.art.printUserAction("FacetPText.picked: " +
				// p.summary.q.getFacetName(facet));
			} else
				connectToPerspective();
			return true;
		}

		void printUserAction(int modifiers) {
			art.printUserAction(Bungee.BAR_LABEL, facet, modifiers);
		}

		public boolean highlight(boolean state, int modifiers, PInputEvent e) {
			// Util.print("PV.FacetPText.highlight " + p + "."
			// + facet + " " + state + " "
			// + (art.highlightedFacet==null ? null :
			// art.highlightedFacet) + " " + mouseNameLabel.getPickable());
			Point2D mouseCoords = e.getPositionRelativeTo(this);
			// Util.print(mouseCoords);
			double x = mouseCoords.getX();
			double _y = mouseCoords.getY();
			modifiers = getModifiersInternal(modifiers, x);
			boolean isHighlighted = art.highlightedFacets.contains(facet);
			if (state) {
				// Workaround Piccolo rotated-selection bug by
				// skipping redundant calls and checking for erroneous ones.
				if (!isHighlighted) {
					if (x >= -5.0 && x <= w + 5 && _y >= -5.0
							&& _y <= getHeight() + 5.0) {
						highlightInternal(state, modifiers);
					}
				}
			} else if (isHighlighted) {
				highlightInternal(state, modifiers);
			}
			return true;
		}
	}

	// public boolean pick(FacetText node, int modifiers) {
	// boolean result = !isConnected();
	// if (result)
	// connectToPerspective();
	// return result;
	// }
	//
	// public boolean highlight(FacetText node, boolean state, int modifiers,
	// PInputEvent e) {
	// Point2D mouseCoords = e.getPositionRelativeTo(this);
	// // Util.print(mouseCoords);
	// double x = mouseCoords.getX();
	// double _y = mouseCoords.getY();
	// modifiers = node.getModifiersInternal(modifiers, x);
	// if (state) {
	// // Workaround Piccolo rotated-selection bug by
	// // skipping redundant calls and checking for erroneous ones.
	// if (art.highlightedFacet != node.facet) {
	// if (x >= -5.0 && x <= getWidth() + 5 && _y >= -5.0
	// && _y <= getHeight() + 5.0) {
	// return false;
	// }
	// }
	// } else if (art.highlightedFacet == node.facet) {
	// return false;
	// }
	// return true;
	// }
	//
	// Bar findFrontRect(Perspective facet) {
	// Bar result = null;
	// Bar bar = lookupBar(facet);
	// if (bar != null)
	// result = bar;
	// return result;
	// }

	/**
	 * Only called by replayOps
	 */
	void clickBar(Perspective facet, int modifiers) {
		// Util.print("clickBar " + p + "." + facet);
		maybeDrawBar(facet, barWidthRatio(), true);
		Bar bar = lookupBar(facet);
		assert bar != null : facet + " " + facet.getTotalCount() + " " + p
				+ " " + p.getTotalChildTotalCount() + " " + visibleWidth()
				+ " " + logicalWidth;
		bar.pick(modifiers, null);
	}

	public String toString() {
		return "<PerspectiveViz " + p + ">";
	}

	void restrict() {
		// if (//query().usesPerspective(p) &&
		// p.restrictData())
		// During a restrict, our parent may have removed us,
		// and calling restrictData would wrongly resetData to -1
		// if (query().usesPerspective(p))
		drawBars();
		drawLabels();
	}

	public FacetNode startDrag(PNode ignore, Point2D local) {
		assert Util.ignore(ignore);
		dragStartOffset = local.getX();
		return this;
	}

	private double dragStartOffset;

	private Letters letters;

	public void drag(Point2D ignore, PDimension delta) {
		assert Util.ignore(ignore);
		// int viewW = (int) (rightEdge - leftEdge);
		double vertical = delta.getHeight();
		double horizontal = delta.getWidth();
		// If you want to just pan, zooming screws you up, and vice-versa, so
		// choose one or the other.
		if (Math.abs(vertical) > Math.abs(horizontal))
			horizontal = 0;
		else
			vertical = 0;
		double deltaZoom = Math.pow(2, -vertical / 20.0);
		double newLogicalWidth = logicalWidth * deltaZoom;
		double newLeftEdge = 0;
		if (newLogicalWidth < visibleWidth()) {
			// rightEdge = viewW;
			newLogicalWidth = visibleWidth();
		} else {
			// recalculate zoom after rounding newLogicalWidth
			deltaZoom = newLogicalWidth / logicalWidth;
			double pan = -horizontal;
			newLeftEdge = Util.constrain(leftEdge + pan
					+ (leftEdge + dragStartOffset) * (deltaZoom - 1), 0,
					newLogicalWidth - visibleWidth());
			// rightEdge = leftEdge + viewW;
			assert newLeftEdge >= 0
					&& newLeftEdge + visibleWidth() <= newLogicalWidth : newLeftEdge
					+ "/" + newLogicalWidth + " " + visibleWidth();
		}
		setLogicalBounds(newLeftEdge, newLogicalWidth);
		// Util.print(zoom + " " + viewW + " " + newW + " " + leftEdge + " " +
		// rightEdge);
	}

	final class MedianArrow extends Arrow {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		final MedianArrowHandler medianArrowHandler = new MedianArrowHandler();

		ItemPredicate unconditionalMedian;

		ItemPredicate conditionalMedian;

		int significant = 0;

		int highlighted = 1;

		MedianArrow(Paint color, int size, int length) {
			super(color, size, length);
			setPickable(false);
			// line.setPickable(false);
			addInputEventListener(medianArrowHandler);
		}

		void updateColor(int _significant) {
			significant = _significant;
			redraw();
		}

		void redraw() {
			Color color = Bungee.significanceColor(significant, highlighted);
			// Color[] colors = significant == 0 ? Markup.whites
			// : (significant > 0 ? Markup.blues : Markup.oranges);
			// Color color = colors[highlighted];
			setStrokePaint(color);
		}

		void highlight(boolean state) {
			highlighted = state ? 1 : 0;
			redraw();
		}

		final class MedianArrowHandler extends MyInputEventHandler {

			MedianArrowHandler() {
				super(PPath.class);
			}

			public boolean exit(PNode node) {
				art().showMedianArrowDesc(null);
				highlight(false);
				return true;
			}

			public boolean enter(PNode ignore) {
				// boolean unconditional = node == tail;
				// Perspective median = unconditional ? unconditionalMedian
				// : conditionalMedian;
				// // Util.print(median);
				// if (median != null) {
				// StringBuffer buf = new StringBuffer();
				// buf.append("The median ");
				// buf.append(median.getFacetType().getName());
				// if (unconditional)
				// buf.append(" is ");
				// else
				// buf.append(" satisfying the query is ");
				// buf.append(median.getName());
				// if (!unconditional) {
				// buf.append(" ");
				// MouseDoc.formatPvalue(p.medianTest(), buf);
				// }
				// Vector desc = new Vector();
				// desc.add(buf.toString());
				art().showMedianArrowDesc(p);
				// }
				highlight(true);
				return true;
			}
		}

	}

	public Bungee art() {
		return summary.art;
	}

	Query query() {
		return summary.art.query;
	}

	LazyPNode anchorForPopup(Perspective facet) {
		LazyPNode bar = null;
		if (facet == p) {
			bar = rankLabel;
		} else if (p.isPrefetched() && isVisible(facet)) {
			if (areLabelsInited())
				drawMouseLabelInternal(facet);
			else if (barTable.size() > 0)
				maybeDrawBar(facet, barWidthRatio(), true);
			bar = lookupBar(facet);
		}
		// assert bar != null;
		// if (bar != null) {
		// // facetDesc.moveToFront();
		// // breakAtColon(facetDesc);
		// facetDesc.setAnchor(bar);
		// // facetDesc.layout(summary.getWidth(),
		// // getGlobalTranslation().getY());
		// facetDesc.update(facet, rank);
		// return true;
		// } else
		// return false;
		return bar;
	}

	void prepareAnimation() {
		// work around display bug - line gets drawn too thick initially after
		// changes, so hide it during animation. The problem still shows up when
		// the popup translates across it.
		if (medianArrow != null) {
			medianArrow.setVisible(false);
		}
		// front.setStrokePaint(null);
	}

	void animate(float zeroToOne) {
		if (zeroToOne == 1) {
			if (medianArrow != null) {
				medianArrow.setVisible(true);
			}
			// front.setStrokePaint(Bungee.summaryFG.brighter());
		}
	}

	// private void breakAtColon(TextNfacets facetDesc) {
	// Vector v = facetDesc.content;
	// for (ListIterator it = v.listIterator(); it.hasNext();) {
	// Object o = it.next();
	// if (o instanceof String) {
	// String s = (String) o;
	// int n = s.indexOf(':');
	// if (n == 0) {
	// it.remove();
	// it.add(TextNfacets.newlineTag);
	// it.add(s.substring(n + 2, s.length()));
	// } else if (n == s.length() - 1) {
	// it.remove();
	// it.add(s.substring(0, n));
	// it.add(TextNfacets.newlineTag);
	// } else if (n > 0) {
	// it.remove();
	// it.add(s.substring(0, n));
	// it.add(TextNfacets.newlineTag);
	// it.add(s.substring(n + 2, s.length()));
	// }
	// if (n >= 0)
	// break;
	// }
	// }
	// }

	/**
	 * We want to draw bars greedily by totalCount. However there may be 100,000
	 * children, of which only 100 or so might be drawn. barXs keeps the child
	 * with the highest totalCount that maps into each pixel.
	 * 
	 */
	private void computeBars() {
		// Util.print("computeBars " + w);
		Bar.release(barTable.values());
		barTable.clear();
		int iVisibleWidth = (int) visibleWidth();
		assert iVisibleWidth <= logicalWidth : iVisibleWidth + "/"
				+ logicalWidth;
		if (iVisibleWidth > 0) {
			barXs = new Perspective[iVisibleWidth + 1];
			double divisor = barWidthRatio();
			// Util.print("computebars " + p + " " + p.getTotalChildTotalCount()
			// + " " + logicalWidth);
			for (Iterator it = slowVisibleChildIterator(); it.hasNext();) {
				Perspective child = (Perspective) it.next();
				int totalCount = child.getTotalCount();
				if (totalCount > 0) {
					int iMaxX = maxBarPixel(child, divisor);
					int iMinX = minBarPixel(child, divisor);
					// if (p.getID() == 3)
					// printBar(child, iMinX, iMaxX, "bar");
					if (iMaxX >= 0 && iMinX <= iVisibleWidth) {
						assert iMaxX >= 0
								&& iMinX <= iVisibleWidth
								&& iMaxX >= iMinX
								&& child.cumCountInclusive() <= p
										.getTotalChildTotalCount() : printBar(
								child, iMinX, iMaxX, "bar");
						// if this bar is the highest so far for iMinX, record
						// it. if iMaxX > iMinX, no later child can override it,
						// so don't need to mark iMinX+1
						// always mark iMaxX and iMaxX-1
						if (totalCount > itemTotalCount(iMinX))
							barXs[iMinX] = child;
						if (iMaxX > iMinX) {
							barXs[iMinX + 1] = child;
							barXs[iMaxX] = child;
							if (iMaxX > iMinX + 1)
								// without this test, it might mark iMinX even
								// if count isn't highest
								barXs[iMaxX - 1] = child;
						}
					}
				}
			}
			assert verifyBarTables();
			drawComputedBars();
			drawLetters();
		}
	}

	void drawLetters() {
		// Check so that setFeatures doesn't initialize Letters before barXs is set.
		if (art().getShowZoomLetters() && barXs != null) {
			if (letters == null) {
				letters = new Letters();
				layoutLetters();
				addChild(letters);
			} else {
				letters.redraw();
			}
		}
	}

	private void drawComputedBars() {
		int iVisibleWidth = (int) visibleWidth();
		// if ("Genre".equals(p.getName()))
		// Util.print("drawComputedBars " + this + " " + frontW + " " +
		// logicalWidth + " " + leftEdge);
		double divisor = barWidthRatio();
		for (int x = 0; x <= iVisibleWidth;) {
			Perspective toDraw = barXs[x];
			assert toDraw != null : x + "/" + barXs.length + " "
					+ Util.valueOfDeep(barXs);
			int pixels = maybeDrawBar(toDraw, divisor, false);
			// if ("Genre".equals(p.getName()))
			// Util.print(x + " " + pixels + " " + toDraw + " "
			// + minBarPixel(toDraw, divisor) + "-"
			// + maxBarPixel(toDraw, divisor));
			assert pixels > 0;
			x += pixels;
		}
	}

	private void computeLabels() {
		int iVisibleWidth = (int) visibleWidth();
		labelXs = new Perspective[iVisibleWidth + 1];
		double divisor = barWidthRatio();
		Perspective[] restrictions = (Perspective[]) p.allRestrictions()
				.toArray(new Perspective[0]);
		int restrictionBonus = query().getTotalCount();
		boolean isQueryRestricted = query().isRestricted();
		for (Iterator it = visibleChildIterator(); it.hasNext();) {
			Perspective child = (Perspective) it.next();
			int totalCount = child.getTotalCount();
			if (totalCount > 0) {
				int iMaxX = maxBarPixel(child, divisor);
				int iMinX = minBarPixel(child, divisor);
				assert iMaxX >= 0 && iMinX <= iVisibleWidth : printBar(child,
						iMinX, iMaxX, "label");
				int iMidX = (iMaxX + iMinX) / 2;
				if (priorityCount(child, restrictions, isQueryRestricted,
						restrictionBonus) > itemOnCount(iMidX, restrictions,
						isQueryRestricted, restrictionBonus))
					labelXs[iMidX] = child;
			}
		}
		drawComputedLabel(null, divisor, restrictions, isQueryRestricted,
				restrictionBonus);
	}

	/**
	 * March along pixels, finding the child Perspectives to draw. At each
	 * pixel, you draw the child with the highest count at that pixel, which was
	 * computed above, unless another child with a higher count has a label that
	 * would occlude it, unless IT would be occluded. So you get a recusive
	 * test, where a conflict on the rightmost label can propagate all the way
	 * back to the left. At each call, you know there are no conflicts with
	 * leftCandidate from the left. You look for a conflict on the right (or
	 * failing that, the next non-conflict on the right) and recurse on that to
	 * get the next labeled Perspective to the right. You draw leftCandidate iff
	 * it doesn't conflict with that next label.
	 */
	Perspective drawComputedLabel(Perspective leftCandidate, double divisor,
			Perspective[] restrictions, boolean isQueryRestricted,
			int restrictionBonus) {
		assert query().isQueryValid();
		Perspective result = null;
		int x1 = -1;
		int x0 = -1;
		int threshold = -1;
		if (leftCandidate != null) {
			x0 = midLabelPixel(leftCandidate, divisor);
			threshold = priorityCount(leftCandidate, restrictions,
					isQueryRestricted, restrictionBonus);
			x1 = x0 + labelHprojectionW;
		}
		int iVisibleWidth = (int) visibleWidth();
		// Util.print("drawComputedLabel " + p + "." + leftCandidate + " " + x0
		// + "-" + iVisibleWidth + " " + threshold);
		for (int x = x0 + 1; x < iVisibleWidth && result == null; x++) {
			if (x > x1)
				threshold = -1;
			Perspective rightCandidate = labelXs[x];
			if (rightCandidate != null
					&& priorityCount(rightCandidate, restrictions,
							isQueryRestricted, restrictionBonus) > threshold) {
				Perspective nextDrawn = drawComputedLabel(rightCandidate,
						divisor, restrictions, isQueryRestricted,
						restrictionBonus);
				if (nextDrawn != null
						&& midLabelPixel(nextDrawn, divisor) <= midLabelPixel(
								rightCandidate, divisor)
								+ labelHprojectionW) {
					result = nextDrawn;
				} else {
					result = rightCandidate;
					// Util.print(" drawing " + x + " " + result);
					maybeDrawLabel(result);
				}
			}
		}
		return result;
	}

	private String printBar(Perspective child, int iMinX, int iMaxX, String what) {
		Util.print("Adding " + what + " for " + child + ": totalCount="
				+ child.getTotalCount() + " range=" + child.cumCountExclusive()
				+ "-" + child.cumCountInclusive() + "/"
				+ p.getTotalChildTotalCount() + " iMinX-iMaxX=" + iMinX + "-"
				+ iMaxX + " left/logical=" + leftEdge + "/" + logicalWidth);
		return "";
	}

	private int itemTotalCount(int x) {
		if (barXs[x] == null)
			return 0;
		else
			return barXs[x].getTotalCount();
	}

	/**
	 * @param child
	 * @return give filters priority over other children
	 */
	private int priorityCount(Perspective child, Perspective[] restrictions,
			boolean isQueryRestricted, int restrictionBonus) {
		int result = child.getOnCount(isQueryRestricted);
		if (Util.isMember(restrictions, child))
			result += restrictionBonus;
		return result;
	}

	private int itemOnCount(int x, Perspective[] restrictions,
			boolean isQueryRestricted, int restrictionBonus) {
		if (labelXs[x] == null)
			return -1;
		else
			return priorityCount(labelXs[x], restrictions, isQueryRestricted,
					restrictionBonus);
	}

	int maxBarPixel(Perspective child) {
		return maxBarPixel(child, barWidthRatio());
	}

	/**
	 * @param child
	 * @param divisor
	 * @return x offset relative to leftEdge. return w-1 if offset would be off
	 *         the screen.
	 */
	int maxBarPixel(Perspective child, double divisor) {
		// Util.print("maxBarPixel " + child + " " + (child.cumCount() *
		// divisor)
		// + " " + leftEdge);

		double dPixel = child.cumCountInclusive() * divisor - leftEdge;
		// assert dPixel >= 0 : child + " " + dPixel + " "
		// + child.cumCountExclusive() + "-" + child.cumCountInclusive();
		return (int) Math.min(visibleWidth(), dPixel);

		// maxX = Math.min(rightEdge, maxX) - leftEdge;
		// return (int) maxX;
	}

	int minBarPixelRaw(Perspective child) {
		return (int) (child.cumCountExclusive() * barWidthRatio() - leftEdge);
	}

	int maxBarPixelRaw(Perspective child) {
		return (int) (child.cumCountInclusive() * barWidthRatio() - leftEdge);
	}

	/**
	 * @param child
	 * @param divisor
	 * @return x offset relative to left edge. return 0 if offset would be
	 *         negative.
	 */
	int minBarPixel(Perspective child, double divisor) {
		// If bar should start a fraction after w-1, we shouldn't draw it, so
		// always round up.
		double dPixel = child.cumCountExclusive() * divisor - leftEdge;
		// assert dPixel <= visibleWidth() : child + " " + dPixel + "/"
		// + visibleWidth() + " " + leftEdge + "/" + logicalWidth + " ("
		// + child.cumCountExclusive() + "-" + child.cumCountInclusive()
		// + ")/" + p.getTotalChildTotalCount();
		return Math.max(0, (int) dPixel);
	}

	/**
	 * @param child
	 * @param divisor
	 * @return x offset of middle of label relative to leftEdge
	 */
	private int midLabelPixel(Perspective child, double divisor) {
		return (minBarPixel(child, divisor) + maxBarPixel(child, divisor)) / 2;
	}

	boolean isVisible(Perspective child) {
		double divisor = barWidthRatio();
		// 0.5's round the result to the nearest integer
		double minPixel = (child.cumCountExclusive() + 0.5) * divisor
				- leftEdge;
		double maxPixel = (child.cumCountInclusive() - 0.5) * divisor
				- leftEdge;
		return minPixel < visibleWidth() && maxPixel > 0;
	}

	private class Letters extends LazyPNode implements FacetNode,
			PickFacetTextNotifier, PerspectiveObserver {
		// PerspectiveViz pv;

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Letters() {
			// pv = _pv;
			addInputEventListener(Bungee.facetClickHandler);
			redraw();
		}

		public boolean isAlphabetic() {
			return p.isAlphabetic() && art().getShowZoomLetters();
		}

		public void redraw() {
			CollationKey prefix = prefix();
			if (prefix != null) {
				// removeAllChildren();
				int iVisibleWidth = (int) visibleWidth();
				// Util.print("redraw " + p + " '" + prefix.getSourceString() +
				// "' "
				// + barTable.size());
				setWidth(PerspectiveViz.this.getWidth());
				// if size==1, prefix==name so getLetter returns 0 and you get
				// an infinite loop
				if (p.isAlphabetic()) {
					if (barTable.size() == 1) {
						removeAllChildren();
						return;
					}
					double divisor = barWidthRatio();
					int[] counts = new int[iVisibleWidth + 1];
					char[] letterXs = new char[iVisibleWidth + 1];

					Perspective firstWithLetter = firstVisiblePerspective();
					Perspective lastVisiblePerspective = lastVisiblePerspective();
					for (Iterator it = p.letterOffsetsIterator(prefix); it
							.hasNext();) {
						Entry entry = (Entry) it.next();
						CollationKey letter = ((CollationKey) entry.getKey());
						Perspective lastWithLetter = ((Perspective[]) entry
								.getValue())[1];
						if (lastWithLetter.compareTo(firstWithLetter) >= 0
								&& firstWithLetter
										.compareTo(lastVisiblePerspective) <= 0) {
							int iMidX = (minBarPixel(firstWithLetter, divisor) + maxBarPixel(
									lastWithLetter, divisor)) / 2;
							assert iMidX >= 0 : p + " " + prefix + " " + letter
									+ " " + lastWithLetter + " "
									+ minBarPixel(firstWithLetter, divisor)
									+ "-"
									+ maxBarPixel(lastWithLetter, divisor);
							letterXs[iMidX] = letter.getSourceString()
									.toUpperCase().charAt(0);
							counts[iMidX] = (firstWithLetter == lastWithLetter) ? firstWithLetter
									.getTotalCount()
									: lastWithLetter.cumCountExclusive()
											- firstWithLetter
													.cumCountExclusive();
							// if ("Label".equals(p.getNameIfPossible()))
							// Util.print("redraw " + p + " '"
							// + letter.getSourceString() + "' "
							// + firstWithLetter + "-"
							// + lastWithLetter + " " + iMidX + " "
							// + counts[iMidX]);
							// Util.print(lastWithLetter + " "
							// + lastWithLetter.whichChild() + "/"
							// + p.nChildren());
							firstWithLetter = lastWithLetter.nextSibling();
							assert firstWithLetter != null || !it.hasNext() : this
									+ " "
									+ lastWithLetter
									+ " "
									+ lastWithLetter.whichChild()
									+ "/"
									+ p.nChildren()
									+ "\n"
									+ p.getLetterOffsets(prefix);
						}
					}
					removeAllChildren();
					drawComputedLetter(-1, letterXs, counts, prefix
							.getSourceString());
				}
			}
		}

		private int drawComputedLetter(int leftCandidateX, char[] letterXs,
				int[] counts, String prefix) {
			int result = -1;
			int x1 = -1;
			int x0 = -1;
			int threshold = 0; // don't draw letters with 0 count
			if (leftCandidateX >= 0) {
				x0 = leftCandidateX;
				threshold = counts[leftCandidateX];
				x1 = x0 + labelHprojectionW;
			}
			int iVisibleWidth = (int) visibleWidth();
			// if ("Label".equals(p.getNameIfPossible()))
			// Util.print("drawComputedLetter " + p + "." + leftCandidateX
			// + " " + x0 + " " + threshold);
			for (int x = x0 + 1; x < iVisibleWidth && result < 0; x++) {
				if (x > x1)
					threshold = 0;
				char rightCandidate = letterXs[x];
				if (rightCandidate > 0 && counts[x] > threshold) {
					int nextDrawnX = drawComputedLetter(x, letterXs, counts,
							prefix);
					if (nextDrawnX > 0 && nextDrawnX <= x + labelHprojectionW) {
						result = nextDrawnX;
					} else {
						result = x;
						maybeDrawLetter(
								prefix.toLowerCase() + letterXs[result], x);
					}
				}
			}
			return result;
		}

		private Map letterPTextCache = new Hashtable();

		private void maybeDrawLetter(String s, int midX) {
			// if ("Label".equals(p.getNameIfPossible()))
			// Util.print("maybeDrawLetter " + p + "." + s + " " + midX);
			int iVisibleWidth = (int) visibleWidth();
			assert midX >= 0 : s;
			assert midX < iVisibleWidth : s + " " + midX + " " + iVisibleWidth;
			FacetPText label = (FacetPText) letterPTextCache.get(s);
			if (label == null || label.getFont() != art().font) {
				label = getFacetPText(null, 0.0, midX);
				label.setPermanentTextPaint(Bungee.summaryFG.darker()); // Color.
				// darkGray
				// );
				label.setPaint(Color.black);
				label.pickFacetTextNotifier = this;
				label.setText(s);
				letterPTextCache.put(s, label);
			}
			assert label.getParent() == null;
			label.setPTextOffset(midX + label.getHeight() / 2
					+ label.getWidth() * 1.4, -labelHprojectionW
					- label.getWidth());
			// label.setScale(1.0/32); //summary.selectedFrontH());
			addChild(label);
		}

		/**
		 * @return longest common prefix of the names of p's children that would
		 *         be displayed if bars were infinitesimally thin, or null if
		 *         some names haven't been cached yet. I.e. consider children
		 *         whose bar would be too small to draw, since letterOffsets
		 *         consider them. Otherwise zooming to a prefix might zoom to a
		 *         longer prefix because only one extension of it has visible
		 *         bars.
		 */
		private CollationKey prefix() {
			// Compute the first perspective that would be visible but for
			// collisions
			Perspective first = firstVisiblePerspective();
			for (Perspective prev = first.previousSibling(); prev != null
					&& isVisible(prev); prev = prev.previousSibling()) {
				first = prev;
			}
			// Compute the last perspective that would be visible but for
			// collisions
			Perspective last = lastVisiblePerspective();
			for (Perspective next = last.nextSibling(); next != null
					&& isVisible(next); next = next.nextSibling()) {
				last = next;
			}
			String name1 = first.getName(this, null);
			String name2 = last.getName(this, null);
			CollationKey result = (name1 != null && name2 != null) ? Util
					.toCollationKey(Util.commonPrefix(name1, name2)) : null;
			// Util.print("prefix '" + name1 + "'-'" + name2 + "' => '"
			// + (result == null ? null : result.getSourceString()) + "'");
			return result;
		}

		boolean keyPress(char suffix) {
			finishPanZoom();
			CollationKey prefix = prefix();
			// Util.print("keyPress '"
			// + (prefix == null ? null : prefix.getSourceString()) + "' "
			// + suffix);
			if (prefix != null && p.isAlphabetic()) {
				String prefixString = prefix.getSourceString();
				if (suffix == '\b') {
					// Find the longest prefix that is shorter than prefix() and
					// that will include at least one additional child.
					if (prefixString.length() > 0) {
						// prefix = prefix.toUpperCase();
						while (prefixString.length() > 0
								&& p
										.getLetterOffsets(
												prefix = Util
														.toCollationKey(prefixString = prefixString
																.substring(
																		0,
																		prefixString
																				.length() - 1)))
										.size() == 1) {
							// As long as map size == 1, the only extension is
							// this prefix,
							// so there won't be any *additional* children
						}
						// Util.print("backspace '" + prefix);
					} else if (!isZooming()) {
						art().setTip(
								p.getName() + " is fully zoomed out already");
						return false;
					}
				} else
					prefix = Util.toCollationKey(prefixString + suffix);
				return zoom(prefix);
			} else
				return false;
		}

		public boolean pick(FacetText node, int modifiers) {
			return zoom(Util.toCollationKey(node.getText()));
		}

		boolean zoom(final CollationKey s) {
			// Util.print("ZOOM to '" + s.getSourceString() + "' " + leftEdge
			// + " " + logicalWidth);
			if (s.getSourceString().length() == 0) {
				animatePanZoom(0, visibleWidth());
			} else {
				// PerspectiveObserver rePicker = new Repicker(s);
				Perspective rightFacet = p.lastWithPrefix(s);

				if (rightFacet != null) {
					Perspective leftFacet = p.firstWithPrefix(s);
					// Util.print("left/right " + leftFacet + " " + rightFacet);
					assert leftFacet != null : p + " " + s;
					double newLogicalWidth = visibleWidth()
							* p.getTotalChildTotalCount()
							/ (rightFacet.cumCountInclusive() - leftFacet
									.cumCountExclusive());
					// Util.print("ZOOM to " + s + " " + leftFacet + "-"
					// + rightFacet + " " + newLogicalWidth + " w="
					// + w);
					double newLeftEdge = leftFacet.cumCountExclusive()
							* (newLogicalWidth / p.getTotalChildTotalCount());
					// Util.print("");
					// bbb(leftFacet, newLeftEdge, newLogicalWidth);
					// bbb(rightFacet, newLeftEdge, newLogicalWidth);
					// bbb(query().findPerspective(185737), newLeftEdge,
					// newLogicalWidth);
					// Util.print(" " + newLeftEdge + " " + newLogicalWidth);
					animatePanZoom(newLeftEdge, newLogicalWidth);
					// setLogicalBounds(newLeftEdge, newLogicalWidth);
				} else {
					art().setTip(
							"No " + p.getName() + " tags start with '"
									+ s.getSourceString().toUpperCase() + "'");
				}
			}
			return true;
		}

		// void bbb(Perspective facet, double leftEdge1, double w) {
		// double left = facet.cumCountExclusive() * w
		// / p.getTotalChildTotalCount() - leftEdge1;
		// double right = facet.cumCountInclusive() * w
		// / p.getTotalChildTotalCount() - leftEdge1;
		// Util.print("range " + facet + " " + left + "-" + right);
		// }

		// private class Repicker implements PerspectiveObserver {
		// String text;
		//
		// Repicker(String s) {
		// text = s;
		// }
		//
		// public void redraw() {
		// zoom(text);
		// }
		// }

		// private String backspacePrefix(Perspective candidate, String prefix)
		// {
		// if (prefix.length() > 0) {
		// String name = candidate.getNameIfPossible();
		// if (name != null) {
		// Util.print("backspacePrefix " + prefix + " " + name
		// + " => " + Util.commonPrefix(prefix, name, false));
		// prefix = Util.commonPrefix(prefix, name, false);
		// }
		// }
		// return prefix;
		// }

		public boolean highlight(FacetText node, boolean state, int modifiers) {
			// Util.print("Letters.highlight " + node.getText());
			String prefix = node.getText();
			int newPrefixLength = prefix.length() - 1;
			char suffix = prefix.charAt(newPrefixLength);
			String msg = null;
			if (state) {
				msg = "zoom into tags starting with '" + prefix
						+ "', as will typing '" + suffix + "'";
				if (newPrefixLength > 0) {
					String unzoomPrefix;
					if (newPrefixLength == 1)
						unzoomPrefix = "any letter.";
					else
						unzoomPrefix = "'"
								+ prefix.substring(0, newPrefixLength - 1)
								+ "'.";
					msg += ";  backspace zooms out to tags starting with "
							+ unzoomPrefix;
				}
			}
			art().setClickDesc(msg);
			return true;
		}

		public boolean highlight(boolean state, int modifiers, PInputEvent e) {
			art().setClickDesc(
					state ? "Start dragging up/down to zoom; left/right to pan"
							: null);
			return true;
		}

		public Bungee art() {
			return PerspectiveViz.this.art();
		}

		public Perspective getFacet() {
			return p;
		}

		public void mayHideTransients(PNode node) {
			// TODO Auto-generated method stub
		}

		public boolean pick(int modifiers, PInputEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		public FacetNode startDrag(PNode node, Point2D positionRelativeTo) {
			// TODO Auto-generated method stub
			return PerspectiveViz.this.startDrag(node, positionRelativeTo);
		}

		public void drag(Point2D position, PDimension delta) {
			assert false;
		}

		public void setVisible(boolean state) {
			super.setVisible(state);
			setPickable(state);
			setChildrenPickable(state);
		}

		public String toString() {
			return "<Letters " + p + ">";
		}
	}

	Perspective firstVisiblePerspective() {
		assert barXs[0] != null : verifyBarTables();
		return barXs[0];
	}

	Perspective lastVisiblePerspective() {
		int iVisibleWidth = (int) visibleWidth();
		assert barXs[iVisibleWidth] != null : verifyBarTables();
		// if (w > 0)
		// w--;
		return barXs[iVisibleWidth];
	}

	/**
	 * This depends on barXs already being computed, so don't call this from
	 * computeBars
	 * 
	 * @return Iterator over children whose bars would fall within the visible
	 *         portion of the logicalWidth.
	 */
	Iterator visibleChildIterator() {
		return p.getChildIterator(barXs[0], barXs[(int) visibleWidth()]);
	}

	boolean isPerspectiveVisible(Perspective facet) {
		double divisor = barWidthRatio();
		int minCount = (int) Math.ceil(leftEdge / divisor + epsilon);
		int maxCount = (int) ((leftEdge + visibleWidth()) / divisor - epsilon);
		return facet.cumCountInclusive() >= minCount
				&& facet.cumCountExclusive() <= maxCount;

		// return facet.compareTo(barXs[0]) >= 0
		// && facet.compareTo(barXs[(int) visibleWidth()]) <= 0;
	}

	Iterator slowVisibleChildIterator() {
		double divisor = barWidthRatio();
		int minCount = (int) Math.ceil(leftEdge / divisor + epsilon);
		int maxCount = (int) ((leftEdge + visibleWidth()) / divisor - epsilon);
		// Util.print(leftEdge + " " +(leftEdge + epsilon));
		// Util.print((leftEdge / divisor) + " " +((leftEdge + epsilon)/
		// divisor));
		// Util.print("slowVisibleChildIterator " + p + " " + leftEdge + "/"
		// + logicalWidth + " => (ceil("
		// + ((leftEdge + epsilon) / divisor) + "}-(floor("
		// + ((leftEdge + visibleWidth() - epsilon) / divisor)
		// + "))/" + p.getTotalChildTotalCount());
		return p.cumCountChildIterator(minCount, maxCount);
	}

	public Perspective getFacet() {
		return p;
	}

	public boolean highlight(boolean state, int modifiers, PInputEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	public void mayHideTransients(PNode node) {
		// TODO Auto-generated method stub

	}

	public boolean pick(int modifiers, PInputEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	boolean keyPress(char key) {
		if (letters != null)
			return letters.keyPress(key);
		else if (!art().getShowZoomLetters())
			art().setTip("Zooming is disabled in beginner mode");
		else if (!p.isAlphabetic())
			art().setTip(
					"Zooming is disabled because " + p.getName()
							+ " tags are not in alphabetical order");
		else
			assert false;
		return false;
	}
}

// This goes on rankLabel
// final class ShowPerspectiveDocHandler extends MyInputEventHandler {
//
// public ShowPerspectiveDocHandler() {
// super(PText.class);
// }
//
// PerspectiveViz getPerspectiveViz(LazyPNode node) {
// if (node == null || node instanceof PerspectiveViz)
// return (PerspectiveViz) node;
// return getPerspectiveViz(node.getParent());
// }
//
// Perspective getFacet(LazyPNode node) {
// if (node instanceof FacetNode)
// return ((FacetNode) node).getFacet();
// else if (node == null)
// return null;
// else
// return getFacet(node.getParent());
// }
//
// public boolean enter(PNode node) {
// Util.print("ShowPerspectiveDocHandler.enter");
// Perspective facet = getFacet(node);
// PerspectiveViz pv = getPerspectiveViz(node);
// if (pv != null)
// pv.showDoc(facet, true);
// return pv != null;
// }
//
// public boolean exit(PNode node) {
// PerspectiveViz pv = getPerspectiveViz(node);
// if (pv != null)
// pv.showDoc(null, false);
// return pv != null;
// }
// }

final class HotZoneListener extends PBasicInputEventHandler {

	PerspectiveViz pv;

	HotZoneListener(PerspectiveViz _pv) {
		pv = _pv;
	}

	public void mouseEntered(PInputEvent e) {
		double y = e.getPositionRelativeTo(e.getPickedNode()).getY();
		pv.hotZone(y);
		e.setHandled(true);
	}

	public void mouseExited(PInputEvent e) {
		pv.loseHotZone();
		e.setHandled(true);
	}

	public void mouseMoved(PInputEvent e) {
		double y = e.getPositionRelativeTo(e.getPickedNode()).getY();
		pv.hotZone(y);
		e.setHandled(true);
	}

}

final class SqueezablePNode extends LazyPNode {

	// PBounds clip;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	double goalYscale = -1;

	private double goalY = -1;

	// private long loadTime;

	SqueezablePNode() {
		// setBounds(0, 0, 1, 1);
		setPickable(false);
	}

	void setVisible() {
		// PerspectiveViz pv = ((PerspectiveViz) getParent());
		setVisible(goalYscale > 0);
	}

	void layout(double y, double yScale) {
		// Util.print("PV.layout " + getParent() + " scale = " + yScale + " "
		// + goalYscale + " " + y + " " + goalY);
		// PerspectiveViz pv = ((PerspectiveViz) getParent());
		if (yScale != goalYscale || y != goalY) {
			goalYscale = yScale;
			goalY = y;
			boolean isVisible = (yScale > 0);
			// Util.print("PV.layout scale = " + yScale + " " + y);
			setVisible(isVisible);
			if (isVisible) {
				setTransform(Util.scaleNtranslate(1.0, yScale, 0.0, y));
			}
		}
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		setChildrenPickable(visible);
	}

	// protected void paint(PPaintContext paintContext) {
	// Graphics2D g2 = paintContext.getGraphics();
	// g2.setPaint(Bungee.summaryFG);
	// g2.setStroke(LazyPPath.getStrokeInstance(0));
	// g2.draw(getBounds());
	// }

	// protected void paint(PPaintContext paintContext) {
	// // loadTime = new Date().getTime();
	// if (clip != null) {
	// // Util.print("SqueezablePNode.paint " + clip.getY() + " " +
	// // getVisible());
	// paintContext.pushClip(clip);
	// super.paint(paintContext);
	// paintContext.popClip(clip);
	// } else
	// super.paint(paintContext);
	// }

	// void paintAfterChildren(PPaintContext paintContext) {
	// if (clip != null)
	// paintContext.popClip(clip);
	//
	// // Util.print("painting " + ((PerspectiveViz) getParent()).p + " took "
	// // + (new Date().getTime() - loadTime));
	// }
}
