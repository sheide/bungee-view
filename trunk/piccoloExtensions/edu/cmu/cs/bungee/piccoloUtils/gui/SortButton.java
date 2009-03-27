package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class SortButton extends TextButton {

	private static final String z_aLabel = "\u2193";

	private static final String a_zLabel = "\u2191";

	private static final String noneLabel = " ";

	private int sortField;

	private int defaultDirection;

	private SortButtons container;

	public SortButton(int field, int _defaultDirection, Font font,
			SortButtons _container, Color textColor, Color bgColor) {
		super(noneLabel, font, 0, 0, -1, -1, null, "Sort by this column", 2, textColor, bgColor);
		setJustification(Component.CENTER_ALIGNMENT);
		sortField = field;
		defaultDirection = _defaultDirection;
		container = _container;
	}

	private void setOrder(int field, int direction) {
		int myDirection = field == sortField ? direction : 0;
		String s = null;
		switch (myDirection) {
		case 1:
			s = a_zLabel;
			break;
		case 0:
			s = noneLabel;
			break;
		case -1:
			s = z_aLabel;
			break;
		default:
			assert false : direction;
		}
		setText(s);
	}

	private int getDirection() {
		int result = 0;
		String s = getText();
		if (s.equals(a_zLabel))
			result = 1;
		else if (s.equals(noneLabel))
			result = 0;
		else if (s.equals(z_aLabel))
			result = -1;
		else
			assert false : s;
		return result;
	}

	public void doPick() {
		int oldDirection = getDirection();
		int direction = oldDirection == 0 ? defaultDirection : -oldDirection;
		updateButtons(sortField, direction);
		container.setOrder(sortField, direction);
	}

	public void updateButtons(int field, int direction) {
		SortButton[] buttons = container.getSortButtons();
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].setOrder(field, direction);
		}
	}

}
