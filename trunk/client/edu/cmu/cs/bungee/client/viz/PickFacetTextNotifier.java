package edu.cmu.cs.bungee.client.viz;

interface PickFacetTextNotifier {
	
	boolean pick(FacetText node, int modifiers);
	
	boolean mouseMoved(FacetText node, int modifiers);

	boolean highlight(FacetText node, boolean state, int modifiers);	

}
