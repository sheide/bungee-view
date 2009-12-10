package edu.cmu.cs.bungee.client.viz;

import edu.cmu.cs.bungee.piccoloUtils.gui.APText;

public class GridText extends APText implements GridElement {

	private final GridElementWrapper wrapper;

	GridText(GridElementWrapper wrapper) {
		super(wrapper.art().font);
		this.wrapper = wrapper;
		setTextPaint(Bungee.gridFG);
		setConstrainWidthToTextWidth(false);
		setConstrainHeightToTextHeight(false);
//		setWrapOnWordBoundaries(false);
		setText(wrapper.itemImage.getName());
//		Util.print(getText());
//		setPaint(Color.gray);
	}

	public double getImageHeight() {
		return getHeight();
	}

	public double getImageWidth() {
		return getWidth();
	}

	public GridElementWrapper getWrapper() {
		return wrapper;
	}

	public void setSize(int w, int h) {
		setWidth(w-3);
		setHeight(h-3);
	}

}
