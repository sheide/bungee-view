package edu.cmu.cs.bungee.client.viz;

/**
 * If a FacetText has a pickFacetTextNotifier, it will get a chance to handle
 * pick, mouseMove, and highlight gestures before the normal FacetText handler.
 * (If it doesn't handle it, the normal handing occurs.)
 * 
 * @author mad
 * 
 */
interface PickFacetTextNotifier {

	/**
	 * @param node
	 *            the picked node
	 * @param modifiers
	 *            the shift keys
	 * @return whether the gesture was handled
	 */
	boolean pick(FacetText node, int modifiers);

	// Now we just do these with highlight
//	/**
//	 * @param node
//	 *            the node the mouse moved over
//	 * @param modifiers
//	 *            the shift keys
//	 * @return whether the gesture was handled
//	 */
//	boolean mouseMoved(FacetText node, int modifiers);
//
//	/**
//	 * @param node
//	 *            the node the mouse is over
//	 * @param modifiers
//	 *            the shift keys
//	 * @return whether the gesture was handled
//	 */
//	boolean shiftKeysChanged(FacetText node, int modifiers);

	/**
	 * @param node
	 *            the node entered/exited
	 * @param state
	 *            whether the mouse entered (as opposed to exited)
	 * @param modifiers
	 *            the shift keys
	 * @return whether the gesture was handled
	 */
	boolean highlight(FacetText node, boolean state, int modifiers);

}
