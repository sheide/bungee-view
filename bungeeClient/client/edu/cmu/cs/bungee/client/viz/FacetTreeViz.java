package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.DisplayTree;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.TextButton;
import edu.cmu.cs.bungee.piccoloUtils.gui.VScrollbar;
import edu.umd.cs.piccolo.PNode;

final class FacetTreeViz extends LazyPNode implements MouseDoc {

	Bungee art;

	DisplayTree tree;

	// double w;

	boolean dontHideTransients = false;

	boolean showCheckBox = true;

	double openButtonW;

	static final double openButtonScale = 0.7; // 0.6;

	final static Color openButtonBG = new Color(0x111100);

	final static Color openButtonFG = new Color(0x444400);

	PickFacetTextNotifier pickFacetTextNotifier;

	VScrollbar selectionTreeScrollBar;

	private List contracted = new LinkedList();

	int nInvisibleLines;

	int prevOffset;

	private final APText[] separators = new APText[100];

	private int separatorIndex = 0;

	private final OpenButton[] openButtons = new OpenButton[100];

	private int openButtonIndex = 0;

	int visibleLines;

	FacetTreeViz(Bungee _art) {
		art = _art;

		Runnable scrollTree = new Runnable() {

			public void run() {
				int offset = (int) (selectionTreeScrollBar.getPos()
						* nInvisibleLines + 0.5);
				if (offset != prevOffset) {
					prevOffset = offset;
					double usableW = getWidth() - art.scrollBarWidth;
					// Util.print("scrollTree: offset = " + offset + " "
					// + selectionTreeScrollBar.getPos());
					removeTree();
					drawTree(usableW, offset, offset + visibleLines);
				}
			}
		};

		selectionTreeScrollBar = new VScrollbar(art.scrollBarWidth, 100,
				Bungee.facetTreeScrollBG, Bungee.facetTreeScrollFG, scrollTree);
		selectionTreeScrollBar.setVisible(false);
		addChild(selectionTreeScrollBar);
	}

	void validate(double _w, double _h) {
		// w = _w;
		setWidth(_w);
		setHeight(_h);
		openButtonW = Math.ceil(art.lineH * openButtonScale);
		// facetTexts.clear();
		facetTreeWidths.clear();
		// initSeparators();

		// Util.print("FTV.validate " + availableLines());
		selectionTreeScrollBar.setH(availableLines() * art.lineH);
		selectionTreeScrollBar.setOffset(getWidth() - art.scrollBarWidth, 0);
	}

	APText getSeparator() {
		APText result = separators[separatorIndex++];
		if (result == null) {
			result = art.oneLineLabel();
			// separator.setTextPaint(Art.unselectedLabelColor);
			result.setConstrainWidthToTextWidth(false);
			result.setWidth(art.parentIndicatorWidth);
			result.setText(Markup.parentIndicatorPrefix); // " > ");
			separators[separatorIndex - 1] = result;
		}
		return result;
	}

	OpenButton getOpenButton(Object treeObject, boolean state) {
		OpenButton result = openButtons[openButtonIndex++];
		if (result == null) {
//			Util.err("new open button " + openButtonIndex);
			result = new OpenButton();
			openButtons[openButtonIndex - 1] = result;
		}
		result.p = treeObject;
		result.setLabel(state);
		return result;
	}

	void setTree(DisplayTree _tree) {
		tree = _tree;
		contracted.clear();
	}

	int redraw() {
		return redraw(drawTree());
	}

	int availableLines() {
		return (int) (getHeight() / art.lineH);
	}

	// y margins:
	// <top> 0 <label> 10 <image> 10 <desc> 10 <tree> 2 <bottom>
	// x margins:
	// <left> leftMargin <textBox> leftMargin <right>
	// <left> leftMargin <facetTree> leftMargin <scrollbar> leftMargin <right>
	int redraw(int nSelectedItemTreeMinLines) {
		removeTree();
		nInvisibleLines = 0;
		int availableLines = availableLines();
		// Util.print("FTV.redraw " + availableLines);
		if (availableLines >= nSelectedItemTreeMinLines) {
			visibleLines = nSelectedItemTreeMinLines;
			// facetTree.setHeight(visibleLines * art.lineH);
			drawTree(getWidth(), 0, availableLines);
			selectionTreeScrollBar.setVisible(false);
		} else {
			nInvisibleLines = 1;
			visibleLines = availableLines;
			// facetTree.setHeight(visibleLines * art.lineH);
			int nSelectedItemTreeMaxLines = drawTree(getWidth()
					- art.scrollBarWidth, 0, 999999);
			nInvisibleLines = nSelectedItemTreeMaxLines - availableLines;
			prevOffset = 0;
			selectionTreeScrollBar.reset();
			selectionTreeScrollBar.setBufferPercent(availableLines,
					nSelectedItemTreeMaxLines);
			selectionTreeScrollBar.setVisible(true);
		}
		// Util.print("FTV.redraw return " + nInvisibleLines);
		return nInvisibleLines;
	}

	int drawTree() {
		return drawTree(getWidth(), 999999, availableLines() + 1);
	}

	// offsetLines is how many to skip. 999999 means dont draw, just compute
	// number of lines.
	// lastLine includes both offsetLines and visible lines.
	// The return value also includes offset plus visible lines.
	int drawTree(double treeW, int offsetLines, int lastLine) {
		if (tree != null) {
			addChild(selectionTreeScrollBar);
			double margin = (nInvisibleLines > 0 || contracted.size() > 0) ? openButtonW
					: 0;
			// System.out.println(tree);
			int nLines = drawTreeInternal(tree, margin, treeW, offsetLines,
					lastLine);
			// Util.print("drawTree: offsetLines=" + offsetLines + "; lastLine="
			// + lastLine + "; margin=" + margin + "; treeW=" + treeW + " => "
			// + nLines);
			return nLines;
		} else
			return -1;
	}

	void removeTree() {
		removeAllChildren();
		// addChild(selectionTreeScrollBar);
		// separatorTexts.clear();
		separatorIndex = 0;
		openButtonIndex = 0;
	}

	private int drawTreeInternal(DisplayTree subtree, double x, double treeW,
			int offsetLines, int lastLine) {
		int nLines = 0; // This includes offsetLines
		APText separator = null;
		Object treeObject = subtree.treeObject();
		boolean showChildren = subtree.nChildren() > 0
				&& !isContracted(treeObject);
//		 Util.print("drawTreeInternal " + treeObject + " " + offsetLines + " "
//		 + lastLine + " " + x + " " + treeW + " " + showChildren);
		if (treeObject != null && !(treeObject instanceof Item)) { // don't
			// display
			// null's or
			// Item's
			FacetText facetLabel = null;
			double y = -offsetLines * art.lineH;
			if (y > getHeight() - art.lineH)
				// When first drawing scrolling tree, we set lastLine to 999999.
				// Check here to see if we're off the page yet.
				offsetLines = 999999;
			if (offsetLines <= 0) {

				// if the facet is being displayed in a tree, its count has to
				// be > 0
				facetLabel = FacetText.getFacetText(treeObject, art,
						showCheckBox, pickFacetTextNotifier);
				facetLabel.dontHideTransients = dontHideTransients;
				// Util.print("adding label " + treeObject + " at (" + x + ", "
				// + y + ")");
				addChild(facetLabel);
				facetLabel.setOffset(x, y);
			}
			boolean overflow = drawWidth(subtree) + x > treeW;
			if (offsetLines <= 0
					&& ((nInvisibleLines > 0 && ((overflow && showChildren) || !(subtree
							.getParent().treeObject() instanceof Perspective)
					// || tree.p == null || tree.p.parent == null
					)) || isContracted(treeObject))) {
				boolean state = isContracted(treeObject);
				OpenButton open = getOpenButton(treeObject, state);
				// Util.print(treeObject + " " + x + " " + overflow);
				addChild(open);
				open.setOffset(x - openButtonW - 2, y
						+ Math.ceil((art.lineH - openButtonW) / 2));
			}
			if (overflow) {
				// Now we know this line is done.
				if (facetLabel != null && x + facetLabel.getWidth() > treeW) {
					facetLabel.setConstrainWidthToTextWidth(false);
					facetLabel.setWidth(treeW - x);
				}
				nLines++;
				x += 20.0;
			} else {
				if (facetLabel != null) {
					facetLabel.setConstrainWidthToTextWidth(true);
					assert checkFacetLabel(facetLabel, treeObject);
				}
				x += art.getFacetStringWidth(treeObject, false, showCheckBox);
				if (showChildren) { // Add '>'
					if (facetLabel != null) {
						separator = getSeparator();
						separator.setOffset(x, y);
						separator.setTextPaint(Bungee.selectedItemFG); // unselectedLabelColor);
						addChild(separator);
					}
					x += art.parentIndicatorWidth;
				} else {
					nLines++;
				}
			}
		}
		if (showChildren) {
			for (Iterator it = subtree.childIterator(); it.hasNext()
					&& nLines < lastLine;) {
				DisplayTree child = (DisplayTree) it.next();
				nLines += drawTreeInternal(child, x, treeW, offsetLines
						- nLines, lastLine - nLines);
				// if (separator != null) {
				// Perspective facet = child.p;
				// separatorTexts.put(facet.facet_id, separator);
				// if (Util.isMember(allRestrictions, facet))
				// separator.setTextPaint(Art.selectedLabelColor);
				// }
			}
		}
		// Util.print("drawTreeInternal " + treeObject + " return " + nLines);
		// assert nLines > 0; // There may be 0 clusters
		return nLines;
	}

	boolean checkFacetLabel(FacetText facetLabel, Object treeObject) {
		double actualW = Math.ceil(facetLabel.getWidth());
		double fsw = art.getFacetStringWidth(treeObject, false, showCheckBox);
		if (!(facetLabel.getText().equals("?") || actualW == fsw || actualW + 1 == fsw))
			Util.err(treeObject + " '" + facetLabel.getText() + "' has w="
					+ facetLabel.getWidth()
					+ ",\n and should be a little less than "
					+ art.getStringWidth(facetLabel.getText())
					+ ", but the cached value is " + fsw);
		return true;
	}

	void contract(Object p, boolean isContract) {
		// Util.print("contract " + p + " " + state);
		assert contracted.contains(p) == !isContract : p + " " + contracted;
		if (isContract) {
			contracted.add(p);
		} else {
			contracted.remove(p);
		}
		redraw();
	}

	private boolean isContracted(Object p) {
		return contracted.contains(p);
	}

	private Hashtable facetTreeWidths = new Hashtable();

	private double drawWidth(DisplayTree subtree) {
		Double w = (Double) facetTreeWidths.get(subtree);
		if (w == null) {
			double treeW = 0.0;
			for (Iterator it = subtree.childIterator(); it.hasNext();) {
				DisplayTree child = (DisplayTree) it.next();
				treeW = Math.max(treeW, drawWidth(child));
			}
			if (treeW > 0.0)
				treeW += art.parentIndicatorWidth;
			treeW += art.getFacetStringWidth(subtree.treeObject(), false,
					showCheckBox);
			w = new Double(treeW);
			facetTreeWidths.put(subtree, w);
		}
		return w.doubleValue();
	}

	// private IntHashtable separatorTexts = new IntHashtable();

	// private Perspective[] allRestrictions;

	void highlightFacet(Set highlightFacets) {
		// FacetText child = getFacetText(highlightFacet);
		// if (child != null && child.getParent() != null) {
		// child.highlightFacet();
		// }
		int nChildren = getChildrenCount();
		for (int i = 0; i < nChildren; i++) {
			PNode node = getChild(i);
			if (node instanceof FacetText) {
				FacetText ft = (FacetText) node;
				if (highlightFacets.contains(ft.facet)
						|| highlightFacets.contains(ft.cluster))
					ft.highlightFacet();
			}
		}
		// APText sep = (APText) separatorTexts.get(highlightFacet.facet_id);
		// if (sep != null) {
		// boolean isRestricted = highlightFacet.isRestriction();
		// sep.setTextPaint(isRestricted ? Art.selectedLabelColor
		// : Art.unselectedLabelColor);
		// }
	}

	void updateColors() {
		// Util.print("FacetTreeViz.synchronizeWithQuery");
		// leafRestrictions = q.dontUnderline();
		// allRestrictions = q.restrictions();
		for (Iterator it = getChildrenIterator(); it.hasNext();) {
			PNode x = (PNode) it.next();
			if (x instanceof FacetText) {
				FacetText child = (FacetText) x;
				child.selectFacet();
				// highlightFacet(child.getFacet());
			}
		}
		// Util.print("FacetTreeViz.synchronizeWithQuery return");
	}

	// void clickText(Perspective facet, int modifiers) {
	// // FacetText facetLabel = getFacetText(facet);
	// // if (facetLabel == null)
	// // // fake it
	// art.toggleFacet(facet, modifiers);
	// // else
	// // facetLabel.pick(modifiers);
	// }

	final class OpenButton extends TextButton {

		/**
		 * plus sign
		 */
		private static final String expandLabel = "\u271A";

		/**
		 * minus sign
		 */
		private static final String contractLabel = "\u25AC";

		Object p;

		OpenButton() {
			// super("-", art.font, 0, 0, art.lineH, art.lineH, null, 1.8f,
			// openButtonFG, openButtonBG);
//			super(contractLabel+ " ", openButtonFG, openButtonBG,
//					FacetTreeViz.this.art);

			super(contractLabel+" ", art.font, 0, 0, -1, -1, null, 0, openButtonFG, openButtonBG);
			
//			((APText) child).setConstrainWidthToTextWidth(true);
//			((APText) child).setConstrainHeightToTextHeight(true);
			setScale(openButtonScale);
		}

		/**
		 * Don't call this setState, because Button.setPaths would call it and
		 * screw up the state we want.
		 * 
		 * @param isContracted
		 */
		void setLabel(boolean isContracted) {
			// super.setState(isContracted);

			// If isContracted, button should show the expand sign
			setText(isContracted ? expandLabel : contractLabel);
			mouseDoc = isContracted ? "Show indented lines"
					: "Hide indented lines";
		}

		public void doPick() {
			assert contractLabel.equals(getText())
					|| expandLabel.equals(getText()) : getText();
			contract(p, contractLabel.equals(getText()));
		}

	}

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}
}
