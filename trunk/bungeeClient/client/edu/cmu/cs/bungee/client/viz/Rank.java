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



import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PrintArray;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;

class Rank extends LazyPNode {

	private static final double margin = 5.0;

	private double queryW;

	private static final double labelMargin = 3.0;

	Rank parentRank;

	private Summary summary;

	double w = 0.0;

	 PerspectiveViz[] perspectives = {};

	 boolean isConnected = false;

//	final static private SelectionHandler selectionHandler = new SelectionHandler();

	Rank(Summary _summary, Rank _parent) {
		parentRank = _parent;
		summary = _summary;
		addInputEventListener(new SelectionHandler());
	}

	void validate(double _w, double _queryW) {
		// Util.print("r.validate " + _w + " " + _queryW);
		w = _w - 5.0; // Leave room for rightmost rotated label
		queryW = _queryW;
		// This happens in layoutChildren;
		// for (int i = 0; i < children.length; i++) {
		// children[i].validate(w);
		// }
		redraw();
	}

	void delayedInit() {
		for (int i = 0; i < perspectives.length; i++)
			perspectives[i].delayedInit();
	}

	void redraw() {
		// Util.print("r.redraw " + getName() + " " + w + " "
		// + perspectives.length);
		int nPerspectives = perspectives.length;
		if (w > 0 && nPerspectives > 0) {
			double barW = w - queryW - margin * (nPerspectives - 1);
			double ratio = barW / totalChildTotalCount();
			double xOffset = queryW;
			for (int i = 0; i < nPerspectives; i++) {
				PerspectiveViz pv = perspectives[i];
				if (pv.p.isUsed()) {
					int childW = (int) (ratio * pv.p.getTotalCount());
					pv.setHeight(getHeight());
					pv.setOffset(xOffset, 0.0);
					pv.validate(childW, i == 0);
					xOffset += childW + margin;
				} else {
					summary.synchronizeWithQuery();
				}
			}
			TextNfacets label = perspectives[0].rankLabel;
			if (label.getWidth() != rankLabelWdith()) {
				perspectives[0].layoutRankLabel(false);
			}
			label.setOffset(labelMargin - queryW, 0);
		}
	}

	void redrawLabels() {
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz pv = perspectives[i];
			pv.redrawLabels();
		}
	}

	public void restrict() {
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz pv = perspectives[i];
			pv.restrict();
		}
	}

	double rankLabelWdith() {
		return queryW - 2.0 * labelMargin;
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
		return result;
	}

	void updateData() {
		decacheTotalChildOnCount();
		// double curtainH = 0;
		// if (totalChildOnCount() > 0) {
		// double hingeH = hingeH();
		// if (hingeH > 0.0) {
		// curtainH = Bar.curtainHeight(perspectives[0], hingeH);
		// } else {
		// curtainH = 1;
		// }
		// }
		// Util.print("rank.updateData: " + this + " " +
		// perspectives[0].p.getQuery().isQueryValid());
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			p.updateData();
		}
		perspectives[0].redraw100PercentLabel();
		// perspectives[0].rankLabel.layoutBestFit();
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

	private double prevH = -1;

	double frontH;

	double texth;

	double foldH;

	public void layoutChildren() {
		if (w > 0) { // Make sure we've been initialized.
			double h = getHeight();
			if (Math.abs(h - prevH) > 1.0) {
				prevH = h;
				computeHeights();
				for (int i = 0; i < perspectives.length; i++) {
					// Don't animate the perspectives, as we are getting
					// animated outselves.
					perspectives[i].setHeight(h);
				}
			}
			// Util.print("rank.layoutChildren return " + getName() + " "
			// + new Date().getTime());
		}
	}

	void computeHeights() {
		double h = getHeight();
		if (h > summary.selectedFrontH()) {
			frontH = summary.selectedFrontH();
			double extra = h - frontH;
			if (summary.selectedLabelH() == 0)
				texth = 0;
			else {
				double ratio = summary.selectedFoldH() / summary.selectedLabelH();
				texth = Math.round(extra / (1.0 + ratio));
			}
			foldH = Math.round(h - frontH - texth);
		} else {
			frontH = Math.round(h);
			texth = 0.0;
			foldH = 0.0;
		}
//		Util.print("Rank.computeHeights fold, front, text = " + foldH + " "
//				+ frontH + " " + texth);
	}

	void setConnected(boolean connected) {
		// Util.print("setConnected " + getName() + " " + connected);
		art().ensurePerspectiveList(false);
		isConnected = connected;
		for (int i = 0; i < perspectives.length; i++) {
			perspectives[i].setConnected(connected);
		}
	}

	void connect() {
		summary.connectToRank(this);
		summary.layoutChildrenWhenNeeded();
	}

	boolean click() {
		art().printUserAction(Bungee.RANK_LABEL, perspectives[0].p, 0);
		connect();
		return true;
	}

	int totalChildTotalCount() {
		int totalCount = 0;
		for (int i = 0; i < perspectives.length; i++) {
			PerspectiveViz p = perspectives[i];
			totalCount += p.p.getTotalCount();
		}
		// Util.print("Rank.totalChildTotalCount " + getName() + " " +
		// totalCount);
		return totalCount;
	}

	private int totalChildOnCount = -1;

	private double warpPower = -1.0;

	private static final double sigmoidSteepness = 1.0;

	private static final double sigmoidMin = 1.0 / (1.0 + Math.pow(Math.E,
			sigmoidSteepness / 2.0));

	private static final double sigmoidScale = 1.0 - 2.0 * sigmoidMin;

	// private double maxChildPercentOn = -1;

	int totalChildOnCount() {
		if (totalChildOnCount < 0) {
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
		// Util.print("Rank.decacheTotalChildOnCount " + getName());
		totalChildOnCount = -1;
		// maxChildPercentOn = -1;
		warpPower = -1.0;
	}

	double warpPower() {
		double percent = expectedPercentOn();
		if (warpPower < 0) {
			if (percent > 0.0 && percent < 1.0) {
				warpPower = -Math.log(2) / Math.log(percent);
			} else {
				warpPower = 1.0;
			}
		}
		assert warpPower > 0.0 : expectedPercentOn() + " " + warpPower;
		return warpPower;
	}

	double warp(double percent) {
		if (percent == 0.0 || percent == 1.0)
			return percent;
		double powWarped = Math.pow(percent, warpPower());
		double result = 1.0 / (1.0 + Math.pow(Math.E, sigmoidSteepness
				* (0.5 - powWarped)));
		result = (result - sigmoidMin) / sigmoidScale;
		// if (getName().equals("Genre"))
		// Util.print(warpPower + " " + expectedPercentOn() + " percent="
		// + percent + " " + powWarped + " " + result);
		return result;
	}

	double unwarp(double y) {
		if (y == 0.0 || y == 1.0)
			return y;
		y = y * sigmoidScale + sigmoidMin;
		double result = Math.pow(0.5 - Math.log(1.0 / y - 1.0)
				/ sigmoidSteepness, 1.0 / warpPower());
		// if (getName().equals("Genre"))
		// Util.print("unwarp " + warpPower + " y=" + x + " => " + y + " "
		// + result + " inv=" + warp(result));
		return result;
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
				+ totalChildTotalCount() + " "
				+ PrintArray.printArrayString(perspectives);
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
	}

	public String getName() {
		if (cachedName == null) {
			// Util.print("Rank.getName "
			// + PrintArray.printArrayString(perspectives));
			int nPerspectives = perspectives.length;
			String[] names = new String[nPerspectives];
			int index = 0;
			for (int i = 0; i < nPerspectives; i++) {
				names[index++] = perspectives[i].getName();
			}
			cachedName = Util.arrayToEnglish(names, " and ");
			if (parentRank != null)
				cachedName = namePrefix(false) + cachedName;
		}
		return cachedName;
	}

	Perspective[] getRestrictions(boolean require) {
		Perspective[] restrictions = {};
		for (int i = 0; i < perspectives.length; i++) {
			restrictions = (Perspective[]) Util.append(restrictions,
					perspectives[i].p.getRestrictionFacetInfos(true, require),
					Perspective.class);
		}
		// Util.print(this + ".getRestrictions(" + require + ") return "
		// + PrintArray.printArrayString(restrictions));
		return restrictions;
	}

	Perspective[] getPerspectives() {
		Perspective[] restrictions = {};
		for (int i = 0; i < perspectives.length; i++) {
			restrictions = (Perspective[]) Util.push(restrictions,
					perspectives[i].p, Perspective.class);
		}
		return restrictions;
	}

	// Update name and restrictionName labels
	void updatePerspectiveSelections(Perspective mousedFacet, boolean state) {
		// Util.print("Rank.updatePerspectiveSelections " + getName() + " "
		// + mousedFacet + " " + isRestricted() + " " + parentRank + " "
		// + perspectives[0].p);
		perspectives[0].rankLabel.updateSelections(mousedFacet);

		// Update bars
		for (int i = 0; i < perspectives.length; i++)
			perspectives[i].updateSelection(mousedFacet, state);
	}

	String namePrefix(boolean forRestriction) {
		String result = "";
		int level = level();
		if (forRestriction) {
			level++;
			// result = "\n";
		}
		if (level > 0) {
			for (int i = 1; i < level; i++)
				result += "  ";
			result += Bungee.parentIndicatorPrefix;
		}
		return result;
	}

	int level() {
		if (parentRank == null)
			return 0;
		else
			return 1 + parentRank.level();
	}

	void updateNameLabels() {
		Markup content = Query.emptyMarkup();
		String prefix = namePrefix(false);
		if (prefix.length() > 0)
			content.add(prefix);
		Query.toEnglish(getPerspectives(), " and ", content);

		Markup[] descriptions = new Markup[2];
		boolean[] reqtTypes = { true, false };
		for (int type = 0; type < 2; type++) {
			boolean reqtType = reqtTypes[type];
			Perspective[] info = getRestrictions(reqtType);
			if (info != null && info.length > 0) {
				descriptions[type] = Query.emptyMarkup();
				Query.toEnglish(info, " or ", descriptions[type]);
			}
		}
		Markup result = perspectives[0].p.tagDescription(descriptions, false,
				Util.splitSemicolon(" ; NOT "));

		if (result.size() > 0) {
			content.add(Markup.NEWLINE_TAG);
			content.add(namePrefix(true));
			content.addAll(result);
		}

		// Util.print("Rank.updateNameLabels " + getName() + " " + content);

		TextNfacets label = perspectives[0].rankLabel;
		label.setContent(content);
		perspectives[0].layoutRankLabel(true);
	}

	public boolean isConnected() {
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
				&& perspectives[index].p.getIndex() < pv.p.getIndex()) {
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

		decacheName();
		decacheTotalChildOnCount();
		addChild(pv);
		redraw();
	}

	// returns isZombie
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
				decacheTotalChildOnCount();
				redraw();
			}
		}
		return perspectives.length == 0;
	}

	public void highlight(boolean state) {
		// Util.print("Rank.highlight " + this + " " + state);
		if (perspectives.length == 1)
			// This will paint more efficiently
			art().highlightFacet(perspectives[0].p, state,
					isConnected() ? -1 : -2);
		else {
			Markup desc = null;
			if (state && !isConnected()) {
				desc = Query.emptyMarkup();
				desc.add("open category ");
				Query.toEnglish(getPerspectives(), " and ", desc);
			}
			art().setClickDesc(desc);
		}
	}

	private Bungee art() {
		return summary.art;
	}

	public void mayHideTransients() {
		summary.mayHideTransients();
	}

	private double startOffset;

	private double goalOffset;

	private double startH;

	private double goalH;

	public void prepareAnimation(double offset, double desiredH) {
		// Util.print("prepareAnimation " + getName() + " " + offset + " "
		// + desiredH);
		startOffset = getYOffset();
		if (startOffset == 0) {
			// we've just been created.
			if (parentRank == null)
				startOffset = offset;
			else
				startOffset = parentRank.getYOffset() + parentRank.getHeight();
		}
		startH = getHeight();
		if (parentRank == null && startH == 0)
			// This is the initial draw
			startH = desiredH;

		goalOffset = offset;
		goalH = desiredH;
	}

	public void animateRank(float zeroToOne) {
		double y = Math.round(startOffset + zeroToOne
				* (goalOffset - startOffset));
		double newH = Math.round(startH + zeroToOne * (goalH - startH));
		// if (newH != getHeight() || y != getYOffset())
		// Util.print("animateRank " + getName() + " " + y + " " + newH);
		setOffset(0, y);
		setHeight(newH);
	}

	public void updateHeights() {
		for (int i = 0; i < perspectives.length; i++)
			perspectives[i].layoutPercentLabels();
	}

	public String toString() {
		return "<Rank " + (perspectives[0] != null ? perspectives[0].p : null)
				+ ">";
	}
}

class SelectionHandler extends MyInputEventHandler {

	public SelectionHandler() {
		super(Rank.class);
	}

	public boolean click(PNode node) {
		return ((Rank) node).click();
	}

	public void mayHideTransients(PNode node) {
		((Rank) node).mayHideTransients();
	}

	public boolean enter(PNode node) {
		// Util.print("Rank.SelectionHandler.enter");
		((Rank) node).highlight(true);
		return true;
	}

	public boolean exit(PNode node) {
		((Rank) node).highlight(false);
		return true;
	}
}
