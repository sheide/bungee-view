package edu.cmu.cs.bungee.client.query;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import edu.cmu.cs.bungee.javaExtensions.*;



public class Cluster implements Serializable, ItemPredicate {

	private static final long serialVersionUID = 6810818343599576453L;

	private final Perspective[] facets;

//	Perspective seed;

	private int nOnItems;

	private final int nItems;

	private final double pValue;

//	public Cluster(Perspective[] result, Perspective _seed, int _nItems) {
//		assert !Util.isMember(result, null) : PrintArray
//				.printArrayString(result);
//		facets = result;
//		seed = _seed;
//		nItems = _nItems;
//	}

	public int size() {
		return facets.length;
	}

	public Cluster (ResultSet rs, Query q) {
		Perspective[] _facets = null;
		int _nOnItems = -1;
		int _nItems = -1;
		double _pValue = -1;
		try {
			rs.last();
			// Util.print(rs.getRow());
			_facets = new Perspective[rs.getRow()];
			rs.beforeFirst();
			while (rs.next()) {
//				 Util.print("cluster " + rs.getInt(2) + " " + rs
//							.getString(3) + " " + rs.getInt(1));
				if (rs.getInt(7) == 0) {
					Perspective p = q.findPerspective(rs.getInt(2));
					_facets[rs.getRow() - 1] = p;
					_nOnItems = rs.getInt(9);
					_nItems = rs.getInt(10);
					_pValue = rs.getDouble(8);
				}
			}
			q.close(rs);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		nOnItems = _nOnItems;
		nItems = _nItems;
		pValue = _pValue;
		facets = (Perspective[]) Util.delete(_facets, (ItemPredicate) null,
				Perspective.class);
	}
	
	public double pValue() {
		assert 0 <= pValue && pValue <= 1;
		return pValue;
	}
	
	public Cluster(Perspective[] _facets) {
		nOnItems = -1;
		nItems = -1;
		pValue = -1;
		facets = (Perspective[]) _facets.clone();
	}

//	public String toString() {
//		return "Cluster of " + size() + " features, having " + nItems
//				+ " items, from " + seed;
//	}
	
	public StringBuffer facetNames(StringBuffer buf, PerspectiveObserver redraw) {
		if (buf == null)
			buf = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			if (i > 0)
				buf.append(", ");
			buf.append(facets[i].getName(redraw));
//			desc.add(facet.facets[i]);
		}
		return buf;
	}

	public String toString() {
//		return "Cluster of " + size() + " features, having " + nOnItems + "/" + nItems
//				+ " items, with p = " + chiSq;
		StringBuffer buf = new StringBuffer();
		buf.append("Cluster of ").append(size()).append(" features: ");
		facetNames(buf, null);
		return buf.toString();
	}

	public boolean equals(Object aThat) {
		if (this == aThat)
			return true;
		if (!(aThat instanceof Cluster))
			return false;
		Cluster that = (Cluster) aThat;
		return Arrays.equals(this.facets, that.facets);
	}

	public int hashCode() {
		return facets.hashCode();
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

	public Query getQuery() {
		return facets[0].getQuery();
	}

	public int nRestrictions() {
		return facets.length;
	}

	public Perspective[] allRestrictions() {
		return (Perspective[]) facets.clone();
	}

	public boolean isRestriction(ItemPredicate facet, boolean required) {
		return required ? Util.isMember(facets, facet) : false;
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

}
