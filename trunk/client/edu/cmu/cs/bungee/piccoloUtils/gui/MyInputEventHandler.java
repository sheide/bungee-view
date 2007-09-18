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

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

public class MyInputEventHandler extends PBasicInputEventHandler {

	Class nodeType;

	/**
	 * Functions should return true iff they handle the event.
	 */
	public MyInputEventHandler(Class _nodeType) {
		nodeType = _nodeType;
	}

	public boolean click(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean enter(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean exit(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean press(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean release(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean drag(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		return false;
	}

	public boolean keyPress(int key) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		return false;
	}

	public boolean keyRelease(int key) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		return false;
	}

	public boolean click(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean enter(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean exit(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean press(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean release(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean drag(PNode node, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean keyPress(int key, PInputEvent e) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(key);
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(e);
		return false;
	}

	public boolean keyRelease(int key, PInputEvent e) {
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

	public void mayHideTransients(PNode node) {
		// Override this
		assert edu.cmu.cs.bungee.javaExtensions.Util.ignore(node);
	}

	public void mouseClicked(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !nodeType.isInstance(node))
			node = node.getParent();
		if (node != null) {
			// System.out.println("PerspectiveSelectionHandler connecting to " +
			// ((Rank) node).getName());
			// Util.printDescendents(e.getPickedNode(), 5);
			mayHideTransients(node);
			if (click(node) || click(node, e))
				e.setHandled(true);
		} else {
			wrongType(e, "mouseClicked");
		}
	}

	public void mouseEntered(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !nodeType.isInstance(node))
			node = node.getParent();
		if (node != null) {
//			System.out.println("\nmouse entered");
//			 Util.printDescendents(e.getPickedNode());
			if (enter(node) || enter(node, e))
				e.setHandled(true);
		} else {
			wrongType(e, "mouseEntered");
		}
	}

	public void mouseExited(PInputEvent e) {
		// Mouse process should do this automatically
//		try {
			PNode node = e.getPickedNode();
//			System.out.println("MyInputEventHandler mouseExited "
//					+ e.getPickedNode());
			while (node != null && !nodeType.isInstance(node))
				node = node.getParent();
			if (node != null) {
				if (exit(node) || exit(node, e))
					e.setHandled(true);
			} else {
				wrongType(e, "mouseExited");
			}
//		} catch (Throwable ex) {
//			System.err.println("Ignoring exception in Fetcher: " + ex);
//			ex.printStackTrace();
//		}
	}

	public void mousePressed(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !nodeType.isInstance(node))
			node = node.getParent();
		if (node != null) {
			// System.out.println("PerspectiveSelectionHandler connecting to " +
			// ((Rank) node).getName());
			// Util.printDescendents(e.getPickedNode(), 5);
			mayHideTransients(node);
			if (press(node) || press(node, e))
				e.setHandled(true);
		} else {
			wrongType(e, "mousePressed");
		}
	}

	public void mouseReleased(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !nodeType.isInstance(node))
			node = node.getParent();
		if (node != null) {
			// System.out.println("PerspectiveSelectionHandler connecting to " +
			// ((Rank) node).getName());
			// Util.printDescendents(e.getPickedNode(), 5);
			if (release(node) || release(node, e))
				e.setHandled(true);
		} else {
			wrongType(e, "mouseReleased");
		}
	}

	public void mouseDragged(PInputEvent e) {
		PNode node = e.getPickedNode();
		while (node != null && !nodeType.isInstance(node))
			node = node.getParent();
		if (node != null) {
			// System.out.println("PerspectiveSelectionHandler connecting to " +
			// ((Rank) node).getName());
			// Util.printDescendents(e.getPickedNode(), 5);
			if (drag(node) || drag(node, e))
				e.setHandled(true);
		} else {
			wrongType(e, "mouseDragged");
		}
	}

	// Note that Windows catches the Alt key and the window loses focus!!!
	public void keyPressed(PInputEvent e) {
		mayHideTransients(null);
		if (keyPress(e.getKeyCode()) || keyPress(e.getKeyCode(), e))
			e.setHandled(true);
	}

	public void keyReleased(PInputEvent e) {
		mayHideTransients(null);
		if (keyRelease(e.getKeyCode()) || keyRelease(e.getKeyCode(), e))
			e.setHandled(true);
	}
}
