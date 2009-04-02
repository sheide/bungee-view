/**
 * 
 */
package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;

import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;

final class ColorKey extends LazyPNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final Bungee art;
	final ColorKeyHover colorKeyHover = new ColorKeyHover();

	ColorKey(Bungee art) {
		this.art = art;
		Color[][] colors = { Markup.INCLUDED_COLORS,
				Markup.POSITIVE_ASSOCIATION_COLORS, Markup.UNASSOCIATED_COLORS,
				Markup.NEGATIVE_ASSOCIATION_COLORS, Markup.EXCLUDED_COLORS };
		String[] msgs = {
				"Tag required by filters",
				"Tag associated with filters; Items with this tag are more likely to satisfy the filters",
				"Unassociated tag; There is no statistically significant association between this tag and the filters",
				"Tag negatively associated with filters; Items with this tag are less likely to satisfy the filters",
				"Tag prohibited by filters" };
		int nColors = colors.length;
		if (!art.getIsShortcuts() && !art.getShowCheckboxes())
			nColors--;
		APText label1 = art.oneLineLabel();
		label1.setTextPaint(Bungee.headerFG);
		label1.setText("Color Key");
		// this.summary.label.setPickable(false);
		addChild(label1);
		// if (label1.getMaxX() > this.summary.w)
		// label1.setScale(this.summary.w / label1.getMaxX());
		double buttonW = Math.round(label1.getMaxX() / nColors);
		// double y = 1.5 * art.lineH;
		// double buttonH = Math.round(h - y);
		double buttonH = label1.getHeight();
		double y = label1.getMaxY();
		for (int i = 0; i < nColors; i++) {
			PNode node = new ColorKeyKey(colors[i][0], colors[i][1], msgs[i],
					buttonW, buttonH);
			node.setOffset(i * buttonW, y);
			addChild(node);
		}
	}

	private class ColorKeyKey extends LazyPNode {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Color color;
		Color highlight;
		String msg;

		ColorKeyKey(Color _color, Color _highlight, String _msg, double w,
				double h) {
			color = _color;
			highlight = _highlight;
			msg = _msg;
			setBounds(0, 0, w, h);
			setPaint(color);
			addInputEventListener(colorKeyHover);
		}

		void enter() {
			setPaint(highlight);
			// APText text = art.oneLineLabel();
			// text.setPaint(Bungee.summaryBG);
			// text.setTextPaint(Bungee.helpColor);
			// text.setText(msg);
			// text.setOffset(getX(), getMaxY() - text.getHeight());
			// addChild(text);
			art.setNonClickMouseDoc(msg);
		}

		void exit() {
			setPaint(color);
			removeAllChildren();
		}
	}

	private final class ColorKeyHover extends MyInputEventHandler {

		// private ColorKey colorKey;

		ColorKeyHover() {
			super(ColorKeyKey.class);
			// colorKey = _colorKey;
		}

		public boolean enter(PNode node) {
			// Util.print("SummaryTextHover.enter " + node);
			((ColorKeyKey) node).enter();
			return true;
		}

		public boolean exit(PNode node, PInputEvent e) {
			((ColorKeyKey) node).exit();
			return true;
		}
	}

}