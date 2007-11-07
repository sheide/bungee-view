package edu.cmu.cs.bungee.client.viz;

import java.awt.Image;
import java.awt.image.BufferedImage;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PImage;

final class GridImage extends PImage /* implements FacetNode */{

	int offset;

	/**
	 * This is the image as read from the database, of which we are a scaled
	 * version. Multiple GridImages can have the same ItemImage (well, two
	 * anyway, for the grid and the selectedItem).
	 */
	ItemImage itemImage;

	static final GridImageHandler gridImageHandler = new GridImageHandler();

	// not currently used - called by MyTile
	// GridImage(Art _art, int _item, BufferedImage image) {
	// itemImage = new ItemImage(_art, _item);
	// if (image != null)
	// itemImage.setRawImage(image);
	// addInputEventListener(gridImageHandler);
	// }

	GridImage(ItemImage im) {
		itemImage = im;
		addInputEventListener(gridImageHandler);
	}

	public Image getImage() {
		Image result = super.getImage();
		Image rawImage = itemImage.getRawImage();
		if (result == null) { // || rawImage.getWidth(null) >
			// result.getWidth(null)) {
			result = rawImage;
			// Util.print("getImage is setting image");
			// Util.printStackTrace();
			setImage(result);
		}
		return result;
	}

	// If cached value is the right size, or as big as possible, do nothing.
	// Otherwise recompute from itemImage
	void scale(int w, int h) {
		// Util.print("Enter scale");
		if (!correctSize(w, h)) {
			int rawW = itemImage.currentW();
			int rawH = itemImage.currentH();
			double ratio = Math.min(w / (double) rawW, h / (double) rawH);
			int newW = (int) (rawW * ratio);
			int newH = (int) (rawH * ratio);
			// if (newW > 0 && newH > 0) {
			BufferedImage scaled = Util.resize(itemImage.getRawImage(), newW,
					newH, true);
			setImage(scaled);
			// Util.printStackTrace();
			// Util.print(" " + scaled.getWidth() + "*" + scaled.getHeight());
			assert getWidth() <= w : w + " " + getWidth() + " "
					+ scaled.getWidth() + " " + newW + " " + newH;
			// }
		}
		assert getWidth() <= w : w + " " + getWidth();
		// Util.print(itemImage.currentW() + "*" + itemImage.currentH() + " "
		// + itemImage.getRawImage().getWidth(null) + "*"
		// + itemImage.getRawImage().getHeight(null));
		// Util.print(" Exit scale");
	}

	boolean correctSize(int w, int h) {
		int actualW = (int) Math.round(getWidth());
		int actualH = (int) Math.round(getHeight());
		int itemImageW = itemImage.currentW();
		int itemImageH = itemImage.currentH();

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

	// void setArea(double area) {
	// double scale = area / (itemImage.rawW() * itemImage.rawH());
	// if (scale > 1)
	// scale = 1;
	// else
	// scale = Math.sqrt(scale);
	// int w = Util.round(scale * itemImage.rawW());
	// int h = Util.round(scale * itemImage.rawH());
	// // Util.print("setArea " + area + " " + itemImage.rawW() + " " +
	// // itemImage.rawH() + " " + scale + " " + w + "x" + h);
	// scale(w, h);
	// }

	ResultsGrid grid() {
		return itemImage.art.grid;
	}

	// public boolean pick(PInputEvent e) {
	// return pick(e.getModifiersEx());
	// }

	 boolean pick(boolean middleButton, boolean rightButton) {
		// parentGrid.art.printUserAction("GridImage pick");
		if (!((middleButton || rightButton) &&
			grid().art.itemMiddleMenu(itemImage.item, rightButton))) {
			assert offset >= 0;
			assert offset < grid().onCount;
			grid().computeSelectedItemFromSelectedOffset(offset, -1);
		}
		return true;
	}

	String mouseDoc = "Select this item";

	private String getMouseDoc() {
		// if (mouseDoc == null) {
		// mouseDoc = "Select this item"; // +
		// // parentGrid.art.query.genericObjectLabel;
		// }
		return mouseDoc;
	}

	 boolean highlight(boolean state) {
		if (!state) {
			grid().art.setClickDesc((String) null);
			
			// buttons don't make sense for highlight
//		} else if (Query.isEditable && (middleButton || rightButton)) {
//				grid().art.setClickDesc("Open edit menu");	
			
		} else if (itemImage.item != grid().selectedItem) {
			grid().art.setClickDesc(getMouseDoc());
		} else
			return false;
		return true;
	}

	 void mayHideTransients(PNode ignore) {
		assert Util.ignore(ignore);
		grid().mayHideTransients();
	}

}

final class GridImageHandler extends MyInputEventHandler {

	GridImageHandler() {
		super(GridImage.class);
	}

	protected boolean enter(PNode node, PInputEvent e) {
		// Util.print("FacetClickHandler.enter");
		return ((GridImage) node).highlight(true);
	}

	protected boolean exit(PNode node) {
		return ((GridImage) node).highlight(false);
	}

	protected boolean click(PNode node, PInputEvent e) {
		return ((GridImage) node).pick(e.isMiddleMouseButton(), e
				.isRightMouseButton());
	}

	protected void mayHideTransients(PNode node) {
		((GridImage) node).mayHideTransients(node);
	}
}