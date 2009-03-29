/* 

 Created on Jun 20, 2005

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
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;

public class TextButton extends Button {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Must specify final w and h, rather than have it computed from text.
	// Otherwise, Button would try to shrink it, which wouldn't have an effect,
	// and child would occlude the whole Button.
	public TextButton(String label, Font f, double x, double y, double outerW,
			double outerH, String disabledMessage, String documentation,
			float fadeFactor1, Color textColor, Paint paint) {
		super(x, y, outerW >= 0 ? outerW : 10, outerH >= 0 ? outerH : 10,
				disabledMessage, documentation, fadeFactor1, paint);
		child = new APText(f);
		((APText) child).setWrapOnWordBoundaries(false);
		if (textColor == null)
			textColor = Color.BLACK;
		((APText) child).setTextPaint(textColor);
		// child.setPaint(bgColor);
		((APText) child).setText(label);
		// if (h>0)
		((APText) child).setConstrainHeightToTextHeight(false);
		// if (w>0)
		((APText) child).setConstrainWidthToTextWidth(false);
		((APText) child).setJustification(Component.CENTER_ALIGNMENT);
		if (outerW < 0 || outerH < 0) {
			if (outerW > 0)
				child.setWidth(outerW - 2 * borderW());
			if (outerH > 0)
				child.setHeight(outerH - 2 * borderW());
			fitToChild();
		} else {
			positionChild();
		}
	}

	Font getFont() {
		return ((APText) child).getFont();
	}

	public void setFont(Font f) {
		if (child != null
				&& !edu.cmu.cs.bungee.javaExtensions.Util.equalsNullOK(
						getFont(), f)) {
			((APText) child).setFont(f);
			if (((APText) child).isConstrainWidthToTextWidth()) {
				fitToChild();
				// setWidth(child.getWidth() /* + 2 */);
			} else
				positionChild();
		}
	}

	public void setText(String text) {
		if (child != null
				&& !edu.cmu.cs.bungee.javaExtensions.Util.equalsNullOK(
						getText(), text)) {
			((APText) child).setText(text);
			if (((APText) child).isConstrainWidthToTextWidth()) {
				fitToChild();
			} else
				positionChild();
		}
	}

	public void setTextPaint(Paint paint) {
		((APText) child).setTextPaint(paint);
	}

	// e.g. javax.swing.JLabel.LEFT_ALIGNMENT;
	public void setJustification(float just) {
		((APText) child).setJustification(just);
	}

	public String getText() {
		return child == null ? null : ((APText) child).getText();
	}

	public void setVisible(boolean state) {
		setPickable(state);
		setChildrenPickable(state);
		super.setVisible(state);
	}
}