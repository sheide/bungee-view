/* 

 Created on Feb 20, 2006

 The Bungee View applet lets you search, browse, and data-mine an image collection.  
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
package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TextMeasurer;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Iterator;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;
import edu.umd.cs.piccolox.PFrame;

/**
 * @author mad
 * 
 */
public class Util {

	private Util() {
		// Disallow instantiation
	}

	void showFonts(PFrame frame, int textH) {
		String[] fonts = java.awt.GraphicsEnvironment
				.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int i = 0; i < fonts.length; i++) {
			APText s = new APText();
			s.setText("Location of Origin " + fonts[i]);
			s.setOffset(5, textH * i);
			Font f = new Font(fonts[i], Font.PLAIN, textH);
			// PrintArray.printArray(f.getAvailableAttributes());
			s.setFont(f);

			frame.getCanvas().getLayer().addChild(s);
			System.out.println(fonts[i]);
		}
	}

	void showChars(PFrame frame, char start, int n, Font f) {
		PText s = new PText();
		s.setFont(f);
		s.setConstrainWidthToTextWidth(false);
		s.setWidth(frame.getCanvas().getLayer().getWidth());
		StringBuffer buf = new StringBuffer(n);
		for (char i = start; n > 0; i++) {
			if (Character.getType(i) == Character.MATH_SYMBOL
					&& f.canDisplay(i)) {
				n--;
				buf.append(i);
			}
		}
		s.setText(buf.toString());
		System.out.println(buf.toString());
		frame.getCanvas().getLayer().addChild(s);
	}

	public static void printDescendents(PNode node) {
		printDescendents(node, 5, false);
	}

	public static void printDescendents(PNode node, int maxChildren,
			boolean invalidOnly) {
		printAncestors(node);
		if (node.getChildrenCount() > 0)
			printDescendentsInternal(node, "", maxChildren, invalidOnly);
		else {
			System.out.println(nodeDesc(node) + " " + node.getScale() + " "
					+ node.getBounds());
			System.out.print("  <no descendents");
		}
	}

	public static void printAncestors(PNode node) {
		PNode each = node.getParent();
		if (each != null)
			printAncestorsInternal(each, "  ");
		else
			System.out.print("  <no ancestors>");
	}

	public static void printAncestorsInternal(PNode node, String indent) {
		PNode each = node.getParent();
		if (each != null)
			printAncestorsInternal(each, indent + "  ");
		String name = nodeDesc(node);
		System.out.println(indent + name + " " + node.getScale() + " "
				+ node.getBounds());
	}

	public static String nodeDesc(PNode node) {
		String name = "";
		try {
			if (node instanceof PText) {
				PText tnode = (PText) node;
				name = "'" + tnode.getText() + "' " + tnode.getTextPaint();
			} else
				name = node.getClass().getName();
		} catch (java.lang.Exception se) {
			se.printStackTrace(System.out);
		}
		return name;
	}

	static void printDescendentsInternal(PNode node, String indent,
			int maxChildren, boolean invalidOnly) {
		int nChildren = node.getChildrenCount();
		String name = nodeDesc(node);
		System.out.println(indent + name + " " + node.getScale() + " "
				+ node.getBounds());
		Iterator i = node.getChildrenIterator();
		int index = 0;
		while (i.hasNext() && index < maxChildren) {
			PNode each = (PNode) i.next();
			if (!invalidOnly || each.getPaintInvalid()) {
				printDescendentsInternal(each, indent + "  ", maxChildren,
						invalidOnly);
				index++;
			}
		}
		if (nChildren > index && !invalidOnly)
			System.out.println(indent + "..." + (nChildren - index) + " more");
	}

	// public static String truncateTextx(String name, double w, Font font) {
	// //Util.print(w + " " + getStringWidth(s) + " " + s);
	// if (getStringWidth(name, font) <= w)
	// return name;
	// else {
	// String sub = name;
	// int l = name.length();
	// for (int i = l - 2; i >= 0; i--) {
	// sub = name.substring(0, i);
	// if (getStringWidth(sub, font) <= w)
	// return sub;
	// }
	// return sub;
	// }
	// }

	public static String truncateText(String text, float availableWidth,
			Font font) {
		String result = null;
		if (text != null && text.length() > 0) {
			// System.out.println("trucateText " + text);
			AttributedString atString = new AttributedString(text);
			atString.addAttribute(TextAttribute.FONT, font);
			AttributedCharacterIterator itr = atString.getIterator();
			TextMeasurer measurer = new TextMeasurer(itr,
					PPaintContext.RENDER_QUALITY_HIGH_FRC);
			int charAtMaxAdvance = measurer
					.getLineBreakIndex(0, availableWidth);
			result = text.substring(0, charAtMaxAdvance);
		}
		return result;
	}

	public static float getStringWidth(String text, Font font) {
		// return SwingUtilities.computeStringWidth(font.getf, text);;
		float result = 0;
		if (text != null && text.length() > 0) {
			// System.out.println("getStringWidth " + text);
			AttributedString atString = new AttributedString(text);
			atString.addAttribute(TextAttribute.FONT, font);
			AttributedCharacterIterator itr = atString.getIterator();
			TextMeasurer measurer = new TextMeasurer(itr,
					PPaintContext.RENDER_QUALITY_HIGH_FRC);
			result = measurer.getAdvanceBetween(0, text.length());
		}
		return result;
	}

	// Ignores newLines
	public static double getStringHeight(String s, float width, Font font) {
		// System.out.println("getStringHeight " + s);
		Rectangle r = wrapTextInternal(s, width, font, null);
		return r.getHeight();
	}

	public static String wrapText(String s, float w, Font font) {
		// System.out.println("wrapText " + s);
		String[] lines = s.split("\n");
		for (int i = 0; i < lines.length; i++) {
			StringBuffer buf = new StringBuffer(lines[i].length() + 20);
			wrapTextInternal(lines[i], w, font, buf);
			lines[i] = buf.toString();
		}
		return edu.cmu.cs.bungee.javaExtensions.Util.join(lines, "\n");
	}

	public static Rectangle wrapTextInternal(String text, float availableWidth,
			Font font, StringBuffer wrapped) {
		double textWidth = 0;
		double textHeight = 0;

		if (text != null && text.length() > 0) {
			// Util.print("\nenter recomputelayout " + text);
			AttributedString atString = new AttributedString(text);
			atString.addAttribute(TextAttribute.FONT, font);
			AttributedCharacterIterator itr = atString.getIterator();
			LineBreakMeasurer measurer = new LineBreakMeasurer(itr,
					PPaintContext.RENDER_QUALITY_HIGH_FRC);

			int nextLineBreakOffset = Integer.MAX_VALUE;
			int prevPosition = 0;

			while (measurer.getPosition() < itr.getEndIndex()) {
				TextLayout aTextLayout = measurer.nextLayout(availableWidth,
						nextLineBreakOffset, false);

				if (wrapped != null) {
					if (wrapped.length() > 0)
						wrapped.append("\n");
					int position = prevPosition
							+ aTextLayout.getCharacterCount();
					wrapped.append(text.substring(prevPosition, position));
					prevPosition = position;
				}
				textHeight += aTextLayout.getAscent();
				textHeight += aTextLayout.getDescent()
						+ aTextLayout.getLeading();
				textWidth = Math.max(textWidth, aTextLayout.getAdvance());
			}
		}
		return new Rectangle((int) Math.ceil(textWidth), (int) Math
				.ceil(textHeight));
	}

	// This offers more parameters than PNode.animateToTransparency
	public static PInterpolatingActivity animateToTransparency(PNode node,
			float zeroToOne, long delay, long duration) {
		if (duration == 0 && delay == 0) {
			node.setTransparency(zeroToOne);
			return null;
		} else {
			final float dest = zeroToOne;
			final PNode _node = node;

			PInterpolatingActivity ta = new PInterpolatingActivity(duration,
					PUtil.DEFAULT_ACTIVITY_STEP_RATE, delay
							+ System.currentTimeMillis(), 1,
					PInterpolatingActivity.SOURCE_TO_DESTINATION) {
				private float source;

				protected void activityStarted() {
					source = _node.getTransparency();
					super.activityStarted();
				}

				public void setRelativeTargetValue(float zeroToOne1) {
					_node.setTransparency(source
							+ (zeroToOne1 * (dest - source)));
				}
			};

			node.addActivity(ta);
			return ta;
		}
	}

	public static void outline(LazyPPath outline, Rectangle2D bounds) {
		float x = (float) bounds.getX();
		float y = (float) bounds.getY();
		float w = (float) bounds.getWidth();
		float h = (float) bounds.getHeight();
		outline.reset();
		outline.moveTo(x, y);
		outline.lineTo(x + w, y);
		outline.lineTo(x + w, y + h);
		outline.lineTo(x, y + h);
		outline.lineTo(x, y);
	}
}
