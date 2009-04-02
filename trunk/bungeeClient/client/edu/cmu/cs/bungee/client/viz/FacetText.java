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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.client.query.Query.ItemList;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Button;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;

class FacetText extends APText implements FacetNode, PerspectiveObserver {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

	// boolean isPickable;
	//
	// /**
	// * Only meaningful if !isPickable -1 means do nothing; -2 means open
	// * category
	// */
	// int unpickableAction = -1;

	double numW = -1;

	double nameW = -1;

	boolean padToNameW = false;

	PickFacetTextNotifier pickFacetTextNotifier;

	private boolean numFirst;

	// static Font checkFont;

	static final FacetTextHandler facetTextHandler = new FacetTextHandler();

	// Use this to avoid the non-null assert on getName, in carefully considered
	// places
	private static final PerspectiveObserver NULL_REDRAWER = new TextNfacets(
			null, null, false);

	/**
	 * Called by QueryViz on a String or a Cluster and called by
	 * drawTreeInternal
	 */
	static FacetText getFacetText(Object treeObject, Bungee a,
			boolean _showCheckBox, PickFacetTextNotifier notifier) {
		FacetText result = getFacetText(treeObject, a, -1, -1, false,
				_showCheckBox, 1, null, defaultUnderline(treeObject,
						_showCheckBox, notifier));
		result.pickFacetTextNotifier = notifier;
		return result;
	}

	// static FacetText getFacetText(Object treeObject, Bungee a, double _numW,
	// double _nameW, boolean _showChildIndicator, boolean _showCheckBox,
	// boolean _dontHideTransients, PickFacetTextNotifier notifier,
	// int onCount) {
	// FacetText result = getFacetText(treeObject, a, _numW, _nameW,
	// _showChildIndicator,
	// _showCheckBox, onCount,
	// null, defaultUnderline(treeObject, _showCheckBox, notifier));
	// result.dontHideTransients = _dontHideTransients;
	// return result;
	// }

	/**
	 * @param treeObject
	 *            the Perspective, Cluster, or String represented
	 * @param a
	 *            the application
	 * @param _numW
	 *            the width to make the onCount, or -1 to not show counts.
	 * @param _nameW
	 *            the width to make the name, or -1 for unlimited.
	 * @param _showChildIndicator
	 *            append a down arrow?
	 * @param _showCheckBox
	 *            prepend check- and x- boxes?
	 * @param onCount
	 *            the number to prepend; also used to determine text color,
	 *            unless _permanentTextPaint is non-null
	 * @param _redraw
	 *            call this Redrawer when the treeObject's name becomes known
	 * @param _underline
	 *            underline the text?
	 * @return a FacetText whose text is truncated (not at word boundaries)
	 *         according to numW & nameW, with setConstrainWidthToTextWidth =
	 *         true and setConstrainHeightToTextWidth = false.
	 */
	// This can't be static, because Perspective.equals only looks at the ID, so
	// even when you get new objects (when changing dbs), you get the old ft.
	static FacetText getFacetText(Object treeObject, Bungee a, double _numW,
			double _nameW, boolean _showChildIndicator, boolean _showCheckBox,
			int onCount, PerspectiveObserver _redraw, boolean _underline) {
		// Util.print("getFacetText " + treeObject + " " + _showCheckBox);
		// Cluster _cluster = null;
		_showCheckBox = _showCheckBox && a.getShowCheckboxes();
		if (treeObject instanceof Perspective) {
			_showCheckBox = _showCheckBox
					&& ((Perspective) treeObject).getParent() != null;
		} else {
			onCount = 1;
			// _numW = -1;
			// _nameW = -1;
			_showChildIndicator = false;
			_showCheckBox = false;
			if (treeObject instanceof Cluster) {
				// _cluster = (Cluster) treeObject;
			} else {
				assert treeObject instanceof String
						|| treeObject instanceof Item
						|| treeObject instanceof ItemList : treeObject;
			}
		}
		FacetText result = null;
		List texts = a.lookupFacetText(treeObject);
		if (texts != null) {
			for (Iterator it = texts.iterator(); it.hasNext() && result == null;) {
				FacetText facetLabel = (FacetText) it.next();
				if (facetLabel.getParent() == null
						&& a == facetLabel.art
						&& facetLabel.numW == _numW
						&& facetLabel.nameW == _nameW
						&& facetLabel.padToNameW == false
						&& facetLabel.showChildIndicator == _showChildIndicator
						&& facetLabel.showCheckBox == _showCheckBox
						&& _underline == facetLabel.underline
						&& (_numW < 0 || facetLabel.getText().equals(
								computeText(a, treeObject, _numW, _nameW,
										onCount, true, _showChildIndicator,
										_showCheckBox,
										_redraw == null ? NULL_REDRAWER
												: _redraw)))) {
					facetLabel.setConstrainWidthToTextWidth(true);
					facetLabel.setConstrainHeightToTextHeight(false);
					result = facetLabel;
				}
			}
		} else {
			texts = new ArrayList();
			a.putFacetText(treeObject, texts);
		}
		if (result == null) {
			result = new FacetText(treeObject, a, _numW, _nameW,
					_showChildIndicator, _showCheckBox, _underline, onCount,
					_redraw);
			texts.add(result);
			// Util.print("new FacetText '" + treeObject + "' => '" +
			// result.getText() + "'");
		}
		result.redraw = _redraw == null ? result : _redraw;
		result.permanentTextPaint = null;
		result.setColor();
		result.numFirst = true;
		result.dontHideTransients = false;
		result.pickFacetTextNotifier = null;
		return result;
	}

	/**
	 * TextNfacets.myWrap calls this when it setText's to break on word
	 * boundaries
	 * 
	 * Don't understand why ((APText) foo).setText was calling this
	 */
	public void setTextAndDecache(String text, Object treeObject) {
		if (!Util.equalsNullOK(text, getText())) {
			decache(treeObject);
			super.setText(text);
		}
	}

	void setObject(Object treeObject, Object oldTreeObject) {
		if (!treeObject.equals(treeObject())) {
			decache(oldTreeObject);
			if (treeObject instanceof Perspective) {
				facet = (Perspective) treeObject;
				addInputEventListener(facetTextHandler);
			} else if (treeObject instanceof Cluster) {
				cluster = (Cluster) treeObject;
				addInputEventListener(facetTextHandler);
			} else {
				removeInputEventListener(facetTextHandler);
			}
		}
	}

	void decache(Object treeObject) {
		// Object treeObject = treeObject();
		// Util.print("decache " + treeObject + " " + "'" + getText() + "'");
		List texts = treeObject == null ? null : art
				.lookupFacetText(treeObject);
		assert treeObject != null;
		if (texts != null) {
			// the lookup table is a SoftReference, so there's no guarantee that
			// this is in it
			texts.remove(this);
		}
	}

	// underline = isPickable
	// isPickable = showCheckBox
	// showCheckBox = _showCheckBox && _facet != null && _facet.parent != null
	protected FacetText(Object treeObject, Bungee a, double _numW,
			double _nameW, boolean _showChildIndicator, boolean _showCheckBox,
			boolean _underline, int onCount, PerspectiveObserver _redraw) {
		super(a.font);
		if (treeObject instanceof Perspective) {
			facet = (Perspective) treeObject;
			addInputEventListener(facetTextHandler);
		} else if (treeObject instanceof Cluster) {
			cluster = (Cluster) treeObject;
			addInputEventListener(facetTextHandler);
		}
		// if (facet != null) {
		// onCount = facet.getOnCount();
		// if (onCount < 0)
		// onCount = facet.totalCount;
		// }
		art = a;
		numW = _numW;
		nameW = _nameW;
		showChildIndicator = _showChildIndicator;
		setConstrainHeightToTextHeight(false);
		setHeight(art.lineH);
		setWrapOnWordBoundaries(false);

		showCheckBox = _showCheckBox;
		setUnderline(_underline); // set this before setText for
		// efficiency

		String s = computeText(a, treeObject, _numW, _nameW, onCount, true,
				_showChildIndicator, _showCheckBox, _redraw == null ? this
						: _redraw);
		super.setText(s);
	}

	// only called by FacetPText, which keeps its own cache
	FacetText(Bungee a, double _numW, double _nameW) {
		super(a.font);
		art = a;
		numW = _numW;
		nameW = _nameW;
		padToNameW = true;
		redraw = this;
		setConstrainHeightToTextHeight(false);
		setHeight(a.lineH);
		setWrapOnWordBoundaries(false);
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
				assert treeObject instanceof String
						|| treeObject instanceof Item
						|| treeObject instanceof ItemList : treeObject;
				notifier = null;
			}
		}
		return _showCheckBox || notifier != null || _cluster != null;
	}

	static String computeText(Bungee a, Object treeObject, double _numW,
			double _nameW, int _onCount, boolean _numFirst,
			boolean _showChildIndicator, boolean _showCheckBox,
			PerspectiveObserver _redraw) {
		String result = null;
		if (treeObject instanceof ItemPredicate)
			result = a.facetLabel((ItemPredicate) treeObject, _numW, _nameW,
					_onCount, _numFirst, _showChildIndicator, _showCheckBox,
					false, _redraw);
		else if (_nameW >= 0)
			result = a.truncateText(treeObject.toString(), _nameW);
		else
			result = treeObject.toString();
		// Util.print("ComputeText " + treeObject + " => " + result);
		return result;
	}

	public void redraw() {
		super
				.setText(art.facetLabel((ItemPredicate) treeObject(), numW,
						nameW, numFirst, showChildIndicator, showCheckBox,
						padToNameW, redraw));
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

	Cluster getCluster() {
		return cluster;
	}

	Object treeObject() {
		if (facet != null)
			return facet;
		else if (cluster != null)
			return cluster;
		else
			return getText();
	}

	void setPermanentTextPaint(Paint paint) {
		// Util.print("FacetText.setPermanentTextPaint " + paint + " " + this);
		if (paint != permanentTextPaint) {
			permanentTextPaint = paint;
			setColor();
		}
	}

	void selectFacet() {
		setColor();
	}

	void highlightFacet() {
		// setColor(1); // Why did we use a constant here?
		setColor();
	}

	// void setColor() {
	// int onCount = facet != null ? facet.guessOnCount() : 1;
	// setColor(onCount);
	// }

	void setColor() {
		// Util.print("FT.setColor "+this+" "+permanentTextPaint);
		if (permanentTextPaint != null) {
			// Util.print("...setColor " + getText() + " " +
			// permanentTextPaint);
			// Gross hack for MouseDoc line
			setTextPaint(permanentTextPaint);
		} else
			setTextPaint(art.facetTextColor(facet, cluster));
		updateCheckBox();
	}

	double checkboxOffset() {
		return Math.ceil(art.lineH * 0.6);
	}

	void updateCheckBox() {
		// Util.print("updateCheckBox " + facet + " " + showCheckBox);
		if (showCheckBox && facet != null && facet.getParent() != null) {
			double checkXoffset = checkboxOffset();
			double size = Math.ceil(checkXoffset * 0.7 /* 0.9 */);
			double textYoffset = Math.round(art.lineH / 8);
			if (checkBox == null) {
				double y = (art.lineH - size) / 2;
				float fadeFactor = 0;// .8f;

				checkBox = new Button(checkXoffset, y, size, size, null, null,
						fadeFactor, Bungee.checkFG);
				checkBox.setPickable(false);
				checkBox.setState(false);
				addChild(checkBox);

				xBox = new Button(0, y, size, size, null, null, fadeFactor,
						Bungee.xFG);
				xBox.setPickable(false);
				xBox.setState(false);
				addChild(xBox);
			}
			boolean isFacetRequired = facet.isRestriction(true);
			if (facet.getParent() != null)
				// Util.print("updateCheckBox " + isFacetRequired + " " +
				// check);
				if (checkmark != null) {
					checkmark.setVisible(isFacetRequired);
				} else if (isFacetRequired) {
					// Util.print("Creating check for " + getText());
					checkmark = new LazyPPath();
					checkmark.setStroke(null);
					checkmark.setPaint(Bungee.checkColor);
					float[] Xs = { 22, 0, 58, 93, 106, 138, 417, 450, 456, 112,
							69, 22 };
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

	// point of information: facet labels' isPickable = whether they are
	// connected.
	int getModifiers(int modifiers, PInputEvent e) {
		int result = getModifiersInternal(
		/* isPickable ? */modifiers /* : 0 */, e.getPositionRelativeTo(this)
				.getX());
		// if (!isPickable && result == 0)
		// result = unpickableAction;
		// Util.print("FacetText.getModifiers " + isPickable + " => " + result);
		assert result >= 0;
		return result;
	}

	/**
	 * @param modifiers
	 *            normal modifiers from mouse gesture
	 * @param x
	 *            local x-coordinate of gesture
	 * @return modifiers + implicit modifiers from being over the check- or
	 *         x-box.
	 */
	int getModifiersInternal(int modifiers, double x) {
		// Util.print("FacetText.getModifiersInternal " + facet + " " +
		// showCheckBox + " " + modifiers);
		if (showCheckBox && x < 2 * checkboxOffset()) {
			if (x > checkboxOffset())
				modifiers |= InputEvent.CTRL_DOWN_MASK;
			else {
				modifiers |= ItemPredicate.EXCLUDE_ACTION;
				modifiers &= ~InputEvent.CTRL_DOWN_MASK;
			}
		}
		// Util.print(" FacetText.getModifiersInternal return " + showCheckBox +
		// " => "
		// + modifiers);
		return modifiers;
	}

	public boolean highlight(boolean state, int modifiers, PInputEvent e) {
		// Util.print("FacetText.highlight " + state + " " + modifiers);
		return highlightInternal(state, getModifiers(modifiers, e));
	}

	boolean mouseMoved(int modifiers, PInputEvent e) {
		return highlightInternal(true, getModifiers(modifiers, e));
	}

	boolean highlightInternal(boolean state, int modifiers) {
		// Util.print("FT.highlight " + state + " " + getText());
		if (pickFacetTextNotifier != null
				&& pickFacetTextNotifier.highlight(this, state, modifiers)) {
			return true;
		} else if (facet != null) {
			art.highlightFacet(state ? facet : null, modifiers);
			art.setClickDesc(state ? facet.facetDoc(modifiers) : null);
		} else if (cluster != null)
			art.highlightCluster(state ? cluster : null);
		else
			return false;
		return true;
	}

	public boolean pick(int modifiers, PInputEvent e) {
		modifiers = getModifiers(modifiers, e);
		if (pickFacetTextNotifier != null
				&& pickFacetTextNotifier.pick(this, modifiers)) {
			// already handled - don't do anything here.
		} else if (facet != null && facet.getParent() != null) {
			printUserAction(modifiers);
			art.toggleFacet(facet, modifiers);
			// highlight(true, modifiers);
		} else if (cluster != null) {
			art.toggleCluster(cluster);
		} else
			return false;
		return true;
	}

	public void mayHideTransients(PNode ignore) {
		if (!dontHideTransients)
			art.mayHideTransients();
	}

	public void drag(Point2D position, PDimension delta) {
		assert false;
	}

	public FacetNode startDrag(PNode node, Point2D positionRelativeTo) {
		// TODO Auto-generated method stub
		return null;
	}

	// boolean shiftKeysChanged(int modifiers, PInputEvent e) {
	// return highlight(true, modifiers);
	// }

	void printUserAction(int modifiers) {
		art.printUserAction(Bungee.FACET_TREE, facet, modifiers);
	}

	public String toString() {
		return "<FacetText " + treeObject() + " (" + treeObject().getClass()
				+ ")>";
	}

	public Bungee art() {
		return art;
	}
}

class FacetTextHandler extends FacetClickHandler {

	protected boolean moved(PNode node, int modifiers, PInputEvent e) {
		return ((FacetText) node).mouseMoved(modifiers, e);
	}

	// protected boolean shiftKeysChanged(PNode node, int modifiers, PInputEvent
	// e) {
	// // Util.print("FT.shiftKeysChanged");
	// return ((FacetText) node).shiftKeysChanged(modifiers, e);
	// }

}
