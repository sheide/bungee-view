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

import edu.umd.cs.piccolo.PNode;

import java.awt.Color;
import java.awt.Font;

public class MenuButton extends APText {

	private final String desc; // = "Select this menu choice";
	
	final Object data;

	static final MenuButtonClickHandler menuButtonClickHandler = new MenuButtonClickHandler();

	public MenuButton(String label, String _desc, Color BG, Color FG, Font _font, Object _data) {
		super(_font);
		data = _data;
		desc = _desc;
		if (FG != null)
			setTextPaint(FG);
		if (BG != null)
			setPaint(BG);
		setWrapOnWordBoundaries(false);
		setText(label);
		setConstrainHeightToTextHeight(false);
		setHeight(Math.ceil(getHeight()));
		addInputEventListener(menuButtonClickHandler);
	}

	void draw(double y, boolean visible) {
		setVisible(visible);
		setPickable(visible);
		if (visible) {
			getParent().moveToFront();
		}
		setOffset(getXOffset(), y);
	}

	void pick() {
		((Menu) getParent()).choose(getText(), data);
	}

	public void setW(double w) {
		setConstrainWidthToTextWidth(false);
		setWidth(w);
	}

	//    void pickDoc() {
	//        ((Menu) getParent()).setDoc(desc);
	//    }

	 void setMouseDoc(boolean state) {
		if (getParent() instanceof MouseDoc)
			((MouseDoc) getParent()).setMouseDoc(state ? desc : null);
	}
}

class MenuButtonClickHandler extends MyInputEventHandler {

	MenuButtonClickHandler() {
		super(MenuButton.class);
	}

	//    public boolean enter(PNode node) {
	//    	((MenuButton) node).highlight(true);
	//        return true;
	//    }

	//  public boolean exit(PNode node) {
	//  	((MenuButton) node).highlight(false);
	//      return true;
	//  }

	protected boolean click(PNode node) {
		((MenuButton) node).pick();
		return true;
	}

	protected boolean exit(PNode node) {
		((MenuButton) node).setMouseDoc(false);
		return true;
	}

	protected boolean enter(PNode node) {
		((MenuButton) node).setMouseDoc(true);
		return true;
	}
}