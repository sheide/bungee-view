package log;

import java.awt.Color;
import java.awt.Component;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;

class Yaxis extends LazyPNode {
	
	SortedSet<String> values = new TreeSet<String>();

	double tickLength = 6;

	Yaxis(Set<String> _values) {
		values.addAll(_values);
	}

	void draw(double width, double length) {
		setBounds(0, 0, 1, length);
		int i = 0;
		int nTicks = values.size();
		for (Iterator<String> it = values.iterator(); it.hasNext();) {
			String name = it.next();
			double y = length * (1 - i++ /(double) (nTicks-1));
			
			LazyPNode tick = new LazyPNode();
			tick.setBounds(-tickLength / 2, y, tickLength, 1);
			tick.setPaint(Color.black);
			addChild(tick);

			APText label = getLabel(name, width - tickLength);
			label.setOffset(-tickLength, y);
			addChild(label);
		}
	}

	APText getLabel(String s, double w) {
		APText label = new APText();
		label.setJustification(Component.RIGHT_ALIGNMENT);
		label.setConstrainWidthToTextWidth(false);
		label.setWidth(w);
		label.setText(s);
		label.setX(-w);
		label.setY(-label.getHeight()/2);
		return label;
	}

	double encode(String value) {
		int index = values.headSet(value).size();
		int nTicks = values.size();
		double y = getHeight() * (1 - index /(double) (nTicks-1));
		return Math.round(y);
	}
}
