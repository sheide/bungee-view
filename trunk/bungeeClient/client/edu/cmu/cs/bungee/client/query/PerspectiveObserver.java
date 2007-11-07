package edu.cmu.cs.bungee.client.query;

/**
 * @author mad
 * An object needs to know the name of a Perspective
 */
public interface PerspectiveObserver {
	
	/**
	 * Callback if the Perspective name wasn't available when this PerspectiveObserver asked originally, but now is.
	 */
	public void redraw();

}
