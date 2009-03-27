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

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.nodes.PPath;

public class LazyPPath extends PPath implements Cloneable {

	// public boolean setBounds(double x, double y, double w, double h) {
	// assert x == Math.round(x);
	// assert y == Math.round(y);
	// assert w == Math.round(w);
	// assert h == Math.round(h);
	// return super.setBounds(x, y, w, h);
	// }

	public static final Stroke[] strokeCache = new BasicStroke[11];

	public LazyPPath() {
		super(new GeneralPath(), null);
	}

	public void setOffset(double x, double y) {
		assert x == Math.round(x);
		assert y == Math.round(y);
		if (x != getXOffset() || y != getYOffset())
			super.setOffset(x, y);
	}

	/**
	 * set x offset
	 * @param x
	 */
	public void setXoffset(double x) {
		setOffset(x, getYOffset());
	}

	/**
	 * Set y offset
	 * @param y
	 */
	public void setYoffset(double y) {
		setOffset(getXOffset(), y);
	}

	public void setScale(double scale) {
		if (scale != getScale())
			super.setScale(scale);
	}

	public void setStrokePaint(Paint aPaint) {
		if (aPaint == null) {
			if (getStrokePaint() != null)
				super.setStrokePaint(null);
		} else if (!aPaint.equals(getStrokePaint()))
			super.setStrokePaint(aPaint);
	}

	public void setStroke(Stroke aStroke) {
		if (aStroke == null) {
			if (getStroke() != null)
				super.setStroke(null);
		} else if (!aStroke.equals(getStroke()))
			super.setStroke(aStroke);
	}

	public static PPath createLine(float x1, float y1, float x2, float y2,
			Stroke stroke) {
		LazyPPath path = new LazyPPath();
		float[] Xs = { x1, x2 };
		float[] Ys = { y1, y2 };
		path.setPathToPolyline(Xs, Ys);
		path.setStroke(stroke);
		return path;
	}

	public final Object clone() {
		LazyPPath result = (LazyPPath) super.clone();
		result.setPaint(getPaint());
		result.setStroke(getStroke());
		result.setStrokePaint(getStrokePaint());
		result.setPathTo(getPathReference());
		return result;
	}

	public static Stroke getStrokeInstance(int i) {
		assert i >= 0;
		if (i >= strokeCache.length)
			return new BasicStroke(i);
		Stroke result = strokeCache[i];
		if (result == null) {
			result = new BasicStroke(i);
			strokeCache[i] = result;
		}
		return result;
	}

	/* 
	 * This is more efficient when the Stroke is a BasicStroke, which we assume it always will be.
	 * (non-Javadoc)
	 * @see edu.umd.cs.piccolo.nodes.PPath#getPathBoundsWithStroke()
	 */
	public Rectangle2D getPathBoundsWithStroke() {
		Rectangle2D rect = getPathReference().getBounds2D();
		if (getStroke() != null) {
			double dx = ((BasicStroke) getStroke()).getLineWidth();
			rect.setRect(rect.getX() - dx / 2, rect.getY() - dx / 2, rect
					.getWidth()
					+ dx, rect.getHeight() + dx);
		}
		return rect;
	}

}
