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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.bungee.javaExtensions.*;

import JSci.maths.statistics.ChiSq2x2;
import JSci.maths.statistics.OutOfRangeException;


public class Perspective implements Comparable, Serializable, ItemPredicate {

	private static final long serialVersionUID = 6142825395422325567L;

	/**
	 * Perspective for the more general facet or facet_type, Only changes if
	 * isEditable
	 */
	private Perspective parent;

	/**
	 * Only changes if
	 * isEditable
	 */
	private int facet_id;

	public int totalCount = -1;

	public int onCount = -1;

	/**
	 * The SUM of the totalCounts for us and our previous siblings. Used for
	 * placing bars.
	 */
	public int cumCount;

	private InstantiatedPerspective instantiatedPerspective = null;

//	public static final Comparator indexComparator = new IndexComparator();

	static final String[] filterTypes = { " = ", " \u2260 " };

	static final Color[] filterColors = { Markup.greens[3], Markup.reds[3] };

	// static final Perspective[] noDescendents = new ItemPredicate[0];

	Perspective(int _facet_id, Perspective _parent, String _name,
			int _children_offset, int n_children) {
		// Util.print("Perspective " + _name);
		assert _parent != null;
		assert _parent.getQuery().findPerspectiveIfPossible(facet_id) == null : _parent + " " + _name;
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
		assert _q.findPerspectiveIfPossible(facet_id) == null : _parent + " " + _name;
		parent = _parent;
		facet_id = _facet_id;
		instantiatedPerspective = new InstantiatedPerspective(n_children,
				_children_offset, _name, _descriptionCategory,
				_descriptionPreposition, _q);
	}

//	void setDescriptionInfo(String _descriptionPreposition) {
//		// Util.print("instantiate " + name + " " + nChildren);
//		instantiatedPerspective.setDescriptionInfo(_descriptionPreposition);
//	}

	public InstantiatedPerspective ensureInstantiatedPerspective() {
		if (instantiatedPerspective == null)
			instantiatedPerspective = new InstantiatedPerspective(this);
		return instantiatedPerspective;
	}

	public Perspective getParent() {
		return parent;
	}

	public void setParent(Perspective _parent) {
		if (Query.isEditable)
			 parent = _parent;
		else {
			throw (new UnsupportedOperationException("Can't change parent of "
					+ this));
		}
	}

	public int getID() {
		return facet_id;
	}

	public void setID(int _facet_id) {
		if (Query.isEditable)
			 facet_id = _facet_id;
		else {
			throw (new UnsupportedOperationException("Can't change facet_id of "
					+ this));
		}
	}

	public int nChildren() {
		if (isInstantiated())
			return instantiatedPerspective.getNchildren();
		else
			return 0;
	}

	public boolean isEffectiveChildren() {
		// if restrictData, all children might have zero count, in which case
		// we wouldn't have any bars to draw.
		// if it is -1, we don't know yet, so assume OK
		return nChildren() > 0 && getTotalChildTotalCount() != 0;
	}

	public int children_offset() {
		return instantiatedPerspective.children_offset;
	}

	public void setPrefetched() {
		instantiatedPerspective.isPrefetched = true;
	}

	public boolean isPrefetched() {
		return instantiatedPerspective != null
				&& instantiatedPerspective.isPrefetched;
	}

	boolean isInstantiated() {
		return instantiatedPerspective != null;
	}

	public boolean isUsed() {
		return getQuery().usesPerspective(this);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getOnCount()
	 */
	public int getOnCount() {
		int result;
		// ensurePrefetched();
		if (getQuery().isRestricted()) {
			// if (parent != null) {
			// assert getQuery().usesPerspective(parent) || onCount == -1 :
			// this;
			result = onCount;
			// } else {
			// return getTotalChildOnCount();
			// }
		} else
			result = totalCount;
		// if (parent == null)
		// Util.print(this + " " + result + " " + totalCount);
		return result;
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
		Query q = getQuery();
		if (!q.isRestrictedData() || getParent() == null
				|| q.usesPerspective(getParent()))
			return totalCount;
		else
			return -1;
	}

	public int getIndex() {
		assert getParent() != null : this;
		int result = getID() - getParent().children_offset() - 1;
		assert result >= 0 : this + " " + getID() + " "
				+ getParent().children_offset();
		assert result < getParent().nChildren() : this + " " + getID() + " "
				+ getParent().children_offset() + " " + getParent().nChildren();
		return result;
	}

	public Perspective getNthChild(int n) {
		return instantiatedPerspective.getNthChild(n);
	}

	public int getTotalChildTotalCount() {
		// getQuery().prefetchData(this);
		return instantiatedPerspective.totalChildTotalCount;
	}

	void setTotalChildTotalCount(int cnt) {
		// Util.print("setTotalChildTotalCount " + this + " " + cnt);
		instantiatedPerspective.totalChildTotalCount = cnt;
		if (cnt == 0 && getParent() != null) {
			getParent().deselectFacet(this, true);
		}
	}

	public double medianTest() {
		return instantiatedPerspective.medianTest();
	}

	public int medianTestSignificant(double pValue) {
		return instantiatedPerspective.medianTestSignificant(pValue);
	}

	public double median(int conditionalCoefficient,
			int unconditionalCoefficient) {
		return instantiatedPerspective.median(conditionalCoefficient,
				unconditionalCoefficient);
	}

	public Perspective getMedianPerspective(boolean conditional) {
		Perspective medianChild = null;
		double median = conditional ? median(1, 0) : median(0, 1);
		if (median >= 0.0) {
			int medianIndex = (int) median;
			medianChild = getNthChild(medianIndex);
		}
		return medianChild;
	}

	// public int getTotalChildOnCount() {
	// // getQuery().prefetchData(this);
	// int result;
	// if (getQuery().isRestricted()) {
	// result = perspective.totalChildOnCount;
	// } else
	// result = perspective.totalChildTotalCount;
	// // assert result >= 0; // No, let caller deal with -1
	// return result;
	// }
	//
	// public double maxChildPercentOn() {
	// // getQuery().prefetchData(this);
	// double result;
	// if (getQuery().isRestricted()) {
	// result = perspective.maxChildPercentOn;
	// if (result < 0) {
	// for (int i = 0; i < nValues(); i++) {
	// Perspective child = getValue(i);
	// double percent = child.onCount / (double) child.totalCount;
	// if (percent > result)
	// result = percent;
	// }
	// }
	// } else
	// result = 1;
	// // assert result >= 0; // No, let caller deal with -1
	// return result;
	// }
	//
	// void setTotalChildOnCount(int cnt) {
	// // Util.print("setTotalChildOnCount " + this + " " + cnt);
	// assert cnt <= getTotalChildTotalCount();
	// // perspective.totalChildOnCount = cnt;
	// perspective.maxChildPercentOn = -1;
	// perspective.isDataSorted = false;
	// }
	//
	// void incfTotalChildOnCount(int cnt) {
	// // Util.print("incfTotalChildOnCount " + this + " " + cnt);
	// assert getTotalChildOnCount() == 0;
	// assert getTotalChildOnCount() + cnt <= getTotalChildTotalCount() : this
	// + " " + cnt + "+" + getTotalChildOnCount() + ">"
	// + getTotalChildTotalCount();
	// perspective.totalChildOnCount += cnt;
	// perspective.isDataSorted = false;
	// }

	public String getNameIfPossible() {
		if (isInstantiated())
			return instantiatedPerspective.name;
		else
			return null;
	}

	public String getName(PerspectiveObserver redraw) {
		String result = getNameIfPossible();
		if (result != null) {
			return result;
		} else if (!getParent().isPrefetched()) {
			getQuery().queuePrefetch(getParent());
			// getQuery().prefetchData(parent);
			// assert name != null;
		} else {
			assert redraw != null;
		}
		getQuery().nameGetter.add(this);
		getQuery().nameGetter.add(redraw);
		// Util.print("getName " + parent.isPrefetched() + " " + this);
		return "?";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getName()
	 */
	public String getName() {
		return getName(null);
	}

	public void setName(String _name) {
		if (_name != null)
			ensureInstantiatedPerspective().name = _name;
	}

	void setChildrenOffset(int offset) {
		if (nChildren() > 0)
			ensureInstantiatedPerspective().children_offset = offset;
	}

	void setNchildren(int n, int child_offset) {
		if (n > 0)
			ensureInstantiatedPerspective().setNchildren(n, child_offset);
	}

	// public boolean isNameValid() {
	// return name != null;
	// }

	String[] getDescriptionPreposition() {
		if (isInstantiated())
			return instantiatedPerspective.descriptionPreposition;
		else
			return getParent().getDescriptionPreposition();
	}

	String getDescriptionCategory() {
		if (isInstantiated())
			return instantiatedPerspective.descriptionCategory;
		else
			return getParent().getDescriptionCategory();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getNameIfPossible()).append(" (").append(getID()).append(
				")");
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
		Markup filterDescription = getDescription(false, filterTypes); // getRestrictionName(false);
		filterDescription.add(0, "filter ");
		filterDescription.add(1, getFacetType());
		// filterDescription.add(2, " = ");
		// Util.print("Perspective.describeFilter "
		// + PrintArray.printArrayString(filterDescription));
		return filterDescription;
	}

	void describeFilter(Markup v, Perspective facet, boolean require) {
		v.add(getFacetType());
		v.add(filterColors[require ? 0 : 1]);
		v.add(filterTypes[require ? 0 : 1]);
		v.add(Markup.DEFAULT_COLOR_TAG);
		v.add(facet);
		// Util.print("Perspective.describeFilter "
		// + PrintArray.printArrayString(v));
	}

	// public String getFacetTypeName() {
	// if (parent != null)
	// return parent.getFacetTypeName();
	// else
	// return getName();
	// }

	public Perspective getFacetType() {
		if (getParent() != null)
			return getParent().getFacetType();
		else
			return this;
	}

	public boolean topLevel() {
		return getParent() == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#getQuery()
	 */
	public Query getQuery() {
		if (!isInstantiated())
			return getParent().getQuery();
		else
			return instantiatedPerspective.q;
	}

	public Perspective getNthOnValue(int i) {
		return getNthOnValue(i, true);
	}

	public Perspective getNthOnValue(int i, boolean isNonNull) {
		// assert !Util.hasDuplicates(perspective.dataIndexByOn) :
		// PrintArray.printArrayString(perspective.dataIndexByOn);
		if (isNonNull) {
			assert instantiatedPerspective.dataIndexByOn[i] != null : this
					+ " "
					+ i
					+ " / "
					+ nChildren()
					+ "\n"
					+ PrintArray
							.printArrayString(instantiatedPerspective.dataIndexByOn);
		}
		return instantiatedPerspective.dataIndexByOn[i];
	}

	void setMaxChildTotalCount(int maxCount) {
		// Util.print("setMaxChildTotalCount " + this + " " + maxCount);
		instantiatedPerspective.maxChildTotalCount = maxCount;
	}

	public int maxChildTotalCount() {
		assert instantiatedPerspective.maxChildTotalCount > 0 : this;
		return instantiatedPerspective.maxChildTotalCount;
	}

	boolean isAnyRestrictions() {
		return isRestricted() || nUsedChildren() > 0;
	}

	public void deleteRestriction(ItemPredicate facet, boolean require) {
		restrictions().delete(facet, require);
		decacheDescriptions();
	}

	public void addRestriction(ItemPredicate facet, boolean require) {
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

	public int nRestrictions(boolean require) {
		return restrictions().nRestrictions(require);
	}

	public boolean isRestricted() {
		return restrictions().isRestricted();
	}

	public boolean isRestricted(boolean required) {
		// Util.print("isRestricted " + this + " " + required + " => " +
		// perspective.restrictions.isRestricted(required));
		return restrictions().isRestricted(required);
	}

	Perspective getRestriction(int n, boolean require) {
		return restrictions().getRestriction(n, require);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#allRestrictions()
	 */
	public Perspective[] allRestrictions() {
		return restrictions().allRestrictions();
	}

	ItemPredicate[] restrictions(boolean require) {
		return restrictions().restrictions(require);
	}

	Restrictions restrictions() {
		return instantiatedPerspective.restrictions();
	}

	/**
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
		return restrictions().isRestriction(facet, required);
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
		if (getParent() != null)
			getParent().decacheDescriptions();
	}

	public Markup facetDescription() {
		return MarkupImplementation.facetDescription(this).compile(getQuery().genericObjectLabel);
	}

	// doTag means prepend with descriptionCategory & descriptionProposition
	// always prepend exclude (as NOT).
	Markup getDescription(boolean doTag, String[] patterns) {
		Markup[] descriptions = new Markup[2];
		boolean[] reqtTypes = { true, false };
		for (int type = 0; type < 2; type++) {
			boolean reqtType = reqtTypes[type];
			Perspective[] info = getRestrictionFacetInfos(false, reqtType);
			if (info != null && info.length > 0) {
				descriptions[type] = Query.emptyMarkup();
				MarkupImplementation
						.toEnglish(info, " or ", descriptions[type]);
			}
		}
		Markup result = tagDescription(descriptions, doTag, patterns);
		// Util.print(this + ".getDescription(" + doTag + ") => "
		// + PrintArray.printArrayString(result));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#tagDescription(java.util.Vector[], boolean,
	 *      java.lang.String[])
	 */
	public Markup tagDescription(List[] restrictions, boolean doTag,
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
	// If you click on an unselected value, it gets selected.
	// with Shift, values between it and the last selected value are also
	// selected,
	// otherwise, without Control, other values are deselected.
	// If you click on a selected value with Control, it gets unselected,
	// otherwise, without Shift all values are deselected.
	// Ancestors never change.
	// Descendents of any unselected value are killed.
	// Clicking in the Detail window:
	// On facet_type
	// with Shift or Control, does nothing
	// otherwise clears all values.
	// On descendent not represented by a perspective
	// adds missing perspectives, and then behaves as in summary window.
	// Otherwise, behaves as in summary window.
	//     
	// Algorithm:
	// If descendent add intermediates.
	// If facet_type
	// If Shift or Control, exit, otherwise clear.
	// Perspective where facet is a value will do what it says above.
	//     	
	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#facetDoc(int)
	 */
	public Markup facetDoc(int modifiers) {
		if (getParent() == null)
			// This happens for facet_type labels in SelectedItem frame.
			return facetDoc(this, modifiers);
		else
			return getParent().facetDoc(this, modifiers);
	}

	Markup facetDoc(Perspective facet, int modifiers) {
		boolean require = (modifiers == 0 && isRestriction(facet, false)) ? false
				: !isExcludeAction(modifiers);
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
		if (!isRestricted()) {
			if (facet.getOnCount() != (Perspective.isExcludeAction(modifiers) ? getQuery().onCount
					: 0)
					|| !getQuery().isQueryValid()
					// onCount may not be right if query is invalid.
					|| !getQuery().usesPerspective(facet.getParent()))
				// Don't encourage clicks that will return 0 results; but do
				// allow
				// clicks on deeply nested SelectedItem facets.
				result = selectFacetDoc(facet, require);
		} else if (isRestriction(facet, require)) {
			if (nRestrictions() == 1 || modifiers == 0)
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

	public static boolean isExcludeAction(int modifiers) {
		return (modifiers & EXCLUDE_ACTION) != 0;
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
			return getParent().ancestorRestriction(this);
		} else
			return null;
	}

	Markup deselectAllFacetsDoc(boolean require) {
		// Perspective ancestorRestriction = parent != null ?
		// parent.ancestorRestriction(this) : null;
		Perspective ancestorRestriction = ancestorRestriction(null);
		if (ancestorRestriction != null)
			return replaceFacetDoc(ancestorRestriction, require);
		else {
			int nRestrictions = getFacetType().nUsedChildren();
			Markup v = Query.emptyMarkup();
			if (nRestrictions > 1) {
				v.add("Remove " + nRestrictions + "filters on ");
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
		for (Iterator it = getQuery().perspectivesIterator(); it.hasNext();) {
			Perspective child = (Perspective) it.next();
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
		Perspective prev = getRestriction(0, require);
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

	/**
	 * All restriction changes go through this function.
	 * 
	 * @return Returns true if it changed the query.
	 */
	boolean toggleFacet(Perspective facet, int modifiers) {
		// Util.print("Perspective.toggleFacet " + facet + " " + modifiers);
		boolean result = true;
		boolean require = (modifiers == 0 && isRestriction(facet, false)) ? false
				: !isExcludeAction(modifiers);
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

	/**
	 * @return Returns true if it changed the query.
	 */
	private boolean selectInterveningFacets(Perspective facet, boolean require) {
		assert !isRestriction(facet, require) : "selectInterveningFacets problem";
		if (isRestricted(require)) {
			int index = facet.getID();
			int index2 = getRestriction(0, require).getID();
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
		assert isRestricted() : "deselectAllFacets problem";
		instantiatedPerspective.q.clearPerspective(this);
	}

	/**
	 * @return Returns true if it changed the query.
	 */
	 boolean selectFacet(Perspective facet, boolean require) {
		// Util.print("p.selectFacet " + facet + " " + require);
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
		instantiatedPerspective.resetData(count, this);
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
		for (int i = 0; i < nChildren(); i++) {
			Perspective child = getNthChild(i);
			int count = child.onCount;
			// if (isRestriction(p, true) || isRestriction(p, false)) {
			// // Util.print(p + " " + 0);
			// count = 0;
			// p.onCount = 0;
			// }

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
			getQuery().removeRestriction(this);
		setTotalChildTotalCount(childCumCount);
		// totalCount = onCount;
		// Util.print("setting " + this + " totalCount to " + onCount);
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
	// // + PrintArray.printArrayString(result));
	// return result;
	// }

	// int[] getRestrictions(boolean require) {
	// Perspective[] info = getRestrictionFacetInfos(false, require);
	// int[] result = new int[info.length];
	// for (int i = 0; i < result.length; i++)
	// result[i] = info[i].facet_id;
	// return result;
	// }

	// isLocalOnly means don't return restrictions implied by a restriction on a
	// child Perspective.
	// otherwise, return the more specific restriction(s).
	public Perspective[] getRestrictionFacetInfos(boolean isLocalOnly,
			boolean require) {
		// Util.print("getRestrictionFacetInfos " + this + " " + require + " "
		// + isLocalOnly + " " + nRestrictions(require));
		// assert isLocalOnly || require : "Excludes don't propagate up, so you
		// can't search for them non-locally!";
		Perspective[] result = {};
		int n = nRestrictions(require);
		for (int r = 0; r < n; r++) {
			Perspective child = getRestriction(r, require);
			boolean found = false;
			if (require && getQuery().usesPerspective(child)) {
				if (isLocalOnly)
					found = true;
				else {
					ItemPredicate[] childResult = child
							.getRestrictionFacetInfos(isLocalOnly, require);
					if (childResult.length > 0) {
						found = true;
						result = (Perspective[]) Util.append(result,
								childResult, Perspective.class);
					}
				}
			}
			if (!found) {
				assert child != null;
				result = (Perspective[]) Util.push(result, child,
						Perspective.class);
			}
		}
		if (!isLocalOnly && !require) {
			for (Iterator it = getQuery().perspectivesIterator(); it.hasNext();) {
				Perspective child = (Perspective) it.next();
				if (child.getParent() == this) {
					result = (Perspective[]) Util.append(result, child
							.getRestrictionFacetInfos(isLocalOnly, require),
							Perspective.class);
				}
			}
		}
		// Util.print("getRestrictionFacetInfos return "
		// + PrintArray.printArrayString(result));
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
		assert c >= 0 : this + " " + getParent() + " " + c + "/" + totalCount;
		assert c <= totalCount : this + " " + c + "/" + totalCount;
		return c / (double) totalCount;
	}

	void incfChildren(int delta) {
		assert Query.isEditable;
		instantiatedPerspective.incfChildren(delta);
	}

	// public void sortDataIndexByIndex() {
	// instantiatedPerspective.sortDataIndexByIndex();
	// }

	void addFacet(int index, Perspective facet) {
		// Util.print("addFacet " + this + " " + index + " " + facet);
		ensureInstantiatedPerspective().addFacet(index, facet);
	}

	public void sortDataIndexByOn() {
		instantiatedPerspective.sortDataIndexByOn(this);
	}

	boolean isDataIndexByOnComplete() {
		for (int i = 0; i < nChildren(); i++) {
			getNthOnValue(i, true); // Will error if result would be null
		}
		return true;
	}

	void addFacetAllowingNulls(int index, Perspective facet) {
		instantiatedPerspective.addFacetAllowingNulls(index, facet);
	}

//	public boolean isSortedByOn() {
//		return instantiatedPerspective.isSortedByOn;
//	}

	public void sortByOn(Perspective[] perspectives) {
		instantiatedPerspective.sortByOn(perspectives);
	}

	public boolean hasAncestor(ItemPredicate ancestor) {
		return (ancestor == getParent())
				|| (getParent() != null && getParent().hasAncestor(ancestor));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#pValue()
	 */
	public synchronized double pValue() {
		ChiSq2x2 table = ensureInstantiatedPerspective().setChiSqTable(this);
		double result = table.pValue();
		assert 0 <= result && result <= 1;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#chiColorFamily(double)
	 */
	public synchronized int chiColorFamily(double p) {
		ChiSq2x2 table = ensureInstantiatedPerspective().setChiSqTable(this);
		return table.significant(p);
	}

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
	// // PrintArray.printArrayString(table) +
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

	public boolean isOrdered() {
		return getQuery().isOrdered(this);
	}

	public int compareTo(Object arg0) {
		// return indexComparator.compare(this, arg0);
		return ((Perspective) arg0).getID() - getID();
	}

	public boolean equals(Object arg0) {
		return arg0 != null && arg0 instanceof Perspective
				&& compareTo(arg0) == 0;
	}

	public int hashCode() {
		return getID();
	}

}

class InstantiatedPerspective implements Serializable {

	private static final long serialVersionUID = -2623228135828064129L;

	 String name;

	/**
	 * The children will have consecutive facet_id's, starting one after this.
	 * Should only change if isEditable
	 */
	int children_offset = -1;

	/**
	 * Should only change if isEditable
	 */
	private int nChildren = 0;

	/**
	 * Used to determine whether ChiSqr table is up to date
	 */
	private int updateIndex = 0;

	private ChiSq2x2 chiSqTable;

	boolean isPrefetched = false;

	/**
	 * This child's <code>totalCount</code> divided by all siblings'
	 * <code>totalCount</code>.
	 */
	// double parentPercent = 1.0;
	/**
	 * <code>onCount</code> -sorted version of the elements of
	 * <code>data</code>. Would prefer to offer an iterator for this, but you
	 * can't sort anything that supports them.
	 * 
	 * Lazily created by setNchildren
	 * 
	 */
	Perspective[] dataIndexByOn;

	/**
	 * <code>onCount</code> -sorted version of the elements of
	 * <code>data</code>. Would prefer to offer an iterator for this, but you
	 * can't sort anything that supports them.
	 * 
	 */
	Perspective[] dataIndex;

	/**
	 * Query is filtered to return only results with one of these facet values.
	 * Ordered FIFO, to support selecting with SHIFT.
	 */
	Restrictions restrictions;

	/**
	 * object, meta, or content. Used to generate query description.
	 */
	final String descriptionCategory;

	/**
	 * Pattern into which facet name is substituted for '~'. Default is
	 * implicitly [descriptionPreposition ~, NOT descriptionPreposition ~]. Used
	 * to generate query description.
	 */
	final String[] descriptionPreposition;

	final Query q;

	/**
	 * Used to sort dataIndexByOn.
	 */
	private static final Comparator onCountComparator = new OnCountComparator();

	private static final Comparator totalCountComparator = new TotalCountComparator();

	// Vector descriptions = null;

	// Vector filterDescription = null;

	/**
	 * -1 not sorted 0 sorted by index 1 sorted by onCount
	 */
	private boolean isSortedByOn;

	// String[] localRestrictionNames = null; // cache this
	//
	// String[] nonLocalRestrictionNames = null;

	/**
	 * The sum of the totalCounts for our children. Can be less than totalCount
	 * if some items aren't further categorized, or greater if some items are in
	 * multiple child categories.
	 */
	int totalChildTotalCount = -1;

	// int totalChildOnCount = -1;

	int maxChildTotalCount = -1;

	// int maxChildPercentOn = -1;g " + this);

	InstantiatedPerspective(Perspective p) {
		q = p.getQuery();
		descriptionCategory = p.getParent().getDescriptionCategory();
		descriptionPreposition = p.getParent().getDescriptionPreposition();
		// if (nChildren > 0) {
		// dataIndex = new Perspective[p.nChildren()];
		// dataIndexByOn = new Perspective[p.nChildren()];
		// }
		// Util.print("instantiating " + this);
	}

	InstantiatedPerspective(Perspective p, int n_children, int child_offset,
			String _name) {
		assert p != null;
		assert p.getParent() != null : p;
//		InstantiatedPerspective _parent = p.getParent().instantiatedPerspective;
		q = p.getQuery();
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

	synchronized String[] parseDescriptionPreposition(String descriptionPrepositionString) {
		// Util.print("instantiate " + name + " " + nChildren);
		String[] patterns = Util.splitSemicolon(descriptionPrepositionString);
		if (patterns.length == 1)
			patterns = (String[]) Util.endPush(
					patterns,
					" NOT " + patterns[0], String.class);
		return patterns;
	}

	int getNchildren() {
		return nChildren;
	}

	void setNchildren(int n, int child_offset) {
		assert children_offset < 0 || Query.isEditable;
		if (n != nChildren) {
			assert child_offset > 0 : child_offset;
			// Util.print("setNchildren " + this + " " + n + " " +
			// child_offset);
			nChildren = n;
			dataIndex = new Perspective[nChildren];
			dataIndexByOn = new Perspective[nChildren];
			children_offset = child_offset;
		}
	}

	synchronized double medianTest() {
		ChiSq2x2 table = medianTestTable();
		if (table == null)
			return 1.0;
		else
			return table.pValue();
	}

	synchronized int medianTestSignificant(double pValue) {
		ChiSq2x2 table = medianTestTable();
		if (table == null)
			return 0;
		else
			return -table.significant(pValue);
	}

	// int[][] medianTestTable() {
	// double median = median(0, 1);
	// int medianIndex = (int) median;
	// int cumOnCount = 0;
	// int cumOffCount = 0;
	// for (int i = 0; i < medianIndex; i++) {
	// Perspective child = getValue(i);
	// int n = child.getOnCount();
	// if (n < 0)
	// // We may have prefetched, but haven't gotten onCounts yet
	// return null;
	// cumOnCount += n;
	// cumOffCount += child.totalCount - n;
	// }
	// Perspective child = getValue(medianIndex);
	// int n = child.getOnCount();
	// int[][] table = {
	// {
	// cumOffCount,
	// getTotalChildTotalCount() - cumOffCount
	// - child.totalCount + n },
	// { cumOnCount, getTotalChildOnCount() - cumOnCount - n } };
	// // Util.print("medianTest " + this + " " + ChiSqr.pValue(table) + " "
	// // + PrintArray.printArrayString(table));
	// return table;
	// }

	private static final ChiSq2x2 medianTable = new ChiSq2x2();

	ChiSq2x2 medianTestTable() {
		int on = getTotalChildOnCount();
		if (totalChildTotalCount == on)
			return null;
		double median = median(0, 1);
		int medianIndex = (int) median;
		int cumOnCount = 0;
		int cumOffCount = 0;
		for (int i = 0; i < medianIndex; i++) {
			Perspective child = dataIndex[i];
			int n = child.getOnCount();
			if (n < 0)
				// We may have prefetched, but haven't gotten onCounts yet
				return null;
			cumOnCount += n;
			cumOffCount += child.totalCount - n;
		}
		Perspective child = dataIndex[medianIndex];
		int n = child.getOnCount();
		int total = totalChildTotalCount - child.totalCount;
		int row0 = on - n;
		int col0 = cumOnCount + cumOffCount;
		if (row0 == 0 || col0 == 0 || row0 == total || col0 == total)
			return null;
		assert total > 0 : this + " " + child + " " + totalChildTotalCount
				+ " " + child.totalCount;
		assert row0 > 0 : this + " " + child + " " + on + " " + n;
		medianTable.setChiSq2x2(total, row0, col0, cumOnCount);
		// int[][] table = {
		// {
		// cumOffCount,
		// getTotalChildTotalCount() - cumOffCount
		// - child.totalCount + n },
		// { cumOnCount, getTotalChildOnCount() - cumOnCount - n } };
		// Util.print("medianTest " + this + " " + ChiSqr.pValue(table) + " "
		// + PrintArray.printArrayString(table));
		return medianTable;
	}

	public ChiSq2x2 setChiSqTable(Perspective perspective) {
		if (chiSqTable == null)
			chiSqTable = new ChiSq2x2();
		if (updateIndex != q.updateIndex) {
			if (q.isQueryValid() && q.isRestricted()) {
				int totalCount = perspective.totalCount;
				int onCount = perspective.onCount;
				Perspective parent = perspective.getParent();
				int total;
				int on;
				if (parent != null) {
					total = parent.totalCount;
					assert total >= totalCount : perspective + ".totalCount("
							+ totalCount + ") > " + perspective.getParent()
							+ " .totalCount(" + total + ") " + q.isQueryValid();
					on = parent.onCount;
				} else {
					total = q.totalCount;
					assert total >= totalCount : perspective + ".totalCount("
							+ totalCount + ") > query.totalCount(" + total
							+ ") " + q.isQueryValid();
					on = q.onCount;
				}
				if (total > on && total > totalCount && on > 0
						&& totalCount > 0 && onCount >= 0) {
					// Deeply nested facets have on=-1;
					try {
						assert on >= onCount : this + " " + total + " " + on
								+ " " + totalCount + " " + onCount;
						assert totalCount >= onCount : this + " " + total + " "
								+ on + " " + totalCount + " " + onCount;
						chiSqTable.setChiSq2x2(total, on, totalCount, onCount);
					} catch (OutOfRangeException e) {
						System.err.println(this);
						e.printStackTrace();
					}
				} else
					chiSqTable.reset();
			} else
				chiSqTable.reset();
			updateIndex = q.updateIndex;
		}
		return chiSqTable;
	}

	public double median(int conditionalCoefficient,
			int unconditionalCoefficient) {
		int totalOnCount = 0;
		if (conditionalCoefficient != 0) {
			totalOnCount = getTotalChildOnCount();
			if (totalOnCount <= 0)
				return -1.0;
		}
		// sortDataIndexByIndex();
		int cumOnCount = 0;
		int medianCount = unconditionalCoefficient * totalChildTotalCount
				+ conditionalCoefficient * totalOnCount;
		medianCount /= 2;
		for (int i = 0; i < nChildren; i++) {
			Perspective child = dataIndex[i];
			int childCount = conditionalCoefficient * child.getOnCount()
					+ unconditionalCoefficient * child.totalCount;
			cumOnCount += childCount;
			if (cumOnCount > medianCount) {
				double childFraction = 1.0 - (cumOnCount - medianCount)
						/ (double) childCount;
				assert !Double.isNaN(childFraction) : child + " " + medianCount
						+ " " + childCount;
				return i + childFraction;
			}
		}
		assert false : this + " " + conditionalCoefficient + " "
				+ unconditionalCoefficient + " " + medianCount + " "
				+ cumOnCount;
		return -1.0;
	}

	void addFacetAllowingNulls(int index, Perspective facet) {
		// Util.print("addFacet " + this + " " + index + " " + facet);
		assert facet != null : this;
		// sortDataIndexByIndexAllowingNulls();
		isSortedByOn = false;
		assert dataIndex[index] == facet || !Util.isMember(dataIndex, facet) : this
				+ " "
				+ index
				+ " "
				+ facet
				+ "\n"
				+ PrintArray.printArrayString(dataIndex);
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
	// + PrintArray.printArrayString(dataIndexByOn);
	// }

	 void incfChildren(int delta) {
		assert Query.isEditable;
		nChildren += delta;
		Util.print("incfChildren " + delta + " is nuking " + this
				+ "'s dataIndexByOn:\n"
				+ PrintArray.printArrayString(dataIndexByOn));
		dataIndex = new Perspective[nChildren];
		dataIndexByOn = new Perspective[nChildren];
		// all children better be renamed
		isSortedByOn = false;
	}

	void addFacet(int index, Perspective facet) {
		// Util.print("addFacet " + this + " " + index + " " + facet);
		assert facet != null : this;
		assert index < nChildren : facet + " " + index + " " + nChildren;
		assert dataIndex != null : facet + " " + nChildren;
		assert dataIndex.length == nChildren : facet + " " + dataIndex.length
				+ " " + nChildren;
		// sortDataIndexByIndex();
		isSortedByOn = false;

		// This is too slow
		// assert dataIndex[index] == facet || !Util.isMember(dataIndex, facet)
		// : this
		// + " "
		// + index
		// + " "
		// + facet
		// + "\n"
		// + PrintArray.printArrayString(dataIndex);
		dataIndex[index] = facet;
	}

	public Perspective getNthChild(int n) {
		assert n >= 0 : n;
		assert n < nChildren;

		Perspective result = q.findPerspective(
				children_offset + n + 1);
		assert result != null : this + " " + n + " " + nChildren;
		return result;
	}

	void resetData(int count, Perspective myPerspective) {
		// Util.print("Perspective.resetData " + this + " " + count);
		assert count <= 0;
		assert isPrefetched : this;
		// if (isPrefetched())
		for (int i = 0; i < nChildren; i++) {
			getNthChild(i).onCount = count;
		}
		myPerspective.onCount = count;
		isSortedByOn = false;
		// setTotalChildOnCount(count);
		// Util.print(" Perspective.resetData return");
	}

	public void sortDataIndexByOn(Perspective myPerspective) {
		// Util.print("sortDataIndexByOn " + this + " " + q.isRestricted() + " "
		// + isSortedByOn + " " + myPerspective.getOnCount() + " "
		// + dataIndex[0].onCount);
		if (!isSortedByOn && myPerspective.getOnCount() >= 0) {
			assert q.usesPerspective(myPerspective) : myPerspective;
			synchronized (q.childIndexesBusy) {
				// PrintArray.printArray(dataIndexByOn);
				if (dataIndexByOn[0] == null) {
					System.arraycopy(dataIndex, 0, dataIndexByOn, 0, nChildren);
				}
				sortByOn(dataIndexByOn);
				isSortedByOn = true;
				assert isSortedByOn(dataIndexByOn) : this + " " + isSortedByOn
						+ " " + myPerspective.getOnCount() + " "
						+ myPerspective.getQuery().isRestricted();
			}
		} else {
			assert isSortedByOn(dataIndexByOn) : this + " " + isSortedByOn
					+ " " + myPerspective.getOnCount() + " "
					+ myPerspective.getQuery().isRestricted();
		}
		// Util.print(" sortDataIndexByOn return");
	}

	boolean isSortedByOn(Perspective[] perspectives) {
		int prevCount = Integer.MAX_VALUE;
		for (int i = 0; i < nChildren; i++) {
			Perspective child = perspectives[i];
			assert child != null : this + " " + nChildren + " "
					+ PrintArray.printArrayString(perspectives);
			int onCount = child.getOnCount();
			if (onCount > prevCount) {
				Util.err(" NOT SORTED!!! " + this + "." + child + " " + onCount
						+ " " + child.totalCount + " " + perspectives[i - 1]
						+ " " + prevCount + " "
						+ perspectives[i - 1].totalCount);
				return false;
			}
			prevCount = onCount;
		}
		return true;
	} // boolean isSortedByIndex(Perspective[] perspectives) {

	// assert dataIndexByOn.length == nChildren : nChildren + " " +
	// dataIndexByOn.length;
	// for (int i = 0; i < nChildren; i++) {
	// Perspective child = dataIndexByOn[i];
	// assert child == null || child.getIndex() == i : this + "." + child
	// + " " + child.getIndex() + " " + i + " " + children_offset
	// + "\n" + PrintArray.printArrayString(perspectives);
	// }
	// return true;
	// }

	public void sortByOn(Perspective[] perspectives) {
		assert !Util.hasDuplicates(perspectives) : PrintArray
				.printArrayString(perspectives);
		Comparator comparator = q.isRestricted() ? InstantiatedPerspective.onCountComparator
				: InstantiatedPerspective.totalCountComparator;

		assert Util.nOccurrences(perspectives, null) == 0 : this + " "
				+ Util.nOccurrences(perspectives, null) + " / "
				+ perspectives.length;
		Arrays.sort(perspectives, comparator);
	}

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

	public String toString() {
		return "<InstantiatedPerspective " + name + ">";
	}
}

class OnCountComparator extends ValueComparator {

	private static final long serialVersionUID = 2135949170144308724L;

	public int value(Object data) {
		return ((Perspective) data).onCount;
	}
}

class TotalCountComparator extends ValueComparator {

	private static final long serialVersionUID = -2283814082752971019L;

	public int value(Object data) {
		return ((Perspective) data).totalCount;
	}
}

class IndexComparator extends ValueComparator {

	private static final long serialVersionUID = -8035275938809155031L;

	public int value(Object data) {
		// Util.print(data + " " + ((Perspective) data).getIndex());
		return -((Perspective) data).getID();
	}
}

class Restrictions {
	Perspective[] require = {};

	Perspective[] exclude = {};

	void delete(ItemPredicate facet, boolean required) {
		// Util.print("Perspective.delete " + this + "." + facet + " " +
		// required);
		assert isRestriction(facet, required) : facet + " " + required;
		if (required)
			require = (Perspective[]) Util.delete(require, facet,
					Perspective.class);
		else
			exclude = (Perspective[]) Util.delete(exclude, facet,
					Perspective.class);
	}

	void add(ItemPredicate facet, boolean required) {
		// Util.print("add " + facet);
		assert facet != null;
		assert !isRestriction(facet, required);
		if (required)
			require = (Perspective[]) Util.push(require, facet,
					Perspective.class);
		else
			exclude = (Perspective[]) Util.push(exclude, facet,
					Perspective.class);
	}

	// void append(Restrictions r) {
	// require = (Perspective[]) Util.append(require, r.require,
	// Perspective.class);
	// exclude = (Perspective[]) Util.append(exclude, r.exclude,
	// Perspective.class);
	// }

	public int nRestrictions(boolean required) {
		return restrictions(required).length;
	}

	public int nRestrictions() {
		return nRestrictions(true) + nRestrictions(false);
	}

	public boolean isRestricted() {
		return isRestricted(true) || isRestricted(false);
	}

	public boolean isRestricted(boolean required) {
		return restrictions(required).length > 0;
	}

	Perspective getRestriction(int n, boolean required) {
		return restrictions(required)[n];
	}

	Perspective[] restrictions(boolean required) {
		return required ? require : exclude;
	}

	boolean isRestriction(ItemPredicate facet, boolean required) {
		return Util.isMember(restrictions(required), facet);
	}

	Perspective[] allRestrictions() {
		return (Perspective[]) Util.append(require, exclude, Perspective.class);
	}
}
