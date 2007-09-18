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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;



import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.VScrollbar;
import edu.umd.cs.piccolo.PNode;

public class ResultsGrid extends LazyPNode implements MouseDoc {

	private static final long serialVersionUID = -3582704861244473459L;

	int desiredCols = -1;

	private int marginSize = 5;

	 double w;

	private double h;

	/**
	 * Maps from item record_nums to a GridImage (a kind of PImage)
	 */
	private transient SoftReference thumbs;

	 VScrollbar gridScrollBar;

	Bungee art;

	int nRows;

	int nCols; // nRows * nCols >= onCount (= if the last col is full)

	int gridW;

	int gridH;

	int nVisibleRows;

	int visRowOffset;

	private APText label;

	private Thumbnails thumbnails;

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

	private APText countLabel;

	private APText percentLabel;

	private LazyPPath outline;

	private Boundary boundary;

	Menu sortMenu;

	private APText sortLabel;

	ResultsGrid(Bungee a) {
		art = a;
		setPickable(false);
		setPaint(Bungee.gridBG);
	}

	void init() {
		rangeEnsurer = new RangeEnsurer(this);
		rangeEnsurer.start();
		// setPaint(Color.black); // hide initially

		label = art.oneLineLabel();
		label.scale(2.0);
		label.setTextPaint(Bungee.gridFG);
		label.setPickable(false);
		label.setText("Results");

		countLabel = art.oneLineLabel();
		countLabel.setTextPaint(Bungee.gridFG);
		countLabel.setPickable(false);

		percentLabel = art.oneLineLabel();
		percentLabel.setTextPaint(Bungee.gridFG);
		percentLabel.setPickable(false);

		thumbnails = new Thumbnails();

		outline = (LazyPPath) LazyPPath.createRectangle(0, 0, 1, 1,
				LazyPPath.getStrokeInstance(1));
		outline.setStroke(LazyPPath.getStrokeInstance(6));
		outline.setStrokePaint(Bungee.selectedItemOutlineColor);
		outline.setTransparency(0.9f);
		outline.setVisible(false);
		// outline.setPaint(null);

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

		gridScrollBar = new VScrollbar(art.scrollBarWidth, 50,
				Bungee.gridScrollBG, Bungee.gridScrollFG, scroll);

		boundary = new Boundary(this, false);
		addChild(boundary);

		sortLabel = art.oneLineLabel();
		sortLabel.setTextPaint(Bungee.gridFG.darker());
		sortLabel.setText("sorted by:");
		sortLabel.setVisible(false);
		sortLabel.setScale(0.8);
		addChild(sortLabel);

		Runnable doReorder = new Runnable() {
			public void run() {
				reorder(((Integer) sortMenu.getData()).intValue());
			}
		};

		sortMenu = new Menu(Bungee.gridBG, Bungee.gridFG.darker(), doReorder,
				art.font);
		sortMenu.addButton("Random", "Random", new Integer(-1));
		sortMenu.addButton("ID", "ID", new Integer(0));
		for (int i = 1; i <= art.query.nAttributes; i++) {
			ItemPredicate facetType = art.query.findPerspective(i);
			sortMenu.addButton(facetType.getName(), facetType.getName(),
					new Integer(i));
		}
		sortMenu.setVisible(false);
		sortMenu.setScale(0.8);
		addChild(sortMenu);

		validate(w, h);
	}

	private boolean isInitted() {
		return label != null;
	}

	void stop() {
		if (rangeEnsurer != null) {
			rangeEnsurer.exit();
			rangeEnsurer = null;
		}
	}

	double getTopMargin() {
		return (2.0 /* label.getScale() */+ 2.0) * art.lineH + 10.0;
	}

	double getBottomMargin() {
		return h - sortLabel.getYOffset();
	}

	// void validate(double _w, double _h, double _minW, double _maxW) {
	// maxW = _maxW;
	// minW = _minW;
	// validate(_w, _h);
	// }

	void validate(double _w, double _h) {
		w = _w;
		h = _h;
		setBounds(0, 0, w, h);
		if (isInitted()) {
			label.setOffset(marginSize, 0.0);
			label.setFont(art.font);
			label.setHeight(art.lineH);
			double y = label.getScale() * art.lineH;
			countLabel.setOffset(marginSize, y);
			countLabel.setFont(art.font);
			percentLabel.setOffset(marginSize, y + art.lineH);
			percentLabel.setFont(art.font);
			thumbnails.setOffset(marginSize, getTopMargin());
			gridScrollBar.setOffset(w - marginSize - art.scrollBarWidth,
					getTopMargin());
			double sortY = h - sortMenu.getHeight() * sortMenu.getScale() - 5.0;
			if (sortLabel.getWidth() + sortMenu.getWidth() + 5.0 <= w) {
				sortLabel.setOffset(5.0, sortY);
				sortMenu.setOffset(sortLabel.getMaxX(), sortY);
			} else {
				sortMenu.setOffset(5.0, sortY);
				sortY -= sortLabel.getHeight() * sortLabel.getScale();
				sortLabel.setOffset(5.0, sortY);
			}
			// boundary.setMinX(minW);
			// boundary.setMaxX(maxW);
			boundary.validate();
			if (onCount > 0)
				dataUpdated();
		}
	}

	public void updateBoundary(Boundary boundary1) {
		assert boundary1 == boundary;
		validate(boundary1.center(), h);
		art.updateGridBoundary();
	}

	public double minWidth() {
		return art.getStringWidth("(0.001% of 999,999 "
				+ Util.pluralize(art.query.genericObjectLabel) + ")");
	}

	public double maxWidth() {
		return getWidth() + art.selectedItem.getWidth()
				- art.selectedItem.minWidth(null);
	}

	void setDescription() {
		StringBuffer result = new StringBuffer();
		result.append(Util.addCommas(onCount));
		if (onCount == 1)
			result.append(" match");
		else
			result.append(" matches");
		countLabel.setText(result.toString());
		assert w > countLabel.getWidth() : w + " " + countLabel.getWidth()
				+ countLabel.getText();

		result = formatPercent(onCount / (double) art.query.totalCount, null);
		result.insert(0, "(");
		result.append(" of ");

		result.append(Util.addCommas(art.query.totalCount));

		result.append(" ");
		String object = Util.pluralize(art.query.genericObjectLabel);
		result.append(object);
		result.append(")");
		percentLabel.setText(result.toString());
		assert w > percentLabel.getWidth() : w + " " + percentLabel.getWidth()
				+ percentLabel.getText();
	}

	static StringBuffer formatPercent(double fraction, StringBuffer buf) {
		if (buf == null)
			buf = new StringBuffer();
		double percent = 100.0 * fraction;
		if (percent < 0)
			buf.append("?");
		else if (percent >= 0.95 || percent == 0.0)
			// Anything larger than this will round to 1.0 if we go down to
			// tenths.
			buf.append(((int) (percent + 0.5)));
		else if (percent >= 0.095 || percent < 0.00095)
			buf.append("0.").append(((int) (percent * 10.0 + 0.5)));
		else if (percent >= 0.0095)
			buf.append("0.0").append(((int) (percent * 100.0 + 0.5)));
		else if (percent >= 0.00095)
			buf.append("0.00").append(((int) (percent * 1000.0 + 0.5)));
		buf.append("%");
		return buf;
	}

	void dataUpdated() {
		// Util.print("Grid.dataUpdated " + art.query.onCount);
		onCount = art.query.onCount;
		setDescription();
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

	int maxThumbs(int count) {
		double usableH = h - getTopMargin() - getBottomMargin();
		double usableW = w - art.scrollBarWidth - art.scrollMarginSize - 2
				* marginSize;
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
//		Util.print("maxThumbs " + usableW);
		return nVisibleRows * nCols;
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
		edgeW = gridW - 4;
		edgeW = Math.max(1, edgeW);
		edgeH = gridH - 4;
		edgeH = Math.max(1, edgeH);
		if (gridScrollBar != null) {
			gridScrollBar.setH(gridH * nVisibleRows);
			gridScrollBar.setBufferPercent(nVisibleRows, nRows);
		}
		// thumbnails.updateBoundaries(nCols, nVisibleRows, grid);
	}

	public void setDesiredCols(int newNcols) {
		// Util.print("setDesiredCols " + newNcols);
		if (newNcols != desiredCols) {
			desiredCols = newNcols;
			computeEdge();
			desiredCols = nCols; // Would be confusing to change the desire
			// if the user doesn't see a change.
			visRowOffset = -9999;
			computeSelectedItemFromSelectedOffset(selectedItemOffset, -1);
		}
	}

	int getDesiredColumns() {
		if (desiredCols > 0)
			return desiredCols;
		else {
			return (int) (w / Bungee.minThumbSize / art.scaleRatio(0.5));
		}
	}

	void reorder(int facetType) {
		// art.handleCursor(true);
		art.query.reorderItems(facetType);
		// offsetItemTableRangesIndex = 0;
		// doEnsureRange();
		dataUpdated();
		// art.handleCursor(false);
	}

	void doEnsureRange() {
		// schedule a ensureRange, and then doRedraw
		rangeEnsurer.update();
	}

	// We don't want slow db calls in the mouse process, so this is
	// called by rangeEnsurer.
	public void ensureRange() {
		// Util.print("ensureRange " + selectedItem + " " + selectedItemOffset +
		// " " + onCount);
		// If selectedItem is still in onItems, return it's new offset;
		// otherwise, set offset to zero.
		if (selectedItem != null && selectedItemOffset < 0) {
			// This should be pre-computed - no need to talk to server.
			selectedItemOffset = art.query.itemIndex(selectedItem,
			// add an extra 1 to catch the case of paging down.
					(nVisibleRows + 1) * nCols);
			// Util.print("itemIndex = " + selectedItemOffset);
		}
		if (selectedItemOffset < 0) {
			// Need new selectedItem
			mustSetItem = true;
			selectedItemOffset = 0;
		}

		visRowOffset = visibleRowOffset(visRowOffset, selectedItemOffset);
		int originalMinOffset = visRowOffset * nCols;
		int originalMaxOffset = (visRowOffset + nVisibleRows) * nCols;
		if (originalMaxOffset > onCount)
			originalMaxOffset = onCount;

		if (!isInRange(originalMinOffset, originalMaxOffset)) {
			// Util.print("ensureRange " + offsetItemTableRangesIndex + " "
			// + minOffset + " " + maxOffset);
			originalMaxOffset = Util.min(onCount, originalMaxOffset);
			if (offsetItemTable == null) {
				// assert offsetItemTableRangesIndex == 0;
				offsetItemTable = new Item[art.query.totalCount];
				// if (offsetItemTable != null)
				// System.arraycopy(offsetItemTable, 0, tempOffsetItemTable,
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
						newRanges = Util.append(newRanges, Util.subArray(
								offsetItemTableRanges, maxRange + 2,
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

	// checks whether items need ItemImages (which it calls loadThumbs to load
	// from db) or GridImages
	// (which it creates immediately).
	void checkCached(int minOffset, int maxOffset) {
		// Util.print("ensureCached " + edge + " " + onCount + " " + minOffset +
		// " " + maxOffset);
		Item[] toLoad = null;
		if (offsetItemTable != null) {
			assert maxOffset <= offsetItemTable.length : offsetItemTable.length
					+ " " + minOffset + "-" + maxOffset + " "
					+ offsetItemTableRangesIndex + " "
					+ PrintArray.printArrayString(offsetItemTableRanges);
			for (int i = minOffset; i < maxOffset; i++) {
				Item item = offsetItemTable[i];
				if (isInRange(i, i)) {
					assert item != null;

					ItemImage ii = art.lookupItemImage(item);
					// Util.print("offset [" + minOffset + ", " + maxOffset + "]
					// =>
					// "
					// + item + " " + (ii!=null));

					if (ii == null
							|| !ii.bigEnough(edgeW, edgeH, Bungee.ThumbQuality)) {
						toLoad = (Item[]) Util.push(toLoad, item, Item.class);
					} else if (lookupThumb(item) == null) {
						// After unrestrict, thumbs gets wiped but itemImages
						// doesn't
						addThumb(item, new GridImage(ii));
					}
				}
			}
		}
		assert !Util.hasDuplicates(toLoad) : minOffset + "-" + maxOffset + " "
				+ offsetItemTableRangesIndex + " "
				+ PrintArray.printArrayString(offsetItemTableRanges) + " "
				+ PrintArray.printArrayString(toLoad);
		if (toLoad != null) {
			loadThumbs(toLoad);
			javax.swing.SwingUtilities.invokeLater(getDoRedraw());
		}
	}

	// ensureRange makes sure that offsetItemTable is updated
	void ensureRangeInternal(int minOffset, int maxOffset) {
		// Util.print("ensureRangeInternal " + minOffset + "-" + maxOffset);
		java.sql.ResultSet rs = art.query.offsetItems(minOffset, maxOffset);
		// WARNING: don't change rs row, as it may be purposefullly set to an
		// intermediate row.
		try {
			while (rs.next() && minOffset < maxOffset) {
				Item item = Item.ensureItem(rs.getInt(1));
				offsetItemTable[minOffset++] = item;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			art.query.close(rs);
		}
		// Util.print("ensureRangeInternal return " +
		// PrintArray.printArrayString(toLoad));
	}

	void loadThumbs(Item[] myItems) {
		if (myItems != null) {
//			 Util.print("loadThumbs " + PrintArray.printArrayString(myItems));
			Arrays.sort(myItems);
			ResultSet rs = null;
			try {
				rs = art.query.getThumbs(myItems, edgeW, edgeH, Bungee.ThumbQuality);
				boolean hasNext = rs.next();
				Item blobItem = hasNext ? Item.ensureItem(rs.getInt(1)) : null;
				for (int index = 0; index < myItems.length; index++) {
					Item item = myItems[index];
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
					 Util.print("addThumb " + edgeW + "*" + edgeH + " " + item + " " + rawW + "*" + rawH);
					addThumb(item, new GridImage(ii)); // art, item,
					// image));
				}

				// int index = 0;
				// while (rs.next()) {
				// int blobItem = rs.getInt(1);
				// InputStream blobStream = ((MyResultSet) rs)
				// .getInputStream(2);
				// int item;
				// while ((item = myItems[index++]) != blobItem) {
				// thumbs.put(item, new GridImage(item, this,
				// getMissingImage()));
				// }
				// if (blobStream != null) {
				// BufferedImage image = ImageIO.read(blobStream);
				// if (image.getColorModel().getNumColorComponents() < 3)
				// // Workaround for washed out colors when resizing
				// // grayscale images
				// image = PImage.toBufferedImage(image, true);
				// thumbs.put(blobItem, new GridImage(item, this, image));
				// }
				// }
				// while (index < myItems.length) {
				// int item = myItems[index++];
				// thumbs.put(item, new GridImage(item, this,
				// getMissingImage()));
				// }
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				art.query.close(rs);
			}
		}
		// Util.print("loadThumbs return");
	}

	// void revUpLoadThumbs() {
	// // Util.print("revUpLoadThumbs");
	// // loadThumbs takes a while the first time because it has to look
	// // up the appropriate reader for the stream's MIME type.
	// loadThumbs(ensureRangeInternal(0, 1));
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
			addChild(countLabel);
			addChild(percentLabel);
			addChild(gridScrollBar);
			addChild(thumbnails);
		}
		thumbnails.removeAllChildren();
		if (onCount > 0) {
			thumbnails.addChild(outline);
			// Util.print("ResultGrid.drawGrid " + visRowOffset + " "
			// + selectedItem);
			assert visRowOffset == 0 || onCount > nVisibleRows * nCols : visRowOffset;
			int minOffset = visRowOffset * nCols;
			int maxOffset = Util.min(onCount, minOffset + nVisibleRows * nCols);
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
							// Util.print("drawGrid " + item + " " + image.)
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
								double x = col * gridW + (int) ((gridW - iw) / 2);
								double y = row * gridH + (int) ((gridH - ih) / 2);
								image.setOffset(x, y);
								thumbnails.addChild(image);

								// Now that we know the location, art can
								// animate outline.
								if (item == selectedItem) {
									updateSelectionOutline(x, y, iw, ih);
									if (mustSetItem) {
										mustSetItem = false;
										art.setSelectedItemID(item, outline);
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
		}
		// Util.print(" Exit drawGrid");
	}

	// // No longer used -- packs thumbnails keeping aspect ratio
	// void sortRow(int prevRowOffset, int rowOffset, int offset, boolean even,
	// double scale) {
	// Util.print("sortRow " + prevRowOffset + " " + rowOffset + " " + offset);
	// GridImage[] images = new GridImage[offset - rowOffset];
	// for (int i = rowOffset; i < offset; i++) {
	// GridImage gi = (GridImage) thumbs.get(getItem(i));
	// if (gi != null) {
	// images[i - rowOffset] = gi;
	// double area = edge * edge / (scale * scale);
	// gi.setArea(area);
	// // Image ii = gi.getImage();
	// // Util.print(offset + " " + ii.getHeight(null));
	// }
	// }
	// Arrays.sort(images, new GridImageHeightComparator());
	// // if (even)
	// // images = (GridImage[]) Util.reverse(images);
	// int x = 0;
	// int margin = grid - edge;
	// int w = nCols * grid;
	// for (int i = 0; i < images.length; i++) {
	// GridImage gi = images[i];
	// if (gi != null) {
	// int iw = gi.getImage().getWidth(null);
	// // int ih = images[i].getImage().getHeight(null);
	// int realX = even ? x : w - x - iw;
	// int y = findY(prevRowOffset, rowOffset, realX - margin, realX
	// + iw + margin, even, margin);
	// images[i].setOffset(realX, y);
	// x += iw + margin;
	// // Util.print(even + " " + x + " " + iw + "x" + ih);
	// }
	// }
	// }
	//
	// // No longer used -- packs thumbnails keeping aspect ratio
	// int findY(int prevRowOffset, int rowOffset, int x1, int x2, boolean even,
	// int margin) {
	// int result = 0;
	// for (int i = prevRowOffset; i < rowOffset; i++) {
	// GridImage gi = (GridImage) thumbs.get(getItem(i));
	// if (gi.getXOffset() < x2 && gi.getXOffset() + gi.getWidth() > x1)
	// result = Util.max(result, margin
	// + (int) (gi.getYOffset() + gi.getHeight()));
	// }
	// Util.print(prevRowOffset + "-" + rowOffset + " " + x1 + "-" + x2 + " "
	// + result);
	// return result;
	// }
	//
	// // No longer used -- packs thumbnails keeping aspect ratio
	// class GridImageHeightComparator extends ValueComparator {
	//
	// public int value(Object data) {
	// if (data == null)
	// return 0;
	// return ((GridImage) data).getImage().getHeight(null);
	// }
	// }

	// Set one element for use by revUpLoadThumbs.
	Item[] offsetItemTable;

	/**
	 * plist of [min, max] offset ranges for which we've cached corresponding
	 * record_num's in offsetItemTable.
	 */
	private int[] offsetItemTableRanges = {};

	/**
	 * last valid entry in offsetItemTableRanges
	 */
	int offsetItemTableRangesIndex = 0;

	boolean isInRange(int min, int max) {
		for (int i = 0; i < offsetItemTableRangesIndex; i += 2) {
			if (offsetItemTableRanges[i] <= min
					&& offsetItemTableRanges[i + 1] >= max)
				return true;
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
		art.printUserAction(Bungee.KEYPRESS, "grid", selectedItem.getId());
	}

	// Called by handleArrow, pick, and scroll.run
	void computeSelectedItemFromSelectedOffset(int itemOffset, int rowOffset) {
		if (onCount > 0) {
			selectedItemOffset = Util.constrain(itemOffset, 0, onCount - 1);
			if (rowOffset < 0) {
				rowOffset = visibleRowOffset(visRowOffset, selectedItemOffset);
			}
			Item oldSelectedItem = selectedItem;
			selectedItem = getItem(selectedItemOffset);

			// Util.print("computeSelectedItemFromSelectedOffset itemOffset=" +
			// itemOffset +
			// " visRowOffset="
			// + visRowOffset + " rowOffset=" + rowOffset
			// + " selectedItemOffset=" + selectedItemOffset
			// + " oldSelectedItem=" + oldSelectedItem + " selectedItem="
			// + selectedItem);
			if (rowOffset != visRowOffset) {
				visRowOffset = rowOffset;
				updateScrollPos();
				if (oldSelectedItem != selectedItem)
					mustSetItem = true;
				drawGrid();
				// Util.print("^^^ csif calling doEnsureRange");
				doEnsureRange();
				// Util.print("^^^ csif returning doEnsureRange");
			} else {
				assert selectedItem != null;
				GridImage thumb = thumbnails.findThumb(selectedItemOffset);
				// If thumbs cache has been GC'd lookupThumb will fail even if
				// we're displaying that thumb
				// We only want displayed thumbs anyway
				if (thumb != null && thumb.getParent() != null) {
					updateSelectionOutline(thumb.getXOffset(), thumb
							.getYOffset(), thumb.getWidth(), thumb.getHeight());
					if (oldSelectedItem != selectedItem)
						art.setSelectedItemID(selectedItem, thumb);
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

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	void mayHideTransients() {
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
			table = new Hashtable();
			thumbs = new SoftReference(table);
		}
		table.put(item, image);
	}

	// only called by replayOps
	public void clickThumb(Item item) {
		GridImage thumb = lookupThumb(item);
		// Util.print("clickThumb " + item + " " +
		// art.query.itemIndex(selectedItem));
		if (thumb == null) {
			selectedItem = item;
			selectedItemOffset = art.query.itemIndex(item, 0);
			assert selectedItemOffset >= 0;
			ensureRange();
			drawGrid();
			thumb = lookupThumb(selectedItem);
			art.setSelectedItemID(selectedItem, thumb);
		}
		thumb.pick(false, false);
	}

	// only called on startup
	public void clickThumb(String URL) {
		int[] x = art.query.itemIndexFromURL(URL, 0);
		selectedItem = Item.ensureItem(x[0]);
		selectedItemOffset = x[1];
		// Util.print("sss " + selectedItem + " " + selectedItemOffset);
		// doEnsureRange();
		// ensureRange();
		// drawGrid();
		// GridImage thumb = (GridImage) thumbs.get(selectedItem);
		// art.setSelectedItemID(selectedItem, thumb);
		art.selectedItem.animateOutline(selectedItem, -1, -1, -1, -1);
	}
}

class Thumbnails extends LazyPNode implements MouseDoc {

	private static final long serialVersionUID = 5187249169891162433L;

	private Boundary[] vBoundaries;

	private Boundary[] hBoundaries;

	int nCols() {
		return ((ResultsGrid) getParent()).nCols;
	}

	int nRows() {
		return ((ResultsGrid) getParent()).nVisibleRows;
	}

	public void updateBoundaries(int nCols, int nRows, int edgeW, int edgeH) {
//		 Util.print("updateBoundaries " + nCols + " " + edgeW + " " + edgeH);
		setWidth(nCols * edgeW);
		setHeight(nRows * edgeH);
		vBoundaries = updateBoundariesInternal(nCols + 1, vBoundaries, edgeW,
				false);
		hBoundaries = updateBoundariesInternal(nRows + 1, hBoundaries, edgeH,
				true);
	}

	Boundary[] updateBoundariesInternal(int nBoundaries, Boundary[] boundaries,
			int edge, boolean isHorizontal) {
//		Util.print("updateBoundariesInternal " + isHorizontal + " " + nBoundaries + " " + edge);
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
		return 20 * col;
	}

	public double maxWidth(Boundary boundary) {
		int col = Util.member(vBoundaries, boundary);
		return getWidth() * col;
	}

	public double minHeight(Boundary boundary) {
		int row = Util.member(hBoundaries, boundary);
		return 20 * row;
	}

	public double maxHeight(Boundary boundary) {
		int row = Util.member(hBoundaries, boundary);
		return getHeight() * row;
	}

	public void addGrid(int nCols, int nRows, int edgeW, int edgeH) {
//		Util.print("addGrid " + (nCols*edgeW) + " " + getWidth());
		updateBoundaries(nCols, nRows, edgeW, edgeH);
		for (int i = 0; i <= nCols; i++) {
			addChild(vBoundaries[i]);
		}
		for (int i = 0; i <= nRows; i++) {
			addChild(hBoundaries[i]);
		}
	}

	public void updateBoundary(Boundary boundary) {
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
		setBoundariesVisible(true);
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

	public void setMouseDoc(PNode source, boolean state) {
		((ResultsGrid) getParent()).setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc, boolean state) {
		((ResultsGrid) getParent()).setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		((ResultsGrid) getParent()).setMouseDoc(doc, state);
	}

}

class RangeEnsurer extends UpdateNoArgsThread {

	ResultsGrid results;

	public synchronized void exit() {
		// Util.print("RangeEnsurer.exit");
		results = null; // Threads seem to stick around. Lose this reference so
		// it can be gc'd
		super.exit();
	}

	public RangeEnsurer(ResultsGrid _results) {
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