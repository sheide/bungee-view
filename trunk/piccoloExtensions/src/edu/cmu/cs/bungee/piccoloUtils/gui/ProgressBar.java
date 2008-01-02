package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.PFrame;

public final class ProgressBar extends PFrame {

	final static int barW = 200;

	final static int barH = 10;

	final static int barOffset = 5;

	double minValue;

	double maxValue;

	int percent;

	PNode bar;

	InputStream is;
	
	APText status;

	public ProgressBar(InputStream stream, String name) {
		// gross hack to pass arguments to initialize
		super(name, false, null);
		is = stream;
		minValue = 0;
		try {
			maxValue = is.available();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PNode BGbar = new PNode();
		BGbar.setPaint(Color.lightGray);
		BGbar.setOffset(barOffset, barOffset);
		BGbar.setWidth(barW);
		BGbar.setHeight(barH);

		bar = new PNode();
		bar.setPaint(Color.blue);
		bar.setOffset(barOffset, barOffset);
		bar.setHeight(barH);
		
		status = new APText();
		status.setOffset(barOffset, barOffset * 2 + barH);

		PCanvas canvas = getCanvas();
		canvas.setPanEventHandler(null);
		canvas.setZoomEventHandler(null);
		PLayer layer = canvas.getLayer();
		layer.addChild(BGbar);
		layer.addChild(bar);
		layer.addChild(status);
		
		setBounds(getDefaultFrameBounds());
	}

	public void setProgress() {
		try {
			setProgress(maxValue - is.available());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setProgress(String s) {
		try {
			status.setText(s);
			setProgress(maxValue - is.available());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setProgress(double value) {
		int newPercent = (int) (100 * (value - minValue) / (maxValue - minValue));
//		if (newPercent != percent) {
			percent = newPercent;
			if (percent >= 100) {
//				setVisible(false);
				dispose();
			}
			else {
				bar.setWidth(percent * barW / 100);
				repaint();
//				getCanvas().paintImmediately();
				// System.out.println("progess " + bar.getWidth());
			}
//		}
	}

	public Rectangle getDefaultFrameBounds() {
		return new Rectangle(100, 100, 10 + barW + 2 * barOffset, 30 + 2 * barH + 3
				* barOffset);
	}
}
