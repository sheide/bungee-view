/* 

 Created on Mar 4, 2005

 The Bungee View applet lets you search, browse, and data-mine an image collection.  
 Copyright (C) 2006  Mark Derthick

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.  See gpl.html.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 You may also contact the author at 
 mad@cs.cmu.edu, 
 or at
 Mark Derthick
 Carnegie-Mellon University
 Human-Computer Interaction Institute
 Pittsburgh, PA 15213

 */

package edu.cmu.cs.bungee.piccoloUtils.gui;

import java.awt.Color;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;

public class VScrollbar extends PNode implements MouseDoc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * width of bar
	 */
	protected double swidth;

	/**
	 * height of bar, not including up/down buttons
	 */
	protected double sheight;

	protected double thumbSize;

	protected double xpos; // x and y position of [top center of the] bar

	/**
	 * y position of slider, in the range [sposMin, sposMin + sheight -
	 * thumbsize]
	 */
	public double spos;

	/**
	 * min y position of slider. Equals swidth.
	 */
	protected double sposMin;

	protected double ratio;

	protected PNode thumb;

	protected Runnable action;

	protected static final VScrollHandler vscrollHandler = new VScrollHandler();

	protected double spos0; // origin for dragging

	/**
	 * percentage to scroll when up or down button is pressed
	 */
	private double delta = 0.1;

	ScrollButton up;

	ScrollButton down;

	public VScrollbar(double width, double height, Color _BG, Color _FG,
			Runnable _action) {
		init(0, width, height, _BG, _FG, _action);
	}

	public VScrollbar(double xPosition, double width, double height, Color _BG,
			Color _FG, Runnable _action) {
		init(xPosition, width, height, _BG, _FG, _action);
	}

	void init(double xPosition, double width, double height, Color _BG,
			Color _FG, Runnable _action) {
		assert height > 3 * width;
		action = _action;
		swidth = width;
		sheight = height - 2 * width;
		thumbSize = swidth; // default to square thumb
		xpos = xPosition; // - swidth / 2;
		sposMin = width;
		spos = sposMin;
		setPaint(_BG);
		thumb = new PNode();
		thumb.setPaint(_FG);
		thumb.setWidth(width);
		addChild(thumb);

		up = new ScrollButton(xpos, -1, width, -1, _FG, _BG);
		addChild(up);
		down = new ScrollButton(xpos, sheight + swidth + 1, width, 1, _FG, _BG);
		addChild(down);

		resize();
		addInputEventListener(vscrollHandler);
	}

	public void resize() {
		down.setOffset(xpos, sheight + swidth + 1);
		double sposMax = sposMin + sheight - visibleThumbSize();
		ratio = 1.0 / (sposMax - sposMin);
		spos = Util.constrain(spos, sposMin, sposMax);
		setBounds(xpos, sposMin, swidth, sheight);
		thumb.setBounds(xpos, spos, swidth, visibleThumbSize());
		// System.out.println(getBounds());
	}

	public void reset() {
		spos = sposMin;
	}

	public void setVisible(boolean isVisible) {
		super.setVisible(isVisible);
		setPickable(isVisible);
	}

	public void setH(double sh) {
		double newH = Math.max(swidth + 1, sh - 2 * swidth);
		thumbSize *= newH / sheight;
		sheight = newH;
		resize();
	}

	// public void setYpos(double yp) {
	// ypos = yp;
	// resize();
	// }

	public void setBufferPercent(double nVisible, double nTotal) {
		double percent = nVisible / nTotal;
		// System.out.println("setBufferPercent " + percent + " " + sheight);
		if (percent >= 1.0) {
			setVisible(false);
			reset();
		} else {
			thumbSize = percent * sheight;
			resize();
			delta = 1.0 / (nTotal - nVisible);
			setVisible(true);
		}
	}

	// public void setDelta(double percentToIncrement) {
	// Util.print("setDelta " + percentToIncrement);
	// delta = percentToIncrement;
	// }

	void buttonPressed(int direction) {
		// Util.print("buttonPressed " + delta + " " + getPos());
		setPos(getPos() + delta * direction);
	}

	/**
	 * @return value between 0 and 1 representing thumb position
	 */
	public double getPos() {
		// System.out.println("VScrollbar.getPos " + spos + " " + sposMin + " "
		// + ((spos - sposMin) * ratio));
		return (spos - sposMin) * ratio;
	}

	public void setPos(double pos) {
		pos = Util.constrain(pos, 0.0, 1.0);
		spos = pos / ratio + sposMin;
		thumb.setBounds(xpos, spos, swidth, visibleThumbSize());
		assert Math.abs(pos - getPos()) < 0.00001 : pos + " " + spos + " "
				+ sposMin + " " + ratio;
		action.run();
	}

	public void drag(PInputEvent e) {
		double dy = e.getDelta().getHeight();
		spos0 += dy;
		// System.out.println(spos0 * ratio);
		setPos(spos0 * ratio);
	}

	public void startDrag() {
		spos0 = spos - sposMin;
	}

	// public void endDrag() {
	// }

	public double visibleThumbSize() {
		return Math.max(swidth, thumbSize);
	}

	protected int getPageDirection(PInputEvent e) {
		double y = e.getPositionRelativeTo(this).getY();
		int direction = 0;
		if (y <= spos)
			direction = -1;
		else if (y >= spos + visibleThumbSize())
			direction = 1;
		// If mouse enters from outside of the window, y will be the value where
		// it last exited!
		// assert direction != 0 : y + " " + spos + " " + thumbSize;
		return direction;
	}

	public void page(PInputEvent e) {
		int direction = getPageDirection(e);
		setPos(getPos() + (thumbSize / (sheight - visibleThumbSize()))
				* direction);
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// setMouseDoc(state ? ((Button) source).mouseDoc : null);
	// }

	public void setMouseDoc(String doc) {
		if (getParent() instanceof MouseDoc) {
			((MouseDoc) getParent()).setMouseDoc(doc);
		}
	}

	// public void setMouseDoc(Vector doc, boolean state) {
	// if (getParent() instanceof MouseDoc) {
	// ((MouseDoc) getParent()).setMouseDoc(doc, state);
	// }
	// }

	void mouseDoc(PNode node, PInputEvent e, boolean state) {
		assert node == this;
		String desc = null;
		if (state) {
			PNode pickedNode = e.getPickedNode();
			if (pickedNode == node) {
				desc = getPageDirection(e) > 0 ? "Page down" : "Page up";
			} else {
				desc = "Start dragging scrollbar";
			}
		}
		setMouseDoc(desc);
	}
}

class VScrollHandler extends MyInputEventHandler {

	VScrollHandler() {
		super(VScrollbar.class);
	}

	// void checkPickedNode(VScrollbar node, PNode pickedNode) {
	// assert pickedNode == node.thumb || pickedNode == node.up
	// || pickedNode == node.down;
	// }

	public boolean click(PNode node, PInputEvent e) {
		boolean result = true;
		PNode pickedNode = e.getPickedNode();
		if (pickedNode == node) {
			((VScrollbar) node).page(e);
		} else if (pickedNode == ((VScrollbar) node).thumb) {
			((VScrollbar) node).startDrag();
		} else
			result = false;
		return result;
	}

	public boolean drag(PNode node, PInputEvent e) {
		PNode pickedNode = e.getPickedNode();
		if (pickedNode == ((VScrollbar) node).thumb) {
			((VScrollbar) node).drag(e);
			return true;
		}
		return false;
	}

	public boolean release(PNode node, PInputEvent e) {
		PNode pickedNode = e.getPickedNode();
		if (pickedNode == ((VScrollbar) node).thumb) {
			// ((VScrollbar) node).endDrag();
			return true;
		}
		return false;
	}

	public boolean enter(PNode node, PInputEvent e) {
		// Util.print("VScrollbar enter ");
		((VScrollbar) node).mouseDoc(node, e, true);
		return true;
	}

	public boolean exit(PNode node, PInputEvent e) {
		((VScrollbar) node).mouseDoc(node, e, false);
		return true;
	}

	// public void mouseEntered(PInputEvent e) {
	// //highlight(e, true);
	// }
	//
	// public void mouseExited(PInputEvent e) {
	// //highlight(e, false);
	// }

	// protected void highlight(PInputEvent e, boolean state) {
	// //System.out.println("VScrollHandler.highlight " + state);
	// PNode node = e.getPickedNode();
	// if (node instanceof Menu) {
	// //((Menu) node).highlight(state);
	// //System.out.println("Mouse entered a MenuNode: " + node);
	// e.setHandled(true);
	// } else if (node instanceof MenuButton) {
	// //((Button) node).highlight(state);
	// //System.out.println("Mouse entered a MenuNode: " + node);
	// e.setHandled(true);
	// } else {
	// //System.out.println("Mouse entered a non-MenuNode: " + node);
	// super.mousePressed(e);
	// }
	// }

}

// class VScrollHandler extends PBasicInputEventHandler {
//
// //VScrollbar current = null;
//
// public void mousePressed(PInputEvent e) {
// //System.out.println("VScrollHandler.mousePressed " +
// // e.getPickedNode().getClass());
// PNode node = e.getPickedNode();
// if (node instanceof VScrollbar) {
// VScrollbar current = ((VScrollbar) node);
// current.page(e);
// //System.out.println("Pressed on a VScrollbar");
// e.setHandled(true);
// } else if (node.getParent() instanceof VScrollbar) {
// VScrollbar current = ((VScrollbar) node.getParent());
// current.startDrag(e);
// //System.out.println("Pressed on a VScrollbar");
// e.setHandled(true);
// } else {
// System.out.println("Pressed on a " + node.getClass());
// super.mousePressed(e);
// }
// }
//
// public void mouseDragged(PInputEvent e) {
// PNode node = e.getPickedNode().getParent();
// if (node instanceof VScrollbar) {
// VScrollbar current = ((VScrollbar) node);
// //if (current != null) {
// //System.out.println("VScrollHandler.mouseDragged " +
// // e.getPickedNode().getClass());
// current.drag(e);
// //System.out.println("Clicked on a Menu: " + node);
// e.setHandled(true);
// }
// }
//
// public void mouseReleased(PInputEvent e) {
// PNode node = e.getPickedNode().getParent();
// if (node instanceof VScrollbar) {
// VScrollbar current = ((VScrollbar) node);
// //if (current != null) {
// //System.out.println("VScrollHandler.mouseDragged " +
// // e.getPickedNode().getClass());
// current.endDrag(e);
// //System.out.println("Clicked on a Menu: " + node);
// e.setHandled(true);
// }
// }
//
// public void mouseEntered(PInputEvent e) {
// //highlight(e, true);
// }
//
// public void mouseExited(PInputEvent e) {
// //highlight(e, false);
// }
//
// // protected void highlight(PInputEvent e, boolean state) {
// // //System.out.println("VScrollHandler.highlight " + state);
// // PNode node = e.getPickedNode();
// // if (node instanceof Menu) {
// // //((Menu) node).highlight(state);
// // //System.out.println("Mouse entered a MenuNode: " + node);
// // e.setHandled(true);
// // } else if (node instanceof MenuButton) {
// // //((Button) node).highlight(state);
// // //System.out.println("Mouse entered a MenuNode: " + node);
// // e.setHandled(true);
// // } else {
// // //System.out.println("Mouse entered a non-MenuNode: " + node);
// // super.mousePressed(e);
// // }
// // }
//
// }

class ScrollButton extends Button {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int _direction; // +1 or -1

	ScrollButton(double x, double y, double size, int direction, Color _FG,
			Color _BG) {
		super(x, y, size, size, null, direction > 0 ? "Scroll down one line"
				: "Scroll up one line", 2.0f, _BG);
		_direction = direction;
		// Util.print("ScrollButton " + y);

		// PNode bg = new PNode();
		// bg.setPaint(_BG);
		// bg.setBounds(1, 1, size - 2, size - 2);
		// bg.setPickable(false);
		// addChild(bg);

		// setPaint(_BG);
		float innerSize = ((float) size - 2 * borderW());
		float mid = innerSize / 2; // ((float) size - 2) / 2;
		float halfMarkSize = mid - 1; // ((float) size - 2) / 2 - 2;
		child = new LazyPPath();
		positionChild();
		float[] xp = new float[3];
		float[] yp = new float[3];
		xp[0] = mid - halfMarkSize;
		xp[1] = mid + halfMarkSize;
		xp[2] = mid;
		yp[0] = mid - direction * halfMarkSize;
		yp[1] = mid - direction * halfMarkSize;
		yp[2] = mid + direction * halfMarkSize;
		((LazyPPath) child).setPathToPolyline(xp, yp);
		child.setPaint(_FG);
	}

	public void doPick() {
		// Util.print("ScrollButton.dopick");
		((VScrollbar) getParent()).buttonPressed(_direction);
	}

	// make sure drag doesn't propagate up to thumb

	// public boolean press(PNode node, PInputEvent e) {
	// return true;
	// }
	//
	// public boolean drag(PNode node, PInputEvent e) {
	// return true;
	// }
	//
	// public boolean release(PNode node, PInputEvent e) {
	// return true;
	// }
}