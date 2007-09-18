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
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.PrintArray;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.umd.cs.piccolo.PNode;

public class TextNfacets extends LazyPNode implements PerspectiveObserver {

	private static final long serialVersionUID = 2128482444596441148L;

	private final Bungee art;

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

	private final boolean showCheckBox;

	Paint facetTextPaint;

	private boolean isPickable;

	int unpickableAction = -1;

	private PerspectiveObserver redraw;

	TextNfacets(Bungee _art, Paint _defaultTextPaint, boolean _showCheckBox) {
		defaultTextPaint = _defaultTextPaint;
		art = _art;
		showCheckBox = _showCheckBox;
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
		content = v.compile(art.query.genericObjectLabel);
	}

	 boolean isIncomplete() {
		return incomplete;
	}

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

	public void setPickable(boolean _isPickable) {
		super.setPickable(_isPickable);
		isPickable = _isPickable;
	}

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

	 String toText() {
		return toText(content);
	}

	 static String toText(Markup content) {
		return toText(content, null);
	}

	 static String toText(Markup content, PerspectiveObserver _redraw) {
		Iterator it = content.iterator();
		boolean plural = false;
		StringBuffer result = new StringBuffer();
		while (it.hasNext()) {
			Object o = it.next();
			if (o == Markup.PLURAL_TAG) {
				assert !plural;
				plural = true;
			} else if (o == Markup.NEWLINE_TAG) {
				result.append("\n");
			} else if (o instanceof String) {
				assert !plural;
				result.append(o);
			} else if (o instanceof Perspective) {
				Perspective facet = (Perspective) o;
				String name = _redraw == null ? facet.getNameIfPossible()
						: facet.getName(_redraw);
				result.append(name);
				if (plural) {
					Util.pluralize(result);
					plural = false;
				}
			}
		}
		return result.toString();
	}

	 double layout(double w, double h) {
		// Util.print("TNF.layout " + w + " " + h + " " + wrapText + " " +
		// art.lineH);
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
			Iterator it = content.iterator();
			boolean plural = false;
			Paint color = defaultTextPaint;
			int style = Font.BOLD;
			while (it.hasNext() && !incomplete) {
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
					Paint paint;
					String s;
					Perspective facet = null;
					if (o instanceof String) {
						assert !plural;
						paint = color;
						s = (String) o;
					} else if (o instanceof Cluster) {
						s = o.toString();
						paint = art.clusterTextColor((Cluster) o);
					} else {
						assert o instanceof Perspective : o
								+ PrintArray.printArrayString(content);
						facet = (Perspective) o;
						s = art.facetLabel(facet, -1, -1, false, showCheckBox,
								showCheckBox, false, getRedrawer());
						paint = art.facetTextColor(facet);
					}
					if (plural) {
						s = Util.pluralize(s);
						plural = false;
					}
					APText last = myWrap(s, facet, x, y, w, h, paint, style,
							null);
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

	APText myWrap(String s, Perspective facet, double x, double y, double w,
			double h, Paint paint, int style, ItemPredicate[] restrictions) {
		// Util.print("myWrap '" + s + "' " + x + " " + y + " " + w + " "
		// + wrapText);
		APText result = null;
		if (s.length() > 0) {
			if (y + art.lineH <= h) {
				String prefix = s;
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
				APText text;
				if (facet != null) {
					boolean reallyShowCheckBox = showCheckBox
							&& prefix.charAt(0) == Bungee.checkBoxPrefix.charAt(0);
					text = FacetText.getFacetText(facet, art, -1, -1,
							showCheckBox, reallyShowCheckBox, false, null, 0,
							facetTextPaint, unpickableAction, getRedrawer(),
							underline);
					((FacetText) text).isPickable = isPickable
					// && facet.parent != null
					;
					if (!prefix.equals(text.getText())) {
						text.setText(prefix);
					}
				} else {
					// text = art.oneLineLabel();
					// text.setTextPaint(paint);
					// text.setText(prefix);
					text = FacetText.getFacetText(prefix, art, -1, w - x
							+ art.lineH, false, false, false, null, 0, paint,
							-1, getRedrawer(), underline);
					// text.setPaint(Art.summaryBG);
				}
				addChild(text);
				// if (style == Font.ITALIC)
				// text.setFont(art.italicFont);
				text.setOffset(x, y);
				if (x + text.getWidth() > w) {
					text.setConstrainWidthToTextWidth(false);
					// assert w > x : "'" + prefix + "' " + x + " " + w;
					text.setWidth(w - x);
					prefix = text.getBrokenText();
					if (wrapOnWordBoundaries
							&& text.getText().charAt(prefix.length()) != ' ') {
						int index = prefix.lastIndexOf(' ');
						if (index > 0) {
							prefix = prefix.substring(0, index);
							text.setConstrainWidthToTextWidth(true);
							text.setText(prefix);
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
				}
				// addChild(text);

				if (prefix != s) {
					if (prefix.length() == 0) {
						removeChild(text);
						text = null;
					}
					justifyLine(w);
					int i = prefix.length();
					if (s.charAt(i) == ' ')
						i++;
					String suffix = s.substring(i);
					result = myWrap(suffix, facet, 0, y + art.lineH, w, h,
							paint, style, restrictions);
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
}
