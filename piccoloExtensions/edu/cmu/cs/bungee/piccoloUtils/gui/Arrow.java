package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import edu.cmu.cs.bungee.javaExtensions.graph.Edge;
import edu.umd.cs.piccolo.event.PInputEventListener;

/**
 * There are two ways to use Arrow. Preferred is setEndpoints, where "left" and
 * "right" are just names for the two ends, with leftTail and rightHead being
 * visible by default. Arrow sets a transform to place the ends.
 * 
 * PerspectiveViz uses the transform to scale the bars, so handles Arrow
 * placement on its own. It sets head visibility to point left or right as
 * appropriate. xOffset is always for the tail.
 * 
 */
public class Arrow extends LazyPNode {
	public static final int LEFT_TAIL = 1;
	public static final int LEFT_HEAD = 2;
	public static final int RIGHT_TAIL = 3;
	public static final int RIGHT_HEAD = 4;
	public static final int LINE = 5;
	LazyPPath line;
	LazyPPath leftTail;
	LazyPPath rightTail;
	LazyPPath leftHead; // always points to the left
	LazyPPath rightHead;

	public Arrow(Paint color, int size, int length) {
		setHeight(size);
		line = new LazyPPath();
		// float[] xp = { 0, length };
		// float[] yp = { 0, 0 };
		// line.setPathToPolyline(xp, yp);
		leftHead = new LazyPPath();
		rightHead = new LazyPPath();
		line.setStroke(LazyPPath.getStrokeInstance(1));
		// leftHead.setStroke(LazyPPath.getStrokeInstance(1));
		// rightHead.setStroke(LazyPPath.getStrokeInstance(1));
		leftTail = new LazyPPath();
		rightTail = new LazyPPath();
		setSize(LEFT_HEAD, size);
		setSize(RIGHT_HEAD, size);
		setSize(LEFT_TAIL, size);
		setSize(RIGHT_TAIL, size);
		setVisible(LEFT_HEAD, false);
		setVisible(RIGHT_TAIL, false);
		addChild(line);
		addChild(leftHead);
		addChild(rightHead);
		addChild(leftTail);
		addChild(rightTail);
		setLength(length);
		setStrokePaint(color);
	}

	public void setVisible(int part, boolean state) {
		LazyPPath node = getPart(part);
		node.setVisible(state);
		node.setPickable(state);
	}

	public void setStrokePaint(int part, Paint color) {
		switch (part) {
		case LEFT_HEAD:
		case RIGHT_HEAD:
		case LEFT_TAIL:
		case RIGHT_TAIL:
			getPart(part).setPaint(color);
			break;
		case LINE:
			line.setStrokePaint(color);
			colorLabels(color);
			break;
		default:
			assert false;
			break;
		}
	}

	private LazyPPath getPart(int part) {
		LazyPPath result = null;
		switch (part) {
		case LEFT_HEAD:
			result = leftHead;
			break;
		case RIGHT_HEAD:
			result = rightHead;
			break;
		case LEFT_TAIL:
			result = leftTail;
			break;
		case RIGHT_TAIL:
			result = rightTail;
			break;
		case LINE:
			result = line;
			break;
		default:
			assert false;
			break;
		}
		return result;
	}

	public void setStrokePaint(Paint color) {
		leftHead.setPaint(color);
		rightHead.setPaint(color);
		leftTail.setPaint(color);
		rightTail.setPaint(color);
		line.setStrokePaint(color);
		colorLabels(color);
	}

	public void addInputEventListener(PInputEventListener listener) {
		leftHead.addInputEventListener(listener);
		rightHead.addInputEventListener(listener);
		leftTail.addInputEventListener(listener);
		rightTail.addInputEventListener(listener);
		line.addInputEventListener(listener);
	}

	public void setSize(int part, int size) {
		switch (part) {
		case LEFT_HEAD:
		case RIGHT_HEAD:
			setHeadSize(part, size);
			break;
		case LEFT_TAIL:
		case RIGHT_TAIL:
			setTailSize(part, size);
			break;
		case LINE:
			line.setStroke(LazyPPath.getStrokeInstance(size));
			break;
		default:
			assert false;
			break;
		}
	}

	void setHeadSize(int part, int size) {
		// System.out.println("setHeadSize " + (part == RIGHT_HEAD) + " "
		// + isReversed());
		float halfSize = (size / 2) + 1;

		// float[] xp = { -halfSize, -halfSize, halfSize };
		float[] xp = { -size, -size, 0 };
		float[] yp = { -halfSize, halfSize, 0 };
		// float[] yp = { 0,size,halfSize };

		if ((part == RIGHT_HEAD) == isReversed()) {
			for (int j = 0; j < xp.length; j++) {
				xp[j] = -xp[j];
				yp[j] = -yp[j];
			}
		}
		getPart(part).setPathToPolyline(xp, yp);
	}

	void setTailSize(int part, double size) {
		LazyPPath tail = getPart(part);
		float halfSize = (float) (size / 2);
		float fSize = (float) size;
		tail.setPathToEllipse(-halfSize, -halfSize, fSize, fSize);
	}

	public void setLength(int length) {
		// assert length >= 0;
		float[] Xs = { 0, length };
		float[] Ys = { 0, 0 };
		line.setPathToPolyline(Xs, Ys);
		rightHead.setXoffset(length);
		rightTail.setX(length - rightTail.getWidth() / 2);
		placeLabels();
		double maxChildH = 0;
		for (int part = 1; part <= LINE; part++) {
			LazyPPath node = getPart(part);
			if (node.getVisible()) {
				maxChildH = Math.max(maxChildH, node.getHeight());
			}
		}
		setBounds(Math.min(0, length), -maxChildH / 2, Math.abs(length),
				maxChildH);
	}

	public void setLengthAndDirection(int length) {
		setLength(length);
		setVisible(LEFT_HEAD, length < 0);
		setVisible(RIGHT_HEAD, length > 0);
		leftHead.setXoffset(rightHead.getXOffset());
	}

	/**
	 * It's convenient to let the "left" end remain fixed at x==0, and to point
	 * left or right by changing this Arrow's bounds, instead of the application
	 * having to mess with the offset. Setting setLength<0 accomplishes this,
	 * and isReversed tests whether it is.
	 * 
	 */
	private boolean isReversed() {
		return rightHead.getX() + rightHead.getWidth() / 2 < 0;
	}

	public void setEndpoints(double leftX, double leftY, double rightX,
			double rightY) {
		setLength((int) Point2D.distance(leftX, leftY, rightX, rightY));
		double angle = Math.atan2(rightY - leftY, rightX - leftX);
		AffineTransform t = getTransformReference(true);
		t.setToTranslation(leftX, leftY);
		t.rotate(angle);
		// System.out.println("setEndpoints " + tailX + " " + tailY + " " +
		// headX
		// + " " + headY + " " + t);
		placeLabels();
	}

	private APText labels[] = new APText[3];

	public void addLabel(String string, Font font, int position) {
		if (string == null) {
			labels[position] = null;
		} else {
			if (labels[position] == null) {
				labels[position] = APText.oneLineLabel(font);
				labels[position].setTextPaint(line.getStrokePaint());
				addChild(labels[position]);
			}
			labels[position].setText(string);
		}
		placeLabels();
	}

	public void addLabels(String[] strings, Font font) {
		for (int position = 0; position < strings.length; position++) {
			addLabel(strings[position], font, position);
		}
	}

	private void placeLabels() {
		boolean upsideDown = Math.cos(getGlobalRotation()) < 0;
		for (int i = 0; i < labels.length; i++) {
			APText label = labels[i];
			if (label != null) {
//				System.out.println("placeLabels "+i+" "+label.getText());
				label.getTransformReference(true).setToIdentity();
				double x = 0;
				switch (i) {
				case Edge.LEFT_LABEL:
					x = 0;
					break;
				case Edge.CENTER_LABEL:
					x = Math.rint((line.getWidth() - label.getWidth()
							* label.getScale()) / 2.0);
					break;
				case Edge.RIGHT_LABEL:
					x = (line.getWidth() - label.getWidth() * label.getScale());
					break;
				default:
					assert false;
					break;
				}
				label.translate(x + (upsideDown ? label.getWidth() : 0), 0);
				label.setRotation(upsideDown ? Math.PI : 0);
				// label.setXoffset(x);
			}
		}
	}

	private void colorLabels(Paint color) {
		for (int i = 0; i < labels.length; i++) {
			APText label = labels[i];
			if (label != null) {
				label.setTextPaint(color);
			}
		}
	}
}
