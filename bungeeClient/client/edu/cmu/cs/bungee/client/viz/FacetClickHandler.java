package edu.cmu.cs.bungee.client.viz;

import java.awt.geom.Point2D;

import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;

class FacetClickHandler extends MyInputEventHandler {

	PerspectiveViz dragging;

	FacetClickHandler() {
		super(FacetNode.class);
	}

	// protected // public void mouseMoved(PInputEvent e) {
	// //Util.print("FacetClickHandler.mouseEntered");
	// PNode node = e.getPickedNode();
	// if (node instanceof FacetPNode) {
	// // Pass e in so rotated FacetPTexts can verify that mouse
	// // is over them, to work around Piccolo rotated-select bug.
	// ((FacetPNode) node).mouseMoved(e);
	// //Util.print("Mouse entered a FacetNode: " + node);
	// e.setHandled(true);
	// } else {
	// //Util.print("Mouse entered a non-FacetNode: " + node);
	// super.mouseMoved(e);
	// }
	// }

	// Treat it like enter
	protected boolean shiftKeysChanged(PNode node, PInputEvent e) {
		// Util.print("FacetClickHandler.enter " + ((FacetNode) node).getFacet()
		// + " " + e.getModifiersEx());
		return ((FacetNode) node).highlight(true, e.getModifiersEx(), e);
	}

	protected boolean enter(PNode node, PInputEvent e) {
		// Util.print("FacetClickHandler.enter " + ((FacetNode) node).getFacet()
		// + " " + e.getModifiersEx());
		return ((FacetNode) node).highlight(true, e.getModifiersEx(), e);
	}

	protected boolean exit(PNode node, PInputEvent e) {
		// Util.print("FacetClickHandler.exit " + ((FacetNode)
		// node).getFacet());
		return ((FacetNode) node).highlight(false, e.getModifiersEx(), e);
	}

	protected boolean click(PNode node, PInputEvent e) {
		FacetNode f = (FacetNode) node;
		// Util.print("FacetClickHandler.click " + f);
		if (e.isRightMouseButton()
				&& f.art().setSelectedForEdit(f.getFacet(), e.getModifiersEx()))
			return true;
		if (e.isMiddleMouseButton() && f.art().facetMiddleMenu(f.getFacet()))
			return true;
		return f.pick(e);
	}

	protected void mayHideTransients(PNode node) {
		((FacetNode) node).mayHideTransients(node);
	}

	protected boolean press(PNode node, PInputEvent e) {
		boolean result = node instanceof Bar;
		if (result) {
			PNode parent = node.getParent();
			if (parent == null)
				edu.cmu.cs.bungee.piccoloUtils.gui.Util.printDescendents(node);
			assert parent != null;
			// gui.Util.printDescendents(node);
			PerspectiveViz pv = (PerspectiveViz) parent.getParent();
			result = pv.front == parent;
			if (result) {
				Point2D x = e.getPosition();
				dragging = pv;
				pv.startDrag(x, e.getPositionRelativeTo(parent));
			}
		}
		return result;
	}

	protected boolean release(PNode ignore) {
		boolean result = dragging != null;
		dragging = null;
		return result;
	}

	protected boolean drag(PNode ignore, PInputEvent e) {
		boolean result = dragging != null;
		if (result) {
			// gui.Util.printDescendents(node);
			Point2D x = e.getPosition();
			PDimension dim = e.getDelta();
			dragging.drag(x, dim);
		}
		return result;
	}
}