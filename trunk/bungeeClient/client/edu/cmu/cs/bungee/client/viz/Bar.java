package edu.cmu.cs.bungee.client.viz;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;


class Bar extends LazyPNode implements FacetNode {

	private static final double MIN_BAR_HEIGHT = 0.02;

	private static final long serialVersionUID = -3967717713367983041L;

	// static final boolean useDeviationBars = false;

	// private final static double cameraZ = 200.0;

	/**
	 * colors[fadeIndex][colorIndex]. colorIndex is just facet.getIndex() %
	 * Summary.nColors; fadeIndex: 0 = faded; 1 = bright; 2 = highlight; 3 =
	 * fadedSelected; 4 = brightSelected; 5 = hightlightSelected.
	 */
	// static final Color[][] colors = initColors();
	// zero gives thinnest possible line, and Piccolo docs say it's most
	// efficient.
	// stroke is used on selected bar fadedFolds.
	PerspectiveViz pv;

	Perspective facet;

	// FacetPPath fold;

	// FacetPNode front;
	Rectangle2D.Double rect;

	double startY;

	/**
	 * Ranges from 1.0 if observedPercent = 1.0 to 2.0 if observedPercent = 0.0
	 */
	double goalY;

	// This is where we are, allowing that we don't repaint if we're within a
	// few pixels.
	double sloppyY;

	// PBounds brightFoldClip;

	// PBounds brightFrontClip;

	int colorIndex;

	int fadeIndex = 0;

	// boolean doPaint = true;

	// static AffineTransform identity = new AffineTransform();

	Bar(PerspectiveViz _p, double x, double w, Perspective _facet) {
		assert w >= 0.0 : "drawBar problem";
		assert _p.p != _facet : _facet;
		pv = _p;
		facet = _facet;
		facet.ensureInstantiatedPerspective();
		assert checkXparam(x);
		assert checkXparam(w);
		assert checkXparam(x + w);
		// colorIndex = facet.getIndex() % Art.nColors;
		startY = 1.5;
		sloppyY = startY;
		goalY = startY;
		// brightFoldClip = new PBounds(-1000, startY, 2000, 1);
		// brightFrontClip = new PBounds(-1000, 0.5, 2000, useDeviationBars ?
		// 0.01 : 2);

		// front = new FacetPNode(x, 0.0, w, 1);
		// assert checkParam(x, false);
		// assert checkParam(y, false);
		// assert checkParam(w, true);
		// assert checkParam(h, true);
		// canTransformInsteadOfSetBounds = true;
		setBounds(x, 0, w, 1);
		rect = new Rectangle2D.Double(x, 0.5, w, 0.5);
		// addInputEventListener(Art.facetClickHandler);

		updateSelection(art().highlightedFacet, true);
		// parent.addChild(front);
	}
	
	PBounds visibleBounds() {
		return new PBounds(getX(), 1+MIN_BAR_HEIGHT-rect.height, getWidth(), rect.height);
	}

	// void initFold(PNode parent, float center) {
	// if (fold == null) {
	// assert checkXparam(center);
	// fold = new FacetPPath(this, center);
	// fold.setPaint(getColor(0));
	// parent.addChild(fold);
	// }
	// }

	// static Color[][] initColors() {
	// Color[][] result = new Color[9][Art.nColors];
	// for (int i = 0; i < Art.nColors; i++) {
	// // Color color = Summary.colorForIndex(i);
	// result[0][i] = Art.colors[i][0];
	// result[1][i] = Art.colors[i][1];
	// result[2][i] = Art.colors[i][2];
	// result[3][i] = Art.greens[1]; // selectedFadedBarColor;
	// result[4][i] = Art.greens[2]; // selectedBarColor;
	// result[5][i] = Art.greens[3]; // highlightSelectedBarColor;
	// result[6][i] = Art.reds[1]; // selectedFadedBarColor;
	// result[7][i] = Art.reds[2]; // selectedBarColor;
	// result[8][i] = Art.reds[3]; // highlightSelectedBarColor;
	// }
	// return result;
	// }
	//
	// Color getColor(int deltaFadeIndex) {
	// return colors[fadeIndex + deltaFadeIndex][colorIndex];
	// }

	void updateSelection(ItemPredicate mousedFacet, boolean state) {
		// Util.print("Bar.updateSelection for bar " + facet + "; mousedFacet =
		// "
		// + mousedFacet);
		int oldIndex = fadeIndex;
		fadeIndex = 0;
		if (facet.isRestriction(true))
			fadeIndex += 3;
		if (facet.isRestriction(false))
			fadeIndex += 6;
		assert fadeIndex < 9;
		if (state && facet == mousedFacet)
			fadeIndex++;

		if (fadeIndex != oldIndex) {
			invalidatePaint();
			// if (fold != null && fold.getParent().getVisible()) {
			// fold.invalidatePaint();
			// }
		}
	}

	// boolean isMoused() {
	// // Util.print("isMoused " + facet + " " + fadeIndex % 3);
	// return fadeIndex % 3 == 1;
	// }

	// // This is where we are, theoretically
	// double currentY() {
	// return (brightFrontClip.getY() == 0) ? brightFoldClip.getY()
	// : 1 + brightFrontClip.getY();
	// }

	void updateData(double expectedPercentOn) {
		startY = sloppyY; // currentY();
		double observedPercentOn = facet.percentOn();
		if (observedPercentOn > 0 && expectedPercentOn < 1) {
			observedPercentOn = pv.rank.warp(observedPercentOn);
			// if (pv.p.getName().equals("Genre"))
			// Util.print("updateData " + facet + " " + expectedPercentOn + " "
			// + pv.rank.warpPower() + " "
			// + observedPercentOn);
		} else if (expectedPercentOn == 1)
			observedPercentOn = 0.5;
		if (expectedPercentOn > 0)
			goalY = 2.0 - observedPercentOn;
		else
			goalY = 2.0;
		// if (pv.summary.art.showImages)
		// updateImages(hingeOnH, hingeH);
		if (observedPercentOn == expectedPercentOn)
			// speed up for common case
			colorIndex = 0;
		else
			colorIndex = art().chiColorFamily(facet);
	}

	void animateData(double zeroToOne) {
		double y = startY + zeroToOne * (goalY - startY);
		if (y != sloppyY && (zeroToOne == 1.0 || Math.abs(y - sloppyY) > 0.1)) {
			sloppyY = y;
			double minH = MIN_BAR_HEIGHT; // 1.0 / pv.rank.frontH;
			double frontDimH = y - 1 - minH / 2.0;
			// double foldDimH = 1;
			// if (y < 1) {
			// // The whole front is bright.
			// frontDimH = 0;
			// // foldDimH = y;
			// } else {
			// frontDimH = y - 1;
			// // foldDimH = 1;
			// }
			// if (useDeviationBars && brightFrontClip.getY() != frontDimH) {
			// assert frontDimH >= 0 : frontDimH;
			// if (frontDimH > 0.5) {
			// brightFrontClip.setRect(-1000, 0.5, 2000, frontDimH-0.5);
			// } else {
			// brightFrontClip.setRect(-1000, frontDimH, 2000, 0.5-frontDimH);
			// }
			// front.invalidatePaint();
			// } else
			if (rect.getY() != frontDimH) {
				rect.setRect(rect.getX(), frontDimH, rect.getWidth(), 1
						- frontDimH + minH);
				invalidatePaint();
				// Util.print("animateData " + facet + " " + frontDimH);
			}
			// if (fold != null
			// && (curtainChanged || brightFoldClip.getY() != foldDimH)) {
			// brightFoldClip.setOrigin(-1000, foldDimH);
			// fold.invalidatePaint();
			// }
		}
	}

	/**
	 * @param L
	 *            The length of the fold, scaled by foldH.
	 * @return The viewpoint height of the curtain, scaled by foldH. y=1 means
	 *         the curtain covers the whole fold (and L=0). See computeAngle.ppt
	 *         for explanation.
	 */
	// static double curtainHeight(PerspectiveViz pv, double L) {
	// double foldH = pv.summary.selectedFoldH;
	// double cameraZ = pv.cameraZ();
	// assert foldH > 0 : foldH;
	// assert cameraZ > 0 : cameraZ;
	// assert L > 0 && L <= pv.rank.hingeH() + 0.00001 : pv.p + " "
	// + pv.rank.hingeH() + " " + L;
	// double cameraZ2 = cameraZ * cameraZ;
	// double y = 1 - 1 / (1 + cameraZ
	// * Math.sqrt(foldH * foldH / cameraZ2 + 1) / (L * foldH));
	// // Util.print("curtainHeight " + foldH + " " + cameraZ + " " + L + " =>
	// // " + y);
	// assert y >= 0 && y <= 1 : L + " => " + y;
	// // assert Math.abs(invertHingeYb(foldH, cameraZ, y) - L) < 0.1 : y
	// // + " " + L + " " + invertHingeYb(foldH, cameraZ, y);
	// // assert Math.abs(y - computeHingeYb(foldH, L)) < 0.01 : L + " " + y +
	// // " " + computeHingeYb(foldH, L);
	// return y;
	// }
	//
	// static double invertHingeYb(PerspectiveViz pv, double y) {
	// double foldH = pv.summary.selectedFoldH;
	// double cameraZ = pv.cameraZ();
	// assert foldH > 0 : foldH;
	// assert cameraZ > 0 : cameraZ;
	// assert y >= 0 && y >= pv.curtainGoalY - 0.00001 && y <= 1 :
	// pv.curtainGoalY
	// + " " + y;
	// double L = cameraZ * Math.sqrt(foldH * foldH / cameraZ / cameraZ + 1)
	// * (1 - y) / (foldH * y);
	// // Util.print("invertHingeYb " + foldH + " " + cameraZ + " " + y + " =>
	// // " + L);
	// assert L >= 0 && L <= pv.rank.hingeH() + 0.00001 : y + " "
	// + pv.rank.hingeH() + " => " + L;
	// assert Math.abs(curtainHeight(pv, L) - y) < 0.0001 : y + " " + L + " "
	// + curtainHeight(pv, L);
	// return L;
	// }
	boolean checkXparam(double param) {
		assert !Double.isInfinite(param) && !Double.isNaN(param);
		assert param >= 0 : param;
		assert param <= pv.getWidth() : param + " " + pv.getWidth();
		assert Math.round(param) == param;
		return true;
	}

	// boolean checkParam(double param, boolean isPositive) {
	// assert !isPositive || param > 0;
	// assert !Double.isInfinite(param) && !Double.isNaN(param);
	// return true;
	// }

	public void setPaint(Paint ignore) {
		assert false : "FacetPNode.paint ignores paint";
	}

	Color getColor() {
		return art().facetTextColor(facet, facet.getOnCount(), colorIndex);
	}

	 protected void paint(PPaintContext paintContext) {
		// Util.print("fpn.paint " + rect);
		Graphics2D g2 = paintContext.getGraphics();
		g2.setPaint(getColor()); // getColor(1));
		g2.fill(rect);
		if (rect.height > 2.0 / pv.rank.frontH)
			g2.setPaint(Color.black);
		g2.setStroke(LazyPPath.getStrokeInstance(0));
		g2.draw(rect);
	}

	public boolean pick(PInputEvent e) {
		return pick(e.getModifiersEx());
	}

	public boolean pick(int modifiers) {
		art().printUserAction(Bungee.BAR, facet, modifiers);
		if (pv.isConnected()) {
			art().toggleFacet(facet, modifiers);
			// p.summary.art.printUserAction("FacetPNode.picked: " +
			// p.summary.q.getFacetName(facet));
		} else
			pv.connectToPerspective();
		return true;
	}

	public boolean highlight(boolean state, int modifiers, PInputEvent ignore) {
		// Util.print("Bar.FacetPNode.highlight " + facet + " " + state);
		pv.highlightFacet(facet, state, modifiers);

		// pv.summary.barHelp.focus(pv, facet, state);

		return true;
	}

	public void mayHideTransients(PNode ignore) {
		pv.hidePvTransients();
	}

	public Perspective getFacet() {
		return facet;
	}

	public Bungee art() {
		return pv.summary.art;
	}

	// public void updateSelectionX(boolean state) {
	// updateSelection(facet, state);
	// }
}
