package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;

import edu.cmu.cs.bungee.piccoloUtils.gui.TextButton;
import edu.umd.cs.piccolo.PNode;

class BungeeTextButton extends TextButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Bungee art;

	BungeeTextButton(String text, Color textColor, Color bgColor, Bungee _art,
			String documentation) {
		super(text, _art.font, 0, 0, -1 /* _art.getStringWidth(text) + 2 */,
				-1 /* _art.lineH + 2 */, null, documentation, 1.8f, textColor,
				bgColor);
		art = _art;
	}

	public double minWidth() {
		// return Math.ceil(publicScale * (art.getStringWidth(getText()) + 2));
		return outerW();
	}

	// public void setMouseDoc(boolean state) {
	// art.setMouseDoc(mouseDoc);
	// }

	public void mayHideTransients(PNode node) {
		art.mayHideTransients();
	}

}
