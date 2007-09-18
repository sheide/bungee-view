/* 

 Created on May 25, 2006 

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
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;



import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.TextButton;
import edu.cmu.cs.bungee.piccoloUtils.gui.VScrollbar;
import edu.umd.cs.piccolo.PNode;

class PerspectiveList extends LazyPNode implements MouseDoc, PerspectiveObserver {

	// private Perspective parent;

	private Perspective selected;

	private int[] counts;

	private final APText label;

	// private double labelH = 0;

	private static final long serialVersionUID = -4454383021673737826L;

	final Bungee art;

	private double w;

	private double h;

	private double margin = 5;

	private double sortButtonMargin = 1;

	final VScrollbar scrollbar;

	private LazyPPath border = new LazyPPath();

	private SortButton sortBySelection;

	private SortButton sortByOnCount;

	private SortButton sortByNatural;

	int sortField = SortButton.SORT_BY_NATURAL_ORDER;

	double maxNameW;

	/**
	 * 1: a-z; -1: z-a
	 */
	int sortDirection = 1;

	// private final PerspectiveListClickHandler clickHandler;

	private int maxCount;

	private Perspective longestName;

	PerspectiveList(Bungee a) {
		art = a;
		// clickHandler = new PerspectiveListClickHandler(art);
		// setPickable(false);
		setPaint(Color.getHSBColor(0, 0f, 0.1f));

		double labelScale = 1;
		label = art.oneLineLabel();
		label.scale(labelScale);
		label.setTextPaint(Color.gray);
		label.setPickable(false);
		// labelH = labelScale * art.lineH;

		Runnable scroll = new Runnable() {
			public void run() {
				int offset = (int) (scrollbar.getPos() * nInvisibleValues() + 0.5);
				showList(offset);
			}
		};
		
		scrollbar = new VScrollbar(10, 50, Color.white, Color.lightGray, scroll);

		sortBySelection = new SortButton(this, SortButton.SORT_BY_SELECTION);
		sortByOnCount = new SortButton(this, SortButton.SORT_BY_ON_COUNT);
		sortByNatural = new SortButton(this, SortButton.SORT_BY_NATURAL_ORDER);
		sortBySelection.setJustification(Component.CENTER_ALIGNMENT);
		sortByOnCount.setJustification(Component.CENTER_ALIGNMENT);
		sortByNatural.setJustification(Component.CENTER_ALIGNMENT);

		border.setStroke(LazyPPath.getStrokeInstance(3));
		border.setStrokePaint(Bungee.goldBorderColor);
		border.setPickable(false);
	}

	void validate() {
		if (!isHidden()) {
			boolean onTop = false;
			Summary summary = art.summary;
			Rank connectedRank = summary.connectedRank();
			if (connectedRank != null) {
				Rectangle2D rankLabelBounds = summary
						.globalToLocal(connectedRank.perspectives[0].rankLabel
								.getGlobalBounds());
				double y = (int) (onTop ? rankLabelBounds.getMinY()
						: rankLabelBounds.getMaxY());
				if (onTop) {
					setOffset(sortButtonMargin, 0);
					h = y;
				} else {
					setOffset(sortButtonMargin, y);
					h = summary.getHeight() - y;
				}
				// w = 2 * summary.queryW;
				// setBounds(0, 0, w, h);
				label.setFont(art.font);
				label.setHeight(art.lineH);
				redraw();
				// Util.print("PerspectiveList.validate " + w + " " + h + " " +
				// y);
			}
		}
	}

	double validateSortButtons(double numY) {
		setButtonOrder();
		label.setOffset(Math
				.round((w - label.getScale() * label.getWidth()) / 2.0), 0.0);
		double y = label.getMaxY() + margin;
		double checkBoxW = art.checkBoxWidth * 0.7;
		double numW = (int) (numY + art.lineH / 3);
		sortBySelection.setWidth(checkBoxW - 2 * sortButtonMargin);
		sortBySelection.setOffset(margin, y);
		addChild(sortBySelection);

		sortByOnCount.setWidth(numW);
		sortByOnCount.setOffset(margin + checkBoxW, y);
		addChild(sortByOnCount);

		double naturalOffset = sortByOnCount.getMaxX() + sortButtonMargin;
		sortByNatural.setOffset(naturalOffset, y);
		w = longestName == null ? 0 : Math.min(naturalOffset
				+ art.getFacetStringWidth(longestName, true, true),
				2 * art.summary.queryW);
		w = Math.max(label.getWidth(), w);
		if (isScrollBar()) {
			w += art.scrollBarWidth + 2 * art.scrollMarginSize;
		}
		// Util.print(" v " + longestName + " "
		// + art.getFacetStringWidth(longestName, false, false) + " "
		// + naturalOffset + " " + isScrollBar());

		sortByNatural.setWidth((int) (w - naturalOffset - sortButtonMargin));
		addChild(sortByNatural);
		// setWidth(w);
		label.setOffset(Math
				.round((w - label.getScale() * label.getWidth()) / 2.0), 0.0);

		return (int) (y + 1.5 * art.lineH);
	}

	public void redraw() {
		if (!isHidden())
			showList(selected);
	}

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	void show(boolean state) {
		if (!state) {
			removeFromParent();
			// art.selectedItem.setChildrenPickable(true);
		} else {
			showList(selected);
		}
	}

	boolean isHidden() {
		return getParent() == null;
	}

	int size() {
		return counts == null ? 0 : counts.length;
	}

	void maybeInit() {
		// Util.print("maybeInit() " + isHidden() + " " + parent);
		if (isHidden()) {
			art.summary.addChild(this);
			// art.selectedItem.setChildrenPickable(false);
			moveToFront();
			sortField = SortButton.SORT_BY_NATURAL_ORDER;
			sortDirection = 1;

			longestName = null;
			maxCount = 0;
			Perspective parent = selected.getParent();
			counts = new int[parent.nChildren()];
			ResultSet rs = art.query.getCountsIgnoringFacet(parent);
			if (rs == null) {
				for (int i = 0; i < size(); i++) {
					Perspective v = parent.getNthChild(i);
					counts[i] = v.totalCount;
					if (counts[i] > maxCount)
						maxCount = counts[i];
					checkName(v);
				}
			} else {
				try {
					while (rs.next()) {
						Perspective v = art.query.findPerspective(rs.getInt(1));
						assert v.getParent() == parent;
						int i = v.getIndex();
						int count = rs.getInt(2);
						assert count <= v.totalCount : count + " " + v;
						counts[i] = count;
						if (counts[i] > maxCount)
							maxCount = counts[i];
						checkName(v);
					}
				} catch (SQLException se) {
					Util.err("SQL Exception in PerspectiveList.init: "
							+ se.getMessage());
					se.printStackTrace();
				}
			}
			label.setText("(" + Util.addCommas(size()) + " values)");
			validate();
		}
	}

	void checkName(Perspective v) {
		if (w < 2 * art.summary.queryW) {
			if (longestName == null)
				longestName = v;
			else {
				String name = v.getNameIfPossible();
				if (name != null
						&& art.getFacetStringWidth(v, true, true) > art
								.getFacetStringWidth(longestName, true, true)) {
					longestName = v;
				}
			}
			// Util.print("checkName " + v + " " + longestName + " " + w + " "
			// + art.getFacetStringWidth(v, false, false));
		}
	}

	void setSelected(Perspective _selected) {
		// Util.print("PL.setSelected " + isHidden());
		selected = _selected;
		art.arrowFocus = selected;
		maybeInit();
	}

	Perspective handleArrow(Perspective _selected, int key, int modifiers) {
		// Util.print("PerspectiveViz.handleArrow " + key);
		setSelected(_selected);
		int sortIndex = getSortIndex(_selected);
		int delta = 0;
		switch (key) {
		case java.awt.event.KeyEvent.VK_KP_LEFT:
		case java.awt.event.KeyEvent.VK_LEFT:
		case java.awt.event.KeyEvent.VK_KP_UP:
		case java.awt.event.KeyEvent.VK_UP:
			delta = -1;
			break;
		case java.awt.event.KeyEvent.VK_KP_RIGHT:
		case java.awt.event.KeyEvent.VK_RIGHT:
		case java.awt.event.KeyEvent.VK_KP_DOWN:
		case java.awt.event.KeyEvent.VK_DOWN:
			delta = 1;
			break;
		case java.awt.event.KeyEvent.VK_HOME:
			delta = -sortIndex;
			break;
		case java.awt.event.KeyEvent.VK_END:
			delta = size() - 1 - sortIndex;
			break;
		case java.awt.event.KeyEvent.VK_A:
			if (Util.isControlDown(modifiers)) {
				sortIndex = 0;
				_selected = getFacet(0);
				if (!_selected.isRestriction(true))
					art.toggleFacet(_selected, 0);
				modifiers = InputEvent.SHIFT_DOWN_MASK;
				delta = size() - 1;
			}
			break;
		}
		if (delta != 0) {
			for (int index = sortIndex + delta; index >= 0 && index < size(); index += delta) {
				Perspective p = getFacet(index);
				// Util.print(selected + " " + sortIndex + " " + delta + " " +
				// index + " " + p);
				if (counts[p.getIndex()] > 0) {
					setSelected(p);
					art.toggleFacet(selected, modifiers);
					art.setClickDesc((Markup) null);
					break;
				}
			}
			art.printUserAction(Bungee.KEYPRESS, "PerspectiveList", selected.getID());
			showList(selected);
			return selected;
		}
		return null;
	}

	private int nLines() {
		double internalH = h - sortByNatural.getMaxY(); // + art.lineH / 2;
		return (int) (internalH / art.lineH);
	}

	int nInvisibleValues() {
		return size() - nLines();
	}

	void showList(Perspective p) {
		int lineOffset = p == null ? 0 : getSortIndex(p) - nLines() / 2;
		showList(lineOffset);
	}

	boolean isScrollBar() {
		return nInvisibleValues() > 0;
	}

	void showList(int lineOffset) {
		// Util.print("PL.showList " + lineOffset);
		if (size() != selected.getParent().nChildren()) {
			// In case we've added or removed facets
			show(false);
			return;
		}
		moveToFront();
		removeAllChildren();
		double numY = art.numWidth(maxCount);
		double y = validateSortButtons(numY);
		double nameY = w - 10.0 - numY;
		ItemPredicate initialLongName = longestName;
		int nLines = nLines();
		int nInvisibleValues = nInvisibleValues();
		int maxLine;
		if (nInvisibleValues > 0) {
			lineOffset = Util.constrain(lineOffset, 0, size() - nLines);
			if (lineOffset != (int) (scrollbar.getPos() * nInvisibleValues + 0.5)) {
				scrollbar.setPos(lineOffset / (double) nInvisibleValues);
				// setPos will call us again.
				return;
			}
			nameY -= art.scrollBarWidth + 2 * art.scrollMarginSize;
			maxLine = lineOffset + nLines;

			scrollbar.setOffset(w - art.scrollBarWidth + art.scrollMarginSize,
					y);
			scrollbar.setH(h - y - 2);
			scrollbar.setBufferPercent(nLines, size());
			addChild(scrollbar);
		} else {
			lineOffset = 0;
			maxLine = size();
		}

		addChild(label);
		addChild(border);
		for (int i = lineOffset; i < maxLine; i++) {
			Perspective p = getFacet(i);
			FacetText pLabel = FacetText.getFacetText(p, art, numY, nameY,
					true, true, true, null, counts[p.getIndex()], null, -1,
					null);
			addChild(pLabel);
			// Color c = counts[i] == 0 ? Color.darkGray : art.facetBarColor(p);
			// if (p == selected)
			// c = Color.WHITE;
			// pLabel.setTextPaint(c);
			// pLabel.setText(art.facetLabel(p, numY, nameY, counts[i], true,
			// true, true, false));
			pLabel.setOffset(margin, y);
			// pLabel.dontHideTransients = true;
			// pLabel.highlightFacet();
			// pLabel.isPickable = true;
			// pLabel.addInputEventListener(clickHandler);

			// if (p.isRestriction()) {
			// APText checkBox = art.oneLineLabel();
			// checkBox.setTextPaint(Art.darkGreen);
			// checkBox.setText("\u221A");
			// addChild(checkBox);
			// checkBox.setOffset(5.0, y);
			// checkBox.setPickable(false);
			// }
			checkName(p);

			y += art.lineH;
		}
		if (isScrollBar())
			y = h;
		setBounds(0, 0, w, Math.min(h, y));
		border.setPathToRectangle(0, 0, (float) (w), (float) Math.min(h, y));
		if (longestName != initialLongName)
			showList(lineOffset);
	}

	void showFacet(Perspective facet) {
		if (!isHidden() && facet.getParent() == selected.getParent()) {
			// Util.print("PL.showFacet " + facet);
			Iterator it = getChildrenIterator();
			while (it.hasNext()) {
				PNode n = (PNode) it.next();
				if (n instanceof FacetText) {
					FacetText f = (FacetText) n;
					if (f.getFacet() == facet) {
						f.setColor(counts[facet.getIndex()]);
						break;
					}
				}
			}
		}
	}

	class SortButton extends TextButton {

		private static final long serialVersionUID = 8163957367626599694L;

		static final int SORT_BY_SELECTION = 1;

		static final int SORT_BY_ON_COUNT = 2;

		static final int SORT_BY_TOTAL_COUNT = 3;

		static final int SORT_BY_NATURAL_ORDER = 4;

		private static final String z_aLabel = "\u2193";

		private static final String a_zLabel = "\u2191";

		private static final String noneLabel = " ";

		int order = SORT_BY_NATURAL_ORDER;

		// boolean reverseOrder = false;

		PerspectiveList parent;

		SortButton(PerspectiveList _parent, int field) {
			super(field == SORT_BY_NATURAL_ORDER ? a_zLabel : noneLabel,
					art.font, 0, 0, -1, -1, null, 1.8f, Color.darkGray, Color
							.getHSBColor(0, 0, 0.1f));
			mouseDoc = "Sort by this column";
			parent = _parent;
			order = field;
		}

		void setOrder(int direction) {
			String s = null;
			switch (direction) {
			case 1:
				s = a_zLabel;
				break;
			case 0:
				s = noneLabel;
				break;
			case -1:
				s = z_aLabel;
				break;
			default:
				assert false : direction;
			}
			setText(s);
		}

		int getDirection() {
			int result = 0;
			String s = getText();
			if (s.equals(a_zLabel))
				result = 1;
			else if (s.equals(noneLabel))
				result = 0;
			else if (s.equals(z_aLabel))
				result = -1;
			else
				assert false : s;
			return result;
		}

		public void doPick() {
			parent.setOrder(order, getDirection());
		}

		// public void mayHideTransients(PNode node) {
		// parent.mayHideTransients();
		// }

	}

	public void setOrder(int order, int direction) {
		// Util.print("PerspectiveList.setOrder " + order + " " + direction);
		// Util.print(sortDirection + " " + sortField);
		if (sortField == order) {
			assert direction == sortDirection;
			sortDirection = sortDirection == 1 ? -1 : 1;
		} else {
			assert direction == 0;
			sortField = order;
			sortDirection = sortField == SortButton.SORT_BY_NATURAL_ORDER ? 1
					: -1;
		}
		// Util.print("... " + order + " " + direction);
		selected = getFacet(0);
		redraw();
	}

	void setButtonOrder() {
		sortBySelection
				.setOrder(sortField == SortButton.SORT_BY_SELECTION ? sortDirection
						: 0);
		sortByOnCount
				.setOrder(sortField == SortButton.SORT_BY_ON_COUNT ? sortDirection
						: 0);
		sortByNatural
				.setOrder(sortField == SortButton.SORT_BY_NATURAL_ORDER ? sortDirection
						: 0);
	}

	int getSortIndex(Perspective facet) {
		int result = -1;
		Perspective parent = selected.getParent();
		switch (sortField) {
		case SortButton.SORT_BY_NATURAL_ORDER:
			result = facet.getIndex();
			break;
		case SortButton.SORT_BY_ON_COUNT:
			parent.sortDataIndexByOn();
			for (int i = 0; i < size() && result < 0; i++) {
				ItemPredicate child = parent.getNthOnValue(i);
				if (child == facet)
					result = i;
			}
			break;
		case SortButton.SORT_BY_TOTAL_COUNT:
			assert false;
		case SortButton.SORT_BY_SELECTION:
			result = facet.getIndex();
			int indexOffset = 0;
			Perspective[] restrictions = parent.allRestrictions();
			int nRestrictions = restrictions.length;
			for (int i = 0; i < nRestrictions; i++) {
				Perspective child = restrictions[i];
				if (child.getIndex() > result)
					indexOffset++;
			}
			result += indexOffset;
			break;
		default:
			assert false : sortField;
			break;
		}
		if ((sortDirection == -1) == (sortField == SortButton.SORT_BY_NATURAL_ORDER))
			result = size() - result - 1;
		assert result >= 0 && result < size() : facet + " " + result + " "
				+ size();
		return result;
	}

	Perspective getFacet(int sortIndex) {
		Perspective result = null;
		Perspective parent = selected.getParent();
		if ((sortDirection == -1) == (sortField == SortButton.SORT_BY_NATURAL_ORDER))
			sortIndex = size() - sortIndex - 1;
		switch (sortField) {
		case SortButton.SORT_BY_NATURAL_ORDER:
			result = parent.getNthChild(sortIndex);
			break;
		case SortButton.SORT_BY_ON_COUNT:
			parent.sortDataIndexByOn();
			result = parent.getNthOnValue(sortIndex);
			break;
		case SortButton.SORT_BY_TOTAL_COUNT:
			assert false;
		case SortButton.SORT_BY_SELECTION:
			Perspective[] restrictions = parent.allRestrictions();
			int nRestrictions = restrictions.length;
			if (nRestrictions > 0) {
				if (nRestrictions > 1) {
					Perspective[] temp = new Perspective[nRestrictions];
					System.arraycopy(restrictions, 0, temp, 0, nRestrictions);
					restrictions = temp;
					Arrays.sort(restrictions);
				}
				if (sortIndex < nRestrictions) {
					result = restrictions[nRestrictions - sortIndex - 1];
				} else {
					for (int i = 0; i < nRestrictions && result == null; i++) {
						Perspective child = restrictions[nRestrictions - i - 1];
						int nSkipped = child.getIndex() - i;
						if (nSkipped > sortIndex - nRestrictions)
							result = parent.getNthChild(sortIndex
									- nRestrictions + i);
					}
					if (result == null)
						result = parent.getNthChild(sortIndex);
				}
			} else {
				result = parent.getNthChild(sortIndex);
			}
			break;
		default:
			assert false : sortField;
			break;
		}
		assert result != null : parent + " " + sortIndex;
		return result;
	}
}
