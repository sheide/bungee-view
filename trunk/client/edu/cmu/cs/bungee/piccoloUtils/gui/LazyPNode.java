/* 

 Created on Mar 10, 2006

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

import edu.umd.cs.piccolo.PNode;

public class LazyPNode extends PNode {

	public void setOffset(double x, double y) {
		assert x == Math.round(x) : x;
		assert y == Math.round(y) : y;
		if (x != getXOffset() || y != getYOffset())
			super.setOffset(x, y);
	}

	public void setXoffset(double x) {
		setOffset(x, getYOffset());
	}

	public void setYoffset(double y) {
		setOffset(getXOffset(), y);
	}

	public boolean setBounds(double x, double y, double w, double h) {
//		assert x == Math.round(x) : x;
//		assert y == Math.round(y) : y;
//		assert w == Math.round(w) : w;
//		assert h == Math.round(h) : h;
		return super.setBounds(x, y, w, h);
	}

	public void setScale(double scale) {
		if (scale != getScale())
			scale(scale / getScale());
	}
	
	public double getCenterX() {
		return getXOffset() + getWidth() / 2.0;
	}
	
	public double getCenterY() {
		return getYOffset() + getHeight() / 2.0;
	}

	public double getMaxX() {
		return getXOffset() + getWidth() * getScale();
	}

	public double getMaxY() {
		return getYOffset() + getHeight() * getScale();
	}

	public void repaintNow() {
		validateFullBounds();
		validateFullPaint();
	}

	public void updateBoundary(Boundary boundary) {
		System.out.println("LazyPNode.updateBoundary " + this + " "
				+ boundary.center());
		setWidth(boundary.center());
	}

	// Subclasses caring about boundaries should override either the one-argument or two-argument
	// version of min/max width/height
	public double minWidth(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
		return minWidth();
	}

	public double maxWidth(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
		return maxWidth();
	}

	public double minHeight(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
		return minHeight();
	}

	public double maxHeight(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
		return maxHeight();
	}

	public double minWidth() {
		System.err.println("Should override LazyPNode.minWidth");
		return getWidth();
	}

	public double maxWidth() {
		System.err.println("Should override LazyPNode.maxWidth");
		return getWidth();
	}

	public double minHeight() {
		System.err.println("Should override LazyPNode.minHeight");
		return getWidth();
	}

	public double maxHeight() {
		System.err.println("Should override LazyPNode.maxHeight");
		return getWidth();
	}

	public void enterBoundary(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
	}

	public void exitBoundary(Boundary boundary) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(boundary);
	}

	public void addChild(PNode child) {
		if (child.getParent() != this)
			super.addChild(child);
	}

	public void moveToFront() {
		PNode p = getParent();
		if (p != null && p.getChild(p.getChildrenCount() - 1) != this)
			super.moveToFront();
	}

	public void moveToBack() {
		PNode p = getParent();
		if (p != null && p.getChild(0) != this)
			super.moveToBack();
	}
	
	public static void colorChildrenRandomly(PNode node) {
		node.setTransparency(0.5f);
		node.setPaint(Color.getHSBColor((float) Math.random(), 1, 1));
		for (int i=0; i<node.getChildrenCount(); i++)
			colorChildrenRandomly(node.getChild(i));
	}
}
