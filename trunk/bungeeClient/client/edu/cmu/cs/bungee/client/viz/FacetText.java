/* 

 Created on Mar 28, 2006

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

import java.awt.Paint;
import java.awt.event.InputEvent;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Button;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;


public class FacetText extends APText implements FacetNode, PerspectiveObserver {

	private static final long serialVersionUID = 4849947064017481501L;

	 PerspectiveObserver redraw;

	 boolean dontHideTransients;

	 Perspective facet;

	Cluster cluster;

	 Button checkBox;

	/* APText */LazyPPath checkmark;

	 Button xBox;

	/* APText */LazyPPath xmark;

	 Bungee art;

	private Paint permanentTextPaint;

	boolean showCheckBox;

	boolean showChildIndicator;

	public boolean isPickable;

	/**
	 * Only meaningful if !isPickable
	 * -1 means do nothing; -2 means open category
	 */
	int unpickableAction = -1;

	double numW = -1;

	double nameW = -1;

	PickFacetTextNotifier pickFacetTextNotifier;

	private boolean numFirst;

	// static Font checkFont;

	static final FacetTextHandler facetTextHandler = new FacetTextHandler();

	private static transient SoftReference facetTexts;

	// Use this to avoid the non-null assert on getName, in carefully considered places
	private static final PerspectiveObserver NULL_REDRAWER = new TextNfacets(null, null, false);

	static void clearFacetTexts() {
		facetTexts = null;
	}

	static FacetText getFacetText(Object treeObject, Bungee a) {
		return getFacetText(treeObject, a, -1, -1, false, false, false, null,
				0, null, -1, null);
	}

	static synchronized FacetText getFacetText(Object treeObject, Bungee a,
			double _numY, double _nameY, boolean _showChildIndicator,
			boolean _showCheckBox, boolean _dontHideTransients,
			PickFacetTextNotifier notifier, int onCount,
			Paint _permanentTextPaint, int _unpickableAction, PerspectiveObserver _redraw) {
		return getFacetText(treeObject, a, _numY, _nameY, _showChildIndicator,
				_showCheckBox, _dontHideTransients, notifier, onCount,
				_permanentTextPaint, _unpickableAction, _redraw,
				defaultUnderline(treeObject, _showCheckBox, notifier));
	}

	static synchronized FacetText getFacetText(Object treeObject, Bungee a,
			double _numY, double _nameY, boolean _showChildIndicator,
			boolean _showCheckBox, boolean _dontHideTransients,
			PickFacetTextNotifier notifier, int onCount,
			Paint _permanentTextPaint, int _unpickableAction, PerspectiveObserver _redraw,
			boolean _underline) {
		Perspective _facet = null;
		Cluster _cluster = null;
		if (treeObject instanceof Perspective) {
			_facet = (Perspective) treeObject;
			_showCheckBox = _showCheckBox && _facet.getParent() != null;
		} else {
			onCount = 1;
			_numY = -1;
			_nameY = -1;
			_showChildIndicator = false;
			_showCheckBox = false;
			if (treeObject instanceof Cluster) {
				_cluster = (Cluster) treeObject;
			} else {
				assert treeObject instanceof String || treeObject instanceof Item : treeObject;
				notifier = null;
			}
		}
		String s = computeText(a, _facet, treeObject, _numY, _nameY, onCount,
				true, _showChildIndicator, _showCheckBox, false, _redraw == null ? NULL_REDRAWER : _redraw);
		boolean _isPickable = _showCheckBox || notifier != null
				|| _cluster != null;

		FacetText result = null;
		Hashtable table = facetTexts == null ? null : (Hashtable) facetTexts.get();
		if (table == null) {
			table = new Hashtable();
			facetTexts = new SoftReference(table);
		}
		List texts = (List) table.get(treeObject);
		if (texts != null) {
			for (Iterator it = texts.iterator(); it.hasNext() && result == null;) {
				FacetText facetLabel = (FacetText) it.next();
				if (facetLabel.getParent() == null && a == facetLabel.art
						&& s.equals(facetLabel.getText())
						&& _underline == facetLabel.underline) {
					// facetLabel.setFont(a.font);
					// facetLabel.setHeight(a.lineH);
					facetLabel.setConstrainWidthToTextWidth(true);
					facetLabel.setConstrainHeightToTextHeight(false);
					facetLabel.selectFacet();
					if (_numY > 0)
						facetLabel.setText(facetLabel.art.facetLabel(
								(Perspective) treeObject, _numY, _nameY,
								onCount, true, _showChildIndicator,
								_showCheckBox, false, facetLabel));
					result = facetLabel;
				}
			}
		} else {
			texts = new ArrayList();
			table.put(treeObject, texts);
		}
		if (result == null) {
			result = new FacetText(treeObject, a, _numY, _nameY,
					_showChildIndicator);
			texts.add(result);
			result.showCheckBox = _showCheckBox;
			result.setUnderline(_underline); // set this before setText for
			// efficiency
			if (treeObject != null) {
				result.setColor(onCount);
				if (_redraw == null && s.indexOf('?') >= 0)
					s = computeText(a, _facet, treeObject, _numY, _nameY,
							onCount, true, _showChildIndicator, _showCheckBox,
							false, result);
				result.setText(s);
			}
		}
		result.redraw = _redraw == null ? result : _redraw;
		result.isPickable = _isPickable;
		result.setPermanentTextPaint(_permanentTextPaint);
		result.unpickableAction = _unpickableAction;
		result.dontHideTransients = _dontHideTransients;
		result.pickFacetTextNotifier = notifier;
		result.numFirst = true;
		return result;
	}

	// underline = isPickable
	// isPickable = showCheckBox
	// showCheckBox = _showCheckBox && _facet != null && _facet.parent != null
	private FacetText(Object treeObject, Bungee a, double numY, double nameY,
			boolean _showChildIndicator) {
		super(a.font);
		if (treeObject instanceof Perspective)
			facet = (Perspective) treeObject;
		else if (treeObject instanceof Cluster)
			cluster = (Cluster) treeObject;
		// if (facet != null) {
		// onCount = facet.getOnCount();
		// if (onCount < 0)
		// onCount = facet.totalCount;
		// }
		art = a;
		numW = numY;
		nameW = nameY;
		showChildIndicator = _showChildIndicator;
		setConstrainHeightToTextHeight(false);
		setHeight(art.lineH);
		setWrapText(false);
		addInputEventListener(facetTextHandler);
	}

	// only called by FacetPText, which keeps its own cache
	 FacetText(Bungee a, double numY, double nameY) {
		super(a.font);
		art = a;
		numW = numY;
		nameW = nameY;
		redraw = this;
		setConstrainHeightToTextHeight(false);
		setHeight(a.lineH);
		setWrapText(false);
		addInputEventListener(facetTextHandler);
	}

	private static boolean defaultUnderline(Object treeObject,
			boolean _showCheckBox, PickFacetTextNotifier notifier) {
		Perspective _facet = null;
		Cluster _cluster = null;
		if (treeObject instanceof Perspective) {
			_facet = (Perspective) treeObject;
			_showCheckBox = _showCheckBox && _facet.getParent() != null;
		} else {
			_showCheckBox = false;
			if (treeObject instanceof Cluster) {
				_cluster = (Cluster) treeObject;
			} else {
				assert treeObject instanceof String || treeObject instanceof Item : treeObject;
				notifier = null;
			}
		}
		return _showCheckBox || notifier != null || _cluster != null;
	}

	static String computeText(Bungee a, Perspective _facet, Object treeObject,
			double _numY, double _nameY, int _onCount, boolean _numFirst,
			boolean _showChildIndicator, boolean _showCheckBox,
			boolean _justify, PerspectiveObserver _redraw) {
		String result = null;
		if (_facet != null)
			result = a.facetLabel(_facet, _numY, _nameY, _onCount, _numFirst,
					_showChildIndicator, _showCheckBox, _justify, _redraw);
		else
			result = treeObject.toString();
		return result;
	}

	public void redraw() {
		// Util.print("\nFT redraw " + facet);
		setText(art.facetLabel(facet, numW, nameW, numFirst, showChildIndicator,
				showCheckBox, true, redraw));
	}

	// public void setText(String s) {
	// String old = getText();
	// if (old != null && !old.equals(s)) {
	// // Util.print("FacetText.setText " + treeObject() + " '" + old + "'
	// // => '" + s + "'");
	// List cached = (List) facetTexts.get(treeObject());
	// if (cached != null)
	// cached.remove(this);
	// // Util.printStackTrace();
	// }
	// super.setText(s);
	// }

	public Perspective getFacet() {
		return facet;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public Object treeObject() {
		if (facet != null)
			return facet;
		else if (cluster != null)
			return cluster;
		else
			return getText();
	}

	void setPermanentTextPaint(Paint paint) {
		permanentTextPaint = paint;
		setColor();
	}

	void selectFacet() {
		setColor(1);
	}

	public void highlightFacet() {
//		setColor(1);  // Why did we use a constant here?
		setColor();
	}

	 void setColor() {
		int onCount = facet != null ? facet.getOnCount() : 1;
		setColor(onCount);
	}

	 void setColor(int onCount) {
//		 Util.print("...setColor " + getText());
		if (permanentTextPaint != null) {
			// Gross hack for MouseDoc line
			setTextPaint(permanentTextPaint);
		} else
			setTextPaint(art.facetTextColor(facet, onCount, cluster));
		updateCheckBox();
	}

	double checkboxOffset() {
		return Math.ceil(art.lineH * 0.6);

	}

	void updateCheckBox() {
//		 Util.print("updateCheckBox " + facet + " " + showCheckBox);
		if (showCheckBox && facet.getParent() != null) {
			double checkXoffset = checkboxOffset();
			double size = Math.ceil(checkXoffset * 0.6666666);
			double textYoffset = Math.round(art.lineH / 8);
			if (checkBox == null) {
				double y = (art.lineH - size) / 2;
				float fadeFactor = 0.8f;

				checkBox = new Button(checkXoffset, y, size, size, null,
						fadeFactor, Bungee.checkFG);
				checkBox.setPickable(false);
				addChild(checkBox);

				xBox = new Button(0, y, size, size, null, fadeFactor, Bungee.xFG);
				xBox.setPickable(false);
				addChild(xBox);
			}
			boolean isFacetRequired = facet.isRestriction(true);
			if (facet.getParent() != null)
//			 Util.print("updateCheckBox " + isFacetRequired + " " + check);
			if (checkmark != null) {
				checkmark.setVisible(isFacetRequired);
			} else if (isFacetRequired) {
//				Util.print("Creating check for " + getText());
				checkmark = new LazyPPath();
				checkmark.setStroke(null);
				checkmark.setPaint(Bungee.checkColor);
				float[] Xs = { 22, 0, 58, 93, 106, 138, 417, 450, 456, 112, 69,
						22 };
				float[] Ys = { 376, 193, 133, 132, 150, 261, 0, 0, 73, 415,
						415, 376 };
				checkmark.setPathToPolyline(Xs, Ys);
				checkmark.setScale(art.lineH / 600);
				checkmark.setOffset(checkXoffset, textYoffset);
				addChild(checkmark);
			}
			boolean isFacetExcluded = facet.isRestriction(false);
			if (xmark != null)
				xmark.setVisible(isFacetExcluded);
			else if (isFacetExcluded) {
				xmark = new LazyPPath();
				xmark.setStroke(null);
				xmark.setPaint(Bungee.xColor);
				float[] Xs = { 0, 100, 27, 90, 178, 316, 370, 232, 321, 228,
						153, 60, 0 };
				float[] Ys = { 355, 229, 72, 24, 147, 0, 61, 220, 334, 411,
						321, 441, 358 };
				xmark.setPathToPolyline(Xs, Ys);
				xmark.setScale(art.lineH / 600);
				xmark.setOffset(0, textYoffset);
				addChild(xmark);
			}
		}
	}

	int getModifiers(PInputEvent e) {
		int result = getModifiersInternal(isPickable ? e.getModifiersEx() : 0,
				e.getPositionRelativeTo(this).getX());
		if (!isPickable && result == 0)
			result = unpickableAction;
		// Util.print("FacetText.getModifiers " + isPickable + " => " + result);
		return result;
	}

	int getModifiersInternal(int modifiers, double x) {
		if (showCheckBox && x < 2 * checkboxOffset()) {
			if (x > checkboxOffset())
				modifiers |= InputEvent.CTRL_DOWN_MASK;
			else {
				modifiers |= ItemPredicate.EXCLUDE_ACTION;
				modifiers &= ~InputEvent.CTRL_DOWN_MASK;
			}
		}
		// Util.print("FacetText.getModifiersInternal " + showCheckBox + " => "
		// + modifiers);
		return modifiers;
	}

	public boolean highlight(boolean state, int modifiers, PInputEvent e) {
		// Util.print("FacetText.highlight " + state + " " + modifiers);
		return highlight(state, getModifiers(e));
	}

	public boolean highlight(boolean state, int modifiers) {
		// Util.print("FT.highlight " + getText());
		if (pickFacetTextNotifier != null
				&& pickFacetTextNotifier.highlight(this, state, modifiers)) {
			return true;
		} else if (facet != null)
			art.highlightFacet(facet, state, modifiers);
		else if (cluster != null)
			art.highlightCluster(cluster, state);
		else
			return false;
		return modifiers >= 0;
	}

	// private boolean shouldUnderline(Perspective[] restrictions) {
	// isPickable = restrictions == null
	// || !Util.isMember(restrictions, facet);
	// return isPickable;
	// }

	public boolean pick(PInputEvent e) {
//		 Util.print("FacetText.pick " + isPickable + " " +
//		 e.getModifiersEx() + " " + getFacet() + " " + pickFacetTextNotifier);
		return pick(getModifiers(e));
	}

	public boolean pick(int modifiers) {
		if (pickFacetTextNotifier != null
				&& pickFacetTextNotifier.pick(this, modifiers)) {
			return true;
		} else if (modifiers >= 0) {
			if (facet != null) {
				art.printUserAction(Bungee.FACET_TREE, facet, modifiers);
				art.toggleFacet(facet, modifiers);
				// highlight(true, modifiers);
			} else if (cluster != null) {
				art.toggleCluster(cluster);
			}
			return true;
		} else
			return false;
	}

	public void mayHideTransients(PNode ignore) {
		if (!dontHideTransients)
			art.mayHideTransients();
	}

	public void mouseMoved(PInputEvent e) {
		int modifiers = getModifiers(e);
		// Util.print("FacetText.mouseMoved " + getModifiers(e));
		if (pickFacetTextNotifier == null
				|| !pickFacetTextNotifier.mouseMoved(this, modifiers)) {
			art.rehighlightFacet(modifiers, true);
		}
	}

	public Bungee art() {
		return art;
	}
}

class FacetTextHandler extends FacetClickHandler {

	public void mouseMoved(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !FacetText.class.isInstance(node))
			node = node.getParent();
		if (node != null) {
			((FacetText) node).mouseMoved(e);
		}
	}

}
