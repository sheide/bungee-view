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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TextMeasurer;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Iterator;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;
import edu.umd.cs.piccolox.PFrame;

/**
 * @author mad
 * 
 */
public final class Util {

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
			// Util.printDeep(f.getAvailableAttributes());
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
			System.out.println(nodeDesc(node));
			System.out.println("  <no descendents");
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
		System.out.println(indent + nodeDesc(node));
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
		return name + " " + node.getScale() + " " + node.getBounds() + " "
				+ node.getOffset();
	}

	static void printDescendentsInternal(PNode node, String indent,
			int maxChildren, boolean invalidOnly) {
		int nChildren = node.getChildrenCount();
		String name = nodeDesc(node);
		System.out.println(indent + name + " " + node.getScale() + " "
				+ node.getBounds());
		int index = 0;
		for (Iterator i = node.getChildrenIterator(); i.hasNext()
				&& index < maxChildren;) {
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

	/**
	 * Does not pay attention to word breaks.
	 * 
	 * @param text
	 * @param availableWidth
	 * @param font
	 * @return a truncated version of text
	 */
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
			float transparency, long delay, long duration) {
		if (duration == 0 && delay == 0) {
			node.setTransparency(transparency);
			return null;
		} else {
			final float dest = transparency;
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

	/**
	 * @param pnode
	 *            node to print
	 * @param file
	 *            destination jpg
	 * @param dpi
	 *            dots per inch in jpg file. image size is the same as the size
	 *            of the pnode on the screen.
	 * @param quality
	 *            jpg quality from 1 - 100
	 * @throws ImageFormatException
	 * @throws IOException
	 */
	public static void savePNodeAsJPEG(PNode pnode, File file, int dpi,
			int quality) throws ImageFormatException, IOException {
		// JpegOptionsFileDialog fd = JpegOptionsFileDialog.saveFile(
		// "Save the current summary tree", directory, 85, (int) tWin
		// .getWidth(), (int) tWin.getHeight());

		GraphicsConfiguration gc = edu.cmu.cs.bungee.javaExtensions.Util
				.getGraphicsConfiguration();
		AffineTransform normalizingTransform = gc.getNormalizingTransform();
		double relativeScale = dpi / 72.0;
		normalizingTransform.scale(relativeScale, relativeScale);
		PBounds bounds = pnode.getFullBounds();
		double width = bounds.getWidth();
		int pageW = (int) (width * normalizingTransform.getScaleX());
		double height = bounds.getHeight();
		int pageH = (int) (height * normalizingTransform.getScaleY());

		BufferedImage im = edu.cmu.cs.bungee.javaExtensions.Util
				.createCompatibleImage(pageW, pageH);
		Graphics2D g = (Graphics2D) im.getGraphics();
		g.setTransform(gc.getDefaultTransform());
		g.transform(normalizingTransform);

		g.setPaint(Color.white);
		g.fillRect(0, 0, pageW, pageH);
		pnode.print(g, pageFormat(width, height), 0);

		OutputStream out = edu.cmu.cs.bungee.javaExtensions.Util
				.getOutputStream(file);
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(im);
		param.setQuality(quality / 100f, false);
		param.setDensityUnit(JPEGDecodeParam.DENSITY_UNIT_DOTS_INCH);
		param.setXDensity(dpi);
		param.setYDensity(dpi);
		encoder.setJPEGEncodeParam(param);
		encoder.encode(im);
		out.close();
	}

	public static PageFormat pageFormat(double pageW, double pageH) {
		Paper paper = new Paper();
		paper.setSize(pageW, pageH);
		paper.setImageableArea(0, 0, pageW, pageH);
		PageFormat pageFormat = new PageFormat();
		pageFormat.setPaper(paper);
		return pageFormat;
	}
}
