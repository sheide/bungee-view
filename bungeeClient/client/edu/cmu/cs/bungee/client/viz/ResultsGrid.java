/* 

 Created on Mar 4, 2005

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

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.threads.UpdateNoArgsThread;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.AbstractMenuItem;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.VScrollbar;
import edu.umd.cs.piccolo.PNode;

final class ResultsGrid extends LazyContainer implements MouseDoc {

	// int desiredCols = -1;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Number of empty pixels to leave around thumbnails. gridW = edgeW + 2 *
	 * THUMB_BORDER
	 */
	static int THUMB_BORDER = 2;

	/**
	 * Space to the left and right of this ResultsGrid. Redundant with
	 * art.regionMargins
	 */
	// Now hardcoded into margin_size()
	// int margin_size = 40;
	/**
	 * Maps from item record_nums to a GridImage (a kind of PImage)
	 */
	transient SoftReference thumbs;

	VScrollbar gridScrollBar;

	Bungee art;

	int nRows;

	int nCols; // nRows * nCols >= onCount (= if the last col is full)

	int gridW;

	int gridH;

	int nVisibleRows;

	int visRowOffset;

	private APText label;

	Thumbnails thumbnails;

	Item selectedItem = null;

	/**
	 * This changes when: 1. User clicks, scrolls, or presses arrow keys [via
	 * computeSelectedItemFromSelectedOffset] 2. Item is filtered out.
	 */
	int selectedItemOffset = -1;

	private int edgeW;

	private int edgeH;

	private boolean mustSetItem;

	int onCount;

	RangeEnsurer rangeEnsurer;

	// private APText countLabel;

	private LazyPPath outline;

	Boundary boundary;

	Menu sortMenu;

	private APText sortLabel;

	// PInterpolatingActivity highlightAnimator;

	private double minWidth;

	boolean onItemsInvalid;

	ResultsGrid(Bungee a) {
		art = a;
		setPickable(false);
	}

	void init() {
		assert rangeEnsurer == null : "init called twice!!";
		rangeEnsurer = new RangeEnsurer(this);
		rangeEnsurer.start();
		// setPaint(Color.black); // hide initially

		label = art.oneLineLabel();
		label.scale(2.0);
		label.setTextPaint(Bungee.gridFG);
		label.setPickable(false);
		label.setText("Matching " + art.query.getGenericObjectLabel(true));

		// countLabel = art.oneLineLabel();
		// countLabel.setTextPaint(Bungee.gridFG);
		// countLabel.setPickable(false);

		// percentLabel = art.oneLineLabel();
		// percentLabel.setTextPaint(Bungee.gridFG);
		// percentLabel.setPickable(false);

		thumbnails = new Thumbnails();

		outline = new LazyPPath();
		outline.setVisible(false);
		outline.setPickable(false);
		outline.setStroke(LazyPPath.getStrokeInstance(5));
		outline.setStrokePaint(Bungee.selectedItemOutlineColor);
		outline.setPathToRectangle(0, 0, 1, 1);

		Runnable scroll = new Runnable() {

			public void run() {
				int rowOffset = (int) (gridScrollBar.getPos()
						* (nRows - nVisibleRows) + 0.5);
				// Util.print("RG.scroll rowOffset = " + rowOffset);
				if (rowOffset != visRowOffset) {
					int selectedRow = selectedItemOffset / nCols;
					int selectedCol = selectedItemOffset % nCols;
					int newSelectedRow = Util.constrain(selectedRow, rowOffset,
							rowOffset + nVisibleRows - 1);
					int newSelectedItemOffset = newSelectedRow * nCols
							+ selectedCol;
					// Util.print("newSelectedItemOffset " +
					// newSelectedItemOffset);
					computeSelectedItemFromSelectedOffset(
							newSelectedItemOffset, rowOffset);
					// art.printUserAction(Art.SCROLL, "grid", selectedItem);
				}
			}
		};

		// Use a placeholder for height, which is required to be greater than 3
		// times the width
		gridScrollBar = new VScrollbar(art.scrollBarWidth,
				4 * art.scrollBarWidth, Bungee.gridScrollBG,
				Bungee.gridScrollFG, scroll);

		boundary = new Boundary(this, false);
		addChild(boundary);

		sortLabel = art.oneLineLabel();
		sortLabel.setTextPaint(Bungee.gridFG.darker());
		sortLabel.setText("Sorted by:");
		sortLabel.setVisible(false);
		// sortLabel.setScale(0.8);
		if (art.getShowSortMenu())
			addChild(sortLabel);

		// Runnable doReorder = new Runnable() {
		// public void run() {
		// reorder(((Integer) sortMenu.getData()).intValue());
		// }
		// };

		sortMenu = new Menu(Bungee.gridBG, Bungee.gridFG.darker(), art.font);
		sortMenu.mouseDoc = "Choose how Matches are Sorted";
		sortMenu.addButton(new ReorderCommand("Random",
				"Show thumbnails in random order", -1));
		sortMenu.addButton(new ReorderCommand("ID",
				"Order thumbnails by their database ID", 0));
		for (int i = 1; i <= query().nAttributes; i++) {
			ItemPredicate facetType = query().findPerspective(i);
			sortMenu.addButton(new ReorderCommand(facetType.getName(),
					"Order thumbnails by this category of tags ", i));
		}
		sortMenu.setVisible(false);
		// sortMenu.setScale(0.8);
		if (art.getShowSortMenu())
			addChild(sortMenu);

		validate(w, h);
	}

	private class ReorderCommand extends AbstractMenuItem {

		int facetType;

		public ReorderCommand(String label, String mouseDoc, int facetType) {
			super(label, mouseDoc);
			this.facetType = facetType;
		}

		public String doCommand() {
			reorder(facetType);
			return getLabel();
		}
	}

	void setFeatures() {
		if (sortLabel != null) {
			if (art.getShowSortMenu()) {
				addChild(sortLabel);
				addChild(sortMenu);
			} else {
				sortLabel.removeFromParent();
				sortMenu.removeFromParent();
			}
		}
	}

	int margin_size() {
		return art.getShowCheckboxes() ? 8 : 40;
	}

	boolean isInitted() {
		return label != null;
	}

	void stop() {
		if (rangeEnsurer != null) {
			rangeEnsurer.exit();
			rangeEnsurer = null;
		}
	}

	double getTopMargin() {
		// return percentLabel.getMaxY() + art.lineH / 2;
		return art.summary.getTopMargin();
	}

	double getBottomMargin() {
		if (art.getShowSortMenu())
			return h - sortLabel.getYOffset();
		else
			return 0;
	}

	// void validate(double _w, double _h, double _minW, double _maxW) {
	// maxW = _maxW;
	// minW = _minW;
	// validate(_w, _h);
	// }

	void validate(double _w, double _h) {
		w = _w;
		h = _h;
		// setBounds(0, 0, w, h);
		if (isInitted()) {
			boundary.margin = -margin_size() / 2;
			assert w >= minWidth();
			label.setOffset(margin_size(), 0.0);
			label.setFont(art.font);
			// label.setHeight(art.lineH);

			// countLabel.setOffset(margin_size(), label.getMaxY());
			// countLabel.setFont(art.font);

			// percentLabel.setOffset(margin_size(), countLabel.getMaxY() + 4);
			// percentLabel.setFont(art.font);

			thumbnails.setOffset(margin_size(), getTopMargin());
			gridScrollBar.setOffset(w - margin_size() - art.scrollBarWidth,
					getTopMargin());

			sortLabel.setFont(art.font);
			sortMenu.setFont(art.font);

			// Match y of QueryViz text search label
			double sortY = h - (art.lineH + 1) * 1.5;

			if (sortLabel.getWidth() + sortMenu.getWidth() + margin_size() <= w) {
				sortLabel.setOffset(margin_size(), sortY);
				sortMenu.setOffset(sortLabel.getMaxX(), sortY);
			} else {
				sortMenu.setOffset(margin_size(), sortY);
				sortY -= sortLabel.getHeight() * sortLabel.getScale();
				sortLabel.setOffset(margin_size(), sortY);
			}

			// boundary.setMinX(minW);
			// boundary.setMaxX(maxW);
			boundary.validate();

			if (onCount > 0)
				dataUpdated();
		}
	}

	public void updateBoundary(Boundary boundary1) {
		if (art.getShowBoundaries()) {
			assert boundary1 == boundary;
			validate(boundary1.center(), h);
			art.updateGridBoundary();
		}
	}

	public void enterBoundary(Boundary boundary1) {
		// Util.print("grid enter boundary");
		if (!art.getShowBoundaries()) {
			boundary1.exit();
		}
	}

	void computeMinWidth() {
		minWidth = art.getStringWidth("Matching "
				+ art.query.getGenericObjectLabel(true))
				* 2 + 2 * margin_size();
		// Util.print("Grid.computeMinWidth " + minWidth);
	}

	public double minWidth() {
		// Util.print("minWidth " + minWidth);
		// if (minWidth <= 0)
		computeMinWidth();
		return minWidth;
	}

	public double maxWidth() {
		return w + art.selectedItem.w - art.selectedItem.minWidth();
	}

	void dataUpdated() {
		// Util.print("Grid.dataUpdated " + query().getOnCount());
		onItemsInvalid = false;
		onCount = query().getOnCount();
		// setDescription();
		offsetItemTableRangesIndex = 0;
		if (onCount > 0) {
			thumbnails.removeAllChildren();
			computeEdge();
			selectedItemOffset = -1;
			// Util.print("^^^ dataUpdated calling doEnsureRange");
			doEnsureRange();
			// Util.print("^^^ dataUpdated returning doEnsureRange");
		} else {
			gridScrollBar.setVisible(false);
			drawGrid();
		}
	}

	/**
	 * As a side effect, sets nCols, gridW, nVisibleRows, nRows
	 * 
	 * @param count
	 * @return the number of thumbnails to display, assuming onCount is at least
	 *         that high
	 */
	int maxThumbs(int count) {
		double usableH = h - getTopMargin() - getBottomMargin();
		double usableW = usableW();
		int desiredColumns = getDesiredColumns();
		for (nCols = 1;; nCols++) {
			gridW = (int) (usableW / nCols);
			nVisibleRows = (int) (usableH / gridW);
			if (nCols * nVisibleRows >= count || nCols >= desiredColumns)
				break;
		}
		maybeAdjustGrid(usableH);
		nRows = Math.max(1, (int) Math.ceil(count / (double) nCols));
		// Util.print("maxThumbs " + nCols + " " + nRows + " " + nVisibleRows);
		if (nRows <= nVisibleRows) {
			usableW += art.scrollBarWidth + art.scrollMarginSize;
			gridW = (int) (usableW / nCols);
		}
		nVisibleRows = Math.min(nVisibleRows, nRows);
		// Util.print("maxThumbs " + usableW);
		return nVisibleRows * nCols;
	}

	double usableW() {
		return w - art.scrollBarWidth - art.scrollMarginSize - 2
				* margin_size();
	}

	// Fit to height instead of width if it wastes less space.
	void maybeAdjustGrid(double usableH) {
		if (onCount > nVisibleRows * nCols) {
			double wastedRowPercent = usableH / gridW - nVisibleRows;
			if (wastedRowPercent > Math.sqrt(2) - 1) {
				nVisibleRows++;
			}
		}
		gridH = (int) (usableH / nVisibleRows);
	}

	void computeEdge() {
		maxThumbs(onCount);
		edgeW = gridW - 2 * THUMB_BORDER;
		edgeW = Math.max(1, edgeW);
		edgeH = gridH - 2 * THUMB_BORDER;
		edgeH = Math.max(1, edgeH);
		if (gridScrollBar != null) {
			gridScrollBar.setH(gridH * nVisibleRows);
			gridScrollBar.setBufferPercent(nVisibleRows, nRows);
		}
		// Util.print("computeEdge " + edgeW + "x" + edgeH);
		// thumbnails.updateBoundaries(nCols, nVisibleRows, grid);
	}

	void setDesiredCols(int newNcols) {
		// Util.print("setDesiredCols " + newNcols);
		if (newNcols != art.getDesiredNumResultsColumns()) {
			art.setDesiredNumResultsColumns(newNcols);
			computeEdge();
			art.setDesiredNumResultsColumns(nCols);
			// desiredCols = nCols; // Would be confusing to change the desire
			// if the user doesn't see a change.
			visRowOffset = -9999;
			computeSelectedItemFromSelectedOffset(selectedItemOffset, -1);
		}
	}

	int getDesiredColumns() {
		int result;
		if (art.getDesiredNumResultsColumns() > 0)
			result = art.getDesiredNumResultsColumns();
		else {
			// Make thumbnails grow from the minimum size as the square root of
			// the extra space
			result = (int) Math.round(Math.max(1, Math.pow(usableW()
					/ Bungee.MIN_THUMB_SIZE, 0.8)));
		}
		// Util.print("getDesiredColumns " + result);
		return result;
	}

	void chooseReorder(int facetType) {
		sortMenu.choose(facetType + 1);
	}

	void reorder(int facetType) {
		// art.handleCursor(true);
		art.printUserAction(Bungee.REORDER, facetType, 0);
		query().reorderItems(facetType);
		// offsetItemTableRangesIndex = 0;
		// doEnsureRange();
		dataUpdated();
		// art.handleCursor(false);
	}

	void doEnsureRange() {
		// schedule a ensureRange, and then doRedraw
		rangeEnsurer.update();
	}

	/**
	 * Called in thread rangeEnsurer
	 */
	void ensureRange() {
		if (!onItemsInvalid) {
			synchronized (offsetItemTableRanges) {
				// Util.print("ensureRange " + selectedItem + " "
				// + selectedItemOffset + " " + onCount);
				// If selectedItem is still in onItems, return it's new offset;
				// otherwise, set offset to zero.
				if (selectedItem != null && selectedItemOffset < 0) {
					// This should be pre-computed - no need to talk to server.
					selectedItemOffset = query().itemIndex(selectedItem,
					// add an extra 1 to catch the case of paging down.
							(nVisibleRows + 1) * nCols);
					// Util.print("itemIndex = " + selectedItemOffset);
				}
				if (selectedItemOffset < 0) {
					// Need new selectedItem
					mustSetItem = true;
					selectedItemOffset = 0;
				}

				visRowOffset = visibleRowOffset(visRowOffset,
						selectedItemOffset);
				int originalMinOffset = visRowOffset * nCols;
				int originalMaxOffset = (visRowOffset + nVisibleRows) * nCols;
				originalMaxOffset = Math.min(onCount, originalMaxOffset);

				if (!isInRange(originalMinOffset, originalMaxOffset)) {
					// Util.print("ensureRange " + offsetItemTableRangesIndex +
					// " "
					// + minOffset + " " + maxOffset);
					if (offsetItemTable == null) {
						// assert offsetItemTableRangesIndex == 0;
						offsetItemTable = new Item[query().getTotalCount()];
						// if (offsetItemTable != null)
						// System.arraycopy(offsetItemTable, 0,
						// tempOffsetItemTable,
						// 0, offsetItemTable.length);
						// offsetItemTable = tempOffsetItemTable;
					}

					int minRange = -1;
					int maxRange = -1;
					int minOffset = originalMinOffset;
					int maxOffset = originalMaxOffset;
					for (int i = 0; i < offsetItemTableRangesIndex; i += 2) {
						int low = offsetItemTableRanges[i];
						int hi = offsetItemTableRanges[i + 1];
						if (low >= minOffset && hi <= maxOffset) {
							minRange = i;
							offsetItemTableRanges[i + 1] = maxOffset;
						} else if (low <= minOffset && hi >= minOffset) {
							minRange = i;
							minOffset = hi;
							offsetItemTableRanges[i + 1] = maxOffset;
						} else if (low <= maxOffset && low >= minOffset) {
							maxRange = i;
							maxOffset = low;
							offsetItemTableRanges[i] = minOffset;
						}
					}
					if (minRange >= 0 && maxRange >= 0) {
						offsetItemTableRanges[minRange + 1] = offsetItemTableRanges[maxRange + 1];
						int[] newRanges = null;
						if (maxRange > 0) {
							newRanges = Util.subArray(offsetItemTableRanges, 0,
									maxRange - 1);
							if (maxRange < offsetItemTableRanges.length - 2)
								newRanges = Util
										.append(
												newRanges,
												Util
														.subArray(
																offsetItemTableRanges,
																maxRange + 2,
																offsetItemTableRanges.length - 1));
						} else
							newRanges = Util.subArray(offsetItemTableRanges, 2,
									offsetItemTableRanges.length - 1);
						offsetItemTableRangesIndex -= 2;
						offsetItemTableRanges = newRanges;
					} else if (minRange < 0 && maxRange < 0) {
						offsetItemTableRanges = Util.push(Util.push(
								offsetItemTableRanges, maxOffset), minOffset);
						offsetItemTableRangesIndex += 2;
					}
					ensureRangeInternal(minOffset, maxOffset);
					selectedItem = getItem(selectedItemOffset);
					if (selectedItem != null) {
						// // results.art.waitForValidQuery();
						javax.swing.SwingUtilities.invokeLater(getDoRedraw());
						// if (toLoad != null) {
						// loadThumbs(toLoad);
						// javax.swing.SwingUtilities.invokeLater(getDoRedraw());
					}
					// }
				}
				checkCached(originalMinOffset, originalMaxOffset);
				// Util.print("ensureRange return");
			}
		}
	}

	// checks whether items need ItemImages (which it calls loadThumbs to load
	// from db) or GridImages
	// (which it creates immediately).
	/**
	 * Called in thread rangeEnsurer
	 */
	void checkCached(int minOffset, int maxOffset) {
		// Util.print("ensureCached " + edge + " " + onCount + " " + minOffset +
		// " " + maxOffset);
		SortedSet toLoad = new TreeSet();
		if (offsetItemTable != null && !onItemsInvalid) {
			assert maxOffset <= offsetItemTable.length && maxOffset <= onCount : onCount
					+ " "
					+ query().getOnCount()
					+ " "
					+ offsetItemTable.length
					+ " "
					+ minOffset
					+ "-"
					+ maxOffset
					+ " "
					+ offsetItemTableRangesIndex
					+ " "
					+ Util.valueOfDeep(offsetItemTableRanges);
			for (int i = minOffset; i < maxOffset; i++) {
				if (isInRange(i, i + 1)) {
					Item item = offsetItemTable[i];
					if (item != null) {
						ItemImage ii = art.lookupItemImage(item);
						// Util.print("offset [" + minOffset + ", " + maxOffset
						// + "] => " + item + " " + (ii != null));

						if (ii == null
								|| !ii.bigEnough(edgeW, edgeH,
										Bungee.ThumbQuality)) {
							assert !toLoad.contains(item);
							toLoad.add(item);
						} else if (lookupThumb(item) == null) {
							// After unrestrict, thumbs gets wiped but
							// itemImages
							// doesn't
							addThumb(item, new GridImage(ii));
						}
					} else {
						assert false : i + " " + minOffset + "-" + maxOffset
								+ " " + offsetItemTableRangesIndex + " "
								+ Util.valueOfDeep(offsetItemTableRanges);
					}
				}
			}
		}
		if (toLoad.size() > 0) {
			loadThumbs(toLoad);
			javax.swing.SwingUtilities.invokeLater(getDoRedraw());
		}
	}

	// ensureRange makes sure that offsetItemTable is updated
	/**
	 * Called in thread rangeEnsurer
	 */
	void ensureRangeInternal(int minOffset, int maxOffset) {
		// Util.print("ensureRangeInternal " + minOffset + "-" + maxOffset);
		if (!onItemsInvalid) {
			java.sql.ResultSet rs = query().offsetItems(minOffset, maxOffset);
			// WARNING: don't change rs row, as it may be purposefullly set to
			// an
			// intermediate row.
			try {
				int i = minOffset;
				while (rs.next() && i < maxOffset) {
					// Util.print(minOffset + " " + rs.getInt(1));
					Item item = Item.ensureItem(rs.getInt(1));
					offsetItemTable[i++] = item;
				}
				// Util.print(" " + minOffset + " " + maxOffset + " "
				// + rs.getRow());
				assert i == maxOffset : "offsetItems returned "
						+ MyResultSet.nRows(rs) + " rows, but "
						+ (maxOffset - minOffset) + " were expected. "
						+ minOffset + "-" + maxOffset + " " + i + " "
						+ rs.getRow();
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				query().close(rs);
			}
		}
		// Util.print("ensureRangeInternal return ");
	}

	/**
	 * Called in thread rangeEnsurer
	 */
	void loadThumbs(SortedSet items) {
		if (items != null) {
			// Util.print("loadThumbs " + edgeW + "x" + edgeH + " " + items);
			ResultSet[] rss = null;
			try {
				rss = query().getThumbs(items, edgeW, edgeH,
						Bungee.ThumbQuality);
				loadThumbsInternal1(rss[0], items);
				loadThumbsInternal2(rss[1]);
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				if (rss != null) {
					query().close(rss[0]);
					// query().close(rss[1]);
				}
			}
		}
		// Util.print("loadThumbs return");
	}

	void loadThumbsInternal1(ResultSet rs, SortedSet items) throws SQLException {
		boolean hasNext = rs.next();
		Item blobItem = hasNext ? Item.ensureItem(rs.getInt(1)) : null;
		for (Iterator it = items.iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			int rawW = 0;
			int rawH = 0;
			InputStream blobStream = null;
			if (hasNext && item == blobItem) {
				blobStream = ((MyResultSet) rs).getInputStream(2);
				rawW = rs.getInt(3);
				rawH = rs.getInt(4);
				// bi = ImageIO.read(blobStream);
				// if (image.getColorModel().getNumColorComponents() <
				// 3)
				// // Workaround for washed out colors when resizing
				// // grayscale images
				// image = PImage.toBufferedImage(image, true);
				hasNext = rs.next();
				if (hasNext)
					blobItem = Item.ensureItem(rs.getInt(1));
			}
			ItemImage ii = art.ensureItemImage(item, rawW, rawH,
					Bungee.ThumbQuality, blobStream);
			// Util.print("addThumb " + edgeW + "*" + edgeH + " " + item
			// + " " + rawW + "*" + rawH);
			addThumb(item, new GridImage(ii)); // art, item,
			// image));
		}
	}

	List pendingItemFacets = new LinkedList();

	void loadThumbsInternal2(ResultSet rs) throws SQLException {
		Query q = query();
		while (rs.next()) {
			Item item = Item.ensureItem(rs.getInt(1));
			ItemImage ii = art.lookupItemImage(item);
			int facetID = rs.getInt(2);
			Perspective facet = q.findPerspectiveIfPossible(facetID);
			if (facet != null && ii != null) {
				ii.facets.add(facet);
				// Util.print("adding " + ii.item + " " + facet);
			} else {
				Object[] pair = { item, new Integer(facetID) };
				pendingItemFacets.add(pair);
			}
		}
	}

	void reprocessPendingItemFacets() {
		// Util.print("reprocessPendingItemFacets");
		Query q = query();
		assert q != null;
		for (Iterator it = pendingItemFacets.iterator(); it.hasNext();) {
			Object[] pair = (Object[]) it.next();
			assert pair != null;
			assert pair[0] != null;
			assert pair[1] != null;
			int facetID = ((Integer) pair[1]).intValue();
			Perspective facet = q.findPerspectiveIfPossible(facetID);
			if (facet != null) {
				ItemImage ii = art.lookupItemImage((Item) pair[0]);
				if (ii != null)
					// may have been decached
					ii.facets.add(facet);
				// Util.print("vv " + ii + " " + facet);
				it.remove();
			} else {
				// Util.print("xx " + entry.getKey() + " " + entry.getValue());

			}
		}
	}

	// Reduced distraction from animation seems negligible or worse.
	void highlightFacet(Set facets) {
		assert Util.ignore(facets);
		if (thumbnails != null) {
			reprocessPendingItemFacets();
			// Util.print("Grid.highlightFacet " + facet);
			for (Iterator it = thumbnails.getChildrenIterator(); it.hasNext();) {
				PNode child = (PNode) it.next();
				if (child instanceof GridImage) {
					GridImage gi = (GridImage) child;
					if (gi.select())
						gi.animateSelection(1f);
				}
			}
		}
	}

	// void highlightFacet(Set facets) {
	// if (thumbnails != null) {
	// reprocessPendingItemFacets();
	// // Util.print("Grid.highlightFacet " + facet);
	// boolean change = false;
	// for (Iterator it = thumbnails.getChildrenIterator(); it.hasNext();) {
	// PNode child = (PNode) it.next();
	// if (child instanceof GridImage) {
	// GridImage gi = (GridImage) child;
	// change = gi.select() || change;
	// }
	// }
	// if (change) {
	// if (highlightAnimator != null)
	// highlightAnimator
	// .terminate(PActivity.TERMINATE_WITHOUT_FINISHING);
	//
	// highlightAnimator = new PInterpolatingActivity(
	// Bungee.dataAnimationMS, Bungee.dataAnimationMS/4) {
	//
	// protected void activityFinished() {
	// super.activityFinished();
	// highlightAnimator = null;
	// }
	//
	// public void setRelativeTargetValue(float zeroToOne) {
	// for (Iterator it = thumbnails.getChildrenIterator(); it
	// .hasNext();) {
	// PNode child = (PNode) it.next();
	// if (child instanceof GridImage) {
	// GridImage gi = (GridImage) child;
	// gi.animateSelection(zeroToOne);
	// }
	// }
	// }
	// };
	// addActivity(highlightAnimator);
	// }
	// }
	// }

	private transient Runnable doRedraw;

	Runnable getDoRedraw() {
		if (doRedraw == null)
			doRedraw = new Runnable() {

				public void run() {
					updateScrollPos();
					drawGrid();
				}
			};
		return doRedraw;
	}

	// Do this if the query changes
	// (updateData), or you scroll/pick/arrow
	// (computeSelectedItemFromSelectedOffset > updateSelectedItemOffset).
	void drawGrid() {
		// Util.print("Enter drawGrid " + onCount);
		if (art.removeInitialHelp()) {
			addChild(label);
			// addChild(countLabel);
			// addChild(percentLabel);
			addChild(gridScrollBar);
			addChild(thumbnails);
		}
		thumbnails.removeAllChildren();
		if (onCount > 0) {
			// Util.print("ResultGrid.drawGrid " + visRowOffset + " "
			// + selectedItem);
			assert visRowOffset == 0 || onCount > nVisibleRows * nCols : visRowOffset;
			int minOffset = visRowOffset * nCols;
			int maxOffset = Math.min(onCount, minOffset + nVisibleRows * nCols);
			int offset = visRowOffset * nCols - 1;
			// int x = 0;
			// int margin = grid - edge;
			// int prevRowOffset = 0;
			// int rowOffset = 0;
			// double usableW = nCols * grid;
			// double imageArea = edge * edge;
			// double scale = 1;
			// boolean even = true;
			// boolean missing = false;
			for (int row = 0; row < nVisibleRows; row++) {
				for (int col = 0; col < nCols; col++) {
					offset++;
					if (offset < maxOffset) {
						Item item = getItem(offset);
						if (item != null) {
							// Check item in case data changes out from
							// under
							// us.
							GridImage image = lookupThumb(item);
							// Util.print("drawGrid " + item);
							if (image != null) {
								image.offset = offset;
								image.scale(edgeW, edgeH);
								// image.setArea(imageArea);
								double iw = image.getImage().getWidth(null);
								double ih = image.getImage().getHeight(null);
								assert iw <= edgeW : iw + " " + edgeW;
								assert ih <= edgeH;
								// if (x + iw > usableW) {
								// if ((x + iw - usableW) / grid < 0.3) {
								// scale = (x + iw) / usableW;
								// } else {
								// if (offset > rowOffset)
								// sortRow(prevRowOffset, rowOffset,
								// offset, even, scale);
								// x = 0;
								// prevRowOffset = rowOffset;
								// rowOffset = offset;
								// even = !even;
								// scale = 1;
								// }
								// if (iw > usableW)
								// scale = iw / usableW;
								// }
								// image.setOffset(x, 0);
								// x += iw + margin;
								double x = col * gridW
										+ (int) ((gridW - iw) / 2);
								double y = row * gridH
										+ (int) ((gridH - ih) / 2);
								image.setOffset(x, y);
								thumbnails.addChild(image);

								// Now that we know the location, art can
								// animate outline.
								if (item == selectedItem) {
									updateSelectionOutline(x, y, iw, ih);
									if (mustSetItem) {
										mustSetItem = false;
										art.setSelectedItem(item, outline,
												false);
									}
								}
								// } else {
								// missing = true;
							}
						}
					}
				}
			}
			// sortRow(prevRowOffset, rowOffset, offset + 1, even, scale);
			thumbnails.addGrid(nCols, nVisibleRows, gridW, gridH);
			thumbnails.addChild(outline);
		}
		// Util.print(" Exit drawGrid");
	}

	// Set one element for use by revUpLoadThumbs.
	Item[] offsetItemTable;

	/**
	 * plist of [min, max> offset ranges for which we've cached corresponding
	 * record_num's in offsetItemTable. (max is not a valid index)
	 */
	private int[] offsetItemTableRanges = {};

	/**
	 * last valid entry in offsetItemTableRanges
	 */
	int offsetItemTableRangesIndex = 0;

	/**
	 * Can be called in thread rangeEnsurer
	 */
	boolean isInRange(int min, int max) {
		if (!onItemsInvalid) {
			for (int i = 0; i < offsetItemTableRangesIndex; i += 2) {
				if (offsetItemTableRanges[i] <= min
						&& offsetItemTableRanges[i + 1] >= max)
					return true;
			}
		}
		// Util.print("not in range: " + min + "-" + max);
		return false;
	}

	private Item getItem(int offset) {
		// Util.print("getItem " + offset + " " + isInRange(offset, offset) + "
		// "
		// + offsetItemTable[offset]);
		assert offset < onCount : offset + " " + onCount;
		Item result = null;
		if (isInRange(offset, offset + 1)) {
			result = offsetItemTable[offset];
		}
		return result;
	}

	void handleArrow(int key) {
		// Util.print("ResultGrid.handleArrow " + key);
		int offset = selectedItemOffset;
		switch (key) {
		case java.awt.event.KeyEvent.VK_KP_DOWN:
		case java.awt.event.KeyEvent.VK_DOWN:
			offset += nCols;
			break;
		case java.awt.event.KeyEvent.VK_KP_UP:
		case java.awt.event.KeyEvent.VK_UP:
			offset -= nCols;
			break;
		case java.awt.event.KeyEvent.VK_KP_LEFT:
		case java.awt.event.KeyEvent.VK_LEFT:
			offset--;
			break;
		case java.awt.event.KeyEvent.VK_KP_RIGHT:
		case java.awt.event.KeyEvent.VK_RIGHT:
			offset++;
			break;
		case java.awt.event.KeyEvent.VK_HOME:
			offset = 0;
			break;
		case java.awt.event.KeyEvent.VK_END:
			offset = onCount;
			break;
		case java.awt.event.KeyEvent.VK_A:
			return;
		default:
			assert false : key;
		}
		computeSelectedItemFromSelectedOffset(offset, -1);
		if (selectedItem != null)
			// If we went off the screen, and have to call ensureRange, won't
			// know selectedItem yet.
			art.printUserAction(Bungee.GRID_ARROW, selectedItem.getId(), 0);
	}

	/**
	 * Only called by replayOps
	 * 
	 * @param offset
	 */
	void scrollTo(int offset) {
		int rowOffset = visibleRowOffset(visRowOffset, offset);
		computeSelectedItemFromSelectedOffset(offset, rowOffset);
	}

	// Called by handleArrow, pick, and scroll.run
	void computeSelectedItemFromSelectedOffset(int itemOffset, int rowOffset) {
		if (onCount > 0 && !onItemsInvalid) {
			boolean isExplicitly = rowOffset < 0;
			selectedItemOffset = Util.constrain(itemOffset, 0, onCount - 1);
			if (rowOffset < 0) {
				rowOffset = visibleRowOffset(visRowOffset, selectedItemOffset);
			}
			Item oldSelectedItem = selectedItem;
			selectedItem = getItem(selectedItemOffset);

			// Util.print("computeSelectedItemFromSelectedOffset itemOffset="
			// + itemOffset + " visRowOffset=" + visRowOffset
			// + " rowOffset=" + rowOffset + " selectedItemOffset="
			// + selectedItemOffset + " oldSelectedItem="
			// + oldSelectedItem + " selectedItem=" + selectedItem);
			if (rowOffset != visRowOffset) {
				visRowOffset = rowOffset;
				updateScrollPos();
				if (oldSelectedItem != selectedItem)
					mustSetItem = true;
				drawGrid();
				// Util.print("^^^ csif calling doEnsureRange");
				doEnsureRange();
				// Util.print("^^^ csif returning doEnsureRange");
			} else if (selectedItem != null) {
				// if selectedItem is null, it means we're waiting for
				// ensureRange, which will call drawGrid when it's ready.
				GridImage thumb = thumbnails.findThumb(selectedItemOffset);
				// If thumbs cache has been GC'd lookupThumb will fail even if
				// we're displaying that thumb
				// We only want displayed thumbs anyway
				if (thumb != null && thumb.getParent() != null) {
					updateSelectionOutline(thumb.getXOffset(), thumb
							.getYOffset(), thumb.getWidth(), thumb.getHeight());
					if (oldSelectedItem != selectedItem)
						art.setSelectedItem(selectedItem, thumb, isExplicitly);
				}
			}
			// for (int gridOffset=0; gridOffset<nRows*nCols; gridOffset++) {
			// if (thumbnails.findThumb(offset)) {
			// doEnsureRange();
			// break;
			// }
			// }
		}
	}

	void updateScrollPos() {
		gridScrollBar.setPos(visRowOffset
				/ (double) Math.max(1, (nRows - nVisibleRows)));
	}

	/**
	 * Can be called in thread rangeEnsurer
	 */
	int visibleRowOffset(int currentRowOffset, int itemOffset) {
		assert itemOffset >= 0;
		assert itemOffset < onCount || (itemOffset == 0 && mustSetItem) : selectedItem
				+ " " + itemOffset + ">=" + onCount;

		int maxStart = (onCount - 1) / nCols - nVisibleRows + 1;
		if (maxStart <= 0)
			return 0;

		int itemRow = itemOffset / nCols;
		int minStart = Math.max(0, itemRow - nVisibleRows + 1);
		maxStart = Math.min(itemRow, maxStart);
		int firstVisibleRow = Util.constrain(currentRowOffset, minStart,
				maxStart);
		// if ((firstVisibleRow + nVisibleRows - 1) * nCols >= onCount)
		// firstVisibleRow = (onCount - 1) / nCols - nVisibleRows + 1;
		return firstVisibleRow;
	}

	void updateSelectionOutline(double x, double y, double width, double height) {
		// Util.print("updateSelectionOutline " + state);
		outline.setBounds(x - 5.0, y - 5.0, width + 10.0, height + 10.0);
		outline.setVisible(true);
		sortLabel.setVisible(true);
		sortMenu.setVisible(true);
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

	void mayHideTransients() {
		art.arrowFocus = null;
		art.mayHideTransients();
	}

	private Map getThumbTable() {
		return (Map) (thumbs == null ? null : thumbs.get());
	}

	private GridImage lookupThumb(Item item) {
		Map table = getThumbTable();
		return (GridImage) (table == null ? null : table.get(item));
	}

	private void addThumb(Item item, GridImage image) {
		// Util.print("addImage " + image.getWidth() + "*" + image.getHeight());
		Map table = getThumbTable();
		if (table == null) {
			// Util.print("new thumb table");
			table = new Hashtable();
			thumbs = new SoftReference(table);
		}
		table.put(item, image);
		// Util.print("# cached thumbs: " + table.size());
	}

	// only called by replayOps and Informedia
	void clickThumb(Item item, int retries) {
		GridImage thumb = lookupThumb(item);
		// Util.print("clickThumb " + item + " " + query().itemIndex(item, 0)
		// + " " + thumb);
		if (thumb == null || thumb.getParent() == null) {
			// if no parent, offset is not valid, because it is set in drawGrid
			selectedItem = item;
			selectedItemOffset = query().itemIndex(item, 0);
			if (selectedItemOffset >= 0) {
				ensureRange();
				drawGrid();
				thumb = lookupThumb(selectedItem);
				if (thumb != null) {
					art.setSelectedItem(selectedItem, thumb, true);
				} else {
					Util.err("Thumbnail not found for " + selectedItem
							+ ". Probably low on memory and table decached.");
					retryClickThumb(item, retries);
					return;
				}
			} else {
				Util.err("Selected item not found for offset "
						+ selectedItemOffset + " " + query().isQueryValid()
						+ " " + item + "\n" + query());
				retryClickThumb(item, retries);
				return;
			}
		}
		if (onItemsInvalid)
			retryClickThumb(item, retries);
		else
			thumb.pick(false, false);
	}

	void retryClickThumb(final Item item, final int retries) {
		// Util.print("retryClickThumb " + retries + " " + onItemsInvalid);
		Runnable retry = new Runnable() {
			public void run() {
				if (retries > 0) {
					clickThumb(item, retries - (onItemsInvalid ? 0 : 1));
				}
			}
		};
		javax.swing.SwingUtilities.invokeLater(retry);
	}

	Query query() {
		return art.query;
	}

	int maxColumns() {
		// Util.print("maxColumns " + thumbnails.getWidth() + " "
		// + thumbnails.getRoot());
		return (thumbnails != null && thumbnails.getWidth() > 0) ? thumbnails
				.maxColumns() : (int) minWidth() / Thumbnails.MIN_COLUMN_WIDTH;
	}

	void clearTextCaches() {
		minWidth = 0;
	}
}

final class Thumbnails extends LazyPNode implements MouseDoc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static int MIN_COLUMN_WIDTH = 20;

	private Boundary[] vBoundaries;

	private Boundary[] hBoundaries;

	private ResultsGrid grid() {
		return (ResultsGrid) getParent();
	}

	int nCols() {
		return grid().nCols;
	}

	int nRows() {
		return grid().nVisibleRows;
	}

	void updateBoundaries(int nCols, int nRows, int edgeW, int edgeH) {
		// Util.print("updateBoundaries " + nCols + " " + edgeW + " " + edgeH);
		setWidth(nCols * edgeW);
		setHeight(nRows * edgeH);
		vBoundaries = updateBoundariesInternal(nCols + 1, vBoundaries, edgeW,
				false);
		hBoundaries = updateBoundariesInternal(nRows + 1, hBoundaries, edgeH,
				true);
	}

	Boundary[] updateBoundariesInternal(int nBoundaries, Boundary[] boundaries,
			int edge, boolean isHorizontal) {
		// Util.print("updateBoundariesInternal " + isHorizontal + " " +
		// nBoundaries + " " + edge);
		for (int i = 0; i < nBoundaries; i++) {
			if (boundaries == null || boundaries.length <= i) {
				boundaries = (Boundary[]) Util.endPush(boundaries,
						new Boundary(this, isHorizontal), Boundary.class);
			}
			Boundary boundary = boundaries[i];
			if (i == 0)
				boundary.setPickable(false);
			boundary.validate();
			boundary.setCenter(i * edge - 1);
		}
		return boundaries;
	}

	public double minWidth(Boundary boundary) {
		int col = Util.member(vBoundaries, boundary);
		return MIN_COLUMN_WIDTH * col;
	}

	public double maxWidth(Boundary boundary) {
		int col = Util.member(vBoundaries, boundary);
		return getWidth() * col;
	}

	public double minHeight(Boundary boundary) {
		int row = Util.member(hBoundaries, boundary);
		return MIN_COLUMN_WIDTH * row;
	}

	public double maxHeight(Boundary boundary) {
		int row = Util.member(hBoundaries, boundary);
		return getHeight() * row;
	}

	int maxColumns() {
		return (int) getWidth() / MIN_COLUMN_WIDTH;
	}

	void addGrid(int nCols, int nRows, int edgeW, int edgeH) {
		// Util.print("addGrid " + (nCols*edgeW) + " " + getWidth());
		updateBoundaries(nCols, nRows, edgeW, edgeH);
		for (int i = 0; i <= nCols; i++) {
			addChild(vBoundaries[i]);
		}
		for (int i = 0; i <= nRows; i++) {
			addChild(hBoundaries[i]);
		}
	}

	public void updateBoundary(Boundary boundary) {
		if (grid().art.getShowBoundaries()) {
			int col = Util.member(vBoundaries, boundary);
			int row = Util.member(hBoundaries, boundary);
			// System.out.println("Thumbnails.updateBoundary " + col + " "
			// + boundary.center());
			if (col > 0) {
				double newEdge = boundary.center() / col;
				int newNcols = (int) (getWidth() / newEdge);
				((ResultsGrid) getParent()).setDesiredCols(newNcols);
			} else if (row > 0) {
				double newEdge = boundary.center() / row;
				int newNcols = (int) (getWidth() / newEdge);
				((ResultsGrid) getParent()).setDesiredCols(newNcols);
			}
		}
	}

	GridImage findThumb(int offset) {
		for (int i = 0; i < getChildrenCount(); i++) {
			PNode child = getChild(i);
			if (child instanceof GridImage) {
				GridImage result = (GridImage) child;
				if (result.offset == offset)
					return result;
			}
		}
		return null;
	}

	public void enterBoundary(Boundary boundary) {
		if (grid().art.getShowBoundaries()) {
			setBoundariesVisible(true);
		} else {
			boundary.exit();
		}
	}

	public void exitBoundary(Boundary boundary) {
		setBoundariesVisible(false);
	}

	void setBoundariesVisible(boolean state) {
		for (int i = 0; i <= nCols() && i < vBoundaries.length; i++) {
			vBoundaries[i].setVisible(state);
		}
		for (int i = 0; i <= nRows() && i < hBoundaries.length; i++) {
			hBoundaries[i].setVisible(state);
		}
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// ((ResultsGrid) getParent()).setMouseDoc(source, state);
	// }

	public void setMouseDoc(String doc) {
		((ResultsGrid) getParent()).setMouseDoc(doc);
	}

	// public void setMouseDoc(Markup doc, boolean state) {
	// ((ResultsGrid) getParent()).setMouseDoc(doc, state);
	// }

}

final class RangeEnsurer extends UpdateNoArgsThread {

	ResultsGrid results;

	RangeEnsurer(ResultsGrid _results) {
		super("Grid.UpdateData", -2);
		results = _results;
	}

	public void process() {
		// results.art.waitForValidQuery();
		// results.revUpLoadThumbs();
		// results.art.waitForValidQuery();
		// if (isUpToDate()) {
		results.ensureRange();
		// }
	}
}