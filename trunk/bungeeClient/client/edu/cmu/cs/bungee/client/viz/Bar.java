package edu.cmu.cs.bungee.client.viz;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PPaintContext;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mad y-coordinates range from 0 if observedPercent = 1.0 to 1 if
 *         observedPercent = 0.0, except that rect extends an extra
 *         MIN_BAR_HEIGHT/2 above and below
 * 
 */
final class Bar extends LazyPNode implements FacetNode {
	
//	static int paintCount = 0;

	private static final double MIN_BAR_HEIGHT = 0.02;

	private PerspectiveViz pv;

	private Perspective facet;

	// private int colorIndex;

	// private byte fadeIndex = 0;

	private Rectangle2D.Double rect;

	private double startY;

	private double goalY;

	private double currentY;

	private static List barCache = new LinkedList();

	static Bar getBar(PerspectiveViz _pv, double x, double w, Perspective _facet) {
		Bar result = null;
		if (barCache.isEmpty()) {
			result = new Bar();
			result.rect = new Rectangle2D.Double(x, 0.5-MIN_BAR_HEIGHT/2, w,
					MIN_BAR_HEIGHT);
		} else {
			result = (Bar) barCache.remove(0);
			assert result != null;
		}

		result.pv = _pv;
		result.facet = _facet;
		assert result.checkXparam(x);
		assert result.checkXparam(w);
		assert result.checkXparam(x + w);
		assert _pv.p == _facet.getParent() : _facet;
		result.startY = 0.5;
		// sloppyY = startY;
		result.goalY = 0.5;
		result.currentY = result.startY;
		result.setBounds(x, 0, w, 1);
		result.rect.setFrame(x, 0.5-MIN_BAR_HEIGHT/2, w, MIN_BAR_HEIGHT);
		// addInputEventListener(Art.facetClickHandler);

		result.updateSelection();
		return result;
	}

	static void release(Collection toRelease) {
		assert noNulls(toRelease);
		barCache.addAll(toRelease);
	}

	static boolean noNulls(Collection c) {
		for (Iterator it = c.iterator(); it.hasNext();) {
			Object o = it.next();
			assert o != null;
		}
		return true;
	}

	static void release(Bar bar) {
		assert bar != null;
		barCache.add(bar);
	}

	// /**
	// * This is where we are, allowing that we don't repaint if we're within a
	// * few pixels.
	// */
	// // private double sloppyY;
	// Bar(PerspectiveViz _pv, double x, double w, Perspective _facet) {
	// pv = _pv;
	// facet = _facet;
	// assert checkXparam(x);
	// assert checkXparam(w);
	// assert checkXparam(x + w);
	// assert _pv.p == _facet.getParent() : _facet;
	// startY = y0;
	// // sloppyY = startY;
	// goalY = y1;
	// currentY = startY;
	// setBounds(x, 0, w, 1);
	// rect = new Rectangle2D.Double(x, y0, w, 1-y1);
	// // addInputEventListener(Art.facetClickHandler);
	//
	// updateSelection();
	// }

	private boolean checkXparam(double param) {
		assert !Double.isInfinite(param) && !Double.isNaN(param);
		assert param >= 0 : param;
		assert param <= pv.getWidth() : param + " " + pv.getWidth();
		assert Math.round(param) == param;
		return true;
	}

	Rectangle2D visibleBounds() {
		return (Rectangle2D) rect.clone();
		// return new PBounds(getX(), rect.getY(), getWidth(),
		// rect.height);
	}

	void updateSelection() {
		// Util.print("Bar.updateSelection for bar " + facet + "; mousedFacet =
		// "
		// + mousedFacet);
		// int oldIndex = fadeIndex;
		// fadeIndex = 0;
		// if (facet.isRestriction(true))
		// fadeIndex += 3;
		// if (facet.isRestriction(false))
		// fadeIndex += 6;
		// assert fadeIndex < 9;
		// if (facet == art().highlightedFacet)
		// fadeIndex++;
		//
		// if (fadeIndex != oldIndex) {
		// invalidatePaint();
		// }

		Color color = art().facetTextColor(facet);
		// if (!color.equals(getPaint())) {
		setPaint(color);
		// invalidatePaint();
		// }
	}

	void updateData() {
		startY = currentY;
		// goalY = expectedPercentOn == 1 ? 0.5 : 1 - pv.rank.warp(facet
		// .percentOn());
		goalY = 1 - Rank.warp(facet.percentageRatio());
		updateSelection();

		// Try this to increase the salience of bars that max out - so they
		// aren't perceived as background.
		// goalY = (goalY - 0.5) * 0.9 + 0.5;
	}

	void animateData(double zeroToOne) {
		double y = lerp(zeroToOne, startY, goalY);
		double slop = zeroToOne == 1.0 ? 0.0 : 0.1;
		if (Math.abs(y - currentY) > slop) {
			currentY = y;
			double top;
			double h;
			if (y > 0.5) {
				top = 0.5-MIN_BAR_HEIGHT/2;
				h = Math.max(MIN_BAR_HEIGHT, y-0.5);
			}else {
				top = Math.min(0.5-MIN_BAR_HEIGHT/2, y);
				h = Math.max(MIN_BAR_HEIGHT, 0.5-y+MIN_BAR_HEIGHT/2);				
			}
			rect.setRect(rect.getX(), top, rect.getWidth(), h);
			invalidatePaint();
			// Util.print("animateData " + facet + " " + frontDimH);
		}
	}

	// public void setPaint(Paint ignore) {
	// assert false : "FacetPNode.paint ignores paint";
	// }

	// private Color color() {
	// return art().facetTextColor(facet, facet.guessOnCount());
	// }

	protected void paint(PPaintContext paintContext) {
//		paintCount++;
//		if ("Army".equals(facet.getNameIfPossible()))
//		 Util.print("bar.paint " + facet + " " + currentY +" "+ goalY);
		Graphics2D g2 = paintContext.getGraphics();
		// int onCount = facet.guessOnCount();
		// g2.setPaint(Markup.UNASSOCIATED_COLORS[fadeIndex]);
		// g2.fill(getBoundsReference());
//		Color color = art().facetTextColor(facet, facet.guessOnCount());

		// remember paint so updateSelection can detect changes
//		setPaint(color);
		g2.setPaint(getPaint()); // getColor(1));
		g2.fill(rect);
		// if (rect.height > 2.0 / pv.rank.frontH()) {
		// if (facet.getID() % 2 == 0) {
		// strokes separate bars, so only need to draw them for alternate
		// bars.
		// NO! We depend on strokeW=0 to add transparency so we can see a bar's
		// color even
		// if it's width is 1 and the stroke overwrites the interior.
		if (currentY == goalY) {
			// Save time by not painting boundaries during animation
			g2.setPaint(Color.black);
			g2.setStroke(LazyPPath.getStrokeInstance(0));
			g2.draw(getBoundsReference());
		}
		// }
		// }
	}

//	public boolean pick(PInputEvent e) {
//		return pick(e.getModifiersEx());
//	}

	public boolean pick(int modifiers, PInputEvent e) {
		assert Util.ignore(e);
		art().printUserAction(Bungee.BAR, facet, modifiers);
		// Util.print("Bar.pick " + facet + " " + pv.isConnected());
		if (pv.isConnected()) {
			art().toggleFacet(facet, modifiers);
			// p.summary.art.printUserAction("FacetPNode.picked: " +
			// p.summary.q.getFacetName(facet));
		} else
			pv.connectToPerspective();
		return true;
	}

	public boolean highlight(boolean state, int modifiers, PInputEvent e) {
		// Util.print("Bar.FacetPNode.highlight " + facet + " " + state);
		pv.highlightFacet(state ? facet : null, modifiers, e);

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

	public String toString() {
		return "<Bar " + facet + ">";
	}

	public void drag(Point2D position, PDimension delta) {
		assert false;
	}

	public FacetNode startDrag(PNode node, Point2D positionRelativeTo) {
		// TODO Auto-generated method stub
		// return pv.startDrag(node, positionRelativeTo);
		return null;
	}
}
