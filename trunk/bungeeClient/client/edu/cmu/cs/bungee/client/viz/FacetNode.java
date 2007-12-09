/* 

Created on Mar 6, 2005 
 
Bungee View lets you search, browse, and data-mine an image collection.  
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

package edu.cmu.cs.bungee.client.viz;

import java.awt.geom.Point2D;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PDimension;

interface FacetNode {
	// Constructors should
	// addInputEventListener(Art.facetClickHandler);
	
    /**
     * @param e the PInputEvent
     * @return whether the event was handled
     */
    boolean pick(PInputEvent e);
    
    /**
     * @param state did mouse enter (as opposed to exit)?
     * @param modifiers shift keys
     * @param e the PInputEvent
     * @return whether the event was handled
     */
    boolean highlight(boolean state, int modifiers, PInputEvent e);
    
	/**
	 * The user did something, indicating that temporary messages or whatever should be removed.
	 * 
	 * @param node the PNode gestured on
	 */
	void mayHideTransients(PNode node);	
	
	/**
	 * @return the facet associated with this FacetNode
	 */
	Perspective getFacet();	
	
	/**
	 * @return the Bungee PFrame we're in
	 */
	Bungee art();

	/**
	 * @param node
	 * @param positionRelativeTo
	 * @return the node to drag
	 */
	FacetNode startDrag(PNode node, Point2D positionRelativeTo);

	/**
	 * Used to drag Letters, which zooms and pans the PerspectiveViz
	 * @param position
	 * @param delta
	 */
	void drag(Point2D position, PDimension delta);
}
