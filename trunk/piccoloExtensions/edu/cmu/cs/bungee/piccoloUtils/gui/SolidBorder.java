package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

public class SolidBorder extends BevelBorder {

	private static final long serialVersionUID = 1L;

	public SolidBorder(Color color) {
		super(BevelBorder.LOWERED,
		/* highlightOuter */color,
		/* highlightInner */color);
	}

	protected void paint(PPaintContext paintContext) {
		if (strokeW > 0 && getTransparency() > 0) {
			Graphics2D g = paintContext.getGraphics();
			g.setStroke(LazyPPath.getStrokeInstance(strokeW));
			g.setPaint(highlightOuter);

			PBounds bounds = getBorderBounds();
			// each stroke is centered on an edge, so divide by 2
			double halfWidth = strokeW / 2.0;
			double x = bounds.getX() - halfWidth;
			double y = bounds.getY() - halfWidth;
			double right = bounds.getMaxX() + halfWidth;
			double bottom = bounds.getMaxY() + halfWidth;
			g.drawRect((int) x, (int) y, (int) (right - x), (int) (bottom - y));

			setBounds(x - halfWidth, y - halfWidth, right - x + strokeW, bottom
					- y + strokeW);
		}
	}
}
