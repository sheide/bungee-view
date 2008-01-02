package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TextMeasurer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.nodes.PText;

/*
 * Created on Mar 5, 2005 @author mad Supports attributes like UNDERLINE. Also
 * checks whether there's really been a change before updating stuff.
 */
public class APText extends PText {

	private boolean wrapOnWordBoundaries = true;

	protected boolean underline = false;

	private List txtAttributes = new LinkedList();

	private boolean dontRecompute;

	public APText() {
		// More efficient to do nothing here and setText after all other
		// attributes are set
	}

	public APText(Font f) {
		setFont(f);
	}

	/**
	 * Normally, if constrainWidthToTextWidth = false, text will be wrapped at
	 * word boundaries at the width. Sometimes we only have one line, and want
	 * as many characters as possible on it, even if we have to break words.
	 * Setting this to false will break words.
	 */
	public void setWrapOnWordBoundaries(boolean _wrapText) {
		wrapOnWordBoundaries = _wrapText;
	}

	// public boolean setX(double x) {
	// return setBounds(x, getY(), Math.ceil(getWidth()), Math
	// .ceil(getHeight()));
	// }
	//
	// public boolean setY(double y) {
	// return setBounds(getX(), y, Math.ceil(getWidth()), Math
	// .ceil(getHeight()));
	// }

	public void setXoffset(double x) {
		setOffset(x, getYOffset());
	}

	public void setYoffset(double y) {
		setOffset(getXOffset(), y);
	}

	public boolean setHeight(double height) {
		if (height != getHeight()) {
			assert height == Math.round(height);
			return setBounds(getX(), getY(), Math.ceil(getWidth()), height);
		}
		return false;
	}

	public boolean setWidth(double width) {
		if (width != getWidth()) {
			assert width == Math.round(width);
			return setBounds(getX(), getY(), width, Math.ceil(getHeight()));
		}
		return false;
	}

	public boolean setBounds(double x, double y, double w, double h) {
		assert x == Math.round(x);
		assert y == Math.round(y);
		assert w == Math.round(w);
		assert h == Math.round(h);
		assert w >= 0 : w;
		assert h >= 0 : h;
		return super.setBounds(x, y, w, h);
	}

	public void setOffset(double x, double y) {
		assert x == Math.round(x) : x;
		// assert y == Math.round(y) : y;
		if (x != getXOffset() || y != getYOffset()) {
			super.setOffset(x, y);
		}
	}

	public double getCenterX() {
		return getXOffset() + getWidth() * getScale() / 2.0;
	}

	public double getCenterY() {
		return getYOffset() + getHeight() * getScale() / 2.0;
	}

	public double getMaxX() {
		return getXOffset() + getWidth() * getScale();
	}

	public double getMaxY() {
		return getYOffset() + getHeight() * getScale();
	}

	public void setScale(double scale) {
		if (scale != getScale())
			scale(scale / getScale());
	}

	public void setTextPaint(Paint aPaint) {
		if (!Util.equalsNullOK(aPaint, getTextPaint()))
			super.setTextPaint(aPaint);
	}

	public void setText(String _text) {
		if (!Util.equalsNullOK(_text, getText())) {
			decacheCharIter();
			if (_text != null && _text.indexOf("\n\n") >= 0)
				_text = _text.replaceAll("\n\n", "\n \n");
			// recomputeLayout doesn't deal well with empty lines
			super.setText(_text);
		}
	}

	public void setFont(Font aFont) {
		if (aFont != getFont()) {
			decacheCharIter();

			// for one-line labels, update height to match font size
			boolean resize = !wrapOnWordBoundaries
					&& !isConstrainHeightToTextHeight();
			if (resize) {
				dontRecompute = true;
				super.setConstrainHeightToTextHeight(true);
				dontRecompute = false;
			}
			super.setFont(aFont);
			if (resize)
				setConstrainHeightToTextHeight(false);
		}
	}

	public void setConstrainWidthToTextWidth(boolean _constrainWidthToTextWidth) {
		if (_constrainWidthToTextWidth != isConstrainWidthToTextWidth()) {
			dontRecompute = !_constrainWidthToTextWidth;
			// No need to recomputeLayout in this direction
			super.setConstrainWidthToTextWidth(_constrainWidthToTextWidth);
			dontRecompute = false;
		}
	}

	public void setConstrainHeightToTextHeight(
			boolean _constrainHeightToTextHeight) {
		if (_constrainHeightToTextHeight != isConstrainHeightToTextHeight()) {
			dontRecompute = !_constrainHeightToTextHeight;
			// No need to recomputeLayout in this direction
			// constrainHeightToTextHeight = _constrainHeightToTextHeight;
			// else
			super.setConstrainHeightToTextHeight(_constrainHeightToTextHeight);
			dontRecompute = false;
		}
	}

	public void setJustification(float just) {
		if (just != getJustification())
			super.setJustification(just);
	}

	public PInterpolatingActivity animateToBounds(double x, double y,
			double width, double height, long duration) {
		final PBounds dst = new PBounds(x, y, width, height);

		PInterpolatingActivity ta = new PInterpolatingActivity(duration,
				PUtil.DEFAULT_ACTIVITY_STEP_RATE) {

			private PBounds src;

			protected void activityStarted() {
				src = getBounds();
				super.activityStarted();
			}

			public void setRelativeTargetValue(float zeroToOne) {
				APText.this.setBounds(Math.ceil(lerp(zeroToOne, src.x, dst.x)),
						Math.ceil(lerp(zeroToOne, src.y, dst.y)), Math
								.ceil(lerp(zeroToOne, src.width, dst.width)),
						Math.ceil(lerp(zeroToOne, src.height, dst.height)));
			}
		};

		addActivity(ta);
		return ta;

	}

	public void rerender() {
		if (getText() != null) {
			recomputeLayout();
			invalidatePaint();
		}
	}

	public void clearAttributes() {
		if (txtAttributes.size() > 0) {
			decacheCharIter();
			txtAttributes.clear();
			rerender();
		}
	}

	public void addAttribute(AttributedCharacterIterator.Attribute attribute,
			Object value) {
		decacheCharIter();
		Object[] spec = new Object[4];
		spec[0] = attribute;
		spec[1] = value;
		txtAttributes.add(spec);
		rerender();
	}

	public void addAttribute(AttributedCharacterIterator.Attribute attribute,
			Object value, int beginIndex, int endIndex) {
		decacheCharIter();
		Object[] spec = new Object[4];
		spec[0] = attribute;
		spec[1] = value;
		spec[2] = new Integer(beginIndex);
		spec[3] = new Integer(endIndex);
		// Util.print("\n" + getText());
		// Util.printDeep(spec);
		txtAttributes.add(spec);
		rerender();
	}

	public void deleteAttribute(
			AttributedCharacterIterator.Attribute attribute, Object value,
			int beginIndex, int endIndex) {
		decacheCharIter();
		for (Iterator it = txtAttributes.iterator(); it.hasNext();) {
			Object[] spec = (Object[]) it.next();
			if (spec[0] == attribute && spec[1] == value
					&& ((Integer) spec[2]).intValue() == beginIndex
					&& ((Integer) spec[3]).intValue() == endIndex) {
				it.remove();
				break;
			}
		}
		rerender();
	}

	public void setUnderline(boolean u) {
		if (u != underline) {
			// Util.print("setUnderline " + getText() + " " + u);
			decacheCharIter();
			underline = u;
			boolean found = false;
			for (Iterator it = txtAttributes.iterator(); it.hasNext();) {
				Object[] spec = (Object[]) it.next();
				if (spec[0] == TextAttribute.UNDERLINE) {
					found = true;
					if (!u) {
						it.remove();
					}
				}
			}
			if (u && !found) {
				addAttribute(TextAttribute.UNDERLINE,
						TextAttribute.UNDERLINE_ON);
			}
		}
	}

	private transient AttributedCharacterIterator charIter;

	private transient LineBreakMeasurer lbMeasurer;

	private transient TextMeasurer tMeasurer;

	private AttributedCharacterIterator getCharIter() {
		String _text = getText();
		if (charIter == null && _text != null && _text.length() > 0) {
			AttributedString atString = new AttributedString(_text);
			atString.addAttribute(TextAttribute.FONT, getFont());
			if (txtAttributes != null) {
				for (Iterator it = txtAttributes.iterator(); it.hasNext();) {
					Object[] spec = (Object[]) it.next();
					if (spec[2] == null)
						atString
								.addAttribute(
										(AttributedCharacterIterator.Attribute) spec[0],
										spec[1]);
					else
						atString
								.addAttribute(
										(AttributedCharacterIterator.Attribute) spec[0],
										spec[1],
										((Integer) spec[2]).intValue(),
										((Integer) spec[3]).intValue());
				}
				// for (int i = 0; i < txtAttributes.size(); i += 2) {
				// atString.addAttribute(
				// (AttributedCharacterIterator.Attribute) txtAttributes
				// .get(i), txtAttributes.get(i + 1));
				// }
			}
			charIter = atString.getIterator();
		}
		return charIter;
	}

	private void decacheCharIter() {
		charIter = null;
		lbMeasurer = null;
		tMeasurer = null;
		_lines = null;
	}

	private LineBreakMeasurer getLBmeasurer() {
		if (lbMeasurer == null) {
			AttributedCharacterIterator it = getCharIter();
			if (it != null) {
				lbMeasurer = new LineBreakMeasurer(it,
						PPaintContext.RENDER_QUALITY_HIGH_FRC);
			}
		} else
			lbMeasurer.setPosition(0);
		return lbMeasurer;
	}

	private TextMeasurer getTextmeasurer() {
		if (tMeasurer == null) {
			AttributedCharacterIterator it = getCharIter();
			if (it != null) {
				// edu.cmu.cs.bungee.javaExtensions.Util.print(" new tm for "
				// + getText() + " " + getFont());
				tMeasurer = new TextMeasurer(it,
						PPaintContext.RENDER_QUALITY_HIGH_FRC);
			}
		}
		return tMeasurer;
	}

	/**
	 * @return the number of lines displayed by this APText
	 */
	public int getNlines() {
		if (_lines != null)
			return _lines.length;
		else
			return 0;
	}

	private boolean insideRL = false;

	/**
	 * Compute the bounds of the text wrapped by this node. The text layout is
	 * wrapped based on the bounds of this node.
	 */
	public void recomputeLayout() {
		if (!dontRecompute && getCharIter() != null) {
			assert !insideRL;
			assert getWidth() >= 0 : getWidth();
			insideRL = true;
			double textWidth = 0;
			double textHeight = 0;
//			 System.out.println("\nenter APText.recomputelayout " + getText()
//			 + " "+ isConstrainWidthToTextWidth() + " " + getWidth());
			// edu.cmu.cs.bungee.javaExtensions.Util.printStackTrace();
			float availableWidth = isConstrainWidthToTextWidth() ? Float.MAX_VALUE
					: (float) getWidth();
			String _text = getText();
			int nextLineBreakOffset = _text.indexOf('\n');
			if (nextLineBreakOffset == -1)
				nextLineBreakOffset = Integer.MAX_VALUE;
			if (wrapOnWordBoundaries) {
				ArrayList linesList = new ArrayList();
				LineBreakMeasurer measurer = getLBmeasurer();
				while (measurer.getPosition() < charIter.getEndIndex()) {
					if (nextLineBreakOffset < measurer.getPosition()) {
						nextLineBreakOffset = _text.indexOf('\n', measurer
								.getPosition());
						if (nextLineBreakOffset == -1)
							nextLineBreakOffset = Integer.MAX_VALUE;
					}
					if (nextLineBreakOffset == measurer.getPosition())
						nextLineBreakOffset++;
					TextLayout aTextLayout = computeNextLayout(measurer,
							availableWidth, nextLineBreakOffset);
					if (measurer.getPosition() == nextLineBreakOffset
					// && nextLineBreakOffset < charIter.getEndIndex()
					)
						measurer.setPosition(nextLineBreakOffset + 1);

					linesList.add(aTextLayout);
					textHeight += aTextLayout.getAscent();
					textHeight += aTextLayout.getDescent()
							+ aTextLayout.getLeading();
					textWidth = Math.max(textWidth, aTextLayout.getAdvance());
				}
				_lines = (TextLayout[]) linesList
						.toArray(EMPTY_TEXT_LAYOUT_ARRAY);
			} else {
				TextMeasurer primitiveMeasurer = getTextmeasurer();
				int firstCharThatWontFit = primitiveMeasurer.getLineBreakIndex(
						0, availableWidth);
				firstCharThatWontFit = Math.min(nextLineBreakOffset,
						firstCharThatWontFit);
				if (firstCharThatWontFit > 0) {
					_lines = new TextLayout[1];
					_lines[0] = primitiveMeasurer.getLayout(0,
							firstCharThatWontFit);
					if (_lines[0] != null) {
						textHeight = _lines[0].getAscent()
								+ _lines[0].getDescent()
								+ _lines[0].getLeading();
						textWidth = _lines[0].getAdvance();
					} else
						_lines = EMPTY_TEXT_LAYOUT_ARRAY;
				} else
					_lines = EMPTY_TEXT_LAYOUT_ARRAY;
			}
			// edu.cmu.cs.bungee.javaExtensions.Util.print("exit recomputelayout
			// "
			// + _text + " " + _lines.length + " " + textHeight);

			if (isConstrainWidthToTextWidth()
					|| isConstrainHeightToTextHeight()) {
				double newWidth = getWidth();
				double newHeight = getHeight();

				if (isConstrainWidthToTextWidth()) {
					// System.out.println(getText() + " " + newWidth + " => "
					// + Math.ceil(textWidth));
					newWidth = Math.ceil(textWidth);
				}

				if (isConstrainHeightToTextHeight()) {
					// System.out.println(getText() + " " + newHeight + " => "
					// + Math.ceil(textHeight));
					newHeight = Math.ceil(textHeight);
				}

				super.setBounds(getX(), getY(), newWidth, newHeight);
			}
			insideRL = false;
		}
	}

	protected void internalUpdateBounds(double ignore1, double ignore2,
			double ignore3, double ignore4) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(ignore1)
				&& edu.cmu.cs.bungee.javaExtensions.Util.ignore(ignore2)
				&& edu.cmu.cs.bungee.javaExtensions.Util.ignore(ignore3)
				&& edu.cmu.cs.bungee.javaExtensions.Util.ignore(ignore4);
		if (!insideRL)
			recomputeLayout();
	}

	/**
	 * @return _lines joined by "\n"
	 */
	public String getBrokenText() {
		String _text = getText();
		StringBuffer buf = new StringBuffer(_text.length() + _lines.length);
		int pos = 0;
		for (int i = 0; i < _lines.length; i++) {
			if (i > 0)
				buf.append("\n");
			if (_text.charAt(pos) == '\n')
				pos++;
			buf.append(_text.substring(pos, pos += _lines[i]
					.getCharacterCount()));
		}
		return buf.toString();
	}

	// Piccolo 1.2 made lines private, so have to copy all the code that uses
	// it.
	protected transient TextLayout[] _lines;

	protected void paint(PPaintContext paintContext) {
		// super.paint(paintContext);
		// we want to call super.super = PNode
		// Since this is hidden by PText, copy PNode.paint code:
		if (getPaint() != null) {
			Graphics2D g2 = paintContext.getGraphics();
			g2.setPaint(getPaint());
			g2.fill(getBoundsReference());
		}

		// copied from PText, substituting _lines for lines
		float screenFontSize = getFont().getSize()
				* (float) paintContext.getScale();
		if (getTextPaint() != null && getText() != null && screenFontSize > greekThreshold) {
			float x = (float) getX();
			float y = (float) getY();
			float bottomY = (float) getHeight() + y;

			Graphics2D g2 = paintContext.getGraphics();

			if (_lines == null) {
				recomputeLayout();
//				Util.print(getText());
				repaint();
				return;
			}

			g2.setPaint(getTextPaint());

			for (int i = 0; i < _lines.length; i++) {
				TextLayout tl = _lines[i];
				y += tl.getAscent();

				if (bottomY < y) {
					return;
				}

				float offset = (float) (getWidth() - tl.getAdvance())
						* getJustification();
				tl.draw(g2, x + offset, y);

				y += tl.getDescent() + tl.getLeading();
			}
		}
	}
}