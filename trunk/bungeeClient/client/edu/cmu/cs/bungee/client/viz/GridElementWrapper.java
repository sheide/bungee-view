package edu.cmu.cs.bungee.client.viz;

//import edu.cmu.cs.bungee.faceImage.FaceImage;
import java.util.HashSet;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.cmu.cs.bungee.piccoloUtils.gui.SolidBorder;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;

class GridElementWrapper {
	/**
	 * This is the image as read from the database, of which we are a scaled
	 * version. Multiple GridImages can have the same ItemImage (well, two
	 * anyway, for the grid and the selectedItem).
	 */
	ItemImage itemImage;

	SolidBorder border;

	private GridElement gridElement;

	int offset;

	private float prevBorderTransparency = 0;

	private float goalBorderTransparency = 0;

	private final boolean useDescIfNoImage;

	static final GridElementHandler gridElementHandler = new GridElementHandler();

	// not currently used - called by MyTile
	// GridImage(Art _art, int _item, BufferedImage image) {
	// itemImage = new ItemImage(_art, _item);
	// if (image != null)
	// itemImage.setRawImage(image);
	// addInputEventListener(gridImageHandler);
	// }

	GridElementWrapper(ItemImage im, boolean useDescIfNoImage) {
		itemImage = im;
		this.useDescIfNoImage=useDescIfNoImage;
//		mouseDoc = "Select this " + art().query.getGenericObjectLabel(false);

		border = new SolidBorder(Bungee.gridFG.brighter());
		// setPaint(Bungee.gridFG);
		// Need to have non-zero size in order to be painted. paint will set the
		// real border.
		// border.setBounds(0, 0, 10, 10);
		border.setTransparency(0);
		// border.strokeW = 2;
		select();
	}

//	void addInputEventListener(MyInputEventHandler listener) {
//		getGridElement().addInputEventListener(listener);
//	}

	double getWidth() {
		return getGridElement().getImageWidth();
	}

	double getHeight() {
		return getGridElement().getImageHeight();
	}

	// public void setImage(Image image) {
	// super.setImage(image);
	// select();
	// }

	ResultsGrid grid() {
		return itemImage.art.grid;
	}

	Bungee art() {
		return itemImage.art;
	}

	@Override
	public String toString() {
		return "<GridImage " + itemImage + ">";
	}

	// public boolean pick(PInputEvent e) {
	// return pick(e.getModifiersEx());
	// }

	boolean pick(boolean middleButton, boolean rightButton) {
		// parentGrid.art.printUserAction("GridImage pick");
		if (!((middleButton || rightButton) && art().itemMiddleMenu(
				itemImage.item, rightButton))) {
			assert offset >= 0;
			assert offset < grid().onCount : offset + " " + grid().onCount;
			grid().computeSelectedItemFromSelectedOffset(offset, -1);
		}
		return true;
	}

//	String mouseDoc = "Select this match";

	// private FaceImage faceImage;

	private String getMouseDoc() {
		// if (mouseDoc == null) {
		// mouseDoc = "Select this result"; // +
		// // parentGrid.art.query.genericObjectLabel;
		// }
		return "Select this " + art().query.getGenericObjectLabel(false);
	}

	boolean highlight(boolean state) {
		art().highlightFacets(new HashSet<Perspective>(itemImage.facets), state);
		select();
		animateSelection(1f);
		if (!state) {
			art().setClickDesc((String) null);

			// buttons don't make sense for highlight
			// } else if (Query.isEditable && (middleButton || rightButton)) {
			// grid().art.setClickDesc("Open edit menu");

		} else if (itemImage.item != grid().selectedItem) {
			art().setClickDesc(getMouseDoc());
		} else
			return false;
		return true;
	}

	boolean select() {
		// Util.print("kk " + itemImage.item + " " + itemImage.facets);
		prevBorderTransparency = border.getTransparency();
		goalBorderTransparency = Util.intersects(itemImage.facets,
				art().highlightedFacets) ? 1 : 0;
		return goalBorderTransparency != prevBorderTransparency;
	}

	void animateSelection(float zeroToOne) {
		// border.bevelType = isSelected ? BevelBorder.RAISED
		// : BevelBorder.LOWERED;
		border.setTransparency(Util.interpolate(prevBorderTransparency,
				goalBorderTransparency, zeroToOne));
	}

	void mayHideTransients() {
		grid().mayHideTransients();
	}

	public PNode getParent() {
		return getGridElement().getParent();
	}

	void setSize(int w, int h) {
		GridElement gridElement2 = getGridElement();
		if (gridElement2 != null)
			gridElement2.setSize(w, h);
	}

	public PNode pNode() {
		return (PNode) getGridElement();
	}

	GridElement getGridElement() {
		if (gridElement == null) {
			if (useDescIfNoImage&&itemImage.rawImage == art().getMissingImage()) {
				gridElement = new GridText(this);
			} else if (itemImage.rawImage != null) {
				gridElement = new GridIm(this);
			}
			if (gridElement != null) {
				gridElement.addInputEventListener(gridElementHandler);
				gridElement.addChild(border);
			}
		}
		return gridElement;
	}

	void setOffset(double x, double y) {
		getGridElement().setOffset(x, y);
	}

	public double getXOffset() {
		return getGridElement().getXOffset();
	}

	public double getYOffset() {
		return getGridElement().getYOffset();
	}

	public double getScale() {
		return getGridElement().getScale();
	}

	// void handleFaceWarping(PInputEvent e) {
	// Util.print(e.getPositionRelativeTo(this));
	// if (faceImage == null) {
	// faceImage = FaceImageThreePoint.getInstamce(getImage());
	// }
	// faceImage.addPoint(e.getPositionRelativeTo(this));
	// if (faceImage.isSavable()) {
	// ((SelectedItem) getParent()).art.warpImage(itemImage.item,
	// faceImage);
	// // itemImage.rawImage=faceImage.getWarpedImage();
	// // ((SelectedItem) getParent()).art.selectedItem.maybeAddImage();
	// }
	// }
}

final class GridElementHandler extends MyInputEventHandler {

	GridElementHandler() {
		super(GridElement.class);
	}

	private GridElementWrapper getWrapper(PNode node) {
		return ((GridElement) node).getWrapper();
	}

	@Override
	public boolean enter(PNode node, PInputEvent e) {
		// Util.print("GridElementHandler.enter");
		return getWrapper(node).highlight(true);
	}

	@Override
	public boolean exit(PNode node) {
		return getWrapper(node).highlight(false);
	}

	@Override
	public boolean click(PNode node, PInputEvent e) {
		return getWrapper(node).pick(e.isMiddleMouseButton(),
				e.isRightMouseButton());
	}

	@Override
	public void mayHideTransients(PNode node) {
		getWrapper(node).mayHideTransients();
	}
}
