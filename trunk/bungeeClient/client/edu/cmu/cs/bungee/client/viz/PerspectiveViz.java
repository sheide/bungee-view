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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.lang.Math;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;



import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.viz.Summary.RankComponentHeights;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Arrow;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PPaintContext;

class PerspectiveViz extends LazyPNode implements PickFacetTextNotifier {

	 Summary summary;

	 Rank rank;

	PerspectiveViz parentPV;

	 Perspective p;

	SqueezablePNode labels;

	//  SqueezablePNode fold;
	
	LazyPNode parentRect;

	SqueezablePNode front;

	// private PNode hLine;

	 LazyPPath lightBeam = null;

	TextNfacets rankLabel;

	APText[] percentLabels;

	private LazyPNode percentLabelHotZone;

	private LazyPNode hotLine;

//	private LazyPPath percentScale;

	double percentLabelW;

	final static double PERCENT_LABEL_SCALE = 0.75;

	static final Color pvBG = new Color(0x333344 );

	//  boolean isZombie = false;

	private int w = 0;

	/**
	 * The x midpoint of each facet (indexed by facet.index), whether or not it
	 * is displayed. Length = number of facets.
	 */
	private int[] x_mid_coords;

	/**
	 * The facet of the bar at a given x coordinate. Only the endpoints are
	 * recorded. Noone should ask for coordinates in between the endpoints.
	 */
	private Perspective[] barXs;

	/**
	 * The facet of the label at a given x coordinate. All points are recorded.
	 */
	private FacetPText[] labelXs;

	private Hashtable barTable = new Hashtable();
	
	/**
	 * Remember previous layout parameters, and don't re-layout if they are the same.
	 */
	private RankComponentHeights prevComponentHeights;

	double prevH;

	// Steer steer;

	// final static private ShowPerspectiveDocHandler showPerspectiveDocHandler
	// = new ShowPerspectiveDocHandler();

//	AffineTransform unscaleFrontTransform;

	private MedianArrow medianArrow;

	// PAffineTransform unscaleFoldTransform;

	// boolean isFlying;

	PerspectiveViz(Perspective _p, Summary _summary) {
		p = _p;
		summary = _summary;
		parentPV = summary.findPerspective(p.getParent());
		setPickable(false);

		Bungee art = art();
		percentLabelW = art.getStringWidth("0.001%");
		labelHprojectionW = (int) (1.1 * art.lineH);

		x_mid_coords = new int[p.nChildren()];
		// Util.print("PerspectiveViz " + p.getName() + " " + p.nValues());

		front = new SqueezablePNode();
		front.setPaint(Color.black);
		front.setHeight(1);
//		front.clip = new PBounds(-1000, 0.5, 2000, 0.5);
		addChild(front);
		// front isn't visible directly, so bounds don't matter.
		// front.setBounds(0, 0, 1, 1);
		front.addInputEventListener(Bungee.facetClickHandler);
		parentRect = new LazyPNode();
		parentRect.setPaint(pvBG);
		parentRect.setBounds(0, 0.5, 1,0.5);

		// fold = new SqueezablePNode();
		// curtainStartY = 1; // summary.perspectiveFoldH;
		// curtainGoalY = curtainStartY;
		// fold.clip = new PBounds(-1000, curtainStartY, 2000, 1 -
		// curtainStartY);
		// fold.setVisible(false);
		// addChild(fold);
		// // fold.setBounds(0, 0, w, Summary.perspectiveFoldH); //These bounds
		// // don't matter either.
		// fold.addInputEventListener(Art.facetClickHandler);

		labels = new SqueezablePNode();
		labels.setVisible(false);
		addChild(labels); // add labels after front, so labels get
		// picked in
		// favor of rankLabel
		labels.addInputEventListener(Bungee.facetClickHandler);

		// hLine = new LazyPNode();
		// hLine.setPaint(Color.red);
		// hLine.setPickable(false);
		// hLine.setOffset(0.0, Art.perspectiveFoldH);
		// hLine.setHeight(1.0);
		// //hLine.setStroke(new BasicStroke(10.0f));

		rankLabel = new TextNfacets(art(), Bungee.summaryFG, true);
		rankLabel.setPickable(false);
		rankLabel.setWrapText(false);
		rankLabel.setUnderline(true);
		// rankLabel.setRedrawer(this);
		// rankLabel.addInputEventListener(showPerspectiveDocHandler);
		// rankLabel.setPaint(Art.summaryBG);
		// front.addChild(rankLabel);

		// steer = new Steer();
		// steer.setVisible(false);
		// addChild(steer);

		if (p.isOrdered()) {
			Color color = art.facetTextColor(null, 0, 0);
			medianArrow = new MedianArrow(color, color, 6, 0);
			// medianArrow.setVisible(false);
			// medianArrow.setChildrenPickable(false);
			// front.addChild(medianArrow);
		}

		// addInputEventListener(Art.facetClickHandler);
//		 setPaint(Color.black);
	}

	void delayedInit() {
		if (percentLabels == null) {
			percentLabels = new APText[3];
			boolean visible = rankLabel.getVisible();
			double percentLabelScaledW = percentLabelW * PERCENT_LABEL_SCALE;
			double x = Math.round(-percentLabelScaledW);
			for (int i = 0; i < 3; i++) {
				percentLabels[i] = art().oneLineLabel();
				percentLabels[i].setTransparency(0);
				percentLabels[i].setVisible(visible);
				percentLabels[i].setPickable(false);
				percentLabels[i].setTextPaint(Bungee.PERCENT_LABEL_COLOR);
				percentLabels[i].setJustification(Component.RIGHT_ALIGNMENT);
				percentLabels[i].setConstrainWidthToTextWidth(false);
				percentLabels[i].setWidth(percentLabelW);
				// percentLabels[i].setScale(percentLabelScale);
				percentLabels[i].setXoffset(x);
				front.addChild(percentLabels[i]);
			}
			percentLabels[0].setText("0%");
			percentLabels[1].setText("100%");
			percentLabels[2].setText("100%");

			percentLabelHotZone = new LazyPNode();
			percentLabelScaledW *= 0.67; // Make hot zone cover 100%, rather than up to 0.001%
			percentLabelHotZone.setBounds(-percentLabelScaledW, 0, percentLabelScaledW, 1 );
//			front.addChild(percentLabelHotZone);
			percentLabelHotZone.setPickable(false);
//			percentLabelHotZone.setPaint(Color.yellow);
			percentLabelHotZone
					.addInputEventListener(new HotZoneListener(this));

//			percentScale = new LazyPPath();
//			percentScale.setPaint(front.getPaint());
//			percentScale.setOffset(-percentLabelW, 0);
//			percentScale.setTransparency(0);
//			percentScale.setPickable(false);
//			front.addChild(percentScale);
			// drawPercentScale();

			hotLine = new LazyPNode();
			hotLine.setPaint(Bungee.PERCENT_LABEL_COLOR);
			hotLine.setVisible(false);
			hotLine.setPickable(false);

//			layoutPercentLabels();
		}
	}

	String getName() {
		return p.getName();
	}

	// double cameraZ() {
	// return 0.8 * getWidth();
	// }
	//
	// private double curtainStartY;
	//
	// double curtainGoalY;

	void updateData() {
		// Util.print("PV.updateData " + getName() + " " + curtainH + " "
		// + fold.clip.getY());

//		unscaleFrontTransform = AffineTransform.getScaleInstance(1,
//				1 / summary.selectedFrontH);
		// unscaleFoldTransform = new PAffineTransform(AffineTransform
		// .getScaleInstance(1, 1 / summary.selectedFoldH));

		// loseLabels();
		// assert fold != null;
		// assert fold.clip != null;
		// curtainStartY = fold.clip.getY();
		// curtainGoalY = curtainH;
		// if (false && rank.isConnected) {
		// prevH = -1;
		// fold.goalScale = -1;
		// layoutChildren(); // force recompute fold.
		// }
		assert rank.expectedPercentOn() >= 0;
		draw(true);
	}

	void animateData(double zeroToOne) {
		// double newCurtainY = curtainStartY + zeroToOne
		// * (curtainGoalY - curtainStartY);
		// // Util.print("PV.animateData " + getName() + " " + newCurtainY);
		// boolean curtainChanged = Math.abs(newCurtainY - fold.clip.getY())
		// * summary.selectedFoldH > 1.0;
		// fold.clip.setRect(-1000, newCurtainY, 2000, 1 - newCurtainY);
		// fold.clip.setOrigin(-1000, newCurtainY);
		// fold.clip.setSize(2000, 1 - newCurtainY);
		for (Iterator it = barTable.values().iterator(); it.hasNext();) {
			Bar bar = ((Bar) it.next());
			bar.animateData(zeroToOne);
		}
	}
	
	void setBarTransparencies(float zeroToOne) {
		for (Iterator it = barTable.values().iterator(); it.hasNext();) {
			Bar bar = ((Bar) it.next());
			bar.setTransparency(zeroToOne);
		}		
	}

	// Called only by Rank.redraw.
	void validate(int _w, boolean isShowRankLabels) {
		// Util.print("pv.validate " + getName() + " " + _w + " " + w);
		if (p.isPrefetched() || p.getParent() == null) {
			// loseLabels();
//			layoutPercentLabels();
			setPercentLabelVisible();
			leftEdge = 0;
			rightEdge = _w;
			boolean changeW = _w != w;
			if (changeW) {
				revalidate(_w);
				layoutLightBeam();
				rankLabel.setVisible(isShowRankLabels);
			}
		} else {
			queuePrefetch();
		}
	}
	
	void queuePrefetch() {
		List v = new Vector(2);
		v.add(p);
		v.add(getDoValidate());
		art().query.queuePrefetch(v);		
	}

	void revalidate(int _w) {
//		 Util.print("pv.revalidate " + p + " " + _w + " " + w);
		setWidth(_w);
		w = _w;
//		front.setBounds(0, 0, (int) (rightEdge - leftEdge), 1);
		front.setWidth((int) (rightEdge - leftEdge));
		parentRect.setWidth(front.getWidth());
		if (medianArrow != null)
			medianArrow.setOffset(w / 2, 1.0);
		draw(false);
	}
	
	PInterpolatingActivity highlightParentRect(boolean state, long duration) {
		return parentRect.animateToColor(state ? pvBG.brighter() : pvBG, duration);
	}

	void setPercentLabelVisible() {
		if (percentLabels != null) {
			boolean isShowRankLabels = this == rank.perspectives[0];
//			percentScale.setVisible(isShowRankLabels);
			percentLabels[0].setVisible(isShowRankLabels);
			percentLabels[1].setVisible(isShowRankLabels);
			percentLabels[2].setVisible(isShowRankLabels
					&& rank.expectedPercentOn() < 1);
//			hotLine.setWidth(w * summary.selectedFrontH);
		}
	}

	private transient Runnable doValidate;

	 Runnable getDoValidate() {
		if (doValidate == null)
			doValidate = new Runnable() {

				public void run() {
					rank.redraw();
//					((Rank) getParent()).redraw();
				}
			};
		return doValidate;
	}

	void layoutMedianArrow() {
		if (p.isPrefetched()) {
			if (medianArrow.unconditionalMedian == null) {
				medianArrow.unconditionalMedian = p.getMedianPerspective(false);
			}
			double median = p.median(1, 0);
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
				if (bar != null) {
					double x = bar.getX() + childFraction * bar.getWidth();
					double length = x - medianArrow.getXOffset();
					medianArrow.setLength((int) length);
					// Color color = null;
					medianArrow.updateColor(p.medianTestSignificant(0.05));
					// Util.print("median: " + p + " " + median + " " +
					// bar.facet
					// + " " + x + " " + length);
				}
				medianArrow.moveToFront();
			}
		}
	}

	// called as a result of changing deselectedFrontH, etc
	void layoutPercentLabels() {
//		if (percentLabels != null) {
		if (isConnected()) {
			
			double x = Math.round(-percentLabelW * PERCENT_LABEL_SCALE);

			double frontH = summary.selectedFrontH();
			double yOffset = -PERCENT_LABEL_SCALE*art().lineH / 2.0 / frontH;
//			Util.print("percent y offset = " + offset);
//			double x = percentLabels[0].getXOffset();
			double scaleY = PERCENT_LABEL_SCALE / frontH;
			percentLabels[0].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, 1.0 + yOffset));
			percentLabels[1].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, 0.5 + yOffset));
			percentLabels[2].setTransform(Util.scaleNtranslate(
					PERCENT_LABEL_SCALE, scaleY, x, yOffset));
			scaleMedianArrow();
		}
	}

	void scaleMedianArrow() {
		if (medianArrow != null) {
			double scaleY = 1.0 / front.goalScale; // 1.0 /
			// summary.selectedFrontH;
			medianArrow.setTransform(Util.scaleNtranslate(1.0, scaleY,
					medianArrow.getXOffset(), 1.0)); // -
			// medianArrow.getHeight()
			// * scaleY / 2.0));
		}
	}

	void hotZone(double y) {
		double effectiveY = 1.0 - y;
		if (rank.warpPower() == 1.0) {
			y = Math.max(y, 0.5);
			effectiveY = 2.0 - 2.0 * y;
		}
		double percent = rank.unwarp(effectiveY);
		// Util.print("hotZone " + y + " " + rank.expectedPercentOn() + " " +
		// rank.warpPower() + " " + percent);
		double frontH = summary.selectedFrontH();
		double offset = -art().lineH / 2.0 / frontH;
		double scaleY = PERCENT_LABEL_SCALE / frontH;
		percentLabels[1].setTransform(Util.scaleNtranslate(PERCENT_LABEL_SCALE,
				scaleY, percentLabels[1].getXOffset(), y + offset));
		percentLabels[1].setText(ResultsGrid.formatPercent(percent, null)
				.toString());
		hotLine.setVisible(true);
		hotLine.moveToFront();
		hotLine.setScale(1 / frontH);
		hotLine.setBounds(0, (int) (y * frontH), w
				* frontH, 1);
		// Util.print(hotLine.getGlobalBounds());
		// gui.Util.printDescendents(hotLine);
	}

	void loseHotZone() {
		double frontH = summary.selectedFrontH();
		double offset = -art().lineH / 2.0 / frontH;
		double scaleY = PERCENT_LABEL_SCALE / frontH;
		percentLabels[1].setTransform(Util.scaleNtranslate(PERCENT_LABEL_SCALE,
				scaleY, percentLabels[1].getXOffset(), 0.5 + offset));
		percentLabels[1].setText(ResultsGrid.formatPercent(
				rank.expectedPercentOn(), null).toString());
		hotLine.setVisible(false);
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
		if (w > 0) { // Make sure we've been initialized.
			double h = getHeight();
			if (summary.rankComponentHeights != prevComponentHeights || Math.abs(h - prevH) > 1.0) {
				assert w == getWidth();
//				 Util.print("pv.layoutChildren " + p + " " + h + " " +
//				 rank.foldH + " " + rank.frontH);
				prevComponentHeights = summary.rankComponentHeights;
				prevH = h;
				// rank.computeHeights(); // is this needed?
				// fold.layout(0.0, 0*(rank.foldH - 1)); // -1
				// leaves
				// a
				// gap
				// between
				// fold
				// and
				// front;
				front.layout(rank.foldH, rank.frontH);
				scaleMedianArrow();
				double yScale = rank.texth;
				if (yScale > 0)
					// Avoid division by zero
					yScale /= summary.selectedLabelH();
				labels.layout(rank.foldH + rank.frontH, yScale);
				layoutRankLabel(false);
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

	void layoutRankLabel(boolean contentChanged) {
		// Util.print("layoutRankLabel " + p + " " + rankLabel.getVisible() + "
		// " + rank.frontH);
		if (rankLabel.getVisible() && rank.frontH > 0) {
			double xScale = 1.0;
			double yScale = 1.0 / rank.frontH;
			double labelH = rank.frontH + rank.texth;
			double labelW = rankLabel.getWidth();
			double lineH = art().lineH;
			if (labelH < lineH) {
				xScale = labelH / lineH;
				yScale *= xScale;
				labelH = lineH;
			} else
				labelH = lineH * ((int) (labelH / lineH));
			rankLabel.setWidth(Math.floor(rank.rankLabelWdith() / xScale));
			rankLabel.setTransform(Util.scaleNtranslate(xScale, yScale,
					rankLabel.getXOffset(), 0.0));
			if (contentChanged || rankLabel.getHeight() != labelH
					|| rankLabel.getWidth() != labelW) {
				rankLabel.setHeight(labelH);
				rankLabel.layoutBestFit();
				// Noone else should call layout, because notifier will be
				// lost!!!
				hackRankLabelNotifier();
			}
		}
	}

	void hackRankLabelNotifier() {
		for (int i = 0; i < rankLabel.getChildrenCount(); i++) {
			FacetText ft = (FacetText) rankLabel.getChild(i);
			if (ft.getFacet() != null) {
				ft.pickFacetTextNotifier = this;
				art().validatePerspectiveList();
				break;
			}
		}
	}

	// // Redrawer for rankLabel
	// public void redraw() {
	// layoutRankLabel(true);
	// }

	 void draw(boolean redrawOnly) {
//		 Util.print("pv.draw " + p + " " + redrawOnly);
		if (w > 0) {
			// Don't do redraws before we get drawn.
			drawBars(redrawOnly);
			redrawLabels();
		}
	}

	void redrawLabels() {
		loseLabels();
		drawLabels();
	}

	/**
	 * [top, bottom, topLeft, topRight, bottomLeft, bottomRight]
	 */
	private float[] prevLightBeamCoords = null;

	// This has to change shape during animation, so is called in layoutChildren
	// rather than draw.
	//
	// With multiple siblings, the later siblings' curtains would be drawn
	// on top of earlier siblings' light beams,
	// so we have to put the light beams on the parent.
	void layoutLightBeam() {
		if (parentPV != null) {
			Bar parentFrontRect = parentPV.findFrontRect(p);
			if (parentFrontRect != null) {
				// If we select a range of parent values, some might be so small
				// that there's not a bar for it.

				PBounds gBottomRect = front.getGlobalBounds();
				if (gBottomRect.getWidth() > 0) {
					float[] newCoords = new float[6];
					Rectangle2D lBottomRect = rank.globalToLocal(gBottomRect);
					newCoords[1] = (float) (lBottomRect.getY() + front
							.getGlobalBounds().getHeight() / 2);
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
							updateLightBeamTransparency(art().highlightedFacet,
									true);
							rank.addChild(lightBeam);
						} else {
							lightBeam.reset();
							lightBeam.moveToBack();
						}
						float[] Xs = { newCoords[4], newCoords[2],
								newCoords[3], newCoords[5], newCoords[4] };
						float[] Ys = { newCoords[1], newCoords[0],
								newCoords[0], newCoords[1], newCoords[1] };
						lightBeam.setPathToPolyline(Xs, Ys);
					}
				}
			} else
				assert lightBeam == null;
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

	boolean verifyBarTables() {
		// // printBarXs();
		// // Util.print("PV " + getName() + " has " + barTable.size() + "
		// bars.");
		// Perspective prevFacet = barXs[0];
		// assert prevFacet != null;
		// boolean lastEmpty = false;
		// for (int i = 1; i < w; i++) {
		// if (barXs[i] == null)
		// lastEmpty = true;
		// else if (barXs[i] == prevFacet)
		// lastEmpty = false;
		// else {
		// assert lastEmpty == false : i;
		// prevFacet = barXs[i];
		// }
		// }
		// assert lastEmpty == false;
		return true;
	}

	String printBarXs() {
		// PrintArray.printArray(barXs);
		ItemPredicate prevFacet = barXs[0];
		int facetStart = 0;
		for (int i = 1; i < w; i++) {
			if (barXs[i] != null) {
				if (barXs[i] == prevFacet)
					i++;
				else if (facetStart != i - 1)
					Util.print("\nunterminated:");
				Util.print(prevFacet + ": [" + facetStart + ", " + (i - 1)
						+ "]");
				prevFacet = barXs[i];
				facetStart = i;
			}
		}
		return "";
	}

	private int labelHprojectionW;

	// double getExp() {
	// double result = 1;
	// // if (rank.totalChildTotalCount() > 0) {
	// double expectedPercentOn = rank.expectedPercentOn();
	// if (expectedPercentOn > 0 && expectedPercentOn < 1)
	// result = -Math.log(2) / Math.log(expectedPercentOn);
	// // }
	// Util.print(p + " " + expectedPercentOn + " " + result);
	// return result;
	// }

	private void drawBars(boolean redrawOnly) {
		// Util.print("drawBars " + p + " " + redrawOnly + " " + p.nValues());
		assert p.getTotalChildTotalCount() > 0 : p;
		// assert p.getTotalChildTotalCount() >= totalChildOnCount();
		if (redrawOnly) {
			double expectedPercentOn = rank.expectedPercentOn();
			if (expectedPercentOn >= 0) { // Hack so we can
				// draw before
				// our onCounts are updated (in setConnected).

				// double exp = getExp();
				// Util.print("drawBars " + getName() + " "
				// + p.getTotalChildOnCount() + " " + hingeH + " "
				// + expectedPercentOn);
				// if (hingeH > 0 && !foldInitted())
				// initFold();
				Enumeration it = barTable.elements();
				while (it.hasMoreElements()) {
					Bar bar = ((Bar) it.nextElement());
					bar.updateData(expectedPercentOn);
				}
			}
			// steer.setVisible(hingeH);
		} else {
			// Util.print("Drawing initial bars " + getName() + " "
			// + p.getTotalChildOnCount() + " / "
			// + p.getTotalChildTotalCount());
			front.removeAllChildren();
			front.addChild(rankLabel);
			if (percentLabels != null) {
//				front.addChild(percentScale);
				front.addChild(percentLabels[0]);
				front.addChild(percentLabels[1]);
				front.addChild(percentLabels[2]);
				front.addChild(percentLabelHotZone);
				front.addChild(hotLine);
				front.addChild(parentRect);
			}
			if (medianArrow != null) {
				front.addChild(medianArrow);
			}
			// fold.removeAllChildren();
			// if (rank.expectedPercentOn() >= 0 && rank.hingeH() > 0)
			// initFold();
			barTable.clear();
			barXs = new Perspective[w + 2];
			labelXs = new FacetPText[w + labelHprojectionW];
			boolean isOnCountValid = p.getOnCount() >= 0;
			// Will be invalid if we're prefetched but not yet dataUpdated
			if (isOnCountValid)
				p.sortDataIndexByOn();
			double divisor = barWidthRatio();
			int nFreePixels = w;
			for (int i = 0; i < p.nChildren(); i++) {
				Perspective child = isOnCountValid ? p.getNthOnValue(i) : p
						.getNthChild(i);
				nFreePixels -= maybeDrawBar(child, divisor, false);
				// No - need to set mid_x_coords for all facets.
				// if (nFreePixels <= 0)
				// break;
			}
			assert verifyBarTables();
		}
		if (medianArrow != null /* && isConnected() */)
			layoutMedianArrow();
	}

	// double hingeH() {
	// return (p.percentOn() / rank.expectedPercentOn() - 1)
	// * summary.selectedFrontH / summary.selectedFoldH;
	// }

	private int maybeDrawBar(Perspective datum, double divisor,
			boolean forceDraw) {
		assert datum.getParent() == p;
		int barW = 0;
		if (datum.totalCount > 0 && (!forceDraw || lookupBar(datum) == null)) {
			// if !forceDraw, we're drawing all new bars. No need to check
			// table.
			double maxX = datum.cumCount * divisor;
			double minX = Math.max(leftEdge, maxX - datum.totalCount * divisor)
					- leftEdge;
			maxX = Math.min(rightEdge, maxX) - leftEdge;
			assert minX >= 0;
			if (maxX > minX) {
				int iMaxX = (int) maxX;
				int iMinX = (int) minX;
				// Util.print("Adding bar for " + p + "." + datum + " "
				// + (datum.cumCount - datum.totalCount) + "-"
				// + datum.cumCount + "/" + p.getTotalChildTotalCount()
				// + " " + iMinX + "-" + iMaxX + " " + forceDraw);
				assert datum.cumCount <= p.getTotalChildTotalCount();
				assert maxX + 0.99999 <= w : minX + " " + maxX + " " + w;
				x_mid_coords[datum.getIndex()] = (iMaxX + iMinX) >>> 1;
				assert iMinX >= 0 : datum + " " + datum.cumCount + " "
						+ datum.totalCount;
				if (barXs[iMinX] != null) {
					iMinX += 1;
					assert iMinX >= iMaxX || barXs[iMinX] == null : this + " "
							+ datum + " " + printBarXs() + " " + iMinX + "-"
							+ iMaxX ;
				}
				if (barXs[iMaxX] != null) {
					iMaxX -= 1;
					assert ((int) minX) >= iMaxX || barXs[iMaxX] == null : this
							+ " " + datum + " " + printBarXs();
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
					} else if (iMaxX < w - 2
							&& (barXs[iMaxX + 2] == null || barXs[iMaxX + 2] == barXs[iMaxX + 1])) {
						barXs[iMaxX + 2] = barXs[iMaxX + 1];
						iMaxX += 1;
						iMinX = iMaxX;
					} else if (forceDraw) {
						iMinX = (iMinX > 1) ? iMinX - 1 : iMaxX + 1;
						iMaxX = iMinX;
						barTable.remove(barXs[iMinX]);
						// Util.print("Removing bar " + datum);
					}
				}
				if (iMinX <= iMaxX) {
					barW = iMaxX - iMinX + 1;
					Bar bar = new Bar(this, iMinX, barW, datum);
					front.addChild(bar);
					double expectedPercentOn = rank.expectedPercentOn();
					if (expectedPercentOn >= 0 && datum.getOnCount() >= 0) {
						// if (foldInitted()) {
						// bar.initFold(fold, w / 2);
						// assert forceDraw : "fold hasn't been initted on
						// initial
						// drawBars";
						// double exp = getExp();
						bar.updateData(expectedPercentOn);
						if (true || !forceDraw)
							bar.animateData(1.0);
						// }
					}
					setBarX(datum, bar, iMinX);
					setBarX(datum, bar, iMaxX);
				}
			}
		}
		return barW;
	}

	private double barWidthRatio() {
		return (w - 0.999999) / p.getTotalChildTotalCount();
	}

	private void setBarX(Perspective datum, Bar bar, int xCoord) {
		assert validateBarX(datum, xCoord);
		barXs[xCoord] = datum;
		// Util.print("adding bar " + datum + " " + datum.totalCount + " "
		// + datum.getTotalCount());
		barTable.put(datum, bar);
	}

	Bar lookupBar(Perspective facet) {
		Bar result = (Bar) barTable.get(facet);
		// if (result != null)
		// Util.print("lookupBar " + facet + " => " + result.facet);
		return result;
	}

	int nBars() {
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
		double maxX = datum.cumCount * divisor;
		double minX = maxX - datum.totalCount * divisor;
		int iMaxX = (int) (maxX - leftEdge);
		int iMinX = (int) (minX - leftEdge);
		assert xCoord >= iMinX && xCoord <= iMaxX : xCoord + " [" + iMinX
				+ ", " + iMaxX + "]";
		return true;
	}

	private FacetPText mouseNameLabel;

	 void updateSelection(Perspective hoverFacet, boolean state) {
		updateLightBeamTransparency(hoverFacet, state);

		drawMouseLabel();

		Bar bar = lookupBar(hoverFacet);
		if (bar != null) {
			bar.updateSelection(hoverFacet, state);
			// drawLabels();
			if (labels != null && labels.getVisible()) {
				for (int i = 0; i < labels.getChildrenCount(); i++) {
					FacetPText label = (FacetPText) labels.getChild(i);
					if (label.getFacet() == hoverFacet)
						label.setColor();
				}
			}
		}
	}

	 void updateLightBeamTransparency(ItemPredicate hoverFacet,
			boolean state) {
		if (lightBeam != null) {
			lightBeam.setPaint(p.isRestriction(true) ? Markup.greens[2]
					: Color.white);
			if (state && hoverFacet == p)
				lightBeam.setTransparency(0.4f);
			else
				lightBeam.setTransparency(0.2f);
		}
	}

	/**
	 * y coordinate for numeric labels; depends on maxCount.
	 */
	double numY;

	double nameY;

	void loseLabels() {
//		 Util.print("PV.loseLabels " + p + " " + labelXs);
//		 textXindex = 0;
		if (labelXs != null) {
			for (int i = 0; i < labelXs.length; i++)
				labelXs[i] = null;
		}
		labels.removeAllChildren();
		// mouseNameLabel = null; // signals that labels are
		// invalid
	}

	private void initLabels() {
		assert SwingUtilities.isEventDispatchThread();
//		 Util.print("initLabels " + p + " "
//		 + areLabelsInited() + " " + barTable.size() + " " +
//		 p.getOnCount());
		if (!areLabelsInited() && barTable.size() > 0 && p.getOnCount() >= 0) {
			// If there aren't any bars, x_mid_coords hasn't been initialized.
			// This is a problem inside rank.redraw after addPerspective.
			// Util.print("\nPV.initLabels " + getName());
			if (p.isPrefetched()) {
				// summary.q.prefetchData(p);
				int maxCount = rank.maxCount();
				numY = Math.round(art().numWidth(maxCount) + 10.0);
				nameY = Math.floor(summary.selectedLabelH() * 1.4 - numY
						- art().lineH / 2) - 10;

				int freeLabelXs = w + 2;
				Perspective[] allRestrictions = p.allRestrictions();
				int nRestrictions = allRestrictions.length;
				if (allRestrictions.length > 1) {
					Perspective[] copy = new Perspective[nRestrictions];
					System
							.arraycopy(allRestrictions, 0, copy, 0,
									nRestrictions);
					p.sortByOn(copy);
					allRestrictions = copy;
				}
				for (int i = 0; i < allRestrictions.length && freeLabelXs > 0; i++) {
					freeLabelXs -= maybeDrawLabel(allRestrictions[i]);
				}

				// If sort order was really important we should synchronize
				p.sortDataIndexByOn();
				// Util.print("initLabels " + p.sortOrder() + " " + p);
				for (int i = 0; i < p.nChildren() && freeLabelXs > 0; i++) {
					Perspective child = p.getNthOnValue(i);
					// Util.print(child + " " + child.onCount + " " + maxCount);
					freeLabelXs -= maybeDrawLabel(child);
				}
				// Util.print("initLabels done " + p.sortOrder() + " " + p);

				mouseNameLabel = new FacetPText(null, 0.0, -1.0);
				mouseNameLabel.setVisible(false);
				mouseNameLabel.setPickable(false);
				labels.addChild(mouseNameLabel);
			} else {
				queuePrefetch();
			}
		}
	}

	private boolean areLabelsInited() {
		return labels.getChildrenCount() > 0;
	}

	 void drawLabels() {
		if (labels.getVisible()) {
			// if (p.isPrefetched()) {
			// Util.print("drawLabels "
			// + p
			// + " "
			// + (mouseNameLabel == null ? false : mouseNameLabel
			// .getVisible()));
			initLabels();
			drawMouseLabel();
			// } else {
			// summary.fetcher.add(this);
			// }
		}
	}

	private int prevMidX;

	private void drawMouseLabel() {
		// Util.print("drawMouseLabel " + p + "\n" + v);
		if (areLabelsInited()) {
			Perspective mousedFacet = art().highlightedFacet;
			if (mousedFacet != null && mousedFacet.getParent() == p) {
				drawMouseLabelInternal(mousedFacet);
			} else if (mouseNameLabel.getVisible()) {
				drawMouseLabelInternal(null);
			}
		}
	}

	private void drawMouseLabelInternal(Perspective v) {
		int midX;
		boolean state = v != null;
		if (state) {
			midX = x_mid_coords[v.getIndex()];
			assert midX >= 0 : v;
			prevMidX = midX;
		} else
			midX = prevMidX;

		int minX = Util.constrain(midX - labelHprojectionW, 0, w + 1);
		int maxX = Util.constrain(midX + labelHprojectionW, 0, w + 1);
		for (int i = minX; i <= maxX; i++) {
			if (labelXs[i] != null) {
				int midLabel = x_mid_coords[labelXs[i].facet.getIndex()];
				if (Math.abs(midLabel - midX) <= labelHprojectionW) {
					// Util.print("need mouseLabel? " + p + " " + v + " " + " "
					// + labelXs[i].getFacet() + " "
					// + labelXs[i].getVisible() + " "
					// + mouseNameLabel.getVisible());
					if (labelXs[i].getFacet() == v) {
						labelXs[i].setVisible(true);
						mouseNameLabel.setVisible(false);
						return;
					} else
						labelXs[i].setVisible(v == null);
				}
			}
		}

		if (state) {
			maybeDrawBar(v, barWidthRatio(), true);
			assert verifyBarTables();
			mouseNameLabel.setFacet(v);
			mouseNameLabel.setPTextOffset(midX, 0.0);
			labels.moveToFront();
		} else {
			front.moveToFront();
		}
		mouseNameLabel.setVisible(state);
		mouseNameLabel.setPickable(state);
	}

	private int maybeDrawLabel(Perspective v) {
		int nPixelsOccluded = 0;
		if (v.totalCount > 0) {
			int midX = x_mid_coords[v.getIndex()];
			assert midX >= 0 : v;
			assert midX <= w : v + " " + midX + " " + w;
			if (labelXs[midX] == null) {
				maybeDrawBar(v, barWidthRatio(), true);
				assert verifyBarTables();
				if (lookupBar(v) != null) {
					FacetPText label = getFacetPText(v, 0.0, midX);
					labels.addChild(label);

					int minX = Util.constrain(midX - labelHprojectionW, 0,
							w + 1);
					int maxX = Util.constrain(midX + labelHprojectionW, 0,
							w + 1);
					for (int i = minX; i <= maxX; i++) {
						if (labelXs[i] == null)
							nPixelsOccluded++;
						labelXs[i] = label;
					}
					// Util.print("maybeDrawLabel " + v + " " + minX + " - " +
					// maxX);
				}
			}
		}
		return nPixelsOccluded;
	}

	// void toggleFacet(Perspective facet, int modifiers) {
	// // Util.print("toggleFacet " + facet);
	// if (p.restrictions().length == 0 && facet.getOnCount() == 0) {
	// summary.art
	// .setTip("There would be no results if you added this filter. (There are
	// now "
	// + summary.q.describeNfilters() + ")");
	// } else if (p.toggleFacet(facet, modifiers)) {
	// // highlightFacet(facet, true, modifiers);
	// summary.art.updateAllData();
	// }
	// summary.art.arrowFocus = facet;
	// }
	//
	// int totalChildTotalCount() {
	// return p.getTotalChildTotalCount();
	// }

	// int totalChildOnCount() {
	// return p.getTotalChildOnCount();
	// }
	//
	// double maxChildPercentOn() {
	// return p.maxChildPercentOn();
	// }
	//
	// boolean allOn() {
	// return (p.getOnCount() == p.totalCount);
	// }

	boolean isConnected() {
		return rank.isConnected();
	}

	void connectToPerspective() {
		rank.connect();
	}

	void setConnected(boolean connected) {
		// Util.print("PV.setConnected " + p.getName() + " " + connected);
		// if (connected) {
		// // initFold();
		// draw(true);
		// }

		// double hingeH = 0;
		// if (connected && summary.art.showImages) {
		// if (p.getTotalChildTotalCount() >= 0) {
		// double expectedPercentOn = rank.expectedPercentOn();
		// hingeH = rank.hingeH();
		//
		// Enumeration it = barTable.elements();
		// while (it.hasMoreElements()) {
		// Bar bar = ((Bar) it.nextElement());
		// bar.ensureFoldImage(expectedPercentOn, hingeH);
		// }
		// }
		// }
		// steer.setVisible(hingeH);

//		if (medianArrow != null) {
//			// if (connected)
//			layoutMedianArrow();
//			// medianArrow.setVisible(connected);
//		}
		if (rankLabel.getVisible()) {
			// rankLabel.setUnderline(!connected);
			float transparency = connected ? 1 : 0;
			for (int i = 0; i < 3; i++) {
				percentLabels[i].animateToTransparency(transparency,
						Bungee.dataAnimationMS);
			}
			redraw100PercentLabel();
//			percentScale.animateToTransparency(transparency,
//					Art.dataAnimationMS);
			percentLabelHotZone.setPickable(connected);
//			layoutPercentLabels();

			rankLabel.unpickableAction = connected ? -1 : -2;
			rankLabel.setPickable(connected);
		}
	} // Visibiility is determined by whether we are rank's first perspective

	// (in
	// validate).
	// and also by parent's visibility (the fold).
	// Transparency and position are determined by curtainH
	void redraw100PercentLabel() {
		if (isConnected() && rank.totalChildTotalCount() >= 0) {
			// Util.print("PV.redrawPercents " + p.getName() + " " + curtainH +
			// " "
			// + (Art.lineH / summary.foldH));
			// assert curtainH <= 1;
			// double foldH = summary.selectedFoldH;
			// double lineH = summary.art.lineH;
			double percent = rank.expectedPercentOn();
			percentLabels[1].setText(ResultsGrid.formatPercent(percent, null)
					.toString());
			percentLabels[2].setVisible(percent < 1);
//			drawPercentScale();
			// double yOffset = -lineH / 6.0 / foldH;
			// double xOffset = Math.round(-1.2 * percentLabelW *
			// percentLabelScale);
			// double foldPercent = (1.0 - (curtainH - yOffset));
			// The -yOffset is to make up for yOffset not being -h/2, which is
			// in
			// turn
			// because the curtain would occlude the top half.

			// if ((1 - curtainH) > percentLabelScale * lineH / foldH) {
			// // double x = Math.round(foldPercent * getWidth() / 2.0 +
			// xOffset);
			// updatePercentLabel(percentLabels[2], 1f, xOffset, curtainH +
			// yOffset);
			// } else {
			// updatePercentLabel(percentLabels[2], 0f, xOffset, 1 + yOffset);
			// }

			// if ((1 - curtainH) > 2 * percentLabelScale * lineH / foldH) {
			// // double x = Math.round(foldPercent * getWidth() / 4.0 +
			// xOffset);
			// double y = curtainH + foldPercent / 2.0 + yOffset;
			// double halfHingeH = Bar.invertHingeYb(this, (1 + curtainH) /
			// 2.0);
			// double frontH = summary.selectedFrontH / foldH;
			// double percent = (halfHingeH + frontH) / (rank.hingeH() +
			// frontH);
			// percentLabels[3].setText(((int) (percent * 100 + 0.5)) + "%");
			// updatePercentLabel(percentLabels[3], 1f, xOffset, y);
			// } else
			// updatePercentLabel(percentLabels[3], 0f, xOffset, 0*1 + yOffset);
		}
	}

//	void drawPercentScale() {
//		double w = percentLabelW;
//		// double exp = 1 / getExp();
//		float[] Xs = new float[12];
//		float[] Ys = new float[12];
//		int i = 0;
//		for (double y = 0; y <= 1; y += 0.1) {
//			double percent = rank.unwarp(1 - y);
//			Ys[i] = (float) (rank.warpPower() == 1 ? y / 2 + 0.5 : y);
//			Xs[i] = (float) (w * (1 - percent));
//			i++;
//		}
//		Xs[11] = Xs[10];
//		Ys[11] = Ys[0];
//		// Util.print("");
//		// PrintArray.printArray(Xs);
//		// PrintArray.printArray(Ys);
//		// gui.Util.printDescendents(percentScale);
//		percentScale.setPathToPolyline(Xs, Ys);
//	}

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

	void highlightFacet(Perspective facet, boolean state, int modifiers) {
		// Util.print("PV.highlightFacet " + facet);
		art().highlightFacet(facet, state, isConnected() ? modifiers : -2);
	}

	// void showDoc(Perspective facet, boolean state) {
	// if (facet == null)
	// facet = p;
	// highlightFacet(facet, state, -1);
	// if (isConnected() && state)
	// art().setMouseDoc("List the values for this category", state);
	// }

	public boolean pick(FacetText ignore, int modifiers) {
		// Util.print("PV.pick " + p + " " + node + " " + modifiers + " "
		// + node.isPickable);
		boolean result = isConnected() && modifiers == 0; // && node ==
		// rankLabel.getChild(0);
		if (result) {
			art().ensurePerspectiveList(true);
			PerspectiveList perspectiveList = art().perspectiveList;
			boolean state = perspectiveList.isHidden();
			// state is set to the new (toggled) value
			if (state)
				perspectiveList.setSelected(p.getNthChild(0));
			perspectiveList.show(state);
		}
		return result;
	}

	public boolean mouseMoved(FacetText ignore1, int ignore2) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean highlight(FacetText node, boolean state, int modifiers) {
		highlightFacet(node.getFacet(), state, modifiers);
		if (isConnected() && state) {
			PerspectiveList perspectiveList = art().perspectiveList;
			String doc = (perspectiveList == null || perspectiveList.isHidden()) ? "List the values for this category"
					: "Hide the list of values";
			art().setMouseDoc(doc, state);
		}
		return true;
	}

	double frontBottomOffset() {
		return front.getYOffset()
				+ Util.min(Math.round(front.getFullBounds().getHeight()), rank
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
		if (label == null || label.numW != numY || label.nameW != nameY) {
			label = new FacetPText(_facet, _y, x);
			if (_facet != null)
				facetPTexts.put(_facet, label);
		} else {
			 label.setPTextOffset(x, _y);
			label.setText(label.art.facetLabel(_facet, numY, nameY, false,
					true, label.showCheckBox, true, label));
			label.setColor();
		}
		return label;
	}

	class FacetPText extends FacetText {

		private static final long serialVersionUID = 6260447554538885678L;
		
		void setFacet(Perspective _facet) {
			facet = _facet;
			assert facet.getParent() != null;
			setText(art.facetLabel(facet, numY, nameY, false, true,
					true, true, this));
			
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
			super(summary.art, numY, nameY);
			setRotation(-Math.PI / 4.0);
//			setPaint(Art.summaryBG);

			showCheckBox = true;
			isPickable = showCheckBox;
			setUnderline(isPickable);
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

		public boolean pick(PInputEvent e) {
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
				super.pick(e);
				// toggleFacet(facet, modifiers);
				// p.art.printUserAction("FacetPText.picked: " +
				// p.summary.q.getFacetName(facet));
			} else
				connectToPerspective();
			return true;
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
			if (state) {
				// Workaround Piccolo rotated-selection bug by
				// skipping redundant calls and checking for erroneous ones.
				if (art.highlightedFacet != facet) {
					if (x >= -5.0 && x <= getWidth() + 5 && _y >= -5.0
							&& _y <= getHeight() + 5.0) {
						highlight(state, modifiers);
					}
				}
			} else if (art.highlightedFacet == facet) {
				highlight(state, modifiers);
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

	Bar findFrontRect(Perspective facet) {
		Bar result = null;
		Bar bar = lookupBar(facet);
		if (bar != null)
			result = bar;
		return result;
	}

	void clickBar(Perspective facet, int modifiers) {
		findFrontRect(facet).pick(modifiers);
	}

	public String toString() {
		return "<PerspectiveViz " + p + ">";
	}

	public void restrict() {
		// if (//p.getQuery().usesPerspective(p) &&
		// p.restrictData())
		// During a restrict, our parent may have removed us,
		// and calling restrictData would wrongly resetData to -1
		// if (p.getQuery().usesPerspective(p))
		draw(false);
	}

	public void startDrag(Point2D ignore, Point2D local) {
		assert Util.ignore(ignore);
		dragStartOffset = local.getX();
		// Util.print(dragStartOffset);
	}

	private double leftEdge = 0;

	private double rightEdge = 0;

	private double dragStartOffset;

	public void drag(Point2D ignore, PDimension delta) {
		assert Util.ignore(ignore);
		int viewW = (int) (rightEdge - leftEdge);
		double zoom = Math.pow(2, -delta.getHeight() / 20.0);
		int newW = (int) Math.round(w * zoom);
		if (newW < viewW) {
			leftEdge = 0;
			rightEdge = viewW;
			newW = viewW;
		} else {
			zoom = newW / (double) w;
			double pan = -delta.getWidth();
			leftEdge = Util.constrain(leftEdge + pan
					+ (leftEdge + dragStartOffset) * (zoom - 1), 0, newW
					- viewW);
			rightEdge = leftEdge + viewW;
		}
		// Util.print(zoom + " " + viewW + " " + newW + " " + leftEdge + " " +
		// rightEdge);
		revalidate(newW);
	}

	class MedianArrow extends Arrow {

		final MedianArrowHandler medianArrowHandler = new MedianArrowHandler();

		ItemPredicate unconditionalMedian;

		ItemPredicate conditionalMedian;

		int significant = 0;

		int highlighted = 1;

		public MedianArrow(Paint headColor, Paint tailColor, int tailDiameter,
				int length) {
			super(headColor, tailColor, tailDiameter, length);
			setPickable(false);
			// line.setPickable(false);
			leftHead.addInputEventListener(medianArrowHandler);
			rightHead.addInputEventListener(medianArrowHandler);
			tail.addInputEventListener(medianArrowHandler);
			line.addInputEventListener(medianArrowHandler);
		}

		void updateColor(int _significant) {
			significant = _significant;
			redraw();
		}

		void redraw() {
			Color[] colors = significant == 0 ? Markup.whites
					: (significant > 0 ? Markup.blues : Markup.oranges);
			Color color = colors[highlighted];
			setHeadColor(color);
		}

		void highlight(boolean state) {
			highlighted = state ? 2 : 1;
			redraw();
		}

		class MedianArrowHandler extends MyInputEventHandler {

			public MedianArrowHandler() {
				super(PPath.class);
			}

			public boolean exit(PNode node) {
				art().showMedianArrowDesc(null);
				highlight(false);
				return true;
			}

			public boolean enter(PNode ignore) {
//				boolean unconditional = node == tail;
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

	Bungee art() {
		return summary.art;
	}

	public PNode anchorForPopup(Perspective facet) {
		PNode bar;
		if (facet == p) {
			bar = rankLabel;
		} else {
			if (areLabelsInited())
				drawMouseLabelInternal(facet);
			else
				maybeDrawBar(facet, barWidthRatio(), true);
			bar = findFrontRect(facet);
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
}

// This goes on rankLabel
// class ShowPerspectiveDocHandler extends MyInputEventHandler {
//
// public ShowPerspectiveDocHandler() {
// super(PText.class);
// }
//
// PerspectiveViz getPerspectiveViz(PNode node) {
// if (node == null || node instanceof PerspectiveViz)
// return (PerspectiveViz) node;
// return getPerspectiveViz(node.getParent());
// }
//
// Perspective getFacet(PNode node) {
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

class HotZoneListener extends PBasicInputEventHandler {

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

class SqueezablePNode extends LazyPNode {

	private static final long serialVersionUID = -8508360739396784758L;

	PBounds clip;

	double goalScale = -1;

	private double goalY = -1;

	// private long loadTime;

	public SqueezablePNode() {
		setBounds(0, 0, 1, 1);
		setPickable(false);
	}

	void setVisible() {
		// PerspectiveViz pv = ((PerspectiveViz) getParent());
		setVisible(goalScale > 0);
	}

	 void layout(double y, double yScale) {
//		 Util.print("PV.layout scale = " + yScale + " " + goalScale + " " + y + " " + goalY);
		// PerspectiveViz pv = ((PerspectiveViz) getParent());
		if (yScale != goalScale || y != goalY) {
			goalScale = yScale;
			goalY = y;
			boolean isVisible = (yScale > 0);
//			 Util.print("PV.layout scale = " + yScale + " " + y);
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

	 protected void paint(PPaintContext paintContext) {
		// loadTime = new Date().getTime();
		if (clip != null) {
			// Util.print("SqueezablePNode.paint " + clip.getY() + " " +
			// getVisible());
			paintContext.pushClip(clip);
			super.paint(paintContext);
			paintContext.popClip(clip);
		} else
			super.paint(paintContext);
	}

	//  void paintAfterChildren(PPaintContext paintContext) {
	// if (clip != null)
	// paintContext.popClip(clip);
	//
	// // Util.print("painting " + ((PerspectiveViz) getParent()).p + " took "
	// // + (new Date().getTime() - loadTime));
	// }
}
