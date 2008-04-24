package edu.cmu.cs.bungee.client.query;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author mad A set of facets treated conjunctively as a predicate on items (as
 *         are facets themselves) Created by typing 'c' in Bungee View.
 */
public final class Cluster implements ItemPredicate {

	private final SortedSet facets;

	private final String query;

	// Perspective seed;

	private int nOnItems;

	private final int nItems;

	private final double pValue;

//	/**
//	 * @return the number of facets in the cluster
//	 */
//	public int size() {
//		return facets.size();
//	}

	/**
	 * @return the minimum number of this Cluster's facets an item must have to satisfy this ItemPredicate
	 */
	public int quorumSize() {
		return nRestrictions() / 2 + 1;
	}

	/**
	 * @param rs
	 *            columns: parent_facet_id, facet_id, name, n_child_facets,
	 *            first_child_offset, n_items, isAncestor
	 * @param q
	 *            the query whose facets comprise this cluster
	 */
	public Cluster(final ResultSet rs, final Query q) {
		SortedSet _facets = new TreeSet();
		query = q.bookmark();
		int _nOnItems = -1;
		int _nItems = -1;
		double _pValue = -1;
		try {
			// rs.last();
			// Util.print(rs.getRow());
			// _facets = new Perspective[rs.getRow()];
			while (rs.next()) {
				// Util.print("cluster " + rs.getInt(2) + " " + rs
				// .getString(3) + " " + rs.getInt(1));
				Perspective parent = rs.getInt(1) > 0 ? q.findPerspective(rs
						.getInt(1)) : null;
				Perspective p = q.ensurePerspective(rs.getInt(2), parent, rs
						.getString(3), rs.getInt(5), 
						rs.getInt(4));
				if (rs.getInt(7) == 0) {
					_facets.add(p);
					_nOnItems = rs.getInt(9);
					_nItems = rs.getInt(10);
					_pValue = rs.getDouble(8);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			q.close(rs);
		}
		nOnItems = _nOnItems;
		nItems = _nItems;
		pValue = _pValue;
		facets = Collections.unmodifiableSortedSet(_facets);
		assert nOnItems > 0;
		assert nItems >= nOnItems;
		assert pValue >= 0 && pValue <= 1;
		// facets = (Perspective[]) Util.delete(_facets, (ItemPredicate) null,
		// Perspective.class);
	}

	public double pValue() {
		// If cluster is created by replayOps, pValue = -1;
//		assert 0 <= pValue && pValue <= 1;
		return pValue;
	}

	/**
	 * @param _facets
	 *            this cluster's facets
	 */
	public Cluster(Set _facets) {
		nOnItems = -1;
		nItems = -1;
		pValue = -1;
		facets = new TreeSet(_facets);
		query = query().bookmark();
	}

	/**
	 * @param buf
	 *            a StringBuffer to write to, or null to create a new one
	 * @param redraw
	 *            a callback object in case some of the facet names haven't been
	 *            read from the database yet
	 * @return buf, with a comma-delimited list of this cluster's facets
	 *         appended.
	 */
	public StringBuffer facetNames(StringBuffer buf, PerspectiveObserver redraw) {
		if (buf == null)
			buf = new StringBuffer();
		boolean first = true;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (first)
				first = false;
			else
				buf.append(", ");
			buf.append(p.getName(redraw));
			// desc.add(facet.facets[i]);
		}
		return buf;
	}

	public String toString() {
		// return super.toString();
		return getName(null);
	}

	public String getName(PerspectiveObserver redraw) {
		// return "Cluster of " + size() + " tags, having " + nOnItems + "/"
		// + nItems
		// + " items, with p = " + chiSq;
		StringBuffer buf = new StringBuffer();
		buf.append("Cluster of ").append(nRestrictions()).append(" tags: ");
		facetNames(buf, redraw);
		return buf.toString();
	}

	public boolean equals(Object aThat) {
		if (this == aThat)
			return true;
		if (!(aThat instanceof Cluster))
			return false;
		Cluster that = (Cluster) aThat;
		return this.facets.equals(that.facets) && this.query.equals(that.query);
	}

	public int hashCode() {
		return 37 * facets.hashCode() + query.hashCode();
	}

	public int getOnCount() {
		return nOnItems;
	}

	public int getTotalCount() {
		return nItems;
	}

	public String getName() {
		return toString();
	}

	public Markup describeFilter() {
		Markup result = Query.emptyMarkup();
		result.add("filter");
		result.add(this);
		return result;
	}

	public Markup facetDoc(int modifiers) {
		String prefix = null;
		if (Perspective.isExcludeAction(modifiers))
			prefix = "remove ";
		else
			prefix = "add ";
		Markup result = describeFilter();
		result.add(0, prefix);
		return result;
	}

	public Query query() {
		return ((Perspective) facets.first()).query();
	}

	public int nRestrictions() {
		return facets.size();
	}

	public SortedSet allRestrictions() {
		return facets;
	}

	public boolean isRestriction(ItemPredicate facet, boolean required) {
		return required ? facets.contains(facet) : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#isRestriction()
	 */
	public boolean isRestriction() {
		return false;
	}

	public boolean restrictData() {
		assert false : "Not yet implemented";
		return false;
	}

	public double percentOn() {
		return nOnItems / (double) nItems;
	}

	public int chiColorFamily(double p) {
		return pValue <= p ? 1 : 0;
	}

	public String getNameIfPossible() {
		return getName();
	}

//	public ItemPredicate getParent() {
//		return null;
//	}

	public boolean isEffectiveChildren() {
		return false;
	}

	public boolean isRestricted() {
		return false;
	}

	public int guessOnCount() {
		return getOnCount();
	}

}
