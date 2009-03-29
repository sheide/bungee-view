/* 

 Created on Jun 19, 2005

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
import java.awt.Paint;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PNode;

public class Button extends LazyPNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	APText disabledMessage;

	protected PNode child;

	static ButtonHandler handler = new ButtonHandler();

	private BevelBorder border;

	public String mouseDoc;

	// Color _baseColor;

	public Button(double x, double y, double outerW, double outerH,
			String _disabledMessage, String documentation, float fadeFactor,
			Paint paint) {
		setBaseColor(paint);
		mouseDoc = documentation;
		outerW = (int) (outerW + 0.5);
		outerH = (int) (outerH + 0.5);
		boolean is3D = fadeFactor != 0;
		if (is3D) {
			assert getPaint() != null;
			border = new BevelBorder(BevelBorder.RAISED);
			addChild(border);
		}
		setDisabledMessage(_disabledMessage);
		setOffset(x, y);
		adjustSize(outerW, outerH, borderW());
		addInputEventListener(handler);
	}

	public void setDisabledMessage(String message) {
		if (message != null) {
			if (disabledMessage == null) {
				disabledMessage = new APText();
				disabledMessage.setPaint(Color.yellow);
			}
			disabledMessage.setText(message);
		} else {
			disabledMessage = null;
		}
	}

	public void showDisabledState() {
		// Util.print("showDisabledMessage " + isEnabled() + " " + this);
		setTransparency(isEnabled() ? 1 : 0.5f);
	}

	/**
	 * Having raised mean true seems backwards...
	 */
	public void setState(boolean state) {
		if (border != null) {
			border.setBevelType(state ? BevelBorder.RAISED
					: BevelBorder.LOWERED);
		}
	}

	private void setBaseColor(Paint paint) {
		if (paint == null)
			paint = Color.white;
		setPaint(paint);
	}

	public void setBorderColor(Color bgColor) {
		border.setPaint(bgColor);
	}

	public void fitToChild() {
		adjustSize(child.getWidth() + 2 * borderW(), child.getHeight() + 2
				* borderW(), borderW());
	}

	public double getXOffset() {
		return super.getXOffset() - borderW();
	}

	public double getYOffset() {
		return super.getYOffset() - borderW();
	}

	public void setOffset(double outerX, double outerY) {
		outerX += borderW();
		outerY += borderW();
		outerX = (int) (outerX + 0.5);
		outerY = (int) (outerY + 0.5);
		super.setOffset(outerX, outerY);
		if (disabledMessage != null)
			disabledMessage.setOffset(outerX, outerY + outerH() + 1.0);
	}

	/**
	 * @return x-coordinate of right edge in parents coordinate system
	 */
	public double getMaxX() {
		return getXOffset() + outerW() * getScale();
	}

	/**
	 * @return y-coordinate of bottom edge in parents coordinate system
	 */
	public double getMaxY() {
		return getYOffset() + outerH() * getScale();
	}

	int borderW() {
		return border == null ? 0 : border.strokeW * 2;
	}

	// public double getWidth() {
	// return outerW();
	// }

	public double outerW() {
		return super.getWidth() + 2 * borderW();
	}

	double innerW() {
		return super.getWidth();
	}

	public double outerH() {
		return super.getHeight() + 2 * borderW();
	}

	double innerH() {
		return super.getHeight();
	}

	boolean setBorderW(int w) {
		assert w > 0;
		return adjustSize(outerW(), outerH(), w);
	}

	public boolean adjustSize(double outerW, double outerH) {
		return adjustSize(outerW, outerH, borderW());
	}

	boolean adjustSize(double outerW, double outerH, double borderW) {
		double dx = borderW();
		if (border != null) {
			border.strokeW = (int) Math.ceil(borderW / 2);
		}
		dx -= borderW();
		super.setOffset(getXOffset() + dx, getYOffset() + dx);
		double newWidth = Math.round(outerW - 2 * borderW());
		double newHeight = Math.round(outerH - 2 * borderW());
		boolean result = super.setBounds(0, 0, newWidth, newHeight);
		positionChild();
		return result;
	}

	/**
	 * Add this child, make it unpickable, and fit it to our [inner] size.
	 */
	public void positionChild() {
		if (child != null) {
			double w = Math.floor(innerW());
			double h = Math.floor(innerH());
			child.setWidth(w);
			child.setHeight(h);
			child.setOffset(Math.ceil((innerW() - child.getWidth()) / 2.0),
					Math.ceil((innerH() - child.getHeight()) / 2.0)); // in
			// case
			// the
			// sets
			// didn't work
			// (e.g. for auto-sizing PText)
			setChildrenPickable(false);
			addChild(child);
		}
	}

	public boolean isEnabled = true;

	public boolean isEnabled() {
		return isEnabled;
	}

	public void showMessage() {
		getParent().addChild(disabledMessage);
	}

	public void exit() {
		if (disabledMessage != null) {
			disabledMessage.removeFromParent();
		}
		setMouseDoc(false);
	}

	public void enter() {
		setMouseDoc(true);
	}

	public void setMouseDoc(boolean state) {
		// override this
		// Util.print("Button.setMouseDoc " +
		// state+" "+getParent()+" "+mouseDoc);
		if (getParent() instanceof MouseDoc)
			((MouseDoc) getParent()).setMouseDoc(state ? mouseDoc : null);
	}

	public void doPick() {
		Util.print("default (no-op) doPick");
	}

	void pick() {
		if (isEnabled())
			doPick();
		else
			showMessage();
	}

	public void mayHideTransients(PNode node) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		// System.err.println("Should override Buttons.mayHideTransients");
	}
}

class ButtonHandler extends MyInputEventHandler {

	public ButtonHandler() {
		super(Button.class);
	}

	// Pretty much anything you do on a button shouldn't go any further, so
	// always return true.

	protected boolean click(PNode node) {
		((Button) node).pick();
		return true;
	}

	protected boolean exit(PNode node) {
		((Button) node).exit();
		return true;
	}

	protected boolean enter(PNode node) {
		((Button) node).enter();
		return true;
	}

	protected void mayHideTransients(PNode node) {
		((Button) node).mayHideTransients(node);
	}

}