package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Paint;

/**
 * Nodes that don't need paint, which will be more efficient if the bounds are
 * empty, but for which having an abstract width and height is convenient for
 * laying out children.
 * 
 */
public class LazyContainer extends LazyPNode {
	
	public double w,h;
	public boolean setBounds(double x, double y, double width, double height) {
		assert x==0;
		assert y==0;
		boolean result = width !=w||height!=h;
		w=width;
		h=height;
		invalidateLayout();
		return result;
	}
	
	public double getWidth() {
		return w;
	}
	
	public double getHeight() {
		return h;
	}
	
	public void setPaint(Paint paint) {
		assert false;
	}

}
