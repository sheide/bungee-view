package edu.cmu.cs.bungee.client.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * @author mad this is really just a generic tree structure, where every node
 *         has an associated object, the treeObject
 */
public class DisplayTree {

	private final DisplayTree parent;

	private final Object treeObject;

	private final List children = new Vector();

	private final String itemDesc;

	/**
	 * @param _parent
	 *            the parent of this subtree, or null for no parent
	 * @param _treeObject
	 *            _treeObject the object associated with this node
	 * @param _desc
	 *            a description of this subtree
	 */
	public DisplayTree(DisplayTree _parent, Object _treeObject, String _desc) {
		parent = _parent;
		treeObject = _treeObject;
		// label = _label;
		itemDesc = _desc;
		if (parent != null)
			parent.addChild(this);
	}

	/**
	 * @return the parent of this subtree
	 */
	public DisplayTree getParent() {
		return parent;
	}

	/**
	 * @return the object associated with this subtree
	 */
	public Object treeObject() {
		return treeObject;
	}

	/**
	 * @return the subtrees under this node
	 */
	public List getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * @return the number of child subtrees
	 */
	public int nChildren() {
		return children.size();
	}

	/**
	 * @return the description of this node, or null
	 */
	public String description() {
		return itemDesc;
	}

	/**
	 * @param treeObject1
	 *            the object to search for
	 * @return Is treeObject1 the treeObject() of this or one of its
	 *         descendents.
	 */
	public boolean isMember(Object treeObject1) {
		boolean result = treeObject() == treeObject1;
		for (Iterator it = children.iterator(); it.hasNext() && !result;) {
			DisplayTree child = (DisplayTree) it.next();
			result = child.isMember(treeObject1);
		}
		return result;
	}

	/**
	 * @param treeObjects
	 *            set of objects to search for
	 * @return Is one of treeObjects the treeObject() of this or one of its
	 *         descendents.
	 */
	public boolean isMember(Set treeObjects) {
		// we know treeObjects are Perspectives, and contains barfs on non-comparable Objects
		boolean result = treeObject() instanceof Perspective && treeObjects.contains(treeObject());
		for (Iterator it = children.iterator(); it.hasNext() && !result;) {
			DisplayTree child = (DisplayTree) it.next();
			result = child.isMember(treeObjects);
		}
		return result;
	}

	/**
	 * @param index
	 *            0-based index of child to delete
	 */
	public void removeChild(int index) {
		 children.remove(index);
//			Util.print("removeChild " + this);
	}

	/**
	 * @param treeObject1
	 *            child to delete
	 * @return was anything deleted?
	 */
	public boolean removeChild(Object treeObject1) {
//		Util.print("removeChild");
		return children.remove(treeObject1);
	}

	/**
	 * @param child
	 *            subtree to add (after all current children)
	 * @return was anything added?
	 */
	public boolean addChild(DisplayTree child) {
//		Util.print("addChild");
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

	/**
	 * @return a ListIterator over children
	 */
	public ListIterator childIterator() {
		return children.listIterator();
	}

}