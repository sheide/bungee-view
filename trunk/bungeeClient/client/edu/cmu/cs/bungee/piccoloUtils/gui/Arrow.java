package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Paint;

import edu.umd.cs.piccolo.nodes.PPath;

public class Arrow extends LazyPNode {
	protected PPath line;
	protected PPath tail;
	protected PPath leftHead;
	protected PPath rightHead;
//	int halfMarkSize;

	public Arrow(Paint headColor, Paint tailColor, int tailDiameter, int length) {
		int halfMarkSize = tailDiameter / 2;
		setHeight(tailDiameter);
	tail = PPath.createEllipse(-halfMarkSize,
			-halfMarkSize, tailDiameter, tailDiameter);
	setTailColor(tailColor);
	addChild(tail);

	halfMarkSize++;
	rightHead = new LazyPPath();
	float[] xp = new float[3];
	float[] yp = new float[3];
	xp[0] = -halfMarkSize;
	xp[1] = -halfMarkSize;
	xp[2] = halfMarkSize;
	yp[0] = -halfMarkSize;
	yp[1] = halfMarkSize;
	yp[2] = 0;
	rightHead.setPathToPolyline(xp, yp);
	rightHead.setStroke(LazyPPath.getStrokeInstance(1));
	rightHead.setStrokePaint(Color.black);	
	addChild(rightHead);

	leftHead = new LazyPPath();
	for (int j = 0; j < xp.length; j++) {
		xp[j] = -xp[j];
		yp[j] = -yp[j];
	}
	leftHead.setPathToPolyline(xp, yp);
	leftHead.setStroke(LazyPPath.getStrokeInstance(1));
	addChild(leftHead);

	line = LazyPPath.createLine(0, 0, length, 0, LazyPPath.getStrokeInstance(1));
	addChild(line);
	
	setLength(length);
	setHeadColor(headColor);
	}
	
	public void setLength(int length) {
		float[] Xs = { 0, length};
		float[] Ys = { 0, 0 };
		line.setPathToPolyline(Xs, Ys);
		double h = leftHead.getHeight();
		if (length < 0) {
//			tail.setOffset(length - halfMarkSize, -halfMarkSize);
			leftHead.setOffset(length, 0);
//			Xs[0] = -length;
//			Xs[1] = 0;
			setBounds(length-h/2, -h/2, -length+h, h);
		} else	if (length > 0) {
//			tail.setOffset(- halfMarkSize, -halfMarkSize);
			rightHead.setOffset(length, 0);
			setBounds(-h/2, -h/2, length+h, h);
		}
		
		rightHead.setVisible(length > 0);
		leftHead.setVisible(length < 0);
		rightHead.setPickable(length > 0);
		leftHead.setPickable(length < 0);
		line.setVisible(length != 0);
//		Util.printDescendents(line);
	}

	public void setHeadColor(Paint color) {
		rightHead.setPaint(color);
		leftHead.setPaint(color);
		line.setStrokePaint(color);		
	}

	public void setTailColor(Paint color) {
		tail.setPaint(color);		
	}

}
