package edu.cmu.cs.bungee.javaExtensions.graph;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

public class Node {
	private String label;

	private int centerX;
	private int centerY;
	public Object object;

	public Node(String label) {
		this.label = label;
	}

	public int getCenterX() {
		return centerX;
	}

	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public void setCenterY(int centerY) {
		this.centerY = centerY;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public Rectangle2D getRectangle(Font font, FontRenderContext frc) {
		Rectangle2D rect = font.getStringBounds(label, frc);
		double w = rect.getWidth();
		double h = rect.getHeight();
		return new Rectangle2D.Double(centerX - w / 2, centerY - h / 2, w, h);
	}

	public String toString() {
		return "<Node " + label + ">";
	}

}
