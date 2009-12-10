package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import edu.cmu.cs.bungee.javaExtensions.Labeled;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;

public class LabeledVScrollbar extends VScrollbar implements Labeled {

	// private final PriorityLabeler labeler;
	private final Labeled labeled;
	private Font font;
	private LazyPPath labels;
	private PriorityLabeler labeler;

	public LabeledVScrollbar(double width, double height, Color _bg, Color _fg,
			Runnable _action, Labeled labeled, Font font) {
		super(width, height, _bg, _fg, _action);
		this.labeled = labeled;
		this.font = font;
		labels = new LazyPPath();
		labels.setVisible(false);
		labels.setPickable(false);
		labels.setChildrenPickable(false);
		// labels.setStrokePaint(_fg);
		// labels.setStroke(LazyPPath.getStrokeInstance(2));
		// labels.setPaint(Color.yellow);
		addChild(labels);
		labels.moveToBack();
		updateCounts();
	}

	public void updateCounts() {
		labeled.updateCounts();
		labels.removeAllChildren();
		int range = (int) (sheight - thumb.getHeight());
		labeler = new PriorityLabeler(this, range, font.getSize());
		// System.out.println("LabeledVScrollbar h: " + (int) sheight + " "
		// + range + " " + labeler.visibleWidth());
	}

	public void resize() {
		super.resize();
		if (labels != null) {
			int range = (int) (sheight - thumb.getHeight());
			if (labeler == null || range != Math.ceil(labeler.visibleWidth())) {
				updateCounts();
			}
		}
	}

	public void drawLabel(Object o, int from, int to) {
		float y0 = from + 1;
		float height = to - y0 - 1;
		if (height > 1) {
			float width = (float) getWidth();
			double barOffset = width + thumb.getHeight() / 2;
			APText label = new APText(font);
			label.setPaint(getPaint());
			label.setTextPaint(thumb.getPaint());
			label.setText(o.toString());
			label.setOffset(width, barOffset + (from + to - label.getHeight())
					/ 2);
			labels.addChild(label);

			// labels.setWidth(getWidth());
			// labels.setHeight(getHeight());
			// float h = (float) getHeight();
			LazyPNode bar = new LazyPNode();
			bar.setPaint(((Color) thumb.getPaint()).darker());
			// bar.setStroke(LazyPPath.getStrokeInstance(2));
			bar.setBounds(0, y0 + barOffset, width, height);
			// bar.setBounds(getBounds());
			// bar.setPathToRectangle(0, y0 + width, width, y1 - y0);
			labels.addChild(bar);
			// labels.moveTo(0, y0);
			// labels.lineTo(100, y0);
			// labels.lineTo(100, y1);
			// labels.lineTo(0, y1);
			// labels.lineTo(0, y0);
			// setPaint(Color.yellow);
			// System.out.println("dl " + from + " " + height + " " +
			// getHeight()
			// + " " + o + " " + bar.getBounds() + " " + getBounds() + " "
			// + getGlobalScale());

			// for (Iterator it = getChildrenIterator(); it.hasNext();) {
			// PNode child= (PNode) it.next();
			// child.setVisible(false);
			// }
		}
	}

	void mouseDoc(PNode node, PInputEvent e, boolean state) {
		super.mouseDoc(node, e, state);
		labels.setVisible(state);
	}

	public Iterator getChildIterator() {
		return labeled.getChildIterator();
	}

	public Iterator getChildIterator(Object from, Object to) {
		return labeled.getChildIterator(from, to);
	}

	public Iterator cumCountChildIterator(int minCount, int maxCount) {
		return labeled.cumCountChildIterator(minCount, maxCount);
	}

	public int count(Object o) {
		return labeled.count(o);
	}

	public int priority(Object o) {
		return labeled.priority(o);
	}

	public int cumCountInclusive(Object o) {
		return labeled.cumCountInclusive(o);
	}

}
