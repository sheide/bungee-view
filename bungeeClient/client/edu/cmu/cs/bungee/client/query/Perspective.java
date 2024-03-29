/* 
 
 Created on Mar 8, 2005 

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

import java.awt.Color;
import java.awt.event.InputEvent;
import java.io.DataInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.CollationKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import JSci.maths.statistics.ChiSq2x2;
import JSci.maths.statistics.OutOfRangeException;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.comparator.DoubleValueComparator;
import edu.cmu.cs.bungee.javaExtensions.comparator.IntValueComparator;

/**
 * aka Facet. a property that an Item can have.
 * 
 * @author mad
 */
public class Perspective implements ItemPredicate {

	/**
	 * Perspective for the more general facet or facet_type, Only changes if
	 * isEditable
	 */
	Perspective parent;

	/**
	 * Only changes if isEditable
	 */
	int facet_id;

	private int totalCount = -1;

	int onCount = -1;

	/**
	 * The SUM of the totalCounts for us and our previous siblings. Used for
	 * placing bars.
	 */
	int cumCount;

	private InstantiatedPerspective instantiatedPerspective = null;

	// public static final Comparator indexComparator = new IndexComparator();

	static final String[] filterTypes = { " = ", " \u2260 " };

	static final Color[] filterColors = { Markup.INCLUDED_COLORS[0],
			Markup.EXCLUDED_COLORS[0] };

	// static final Perspective[] noDescendents = new ItemPredicate[0];
	//
	// /**
	// * Used to sort dataIndexByOn.
	// */
	// private static final Comparator onCountComparator = new
	// OnCountComparator();
	//
	// private static final Comparator totalCountComparator = new
	// TotalCountComparator();

	Perspective(int _facet_id, Perspective _parent, String _name,
			int _children_offset, int n_children) {
		// Util.print("Perspective " + _name + " " + _children_offset + " "
		// + n_children);
		assert _parent != null;
		assert _parent.query().findPerspectiveIfPossible(facet_id) == null : _parent
				+ " " + _name;
		parent = _parent;
		facet_id = _facet_id;
		if (_name != null || n_children > 0) {
			instantiatedPerspective = new InstantiatedPerspective(this,
					n_children, _children_offset, _name);
		}
	}

	Perspective(int _facet_id, Perspective _parent, String _name,
			int _children_offset, int n_children, String _descriptionCategory,
			String _descriptionPreposition, Query _q) {
		// Util.print("Perspective " + _name);
		assert _q.findPerspectiveIfPossible(facet_id) == null : _parent + " "
				+ _name;
		parent = _parent;
		facet_id = _facet_id;
		instantiatedPerspective = new InstantiatedPerspective(n_children,
				_children_offset, _name, _descriptionCategory,
				_descriptionPreposition, _q);
	}

	// void setDescriptionInfo(String _descriptionPreposition) {
	// // Util.print("instantiate " + name + " " + nChildren);
	// instantiatedPerspective.setDescriptionInfo(_descriptionPreposition);
	// }

	/**
	 * Can be called from thread prefetcher
	 */
	InstantiatedPerspective ensureInstantiatedPerspective() {
		if (instantiatedPerspective == null)
			instantiatedPerspective = new InstantiatedPerspective(this);
		return instantiatedPerspective;
	}

	Perspective() {
		// dummary facet for cumCountChildIterator

		// make sure exclusive cumCount == cumCount
		totalCount = 0;
	}

	void setTotalCount(int count) {
		// if ("Posters".equals(getNameIfPossible())) {
		// Util.print("setTotalCount " + this + " " + totalCount + " => "
		// + count);
		// }
		totalCount = count;
	}

	/**
	 * Used to pass a redrawer into FacetNameComparator.
	 */
	static PerspectiveObserver redrawer;

	static final Perspective dummyCumCount = new Perspective();

	/**
	 * @param minCount
	 * @param maxCount
	 * @return Iterator over children c where: c.cumCount >= minCount &&
	 *         c.cumCountExclusive <= maxCount
	 */
	public Iterator<Perspective> cumCountChildIterator(int minCount,
			int maxCount) {
		return instantiatedPerspective
				.cumCountChildIterator(minCount, maxCount);
	}

	static class CumCountInclusiveComparator extends
			IntValueComparator<Perspective> {

		@Override
		public int value(Perspective data) {
			// Util.print("value cum " + data + " "
			// + ((Perspective) data).cumCount);
			return -data.cumCount;
		}
	}

	static final CumCountInclusiveComparator cumCountInclusiveComparator = new CumCountInclusiveComparator();

	static class CumCountExclusiveComparator extends
			IntValueComparator<Perspective> {

		@Override
		public int value(Perspective data) {
			// Util.print("value cumExc " + data + " " + ((Perspective)
			// data).cumCountExclusive());
			return -data.cumCountExclusive();
		}
	}

	static final CumCountExclusiveComparator cumCountExclusiveComparator = new CumCountExclusiveComparator();

	// /**
	// * Alphabetic comparison for facet names, except fail fast and return 0 if
	// * name isn't cached. Caller is assuming that binarySearch will return
	// * immediately if this Comparator returns 0.
	// *
	// */
	// static class FacetNameComparator implements Comparator {
	//
	// public int compare(Object arg0, Object arg1) {
	// int result = 0;
	// String name0 = ((Perspective) arg0).getName(redrawer, null);
	// if (name0 != null) {
	// String name1 = ((Perspective) arg1).getName(redrawer, null);
	// if (name1 != null)
	// result = name0.compareToIgnoreCase(name1);
	// }
	// // Util.print("compare " + arg0 + " " + arg1 + " => " + result);
	// return result;
	// }
	// }
	//
	// static final FacetNameComparator facetNameComparator = new
	// FacetNameComparator();
	//
	// /**
	// * @param prefix
	// * @param letter
	// * @param redraw
	// * callback in the case of uncached names
	// * @return first (alphabetically) Perspective starting with prefix
	// followed
	// * by letter, or any other Perspective if none exist, or null if we
	// * don't know due to uncached facet names.
	// */
	// public Perspective firstWithLetter(String prefix, char letter,
	// PerspectiveObserver redraw) {
	// return instantiatedPerspective.firstWithLetter(prefix, letter, redraw);
	// }
	//
	// /**
	// * @param prefix
	// * @param letter
	// * @param redraw
	// * callback in the case of uncached names
	// * @return last (alphabetically) Perspective starting with prefix followed
	// * by letter, or any other Perspective if none exist, or null if we
	// * don't know due to uncached facet names.
	// */
	// public Perspective lastWithLetter(String prefix, char letter,
	// PerspectiveObserver redraw) {
	// return instantiatedPerspective.lastWithLetter(prefix, letter, redraw);
	// }

	/**
	 * @return number of ancestor Perspectives. 0 means no parent.
	 */
	int depth() {
		return parent != null ? parent.depth() + 1 : 0;
	}

	/**
	 * @return ancestors (not including this perspective)
	 */
	public Set<Perspective> ancestors() {
		if (parent != null) {
			Set<Perspective> result = parent.ancestors();
			result.add(parent);
			return result;
		} else {
			return new HashSet<Perspective>();
		}
	}

	/**
	 * @param leafs
	 * @return leafs + their ancestors
	 */
	public static SortedSet<Perspective> ancestors(Set<Perspective> leafs) {
		SortedSet<Perspective> result = new TreeSet<Perspective>(leafs);
		for (Iterator<Perspective> it = leafs.iterator(); it.hasNext();) {
			Perspective leaf = it.next();
			result.addAll(leaf.ancestors());
		}
		return result;
	}

	/**
	 * @return the next more general Perspective, e.g. 2007 => 21st century
	 */
	public Perspective getParent() {
		return parent;
	}

	void setParent(Perspective _parent) {
		if (query().isEditable())
			parent = _parent;
		else {
			throw (new UnsupportedOperationException("Can't change parent of "
					+ this));
		}
	}

	public Perspective previousSibling() {
		return previousSibling(true);
	}

	/**
	 * @return preceding sibling in sort and/or alphabetical order, or null
	 */
	public Perspective previousSibling(boolean isErrorIfNotCached) {
		Perspective result = null;
		if (whichChild() > 0) {
			result = isErrorIfNotCached ? query().findPerspective(facet_id - 1)
					: query().findPerspectiveIfPossible(facet_id - 1);
		}
		return result;
	}

	public Perspective nextSibling() {
		return nextSibling(true);
	}

	/**
	 * @return next sibling in sort and/or alphabetical order, or null
	 */
	public Perspective nextSibling(boolean isErrorIfNotCached) {
		Perspective result = null;
		if (whichChild() + 1 < nSiblings()) {
			result = isErrorIfNotCached ? query().findPerspective(facet_id + 1)
					: query().findPerspectiveIfPossible(facet_id + 1);
		}
		return result;
	}

	/**
	 * @return parent.nChildren() or if no parent query().nAttributes
	 */
	public int nSiblings() {
		return parent != null ? parent.nChildren() : query().nAttributes;
	}

	public int getID() {
		return facet_id;
	}

	public String getServerID() {
		return Integer.toString(facet_id);
	}

	void setID(int _facet_id) {
		if (query().isEditable())
			facet_id = _facet_id;
		else {
			throw (new UnsupportedOperationException(
					"Can't change facet_id of " + this));
		}
	}

	/**
	 * @return number of child facets
	 */
	public int nChildren() {
		return instantiatedPerspective == null ? 0
				: instantiatedPerspective.nChildren;
	}

	/**
	 * @return do any of my child facets have non-zero total count?
	 */
	public boolean isEffectiveChildren() {
		// if restrictData, all children might have zero count, in which case
		// we wouldn't have any bars to draw.
		// if it is -1, we don't know yet, so assume OK
		// Util.print("isEffectiveChildren " + this + " " + nChildren() + " "
		// + instantiatedPerspective.totalChildTotalCount);
		return nChildren() > 0
				&& instantiatedPerspective.totalChildTotalCount != 0;
	}

	int childrenOffset() {
		return instantiatedPerspective.children_offset;
	}

	/**
	 * @return Whether children's names are ordered by facet_id. The interface
	 *         will only zoom by prefixes if so.
	 */
	public boolean isAlphabetic() {
		boolean result = instantiatedPerspective.isAlphabetic;
		// assert result == checkAlphabetic() : this + " " + result;
		return result;
	}

	boolean checkAlphabetic() {
		boolean result = true;
		String prev = "";
		for (Iterator<Perspective> it = getChildIterator(); it.hasNext()
				&& result;) {
			Perspective child = it.next();
			String name = child.getNameIfPossible();
			if (name != null) {
				if (name.compareToIgnoreCase(prev) < 0) {
					// Util.print(prev + " >>> " + name);
					result = false;
				}
				prev = name;
			}
		}
		return result;
	}

	/**
	 * Can be called from thread prefetcher. Cannot be called recursively.
	 * 
	 */
	public void prefetchData() {
		if (!isPrefetched()) {
			assert nChildren() > 0 : this
					+ " doesn't have any tags. You should give it a negative value for sort in the raw_facet_types table.";
			synchronized (this) {
				if (!isPrefetched()) {
					ensureInstantiatedPerspective();

					int fetchType = 1;
					if (query().isRestrictedData())
						// The database treats this case specially, but it is
						// treated as 1 in initPerspective
						fetchType = 5;
					else if (instantiatedPerspective.totalChildTotalCount > 0)
						// This indicates that we've already retrieved the
						// counts, which can only be true for top-level tags
						fetchType = 3;
					if (nChildren() > 100)
						fetchType += 1;

					query().prefetch(this, fetchType);

					setPrefetched(true);
					notifyAll();
				}
			}
		}
	}

	void initPerspective(DataInputStream in, int fetchType) {
		if (fetchType > 4)
			fetchType -= 4;
		setChildrenOffset(MyResultSet.readInt(in));
		initPerspective(new MyResultSet(in, prefetchColumnTypes(fetchType)),
				fetchType);
		setIsAlphabetic(MyResultSet.readInt(in) > 0);
		// if (isAlphabetic()) {
		// createLetterOffsets(new MyResultSet(in, MyResultSet.STRING_SINT),
		// Util.toCollationKey(""));
		// }
		// Util.print("offset " + facet + " " + facet.childrenOffset() + " "
		// + facet.isAlphabetic());
	}

	static List<Object> prefetchColumnTypes(int fetchType) {
		List<Object> types = null;
		switch (fetchType) {
		case 1:
			types = MyResultSet.INT_INT_STRING;
			break;
		case 2:
			types = MyResultSet.INT_INT;
			break;
		case 3:
			types = MyResultSet.INT_STRING;
			break;
		case 4:
			types = MyResultSet.INT;
			break;
		default:
			assert false : "prefetch args=" + fetchType;
		}
		return types;
	}

	// cases:
	// 0: initFacetTypes: count
	// 1: prefetch a new facet: count, nChildren, name
	// 2: prefetch a new facet: count, nChildren
	// 3: prefetch a top-level facet (count is already set): nChildren, name
	// 4: prefetch a top-level facet (count is already set): nChildren
	void initPerspective(ResultSet rs, int fetchType) {
		assert isInstantiated();
		try {
			boolean isCount = fetchType <= 2;
			boolean isName = fetchType == 1 || fetchType == 3;
			boolean isNchildren = fetchType > 0;
			// Map cHist = new TreeMap();
			// Map offHist = new TreeMap();
			// Map ncHist = new TreeMap();

			// Util.print("Query.initPerspective isName=" + isName + " isCount="
			// + isCount + " isOffset=" + isOffset + " " + p
			// + " nChildren=" + p.nChildren() + " childrenOffset="
			// + p.children_offset());

			int child_facet_id = childrenOffset();
			int nRemainingChildren = nChildren();
			int child_cumCount = 0;
			int maxCount = -1;
			int count = -1;
			String name1 = null;
			while (--nRemainingChildren >= 0) {
				rs.next();
				Perspective v;
				if (isNchildren) {
					int fieldOffset = isCount ? 1 : 0;
					// int childrenOffset = rs.getInt(fieldOffset + 2);
					int nChildren = rs.getInt(fieldOffset + 1);
					if (isName)
						name1 = rs.getString(fieldOffset + 2);

					// offset is set in v.initPerspective(stream, fetchType)
					v = ensureChild(++child_facet_id, name1, nChildren);

					// incf(offHist, childrenOffset);
					// incf(ncHist, nChildren);

					// if (v.nChildren > 0 && nameGetter != null) {
					// nameGetter.addFacet(v);
					// }
					// if (isCount)
					// count = rs.getInt(1);
				} else {
					assert isCount;
					// count = rs.getInt(1);
					v = ensureChild(++child_facet_id, null, 0);
				}
				if (isCount) {
					count = rs.getInt(1);
					// incf(cHist, count);

					assert count > 0 || query().isRestrictedData() : this + " "
							+ v + " " + count;
					// Need to DELETE FROM facet WHERE count = 0
					child_cumCount += count;
					// v.onCount = count;
					if (count > maxCount) {
						// Util
						// .print("initPerspective setting max child count to "
						// + count + " (" + v + ")");
						maxCount = count;
					}
					// Util.print("Setting " + v + " totalCount=" + count);
					v.setTotalCount(count);
					v.cumCount = child_cumCount;
				}
			}

			// showHist(cHist, "Counts:");
			// showHist(offHist, "Offsets:");
			// showHist(ncHist, "nChilds:");
			if (isCount) {
				setTotalChildTotalCount(child_cumCount);
				setMaxChildTotalCount(maxCount);
				// if (p.parent != null) {
				// // For a new facet, any other displayed siblings have to
				// // make room.
				// p.parent.updateChildPercents();
				// }
			}
			// assert p.isDataIndexByOnComplete();
			rs.close();
		} catch (SQLException se) {
			Util.err("SQL Exception in perspective.updateData: "
					+ se.getMessage());
			se.printStackTrace();
		}
	}

	Perspective ensureChild(int child_facet_id, String child_name,
			int child_nChildren) {
		return query().ensurePerspective(child_facet_id, this, child_name, -1,
				child_nChildren);
	}

	void setPrefetched(boolean state) {
		instantiatedPerspective.isPrefetched = state;
	}

	/**
	 * @return whether our children have been created
	 */
	public boolean isPrefetched() {
		return instantiatedPerspective != null
				&& instantiatedPerspective.isPrefetched;
	}

	boolean isInstantiated() {
		return instantiatedPerspective != null;
	}

	/**
	 * @return whether there should be a row of bars for this perspective
	 */
	public boolean isDisplayed() {
		return query().displaysPerspective(this);
	}

	// void setParentPercent(double percent) {
	// if (instantiatedPerspective != null)
	// // This can be null if GetPerspectiveNames runs at the wrong time.
	// instantiatedPerspective.parentPercent = percent;
	// }
	//
	// void updateChildPercents() {
	// int total = 0;
	//
	// for (Iterator it = getQuery().perspectivesIterator(); it.hasNext();) {
	// Perspective child = (Perspective) it.next();
	// if (child.parent == this) {
	// total += child.totalCount;
	// }
	// }
	// if (total > 0) {
	// double dTotal = total;
	//
	// for (Iterator it = getQuery().perspectivesIterator(); it.hasNext();) {
	// Perspective child = (Perspective) it.next();
	// if (child.parent == this) {
	// child.setParentPercent(child.totalCount / dTotal);
	// }
	// }
	// }
	// }

	public int guessOnCount() {
		if (query().isQueryValid())
			return getOnCount();
		else
			return getTotalCount();
	}

	/**
	 * query() is surprisingly slow, so when it matters don't keep recomputing
	 * it.
	 * 
	 * @param isQueryRestricted
	 * @return onCount
	 */
	public int getOnCount(boolean isQueryRestricted) {
		return isQueryRestricted ? onCount : totalCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getOnCount()
	 */
	public int getOnCount() {
		Query query = query();
		assert query.isQueryValid();
		int onCount2 = getOnCount(query.isRestricted());
		// assert onCount2 <= getTotalCount() : onCount2 + " " + getTotalCount()
		// + " " + this;
		return onCount2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getTotalCount()
	 */
	public int getTotalCount() {
		// deeply nested facets will have totalCount = -1;
		// Util.print("getTotalCount " + this + " " + totalCount);
		// if (totalCount < 0) {
		// assert false : this;
		// assert parent == null;
		// totalCount = getTotalChildTotalCount();
		// }
		Query q = query();
		if (!q.isRestrictedData() || parent == null
				|| q.displaysPerspective(parent)) {
			// Util.print("getTotalCount "+this+" "+totalCount);
			return totalCount;
		} else {
			// assert false : this;
			return -1;
		}
	}

	/**
	 * @return standard deviation of this binary variable, over the whole
	 *         database
	 */
	public double stdDev() {
		double n = query().getTotalCount();
		double count = getTotalCount();
		double stdDev = Math.sqrt(count * (n - count) / (n * (n - 1)));
		assert stdDev >= 0 : count + " " + n + " " + this;
		return stdDev;
	}

	public int parentTotalCount() {
		if (parent == null)
			return query().getTotalCount();
		else
			return parent.getTotalCount();
	}

	public int parentOnCount() {
		if (parent == null)
			return query().getOnCount();
		else
			return parent.getOnCount();
	}

	/**
	 * @return The SUM of the totalCounts for us and our previous siblings. Used
	 *         for placing bars.
	 */
	public int cumCountInclusive() {
		return cumCount;
	}

	/**
	 * @return The SUM of the totalCounts for our previous siblings. Used for
	 *         placing bars.
	 */
	public int cumCountExclusive() {
		return cumCount - totalCount;
	}

	/**
	 * @return index between 0 and nChildren-1
	 */
	public int whichChild() {
		int result = getID() - 1;
		if (parent != null)
			result -= parent.childrenOffset();
		return result;
	}

	/**
	 * @param n
	 *            index into children perspepctives (between 0 and nChildren -
	 *            1)
	 * @return the nth child facet
	 */
	public Perspective getNthChild(int n) {
		return instantiatedPerspective.getNthChild(n);
	}

	/**
	 * @return an iteraor over our child facets
	 */
	public Iterator<Perspective> getChildIterator() {
		return instantiatedPerspective.getChildIterator();
	}

	/**
	 * @param min
	 *            first child returned
	 * @param max
	 *            last child returned
	 * @return an iteraor over some of our child facets (used to iterate over
	 *         visible bars)
	 */
	public Iterator<Perspective> getChildIterator(Perspective min,
			Perspective max) {
		return instantiatedPerspective.getChildIterator(min, max);
	}

	/**
	 * Can be called from thread prefetcher
	 * 
	 * @return the sum of the totalCount of our children. May differ from
	 *         totalCount because an item can have multiple sibling facets,
	 *         and/or may have this facet but not any of our children. -1 if we
	 *         haven't been initPerspective'd.
	 */
	public int getTotalChildTotalCount() {
		// getQuery().prefetchData(this);
		assert instantiatedPerspective.totalChildTotalCount >= 0 : this;
		return instantiatedPerspective.totalChildTotalCount;
	}

	void setTotalChildTotalCount(int cnt) {
		// Util.print("setTotalChildTotalCount " + this + " " + cnt);
		assert cnt >= 0 : this + " " + query() + " "
				+ query().displayedPerspectives();
		instantiatedPerspective.totalChildTotalCount = cnt;
		if (cnt == 0 && getParent() != null) {
			// Uh-oh. In the unrestricted database we have children, but in the
			// restricted one they all have zero count. We didn't know that
			// ahead of time, and added a pv. Oh well, viz can notice this and
			// remove it again, via PV.updateData(). Can't do it in
			// this thread or you'd get concurrent modification on
			// displayedPerspectives.
			// Util.print("zero totalChildTotalCount for " + this + " " +
			// query());
			// parent.deselectFacet(this, true);
		}
	}

	/**
	 * @return the p-value that the conditional median is different from the
	 *         unconditional median.
	 */
	public double medianTest() {
		return instantiatedPerspective.medianPvalue();
	}

	/**
	 * @return is this facet's median significantly different from its parent's
	 *         median?
	 */
	public int medianTestSignificant() {
		double pValue = instantiatedPerspective.medianPvalue();
		double threshold = 0.01 / query().nOrderedAttributes();
		int result = 0;
		if (pValue <= threshold)
			result = instantiatedPerspective.medianPvalueSign();
		return result;
	}

	/**
	 * @param isConditional
	 *            want median according to onCount or totalCount
	 * @return the whichChild (between 0 and nChildren-1] of the median + the
	 *         fraction of the median below the halfway point, when you lay out
	 *         count copies of all the child facets. Returns -1 if no items
	 *         satisfy this predicate.
	 */
	public double median(boolean isConditional) {
		return instantiatedPerspective.median(isConditional);
	}

	/**
	 * @param isConditional
	 *            Want median according to onCount or totalCount?
	 * @return The median child facet.
	 */
	public Perspective getMedianPerspective(boolean isConditional) {
		Perspective medianChild = null;
		if (query().isQueryValid() && getOnCount() > 0) {
			double median = median(isConditional);
			int medianIndex = (int) median;
			medianChild = getNthChild(medianIndex);
		}
		return medianChild;
	}

	/**
	 * @return spaces to indent according to this perspective's ancestor depth,
	 *         followed by a symbol if this perspective has a parent.
	 */
	public String namePrefix() {
		String result = "";
		int level = depth();
		for (int i = 1; i < level; i++)
			result += "  ";
		if (level > 0)
			result += Markup.parentIndicatorPrefix;
		// Util.print("namePrefix " + this + " '" + result + "'");
		return result;
	}

	/**
	 * @return this facet's name, or null if it hasn't been read from the
	 *         database yet
	 */
	public String getNameIfPossible() {
		return instantiatedPerspective == null ? null
				: instantiatedPerspective.name;
	}

	public String getName(PerspectiveObserver redraw) {
		return getName(redraw, "?");
	}

	/**
	 * @param redraw
	 *            if name hasn't been read from the database, callback redraw
	 *            when it is
	 * @param defaultName
	 * @return the name of this facet, or defaultName if it hasn't been read yet
	 */
	public String getName(PerspectiveObserver redraw, String defaultName) {
		String result = getNameIfPossible();
		if (result != null) {
			return result;
		}
		assert redraw != null : this;
		Query q = query();

		// Why did we do this?
		// if (!parent.isPrefetched()) {
		// q.queuePrefetch(getParent());
		// }

		NameGetter nameGetter = q.nameGetter;
		assert nameGetter != null : q;
		nameGetter.add(this);
		nameGetter.add(redraw);
		// Util.print("getName " + parent.isPrefetched() + " " + this);
		return defaultName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getName()
	 */
	public String getName() {
		return getName(null);
	}

	/**
	 * @param _name
	 *            either we just read the name from the database, or we're
	 *            editing and changing the name for this facet
	 */
	public void setName(String _name) {
		if (_name != null)
			ensureInstantiatedPerspective().name = _name;
	}

	void setChildrenOffset(int offset) {
		if (nChildren() > 0) {
			// Util.print("setChildrenOffset " + this + " " + offset);
			ensureInstantiatedPerspective().children_offset = offset;
		}
	}

	void setIsAlphabetic(boolean _isAlphabetic) {
		instantiatedPerspective.isAlphabetic = _isAlphabetic;
	}

	/**
	 * @param prefix
	 * @return Iterator over getLetterOffsets(prefix)
	 */
	public Iterator<Entry<CollationKey, Perspective[]>> letterOffsetsIterator(
			CollationKey prefix) {
		return getLetterOffsets(prefix).entrySet().iterator();
	}

	/**
	 * @param prefix
	 * @return Perspective with lowest facet_id whose name begins with prefix,
	 *         or null if none exist.
	 */
	// public Perspective firstWithPrefix(String prefix) {
	// int charIndex = prefix.length();
	// if (charIndex == 0) {
	// return getNthChild(0);
	// }
	// String prefixButOne = prefix.substring(0, charIndex - 1);
	// char desiredLetter = prefix.charAt(charIndex - 1);
	// Perspective firstWithLetter = null;
	// for (Iterator it = letterOffsetsIterator(prefixButOne); it.hasNext();) {
	// Entry entry = (Entry) it.next();
	// char letter = ((Character) entry.getKey()).charValue();
	// Perspective lastWithLetter = (Perspective) entry.getValue();
	// Util.print("fwl " + desiredLetter + " " + letter + " "
	// + firstWithLetter);
	// if (letter == desiredLetter) {
	// if (firstWithLetter == null)
	// firstWithLetter = firstWithPrefix(prefixButOne);
	// // Util.print("firstWithPrefix " + prefix + " => " +
	// // firstWithLetter);
	// return firstWithLetter;
	// } else if (letter > desiredLetter)
	// return null;
	// firstWithLetter = lastWithLetter.nextSibling();
	// }
	// return null;
	// }
	public Perspective firstWithPrefix(CollationKey prefix) {
		String sourceString = prefix.getSourceString();
		int charIndex = sourceString.length();
		if (charIndex > 0) {
			Perspective[] rangeForPrefix = rangeForPrefix(prefix);
			return rangeForPrefix == null ? null : rangeForPrefix[0];
		} else {
			return getNthChild(0);
		}
	}

	/**
	 * @param prefix
	 * @return Perspective with highest facet_id whose name begins with prefix,
	 *         or null if none exist.
	 */
	public Perspective lastWithPrefix(CollationKey prefix) {
		String sourceString = prefix.getSourceString();
		int charIndex = sourceString.length();
		if (charIndex > 0) {
			Perspective[] rangeForPrefix = rangeForPrefix(prefix);
			return rangeForPrefix == null ? null : rangeForPrefix[1];
		} else {
			return getNthChild(nChildren() - 1);
		}
	}

	public Perspective[] rangeForPrefix(CollationKey prefix) {
		// prefix = Util.toCollationKey(prefix.getSourceString().toUpperCase());
		// Util.print("lastWithPrefix " + prefix);
		String sourceString = prefix.getSourceString();
		int charIndex = sourceString.length();
		// if (charIndex == 0) {
		// return getNthChild(nChildren() - 1);
		// }
		CollationKey prefixButOne = Util.toCollationKey(sourceString.substring(
				0, charIndex - 1));
		CollationKey desiredLetter = Util.toCollationKey(sourceString
				.substring(charIndex - 1));
		// Util.print("rangeForPrefix '"
		// + prefixButOne.getSourceString()
		// + "' "
		// + desiredLetter.getSourceString()
		// + " "
		// + Util.valueOfDeep(getLetterOffsets(prefixButOne).get(
		// desiredLetter)));
		return getLetterOffsets(prefixButOne).get(desiredLetter);
	}

	public Map<CollationKey, Perspective[]> getLetterOffsets(CollationKey prefix) {
		assert isAlphabetic();
		Map<CollationKey, Perspective[]> letterOffsets = instantiatedPerspective.lettersOffsets
				.get(prefix);
		if (letterOffsets == null) {
			// assert prefix.equals(prefix.toUpperCase());
			letterOffsets = createLetterOffsets(prefix);
			// Util.print(prefix + " " + letterOffsets);
		}
		// Util.print("getLetterOffsets " + this+" '"+prefix.getSourceString() +
		// "' "
		// + (letterOffsets == null ? 0 : letterOffsets.size()));
		return letterOffsets;
	}

	public String letterOffsetsString(CollationKey prefix) {
		StringBuffer buf = new StringBuffer();
		for (Iterator<Entry<CollationKey, Perspective[]>> it = letterOffsetsIterator(prefix); it
				.hasNext();) {
			Entry<CollationKey, Perspective[]> entry = it.next();
			CollationKey letter = entry.getKey();
			Perspective[] firstLast = entry.getValue();
			buf.append(letter.getSourceString()).append("\t=> ").append(
					firstLast[0]).append(" - ").append(firstLast[1]).append(
					"\n");
		}
		return buf.toString();
	}

	private Map<CollationKey, Perspective[]> createLetterOffsets(
			CollationKey prefix) {
		// Util.print("createLetterOffsets " + this + " '"
		// + prefix.getSourceString() + "'");
		Map<CollationKey, Perspective[]> letterOffsets = null;
		try {
			ResultSet rs = query().getLetterOffsets(this, prefix);
			Perspective first = firstWithPrefix(prefix);
			// assert first.couldStartWith(prefix.getSourceString());
			assert first != null : this + " '" + prefix.getSourceString()
					+ "'\n"
					+ MyResultSet.valueOfDeep(rs, MyResultSet.STRING_SINT, 200);
			// Util.print("CLO "+MyResultSet.valueOfDeep(rs,
			// MyResultSet.STRING_SINT, 200));
			letterOffsets = new LinkedHashMap<CollationKey, Perspective[]>(
					MyResultSet.nRows(rs));
			while (rs.next()) {
				// assert first.couldStartWith(prefix.getSourceString());
				assert rs.getString(1).length() > 0 : this + ": '"
						+ prefix.getSourceString()
						+ "' is a maximal child name; there are no extensions";
				CollationKey key = Util.toCollationKey(rs.getString(1)
						.charAt(0));
				Perspective last = query().findPerspective(rs.getInt(2));
				assert last != null : this
						+ " '"
						+ prefix.getSourceString()
						+ "' '"
						+ key.getSourceString()
						+ "'\n"
						+ MyResultSet.valueOfDeep(rs, MyResultSet.STRING_SINT,
								200);
				Perspective[] firstLast = { first, last };
				letterOffsets.put(key, firstLast);
				// Util.print(" " + key.getSourceString() + " => "
				// + Util.valueOfDeep(firstLast));
				assert last.couldStartWith(prefix.getSourceString()
						+ key.getSourceString());
				first = last.nextSibling();
			}
			rs.close();
			instantiatedPerspective.lettersOffsets.put(prefix, letterOffsets);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Util.print("letter offsets for " + this + " '" + prefix + "':");
		// Util.print(letterOffsets);
		return letterOffsets;
	}

	private boolean couldStartWith(String prefix) {
		String name = getNameIfPossible();
		return name == null
				|| (name.length() >= prefix.length() && Util.stringEquals(
						prefix, name.substring(0, prefix.length())));
	}

	void setNchildren(int n, int child_offset) {
		if (n > 0)
			ensureInstantiatedPerspective().setNchildren(n, child_offset);
	}

	// public boolean isNameValid() {
	// return name != null;
	// }

	String[] getDescriptionPreposition() {
		return instantiatedPerspective == null ? parent.instantiatedPerspective.descriptionPreposition
				: instantiatedPerspective.descriptionPreposition;
	}

	String getDescriptionCategory() {
		return instantiatedPerspective == null ? parent.instantiatedPerspective.descriptionCategory
				: instantiatedPerspective.descriptionCategory;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getNameIfPossible()).append(" (").append(getServerID())
				.append(")");
		// buf.append(" index=" + getIndex() + " ");
		// buf.append(nChildren).append(" ").append(children_offset);
		// buf.append("; local:
		// ").append(onCount).append("/").append(totalCount);
		// buf.append("; cum on parent: ").append(cumCount).append("/");
		// if (parent != null)
		// buf.append(parent.getTotalChildTotalCount());
		// else
		// buf.append("?");
		return buf.toString();
	}

	public String toString(PerspectiveObserver redrawer1) {
		StringBuffer buf = new StringBuffer();
		buf.append(getName(redrawer1)).append(" (").append(getServerID())
				.append(")");
		// buf.append(" index=" + getIndex() + " ");
		// buf.append(nChildren).append(" ").append(children_offset);
		// buf.append("; local:
		// ").append(onCount).append("/").append(totalCount);
		// buf.append("; cum on parent: ").append(cumCount).append("/");
		// if (parent != null)
		// buf.append(parent.getTotalChildTotalCount());
		// else
		// buf.append("?");
		return buf.toString();
	}

	String toStringWithCumCount() {
		return this + " (" + cumCountExclusive() + " - " + cumCountInclusive()
				+ ")";
	}

	// public String getRestrictionName(boolean isLocalOnly) {
	// String[] names = getRestrictionNames(isLocalOnly);
	// assert perspective.restrictions.length == 0 || names.length > 0;
	// return Util.arrayToEnglish(names, " or ");
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#describeFilter()
	 */
	public Markup describeFilter() {
		Markup filterDescription = getDescription(false, filterTypes); // getRestrictionName
		// (
		// false
		// );
		filterDescription.add(0, "filter ");
		filterDescription.add(1, getFacetType());
		// filterDescription.add(2, " = ");
		// Util.print("Perspective.describeFilter "
		// + Util.valueOfDeep(filterDescription));
		return filterDescription;
	}

	void describeFilter(Markup v, Perspective facet, boolean require) {
		v.add(getFacetType());
		v.add(filterColors[require ? 0 : 1]);
		v.add(filterTypes[require ? 0 : 1]);
		v.add(Markup.DEFAULT_COLOR_TAG);
		v.add(facet);
		// Util.print("Perspective.describeFilter "
		// + Util.valueOfDeep(v));
	}

	/**
	 * @return this facet's most general ancestor
	 */
	public Perspective getFacetType() {
		if (getParent() != null)
			return parent.getFacetType();
		else
			return this;
	}

	// /**
	// * @return whether we don't have a parent
	// */
	// public boolean isTopLevel() {
	// return getParent() == null;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getQuery()
	 */
	public Query query() {
		return instantiatedPerspective == null ? parent.instantiatedPerspective.q
				: instantiatedPerspective.q;
	}

	/**
	 * @return is this one of the facetTypes that get a top-level row of bars
	 */
	public boolean isTopLevel() {
		return parent == null;
	}

	// /**
	// * @param i
	// * which child facet to return
	// * @return the i'th child facet ordered by onCount
	// */
	// public Perspective getNthOnValue(int i) {
	// return getNthOnValue(i, true);
	// }

	// Perspective getNthOnValue(int i, boolean isNonNull) {
	// // assert !Util.hasDuplicates(perspective.dataIndexByOn) :
	// // Util.valueOfDeep(perspective.dataIndexByOn);
	// if (isNonNull) {
	// assert instantiatedPerspective.dataIndexByOn[i] != null : this
	// + " " + i + " / " + nChildren() + "\n"
	// + Util.valueOfDeep(instantiatedPerspective.dataIndexByOn);
	// }
	// return instantiatedPerspective.dataIndexByOn[i];
	// }

	void setMaxChildTotalCount(int maxCount) {
		// Util.print("setMaxChildTotalCount " + this + " " + maxCount);
		instantiatedPerspective.maxChildTotalCount = maxCount;
	}

	/**
	 * @return the largest totalCount of any of our child facets
	 */
	public int maxChildTotalCount() {
		assert instantiatedPerspective.maxChildTotalCount > 0 : this;
		return instantiatedPerspective.maxChildTotalCount;
	}

	boolean isAnyRestrictions() {
		return isRestricted() || nUsedChildren() > 0;
	}

	void deleteRestriction(ItemPredicate facet, boolean require) {
		restrictions().delete(facet, require);
		decacheDescriptions();
	}

	void addRestriction(Perspective facet, boolean require) {
		restrictions().add(facet, require);
		decacheDescriptions();
	}

	void clearRestrictions() {
		// Util.print("clearRestrictions " + this);
		instantiatedPerspective.clearRestrictions();
		decacheDescriptions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#nRestrictions()
	 */
	public int nRestrictions() {
		return restrictions().nRestrictions();
	}

	int nRestrictions(boolean require) {
		return restrictions().nRestrictions(require);
	}

	/**
	 * @return does this facet have a filter on one of its children?
	 */
	public boolean isRestricted() {
		return restrictions().isRestricted();
	}

	/**
	 * @param required
	 *            whether to look for a positive or negative filter
	 * @return is this facet mentioned in a filter whose polarity == required?
	 */
	public boolean isRestricted(boolean required) {
		// Util.print("isRestricted " + this + " " + required + " => " +
		// perspective.restrictions.isRestricted(required));
		return restrictions().isRestricted(required);
	}

	// Perspective getRestriction(int n, boolean require) {
	// return restrictions().getRestriction(n, require);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#allRestrictions()
	 */
	public SortedSet<Perspective> allRestrictions() {
		return restrictions().allRestrictions();
	}

	SortedSet<Perspective> restrictions(boolean require) {
		return restrictions().restrictions(require);
	}

	Restrictions restrictions() {
		return instantiatedPerspective.restrictions();
	}

	/**
	 * @param required
	 * @return Is this one of parent's restrictions?
	 */
	public boolean isRestriction(boolean required) {
		if (getParent() != null)
			return getParent().isRestriction(this, required);
		else if (required)
			return isRestricted(required);
		else
			return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#isRestriction(query.ItemPredicate, boolean)
	 */
	public boolean isRestriction(ItemPredicate facet, boolean required) {
		// assert !isRestricted() || getQuery().usesPerspective(this) : this;
		// return (isRestricted() && (facet == this || perspective.restrictions
		// .isRestriction(facet, required)));
		boolean result = restrictions().isRestriction(facet, required);
		// Util.print("p.isRestriction "+facet+" "+required+" => "+result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#isRestriction()
	 */
	public boolean isRestriction() {
		// assert !isRestricted() || getQuery().usesPerspective(this) : this;
		// return (isRestricted() && (facet == this || perspective.restrictions
		// .isRestriction(facet, required)));
		return isRestriction(true) || isRestriction(false);
	}

	// void setRestrictions(Restrictions _restrictions) {
	// perspective.restrictions = _restrictions;
	// decacheDescriptions();
	// }

	void decacheDescriptions() {
		// Util.print(getName() + ".decacheDescriptions");
		// perspective.descriptions = null;
		// instantiatedPerspective.localRestrictionNames = null;
		// instantiatedPerspective.nonLocalRestrictionNames = null;
		// perspective.filterDescription = null;
		if (parent != null)
			parent.decacheDescriptions();
	}

	/**
	 * @return e.g. 'works from 20th century'
	 */
	public Markup facetDescription() {
		return MarkupImplementation.facetDescription(this);
	}

	/**
	 * @return parent.facetDescription() or if no parent
	 *         query().parentDescription()
	 */
	public Markup parentDescription() {
		if (parent == null) {
			return query().parentDescription();
		} else
			return parent.facetDescription();
		// Perspective parent = facet != null ? facet.getParent() : null;
		// return parent == null ? "" : parent.facetDescription().toText(this);
	}

	/**
	 * @param doTag
	 *            prepend with descriptionCategory & descriptionProposition?
	 *            always prepend exclude (as NOT).
	 * @param patterns
	 *            {positive pattern, negative pattern}
	 * @return description of this perspective's restrictions e.g. 'that show
	 *         religion, but don't show animals'
	 */
	Markup getDescription(boolean doTag, String[] patterns) {
		Markup[] descriptions = new Markup[2];
		boolean[] reqtTypes = { true, false };
		for (int type = 0; type < 2; type++) {
			boolean reqtType = reqtTypes[type];
			SortedSet<Perspective> info = getRestrictionFacetInfos(false,
					reqtType);
			if (!info.isEmpty()) {
				descriptions[type] = Query.emptyMarkup();
				MarkupImplementation
						.toEnglish(info, " or ", descriptions[type]);
			}
		}
		Markup result = tagDescription(descriptions, doTag, patterns);
		// Util.print(this + ".getDescription(" + doTag + ") => "
		// + Util.valueOfDeep(result));
		return result;
	}

	/**
	 * @param restrictions
	 *            {positive restrictions, negative restrictions}
	 * @param doTag
	 *            prepend with descriptionCategory & descriptionProposition?
	 *            always prepend exclude (as NOT).
	 * @param patterns
	 *            {positive pattern, negative pattern}
	 * @return description of the restrictions
	 */
	public Markup tagDescription(Markup[] restrictions, boolean doTag,
			String[] patterns) {
		if (patterns == null)
			patterns = getDescriptionPreposition();
		return MarkupImplementation.tagDescription(restrictions, doTag,
				patterns, getDescriptionCategory());
	}

	// public Perspective[] getUsedChildren() {
	// Perspective[] result = {};
	// Iterator it = instantiatedPerspective.q.perspectivesIterator();
	// while (it.hasNext()) {
	// Perspective child = (Perspective) it.next();
	// if (child.parent == this)
	// result = (Perspective[]) Util.push(result, child,
	// Perspective.class);
	// }
	// return result;
	// }

	// public boolean hasUsedChildren() {
	// boolean found = false;
	// Iterator it = perspective.q.perspectivesIterator();
	// while (it.hasNext() && !found) {
	// Perspective child = (Perspective) it.next();
	// found = child.parent == this;
	// }
	// return found;
	// }

	// Clicking in the Summary window:
	// If you click on an unselected tag, it gets selected.
	// with Shift, tags between it and the last selected tag are also
	// selected,
	// otherwise, without Control, other tags are deselected.
	// If you click on a selected tag with Control, it gets unselected,
	// otherwise, without Shift all tags are deselected.
	// Ancestors never change.
	// Descendents of any unselected tag are killed.
	// Clicking in the Detail window:
	// On facet_type
	// with Shift or Control, does nothing
	// otherwise clears all tags.
	// On descendent not represented by a perspective
	// adds missing perspectives, and then behaves as in summary window.
	// Otherwise, behaves as in summary window.
	//     
	// Algorithm:
	// If descendent add intermediates.
	// If facet_type
	// If Shift or Control, exit, otherwise clear.
	// Perspective where facet is a tag will do what it says above.
	//     	
	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#facetDoc(int)
	 */
	public Markup facetDoc(int modifiers) {
		if (getParent() == null)
			// This happens for facet_type labels in SelectedItem frame.
			return null; // facetDoc(this, modifiers);
		else
			return parent.facetDoc(this, modifiers);
	}

	Markup facetDoc(Perspective facet, int modifiers) {
		boolean require = (!Util.isAnyShiftKeyDown(modifiers) && isRestriction(
				facet, false)) ? false : !isExcludeAction(modifiers);
		// Util.print("\nPerspective.facetDoc " + facet + "\nrequire=" + require
		// + " nRestrictions=" + nRestrictions()
		// + " isRestriction(require)=" + isRestricted(require)
		// + "\nisRestriction(facet, require)="
		// + isRestriction(facet, require) + " modifiers=" + modifiers + "
		// isShiftDown="
		// + isShiftDown(modifiers) + " isControlDown=" +
		// isControlDown(modifiers));
		assert modifiers >= 0;
		Markup result = null;
		if (isDisplayOnlyAction(modifiers))
			result = displayFacetDoc(facet);
		else if (!isRestricted()) {
			if (facet.guessOnCount() != (Perspective.isExcludeAction(modifiers) ? query().onCount
					: 0)
					|| !query().isQueryValid()
					// onCount may not be right if query is invalid.
					|| !query().displaysPerspective(facet.parent))
				// Don't encourage clicks that will return 0 results; but do
				// allow
				// clicks on deeply nested SelectedItem facets.
				result = selectFacetDoc(facet, require);
		} else if (isRestriction(facet, require)) {
			if (nRestrictions() == 1 || !Util.isAnyShiftKeyDown(modifiers))
				result = deselectAllFacetsDoc(require);
			else if (Util.isControlDown(modifiers) || !require)
				result = deselectFacetDoc(facet, require);
			// Do nothing on SHIFT
		} else if (Util.isShiftDown(modifiers))
			result = selectInterveningFacetsDoc(facet, require);
		else if (Util.isControlDown(modifiers) || !require)
			result = selectFacetDoc(facet, require);
		else
			result = replaceFacetDoc(facet, require);
		// Util.print("...facetDoc return " + result);
		return result;
	}

	/**
	 * @param modifiers
	 *            mouse gesture modifiers
	 * @return should the gesture add a negated filter?
	 */
	public static boolean isExcludeAction(int modifiers) {
		return (modifiers & EXCLUDE_ACTION) != 0;
	}

	public static boolean isDisplayOnlyAction(int modifiers) {
		// Util.print("idoa " + (modifiers & Util.modifierMask) + " "
		// + InputEvent.ALT_DOWN_MASK + " " + InputEvent.ALT_MASK);
		return (modifiers & Util.modifierMask) == InputEvent.ALT_DOWN_MASK;
	}

	Markup deselectFacetDoc(Perspective facet, boolean require) {
		// Leave other facets alone.
		Markup v = describeFilter();
		v.add(0, "Remove ");
		v.add(1, facet);
		v.add(2, " from ");
		if (!require) {
			v.add(1, Markup.DEFAULT_COLOR_TAG);
			v.add(1, " NOT ");
			v.add(1, filterColors[require ? 0 : 1]);
		}
		return v;
	}

	/**
	 * Our most recent ancestor that is a restriction. Will always be parent
	 * inside summary, but might be more distant for selectedItem.
	 */
	Perspective ancestorRestriction(Perspective facet) {
		if (isRestriction(facet, true))
			return facet;
		else if (getParent() != null) {
			return parent.ancestorRestriction(this);
		} else
			return null;
	}

	/**
	 * @return the most recent ancestor for which there should be a
	 *         PerspectiveViz
	 */
	public Perspective pv() {
		if (isDisplayed())
			return this;
		else
			return parent.pv();
	}

	/**
	 * @return "<facet type> -- <ancestor> ... <ancestor> -- <this name>" used
	 *         for bookmarking
	 */
	public String fullName() {
		if (parent == null)
			return getName();
		else
			return parent.fullName() + " -- " + getName();
	}

	Markup deselectAllFacetsDoc(boolean require) {
		// Perspective ancestorRestriction = parent != null ?
		// parent.ancestorRestriction(this) : null;
		Perspective ancestorRestriction = ancestorRestriction(this);
		if (ancestorRestriction != null)
			return replaceFacetDoc(ancestorRestriction, require);
		else {
			int nRestrictions = getFacetType().nUsedChildren();
			Markup v = Query.emptyMarkup();
			if (nRestrictions > 1) {
				v.add("Remove " + nRestrictions + " filters on ");
			} else {
				v.add("Remove filter on ");
			}
			v.add(getFacetType());
			// Vector v = describeFilter();
			// v.add(0, "Remove ");
			return v;
		}
	}

	int nUsedChildren() {
		int result = 0;
		for (Iterator<Perspective> it = query().displayedPerspectives()
				.iterator(); it.hasNext();) {
			Perspective child = it.next();
			if (child.getParent() == this) {
				result++;
			}
		}
		return result;
	}

	Markup selectInterveningFacetsDoc(Perspective facet, boolean require) {
		if (!isRestricted(require))
			return selectFacetDoc(facet, require);
		Markup v = describeFilter();
		Perspective low;
		Perspective hi;
		Perspective prev = restrictions(require).first();
		if (facet.getID() < prev.getID()) {
			low = facet;
			// Should set hi to facet alphabetically preceding prev.
			// If this equals facet, call selectFacetDoc instead.
			hi = prev;
		} else {
			low = prev;
			hi = facet;
		}
		// v.add(0, low);
		// v.add(1, " through ");
		v.add(2, hi);
		v.add(3, " to ");
		addAddNOT(v, "Add ", require, low, " through ");
		return v;
	}

	void addAddNOT(Markup v, String op, boolean require, Perspective facet,
			String object) {
		if (object != null)
			v.add(0, object);
		v.add(0, facet);
		if (!require) {
			v.add(0, Markup.DEFAULT_COLOR_TAG);
			v.add(0, "NOT ");
			v.add(0, filterColors[1]);
		}
		v.add(0, op);
	}

	Markup selectFacetDoc(Perspective facet, boolean require) {
		// Util.print("selectFacetDoc " + facetName + " " + restrictions.length
		// + " " + (facet != facet_type_id));
		// Leave other facets alone.
		Markup result = null;
		if (isRestriction(facet, !require)) {
			if (nRestrictions() == 1)
				return replaceFacetDoc(facet, require);
			result = describeFilter();
			addAddNOT(result, "with ", require, facet, " in ");
			addAddNOT(result, "Replace ", !require, facet, null);
		} else if (isRestricted()) {
			result = describeFilter();
			// result.add(0, " to ");
			// result.add(0, facet);
			addAddNOT(result, "Add ", require, facet, " to ");
		} else if (facet != this) {
			result = Query.emptyMarkup();
			result.add("Add filter ");
			describeFilter(result, facet, require);
		}
		return result;
	}

	Markup replaceFacetDoc(Perspective facet, boolean require) {
		// Util.print("replaceFacetDoc " + facetName);
		Markup result = describeFilter();
		result.add(0, "Replace ");
		result.add(Markup.NEWLINE_TAG);
		result.add(" with ");
		describeFilter(result, facet, require);
		return result;
	}

	Markup displayFacetDoc(Perspective facet) {
		// Util.print("replaceFacetDoc " + facetName);
		Markup result = new MarkupImplementation();
		result.add("Ensure that ");
		result.add(facet);
		result.add(" is displayed on the Tag Wall");
		return result;
	}

	public void displayAncestors() {
		if (!isDisplayed()) {
			parent.displayAncestors();
			query().insertPerspective(this);
		}
	}

	/**
	 * All restriction changes go through this function.
	 * 
	 * @return Returns true if it changed the query.
	 */
	boolean toggleFacet(Perspective facet, int modifiers) {
		// Util.print("Perspective.toggleFacet " + facet + " " + modifiers + " "
		// + isRestriction(facet, false) + " "
		// + isRestriction(facet, true) + " " + isRestricted() + " "
		// + allRestrictions());

		if (isDisplayOnlyAction(modifiers))
			return true;
		boolean result = true;
		boolean require = isRequireAction(facet, modifiers);
		if (isRestriction(facet, require)) {
			if (Util.isControlDown(modifiers) || isExcludeAction(modifiers)) {
				deselectFacet(facet, require);
			} else if (!Util.isShiftDown(modifiers))
				deselectAllFacets();
			else
				result = false;
		} else if (Util.isShiftDown(modifiers))
			result = selectInterveningFacets(facet, !isExcludeAction(modifiers));
		else {
			if (isRestricted() && !Util.isControlDown(modifiers) && require)
				deselectAllFacets();
			else if (isRestriction(facet, !require)) {
				deselectFacet(facet, !require);
			}
			result = selectFacet(facet, require);
		}
		return result;
	}

	public boolean isRequireAction(Perspective facet, int modifiers) {
		boolean require = !(isExcludeAction(modifiers) || (!Util
				.isAnyShiftKeyDown(modifiers) && isRestriction(facet, false)));
		return require;
	}

	/**
	 * @return Returns true if it changed the query.
	 */
	private boolean selectInterveningFacets(Perspective facet, boolean require) {
		assert !isRestriction(facet, require) : "selectInterveningFacets problem";
		if (isRestricted(require)) {
			int index = facet.getID();
			int index2 = restrictions(require).first().getID();
			int lowIndex = Math.min(index, index2);
			int highIndex = Math.max(index, index2);
			// Util.print("p.selectInterveningFacets " + lowIndex + " "
			// + highIndex);

			for (int i = lowIndex; i <= highIndex; i++) {
				Perspective p = instantiatedPerspective.q.findPerspective(i);
				if (!isRestriction(p, require))
					selectFacet(p, require);
			}
		} else
			selectFacet(facet, require);
		return true;
	}

	private void deselectFacet(Perspective facet, boolean require) {
		// Util.print("deselectFacet " + facet + " " + require);
		deleteRestriction(facet, require);
		instantiatedPerspective.q.removeRestriction(facet);
	}

	void deselectAllFacets() {
		// Util.print("p.deselectAllFacets " + this);
		assert isRestricted() : this;
		instantiatedPerspective.q.clearPerspective(this);
	}

	/**
	 * @return Returns true if it changed the query.
	 */
	boolean selectFacet(Perspective facet, boolean require) {
		// Util.print("p.selectFacet " + this + "." + facet + " " + require +
		// " "
		// + ancestorRestriction(facet));
		assert !isRestriction(facet, require) : facet + " " + this + " "
				+ require + " " + allRestrictions();
		addRestriction(facet, require);
		if (facet.isEffectiveChildren() && require) {
			instantiatedPerspective.q.insertPerspective(facet);
		}
		return true;
	}

	/**
	 * Reset counts in preparation for reading new data (count = 0), or after
	 * removing from query (count = -1).
	 */
	void resetData(int count) {
		instantiatedPerspective.resetData(count);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#restrictData()
	 */
	public boolean restrictData() {
		// Util.print("Perspective.restrictData " + this);
		assert isPrefetched() : this;
		int childCumCount = 0;
		int maxCount = -1;
		// sortDataIndexByIndex();
		for (Iterator<Perspective> it = getChildIterator(); it.hasNext();) {
			Perspective child = it.next();
			int count = child.onCount;
			assert count >= 0 : child + " " + query() + " "
					+ query().displayedPerspectives();

			// Query.restrict calls Query.clear, which will resetData of facet
			// to -1
			// and then restrictData of facet will make totalCount = -1
			// Pre-empt this by clearing here and then setting totalCount
			// getQuery().removeRestriction(facet);
			if (count > maxCount)
				maxCount = count;
			childCumCount += count;
			// facet.totalCount = count;
			child.cumCount = childCumCount;
		}
		clearRestrictions();
		boolean used = childCumCount > 0;
		if (!used)
			query().removeRestriction(this);
		setTotalChildTotalCount(childCumCount);
		// totalCount = onCount;
		// setTotalChildOnCount(cumCount);
		setMaxChildTotalCount(maxCount);
		// if (parent != null)
		// parent.updateChildPercents();
		return used;
	}

	// public String[] getRestrictionNames(boolean isLocalOnly) {
	// String[] result = isLocalOnly ? perspective.localRestrictionNames
	// : perspective.nonLocalRestrictionNames;
	// if (result == null) {
	// Perspective[] info = getRestrictionFacetInfos(isLocalOnly);
	// String[] names = new String[info.length];
	// for (int i = 0; i < names.length; i++) {
	// names[i] = info[i].getName();
	// if (names[i] == null)
	// return null;
	// }
	// result = names;
	// if (isLocalOnly)
	// perspective.localRestrictionNames = names;
	// else
	// perspective.nonLocalRestrictionNames = names;
	// }
	// // Util.print(getName() + ".getRestrictionNames " + isLocalOnly + " => "
	// // + Util.valueOfDeep(result));
	// return result;
	// }

	// int[] getRestrictions(boolean require) {
	// Perspective[] info = getRestrictionFacetInfos(false, require);
	// int[] result = new int[info.length];
	// for (int i = 0; i < result.length; i++)
	// result[i] = info[i].facet_id;
	// return result;
	// }

	// isLocalOnly means
	/**
	 * @param isLocalOnly
	 *            don't return restrictions implied by a restriction on a child
	 *            Perspective. otherwise, return the more specific
	 *            restriction(s).
	 * @param require
	 *            restriction polarity
	 * @return the restricting facets
	 */
	public SortedSet<Perspective> getRestrictionFacetInfos(boolean isLocalOnly,
			boolean require) {
		// Util.print("getRestrictionFacetInfos " + this + " " + require + " "
		// + isLocalOnly + " " + nRestrictions(require));
		// assert isLocalOnly || require : "Excludes don't propagate up, so you
		// can't search for them non-locally!";
		SortedSet<Perspective> result = new TreeSet<Perspective>();
		// int n = nRestrictions(require);
		for (Iterator<Perspective> it = restrictions(require).iterator(); it
				.hasNext();) {
			Perspective child = it.next();
			boolean found = false;
			if (require && query().displaysPerspective(child)) {
				if (isLocalOnly)
					found = true;
				else {
					SortedSet<Perspective> childResult = child
							.getRestrictionFacetInfos(isLocalOnly, require);
					if (childResult.size() > 0) {
						found = true;
						result.addAll(childResult);
					}
				}
			}
			if (!found) {
				assert child != null;
				result.add(child);
			}
		}
		if (!isLocalOnly && !require) {
			for (Iterator<Perspective> it = query().displayedPerspectives()
					.iterator(); it.hasNext();) {
				Perspective child = it.next();
				if (child.getParent() == this) {
					result.addAll(child.getRestrictionFacetInfos(isLocalOnly,
							require));
				}
			}
		}
		// Util.print("getRestrictionFacetInfos return "
		// + Util.valueOfDeep(result));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#percentOn()
	 */
	public double percentOn() {
		int c = getOnCount();
		assert totalCount > 0 : this + " " + totalCount;
		assert c >= 0 : this + " " + getParent() + " " + c + "/" + totalCount
				+ " " + query().isQueryValid();
		assert c <= totalCount : this + " " + c + "/" + totalCount;
		return c / (double) totalCount;
	}

	void incfChildren(int delta) {
		assert query().isEditable();
		instantiatedPerspective.incfChildren(delta);
	}

	// public void sortDataIndexByIndex() {
	// instantiatedPerspective.sortDataIndexByIndex();
	// }

	void addFacet(int index, Perspective facet) {
		// Util.print("addFacet " + this + " " + index + " " + facet);
		ensureInstantiatedPerspective().addFacet(index, facet);
	}

	// /**
	// * sort dataIndexByOn by decreasing onCount
	// */
	// public void sortDataIndexByOn() {
	// instantiatedPerspective.sortDataIndexByOn(this);
	// }

	// boolean isDataIndexByOnComplete() {
	// for (int i = 0; i < nChildren(); i++) {
	// getNthOnValue(i, true); // Will error if result would be null
	// }
	// return true;
	// }

	void addFacetAllowingNulls(Perspective facet) {
		instantiatedPerspective.addFacetAllowingNulls(facet);
	}

	// public boolean isSortedByOn() {
	// return instantiatedPerspective.isSortedByOn;
	// }
	//
	// /**
	// * @param perspectives
	// * facets to sort in decreasing order of onCount
	// */
	// public static void sortByOn(Perspective[] perspectives) {
	// InstantiatedPerspective.sortByOn(perspectives);
	// }

	/**
	 * @param ancestor
	 * @return is ancestor this perspective or an ancestor of this perspective?
	 */
	public boolean hasAncestor(ItemPredicate ancestor) {
		return (ancestor == this)
				|| (parent != null && parent.hasAncestor(ancestor));
	}

	public ChiSq2x2 pValueCounts() {
		return ensureInstantiatedPerspective().pValueCounts();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#pValue()
	 */
	public double pValue() {
		return ensureInstantiatedPerspective().pValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#percentageRatio()
	 */
	public double percentageRatio() {
		return ensureInstantiatedPerspective().percentageRatio();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#chiColorFamily(double)
	 */
	public int chiColorFamily(double p) {
		int result = 0;
		if (// isBigDeal() &&
		ensureInstantiatedPerspective().pValue() <= p) {
			result = ensureInstantiatedPerspective().pValueSign();
		}
		assert result == 0 || (getOnCount() >= 0 && parentOnCount() >= 0) : this
				+ " " + getOnCount() + " " + getOnCount() + " " + onCount;
		return result;
	}

	private static final double SIGMOID_STEEPNESS = 1.0;

	private static final double SIGMOID_MIN = 1.0 / (1.0 + Math.pow(Math.E,
			SIGMOID_STEEPNESS / 2.0));

	private static final double SIGMOID_SCALE = 1.0 - 2.0 * SIGMOID_MIN;

	static double unwarp(double y, double expectedPercent) {
		if (y == 0.0 || y == 1.0)
			return y;
		y = y * SIGMOID_SCALE + SIGMOID_MIN;
		double result = Math.pow(0.5 - Math.log(1.0 / y - 1.0)
				/ SIGMOID_STEEPNESS, 1.0 / warpPower(expectedPercent));
		// if (getName().equals("Genre"))
		// Util.print("unwarp " + warpPower + " y=" + x + " => " + y + " "
		// + result + " inv=" + warp(result));
		return result;
	}

	static double warpPower(double expectedPercent) {
		double warpPower;
		// if (warpPower < 0) {
		if (expectedPercent > 0.0 && expectedPercent < 1.0) {
			warpPower = -Math.log(2) / Math.log(expectedPercent);
		} else {
			warpPower = 1.0;
		}
		// }
		assert warpPower > 0.0 : expectedPercent + " " + warpPower;
		return warpPower;
	}

	// void computeBigDeals() {
	// double expectedPercent = percentOn();
	// positiveBigDeal = unwarp(0.55, expectedPercent);
	// negativeBigDeal = unwarp(0.45, expectedPercent);
	// // Util.print("Big deals " + this + " " + negativeBigDeal + "-" +
	// // positiveBigDeal);
	// }

	// static double warp(double observedPercent, double expectedPercent) {
	// if (observedPercent == 0.0 || observedPercent == 1.0)
	// return observedPercent;
	// double powWarped = Math
	// .pow(observedPercent, warpPower(expectedPercent));
	// double result = 1.0 / (1.0 + Math.pow(Math.E, SIGMOID_STEEPNESS
	// * (0.5 - powWarped)));
	// result = (result - SIGMOID_MIN) / SIGMOID_SCALE;
	// // if (getName().equals("Genre"))
	// // Util.print(warpPower + " " + expectedPercentOn() + " percent="
	// // + percent + " " + powWarped + " " + result);
	// return result;
	// }

	// private double positiveBigDeal;
	// private double negativeBigDeal;
	//
	// boolean isBigDeal() {
	// // ItemPredicate expectationParent = parent != null ? parent
	// // : (ItemPredicate) getQuery();
	// // return Math.abs(warp(percentOn(), expectationParent.percentOn()) -
	// // 0.5) > 0.1;
	// boolean result = false;
	// if (query().isQueryValid() && getOnCount() >= 0 && totalCount > 0) {
	// // Deeply nested facets will have onCount < 0
	// if (parent == null)
	// result = query().isBigDeal(percentOn());
	// else
	// result = parent.isBigDeal(percentOn());
	// }
	// return result;
	// }
	//
	// boolean isBigDeal(double obervedPercent) {
	// return obervedPercent > positiveBigDeal
	// || obervedPercent < negativeBigDeal;
	// }

	// public int chiColorFamily(double p) {
	// int result = 0;
	// int[][] table = chiSqTable();
	// if (table != null && ChiSqr.significant(table, p)) {
	// int facetTotal = table[0][0] + table[1][0];
	// int on = table[0][0] + table[0][1];
	// int total = on + table[1][0] + table[1][1];
	// double expected = facetTotal * (on / (double) total);
	// assert expected >= 0;
	// result = (expected > table[0][0]) ? -1 : 1;
	// // Util.print(facet + " " +
	// // Util.valueOfDeep(table) +
	// // " "
	// // + ChiSqr.pValue(table) + " " + facetTotal + " " +
	// // expected + " " + result);
	// }
	// return result;
	// }
	//
	// public int[][] chiSqTable() {
	// Query q = getQuery();
	// // if (facet.parent == null) {
	// // Util.print("chiColorFamily " + facet + " " + q.isQueryValid() + " " +
	// // q.onCount + " " + facet.getOnCount());
	// // }
	// if (q.isQueryValid() && q.isRestricted()) {
	// int total;
	// int on;
	// if (parent != null) {
	// total = parent.totalCount;
	// on = parent.onCount;
	// } else {
	// total = q.totalCount;
	// on = q.onCount;
	// }
	// if (total > on && onCount >= 0) {
	// // Deeply nested facets have on=-1;
	// // int facetOn = getOnCount();
	// // if (onCount >= 0) {
	// // int facetTotal = getTotalCount();
	// // assert facetTotal >= 0;
	//
	// int otherOn = on - onCount;
	// int facetOff = totalCount - onCount;
	// int otherOff = total - on - facetOff;
	// assert otherOn >= 0 : this + " " + parent + " " + total
	// + " " + on + " " + onCount + " " + totalCount + " "
	// + otherOn;
	// assert q.findPerspective(facet_id) == this : this;
	// assert otherOff >= 0 : this + " " + total + " " + on + " "
	// + onCount + " " + totalCount + " " + otherOff;
	// assert facetOff >= 0 : this + " " + facetOff;
	// int[][] table = { { onCount, otherOn },
	// { facetOff, otherOff } };
	// return table;
	// // }
	// }
	// }
	// return null;
	// }

	// /**
	// * @return is it sensible for Tetrad to conclude that another Perspective
	// * causes this one?
	// */
	// public boolean isCausable() {
	// return query().isCausable(this);
	// }

	/**
	 * @return does this Perspective have a natural ordering (like Date or
	 *         Rating)?
	 */
	public boolean isOrdered() {
		return query().isOrdered(this);
	}

	public int compareTo(ItemPredicate arg0) {
		// return indexComparator.compare(this, arg0);
		if (arg0 instanceof Perspective)
			return getID() - ((Perspective) arg0).getID();
		else
			return 1;
	}

	// public int compareTo(ItemPredicate caused) {
	// return (caused instanceof Perspective) ? compareTo((Object) caused) : 1;
	// }

	@Override
	public boolean equals(Object arg0) {
		return arg0 instanceof Perspective
				&& compareTo((ItemPredicate) arg0) == 0;
	}

	@Override
	public int hashCode() {
		return getID();
	}

	/**
	 * @return an array of our children sorted from highest onCount to lowest.
	 */
	public Perspective[] getChildren() {
		return instantiatedPerspective.getChildren();
	}

	void updateTopTags(TopTags top, int parentTotalCount, int parentOnCount) {
		assert query().isQueryValid();
		updateTopTags(top, parentTotalCount, parentOnCount, onCount);
		if (onCount < getTotalCount() && nChildren() > 0 && isPrefetched()
				&& onCount > 0 && isDisplayed()) {
			for (Iterator<Perspective> it = getChildIterator(); it.hasNext();) {
				Perspective child = it.next();
				assert child != Perspective.this;
				child.updateTopTags(top, getTotalCount(), onCount);
			}
		}
	}

	public void updateTopTags(TopTags top, int parentTotalCount,
			int parentOnCount, int _onCount) {
		// Util.print("updateTopTags " + this);
		try {
			int total = getTotalCount();
			if (true || _onCount < total) {
				if (total < parentTotalCount && !isRestriction()) {
					if (instantiatedPerspective != null) {
						// assert instantiatedPerspective.checkTable(
						// parentTotalCount, parentOnCount, total,
						// _onCount);
						// Util.print(instantiatedPerspective.checkTableMsg(
						// parentTotalCount, parentOnCount, total, onCount));
						ChiSq2x2 chiSq = getChiSq(parentTotalCount,
								parentOnCount, total, _onCount);
						boolean siblingSelected = parent != null
								&& parent.isRestricted();
						if (chiSq.sign() > 0 || !siblingSelected) {
							double myCramersPhi = chiSq.myCramersPhi();
							double relativePhi = myCramersPhi
									/ Math.sqrt(parentTotalCount());
							top.maybeAdd(chiSq, relativePhi);
						}
					}
				}
			}
		} catch (AssertionError e) {
			// Do our best even if query is invalid, but ignore errors this
			// causes
			if (query().isQueryValid())
				throw (e);
		}
	}

	ChiSq2x2 getChiSq(int parentTotalCount, int parentOnCount, int total,
			int _onCount) {
		assert query().isQueryValid();
		assert _onCount <= total : _onCount + " " + total;
		assert parentOnCount <= parentTotalCount : parentOnCount + " "
				+ parentTotalCount;
		ChiSq2x2 chiSq = ChiSq2x2.getInstance(this, parentTotalCount,
				parentOnCount, total, _onCount, query());
		return chiSq;
	}

	public static class TopTags {
		final int n;
		private double topThreshold = Double.POSITIVE_INFINITY;
		private double bottomThreshold = Double.NEGATIVE_INFINITY;

		// public final Map top = new Hashtable();
		// public final Map bottom = new Hashtable();
		// private Object topThresholdFacet;
		// private Object bottomThresholdFacet;

		public static class TagRelevance {
			public final ChiSq2x2 tag;
			public final double relevance;

			public TagRelevance(ChiSq2x2 o, double _relevance) {
				tag = o;
				relevance = _relevance;
				// assert tag instanceof ChiSq2x2;
			}

			public double relevanceScore() {
				return Util.sgn(relevance) * 100 * Math.pow(Math.abs(relevance
				/* / maxRelevance(facet) */), 0.25);
			}

			@Override
			public String toString() {
				return "<TagRelevance " + relevance + " " + tag.object + ">";
			}
		}

		private static final Comparator<TagRelevance> entryComparator = new EntryComparator();
		public final SortedSet<TagRelevance> top = new TreeSet<TagRelevance>(
				entryComparator);
		public final SortedSet<TagRelevance> bottom = new TreeSet<TagRelevance>(
				entryComparator);

		public TopTags(int _n) {
			assert _n > 0;
			n = _n;
		}

		final static class EntryComparator extends
				DoubleValueComparator<TagRelevance> {
			@Override
			public double value(TagRelevance data) {
				return data.relevance;
			}
		}

		public Iterator<TagRelevance> topIterator() {
			return top.iterator();
		}

		/**
		 * @param o
		 *            the object having the score
		 * @param score
		 *            any monotonically increasing relevance function, where
		 *            positive influences are greater than zero, and vice versa.
		 */
		void maybeAdd(ChiSq2x2 o, double score) {
			if (score > 0) {
				if (top.size() < n || score > topThreshold) {
					if (top.size() == n) {
						// assert topx.containsKey(topThresholdFacet);
						top.remove(top.last());
						assert top.size() < n : topThreshold + " " + top;
					}
					top.add(new TagRelevance(o, score));
					topThreshold = top.last().relevance;
					// updateTopThreshold();
					// assert top.size() < n || topThresholdFacet != null :
					// top.size()
					// + " " + n + " " + this;
					assert top.size() <= n : topThreshold + " " + top;
				}
				assert top.size() <= n : top;
			} else {
				if (bottom.size() < n || score < bottomThreshold) {
					if (bottom.size() == n) {
						// assert bottom.containsKey(bottomThresholdFacet);
						bottom.remove(bottom.first());
						assert bottom.size() < n : bottomThreshold + " "
								+ bottom;
					}
					bottom.add(new TagRelevance(o, score));
					bottomThreshold = bottom.first().relevance;
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			String heading = ChiSq2x2.statisticsHeading();
			topTagsStringInternal(heading + "\nTop ", top, buf);
			buf.append("\n");
			topTagsStringInternal("Bottom ", bottom, buf);
			return buf.toString();
		}

		private void topTagsStringInternal(String which,
				Set<TagRelevance> whichSet, StringBuffer buf) {
			if (whichSet.size() > 0) {
				buf.append(which).append(whichSet.size()).append(" tags:\n");
				for (Iterator<TagRelevance> it = whichSet.iterator(); it
						.hasNext();) {
					TagRelevance tagRelevance = it.next();
					ChiSq2x2 pvalue = tagRelevance.tag;
					pvalue.statisticsLine(buf).append("\n");
				}
			}
		}
	}

	final class InstantiatedPerspective {

		String name;

		/**
		 * The children will have consecutive facet_id's, starting one after
		 * this. Should only change if isEditable
		 */
		int children_offset = -1;

		/**
		 * Whether children's names are ordered by facet_id. The interface will
		 * only zoom by prefixes if so.
		 */
		boolean isAlphabetic = false;

		/**
		 * Should only change if isEditable
		 */
		int nChildren = 0;

		/**
		 * Used to determine whether ChiSqr table is up to date
		 */
		private int updateIndex = 0;

		private ChiSq2x2 pValueCounts;

		private ChiSq2x2 medianPvalue;

		boolean isPrefetched = false;

		/**
		 * Maps from a prefix string to a map from the next letter to the last
		 * child with that letter.
		 */
		Map<CollationKey, Map<CollationKey, Perspective[]>> lettersOffsets = new HashMap<CollationKey, Map<CollationKey, Perspective[]>>();

		Perspective[] dataIndex;

		/**
		 * Query is filtered to return only results with one of these tags.
		 * Ordered FIFO, to support selecting with SHIFT.
		 */
		Restrictions restrictions;

		/**
		 * object, meta, or content. Used to generate query description.
		 */
		final String descriptionCategory;

		/**
		 * Pattern into which facet name is substituted for '~'. Default is
		 * implicitly [descriptionPreposition ~, NOT descriptionPreposition ~].
		 * Used to generate query description.
		 */
		final String[] descriptionPreposition;

		final Query q;

		// Vector descriptions = null;

		// Vector filterDescription = null;

		// /**
		// * -1 not sorted 0 sorted by index 1 sorted by onCount
		// */
		// private boolean isSortedByOn;

		// String[] localRestrictionNames = null; // cache this
		//
		// String[] nonLocalRestrictionNames = null;

		/**
		 * The sum of the totalCounts for our children. Can be less than
		 * totalCount if some items aren't further categorized, or greater if
		 * some items are in multiple child categories.
		 */
		int totalChildTotalCount = -1;

		// int totalChildOnCount = -1;

		int maxChildTotalCount = -1;

		// int maxChildPercentOn = -1;g " + this);

		InstantiatedPerspective(Perspective p) {
			q = p.query();
			descriptionCategory = p.getParent().getDescriptionCategory();
			descriptionPreposition = p.getParent().getDescriptionPreposition();
			// if (nChildren > 0) {
			// dataIndex = new Perspective[p.nChildren()];
			// dataIndexByOn = new Perspective[p.nChildren()];
			// }
			// Util.print("instantiating " + this);
		}

		Perspective[] getChildren() {
			Perspective[] result = new Perspective[nChildren];
			System.arraycopy(dataIndex, 0, result, 0, nChildren);
			// Comparator comparator = q.isRestricted() ? onCountComparator
			// : totalCountComparator;
			// Arrays.sort(result, comparator);
			return result;
		}

		InstantiatedPerspective(Perspective p, int n_children,
				int child_offset, String _name) {
			assert p != null;
			assert p.getParent() != null : p;
			// InstantiatedPerspective _parent =
			// p.getParent().instantiatedPerspective;
			q = p.query();
			descriptionCategory = p.getParent().getDescriptionCategory();
			descriptionPreposition = p.getParent().getDescriptionPreposition();
			name = _name;
			setNchildren(n_children, child_offset);
			// Util.print("instantiating " + this);
		}

		InstantiatedPerspective(int n_children, int child_offset, String _name,
				String _descriptionCategory, String _descriptionPreposition,
				Query _q) {
			// Util.print("instantiating " + this);
			q = _q;
			name = _name;
			descriptionCategory = _descriptionCategory;
			setNchildren(n_children, child_offset);
			descriptionPreposition = parseDescriptionPreposition(_descriptionPreposition);
		}

		/**
		 * @param minCount
		 * @param maxCount
		 * @return Iterator over children c where: c.cumCount >= minCount &&
		 *         c.cumCountExclusive <= maxCount
		 */
		Iterator<Perspective> cumCountChildIterator(int minCount, int maxCount) {
			if (minCount < 0)
				minCount = 0;
			if (maxCount > totalChildTotalCount)
				maxCount = totalChildTotalCount;
			if (minCount <= 1 && maxCount >= totalChildTotalCount - 1)
				// optimize common case
				return getChildIterator();
			synchronized (dummyCumCount) {
				dummyCumCount.cumCount = minCount;
				int minWhichChild = Arrays.binarySearch(dataIndex,
						dummyCumCount, cumCountInclusiveComparator);
				minWhichChild = childIndexFromBinarySearch(minWhichChild, false);
				Perspective minChild = getNthChild(minWhichChild);
				while (minChild.getTotalCount() == 0) {
					// This can happen when restrictedData
					minChild = getNthChild(--minWhichChild);
				}
				// Util.print(minCount + " " + minWhichChild + " "
				// + childIndexFromBinarySearch(minWhichChild, false));
				dummyCumCount.cumCount = maxCount;
				int maxWhichChild = Arrays.binarySearch(dataIndex,
						dummyCumCount, cumCountExclusiveComparator);
				maxWhichChild = childIndexFromBinarySearch(maxWhichChild, true);
				Perspective maxChild = getNthChild(maxWhichChild);
				while (maxChild.getTotalCount() == 0) {
					maxChild = getNthChild(++maxWhichChild);
				}
				// Util.print("cumCountChildIterator " + minCount + "-" +
				// maxCount
				// + "/" + getTotalChildTotalCount() + " " +
				// minChild.toStringWithCumCount()
				// + " -- " + maxChild.toStringWithCumCount());
				assert minChild.cumCountInclusive() >= minCount
						&& minChild.cumCountExclusive() <= minCount
						&& maxChild.cumCountInclusive() >= maxCount
						&& maxChild.cumCountExclusive() <= maxCount;
				return getChildIterator(minChild, maxChild);
			}
		}

		private int childIndexFromBinarySearch(int index, boolean exclusive) {
			// Util.print("cifbs " + index);
			if (index < 0) {
				if (exclusive)
					index = -index - 2;
				else
					index = -index - 1;
			}
			// Util.print(" => " + index);
			return index;
		}

		// /**
		// * @param prefix
		// * @param letter
		// * @param redraw
		// * callback in the case of uncached names
		// * @return first (alphabetically) Perspective starting with prefix
		// * followed by letter, or any other Perspective if none exist,
		// * or null if we don't know due to uncached facet names.
		// */
		// Perspective firstWithLetter(String prefix, char letter,
		// PerspectiveObserver redraw) {
		//
		// synchronized (dummyCumCount) {
		// redrawer = redraw;
		// // if parent isn't set, barfs during instantiation
		// dummyCumCount.parent = Perspective.this;
		// dummyCumCount.setName(prefix + letter);
		// int whichChild = childIndexFromBinarySearch(Arrays
		// .binarySearch(dataIndex, dummyCumCount,
		// facetNameComparator), false);
		// // if (whichChild > 0)
		// // whichChild--;
		// Perspective child = getNthChild(whichChild);
		// if (child.getNameIfPossible() == null)
		// child = null;
		// // Util.print("firstWithLetter '"
		// // + (prefix + letter)
		// // + "' "
		// // + Arrays.binarySearch(dataIndex, dummyCumCount,
		// // facetNameComparator) + " " + whichChild + " "
		// // + child);
		// return child;
		// }
		// }
		//
		// /**
		// * @param prefix
		// * @param letter
		// * @param redraw
		// * callback in the case of uncached names
		// * @return last (alphabetically) Perspective starting with prefix
		// * followed by letter, or any other Perspective if none exist,
		// * or null if we don't know due to uncached facet names.
		// */
		// Perspective lastWithLetter(String prefix, char letter,
		// PerspectiveObserver redraw) {
		//
		// synchronized (dummyCumCount) {
		// redrawer = redraw;
		// // if parent isn't set, barfs during instantiation
		// dummyCumCount.parent = Perspective.this;
		// char nextLetter = (char) (letter + 1);
		// dummyCumCount.setName(prefix + nextLetter);
		// int whichChild = childIndexFromBinarySearch(Arrays
		// .binarySearch(dataIndex, dummyCumCount,
		// facetNameComparator), false);
		// if (whichChild > 0)
		// whichChild--;
		// Perspective child = getNthChild(whichChild);
		// if (child.getNameIfPossible() == null)
		// child = null;
		// // Util.print("lastWithLetter '"
		// // + (prefix + letter)
		// // + "' "
		// // + Arrays.binarySearch(dataIndex, dummyCumCount,
		// // facetNameComparator) + " " + whichChild + " "
		// // + child);
		// return child;
		// }
		// }

		String[] parseDescriptionPreposition(String descriptionPrepositionString) {
			// Util.print("instantiate " + name + " " + nChildren);
			String[] patterns = Util
					.splitSemicolon(descriptionPrepositionString);
			if (patterns.length == 1)
				patterns = (String[]) Util.endPush(patterns, " NOT "
						+ patterns[0], String.class);
			return patterns;
		}

		int getNchildren() {
			return nChildren;
		}

		void setNchildren(int n, int child_offset) {
			// Util.print("setNchildren " + this + " " + n + " " +
			// child_offset);
			// assert children_offset < 0 || children_offset == child_offset
			// || query().isEditable();
			if (n != nChildren) {
				// assert child_offset > 0 : child_offset;
				// Util.print("setNchildren " + this + " " + n + " "
				// + child_offset);
				nChildren = n;
				dataIndex = new Perspective[nChildren];
				// dataIndexByOn = new Perspective[nChildren];
			}
			if (child_offset > 0) {
				assert children_offset < 0 || children_offset == child_offset;
				children_offset = child_offset;
			}
		}

		/**
		 * Sets field medianPvalue.
		 * 
		 * The two distributions to be compared are the on and off items. They
		 * are divided into above and below the unconditional median, allocating
		 * items with the median value in the same proportion they occur in the
		 * unconditional case. *
		 * 
		 * row0 is the number on, and row1 is the number off. col0 is the total
		 * greater than the median, and col1 is the total less than the median.
		 */
		private void computeMedianTestPvalue() {
			medianPvalue = ChiSq2x2.getInstance(Perspective.this);
			int totalChildOnCount = getTotalChildOnCount();
			if (totalChildOnCount < totalChildTotalCount
					&& totalChildOnCount > 0) {
				double median = median(false);
				int medianIndex = (int) median;
				// Util.print(median + " " + dataIndex[medianIndex]);
				int greaterThanMedianChildOnCount = 0;
				int greaterThanMedianChildTotalCount = 0;
				for (int i = medianIndex + 1; i < nChildren; i++) {
					Perspective child = dataIndex[i];
					int childOnCount = child.getOnCount();
					if (childOnCount < 0)
						// We may have prefetched, but haven't gotten onCounts
						// yet
						return;
					greaterThanMedianChildOnCount += childOnCount;
					greaterThanMedianChildTotalCount += child.getTotalCount();
					// Util.print(i + " " + child + " " + childOnCount);
				}
				Perspective medianChild = dataIndex[medianIndex];
				greaterThanMedianChildOnCount += medianChild.getOnCount()
						* (1 - (median - medianIndex));
				greaterThanMedianChildTotalCount += medianChild.getTotalCount()
						* (1 - (median - medianIndex));

				medianPvalue = getChiSq(totalChildTotalCount,
						totalChildOnCount, greaterThanMedianChildTotalCount,
						greaterThanMedianChildOnCount);
				// int[][] table = {
				// { totalChildOnCount - greaterThanMedianChildOnCount,
				// greaterThanMedianChildOnCount },
				// {
				//
				//
				// totalChildTotalCount - totalChildOnCount
				// - greaterThanMedianChildTotalCount
				// + greaterThanMedianChildOnCount,
				// greaterThanMedianChildTotalCount
				// - greaterThanMedianChildOnCount } };
				// Util.print("medianTest " + this + " " + medianTable + " "
				// + Util.valueOfDeep(table));
			}
		}

		double medianPvalue() {
			computeMedianTestPvalue();
			return medianPvalue.pvalue();
		}

		int medianPvalueSign() {
			computeMedianTestPvalue();
			return medianPvalue.sign();
		}

		/**
		 * Sets field pValue
		 */
		ChiSq2x2 pValueCounts() {
			// if (chiSqTable == null)
			// chiSqTable = new ChiSq2x2();
			if (updateIndex != q.updateIndex) {
				pValueCounts = ChiSq2x2.getInstance(Perspective.this);
				if (q.isQueryValid() && q.isRestricted()) {
					int parentTotalCount = parentTotalCount();
					int myTotalCount = getTotalCount();
					int parentOnCount = parentOnCount();
					assert parentTotalCount >= myTotalCount : this
							+ ".totalCount(" + myTotalCount + ") > " + parent
							+ " .totalCount(" + parentTotalCount + ") "
							+ q.isQueryValid();
					if (parentTotalCount > parentOnCount
							&& parentTotalCount > myTotalCount
							&& parentOnCount > 0 && myTotalCount > 0
							&& getOnCount() >= 0) {
						// Deeply nested facets have on=-1;
						// assert checkTable(parentTotalCount, parentOnCount,
						// myTotalCount, onCount);
						try {
							pValueCounts = getChiSq(parentTotalCount,
									parentOnCount, myTotalCount, getOnCount());
							// if ("no date recorded on caption card"
							// .equals(getNameIfPossible()))
							// Util.print(this + " pvalue = "
							// + pValue + " "
							// + parentTotalCount + " "
							// + parentOnCount + " " + totalCount + " " +
							// onCount);
							updateIndex = q.updateIndex;
						} catch (OutOfRangeException e) {
							// Keep going even if there are problems in
							// ChiSq2x2.ChiSq2x2
							System.err.println(this);
							e.printStackTrace();
						}
					}
				}
			}
			return pValueCounts;
		}

		double pValue() {
			return pValueCounts().pvalue();
		}

		int pValueSign() {
			return pValueCounts().sign();
		}

		double percentageRatio() {
			return pValueCounts().percentageRatio();
		}

		/**
		 * @see Perspective#median
		 */
		double median(boolean isConditional) {
			int medianCount = isConditional ? getTotalChildOnCount()
					: totalChildTotalCount;
			if (medianCount > 0) {
				medianCount /= 2;
				int cumOnCount = 0;
				for (int i = 0; i < nChildren; i++) {
					Perspective child = dataIndex[i];
					int childCount = isConditional ? child.getOnCount() : child
							.getTotalCount();
					cumOnCount += childCount;
					if (cumOnCount > medianCount) {
						double childFraction = 1.0 - (cumOnCount - medianCount)
								/ (double) childCount;
						assert !Double.isNaN(childFraction)
								&& childFraction <= 1 && childFraction >= 0 : child
								+ " " + medianCount + " " + childCount;
						return i + childFraction;
					}
				}
				assert false : this + " " + isConditional + " " + medianCount
						+ " " + cumOnCount;
			}
			return -1.0;
		}

		void addFacetAllowingNulls(Perspective facet) {
			// Util.print("addFacet " + this + " " + index + " " + facet);
			assert facet != null : this;
			int index = facet.getID() - childrenOffset() - 1;
			assert dataIndex.length > index : this + " " + index + " " + facet
					+ " " + childrenOffset();
			// sortDataIndexByIndexAllowingNulls();
			// isSortedByOn = false;
			assert dataIndex[index] == facet
					|| !Util.isMember(dataIndex, facet) : this + " " + index
					+ " " + facet + "\n" + Util.valueOfDeep(dataIndex);
			dataIndex[index] = facet;
		}

		// public void sortDataIndexByIndex() {
		// int prevIsDataSorted = sortOrder;
		// if (prevIsDataSorted != Perspective.SORTED_BY_INDEX) {
		// synchronized (q.childIndexesBusy) {
		// Arrays.sort(dataIndexByOn, Perspective.indexComparator);
		// sortOrder = Perspective.SORTED_BY_INDEX;
		// }
		// }
		// assert isSortedByIndex(dataIndexByOn) : prevIsDataSorted + " " + this
		// + Util.valueOfDeep(dataIndexByOn);
		// }

		void incfChildren(int delta) {
			assert query().isEditable();
			nChildren += delta;
			// Util.print("incfChildren " + delta + " is nuking " + this
			// + "'s dataIndexByOn:\n" + Util.valueOfDeep(dataIndexByOn));
			dataIndex = new Perspective[nChildren];
			// dataIndexByOn = new Perspective[nChildren];
			// // all children better be renamed
			// isSortedByOn = false;
		}

		void addFacet(int index, Perspective facet) {
			// Util.print("addFacet " + this + " " + index + " " + facet);
			assert facet != null : this;
			assert index < nChildren : facet + " " + index + " " + nChildren;
			assert dataIndex != null : facet + " " + nChildren;
			assert dataIndex.length == nChildren : facet + " "
					+ dataIndex.length + " " + nChildren;
			// sortDataIndexByIndex();
			// isSortedByOn = false;

			// This is too slow
			// assert dataIndex[index] == facet || !Util.isMember(dataIndex,
			// facet)
			// : this
			// + " "
			// + index
			// + " "
			// + facet
			// + "\n"
			// + Util.valueOfDeep(dataIndex);
			dataIndex[index] = facet;
		}

		/**
		 * @param n
		 * @return nth child facet in alphabetical (or sort) order
		 */
		public Perspective getNthChild(int n) {
			assert n >= 0 : n;
			assert n < nChildren : this + " " + n + "/" + nChildren;

			Perspective result = q.findPerspective(children_offset + n + 1);
			assert result != null : this + " " + n + " " + nChildren;
			return result;
		}

		Iterator<Perspective> getChildIterator() {
			assert children_offset > 0 : this + " has no children! "
					+ nChildren();
			return q.getFacetIterator(children_offset + 1, nChildren);
		}

		Iterator<Perspective> getChildIterator(Perspective min, Perspective max) {
			return q.getFacetIterator(min.facet_id, max.facet_id - min.facet_id
					+ 1);
		}

		void resetData(int count) {
			// Util.print("Perspective.resetData " + this + " " + count);
			assert count <= 0;
			assert isPrefetched : this;
			// if (isPrefetched())
			for (Iterator<Perspective> it = getChildIterator(); it.hasNext();) {
				Perspective child = it.next();
				child.onCount = count;
			}

			// When removing a PV, set children's onCount to -1, but our onCount
			// should be 0 (unless parent sets it to -1). Therefore, must clear
			// children before parents.
			onCount = 0;
		}

		// void sortDataIndexByOn(Perspective myPerspective) {
		// // Util.print("sortDataIndexByOn " + this + " " + q.isRestricted() +
		// " "
		// // + isSortedByOn + " " + myPerspective.getOnCount() + " "
		// // + dataIndex[0].onCount);
		// if (!isSortedByOn && myPerspective.getOnCount() >= 0) {
		// assert q.usesPerspective(myPerspective) : myPerspective;
		// // synchronized (q.childIndexesBusy) {
		// // Util.printDeep(dataIndexByOn);
		// if (dataIndexByOn[0] == null) {
		// System.arraycopy(dataIndex, 0, dataIndexByOn, 0, nChildren);
		// }
		// sortByOn(dataIndexByOn);
		// isSortedByOn = true;
		// assert isSortedByOn(dataIndexByOn) : this + " " + isSortedByOn
		// + " " + myPerspective.getOnCount() + " "
		// + myPerspective.getQuery().isRestricted();
		// // }
		// } else {
		// assert isSortedByOn(dataIndexByOn) : this + " " + isSortedByOn
		// + " " + myPerspective.getOnCount() + " "
		// + myPerspective.getQuery().isRestricted();
		// }
		// // Util.print(" sortDataIndexByOn return");
		// }
		//
		// boolean isSortedByOn(Perspective[] perspectives) {
		// int prevCount = Integer.MAX_VALUE;
		// for (int i = 0; i < nChildren; i++) {
		// Perspective child = perspectives[i];
		// assert child != null : this + " " + nChildren + " "
		// + Util.valueOfDeep(perspectives);
		// int onCount = child.getOnCount();
		// if (onCount > prevCount) {
		// Util.err(" NOT SORTED!!! " + this + "." + child + " " + onCount
		// + " " + child.totalCount + " " + perspectives[i - 1]
		// + " " + prevCount + " "
		// + perspectives[i - 1].totalCount);
		// return false;
		// }
		// prevCount = onCount;
		// }
		// return true;
		// } // boolean isSortedByIndex(Perspective[] perspectives) {

		// assert dataIndexByOn.length == nChildren : nChildren + " " +
		// dataIndexByOn.length;
		// for (int i = 0; i < nChildren; i++) {
		// Perspective child = dataIndexByOn[i];
		// assert child == null || child.getIndex() == i : this + "." + child
		// + " " + child.getIndex() + " " + i + " " + children_offset
		// + "\n" + Util.valueOfDeep(perspectives);
		// }
		// return true;
		// }

		// static void sortByOn(Perspective[] perspectives) {
		// assert !Util.hasDuplicates(perspectives) : Util
		// .valueOfDeep(perspectives);
		// if (perspectives.length > 0) {
		// Query query = perspectives[0].query();
		// Comparator comparator = query.isRestricted() ?
		// InstantiatedPerspective.onCountComparator
		// : InstantiatedPerspective.totalCountComparator;
		//
		// assert Util.nOccurrences(perspectives, null) == 0 : Util
		// .nOccurrences(perspectives, null)
		// + " / " + perspectives.length;
		// Arrays.sort(perspectives, comparator);
		// }
		// }

		int getTotalChildOnCount() {
			int result = 0;
			if (q.isRestricted()) {
				for (int i = 0; i < nChildren; i++) {
					result += dataIndex[i].onCount;
				}
			} else
				result = totalChildTotalCount;
			return result;
		}

		Restrictions restrictions() {
			if (restrictions == null)
				restrictions = new Restrictions();
			return restrictions;
		}

		void clearRestrictions() {
			// Util.print("clearRestrictions " + this + " " + restrictions);
			restrictions = null;
		}

		@Override
		public String toString() {
			return "<InstantiatedPerspective " + name + ">";
		}

	}

	/**
	 * Combine sequential Perspectives into MexPerspectives. If all children
	 * included, substitute parent.
	 * 
	 * @param sortedFacetCollection
	 *            a SortedSet or sorted List of Perspectives
	 * @param onlyComparable
	 *            only coalesce sibling, ordered Perspectives
	 * @return SortedSet of Perspectives and MexPerspectives
	 */
	public static SortedSet<ItemPredicate> coalesce(
			Collection<? extends ItemPredicate> sortedFacetCollection,
			boolean onlyComparable) {
		SortedSet<ItemPredicate> combos = new TreeSet<ItemPredicate>();
		Perspective end = null;
		Perspective start = null;
		for (Iterator<? extends ItemPredicate> iterator = sortedFacetCollection
				.iterator(); iterator.hasNext();) {
			ItemPredicate ip = iterator.next();
			Perspective startP = (Perspective) ((ip instanceof Perspective) ? ip
					: ((MexPerspectives) ip).facets.first());
			Perspective endP = (Perspective) ((ip instanceof Perspective) ? ip
					: ((MexPerspectives) ip).facets.last());
			if (end == null || startP.getID() != end.getID() + 1
					|| startP.getParent() != end.getParent()) {
				coalesceInternal(combos, start, end, onlyComparable);
				start = startP;
			}
			end = endP;
		}
		coalesceInternal(combos, start, end, onlyComparable);
		return combos;
	}

	private static void coalesceInternal(SortedSet<ItemPredicate> combos,
			Perspective start, Perspective end, boolean onlyComparable) {
		// Util.print("rfi " + start + " " + end);
		if (start == end) {
			if (start != null)
				combos.add(end);
		} else if (end.getID() - start.getID() + 1 == start.getParent()
				.nChildren()) {
			combos.add(start.getParent());
		} else if (onlyComparable && !start.getParent().isOrdered()) {
			for (Iterator<Perspective> it = start.getParent().getChildIterator(
					start, end); it.hasNext();) {
				Perspective p = it.next();
				combos.add(p);
			}
		} else {
			combos.add(new MexPerspectives(start, end));
		}
	}
}

final class Restrictions {
	SortedSet<Perspective> require = new TreeSet<Perspective>();

	SortedSet<Perspective> exclude = new TreeSet<Perspective>();

	void delete(ItemPredicate facet, boolean required) {
		// Util.print("Perspective.delete " + this + "." + facet + " " +
		// required);
		assert isRestriction(facet, required) : facet + " " + required;
		if (required)
			require.remove(facet);
		else
			exclude.remove(facet);
	}

	void add(Perspective facet, boolean required) {
		// Util.print("add " + facet);
		assert facet != null;
		assert !isRestriction(facet, required);
		if (required)
			require.add(facet);
		else
			exclude.add(facet);
	}

	// void append(Restrictions r) {
	// require = (Perspective[]) Util.append(require, r.require,
	// Perspective.class);
	// exclude = (Perspective[]) Util.append(exclude, r.exclude,
	// Perspective.class);
	// }

	int nRestrictions(boolean required) {
		return restrictions(required).size();
	}

	int nRestrictions() {
		return nRestrictions(true) + nRestrictions(false);
	}

	boolean isRestricted() {
		return isRestricted(true) || isRestricted(false);
	}

	boolean isRestricted(boolean required) {
		return !restrictions(required).isEmpty();
	}

	// Perspective getRestriction(int n, boolean required) {
	// return restrictions(required).[n];
	// }

	SortedSet<Perspective> restrictions(boolean required) {
		return required ? require : exclude;
	}

	boolean isRestriction(ItemPredicate facet, boolean required) {
		return restrictions(required).contains(facet);
	}

	SortedSet<Perspective> allRestrictions() {
		SortedSet<Perspective> result = new TreeSet<Perspective>(require);
		result.addAll(exclude);
		return result;
	}
}
