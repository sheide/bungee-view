/* 

 Created on Mar 4, 2005

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

import edu.umd.cs.piccolo.PNode;

public class Menu extends PNode implements MouseDoc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String mouseDoc = "Open this menu";

	MenuButton[] buttons;

	public APText value;

	// int buttonCount = 0;

	Paint bg;

	Color fg;

	boolean visible = false;

	double w = 0.0;

	// protected Runnable action;

	private Font font;

	// private Object data;

	// private int alignment; // 0 = left; 1 = middle; 2 = right

	static final MenuClickHandler menuClickHandler = new MenuClickHandler();

	public Menu(Paint _bg, Color _fg, Font _font) {
		bg = _bg;
		fg = _fg;
		// action = _action;
		font = _font;
		// alignment = _alignment;
		value = new APText(font);
		value.setPaint(bg);
		value.setWrapOnWordBoundaries(false);
		setJustification(Component.CENTER_ALIGNMENT);
		value.setTextPaint(fg);
		value.setUnderline(true);
		value.setPickable(false);
		value.setConstrainWidthToTextWidth(false);
		value.setConstrainHeightToTextHeight(false);
		addChild(value);
		addInputEventListener(menuClickHandler);
	}

	public void setJustification(float just) {
		value.setJustification(just);
	}

	// public void addButton(String label, String mouseDoc1) {
	// addButton(label, mouseDoc1, null);
	// }

	public void addButton(MenuItem item) {
		// System.out.println("Menu.addButton " + label);
		int oldNbuttons = buttons == null ? 0 : buttons.length;
		MenuButton[] pButton = new MenuButton[oldNbuttons + 1];
		for (int i = 0; i < oldNbuttons; i++) {
			pButton[i] = buttons[i];
		}
		MenuButton b = new MenuButton(item, bg, fg, font);
		addChild(b);
		// b.addInputEventListener(menuClickHandler);
		pButton[oldNbuttons] = b;
		// float labelW = font.width(label) + 10.0;
		// if (w < labelW)
		// w = labelW * 1.8;
		buttons = pButton;
		if (oldNbuttons == 0) {
			value.setHeight(b.getHeight());
			value.setText(item.getLabel());
		}
		double newW = b.getWidth();
		if (newW > w) {
			setWidth(newW);
		}
		b.setW(w);
		draw();
	}

	// public void addButton(String label, String mouseDoc1, Object _data) {
	// // System.out.println("Menu.addButton " + label);
	// int oldNbuttons = buttons == null ? 0 : buttons.length;
	// MenuButton[] pButton = new MenuButton[oldNbuttons + 1];
	// for (int i = 0; i < oldNbuttons; i++) {
	// pButton[i] = buttons[i];
	// }
	// DefaultMenuItem defaultMenuItem = new DefaultMenuItem(action,label,
	// mouseDoc1);
	// defaultMenuItem.object=_data;
	// MenuButton b = new MenuButton(defaultMenuItem, bg, fg, font);
	// addChild(b);
	// // b.addInputEventListener(menuClickHandler);
	// pButton[oldNbuttons] = b;
	// // float labelW = font.width(label) + 10.0;
	// // if (w < labelW)
	// // w = labelW * 1.8;
	// buttons = pButton;
	// if (oldNbuttons == 0) {
	// value.setHeight(b.getHeight());
	// value.setText(label);
	// }
	// double newW = b.getWidth();
	// if (newW > w) {
	// setWidth(newW);
	// }
	// b.setW(w);
	// draw();
	// }

	public void setText(String desc) {
		// System.out.println("Menu.setText " + desc);
		value.setConstrainWidthToTextWidth(true);
		value.setText(desc);
		double newW = value.getWidth();
		value.setConstrainWidthToTextWidth(false);
		// System.out.println(newW + " " + value.getWidth() + " " +
		// getHeight());
		if (true || newW > w) {
			setWidth(Math.max(w, newW));
		}
	}

	public boolean setWidth(double width) {
		w = Math.ceil(width);
		value.setWidth(w);
		setBounds(value.getBounds());
		int oldNbuttons = buttons == null ? 0 : buttons.length;
		for (int i = 0; i < oldNbuttons; i++) {
			buttons[i].setW(w);
		}
		return super.setWidth(w);
	}

	void draw() {
		double windowH = getRoot() == null ? 999999 : getRoot().getHeight();
		double gy = getGlobalBounds().y;
		double y = value.getHeight();
		double h = 0.0;
		for (int i = 0; i < buttons.length; i++) {
			h += buttons[i].getHeight();
		}
		double delta = 1;
		if ((y + h) * getGlobalScale() + gy > windowH
				&& h * getGlobalScale() <= gy) {
			delta = -1;
		}
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].draw(delta * y, visible);
			y += buttons[i].getHeight();
		}
	}

	public int nChoices() {
		if (buttons == null)
			return 0;
		return buttons.length;
	}

	public void choose(int buttonIndex) {
		MenuButton button = buttons[buttonIndex];
		choose(button.item);
	}

	public void choose(MenuItem item) {
		visible = true; // choose calls pick, which will toggle visibility
		// System.out.println("Menu.choose " + choice);
		// data = item;
		pick();
		// action.run();
		String newValue = item.doCommand();
		if (newValue != null)
			value.setText(newValue);
	}

	public String getChoice() {
		// System.out.println("Menu.getChoice " + value.getText());
		return value.getText();
	}

	// public Object getData() {
	// return data;
	// }

	public void pick() {
		// System.out.println("Menu.pick");
		visible = !visible;
		setMouseDoc(true);
		draw();
	}

	void setMouseDoc(boolean state) {
		if (getParent() instanceof MouseDoc) {
			String doc = state ? (visible ? "Close this menu without doing anything"
					: mouseDoc)
					: null;
			((MouseDoc) getParent()).setMouseDoc(doc);
		}
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// setMouseDoc(state ? ((Button) source).mouseDoc : null);
	// }

	public void setMouseDoc(String doc) {
		if (getParent() instanceof MouseDoc)
			((MouseDoc) getParent()).setMouseDoc(doc);
	}

	// public void setMouseDoc(Vector doc, boolean state) {
	// if (getParent() instanceof MouseDoc)
	// ((MouseDoc) getParent()).setMouseDoc(doc, state);
	// }

	public void setFont(Font font2) {
		if (!font2.equals(font)) {
			double scale = font2.getSize2D() / font.getSize2D();
			setScale(scale);
		}
	}

	public Button getButton(String label) {
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i].getText().equals(label))
				return buttons[i];
		}
		return null;
	}

}

final class MenuClickHandler extends MyInputEventHandler {

	MenuClickHandler() {
		super(Menu.class);
	}

	// public boolean enter(PNode node) {
	// ((Menu) node).highlight(true);
	// return true;
	// }

	// public boolean exit(PNode node) {
	// ((Menu) node).highlight(false);
	// return true;
	// }

	public boolean click(PNode node) {
		((Menu) node).pick();
		return true;
	}

	public boolean exit(PNode node) {
		((Menu) node).setMouseDoc(false);
		return false;
	}

	public boolean enter(PNode node) {
		((Menu) node).setMouseDoc(true);
		return false;
	}
}