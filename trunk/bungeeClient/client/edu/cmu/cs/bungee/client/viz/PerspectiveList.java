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
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.comparator.IntValueComparator;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.SortButton;
import edu.cmu.cs.bungee.piccoloUtils.gui.SortButtons;
import edu.cmu.cs.bungee.piccoloUtils.gui.VScrollbar;
import edu.umd.cs.piccolo.PNode;

final class PerspectiveList extends LazyPNode implements MouseDoc,
		PerspectiveObserver, SortButtons {

	private static final long serialVersionUID = 1L;

	/**
	 * Draw the list above the rank label (as opposed to below)?
	 */
	private static final boolean onTop = false;

	// private Perspective parent;

	/**
	 * The current Bar - base from which arrow keys navigate. Null if we're not
	 * being used.
	 */
	private Perspective selected;

	/**
	 * The onCounts that would result from selecting each Bar (i.e. selecting
	 * siblings of selected).
	 */
	int[] counts;

	private SortButton sortBySelection;

	private SortButton sortByOnCount;

	private SortButton sortByNatural;

	int sortField = SORT_BY_NATURAL_ORDER;

	double maxNameW;

	/**
	 * 1: a-z; -1: z-a
	 */
	int sortDirection = 1;

	private int maxCount;

	private Perspective longestNamedFacet;

	private final APText label;

	final Bungee art;

	/**
	 * Width of this LazyPNode
	 */
	private double w;

	private double h;

	/**
	 * Column separation, and also vertical separation below buttons
	 */
	private double margin = 5;

	/**
	 * Offset of this PerspectiveList
	 */
	private double sortButtonMargin = 8;

	final VScrollbar scrollbar;

	private LazyPPath border = new LazyPPath();

	private Perspective[] countSortedFacets;

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
				// Util.print(scrollbar.getPos() + " " + size() + " " +
				// nLines());
				showList(offset);
			}
		};

		scrollbar = new VScrollbar(10, 50, Color.white, Color.lightGray, scroll);

		sortBySelection = new SortButton(SORT_BY_SELECTION, -1, art.font, this,
				Color.gray, Color.getHSBColor(0, 0, 0.15f));
		sortByOnCount = new SortButton(SORT_BY_ON_COUNT, -1, art.font, this,
				Color.gray, Color.getHSBColor(0, 0, 0.15f));
		sortByNatural = new SortButton(SORT_BY_NATURAL_ORDER, 1, art.font,
				this, Color.gray, Color.getHSBColor(0, 0, 0.15f));

		border.setStroke(LazyPPath.getStrokeInstance(3));
		border.setStrokePaint(Bungee.goldBorderColor);
		border.setPickable(false);
	}

	void setSelected(Perspective _selected) {
		// Util.print("PL.setSelected " + isHidden());
		assert _selected.getParent() != null : _selected;
		art.setArrowFocus(_selected);
		init(_selected);
	}

	void validate() {
		if (!isHidden()) {
			Summary summary = art.summary;
			Rank connectedRank = summary.connectedRank();
			assert connectedRank != null;
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
			label.setFont(art.font);
			// label.setHeight(art.lineH);
			redraw();
			// Util.print("PerspectiveList.validate " + w + " " + h + " " +
			// y);
		}
	}

	@Override
	public double maxWidth() {
		return 2 * art.summary.queryW;
	}

	private double countXoffset() {
		return margin + art.checkBoxWidth;
	}

	private double nameXoffset() {
		// For some reason things line up better adding 1 space instead of 2
		return sortByOnCount.getMaxX() + art.spaceWidth;
	}

	/**
	 * 
	 * layout: <margin> <art.checkBoxWidth> <numW> " " <name> <margin>
	 * <scrollbar> <art.scrollMarginSize>
	 * 
	 * numW has margin built in to it. The two spaces are added by
	 * Bungee.facetLabel.
	 * 
	 * @param numW
	 *            the numW we'll pass to getFacetText. Note that each row is a
	 *            single FacetText, so column placement is dictated by that, not
	 *            arbitrary constants we get to set.
	 * @return the y-coordinate where we can start drawing tags
	 */
	double validateSortButtons(double numW) {
		sortBySelection.updateButtons(sortField, sortDirection);
		// label.setOffset(Math
		// .round((w - label.getScale() * label.getWidth()) / 2.0), 0.0);
		double y = label.getMaxY() + margin;

		// art.checkBoxWidth has some extra space on the right built in, so the
		// 0.7 let's us line up more exactly with the boxes.
		sortBySelection.setWidth(art.checkBoxWidth * 0.7);
		sortBySelection.setOffset(margin, y);
		sortBySelection.positionChild();
		addChild(sortBySelection);

		sortByOnCount.setWidth((int) (numW));
		sortByOnCount.setOffset(countXoffset(), y);
		sortByOnCount.positionChild();
		addChild(sortByOnCount);

		double rightMargin = margin
				+ (isScrollBar() ? art.scrollBarWidth + art.scrollMarginSize
						: 0);
		w = Math.max(label.getWidth() + 2 * margin, Math.min(maxWidth(),
				nameXoffset() + longestNameLength() + rightMargin));
		// Util.print(" v " + longestName + " "
		// + art.getFacetStringWidth(longestName, false, false) + " "
		// + naturalOffset + " " + isScrollBar());

		sortByNatural.setWidth((int) (w - nameXoffset() - rightMargin));
		sortByNatural.setOffset(nameXoffset(), y);
		sortByNatural.positionChild();
		addChild(sortByNatural);

		// setWidth(w);
		label.setOffset(Math
				.round((w - label.getScale() * label.getWidth()) / 2.0), 0.0);

		return (int) (sortByNatural.getMaxY() + margin);
	}

	public void redraw() {
		if (!isHidden())
			showList(lineOffset());
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// art.setMouseDoc(source, state);
	// }

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}

	// public void setMouseDoc(Markup doc, boolean state) {
	// art.setMouseDoc(doc, state);
	// }

	void toggle() {
		// Util.print("PL.toggle "+isHidden());
		if (!isHidden()) {
			removeFromParent();
			// It's confusing if the arrows don't go by natural order if the
			// list is hidden
			sortField = SORT_BY_NATURAL_ORDER;
			sortDirection = 1;
			// art.selectedItem.setChildrenPickable(true);
		} else if (art.getShowTagLists()) {
			art.summary.addChild(this);
			validate();
		}
	}

	boolean isHidden() {
		return getParent() == null;
	}

	Perspective listedPerspective() {
		return (!isHidden() && selected != null) ? selected.getParent() : null;
	}

	Perspective pvp() {
		return selected != null ? selected.getParent() : null;
	}

	int size() {
		return counts == null ? 0 : counts.length;
	}

	void init(Perspective _selected) {
		// Util.print("PL.init() " + isHidden() + " " + _selected);
		assert _selected != null;
		Perspective _selectedParent = _selected.getParent();
		assert _selectedParent != null : _selected;
		// Perspective selectedParent = selected != null ? selected.getParent()
		// : null;
		ensureCounts(_selectedParent);
		selected = _selected;
		// }
		// Util.print(" PL.init return ");
	}

	private void ensureCounts(Perspective _selectedParent) {
		if (selected == null || selected.getParent() != _selectedParent) {
			// if (_selectedParent != selectedParent) {
			// art.summary.addChild(this);
			// art.selectedItem.setChildrenPickable(false);
			moveToFront();
			// sortField = SortButton.SORT_BY_NATURAL_ORDER;
			// sortDirection = 1;

			longestNamedFacet = null;
			maxCount = 0;
			counts = new int[_selectedParent.nChildren()];
			countSortedFacets = null;
			boolean isRestricted = _selectedParent.isRestricted();
			ResultSet rs = isRestricted ? query().getCountsIgnoringFacet(
					_selectedParent) : null;
			if (rs == null) {
				for (Iterator<Perspective> it = _selectedParent.getChildIterator(); it
						.hasNext();) {
					Perspective v = it.next();
					int i = v.whichChild();

					// If query is invalid, we'll recount soon. In the mean
					// time,
					// make all counts non-zero so arrows will still function.
					// If isRestricted but rs == null, must have no other
					// restrictions, and need to use totalCount.
					counts[i] = isRestricted ? v.getTotalCount() : v
							.guessOnCount();

					// Util.print("no rs "+v+" "+counts[i]+" "+isRestricted+" "+query().isQueryValid());

					if (counts[i] > maxCount)
						maxCount = counts[i];
					updateLongestNamedFacet(v);
				}
			} else {
				try {
					while (rs.next()) {
						Perspective v = query().findPerspective(rs.getInt(1));
						assert v.getParent() == _selectedParent;
						int i = v.whichChild();
						int count = rs.getInt(2);
						assert count <= v.getTotalCount() : count + " " + v;
						counts[i] = count;

						// Util.print("rs " + v + " " + count);

						if (counts[i] > maxCount)
							maxCount = counts[i];
						updateLongestNamedFacet(v);
					}
				} catch (Throwable se) {
					Util.err("SQL Exception in PerspectiveList.init: "
							+ se.getMessage());
					se.printStackTrace();
				} finally {
					query().close(rs);
				}
			}
			label.setText(Util.addCommas(size()) + " "
					+ _selectedParent.getName() + " tags");
		}
	}

	double longestNameLength() {
		// Util.print(longestNamedFacet + " yy "
		// + art.getFacetStringWidth(longestNamedFacet, true, false));
		return art.getFacetStringWidth(longestNamedFacet, true, false);
	}

	void updateLongestNamedFacet(Perspective v) {
		if (v.getTotalCount() > 0) {
			// might be <= 0 if isRestrictedData
			if (longestNamedFacet == null) {
				longestNamedFacet = v;
			} else if (w < maxWidth()) {
				String name = v.getNameIfPossible();
				if (name != null
						&& art.getFacetStringWidth(v, true, false) > longestNameLength()) {
					longestNamedFacet = v;
				}
			}
			// Util.print("checkName " + v + " " + longestName + " " + w + " "
			// + art.getFacetStringWidth(v, false, false));
		}
	}

	void updateData() {
		if (!isHidden()) {
			init(selected);
			validate(); // in case offset needs to change due to change in
			// displayed
			// pv's
		}
	}

	Perspective handleArrowForAllPVs(int key, int modifiers) {
		boolean isControlA = key == java.awt.event.KeyEvent.VK_A
				&& Util.isControlDown(modifiers);
		if (isControlA) {
			key = java.awt.event.KeyEvent.VK_END;
			modifiers = InputEvent.SHIFT_DOWN_MASK;
		}
		boolean selectInitialFacet = isControlA;
		boolean continueToEnd = continueToEnd(key);
		int di = delta(key);

		PerspectiveViz selectedPV = art.summary.lookupPV(selected.getParent());
		PerspectiveViz[] ps = selectedPV.rank.perspectives;
		PerspectiveViz pv = isControlA ? ps[0] : selectedPV;
		Perspective finalSelection = null;
		Util.print("PerspectiveList.handleArrowForAllPVs " + key + " "
				+ selected + " " + selectInitialFacet + " " + continueToEnd
				+ " " + di);
		Util.print(Util.member(ps, pv) + " " + ps.length + " " + ps[0] + " "
				+ pv);
		int whichChild = getSortIndex(selected);
		for (int i = Util.member(ps, pv); i >= 0 && i < ps.length
				&& (finalSelection == null || continueToEnd); i += di) {
			pv = ps[i];
			Perspective p = pv.p;
			ensureCounts(p);
			selected = p.getNthChild(0);// Make sure pvp() works.

			if (selectInitialFacet) {
				whichChild = firstChangedFacet(finalSelection, p, di, modifiers);
				finalSelection = getFacet(whichChild);
			}
			if (continueToEnd && size() > 1) {
				whichChild = firstChangedFacet(finalSelection, p, -di,
						modifiers);
				finalSelection = getFacet(whichChild);
			} else {
				for (int index = whichChild + di; index >= 0 && index < size()
						&& finalSelection == null; index += di) {
					finalSelection = handleArrow1(finalSelection, index, false,
							modifiers);
				}
			}
			selectInitialFacet = true;
		}
		if (finalSelection != null) {
			art.toggleFacet(finalSelection, modifiers);
			art.printUserAction(Bungee.FACET_ARROW, selected.getServerID(),
					modifiers);
			if (!isHidden())
				showList(selected);
			art.setClickDesc((Markup) null);
		} else {
			art
					.setTip("No "
							+ pvp().getName()
							+ " tags with non-zero count found in response to navigation keypress.");
		}

		return finalSelection;
	}

	private int firstChangedFacet(Perspective finalSelection,
			Perspective parent, int direction, int modifiers) {
		int whichChild = direction > 0 ? 0 : size() - 1;
		Util.print("firstChangedFacet " + direction + " " + modifiers + " "
				+ whichChild);
		// Need to update query before computing whichChild if isShiftDown.
		if (Util.isShiftDown(modifiers) && parent.isRestricted()) {
			if (finalSelection != null)
				art.toggleFacet(finalSelection, modifiers);
			Perspective facet;
			while (parent.isRestriction(facet = getFacet(whichChild), parent
					.isRequireAction(facet, modifiers))
					&& (direction < 0 ? whichChild > 0
							: whichChild < size() - 1)) {
				whichChild += direction;
			}
		}
		Util.print("   " + whichChild);
		return whichChild;
	}

	private Perspective handleArrow1(Perspective finalSelection,
			int whichChild, boolean zeroCountOK, int modifiers) {
		Util.print("handleArrow1 " + whichChild + " " + zeroCountOK + " "
				+ modifiers + " " + counts[whichChild] + " " + finalSelection);
		if (zeroCountOK || counts[whichChild] > 0) {
			if (finalSelection != null) {
				query().toggleFacet(finalSelection, modifiers);
			}
			finalSelection = getFacet(whichChild);
		}
		return finalSelection;
	}

	private static boolean continueToEnd(int key) {
		int[] keys = { java.awt.event.KeyEvent.VK_HOME,
				java.awt.event.KeyEvent.VK_END };
		return Util.isMember(keys, key);
	}

	private static int delta(int key) {
		int delta = 0;
		switch (key) {
		case java.awt.event.KeyEvent.VK_KP_LEFT:
		case java.awt.event.KeyEvent.VK_LEFT:
		case java.awt.event.KeyEvent.VK_KP_UP:
		case java.awt.event.KeyEvent.VK_UP:
		case java.awt.event.KeyEvent.VK_HOME:
			delta = -1;
			break;

		case java.awt.event.KeyEvent.VK_KP_RIGHT:
		case java.awt.event.KeyEvent.VK_RIGHT:
		case java.awt.event.KeyEvent.VK_KP_DOWN:
		case java.awt.event.KeyEvent.VK_DOWN:
		case java.awt.event.KeyEvent.VK_END:
			delta = 1;
			break;
		}
		return delta;
	}

	/**
	 * @param key
	 *            an arrow key, HOME, END, or c-A
	 * @param modifiers
	 *            control keys pressed along with key
	 * @return the newly-selected Perspective, or null if there is no change.
	 *         (Non-null leads to the input event being marked as handled.)
	 */
	Perspective handleArrowOLD(int key, int modifiers) {
		Util.print("PerspectiveList.handleArrow " + key + " " + modifiers + " "
				+ selected);
		Perspective parent = selected.getParent();
		if (!(parent.isRestriction() || parent.getParent() == null)) {
			assert false;
			// if parent has only negative filters, its grandparent might be
			// unrestricted. If we add positive filters on parent, onItemsQuery
			// won't notice. Need to think about proper behavior for arrows in
			// this case. Should they change to a new negated filter?
			// For now, just disallow.
			return null;
		}
		int sortIndex = getSortIndex(selected);
		int delta = 0;
		boolean zeroCountOK = false;
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
			zeroCountOK = true;
			break;
		case java.awt.event.KeyEvent.VK_END:
			delta = size() - 1 - sortIndex;
			zeroCountOK = true;
			break;
		case java.awt.event.KeyEvent.VK_A:
			if (Util.isControlDown(modifiers)) {
				sortIndex = 0;
				Perspective _selected = getFacet(0);
				delta = size() - 1;
				if (!_selected.isRestriction(true)) {
					if (delta > 0) {
						// Don't tell Bungee, which would trigger an extra DB
						// call.
						query().toggleFacet(_selected, 0);
					} else {
						art.toggleFacet(_selected, 0);
					}
				}
				modifiers = InputEvent.SHIFT_DOWN_MASK;
				zeroCountOK = true;
			}
			break;
		}
		if (delta != 0) {
			boolean nowhereToGo = true;
			// Iterate until you find the first non-zero count perspective
			for (int index = sortIndex + delta; index >= 0 && index < size(); index += delta) {
				Perspective p = getFacet(index);
				// Util.print("handleArrow " + selected + " " + sortIndex + " "
				// + delta + " " + index + " " + p + " "
				// + counts[p.whichChild()]);
				if (counts[p.whichChild()] > 0 || zeroCountOK) {
					selected = p;
					// art.toggleFacet(selected, modifiers);
					art.setClickDesc((Markup) null);
					nowhereToGo = false;
					break;
				}
			}
			if (nowhereToGo) {
				art
						.setTip("No "
								+ selected.getParent().getName()
								+ " tags with non-zero count found in response to navigation keypress.");
			} else {
				art.printUserAction(Bungee.FACET_ARROW, selected.getServerID(),
						modifiers);
				if (!isHidden())
					showList(selected);
				return selected;
			}
		}
		return null;
	}

	Perspective handleArrow(int delta, boolean continuetoEnd, int modifiers) {
		Util.print("PerspectiveList.handleArrow " + delta + " " + modifiers
				+ " " + selected);
		if (!(selected.getParent().isRestriction() || selected.getParent()
				.getParent() == null)) {
			assert false;
			// if parent has only negative filters, its grandparent might be
			// unrestricted. If we add positive filters on parent, onItemsQuery
			// won't notice. Need to think about proper behavior for arrows in
			// this case. Should they change to a new negated filter?
			// For now, just disallow.
			return null;
		}
		assert delta != 0;
		int sortIndex = getSortIndex(selected);
		if (continuetoEnd)
			sortIndex = delta < 0 ? -sortIndex : size() - 1 - sortIndex;
		// Iterate until you find the first non-zero count perspective
		for (int index = sortIndex + delta; index >= 0 && index < size(); index += delta) {
			Perspective p = getFacet(index);
			// Util.print("handleArrow " + selected + " " + sortIndex + " "
			// + delta + " " + index + " " + p + " "
			// + counts[p.whichChild()]);
			if (counts[p.whichChild()] > 0 || continuetoEnd) {
				selected = p;
				// art.toggleFacet(selected, modifiers);
				return p;
			}
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

	private boolean isScrollBar() {
		return nInvisibleValues() > 0;
	}

	private int lineOffset() {
		if (isScrollBar())
			return (int) (scrollbar.getPos() * nInvisibleValues() + 0.5);
		else
			return 0;
	}

	void showList(int lineOffset) {
		// Util.print("PL.showList " + lineOffset);
		assert !isHidden();
		if (size() != selected.nSiblings()) {
			// In case we've added or removed facets
			removeFromParent();
			return;
		}
		moveToFront();
		removeAllChildren();
		double numW = art.numWidth(maxCount) + margin;
		double y = validateSortButtons(numW);
		double nameW = w - 2 * margin - numW;
		ItemPredicate initialLongName = longestNamedFacet;
		int nLines = nLines();
		int maxLine;
		if (nInvisibleValues() > 0) {
			lineOffset = Util.constrain(lineOffset, 0, size() - nLines);
			if (lineOffset != (int) (scrollbar.getPos() * nInvisibleValues() + 0.5)) {
				scrollbar.setPos(lineOffset / (double) nInvisibleValues());
				// setPos will have called us recursively, so our work is done.
				return;
			}
			nameW -= art.scrollBarWidth + 2 * art.scrollMarginSize;
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

			// redrawer has to be this, in case longestNamedFacet changes
			FacetText pLabel = FacetText.getFacetText(p, art, numW, nameW,
					true, true, counts[p.whichChild()], this, true);

			// We want to paint based on counts ignoring restrictions, so make
			// sure highlighting doesn't fade it.
			// pLabel.setPermanentTextPaint(pLabel.getTextPaint());

			pLabel.dontHideTransients = true;
			addChild(pLabel);
			pLabel.setOffset(margin, y);
			updateLongestNamedFacet(p);

			y += art.lineH;
		}
		if (isScrollBar())
			y = h;
		setBounds(0, 0, w, Math.min(h, y));
		border.setPathToRectangle(0, 0, (float) (w), (float) Math.min(h, y));
		if (longestNamedFacet != initialLongName) {
			showList(lineOffset);
		}
		// Util.print(" PL.showList return");
	}

	@SuppressWarnings("unchecked")
	void highlightFacet() {
		if (!isHidden()) {
			// Util.print("PL.showFacet " + art.highlightedFacets);
			for (Iterator<PNode> it = getChildrenIterator(); it.hasNext();) {
				PNode n = it.next();
				if (n instanceof FacetText) {
					FacetText f = (FacetText) n;
					f.setColor();
				}
			}
		}
	}

	// public final class SortButton extends BungeeTextButton {
	//
	static final int SORT_BY_SELECTION = 1;

	static final int SORT_BY_ON_COUNT = 2;

	static final int SORT_BY_TOTAL_COUNT = 3;

	static final int SORT_BY_NATURAL_ORDER = 4;

	//
	// private static final String z_aLabel = "\u2193";
	//
	// private static final String a_zLabel = "\u2191";
	//
	// private static final String noneLabel = " ";
	//
	// int order = SORT_BY_NATURAL_ORDER;
	//
	// // boolean reverseOrder = false;
	//
	// // PerspectiveList parent;
	//
	// SortButton(int field) {
	// // super(field == SORT_BY_NATURAL_ORDER ? a_zLabel : noneLabel,
	// // art.font, 0, 0, -1, -1, null, 1.8f, Color.darkGray, Color
	// // .getHSBColor(0, 0, 0.1f));
	// super(field == SORT_BY_NATURAL_ORDER ? a_zLabel : noneLabel,
	// Color.gray, Color.getHSBColor(0, 0, 0.15f),
	// PerspectiveList.this.art);
	// mouseDoc = "Sort by this column";
	// // parent = _parent;
	// order = field;
	// }
	//
	// void setOrder(int direction) {
	// String s = null;
	// switch (direction) {
	// case 1:
	// s = a_zLabel;
	// break;
	// case 0:
	// s = noneLabel;
	// break;
	// case -1:
	// s = z_aLabel;
	// break;
	// default:
	// assert false : direction;
	// }
	// setText(s);
	// }
	//
	// int getDirection() {
	// int result = 0;
	// String s = getText();
	// if (s.equals(a_zLabel))
	// result = 1;
	// else if (s.equals(noneLabel))
	// result = 0;
	// else if (s.equals(z_aLabel))
	// result = -1;
	// else
	// assert false : s;
	// return result;
	// }
	//
	// public void doPick() {
	// PerspectiveList.this.setOrder(order, getDirection());
	// }
	//
	// // public void mayHideTransients(PNode node) {
	// // parent.mayHideTransients();
	// // }
	//
	// }

	public void setOrder(int order, int direction) {
		// Util.print("PerspectiveList.setOrder " + order + " " + direction);
		sortField = order;
		sortDirection = direction;
		selected = getFacet(0);
		redraw();
	}

	// void setButtonOrder() {
	// sortBySelection
	// .setOrder(sortField == SortButton.SORT_BY_SELECTION ? sortDirection
	// : 0);
	// sortByOnCount
	// .setOrder(sortField == SortButton.SORT_BY_ON_COUNT ? sortDirection
	// : 0);
	// sortByNatural
	// .setOrder(sortField == SortButton.SORT_BY_NATURAL_ORDER ? sortDirection
	// : 0);
	// }

	int getSortIndex(Perspective facet) {
		int result = -1;
		switch (sortField) {
		case SORT_BY_NATURAL_ORDER:
			result = facet.whichChild();
			break;
		case SORT_BY_ON_COUNT:
			for (int i = 0; i < size() && result < 0; i++) {
				ItemPredicate child = countSortedFacets()[i];
				if (child == facet)
					result = i;
			}
			break;
		case SORT_BY_TOTAL_COUNT:
			assert false;
		case SORT_BY_SELECTION:
			result = facet.whichChild();
			int indexOffset = 0;
			SortedSet<Perspective> restrictions = pvp().allRestrictions();
			for (Perspective child: restrictions) {
				if (child.whichChild() > result)
					indexOffset++;
			}
			result += indexOffset;
			break;
		default:
			assert false : sortField;
			break;
		}
		if ((sortDirection == -1) == (sortField == SORT_BY_NATURAL_ORDER))
			result = size() - result - 1;
		assert result >= 0 && result < size() : facet + " " + result + " "
				+ size();
		return result;
	}

	Perspective getFacet(int sortIndex) {
		assert sortIndex < counts.length : selected;
		Perspective result = null;
		Perspective parent = pvp();
		if ((sortDirection == -1) == (sortField == SORT_BY_NATURAL_ORDER))
			sortIndex = size() - sortIndex - 1;
		switch (sortField) {
		case SORT_BY_NATURAL_ORDER:
			result = parent.getNthChild(sortIndex);
			break;
		case SORT_BY_ON_COUNT:
			result = countSortedFacets()[sortIndex];
			break;
		case SORT_BY_TOTAL_COUNT:
			assert false;
		case SORT_BY_SELECTION:
			List<Perspective> restrictions = new ArrayList<Perspective>(parent.allRestrictions());
			int nRestrictions = restrictions.size();
			if (nRestrictions > 0) {
				// if (nRestrictions > 1) {
				// Perspective[] temp = new Perspective[nRestrictions];
				// System.arraycopy(restrictions, 0, temp, 0, nRestrictions);
				// restrictions = temp;
				// Arrays.sort(restrictions);
				// }
				if (sortIndex < nRestrictions) {
					result = restrictions.get(nRestrictions
							- sortIndex - 1);
				} else {
					for (int i = 0; i < nRestrictions && result == null; i++) {
						Perspective child = restrictions
								.get(nRestrictions - i - 1);
						int nSkipped = child.whichChild() - i;
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

	Perspective[] countSortedFacets() {
		if (countSortedFacets == null) {
			countSortedFacets = pvp().getChildren();
			Arrays.sort(countSortedFacets, new OnCountComparator());
		}
		return countSortedFacets;
	}

	final class OnCountComparator extends IntValueComparator<Perspective> {

		@Override
		public int value(Perspective data) {
			return counts[data.whichChild()];
		}
	}

	Query query() {
		return art.query;
	}

	public SortButton[] getSortButtons() {
		SortButton[] result = { sortByNatural, sortByOnCount, sortBySelection };
		return result;
	}
}
