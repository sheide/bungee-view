/* 

 Created on Dec 9, 2005

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

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * Extends PBasicInputEventHandler by automatically looking up the pick path for
 * a node of the given type, and for each gesture calling a new version of the
 * function on that node.
 * 
 * @author mad
 * 
 */
public class MyInputEventHandler extends PBasicInputEventHandler {

	final Class nodeType;
	private int SHIFT_KEYS_CHANGED = MouseEvent.MOUSE_LAST + 1;

	/**
	 * Functions should return true iff they handle the event.
	 * 
	 * @param _nodeType
	 *            Search up the PNode hierarchy for a _nodeType
	 */
	public MyInputEventHandler(Class _nodeType) {
		nodeType = _nodeType;
	}

	/**
	 * Mask for up, down, left, right, home, end, and c-A
	 */
	public final static char[] arrowKeys = arrowKeys();

	private static char[] arrowKeys() {
		char[] arrows = { java.awt.event.KeyEvent.VK_KP_DOWN,
				java.awt.event.KeyEvent.VK_KP_UP,
				java.awt.event.KeyEvent.VK_KP_LEFT,
				java.awt.event.KeyEvent.VK_KP_RIGHT,
				java.awt.event.KeyEvent.VK_DOWN, java.awt.event.KeyEvent.VK_UP,
				java.awt.event.KeyEvent.VK_LEFT,
				java.awt.event.KeyEvent.VK_RIGHT, //java.awt.event.KeyEvent.VK_A
				// ,
				java.awt.event.KeyEvent.VK_END, java.awt.event.KeyEvent.VK_HOME };
		Arrays.sort(arrows);
		return arrows;
	}

	/**
	 * Mask for shift and control
	 */
	public final static char[] shiftKeys = shiftKeys();

	private static char[] shiftKeys() {
		char[] shifts = { java.awt.event.KeyEvent.VK_ALT,
				java.awt.event.KeyEvent.VK_CONTROL,
				java.awt.event.KeyEvent.VK_SHIFT };
		Arrays.sort(shifts);
		return shifts;
	}

	protected boolean click(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean enter(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean exit(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean press(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean release(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean drag(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean moved(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean shiftKeysChanged(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	protected boolean keyPress(char key) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		return false;
	}

	protected boolean keyRelease(int key) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		return false;
	}

	protected boolean click(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean enter(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean exit(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean press(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean release(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean drag(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean moved(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean shiftKeysChanged(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean keyPress(char key, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	protected boolean keyRelease(int key, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	private void wrongType(PInputEvent e, String eventType) {
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(eventType);
		// gui.Util.printDescendents(e.getPickedNode());
		//		
		// Exception ex = new Exception(
		// "MyInputEventHandler " + eventType + " a PNode that's not a "
		// + nodeType + ": " + e.getPickedNode());
		//		
		// // Don't need both of these
		// // Util.print(ex);
		// ex.printStackTrace();
		// // throw(ex);
	}

	protected void mayHideTransients(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
	}

	/*
	 * see mousePressed
	 */
	public final void mouseClicked(PInputEvent e) {
		// Toolkit.getDefaultToolkit().beep();
		PNode node = findNodeType(e);
		if (false && node != null) {
			mayHideTransients(node);
			e.setHandled(click(node) || click(node, e));
		}
	}

	public void mouseEntered(PInputEvent e) {
		PNode node = findNodeType(e);
		if (node != null)
			e.setHandled(enter(node) || enter(node, e));
	}

	public void mouseExited(PInputEvent e) {
		PNode node = findNodeType(e);
		if (node != null)
			e.setHandled(exit(node) || exit(node, e));
	}

	/*
	 * On Alex's computer, clicks are sometimes getting lost, but pressed
	 * doesn't. Therefore treat pressed as both. This means a click will act
	 * like 2 presses and a click, so don't do any actions on press, just set
	 * drag initial states.
	 * 
	 * Note that modifiers may be different for press and click, at least for
	 * mouse button modifiers
	 */
	public final void mousePressed(PInputEvent e) {
		// Util.print("mousePressed");
		PNode node = findNodeType(e);
		if (node != null/* &&e.getClickCount()==1 */) {
			mayHideTransients(node);
			e.setHandled(press(node) || press(node, e));
			e.setHandled(click(node) || click(node, e));
		}
	}

	public void mouseReleased(PInputEvent e) {
		PNode node = findNodeType(e);
		if (node != null)
			e.setHandled(release(node) || release(node, e));
	}

	public void mouseDragged(PInputEvent e) {
		PNode node = findNodeType(e);
		if (node != null)
			e.setHandled(drag(node) || drag(node, e));
	}

	public void mouseMoved(PInputEvent e) {
		PNode node = findNodeType(e);
		if (node != null)
			e.setHandled(moved(node) || moved(node, e));
	}

	private PNode findNodeType(PInputEvent e) {
		prevModifiers = e.getModifiersEx();
		PNode node = null;
		if (!e.isHandled()) {
			node = findNodeType(e.getPickedNode());
			if (node == null) {
				wrongType(e, e.getSourceSwingEvent().toString());
			}
		}
		return node;
	}

	private PNode findNodeType(PNode node) {
		while (node != null && !nodeType.isInstance(node)) {
			// Util.print(nodeType + " " + node);
			node = node.getParent();
		}
		// Util.print(nodeType + " => " + node);
		return node;
	}

	// Note that Windows catches the Alt key and the window loses focus!!!
	public void keyPressed(PInputEvent e) {
		mayHideTransients(null);
		char key = (char) e.getKeyCode();
		// Util.print("keyPressed " + e.getKeyCode() + " " + key);
		if (keyPress(key) || keyPress(key, e)) {
			e.setHandled(true);
		} else {
			maybeShiftKeysChanged(e);
		}
	}

	public void keyReleased(PInputEvent e) {
		// mayHideTransients(null);
		int key = e.getKeyCode();
		if (keyRelease(key) || keyRelease(key, e)) {
			e.setHandled(true);
		} else {
			maybeShiftKeysChanged(e);
		}
	}

	private int prevModifiers;

	/**
	 * Transform a shift- or control-key event to a MouseEvent on the current
	 * mouseOver. I.e. treat control keys like additional mouse buttons.
	 * 
	 * @param e
	 */
	private void maybeShiftKeysChanged(PInputEvent e) {
		if (Util.isMember(shiftKeys, (char) e.getKeyCode())) {
			PPickPath path = e.getInputManager().getMouseOver();
			PInputEvent ePrime = new PInputEvent(e.getInputManager(),
					new MouseEvent((Component) e.getSourceSwingEvent()
							.getSource(), SHIFT_KEYS_CHANGED, e.getWhen(), e
							.getModifiersEx(), -1, -1, 0, false,
							MouseEvent.NOBUTTON));
			try {
				path.processEvent(ePrime, SHIFT_KEYS_CHANGED);
			} catch (Throwable ignore) {
				// ePrime might be handled by a listener that isn't a
				// MyInputEventHandler,
				// in which case it will barf on SHIFT_KEYS_CHANGED
			}
			// Make sure our super doesn't get an unhandled event type it
			// doesn't recognize.
			e.setHandled(true);
		}
	}

	public void processEvent(PInputEvent e, int type) {
		if (type == SHIFT_KEYS_CHANGED) {
			// Util.print("SHIFT_KEYS_CHANGED");
			if (e.getModifiersEx() != prevModifiers) {
				PNode node = findNodeType(e);
				if (node != null)
					e.setHandled(shiftKeysChanged(node)
							|| shiftKeysChanged(node, e));
			}
		} else {
			super.processEvent(e, type);
		}
	}
}
