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


import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PNode;

public class Button extends LazyPNode {

	float fadeFactor;

	private static final long serialVersionUID = -4885565420352715444L;

	APText message;

	protected PNode child;

	public int borderW = 1;

	// PPath disabled;

	static ButtonHandler handler = new ButtonHandler();

	private PNode[] paths;

	public String mouseDoc;

	Color _baseColor;

	public Button(double x, double y, double w, double h,
			String disabledMessage, float _fadeFactor, Color baseColor) {
		_baseColor = baseColor;
		fadeFactor = _fadeFactor;
		w = (int) (w + 0.5);
		h = (int) (h + 0.5);
		setWidth(w);
		setHeight(h);

		setPaths();

		if (disabledMessage != null) {
			message = new APText();
			message.setPaint(Color.yellow);
			message.setText(disabledMessage);
		}

		setOffset(x, y);

		addInputEventListener(handler);
	}

	void setPaths() {
		// removeAllChildren();
		if (isThreeD()) {
			if (paths == null)
				paths = new PNode[2];
			// Add 1-pixel boundary on right & bottom edges
			if (paths[0] == null) {
				paths[0] = new PNode();
				paths[0].setPickable(false);
				addChild(paths[0]);
			}
			paths[0].setBounds(getWidth() - borderW, borderW, borderW,
					getHeight() - borderW);

			if (paths[1] == null) {
				paths[1] = new PNode();
				paths[1].setPickable(false);
				addChild(paths[1]);
			}
			paths[1].setBounds(0, getHeight() - borderW, getWidth(), borderW);
		}
		setState(true);
	}

	boolean isThreeD() {
		return fadeFactor != 0;
	}

	public void setState(boolean state) {
		setBaseColor(_baseColor, state ? fadeFactor : 1f / fadeFactor);
	}

	private void setBaseColor(Color baseColor, float fadeFactor1) {
		if (baseColor == null)
			baseColor = Color.white;

		Color bottomColor = baseColor;
		if (isThreeD()) {
			bottomColor = Util.brighten(baseColor, 1.0f / fadeFactor1);
			baseColor = Util.brighten(baseColor, fadeFactor1);
			for (int i = 0; i < paths.length; i++) {
				paths[i].setPaint(bottomColor);
			}
		}
		setPaint(baseColor);
	}

	public void setOffset(double x, double y) {
		x = (int) (x + 0.5);
		y = (int) (y + 0.5);
		super.setOffset(x, y);
		if (message != null)
			message.setOffset(x, y + getHeight() + 1.0);
	}

	public boolean setWidth(double w) {
		boolean result = super.setWidth(w);
		if (child != null) {
			setPaths();
			positionChild();
		}
		return result;
	}

	public void positionChild() {
		double w = getWidth();
		double h = getHeight();
		child.setWidth(w - (isThreeD() ? 2.0 * borderW : 0.0));
		child.setHeight(h - (isThreeD() ? 2.0 * borderW : 0.0));
		// setPaths();
		// Util.print("jjj " + child.getBounds());
		// child.setOffset(1.0, 1.0);
		child.setOffset(Math.ceil((w - child.getWidth()) / 2.0), Math
				.ceil((h - child.getHeight()) / 2.0)); // in case the sets
		// didn't work
		// (e.g. for auto-sizing PText)
		child.setPickable(false);
		addChild(child);

		// disabled = new PPath();
		// disabled.moveTo(0.0f, 0.0f);
		// disabled.lineTo((float) w, (float) h);
		// disabled.moveTo((float) w, 0.0f);
		// disabled.lineTo(0.0f, (float) h);
		// disabled.setPickable(false);
		// disabled.setStrokePaint(Color.gray);
		// addChild(disabled);
	}

	// public void redraw() {
	// //disabled.setVisible(!isEnabled());
	// }

	public boolean isEnabled() {
		return true;
	}

	public void showMessage() {
		getParent().addChild(message);
	}

	public void exit() {
		if (message != null) {
			PNode p = message.getParent();
			if (p != null) {
				p.removeChild(message);
			}
		}
		setMouseDoc(false);
	}

	public void enter() {
		setMouseDoc(true);
	}

	public void setMouseDoc(boolean state) {
		// override this
		// Util.print("Button.setMouseDoc " + state);
		if (getParent() instanceof MouseDoc)
			((MouseDoc) getParent()).setMouseDoc(this, state);
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
		System.err.println("Should override Buttons.mayHideTransients");
	}
}

class ButtonHandler extends MyInputEventHandler {

	public ButtonHandler() {
		super(Button.class);
	}

	// Pretty much anything you do on a button shouldn't go any further, so
	// always return true.

	public boolean click(PNode node) {
		((Button) node).pick();
		return true;
	}

	public boolean exit(PNode node) {
		((Button) node).exit();
		return true;
	}

	public boolean enter(PNode node) {
		((Button) node).enter();
		return true;
	}

	public void mayHideTransients(PNode node) {
		((Button) node).mayHideTransients(node);
	}

}