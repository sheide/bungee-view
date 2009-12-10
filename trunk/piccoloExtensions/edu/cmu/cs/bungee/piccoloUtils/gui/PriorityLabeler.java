package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.util.Iterator;

import javax.swing.SwingUtilities;

import edu.cmu.cs.bungee.javaExtensions.Labeled;
import edu.cmu.cs.bungee.javaExtensions.Util;

// Create labeler, set widths, set objects 

public class PriorityLabeler {

	private final Labeled labeled;

	private static final double epsilon = 1.0e-10;

	private int iVisibleWidth;

	/**
	 * Our logical width, of which floor(leftEdge) - floor(leftEdge +
	 * visibleWidth) is visible. logicalWidth>=visibleWidth();
	 * 
	 * visibleWidth = w - epsilon bars are placed at floor(0 - logicalWidth)
	 */
	private double logicalWidth = 0;

	/**
	 * offset into logicalWidth of the leftmost visible pixel. Rightmost visible
	 * pixel is leftEdge + w - epsilon 0<=leftEdge<logicalWidth-visibleWidth;
	 */
	private double leftEdge = 0;

	private int labelHprojectionW;

	/**
	 * The facet of the label at a given x coordinate. All points are recorded.
	 */
	private Object[] labelXs;

	// private Hashtable cumCounts = new Hashtable();

	private final int totalCount;

	public PriorityLabeler(Labeled labeled, int width, int labelW) {
		this.labeled = labeled;
		iVisibleWidth = width;
		logicalWidth = width;
		labelHprojectionW = labelW;
		// this.objects = objects;
		int total = 0;
		for (Iterator it = labeled.getChildIterator(); it.hasNext();) {
			Object o = it.next();
			total += labeled.count(o);
		}
		totalCount = total;
		drawLabels();
	}

	// private boolean isZooming() {
	// return logicalWidth > visibleWidth();
	// }
	//
	// private void resetLogicalBounds() {
	// setLogicalBounds(0, visibleWidth());
	// }
	//
	// private void setLogicalBounds(double x, double w) {
	// // Util.print("setLogicalBounds " + x + " " + w);
	// assert x >= 0 && x + visibleWidth() <= w : x + "/" + w + " "
	// + visibleWidth();
	// if (x != leftEdge || w != logicalWidth) {
	// leftEdge = x;
	// logicalWidth = w;
	// drawLabels();
	// }
	// }

	 double visibleWidth() {
		return iVisibleWidth - epsilon;
		// return getWidth() - 1.0e-12;
	}

	private int getCount(Object child) {
		return labeled.count(child);
	}

	private int cumCountInclusive(Object child) {
		return labeled.cumCountInclusive(child);
	}

	private int cumCountExclusive(Object child) {
		return cumCountInclusive(child) - getCount(child);
	}

	private int priority(int x) {
		if (labelXs[x] == null)
			return -1;
		else
			return labeled.priority(labelXs[x]);
	}

	// private int maxCount() {
	// int result = 0;
	// for (Iterator it = counts.values().iterator(); it.hasNext();) {
	// int i = ((Integer) it.next()).intValue();
	// if (i > result)
	// result = i;
	// }
	// return result;
	// }

	// private int totalCount() {
	// int result = 0;
	// for (Iterator it = counts.values().iterator(); it.hasNext();) {
	// int i = ((Integer) it.next()).intValue();
	// result += i;
	// }
	// return result;
	// }

	private double barWidthRatio() {
		return logicalWidth / totalCount;
	}

	// /**
	// * This depends on labelXs already being computed, so don't call this from
	// * computeBars
	// *
	// * @return Iterator over children whose bars would fall within the visible
	// * portion of the logicalWidth.
	// */
	// private Iterator visibleChildIterator() {
	// return labeled.getChildIterator(labelXs[0], labelXs[iVisibleWidth]);
	// }

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
		return labeled.cumCountChildIterator(minCount, maxCount);
	}

	private void drawLabels() {
		assert SwingUtilities.isEventDispatchThread();
//		Util.print("drawLabels " + totalCount + " " + iVisibleWidth + " "
//				+ logicalWidth + " " + labeled);
		assert totalCount > 0;

		labelXs = new Object[iVisibleWidth + 1];
		double divisor = barWidthRatio();
		for (Iterator it = slowVisibleChildIterator(); it.hasNext();) {
			Object child = it.next();
			// int totalCount = getCount(child);
			int iMaxX = maxBarPixel(child, divisor);
			int iMinX = minBarPixel(child, divisor);
			assert iMaxX >= 0 && iMinX <= iVisibleWidth : printBar(child,
					iMinX, iMaxX, "label");
			int iMidX = (iMaxX + iMinX) / 2;

			// Util.print(child + " " + cumCountExclusive(child) + " "
			// + getCount(child) + " >? " + priority(iMidX) + " " + iMinX
			// + "-" + iMaxX);

			if (getCount(child) > priority(iMidX))
				labelXs[iMidX] = child;
		}
//		Util.print(Util.valueOfDeep(labelXs));
		drawComputedLabel(null, divisor);
	}

	/**
	 * March along pixels, finding the child Objects to draw. At each pixel, you
	 * draw the child with the highest count at that pixel, which was computed
	 * above, unless another child with a higher count has a label that would
	 * occlude it, unless IT would be occluded. So you get a recusive test,
	 * where a conflict on the rightmost label can propagate all the way back to
	 * the left. At each call, you know there are no conflicts with
	 * leftCandidate from the left. You look for a conflict on the right (or
	 * failing that, the next non-conflict on the right) and recurse on that to
	 * get the next labeled Object to the right. You draw leftCandidate iff it
	 * doesn't conflict with that next label.
	 */
	private Object drawComputedLabel(Object leftCandidate, double divisor) {
		Object result = null;
		int x1 = -1;
		int x0 = -1;
		int threshold = -1;
		if (leftCandidate != null) {
			x0 = midLabelPixel(leftCandidate, divisor);
			threshold = getCount(leftCandidate);
			x1 = x0 + labelHprojectionW;
		}
		// Util.print("drawComputedLabel " + p + "." + leftCandidate + " " + x0
		// + "-" + iVisibleWidth + " " + threshold);
		for (int x = x0 + 1; x < iVisibleWidth && result == null; x++) {
			if (x > x1)
				threshold = -1;
			Object rightCandidate = labelXs[x];
			if (rightCandidate != null && getCount(rightCandidate) > threshold) {
				Object nextDrawn = drawComputedLabel(rightCandidate, divisor);
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

	private int maybeDrawLabel(Object v) {
		int nPixelsOccluded = 0;
		if (getCount(v) > 0) {
			double divisor = barWidthRatio();
			int midX = midLabelPixel(v, divisor);
			assert midX >= 0 && midX < iVisibleWidth : v + " " + midX + " "
					+ iVisibleWidth;

			// Util.print("maybeDrawLabel " + v + " " + midX + " "
			// + minBarPixel(v, barWidthRatio()) + "-"
			// + maxBarPixel(v, barWidthRatio()));

			if (labelXs[midX] == null || labelXs[midX] == v) {
				// assert verifyBarTables();

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
				labeled.drawLabel(v, minBarPixel(v, divisor), maxBarPixel(v,
						divisor));
			}
		}
		return nPixelsOccluded;
	}

	// private boolean areLabelsInited() {
	// return labels.getChildrenCount() > 0;
	// }

	// FacetText findLabel(Object facet) {
	// if (facet != null) {
	// for (int i = 0; i < labels.getChildrenCount(); i++) {
	// FacetText child = (FacetText) labels.getChild(i);
	// if (child.facet == facet)
	// return child;
	// }
	// }
	// return null;
	// }

	// private static final boolean VERIFY_BAR_TABLES = true;
	//
	// private boolean verifyBarTables() {
	// if (VERIFY_BAR_TABLES) {
	// Object prevFacet = labelXs[0];
	// assert prevFacet != null : printlabelXs();
	// boolean lastEmpty = false;
	// for (int i = 1; i < labelXs.length; i++) {
	// if (labelXs[i] == null)
	// lastEmpty = true;
	// else if (labelXs[i] == prevFacet)
	// // next x must have the next facet
	// lastEmpty = false;
	// else {
	// assert lastEmpty == false : i + printlabelXs();
	// // starting new bar
	// prevFacet = labelXs[i];
	// }
	// }
	// assert lastEmpty == false : printlabelXs();
	// }
	// return true;
	// }
	//
	// private String printlabelXs() {
	// Util.print(labeled + " has " + "?" + " bars over " + labelXs.length
	// + " pixels.");
	// Util.printDeep(labelXs);
	// Object prevFacet = labelXs[0];
	// int facetStart = 0;
	// for (int i = 1; i < labelXs.length; i++) {
	// Object facet = labelXs[i];
	// if (facet != null) {
	// if (facet == prevFacet)
	// i++;
	// else if (facetStart != i - 1)
	// Util.print("\nunterminated:");
	// Util.print(prevFacet + ": " + getCount(facet) + " "
	// + cumCountExclusive(facet) + "-"
	// + cumCountInclusive(facet) + " [" + facetStart + ", "
	// + (i - 1) + "]");
	// prevFacet = facet;
	// facetStart = i;
	// }
	// }
	// return "";
	// }

	// private int maxBarPixel(Object child) {
	// return maxBarPixel(child, barWidthRatio());
	// }

	/**
	 * @param child
	 * @param divisor
	 * @return x offset relative to leftEdge. return w-1 if offset would be off
	 *         the screen.
	 */
	private int maxBarPixel(Object child, double divisor) {
		// Util.print("maxBarPixel " + child + " " + (child.cumCount() *
		// divisor)
		// + " " + leftEdge);

		double dPixel = cumCountInclusive(child) * divisor - leftEdge;
		// assert dPixel >= 0 : child + " " + dPixel + " "
		// + child.cumCountExclusive() + "-" + child.cumCountInclusive();
		return (int) Math.min(visibleWidth(), dPixel);

		// maxX = Math.min(rightEdge, maxX) - leftEdge;
		// return (int) maxX;
	}

	// private int minBarPixelRaw(Object child) {
	// return (int) (cumCountExclusive(child) * barWidthRatio() - leftEdge);
	// }
	//
	// private int maxBarPixelRaw(Object child) {
	// return (int) (cumCountInclusive(child) * barWidthRatio() - leftEdge);
	// }

	/**
	 * @param child
	 * @param divisor
	 * @return x offset relative to left edge. return 0 if offset would be
	 *         negative.
	 */
	private int minBarPixel(Object child, double divisor) {
		// If bar should start a fraction after w-1, we shouldn't draw it, so
		// always round up.
		double dPixel = cumCountExclusive(child) * divisor - leftEdge;
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
	private int midLabelPixel(Object child, double divisor) {
		return (minBarPixel(child, divisor) + maxBarPixel(child, divisor)) / 2;
	}

	// private boolean isVisible(Object child) {
	// double divisor = barWidthRatio();
	// // 0.5's round the result to the nearest integer
	// double minPixel = (cumCountExclusive(child) + 0.5) * divisor - leftEdge;
	// double maxPixel = (cumCountInclusive(child) - 0.5) * divisor - leftEdge;
	// return minPixel < visibleWidth() && maxPixel > 0;
	// }

	private String printBar(Object child, int iMinX, int iMaxX, String what) {
		Util.print("Adding " + what + " for " + child + ": totalCount="
				+ getCount(child) + " range=" + cumCountExclusive(child) + "-"
				+ cumCountInclusive(child) + " iMinX-iMaxX=" + iMinX + "-"
				+ iMaxX + " left/logical=" + leftEdge + "/" + logicalWidth);
		return "";
	}

}
