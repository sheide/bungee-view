package edu.cmu.cs.bungee.client.query;

import java.util.Iterator;

import edu.cmu.cs.bungee.javaExtensions.DefaultLabeled;

public class OnCountLabeled extends DefaultLabeled<Perspective> {

	private final Perspective parent;

	OnCountLabeled() {
		assert false;
		parent = null;
	}

	public OnCountLabeled(Perspective parent) {
		super();
		assert parent != null;
		this.parent = parent;
		// Util.print("OnCountLabeled onCount: "+parent.getOnCount());
	}

	public int count(Perspective o) {
		if (o.query().isQueryValid())
			return o.getOnCount();
		return 1;
	}

	public void drawLabel(Perspective o, int from, int to) {
		assert false;
	}

	public Iterator<Perspective> getChildIterator() {
		return parent.getChildIterator();
	}

	@Override
	public Iterator<Perspective> getChildIterator(Perspective from, Perspective to) {
		return parent.getChildIterator(from, to);
	}

}
