/* 

 Created on Mar 5, 2005 

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

package edu.cmu.cs.bungee.client.query;

import java.io.Serializable;
import java.sql.ResultSet;

import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.*;



public class FacetTree extends DisplayTree implements Serializable {

	private static final long serialVersionUID = -6993118734677325114L;
	
	/* (non-Javadoc)
	 * @see query.DisplayTree#isTopLevel()
	 */
	public boolean isTopLevel() {
		return !(treeObject() instanceof Perspective) || ((Perspective) treeObject()).getParent() == null;
	}

	// called by ClusterViz
	public FacetTree(DisplayTree _parent, Perspective[] leafs, String desc) {
		super(_parent, null, desc);
		// Util.print("FacetTree " + p);
		for (int i = 0; i < leafs.length; i++) {
			Perspective parentPerspective = leafs[i].getParent();
			if (parentPerspective != null && !Util.isMember(leafs, parentPerspective))
				leafs = (Perspective[]) Util.endPush(leafs, parentPerspective,
						Perspective.class);
		}
		createDescendentTree(leafs);
	}

	private void createDescendentTree(Perspective[] leafs) {
		Perspective parentFacet = (Perspective) treeObject();
		for (int i = 0; i < leafs.length; i++) {
			if (leafs[i].getParent() == parentFacet) {
				FacetTree child = new FacetTree(this, leafs[i]);
				child.createDescendentTree(leafs);
			}
		}
	}
	
	// called by createDescendentTree
	FacetTree(DisplayTree _parent, Perspective facet) {
		super(_parent, facet, null);
	}

	// called by ClusterViz.createTree
	public FacetTree(DisplayTree parent, ResultSet rs, Query q) {
		super(parent, new Cluster(rs, q), null);
		// Util.print("FacetTree " + p);
//		Query q = art.query;
		facetTreeInternal(q, rs);
//		facet = new Cluster(rs, q);
	}

	// called by SelectedItem.itemSetter
	public FacetTree(Item item, String description, Query q) {
		super(null, item, description);
		ResultSet rs = null;
		try {
			rs = q.getItemInfo(item);
			// returns [parent_facet_id, facet_id, name, n_child_facets,
			// first_child_offset, n_items]
			facetTreeInternal(q, rs);
		} finally {
			q.close(rs);
		}
	}

	private void facetTreeInternal(Query q, ResultSet rs) {
		try {
			int lastFacetType = -1;
			while (rs.next()) {
				// Util.print(rs.getInt(1) + " " + rs.getInt(2) + " " +
				// rs.getString(3));
				int parent_facet = rs.getInt(1);
				if (q.isTopLevel(parent_facet) && parent_facet != lastFacetType) {
					lastFacetType = parent_facet;
					Perspective parentPerspective = q
							.findPerspective(parent_facet);
					assert parentPerspective.isInstantiated();
					// println(facet_type + " " + rs.getString("name") + " " +
					// p);
					int row = rs.getRow();
					new FacetTree(this, parentPerspective, rs, q);
					rs.absolute(row);
				}
			}
			// q.close(rs);
		} catch (java.sql.SQLException se) {
			se.printStackTrace();
		}
	}

	// called by facetTreeInternal (and recursively)
	private FacetTree(DisplayTree _parent, Perspective _p, ResultSet rs, Query q) {
		super(_parent, _p, null);
		assert _p != null;
		assert _parent != null;
		assert rs != null;
		if (_p.nChildren() > 0) {
			_p.ensureInstantiatedPerspective();
		}
//		 Util.print("FacetTree for " + p + " " + p.nChildren()+ " " + p.children_offset());
		try {
			rs.beforeFirst();
			while (rs.next())
				if (rs.getInt(1) == _p.getID()) {
					int row = rs.getRow();
//					Util.print(rs.getInt(2) + " " + p + " " + rs
//							.getString(3) + " " + rs.getInt(5) + " " + rs.getInt(4));
					Perspective child = q.ensurePerspective(rs.getInt(2), _p, rs
							.getString(3), rs.getInt(5), rs.getInt(4));
					if (!q.isRestrictedData())
						child.totalCount = rs.getInt(6);
					new FacetTree(this, child, rs, q);
					rs.absolute(row);
				}
		} catch (java.sql.SQLException se) {
			se.printStackTrace();
		}
	}

}
