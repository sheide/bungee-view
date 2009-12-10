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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Query.Item;

/**
 * @author mad
 * top-level node can be an Item, Cluster, String, or null
 * If the tree object is null, FacetTreeViz won't display the node (used for cluster tags to ignore)
 * descendents are the associated facets, arranged in a hierarchy by their parent relationships
 */
public final class FacetTree extends DisplayTree  {
	
/**
 * @param _parent this subtree's parent tree
 * @param leafs facets to create child subtrees for
 * @param desc description of this subtree
 */
	// called by ClusterViz
	public FacetTree(DisplayTree _parent, Set<Perspective> leafs, String desc) {
		super(_parent, desc, null);
		// Util.print("FacetTree " + p);
		createDescendentTree(Perspective.ancestors(leafs));
	}

	private void createDescendentTree(Set<Perspective> facets) {
		Object parentFacet = (treeObject() instanceof Perspective) ? treeObject() : null;
		for (Iterator<Perspective> it = facets.iterator(); it.hasNext();) {
			Perspective p = it.next();
			if (p.getParent() == parentFacet) {
				FacetTree child = new FacetTree(this, p);
				child.createDescendentTree(facets);
			}
		}
	}
	
	// called by createDescendentTree
	FacetTree(DisplayTree _parent, Perspective facet) {
		super(_parent, facet, null);
		assert facet != null;
	}

	/**
	 * @param parent this subtree's parent tree
	 * @param rs parent_facet_id, facet_id, name, n_child_facets, first_child_offset, n_items, isAncestor
	 * @param q the Query of this tree's objects
	 * @throws SQLException 
	 */
	// called by ClusterViz.createTree
	public FacetTree(DisplayTree parent, ResultSet rs, Query q) throws SQLException {
		super(parent, new Cluster(rs, q), null);
		// Util.print("FacetTree " + p);
//		Query q = art.query;
		rs.beforeFirst();
		facetTreeInternal(q, rs);
//		facet = new Cluster(rs, q);
	}

	// called by SelectedItem.itemSetter
	/**
	 * @param item create a FacetTree for item, containing its facets
	 * @param description concatenated values for all the description fields for this item
	 * @param q the Query the item is in
	 */
	public FacetTree(Item item, String description, Query q) {
		super(null, item, description);
		assert item != null;
		ResultSet rs = null;
		try {
			rs = q.getItemInfo(item);
			// returns [parent_facet_id, facet_id, name, n_child_facets,
			// first_child_offset, n_items]
			facetTreeInternal(q, rs);
		} catch (Throwable e) {
			e.printStackTrace();
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
//		 Util.print("FacetTree for " + _p + " " + _p.nChildren()+ " " + _p.children_offset());
		try {
			rs.beforeFirst();
			while (rs.next())
				if (rs.getInt(1) == _p.getID()) {
					int row = rs.getRow();
//					Util.print(rs.getInt(2) + " " + _p + " " + rs
//							.getString(3) + " " + rs.getInt(5) + " " + rs.getInt(4));
					Perspective child = q.ensurePerspective(rs.getInt(2), _p, rs
							.getString(3), rs.getInt(5), 
							rs.getInt(4));
					if (!q.isRestrictedData())
						child.setTotalCount ( rs.getInt(6));
					new FacetTree(this, child, rs, q);
					rs.absolute(row);
				}
		} catch (java.sql.SQLException se) {
			se.printStackTrace();
		}
	}

}
