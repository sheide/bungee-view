package edu.cmu.cs.bungee.client.query;

import java.util.SortedSet;

/**
 * @author mad
 * Generalization of Perspective and Cluster
 */
public interface ItemPredicate {

	/**
	 * modifier flagging that user gesture adds a negated filter
	 * choose a bit that isn't used by InputEvent (control, shift, etc)
	 */
	public static final int EXCLUDE_ACTION = 16384;

	/**
	 * @return the number of items satisfying this predicate and the current query
	 */
	public abstract int getOnCount();

	/**
	 * @return the number of items in the [possibly restricted] collection satisfying this predicate
	 */
	public abstract int getTotalCount();

	/**
	 * @return human-readable naem of this predicate
	 */
	public abstract String getName();

	/**
	 * @return E.g. 'filter <parent> = <this>'
	 */
	public abstract Markup describeFilter();

	/**
	 * @return the query this predicate is defined in
	 */
	public abstract Query query();

	/**
	 * @return allRestrictions().length, but more efficient
	 */
	public abstract int nRestrictions();

	/**
	 * @return the child Perspectives involved in positive or negative filters
	 */
	public abstract SortedSet allRestrictions();

	/**
	 * @param facet child perspective possibly involved in a filter
	 * @param required look for facet as a positive or negative filter?
	 * @return Does this perspective have a filter (of polarity required) on facet?
	 */
	public abstract boolean isRestriction(ItemPredicate facet, boolean required);

	/**
	 * @return Is this perspective one of its parent's filters (of any polarity)?
	 */
	public abstract boolean isRestriction();

	// public abstract Markup tagDescription(List[] restrictions, boolean doTag,
	// String[] patterns);

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
	/**
	 * @param modifiers -
	 *            from InputEvent.
	 * @return Description of what will happen if the mouse is clicked, e.g. 'add filter <parent> = <this>'
	 */
	public abstract Markup facetDoc(int modifiers);

	/**
	 * Recompute child cumCounts, totalChildTotalCount, maxChildTotalCount, and
	 * clear restrictions.
	 * 
	 * @return whether any items satisfy this predicate
	 * 
	 * Does not change totalCount
	 */
	public abstract boolean restrictData();

	/**
	 * @return getOnCount() / getTotalCount()
	 */
	public abstract double percentOn();

	/**
	 * @return significance level of percentOn() compared to parant's percentOn()
	 */
	public abstract double pValue();

	/**
	 * @param significanceThreshold
	 * @return 0 if p-value > significanceThreshold, else 1 if this predicate's
	 *         percentOn is larger than its parent, else -1
	 */
	public abstract int chiColorFamily(double significanceThreshold);

	public abstract String getName(PerspectiveObserver _redraw);

	public abstract String getNameIfPossible();

//	public abstract ItemPredicate getParent();

	public abstract boolean isEffectiveChildren();

	public abstract boolean isRestricted();

	/**
	 * Used to color text, so only zero vs. non-zero matters. We want to update
	 * text as soon as filters are changed, but we don't know the counts yet.
	 * SelectedItem counts are always > 0. Bar labels, facetType labels, and
	 * summary description will always be redrawn once we know the counts. In
	 * the mean time, don't gray them out.
	 * 
	 * @return the onCount, or totalCount if we don't know.
	 */
	public abstract int guessOnCount();

}