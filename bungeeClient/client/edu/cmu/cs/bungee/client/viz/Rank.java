/*
 * 
 * Created on Mar 11, 2005
 * 
 * Bungee View lets you search, browse, and data-mine an image
 * collection. Copyright (C) 2006 Mark Derthick
 * 
 * This program is free software; you can redistribute it and/or modify it under
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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.viz.Summary.RankComponentHeights;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.umd.cs.piccolo.PNode;

final class Rank extends LazyContainer {

	/**
	 * Margin between multiple PVs in a rank
	 */
	private static final double PV_MARGIN = 5.0;

	// private double queryW;

	/**
	 * Margin before the rank label
	 */
	static final double LABEL_LEFT_MARGIN = 2;

	/**
	 * Margin between the rank label and the bars or, if connected, the 100%
	 * label
	 */
	static final double LABEL_RIGHT_MARGIN = 10;

	Rank parentRank;

	private Summary summary;

	// double w = 0.0;

	PerspectiveViz[] perspectives = {};

	boolean isConnected = false;

	// final static private SelectionHandler selectionHandler = new
	// SelectionHandler();

	Rank(Summary _summary, Rank _parent) {
		parentRank = _parent;
		summary = _summary;
		// addInputEventListener(new RankHandler());
	}

	void validate(double _w) {
		// Util.print("r.validate " + _w + " " + _queryW);
		w = _w;
		// queryW = _queryW;
		// This happens in layoutChildren;
		// for (int i = 0; i < children.length; i++) {
		// children[i].validate(w);
		// }
		// computeHeights();
		validateInternal();
	}

	void delayedInit() {
		for (int i = 0; i < perspectives.length; i++)
			perspectives[i].delayedInit();
	}

	void validateInternal() {
		int nPerspectives = perspectives.length;
		if (w > 0 && nPerspectives > 0) {
			// Util.print("Rank.validateInternal " + this + " " + (w-
			// summary.queryW) + " " +
			// perspectives.length + " " + totalChildTotalCount());

			double availableW = w - summary.queryW;
			// Don't let margins take up more than half the width
			int margin = (int) Math.min(availableW / 2 / (nPerspectives - 1),
					PV_MARGIN);
			double barW = availableW - margin * (nPerspectives - 1);
			assert barW > 0;
			double ratio = barW / totalChildTotalCount();
			double xOffset = summary.queryW;
			for (int i = 0; i < nPerspectives; i++) {
				PerspectiveViz pv = perspectives[i];
				if (pv.p.isDisplayed()) {
					int childW = (int) Math.round(ratio * pv.p.getTotalCount());
					// assert childW > 0 : pv.p + " " + pv.p.getTotalCount();
					if (childW <= 0)
						Util.print("WARNING: zero width for " + pv);
					pv.setHeight(getHeight());
					pv.setOffset(xOffset, 0.0);
					pv.validate(Math.max(1, childW), i == 0);
					xOffset += childW + margin;
				} else {
					summary.synchronizeWithQuery();
				}
			}
			TextNfacets label = perspectives[0].rankLabel;
			// if (label.getWidth() != rankLabelWdith()) {
			perspectives[0].layoutRankLabel(false);
			label.setOffset(LABEL_LEFT_MARGIN - summary.queryW, 0);
		}
	}

	/**
	 * Add PerspectiveVizs as the pickFacetTextNotifier of all rankLabel
	 * FacetTexts with an actual facet.
	 */
	void hackRankLabelNotifier() {
//		Util.print("hackRankLabelNotifier " + this);
		TextNfacets rankLabel = perspectives[0].rankLabel;
		for (int i = 0; i < rankLabel.getChildrenCount(); i++) {
			FacetText ft = (FacetText) rankLabel.getChild(i);
			Perspective ftFacet = ft.getFacet();
			for (int j = 0; j < perspectives.length; j++) {
				PerspectiveViz pv = perspectives[j];
//				Util.print("vvv " + ftFacet+" " + pv.p);
				if (pv.p == ftFacet) {
					ft.pickFacetTextNotifier = pv;
					// art().validatePerspectiveList();
					// break;
				}
			}
		}
	}

	void setFeatures() {
		for (int i = 0; i < perspectives.length; i++) {
			perspectives[i].setFeatures();
		}
	}

	// void redrawLabels() {
	// for (int i = 0; i < perspectives.length; i++) {
	// PerspectiveViz pv = perspectives[i];
	// pv.drawLabels();
	// }
	// }

	void restrict() {
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz pv = perspectives[i];
			pv.restrict();
			totalChildTotalCount = -1;
		}
	}

	double rankLabelWdith() {
		return summary.queryW - LABEL_LEFT_MARGIN - LABEL_RIGHT_MARGIN
				- (isConnected ? perspectives[0].percentLabelW() : 0);
	}

	int nBars() {
		int result = 0;
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz pv = perspectives[i];
			result += pv.nBars();
		}
		return result;
	}

	double[] pValues() {
		double[] result = null;
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz pv = perspectives[i];
			result = Util.append(result, pv.pValues());
		}
		assert result != null : this + " " + perspectives.length;
		return result;
	}

	void updateData() {
		// Util.print("rank.updateData: " + this + " " +
		// perspectives[0].query().isQueryValid());
		// decacheTotalChildOnCount();
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.updateData();
		}
		// perspectives[0].redraw100PercentLabel();
		perspectives[0].layoutRankLabel(true); // true in case colors have
		// changed
	}

	void animateData(double zeroToOne) {
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.animateData(zeroToOne);
		}
	}

	void setBarTransparencies(float zeroToOne) {
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.setBarTransparencies(zeroToOne);
		}
	}

	// void componentHeightsChanged() {
	// computeHeights();
	// for (int i = 0; i < perspectives.length; i++) {
	// perspectives[i].invalidateLayout();
	// }
	// }

	// private double prevH = -1;
	//
	// double frontH;
	//
	// double texth;
	//
	// double foldH;

	RankComponentHeights componentHeights = new RankComponentHeights(0, 0, 0, 0);

	double frontH() {
		return componentHeights.frontH();
	}

	double foldH() {
		return componentHeights.foldH();
	}

	double labelsH() {
		return componentHeights.labelsH();
	}

	// // perspective heights should follow our height as we're animated
	// public void layoutChildren() {
	// double h = getHeight();
	// Util.print("Rank.layoutChildren " + this + " " + h);
	// componentHeights = computeHeights(getHeight());
	// for (int i = 0; i < perspectives.length; i++) {
	// perspectives[i].setHeight(h);
	// }
	// }

	RankComponentHeights computeHeights(double h1) {
		double frontH, foldH, texth;
		if (h1 > summary.selectedFrontH()) {
			frontH = summary.selectedFrontH();
			double extra = h1 - frontH;
			if (summary.selectedLabelH() == 0)
				texth = 0;
			else {
				double ratio = summary.selectedFoldH()
						/ summary.selectedLabelH();
				texth = Math.round(extra / (1.0 + ratio));
			}
			foldH = Math.round(h1 - frontH - texth);
		} else {
			frontH = Math.round(h1);
			texth = 0.0;
			foldH = 0.0;
		}
		// Util.print("Rank.computeHeights fold, front, text = " + this + " " +
		// h + " " + foldH + " "
		// + frontH + " " + texth);
		return new RankComponentHeights(foldH, frontH, texth, 0);
	}

	void setConnected(boolean connected) {
		// Util.print("setConnected " + getName() + " " + connected);
		assert connected != isConnected;
		isConnected = connected;
		updateNameLabels();
		for (int i = 0; i < perspectives.length; i++) {
			perspectives[i].setConnected(connected);
		}
	}

	void connect() {
		summary.flagConnectedRank(this);
		summary.layoutChildrenWhenNeeded();
	}

	// boolean click() {
	// art().printUserAction(Bungee.RANK_LABEL, perspectives[0].p, 0);
	// connect();
	// return true;
	// }

	int totalChildTotalCount() {
		if (totalChildTotalCount < 0) {
			totalChildTotalCount = 0;
			for (int i = 0; i < perspectives.length; i++) {
				PerspectiveViz p = perspectives[i];
				totalChildTotalCount += p.p.getTotalCount();
			}
			// Util.print("Rank.totalChildTotalCount " + getName() + " " +
			// totalCount);
		}
		return totalChildTotalCount;
	}

	private int totalChildOnCount = -1;

	private int totalChildTotalCount = -1;

	// private double warpPower = -1.0;
	//
	// private static final double SIGMOID_STEEPNESS = 10.0;
	//
	// private static final double SIGMOID_MIN = 1.0 / (1.0 + Math.pow(Math.E,
	// SIGMOID_STEEPNESS / 2.0));
	//
	// private static final double SIGMOID_SCALE = 1.0 - 2.0 * SIGMOID_MIN;

	// private double maxChildPercentOn = -1;

	/**
	 * Used to determine whether totalChildOnCount is up to date
	 */
	private int updateIndex = 0;

	int totalChildOnCount() {
		assert query().isQueryValid();
		if (updateIndex != query().updateIndex) {
			totalChildOnCount = 0;
			for (int i = 0; i < perspectives.length; i++) {
				PerspectiveViz p = perspectives[i];
				int childOnCount = p.p.getOnCount();
				if (childOnCount < 0) {
					totalChildOnCount = childOnCount;
					return totalChildOnCount;
				}
				totalChildOnCount += childOnCount;
			}
			// Util.print("Rank.totalChildOnCount " + getName() + " = " +
			// totalChildOnCount);
		}
		return totalChildOnCount;
	}

	// double maxChildPercentOn() {
	// if (maxChildPercentOn < 0) {
	// maxChildPercentOn = 0;
	// for (int i = 0; i < perspectives.length; i++) {
	// PerspectiveViz p = perspectives[i];
	// double childPercentOn = p.maxChildPercentOn();
	// if (childPercentOn < 0) {
	// maxChildPercentOn = childPercentOn;
	// return maxChildPercentOn;
	// } else if (childPercentOn > maxChildPercentOn)
	// maxChildPercentOn = childPercentOn;
	// }
	// // Util.print("Rank.totalChildOnCount " + getName() + " = " +
	// // totalChildOnCount);
	// }
	// return maxChildPercentOn;
	// }

	void decacheTotalChildOnCount() {
		// Util.print("Rank.decacheTotalChildOnCount " + this);
		updateIndex = 0;
		// totalChildOnCount = -1;
		// maxChildPercentOn = -1;
		// warpPower = -1.0;
	}

	// static final int WARP_PERCENT = 0;
	// static final int WARP_CORRELATION = 1;
	// static final int WARP_MUT_INF = 2;
	// static final int WARP_TYPE = WARP_CORRELATION;
	//
	// double warp(Perspective facet) {
	// // if ("MPAA Rating".equals(getName())) {
	// // printTable(facet);
	// // }
	// switch (WARP_TYPE) {
	// case WARP_PERCENT:
	// return warpPercent(facet.percentOn());
	// case WARP_CORRELATION:
	// return warpLogOddsRatio(facet);
	// case WARP_MUT_INF:
	// return warpMutInf(facet, false);
	// }
	// return -1;
	// }

	void printTable(Perspective facet) {
		int a = facet.getOnCount();
		int row0 = facet.getTotalCount();
		int col0 = totalChildOnCount();
		int itotal = totalChildTotalCount();
		int col1 = itotal - col0;
		int row1 = itotal - row0;
		int b = row0 - a;
		int c = col0 - a;
		int d = col1 - b;
		Util.print("\nwarp " + facet);
		DecimalFormat f = new DecimalFormat(" 000,000");
		DecimalFormat f2 = new DecimalFormat(" 0.000");
		Util.print(f.format(a) + f.format(b) + f.format(row0));
		Util.print(f.format(c) + f.format(d) + f.format(row1));
		Util.print(f.format(col0) + f.format(col1) + f.format(itotal));
		Util.print(
		// f2.format(warpPercent(facet.percentOn()))
				// + f2.format(warpCorrelation(facet))
				// + f2.format(warpMutInf(facet, false))
				// + f2.format(warpMutInf(facet, true))
				f2.format(warp(oddsRatio(facet))));
	}

	// /**
	// * mutual information
	// *
	// * SUMcell in {a,b,c,d} [cell/total * log((cell/total) / ((row/total) *
	// * (col/total))] = cell/total * log(total*cell/(row*col)) = cell/total *
	// * (log(total) + log(cell/(row*col))) = 1/total * (a*log(total*a/(row0
	// *
	// * @see "http://en.wikipedia.org/wiki/Mutual_information"
	// * @param facet
	// * @return
	// */
	// double warpMutInf(Perspective facet, boolean isMin) {
	// double result = 0.5;
	// int a = facet.getOnCount();
	// int row0 = facet.getTotalCount();
	// int col0 = totalChildOnCount();
	// int itotal = totalChildTotalCount();
	// int col1 = itotal - col0;
	// int row1 = itotal - row0;
	// // Util.print("wapr " + facet + " " + row0 + " " + col0 + " " + col1 + "
	// // " + row1);
	// if (row0 > 0 && row1 > 0 && col0 > 0 && col1 > 0) {
	// double total = itotal;
	// int b = row0 - a;
	// int c = col0 - a;
	// int d = col1 - b;
	// double entropy = 0;
	// entropy -= col0 / total * Math.log(col0 / total);
	// entropy -= col1 / total * Math.log(col1 / total);
	//
	// if (isMin) {
	// double entropy2 = 0;
	// entropy2 -= row0 / total * Math.log(row0 / total);
	// entropy2 -= row1 / total * Math.log(row1 / total);
	// if (entropy2 < entropy)
	// entropy = entropy2;
	// }
	//
	// double mutInf = 0.0;
	// if (a > 0)
	// mutInf += a * Math.log(total * a / row0 / col0);
	// // Util.print(result);
	// if (b > 0)
	// mutInf += b * Math.log(total * b / row0 / col1);
	// // Util.print(result);
	// if (c > 0)
	// mutInf += c * Math.log(total * c / row1 / col0);
	// // Util.print(result);
	// if (d > 0)
	// mutInf += d * Math.log(total * d / row1 / col1);
	// mutInf /= total;
	// // if (mutInf > 0.1)
	// // Util.print(facet + " " + mutInf + " " + entropy);
	// // Util.print(a + " " + b + " " + c + " " + d + "\n");
	// assert mutInf >= 0 && mutInf <= 1 : a + " " + b + " " + c + " " + d
	// + " " + facet + " " + mutInf;
	// result = mutInf / entropy;
	// if (facet.percentOn() > expectedPercentOn())
	// result = 0.5 + result / 2;
	// else
	// result = 0.5 - result / 2;
	// }
	// return result;
	// }
	//
	// /**
	// * For 2x2 contingency table abcd, return correlation row ab is facet
	// count
	// * col ac is onCount
	// *
	// * ad - bc / sqrt((a+b)(a+c)(b+d)(c+d))
	// *
	// * scaled to the interval [0-1]
	// *
	// * @see "http://mpra.ub.uni-muenchen.de/2662/01/MPRA_paper_2662.pdf"
	// *
	// * @return
	// */
	// double warpCorrelation(Perspective facet) {
	// double result = 0.5;
	// int a = facet.getOnCount();
	// int aPlusb = facet.getTotalCount();
	// int aPlusc = totalChildOnCount();
	// int aPlusbcd = totalChildTotalCount();
	// int bPlusd = aPlusbcd - aPlusc;
	// int cPlusd = aPlusbcd - aPlusb;
	// double denom = ((double) aPlusb) * aPlusc * bPlusd * cPlusd;
	// if (denom > 0) {
	// int b = aPlusb - a;
	// int c = aPlusc - a;
	// int d = bPlusd - b;
	// double correlation = (a * d - b * c) / Math.sqrt(denom);
	// result = (correlation + 1.0) / 2.0;
	// // Util.print(aPlusb + " " + aPlusc + " " + bPlusd + " " + cPlusd);
	// // Util.print(a + " " + b + " " + c + " " + d + "\n");
	// assert result >= 0 && result <= 1 : a + " " + b + " " + c + " " + d
	// + " " + facet + " " + correlation + " " + result + " "
	// + denom;
	// }
	// return result;
	// }

	static final double LOG_ODDS_RANGE = Math.log(100);

	// facet ~facet
	// _ a ____ b __ query
	// _ c ____ d _ ~query
	double oddsRatio(Perspective facet) {
		if (!query().isQueryValid())
			return 1;
		int a = facet.getOnCount();
		int aPlusc = facet.getTotalCount();
		int aPlusb = totalChildOnCount();
		int b = aPlusb - a;
		int aPlusbcd = totalChildTotalCount();
		int bPlusd = aPlusbcd - aPlusc;
		if (!(a >= 0 && b >= 0 && bPlusd >= 0 && aPlusc >= 0))
			assert a >= 0 && b >= 0 && bPlusd >= 0 && aPlusc >= 0 : facet + " "
					+ a + " " + b + " " + bPlusd + " " + aPlusc;

		// Util.print(facet + " " + a + " " + b + " " + aPlusc + " " + bPlusd);
		double result;
		if (aPlusc == 0 || bPlusd == 0 || aPlusb == 0)
			// make sure 0/0 == 1
			result = 1;
		else if (b == 0)
			result = Double.POSITIVE_INFINITY;
		else
			// cast to double to avoid overflow
			result = (a * (double) bPlusd) / (b * (double) aPlusc);
		assert result >= 0 : facet + " " + a + " " + b + " " + bPlusd + " "
				+ aPlusc;
		return result;
	}

	// double warp(Perspective facet) {
	// double result = 0.5;
	// int a = facet.getOnCount();
	// int aPlusb = facet.getTotalCount();
	// int aPlusc = totalChildOnCount();
	// int aPlusbcd = totalChildTotalCount();
	// int bPlusd = aPlusbcd - aPlusc;
	// int cPlusd = aPlusbcd - aPlusb;
	// // double denom = ((double) aPlusb) * aPlusc * bPlusd * cPlusd;
	// if (aPlusb > 0 && aPlusc > 0 && bPlusd > 0 && cPlusd > 0) {
	// int b = aPlusb - a;
	// int c = aPlusc - a;
	// int bc = b * c;
	// if (bc == 0) {
	// result = 1.0;
	// } else {
	// int d = bPlusd - b;
	// double logOdds = Math.log(a * d / (double) bc);
	// // Util.print((a*d/(double) b/c) + " " + logOdds);
	// result = warp(logOdds);
	// // Util.print(aPlusb + " " + aPlusc + " " + bPlusd + " " +
	// // cPlusd);
	// // Util.print(a + " " + b + " " + c + " " + d + "\n");
	// assert result >= 0 && result <= 1 : a + " " + b + " " + c + " "
	// + d + " " + facet + " " + logOdds + " " + result;
	// }
	// }
	// return result;
	// }

	/**
	 * @param oddsRatio
	 *            ranges from 0 to +infinity
	 * @return ranges from 0 to 1
	 */
	static double warp(double oddsRatio) {
		// Util.print("warp " + oddsRatio + " " + ((int) Math.round(oddsRatio))
		// + " " + Util
		// .constrain(Math.log(oddsRatio), -logOddsRange, logOddsRange));
		return (constrainLogOdds(oddsRatio) + LOG_ODDS_RANGE)
				/ (2 * LOG_ODDS_RANGE);
	}

	static double constrainLogOdds(double oddsRatio) {
		return Util.constrain(Math.log(oddsRatio), -LOG_ODDS_RANGE,
				LOG_ODDS_RANGE);
	}

	static double constrainOddsRatio(double oddsRatio) {
		return Math.exp(constrainLogOdds(oddsRatio));
	}

	// double warpPower() {
	// double percent = expectedPercentOn();
	//
	// if (warpPower < 0) {
	// if (percent > 0.0 && percent < 1.0) {
	// warpPower = -Math.log(2) / Math.log(percent);
	// } else {
	// warpPower = 1.0;
	// }
	// }
	// assert warpPower > 0.0 : expectedPercentOn() + " " + warpPower;
	// return warpPower;
	// }
	//
	// double warpPercent(double percent) {
	// if (percent == 0.0 || percent == 1.0)
	// return percent;
	//
	// double powWarped = Math.pow(percent, warpPower());
	// double result = 1.0 / (1.0 + Math.pow(Math.E, SIGMOID_STEEPNESS
	// * (0.5 - powWarped)));
	// result = (result - SIGMOID_MIN) / SIGMOID_SCALE;
	// // if (getName().equals("Genre"))
	// // Util.print(warpPower + " " + expectedPercentOn() + " percent="
	// // + percent + " " + powWarped + " " + result);
	// return result;
	// }
	//
	// double unwarp(double y) {
	// if (y == 0.0 || y == 1.0)
	// return y;
	// y = y * SIGMOID_SCALE + SIGMOID_MIN;
	// double result = Math.pow(0.5 - Math.log(1.0 / y - 1.0)
	// / SIGMOID_STEEPNESS, 1.0 / warpPower());
	// // if (getName().equals("Genre"))
	// // Util.print("unwarp " + warpPower + " y=" + x + " => " + y + " "
	// // + result + " inv=" + warp(result));
	//
	// return result;
	// }

	/**
	 * @param y
	 *            1-y-coordinate, so most positive association is 0
	 * @return the odds ratio for this y value
	 */
	static double unwarp(double y) {
		double logOdds = (y - 0.5) * 2 * LOG_ODDS_RANGE;
		double odds = Math.exp(logOdds);
		return odds;
	}

	int maxCount() {
		int totalCount = 0;
		for (int i = 0; i < perspectives.length; i++) {
			Perspective p = perspectives[i].p;
			if (p.isPrefetched()) {
				int count = p.maxChildTotalCount();
				if (count > totalCount)
					totalCount = count;
			}
		}
		return totalCount;
	}

	double expectedPercentOn() {
		// Util.print("rank.expectedPercentOn " + getName() + " " +
		// totalChildOnCount() + " / "
		// + totalChildTotalCount());
		assert totalChildTotalCount() > 0 : getName() + " "
				+ totalChildTotalCount() + " " + Util.valueOfDeep(perspectives);
		return totalChildOnCount() / (double) totalChildTotalCount();
		// return maxChildPercentOn();
	}

	// double hingeH() {
	// double expectedPercentOn = expectedPercentOn();
	// double result = expectedPercentOn == 0.0 ? Double.MAX_VALUE
	// : (summary.selectedFrontH / expectedPercentOn - summary.selectedFrontH)
	// / summary.selectedFoldH;
	// // Util.print("rank.hingeH " + this + " " + result);
	// return result;
	// }

	private String cachedName;

	void decacheName() {
		cachedName = null;
		totalChildTotalCount = -1;
		decacheTotalChildOnCount();
		validateInternal();
	}

	String getName() {
		if (cachedName == null) {
			// Util.print("Rank.getName "
			// + Util.valueOfDeep(perspectives));
			int nPerspectives = perspectives.length;
			String[] names = new String[nPerspectives];
			int index = 0;
			for (int i = 0; i < nPerspectives; i++) {
				names[index++] = perspectives[i].getName();
			}
			cachedName = Util.arrayToEnglish(names, " and ");
			if (parentRank != null)
				cachedName = perspectives[0].p.namePrefix() + cachedName;
		}
		return cachedName;
	}

	SortedSet getRestrictions(boolean require) {
		SortedSet restrictions = new TreeSet();
		for (int i = 0; i < perspectives.length; i++) {
			restrictions.addAll(perspectives[i].p.getRestrictionFacetInfos(
					true, require));
		}
		// Util.print(this + ".getRestrictions(" + require + ") return "
		// + Util.valueOfDeep(restrictions));
		return restrictions;
	}

	SortedSet getPerspectives() {
		SortedSet restrictions = new TreeSet();
		for (int i = 0; i < perspectives.length; i++) {
			restrictions.add(perspectives[i].p);
		}
		return restrictions;
	}

	// Update name and restrictionName labels
	void updatePerspectiveSelections(Set facets) {
		// Util.print("Rank.updatePerspectiveSelections " + parentRank + " "
		// + perspectives[0].p);
		perspectives[0].rankLabel.updateSelections(facets);

		// Update bars
		for (int i = 0; i < perspectives.length; i++)
			perspectives[i].updateSelection(facets);
	}

	void updateNameLabels() {
		Markup content = Query.facetSetDescription(getPerspectives());
		if (art().getShowTagLists() || !isConnected())
			content.add(0, Markup.UNDERLINE_TAG);
		else {
			int i = content.indexOf(Markup.NEWLINE_TAG);
			if (i >= 0)
				content.add(i, Markup.UNDERLINE_TAG);
		}
		// Util.print("Rank.updateNameLabels " + this + " " + content);

		TextNfacets label = perspectives[0].rankLabel;
		label.setContent(content);
		perspectives[0].layoutRankLabel(true);
	}

	boolean isConnected() {
		return isConnected;
	}

	boolean usesPerspective(PerspectiveViz pv) {
		for (int i = 0; i < perspectives.length; i++) {
			if (pv == perspectives[i])
				return true;
		}
		return false;
	}

	void addPerspective(PerspectiveViz pv) {
		// Util.print("Rank.addPerspective " + pv.getName());
		pv.rank = this;
		if (pv.parentPV != null && perspectives.length == 0) {
			setOffset(0.0, parentRank.getYOffset()
					+ pv.parentPV.frontBottomOffset());
		}

		PerspectiveViz[] a = new PerspectiveViz[perspectives.length + 1];
		int index = 0;
		while (index < perspectives.length
				&& perspectives[index].p.whichChild() < pv.p.whichChild()) {
			a[index] = perspectives[index];
			index++;
		}
		a[index] = pv;
		index++;
		while (index < a.length) {
			a[index] = perspectives[index - 1];
			index++;
		}
		perspectives = a;

		addChild(pv);
		decacheName();
	}

	boolean removePerspectives(PerspectiveViz[] pvs) {
		if (pvs != null && pvs.length > 0) {
			// Util.print("removePerspectives " + getName());
			perspectives = (PerspectiveViz[]) Util.setDifference(perspectives,
					pvs, PerspectiveViz.class);
			if (perspectives.length > 0) {
				for (int i = 0; i < pvs.length; i++) {
					PNode lightBeam = pvs[i].lightBeam;
					if (lightBeam != null)
						removeChild(lightBeam);
					removeChild(pvs[i]);
				}
				decacheName();
			}
		}
		return perspectives.length == 0;
	}

	// void highlight(boolean state) {
	// Util.print("Rank.highlight " + this + " " + state);
	// if (perspectives.length == 1)
	// // This will paint more efficiently
	// art().highlightFacet(perspectives[0].p, state,
	// isConnected() ? -1 : -2);
	// else {
	// Markup desc = null;
	// if (state && !isConnected()) {
	// desc = Query.emptyMarkup();
	// desc.add("open category ");
	// Query.toEnglish(getPerspectives(), " and ", desc);
	// }
	// art().setClickDesc(desc);
	// }
	// }

	private Bungee art() {
		return summary.art;
	}

	private Query query() {
		return summary.art.query;
	}

	// void mayHideTransients() {
	// summary.mayHideTransients();
	// }

	private double startOffset;

	private double goalOffset;

	private RankComponentHeights startHeights;

	private RankComponentHeights goalHeights;

	void prepareAnimation(double offset, double desiredH) {
		// Util.print("prepareAnimation " + this + " " + getHeight() + " "
		// + desiredH);

		// height
		startHeights = componentHeights;
		goalHeights = computeHeights(desiredH);
		if (parentRank == null && startHeights.frontH() == 0) {
			// This is the initial draw
			componentHeights = goalHeights;
			startHeights = goalHeights;
			setHeight(desiredH);
		}

		// offset
		startOffset = getYOffset();
		if (startOffset == 0) {
			// we've just been created.
			if (parentRank == null)
				startOffset = offset;
			else
				startOffset = parentRank.getYOffset() + parentRank.getHeight();
		}
		goalOffset = offset;

		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.prepareAnimation();
		}
	}

	void animateRank(float zeroToOne) {
		double y = Math.round(lerp(zeroToOne, startOffset, goalOffset));
		setOffset(0, y);

		if (!goalHeights.equals(startHeights)) {
			componentHeights = RankComponentHeights.lerp(zeroToOne,
					startHeights, goalHeights);
			double newH = componentHeights.foldH() + componentHeights.frontH()
					+ componentHeights.labelsH();
			// Util.print("animateRank " + getName() + " " + y + " " + newH);
			boolean heightChanged = setHeight(newH);
			for (int i = 0; i < perspectives.length; i++) {
				if (heightChanged) {
					perspectives[i].setHeight(newH);
				} else {
					// If fold, front, or labels heights have changed
					// without changing overall selectedH, piccolo doesn't know
					// to call layoutChildren
					perspectives[i].invalidateLayout();
				}
			}
		}

		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.animate(zeroToOne);
		}
	}

	public String toString() {
		return "<Rank " + Arrays.asList(perspectives) + ">";
	}

	boolean keyPress(char key) {
		return perspectives[0].keyPress(key);
	}
}

// final class RankHandler extends MyInputEventHandler {
//
// RankHandler() {
// super(Rank.class);
// }
//
// public boolean click(PNode node) {
// return ((Rank) node).click();
// }
//
// public void mayHideTransients(PNode node) {
// ((Rank) node).mayHideTransients();
// }
//
// public boolean enter(PNode node) {
// // Util.print("Rank.SelectionHandler.enter");
// ((Rank) node).highlight(true);
// return true;
// }
//
// public boolean exit(PNode node) {
// ((Rank) node).highlight(false);
// return true;
// }
// }
