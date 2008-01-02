/* 

 Created on Mar 20, 2005

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
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;




 final class MouseDocLine extends LazyPNode implements PerspectiveObserver {

	private static final Color tipBG = Bungee.helpColor; //Color.yellow;

	private TextNfacets clickDesc;

	private APText tip;

	Bungee art;

	private TextNfacets facetDesc;

	 void validate(double w, double h) {
		// Util.print("MouseDoc.validate: " + _w + " x " + _h);
		setBounds(0, 0, w, h);
		clickDesc.setBounds(0, 0, w, h);
		tip.setBounds(0, 0, w, h);
	}

	 MouseDocLine(Bungee _art) {
		art = _art;
		facetDesc = new TextNfacets(art, Color.lightGray, false);
		// facetDesc.setJustification(Component.CENTER_ALIGNMENT);
		// RIGHT_ALIGNMENT);
		facetDesc.setPaint(Util.lighten(Bungee.headerBG, 1.2f));
		// facetDesc.setWrapText(true);
		// facetDesc.facetTextPaint = Color.white;
		// addChild(facetDesc);
		// Do this after clickDesc, so facetDesc will be on top.
		// NO - now we make facetDesc full-width and transparent, so we don't
		// have to compute it and reposition it.
		facetDesc.setPickable(false);
		facetDesc.setChildrenPickable(false);
		facetDesc.setWrapOnWordBoundaries(true);
		facetDesc.setWrapText(true);
		facetDesc.setVisible(false);
		facetDesc.setTrim(0, 0);
		facetDesc.setRedrawer(this);
		addChild(facetDesc);

		clickDesc = new TextNfacets(art, Bungee.mouseDocFG, false);
		clickDesc.setPaint(Bungee.headerBG.brighter());
		clickDesc.setVisible(false);
		clickDesc.facetPermanentTextPaint = Color.white;
		// clickDesc.setWrapText(true);
		clickDesc.setTrim(20, 0);
		addChild(clickDesc);

		tip = art.oneLineLabel();
		// tip.setHeight(2 * art.lineH);
		// tip.setTextPaint(Color.black);
		tip.setConstrainWidthToTextWidth(false);
		tip.setPaint(tipBG);
		tip.setVisible(false);
		addChild(tip);
		// Do this after facetDesc, so it will be on top.

		setPickable(false);
		setChildrenPickable(false);
		setPaint(Bungee.headerBG);
	}

	 void setTip(String s) {
		if (s != null) {
			tip.setText(s);
			tip.setVisible(true);
			tip.setWidth(0);
			tip.animateToBounds(0, 0, getWidth(), getHeight(),
					Bungee.dataAnimationMS);
			tip.moveToFront();
		} else {
			tip.setVisible(false);
		}
	}

	 void setClickDesc(String s) {
		// Util.print("setClickDesc " + s);
		if (s == null)
			defaultClickDesc();
		else {
			Markup v = Query.emptyMarkup();
			v.add(s);
			setClickDesc(v);
		}
	}

	 void setClickDesc(Markup s) {
		// Util.print("setClickDesc " + s);
		if (s == null)
			defaultClickDesc();
		else {
			s.add(0, "Clicking will: ");
			setClickDescInternal(s);
		}
	}

	private void setClickDescInternal(Markup s) {
//		 Util.print("setClickDesc " + s);
		assert s != null;
		clickDesc.setContent(s);
		// Allow room for facetDoc to at least say "mmmm (100% of nnnn) P=1E-10"
		double maxW = getWidth() - (art.isPopups ? 0 : art.lineH * 20);
		clickDesc.setWidth(maxW);
		clickDesc.layout();
		// clickDesc.trim(20, 0);
		clickDesc.setVisible(true);
	}

	/**
	 * The mouse doc to display when the mouse isn't over something you can click on.
	 */
	private void defaultClickDesc() {
		// if (clickDesc.content != null) {
		// for (Iterator it=clickDesc.content.iterator(); it.hasNext(); ) {
		// if (it.next() instanceof Cluster)
		// Util.printStackTrace();
		// }
		// }
		if (art.arrowFocus == null
				&& (art.grid == null || art.grid.onCount <= 1)) {
			clickDesc.setVisible(false);
		} else {
			Markup v = Query.emptyMarkup();
			v.add("Arrow keys will move through: ");
			if (art.arrowFocus == null) {
				v.add(Bungee.gridFG);
				v.add("Results");
			} else
				v.add(art.arrowFocus.getParent());
			setClickDescInternal(v);
		}
	}

	static final DecimalFormat pValueFormat = new DecimalFormat("0E0");

	static final DecimalFormat pValueFormat2 = new DecimalFormat("0.0#");

	static final FieldPosition stupid = new FieldPosition(
			NumberFormat.FRACTION_FIELD);

	 void showObjectDesc(Markup desc, Object facet) {
		// Util.print("showObjectDesc " + desc);
		if (desc == null || facet == null) {
			facetDesc.setVisible(false);
		} else {
			facetDesc.setContent(desc);
			// facetDesc.update((Perspective) facet, true);
			facetDesc.setVisible(true);
			redraw();
		}
		// gui.Util.printDescendents(facetDesc);
	}

	 void showPopup(Perspective facet) {
		// Util.print("MouseDoc.showFacet " + state + " " + facet);
		Markup desc = null;
		if (facet != null) {
			desc = Query.emptyMarkup();
			if (facet.getParent() == null) {
				desc.add(Util.pluralize(art.query.genericObjectLabel));
				desc.add(" having ");
			}
			desc.add(facet);
			desc.add(facetInfo(facet));
		}
		showObjectDesc(desc, facet);
	}

	 public void redraw() {
		if (facetDesc.getVisible()) {
			facetDesc.layout(getWidth(), getHeight());
//			Util.print("MD.redraw " + facetDesc.getWidth() + " " + facetDesc.toText());
			facetDesc.setXoffset(getWidth() - facetDesc.getWidth());
		}
	}

	String facetInfo(Perspective facet) {
		// Util.printDeep(facet.setChiSqTable());
		return facetInfo(facet.guessOnCount(), facet.getTotalCount(), facet.pValue());
	}

	String facetInfo(Cluster facet) {
		return facetInfo(facet.getOnCount(), facet.getTotalCount(), facet.pValue());
	}

	String facetInfo(int onCount, int count, double pValue) {
		// Util.print("MD.facetInfo " + name + " " + onCount);

		StringBuffer buf = new StringBuffer(60);
		buf.append(": ");

		if (onCount >= 0)
			buf.append(Util.addCommas(onCount));
		else
			// This is for deeply nested facets in SelectedItem FacetTree.
			buf.append("?");
		if (onCount == 1)
			buf.append(" match (");
		else
			buf.append(" matches (");

		ResultsGrid.formatPercent(onCount / (double) count, buf);

		buf.append(" of ");
		buf.append(Util.addCommas(count));
		// buf.append(getDescription(name);
		buf.append(") ");
		if (pValue >= 0.0)
			formatPvalue(pValue, buf);
		return buf.toString();
	}

	static StringBuffer formatPvalue(double pValue, StringBuffer buf) {
		if (!Double.isNaN(pValue)) {
			buf.append("p=");
			if (pValue == 0.0)
				buf.append("0");
			else if (pValue == 1.0)
				buf.append("1");
			else if (pValue > 0.0095)
				pValueFormat2.format(pValue, buf, stupid);
			else
				pValueFormat.format(pValue, buf, stupid);
		}
		return buf;
	}

}
