package log;

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;

import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;

class Xaxis extends LazyPNode {

	Date min;
	Date max;
	long zoomMin;
	long zoomMax;
	int nTicks = 5;
	double tickLength = 6;

	Xaxis(Date _min, Date _max) {
		min = _min;
		max = _max;
		zoomMin = min.getTime();
		zoomMax = max.getTime();
		addInputEventListener(new AxisEventHandler());
	}

	void draw(double length) {
		removeAllChildren();
		setBounds(0, 0, length, 100);
		for (int i = 0; i < nTicks; i++) {
			LazyPNode tick = new LazyPNode();
			double x = i * length / (nTicks - 1);
			tick.setBounds(x, 0, 1, tickLength);
			tick.setPaint(Color.black);
			addChild(tick);

			long mid = zoomMin + ((zoomMax - zoomMin) * i) / (nTicks - 1);
			APText label = new APText();
			label.setText(DateFormat.getDateInstance().format(new Date(mid)));
			label.setOffset(Math.round(x - label.getWidth() / 2), tickLength);
			addChild(label);
		}
	}

	double encode(Date value) {
		double percent = (value.getTime() - zoomMin)
				/ (double) (zoomMax - zoomMin);
		// Util.print("x encode " + percent + " " + value.getTime());
		return Math.round(getWidth() * percent);
	}

	private class AxisEventHandler extends MyInputEventHandler {

		private double dragStartOffset;

		AxisEventHandler() {
			super(Xaxis.class);
		}

		@Override
		protected boolean press(PNode node, PInputEvent e) {
			assert node == Xaxis.this;
			dragStartOffset = e.getPositionRelativeTo(node).getX();
			return true;
		}

		@Override
		protected boolean drag(PNode node, PInputEvent e) {
			assert Util.ignore(node);
			PDimension delta = e.getDelta();
			double vertical = delta.getHeight();
			double horizontal = delta.getWidth();
			// If you want to just pan, zooming screws you up, and vice-versa,
			// so
			// choose one or the other.
			if (Math.abs(vertical) > Math.abs(horizontal))
				horizontal = 0;
			else
				vertical = 0;
			double deltaZoom = Math.pow(2, -vertical / 20.0);
			long minTime = min.getTime();
			long maxTime = max.getTime();
			long range = maxTime - minTime;
			long zoomRange = zoomMax - zoomMin;
			double logicalWidth = getWidth() * range / zoomRange;
			double leftEdge = getWidth() * (zoomMin - minTime) / zoomRange;
			double newLogicalWidth = logicalWidth * deltaZoom;
			double newLeftEdge = 0;
			if (newLogicalWidth < getWidth()) {
				zoomMin = minTime;
				zoomMax = maxTime;
			} else {
				// recalculate zoom after rounding newLogicalWidth
				deltaZoom = newLogicalWidth / logicalWidth;
				double pan = -horizontal;
				newLeftEdge = Util.constrain(leftEdge + pan
						+ (leftEdge + dragStartOffset) * (deltaZoom - 1), 0,
						newLogicalWidth - getWidth());
				zoomMin = minTime + (long) (newLeftEdge * range / newLogicalWidth);
				zoomMax = minTime
						+ (long) ((newLeftEdge + getWidth()) * range / newLogicalWidth);
			}
			((Chart) getParent()).redraw();
			return true;
		}
	}
}
