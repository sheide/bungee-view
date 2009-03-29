package edu.cmu.cs.bungee.piccoloUtils.gui;

/* BevelBorder.java --
 Copyright (C) 2003 Free Software Foundation, Inc.

 This file is part of GNU Classpath.

 GNU Classpath is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2, or (at your option)
 any later version.

 GNU Classpath is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with GNU Classpath; see the file COPYING.  If not, write to the
 Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 02110-1301 USA.

 Linking this library statically or dynamically with other modules is
 making a combined work based on this library.  Thus, the terms and
 conditions of the GNU General Public License cover the whole
 combination.

 As a special exception, the copyright holders of this library give you
 permission to link this library with independent modules to produce an
 executable, regardless of the license terms of these independent
 modules, and to copy and distribute the resulting executable under
 terms of your choice, provided that you also meet, for each linked
 independent module, the terms and conditions of the license of that
 module.  An independent module is a module which is not derived from
 or based on this library.  If you modify this library, you may extend
 this exception to your version of the library, but you are not
 obligated to do so.  If you do not wish to do so, delete this
 exception statement from your version. */

import java.awt.Color;
import java.awt.Graphics2D;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Draw a beveled border around parent PNode.
 * 
 * A rectangular, two pixel thick border that causes the enclosed area to appear
 * as if it was raising out of or lowered into the screen. Some LookAndFeels use
 * this kind of border for rectangular buttons.
 * 
 * <p>
 * A BevelBorder has a highlight and a shadow color. In the raised variant, the
 * highlight color is used for the top and left edges, and the shadow color is
 * used for the bottom and right edge. For an image, see the documentation of
 * the individual constructors.
 * 
 * @author Sascha Brawer (brawer@dandelis.ch)
 */
public class BevelBorder extends LazyPNode {
	/**
	 * Determined using the <code>serialver</code> tool of Apple/Sun JDK 1.3.1
	 * on MacOS X 10.1.5.
	 */
	static final long serialVersionUID = -1034942243356299676L;

	/**
	 * Indicates that the BevelBorder looks like if the enclosed area was
	 * raising out of the screen.
	 */
	public static final int RAISED = 0;

	/**
	 * Indicates that the BevelBorder looks like if the enclosed area was
	 * pressed into the screen.
	 */
	public static final int LOWERED = 1;

	/**
	 * The type of this BevelBorder, which is either {@link #RAISED} or
	 * {@link #LOWERED}.
	 */
	private int bevelType;

	/**
	 * The outer highlight color, or <code>null</code> to indicate that the
	 * color shall be derived from the background of the component whose border
	 * is being painted.
	 */
	protected Color highlightOuter;

	/**
	 * The inner highlight color, or <code>null</code> to indicate that the
	 * color shall be derived from the background of the component whose border
	 * is being painted.
	 */
	protected Color highlightInner;

	/**
	 * The outer shadow color, or <code>null</code> to indicate that the color
	 * shall be derived from the background of the component whose border is
	 * being painted.
	 */
	protected Color shadowOuter;

	/**
	 * The inner shadow color, or <code>null</code> to indicate that the color
	 * shall be derived from the background of the component whose border is
	 * being painted.
	 */
	protected Color shadowInner;

	public int strokeW = 1;

	/**
	 * If null, draw border around parent border; otherwise around this one.
	 */
	public PBounds borderBounds;

	/**
	 * Constructs a BevelBorder whose colors will be derived from the background
	 * of the enclosed component. The background color is retrieved each time
	 * the border is painted, so a BevelBorder constructed by this method will
	 * automatically reflect a change to the component&#x2019;s background
	 * color.
	 * 
	 * <p>
	 * <img src="doc-files/BevelBorder-1.png" width="500" height="150" alt="[An
	 * illustration showing raised and lowered BevelBorders]" />
	 * 
	 * @param bevelType
	 *            the desired appearance of the border. The value must be either
	 *            {@link #RAISED} or {@link #LOWERED}.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>bevelType</code> has an unsupported value.
	 */
	public BevelBorder(int bevelType) {
		if ((bevelType != RAISED) && (bevelType != LOWERED))
			throw new IllegalArgumentException();

		this.bevelType = bevelType;
		// Need to have non-zero size in order to be painted. paint will set the
		// real border.
		setBounds(0, 0, 10, 10);
		setPickable(false);
	}

	public BevelBorder(int bevelType, Color bgColor) {
		this(bevelType,
		/* highlight */bgColor.brighter(),
		/* shadow */bgColor.darker());
	}

	/**
	 * Constructs a BevelBorder given its appearance type and two colors for its
	 * highlight and shadow.
	 * 
	 * <p>
	 * <img src="doc-files/BevelBorder-2.png" width="500" height="150" alt="[An
	 * illustration showing BevelBorders that were constructed with this
	 * method]" />
	 * 
	 * @param bevelType
	 *            the desired appearance of the border. The value must be either
	 *            {@link #RAISED} or {@link #LOWERED}.
	 * 
	 * @param highlight
	 *            the color that will be used for the inner side of the
	 *            highlighted edges (top and left if if <code>bevelType</code>
	 *            is {@link #RAISED}; bottom and right otherwise). The color for
	 *            the outer side is a brightened version of this color.
	 * 
	 * @param shadow
	 *            the color that will be used for the outer side of the shadowed
	 *            edges (bottom and right if <code>bevelType</code> is
	 *            {@link #RAISED}; top and left otherwise). The color for the
	 *            inner side is a brightened version of this color.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>bevelType</code> has an unsupported value.
	 * 
	 * @throws NullPointerException
	 *             if <code>highlight</code> or <code>shadow</code> is
	 *             <code>null</code>.
	 * 
	 * @see java.awt.Color#brighter()
	 */
	public BevelBorder(int bevelType, Color highlight, Color shadow) {
		this(bevelType,
		/* highlightOuter */highlight.brighter(),
		/* highlightInner */highlight,
		/* shadowOuter */shadow,
		/* shadowInner */shadow.brighter());
	}

	/**
	 * Constructs a BevelBorder given its appearance type and all colors.
	 * 
	 * <p>
	 * <img src="doc-files/BevelBorder-3.png" width="500" height="150" alt="[An
	 * illustration showing BevelBorders that were constructed with this
	 * method]" />
	 * 
	 * @param bevelType
	 *            the desired appearance of the border. The value must be either
	 *            {@link #RAISED} or {@link #LOWERED}.
	 * 
	 * @param highlightOuter
	 *            the color that will be used for the outer side of the
	 *            highlighted edges (top and left if <code>bevelType</code> is
	 *            {@link #RAISED}; bottom and right otherwise).
	 * 
	 * @param highlightInner
	 *            the color that will be used for the inner side of the
	 *            highlighted edges.
	 * 
	 * @param shadowOuter
	 *            the color that will be used for the outer side of the shadowed
	 *            edges (bottom and right if <code>bevelType</code> is
	 *            {@link #RAISED}; top and left otherwise).
	 * 
	 * @param shadowInner
	 *            the color that will be used for the inner side of the shadowed
	 *            edges.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>bevelType</code> has an unsupported value.
	 * 
	 * @throws NullPointerException
	 *             if one of the passed colors is <code>null</code>.
	 */
	public BevelBorder(int bevelType, Color highlightOuter,
			Color highlightInner, Color shadowOuter, Color shadowInner) {
		this(bevelType); // checks the validity of bevelType

		if ((highlightOuter == null) || (highlightInner == null)
				|| (shadowOuter == null) || (shadowInner == null))
			throw new NullPointerException();

		this.highlightOuter = highlightOuter;
		this.highlightInner = highlightInner;
		this.shadowOuter = shadowOuter;
		this.shadowInner = shadowInner;
		// Need to have non-zero size in order to be painted. paint will set the
		// real border.
		setBounds(0, 0, 10, 10);
		setPickable(false);
	}

	void setBevelType(int type) {
		bevelType = type;
		invalidatePaint();
	}

	/**
	 * Paints the border for a given component.
	 */

	protected void paint(PPaintContext paintContext) {
		switch (bevelType) {
		case RAISED:
			paintRaisedBevel(paintContext);
			break;

		case LOWERED:
			paintLoweredBevel(paintContext);
			break;
		}
	}

	/**
	 * Determines the color that will be used for the outer side of highlighted
	 * edges when painting the border. If a highlight color has been specified
	 * upon constructing the border, that color is returned. Otherwise, the
	 * inner highlight color is brightened.
	 * 
	 * @see #getHighlightInnerColor()
	 * @see java.awt.Color#brighter()
	 */
	public Color getHighlightOuterColor() {
		if (highlightOuter != null)
			return highlightOuter;
		else
			return brighten(backgroundColor(), 2);
	}

	/**
	 * Determines the color that will be used for the inner side of highlighted
	 * edges when painting the border. If a highlight color has been specified
	 * upon constructing the border, that color is returned. Otherwise, the
	 * background color of the enclosed component is brightened.
	 * 
	 * @see java.awt.Component#getBackground()
	 * @see java.awt.Color#brighter()
	 */
	public Color getHighlightInnerColor() {
		if (highlightInner != null)
			return highlightInner;
		else {
			return brighten(backgroundColor(), 1);
		}
	}

	/**
	 * Determines the color that will be used for the inner side of shadowed
	 * edges when painting the border. If a shadow color has been specified upon
	 * constructing the border, that color is returned. Otherwise, the
	 * background color of the enclosed component is darkened.
	 * 
	 * @see java.awt.Component#getBackground()
	 * @see java.awt.Color#darker()
	 */
	public Color getShadowInnerColor() {
		if (shadowInner != null)
			return shadowInner;
		else
			return brighten(backgroundColor(), -1);
	}

	private Color backgroundColor() {
		Color result = (Color) (getPaint() == null ? getParent().getPaint()
				: getPaint());
		if (result == null)
			Util.printAncestors(this);
		assert result != null : "Border or its parent must have a color.";
		return result;
	}

	/**
	 * Determines the color that will be used for the outer side of shadowed
	 * edges when painting the border. If a shadow color has been specified upon
	 * constructing the border, that color is returned. Otherwise, the inner
	 * shadow color is darkened.
	 * 
	 * @see #getShadowInnerColor()
	 * @see java.awt.Color#darker()
	 */
	public Color getShadowOuterColor() {
		if (shadowOuter != null)
			return shadowOuter;
		else
			return brighten(backgroundColor(), -2);
	}

	/**
	 * @param color
	 * @return brighter color
	 */
	static Color brighten(Color color, int amount) {
		int abs = Math.abs(amount);
		assert abs == 1 || abs == 2 : amount;
		float[] rgb = color.getRGBColorComponents(null);
		return new Color(brightenComponent(rgb[0], amount), brightenComponent(
				rgb[1], amount), brightenComponent(rgb[2], amount));
	}

	static float brightenComponent(float component, int amount) {
		float factor = 0.8f;
		if (amount > 0 && component / factor / factor > 1) {
			factor = (float) Math.sqrt(component);
		}
		if (Math.abs(amount) == 2)
			factor *= factor;
		if (amount > 0)
			factor = 1 / factor;
		float result = Math.min(1, component * factor);
		return result;
	}

	/**
	 * Returns the appearance of this border, which is either {@link #RAISED} or
	 * {@link #LOWERED}.
	 */
	public int getBevelType() {
		return bevelType;
	}

	/**
	 * Determines whether this border fills every pixel in its area when
	 * painting.
	 * 
	 * <p>
	 * If the border colors are derived from the background color of the
	 * enclosed component, the result is <code>true</code> because the
	 * derivation method always returns opaque colors. Otherwise, the result
	 * depends on the opacity of the individual colors.
	 * 
	 * @return <code>true</code> if the border is fully opaque, or
	 *         <code>false</code> if some pixels of the background can shine
	 *         through the border.
	 */
	public boolean isBorderOpaque() {
		/*
		 * If the colors are to be drived from the enclosed Component's
		 * background color, the border is guaranteed to be fully opaque because
		 * Color.brighten() and Color.darken() always return an opaque color.
		 */
		return ((highlightOuter == null) || (highlightOuter.getAlpha() == 255))
				&& ((highlightInner == null) || (highlightInner.getAlpha() == 255))
				&& ((shadowInner == null) || (shadowInner.getAlpha() == 255))
				&& ((shadowOuter == null) || (shadowOuter.getAlpha() == 255));
	}

	/**
	 * Paints a raised bevel border around a component.
	 */
	protected void paintRaisedBevel(PPaintContext paintContext) {
		paintBevel(paintContext, getHighlightOuterColor(),
				getHighlightInnerColor(), getShadowInnerColor(),
				getShadowOuterColor());
	}

	/**
	 * Paints a lowered bevel border around a component.
	 */
	protected void paintLoweredBevel(PPaintContext paintContext) {
		paintBevel(paintContext, getShadowInnerColor(), getShadowOuterColor(),
				getHighlightInnerColor(), getHighlightOuterColor());
	}

	/**
	 * Paints a two-pixel bevel in four colors.
	 * 
	 * <pre>
	 * ++++++++++++
	 * +..........#    + = color a
	 * +.        X#    . = color b
	 * +.        X#    X = color c
	 * +.XXXXXXXXX#    # = color d
	 * ############
	 * </pre>
	 * 
	 * @param a
	 *            the color for the outer side of the top and left edges.
	 * @param b
	 *            the color for the inner side of the top and left edges.
	 * @param c
	 *            the color for the inner side of the bottom and right edges.
	 * @param d
	 *            the color for the outer side of the bottom and right edges.
	 */
	private void paintBevel(PPaintContext paintContext, Color a, Color b,
			Color c, Color d) {
		if (strokeW > 0 && getTransparency() > 0) {
			Graphics2D g = paintContext.getGraphics();
			g.setStroke(LazyPPath.getStrokeInstance(strokeW));

			PBounds bounds = getBorderBounds();
			int x = (int) bounds.getX() - 2 * strokeW;
			int y = (int) bounds.getY() - 2 * strokeW;
			int right = (int) (bounds.getWidth() + 2 * strokeW - 1);
			int bottom = (int) (bounds.getHeight() + 2 * strokeW - 1);
			setBounds(x, y, right - x, bottom - y);

			/*
			 * To understand this code, it might be helpful to look at the
			 * images that are included with the JavaDoc. They are located in
			 * the "doc-files" subdirectory.
			 */
			g.setColor(a);
			g.drawLine(x, y, right, y); // a, horizontal
			g.drawLine(x, y + strokeW, x, bottom); // a, vertical

			g.setColor(b);
			g.drawLine(x + strokeW, y + strokeW, right - strokeW, y + strokeW); // b,
			// horizontal
			g.drawLine(x + strokeW, y + 2 * strokeW, x + strokeW, bottom
					- strokeW); // b, vertical

			g.setColor(c);
			g.drawLine(x + 2 * strokeW, bottom - strokeW, right - strokeW,
					bottom - strokeW); // c, horizontal
			g.drawLine(right - strokeW, y + 2 * strokeW, right - strokeW,
					bottom - strokeW); // c, vertical

			g.setColor(d);
			g.drawLine(x + strokeW, bottom, right, bottom); // d, horizontal
			g.drawLine(right, x + strokeW, right, bottom - strokeW); // d,
			// vertical
		}
	}

	PBounds getBorderBounds() {
		return borderBounds != null ? borderBounds : getParent().getBounds();
	}
}
