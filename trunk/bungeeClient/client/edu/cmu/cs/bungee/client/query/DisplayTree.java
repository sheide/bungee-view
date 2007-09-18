package edu.cmu.cs.bungee.client.query;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import edu.cmu.cs.bungee.javaExtensions.Util;


public class DisplayTree {
	
	private final DisplayTree parent;
	
	private final Object treeObject;
	
	private final List children = new Vector();

	private final String itemDesc;

//	private final String label;
	
	public DisplayTree(DisplayTree _parent, Object _treeObject) {
		parent = _parent;
		treeObject = _treeObject;
//		label = null;
		itemDesc = null;
		parent.addChild(this);
	}
	
	public DisplayTree(DisplayTree _parent, Object _treeObject, String _desc) {
		parent = _parent;
		treeObject = _treeObject;
//		label = _label;
		itemDesc = _desc;
		if (parent != null)
		parent.addChild(this);
	}

	public DisplayTree getParent() {
		return parent;
	}

	public Object treeObject() {
		return treeObject;
	}

	public List getChildren() {
		return children;
	}

	public int nChildren() {
		return children.size();
	}
	
	public String description() {
		return itemDesc;
	}

	/* 
	 * Is treeObject1 the treeObject() of this or one of its descendents.
	 */
	public boolean isMember(Object treeObject1) {
		boolean result = treeObject() == treeObject1;
		for (Iterator it = children.iterator(); it.hasNext() && !result;) {
			DisplayTree child = (DisplayTree) it.next();
			result = child.isMember(treeObject1);
		}
		return result;
	}

	/* 
	 * Is one of treeObjects the treeObject() of this or one of its descendents.
	 */
	public boolean isMember(Object[] treeObjects) {
		boolean result = Util.isMember(treeObjects, treeObject());
		for (Iterator it = children.iterator(); it.hasNext() && !result;) {
			DisplayTree child = (DisplayTree) it.next();
			result = child.isMember(treeObjects);
		}
		return result;
	}
	
	public boolean removeChild(Object treeObject1) {
		return children.remove(treeObject1);
	}
	
	public boolean addChild(DisplayTree child) {
		return children.add(child);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		toStringInternal(buf, "\n");
		return buf.toString();
	}

	void toStringInternal(StringBuffer buf, String indent) {
		if (treeObject() != null)
			buf.append(indent).append(treeObject());
		else
			buf.append(indent).append("[Facet Tree Root]");
		for (Iterator it = children.iterator(); it.hasNext();) 
			((DisplayTree) it.next()).toStringInternal(buf, indent + " ");
	}

	public ListIterator childIterator() {
		return children.listIterator();
	}
	
}