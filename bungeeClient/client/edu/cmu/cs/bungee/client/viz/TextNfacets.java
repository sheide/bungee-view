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
import java.util.Set;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.umd.cs.piccolo.PNode;

/**
 * Used by Summary.summaryText, facet labels, mouse doc & popups
 * 
 */
class TextNfacets extends LazyPNode implements PerspectiveObserver {

	 final Bungee art;

	/**
	 * Default text paint for Strings. Ignored for ItemPredicates.
	 */
	private final Paint defaultTextPaint;

	protected Markup content = Query.emptyMarkup();

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

	// private boolean underline;

	private float justification = Component.LEFT_ALIGNMENT;

	/**
	 * Only rank labels set this true. This is implicitly ANDed with
	 * features.checkbox
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

	/**
	 * @param v
	 * @return whether the content changed
	 */
	boolean setContent(Markup v) {
		// Util.print("TNF.setContent input: " + v);
		boolean result = false;
		assert v != null;
		assert art != null;
		if (query() != null) {
			assert query().getGenericObjectLabel(true) != null;
			Markup newV = v.compile(query().getGenericObjectLabel(true));
			if (!newV.equals(content)) {
				content = newV;
				result = true;
			}
		}
		return result;
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

	// void setUnderline(boolean isUnderline) {
	// underline = isUnderline;
	// for (int i = 0; i < getChildrenCount(); i++) {
	// ((APText) getChild(i)).setUnderline(underline);
	// }
	// }

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
//		Util.print("TNF.layout " + w + " " + h + " " + wrapText + " "
//				+ art.lineH + " " + wrapOnWordBoundaries);
//		Util.print(content);
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
			boolean underline = false;
			Paint color = defaultTextPaint;
			int style = Font.BOLD;
			for (Iterator it = content.iterator(); it.hasNext() && !incomplete;) {
				Object o = it.next();
				if (o == Markup.PLURAL_TAG) {
					assert !plural;
					plural = true;
				} else if (o == Markup.UNDERLINE_TAG) {
					assert !underline;
					underline = true;
				} else if (o == Markup.NO_UNDERLINE_TAG) {
					assert underline;
					underline = false;
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
					Object facet = null;
					if (o instanceof String) {
						assert !plural;
						paint = color;
						s = (String) o;
					} else if (o instanceof Cluster) {
						s = o.toString();
					} else if (o instanceof SearchText) {
						s = o.toString();
						paint = color;
						facet = o;
					} else {
						assert o instanceof ItemPredicate : o
								+ Util.valueOfDeep(content);
						facet = o;
						s = art.facetLabel((ItemPredicate) o, -1, -1, false,
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
							underline, null, true);
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

	APText myWrap(final String s, Object facet, double x, double y,
			double w, double h, Paint paint, int style, boolean underline,
			ItemPredicate[] restrictions, boolean isFirstLine) {
//		Util.print("myWrap '" + s + "' x=" + x + " y=" + y + " w=" + w
//				+ " wrapOnWordBoundaries=" + wrapOnWordBoundaries
//				+ " isFirstLine=" + isFirstLine + " " + facet);
		APText result = null;
		if (s.length() > 0) {
			if (y + art.lineH <= h) {
				FacetText text;
				// on recursive calls, s is just the tail end of the facet name,
				// possibly with checkbox prefix and childindicator suffix
				if (facet instanceof ItemPredicate && isFirstLine
						&& strip(s).equals(strip(((ItemPredicate) facet).getNameIfPossible()))) {
					// // if we break the text, second part shouldn't show check
					// // box
					// boolean reallyShowCheckBox =
					// showCheckBoxAndChildIndicator
					// && s.startsWith(Bungee.checkBoxPrefix);
					text = FacetText.getFacetText(facet, art, -1, w - x,
							showCheckBoxAndChildIndicator,
							showCheckBoxAndChildIndicator,
							((ItemPredicate) facet).guessOnCount(), getRedrawer(), underline);
					text.setPermanentTextPaint(facetPermanentTextPaint);
					// ((FacetText) text).isPickable = isPickable
					// && facet.parent != null
					// if (!prefix.equals(text.getText())) {
					// text.setText(s);
					// }
				} else if (facet instanceof SearchText) {
					text = (FacetText) facet;
					text.setPermanentTextPaint(facetPermanentTextPaint);
				} else if (facet != null) {
					text = FacetText.getFacetText(s, art, -1, w - x,
							showCheckBoxAndChildIndicator, false, 0,
							getRedrawer(), underline);
					text.setObject(facet, s);
					text.setPermanentTextPaint(facetPermanentTextPaint);
				} else {
					text = FacetText.getFacetText(s, art, -1, w - x, false,
							false, 0, getRedrawer(), underline);
					text.setPermanentTextPaint(paint);
				}
				addChild(text);

				// if (style == Font.ITALIC)
				// text.setFont(art.italicFont);
				text.setOffset(x, y);
				String textString = text.getText();
				// Util.print("___ '" + textString + "' " + x + ", " + y + " '"
				// + s + "' " + textString.equals(s));
				if (!textString.equals(s)) {
					assert textString.length() < s.length() : "'" + textString
							+ "' '" + s + "' " + facet + " " + isFirstLine;
					if (textString.endsWith(Bungee.childIndicatorSuffix)) {
						// getFacetText will truncate the name and add
						// childIndicatorSuffix.
						// We're going to add the rest of the name on the next
						// line, so undo the suffix.
						textString = textString.substring(0, textString
								.length() - 1);
						text.setTextAndDecache(textString,
								facet != null ? facet : (Object) s);
					}
					if (wrapOnWordBoundaries
							&& s.charAt(textString.length()) != ' '
							&& !s.startsWith(Bungee.childIndicatorSuffix)) {
						int index = textString.lastIndexOf(' ');
						if (s.startsWith(Bungee.checkBoxPrefix)
								&& index == Bungee.checkBoxPrefix.length() - 1)
							index = -1;
						if (index > 0) {
							textString = s.substring(0, index);

							// If a facet name has a lot of spaces in it, go
							// ahead and treat them as normal characters.
							// assert textString.trim().length() > 0;

							text.setTextAndDecache(textString,
									facet != null ? facet : (Object) s);
						} else if (x > 0) {
							textString = "";
						}
					}
					if (textString.length() == 0) {
						removeChild(text);
						text = null;
					}
					justifyLine(w);
					if (s.length() > textString.length()) {
						// optimize common case since substring is slow
						int index = textString.length();
						if (!s.startsWith(Bungee.checkBoxPrefix)) {
							while (index < s.length() && s.charAt(index) == ' ')
								index++;
						}
						String suffix = s;
						if (index > 0) {
							suffix = s.substring(index);
							// If facet name starts with spaces, and
							// isFirstLine, the call to myWrap below will barf
							// because s won't match text.getText()
							isFirstLine = false;
						}

						result = myWrap(suffix, facet, 0, y + art.lineH, w, h,
								paint, style, underline, restrictions,
								isFirstLine && textString.length() == 0);
					}
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

	private static final Pattern CHILD_INDICATOR_PATTERN = Pattern
			.compile(Bungee.childIndicatorSuffix);

	private String strip(String embroderedName) {
		if (embroderedName == null)
			return null;
		String result = embroderedName.trim();
		result = CHILD_INDICATOR_PATTERN.split(result)[0];
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

	protected void trim() {
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

	void updateSelections(Set facets) {
		for (Iterator it = getChildrenIterator(); it.hasNext();) {
			PNode node = (PNode) it.next();
			if (node instanceof FacetText) {
				FacetText child = (FacetText) node;
				Perspective childFacet = child.getFacet();
				if (childFacet != null && facets.contains(childFacet)) {
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
