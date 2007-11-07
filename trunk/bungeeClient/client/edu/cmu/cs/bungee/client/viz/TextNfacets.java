/* 

 Created on Mar 27, 2006

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
import java.awt.Font;
import java.awt.Paint;
import java.util.Iterator;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.umd.cs.piccolo.PNode;

/**
 * Used by Summary.summaryText, facet labels, mouse doc & popups
 * 
 */
final class TextNfacets extends LazyPNode implements PerspectiveObserver {

	private final Bungee art;

	/**
	 * Default text paint for Strings. Ignored for ItemPredicates.
	 */
	private final Paint defaultTextPaint;

	private Markup content = Query.emptyMarkup();

	// static final double margin = 0;

	private double trimW = -1;

	private double trimH = -1;

	private double untrimmedW;

	private double untrimmedH;

	static final int NO_ANCHOR = -1;

	static final int BOTTOM_LEFT = 6;

	static final int BOTTOM_CENTER = 7;

	int anchor = NO_ANCHOR;

	private double anchorX;

	private double anchorY;

	private boolean incomplete;

	private boolean wrapText = false;

	private boolean wrapOnWordBoundaries = false;

	private boolean underline;

	private float justification = Component.LEFT_ALIGNMENT;

	/**
	 * Only rank labels set this true
	 */
	private final boolean showCheckBoxAndChildIndicator;

	/**
	 * Overrides Art.facetTextColor();
	 */
	Paint facetPermanentTextPaint;

	// private boolean isPickable;
	//
	// int unpickableAction = -1;

	private PerspectiveObserver redraw;

	TextNfacets(Bungee _art, Paint _defaultTextPaint,
			boolean _showCheckBoxAndChildIndicator) {
		defaultTextPaint = _defaultTextPaint;
		art = _art;
		showCheckBoxAndChildIndicator = _showCheckBoxAndChildIndicator;
	}

	// public void add(Object stringOrFacet) {
	// assert stringOrFacet instanceof String
	// || stringOrFacet instanceof Perspective;
	// content.add(stringOrFacet);
	// }

	// public void clear() {
	// content.clear();
	// }

	void setContent(Markup v) {
		// Util.print("TNF.setContent input: " + v);
		assert v != null;
		assert art != null;
		if (query() != null) {
			assert query().genericObjectLabel != null;
			content = v.compile(query().genericObjectLabel);
		}
	}

	boolean isIncomplete() {
		return incomplete;
	}

	/**
	 * If set to false, will ignore newlines in content
	 * 
	 * @param isWrap
	 */
	void setWrapText(boolean isWrap) {
		wrapText = isWrap;
	}

	void setWrapOnWordBoundaries(boolean isWrap) {
		wrapOnWordBoundaries = isWrap;
	}

	void setJustification(float _justification) {
		justification = _justification;
	}

	void setUnderline(boolean isUnderline) {
		underline = isUnderline;
		for (int i = 0; i < getChildrenCount(); i++) {
			((APText) getChild(i)).setUnderline(underline);
		}
	}

	// public void setPickable(boolean _isPickable) {
	// super.setPickable(_isPickable);
	// isPickable = _isPickable;
	// }

	void setAnchor(int anchorType, double x, double y) {
		anchor = anchorType;
		anchorX = x;
		anchorY = y;
	}

	void setRedrawer(PerspectiveObserver _redraw) {
		redraw = _redraw;
	}

	PerspectiveObserver getRedrawer() {
		return redraw == null ? this : redraw;
	}

	double layoutBestFit() {
		// System.out.println("layoutBestFit " + content);
		double result;
		if (getHeight() < 2 * art.lineH) {
			setWrapText(false);
			result = layout();
		} else {
			setWrapText(true);
			result = layout();
			if (isIncomplete()) {
				setWrapText(false);
				result = layout();
			}
		}
		setHeight(result);
		return result;
	}

	double layout() {
		return layout(getWidth(), getHeight());
	}

	public void redraw() {
		// Util.print("\ntNf redraw " + toText());
		layout(untrimmedW, untrimmedH);
	}

	// String toText() {
	// return content.toText();
	// }

	public String toString() {
		return "<TextNfacets " + content + ">";
	}

	boolean isEmpty() {
		return content.toText().trim().length() == 0;
	}

	double layout(double w, double h) {
		// Util.print("TNF.layout " + w + " " + h + " " + wrapText + " "
		// + art.lineH + " " + wrapOnWordBoundaries);
		// Util.print(content);
		untrimmedW = w;
		untrimmedH = h;
		removeAllChildren();
		if (w <= 0) {
			incomplete = content.size() > 0;
			return 0;
		} else {
			incomplete = false;
			double x = 0;
			double y = 0;
			boolean plural = false;
			Paint color = defaultTextPaint;
			int style = Font.BOLD;
			for (Iterator it = content.iterator(); it.hasNext() && !incomplete;) {
				Object o = it.next();
				if (o == Markup.PLURAL_TAG) {
					assert !plural;
					plural = true;
				} else if (o == Markup.NEWLINE_TAG) {
					if (wrapText) {
						justifyLine(w);
						x = 0;
						y += art.lineH;
					}
				} else if (o == Markup.DEFAULT_STYLE_TAG) {
					style = Font.BOLD;
				} else if (o == Markup.ITALIC_STRING_TAG) {
					style = Font.ITALIC;
				} else if (o == Markup.DEFAULT_COLOR_TAG) {
					color = defaultTextPaint;
				} else if (o instanceof Color) {
					color = (Color) o;
				} else {
					Paint paint = null;
					String s;
					ItemPredicate facet = null;
					if (o instanceof String) {
						assert !plural;
						paint = color;
						s = (String) o;
					} else if (o instanceof Cluster) {
						s = o.toString();
						paint = art.clusterTextColor((Cluster) o);
					} else {
						assert o instanceof ItemPredicate : o
								+ Util.valueOfDeep(content);
						facet = (ItemPredicate) o;
						s = art.facetLabel(facet, -1, -1, false,
								showCheckBoxAndChildIndicator,
								showCheckBoxAndChildIndicator, false,
								getRedrawer());

						// paint is ignored for Perspectives. getFacetText will
						// compute color.
						// paint = art.facetTextColor(facet);
					}
					if (plural) {
						s = Util.pluralize(s);
						plural = false;
					}
					APText last = myWrap(s, facet, x, y, w, h, paint, style,
							null, true);
					if (last != null) {
						y = last.getYOffset();
						x = last.getXOffset() + Math.ceil(last.getWidth());
					}
				}
			}
			trim();
			justifyLine(w);
			anchor();
			// Util.print("layout return " + (y + art.lineH) + " " +
			// incomplete);
			return y + art.lineH;
		}
	}

	APText myWrap(String s, ItemPredicate facet, double x, double y, double w,
			double h, Paint paint, int style, ItemPredicate[] restrictions,
			boolean isFirstLine) {
		// Util.print("myWrap '" + s + "' " + x + " " + y + " " + w + " "
		// + wrapOnWordBoundaries + " " + isFirstLine);
		APText result = null;
		if (s.length() > 0) {
			if (y + art.lineH <= h) {
				// String prefix = s;
				// int index = 9999;
				// if (wrapText)
				// while (index > 0 && x + art.getStringWidth(prefix) > w) {
				// index = prefix.lastIndexOf(' ');
				// if (index > 0)
				// prefix = prefix.substring(0, index);
				// }
				// else if (x + art.getStringWidth(s.substring(0, 1)) > w) {
				// index = 0;
				// // justifyLine(w);
				// // incomplete = true;
				// // return result;
				// }
				// if (index < 1) {
				// justifyLine(w);
				// // Nothing more will fit on this line.
				// result = myWrap(s, facet, 0, y + art.lineH, w, h, paint,
				// style,
				// restrictions);
				// } else {
				FacetText text;
				// on recursive calls, s is just the tail end of the facet name,
				// possibly with checkbox prefix and childindicator suffix
				if (facet != null && isFirstLine
						&& strip(s).equals(strip(facet.getNameIfPossible()))) {

					// // if we break the text, second part shouldn't show check
					// // box
					// boolean reallyShowCheckBox =
					// showCheckBoxAndChildIndicator
					// && s.startsWith(Bungee.checkBoxPrefix);
					text = FacetText.getFacetText(facet, art, -1, w - x,
							showCheckBoxAndChildIndicator,
							showCheckBoxAndChildIndicator,
							facet.guessOnCount(), getRedrawer(), underline);
					text.setPermanentTextPaint(facetPermanentTextPaint);
					// ((FacetText) text).isPickable = isPickable
					// && facet.parent != null
					// if (!prefix.equals(text.getText())) {
					// text.setText(s);
					// }
				} else if (facet != null) {
					text = FacetText.getFacetText(s, art, -1, w - x,
							showCheckBoxAndChildIndicator, false, 0,
							getRedrawer(), underline);
					text.setObject(facet, s);
					text.setPermanentTextPaint(facetPermanentTextPaint);
					text.setColor();
				} else {
					// text = art.oneLineLabel();
					// text.setTextPaint(paint);
					// text.setText(prefix);
					text = FacetText.getFacetText(s, art, -1, w - x, false,
							false, 0, getRedrawer(), underline);
					text.setPermanentTextPaint(paint);
					// text.setPaint(Art.summaryBG);
				}
				addChild(text);

				// if (style == Font.ITALIC)
				// text.setFont(art.italicFont);
				text.setOffset(x, y);
				String textString = text.getText();
				// Util.print("___" + textString + " " + x + ", " + y);
				if (!textString.equals(s)) {
					assert textString.length() < s.length() : textString + " "
							+ s + " " + facet;
					// if (x + text.getWidth() > w) {
					// text.setConstrainWidthToTextWidth(false);
					// // assert w > x : "'" + prefix + "' " + x + " " + w;
					// text.setWidth(w - x);
					// prefix = text.getBrokenText();
					if (wrapOnWordBoundaries
							&& s.charAt(textString.length()) != ' '
							&& !s.startsWith(Bungee.childIndicatorSuffix)) {
						int index = textString.lastIndexOf(' ');
						if (index > 0) {
							if (x > 0
									&& s
											.startsWith(Bungee.childIndicatorSuffix)) {
								// Don't break on childPrefix spaces
								textString = "";
							} else {
								textString = s.substring(0, index);
								// Util.print("FT.st " + textString);
								assert textString.trim().length() > 0;
								// text.setConstrainWidthToTextWidth(true);
								text.setTextAndDecache(textString,
										facet != null ? facet : (Object) s);
							}
							// text.setPaint(Color.red);
							// Util.print("... " + prefix);
							// Util.print(">>> " + text.getWidth() + " "
							// + art.getStringWidth(text.getText()) + " "
							// + text.getText());
							// Util.print(s.substring(index));
						}
					}
					// if (x > 0)
					// incomplete = true;
					// }
					// addChild(text);

					// if (prefix != s) {
					if (textString.length() == 0) {
						removeChild(text);
						text = null;
					}
					justifyLine(w);
					// int i = textString.length();
					// if (s.charAt(i) == ' ')
					// i++;
					// Util.print("cc '" + textString + "'");
					String suffix = s.substring(textString.length());
					if (!s.startsWith(Bungee.checkBoxPrefix))
						suffix = suffix.trim();
					result = myWrap(suffix, facet, 0, y + art.lineH, w, h,
							paint, style, restrictions, false);
				}
				if (result == null)
					result = text;
				// }
			} else
				incomplete = true;
		}
		// Util.print("myWrap return ");
		return result;
	}

	private String strip(String embroderedName) {
		if (embroderedName == null)
			return null;
		String result = embroderedName.trim();
		result = result.split(Bungee.childIndicatorSuffix)[0];
		return result;
	}

	private void justifyLine(double w) {
		if (justification != Component.LEFT_ALIGNMENT) {
			int nChildren = getChildrenCount();
			if (nChildren > 0) {
				PNode last = getChild(nChildren - 1);
				double y = last.getYOffset();
				double x = last.getXOffset() + Math.ceil(last.getWidth());
				double offset = w - x;
				if (justification == Component.CENTER_ALIGNMENT)
					offset = Math.round(offset / 2.0);
				for (int i = 0; i < nChildren; i++) {
					PNode child = getChild(i);
					if (child.getYOffset() == y)
						child.setOffset(child.getXOffset() + offset, y);
				}
			}
		}
	}

	void setTrim(int Xmargin, int Ymargin) {
		trimW = Xmargin;
		trimH = Ymargin;
	}

	private void trim() {
		// Util.print("trim " + content.toString());
		if (trimW >= 0 || trimH >= 0) {
			double w = 0;
			double h = 0;
			for (int i = 0; i < getChildrenCount(); i++) {
				PNode child = getChild(i);
				double right = child.getXOffset() + Math.ceil(child.getWidth());
				if (right > w)
					w = right;
				double top = child.getYOffset() + child.getHeight();
				if (top > h)
					h = top;
			}
			if (trimW >= 0)
				setWidth(w + trimW);
			if (trimH >= 0)
				setHeight(h + trimH);
			// anchor();
		}
	}

	void anchor() {
		switch (anchor) {
		case BOTTOM_LEFT:
			setOffset(anchorX, anchorY - getHeight());
			break;
		case BOTTOM_CENTER:
			setOffset(anchorX - (int) getWidth() / 2, anchorY - getHeight());
			break;
		default:
			assert anchor == NO_ANCHOR;
		}
	}

	void updateSelections(ItemPredicate facet) {
		for (int i = 0; i < getChildrenCount(); i++) {
			if (getChild(i) instanceof FacetText) {
				FacetText child = (FacetText) getChild(i);
				if (child.getFacet() == facet) {
					child.highlightFacet();
					// prevSelection = facet;
					// } else if (child.facet == prevSelection) {
					// child.highlightFacet(false, true);
					// prevSelection = null;
				}
			}
		}
	}

	void updateSelections(Cluster facet) {
		for (int i = 0; i < getChildrenCount(); i++) {
			if (getChild(i) instanceof FacetText) {
				FacetText child = (FacetText) getChild(i);
				if (child.getCluster() == facet) {
					child.highlightFacet();
					// prevSelection = facet;
					// } else if (child.facet == prevSelection) {
					// child.highlightFacet(false, true);
					// prevSelection = null;
				}
			}
		}
	}

	Query query() {
		return art.query;
	}
}
