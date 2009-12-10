package edu.cmu.cs.bungee.client.viz;

import java.awt.Image;
import java.awt.image.BufferedImage;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.nodes.PImage;

class GridIm extends PImage implements GridElement {

	private final GridElementWrapper wrapper;

	GridIm(GridElementWrapper wrapper) {
		super();
		this.wrapper = wrapper;
	}

	// If cached value is the right size, or as big as possible, do nothing.
	// Otherwise recompute from itemImage
	public void setSize(int w, int h) {
		// Util.print("Enter scale " + this + " " + w + "x" + h + " "
		// + itemImage.currentW() + "x" + itemImage.currentH() + " "
		// + correctSize(w, h));
		if (!correctSize(w, h)) {
			int rawW = wrapper.itemImage.currentW();
			int rawH = wrapper.itemImage.currentH();
			double ratio = Math.min(w / (double) rawW, h / (double) rawH);
			int newW = (int) (rawW * ratio);
			int newH = (int) (rawH * ratio);
			// if (newW > 0 && newH > 0) {
			BufferedImage scaled = Util.resize(wrapper.itemImage.getRawImage(), newW,
					newH, true);
			setImage(scaled);
			// Util.printStackTrace();
			// Util.print(" " + scaled.getWidth() + "*" + scaled.getHeight());
			assert getWidth() <= w : w + " " + getWidth() + " "
					+ scaled.getWidth() + " " + newW + " " + newH;
			// }
		}
	}

	boolean correctSize(int w, int h) {
		int actualW = (int) Math.round(getWidth());
		int actualH = (int) Math.round(getHeight());
		int itemImageW = wrapper.itemImage.currentW();
		int itemImageH = wrapper.itemImage.currentH();

		// The best we can do is be exactly right, or be as big as we can and
		// still be too small
		boolean bestW = actualW == w
				|| (itemImageW < w && actualW == itemImageW);
		boolean bestH = actualH == h
				|| (itemImageH < h && actualH == itemImageH);

		// We'll be limited by either w or h, and the other dimension will be
		// just right or too small
		boolean result = (bestW && actualH <= h) || (bestH && actualW <= w);

		// boolean result = (actualW == w || (rawW < w && actualW == rawW) ||
		// (rawH < h && actualH == rawH));
		// Util
		// .print("bigEnough "
		// + item
		// + " "
		// + w
		// + " "
		// + rawW
		// + " "
		// + actualW
		// + " "
		// + h
		// + " "
		// + rawH
		// + " "
		// + actualH
		// + " "
		// + result);
		return result;
	}

	@Override
	public Image getImage() {
		Image result = super.getImage();
		if (result == null) { // || rawImage.getWidth(null) >
			// result.getWidth(null)) {
			result = wrapper.itemImage.getRawImage();
			// Util.print("getImage is setting image");
			// Util.printStackTrace();
			setImage(result);
		}
		return result;
	}

	@Override
	public void setImage(Image image) {
		super.setImage(image);
		wrapper.select();
	}

	public double getImageHeight() {
		return getImage().getHeight(null);
	}

	public double getImageWidth() {
		return getImage().getWidth(null);
	}

	public GridElementWrapper getWrapper() {
		return wrapper;
	}

}
