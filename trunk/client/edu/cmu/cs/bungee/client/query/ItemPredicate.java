package edu.cmu.cs.bungee.client.query;

public interface ItemPredicate {

	public static final int EXCLUDE_ACTION = 16384;

	public abstract int getOnCount();

	public abstract int getTotalCount();

	public abstract String getName();

	/**
	 * @return E.g. 'filter <parent> = <this>'
	 */
	public abstract Markup describeFilter();

	public abstract Query getQuery();

	public abstract int nRestrictions();

	public abstract Perspective[] allRestrictions();

	/**
	 * @param facet
	 * @return Does this perspective have a filter on this facet?
	 */
	public abstract boolean isRestriction(ItemPredicate facet, boolean required);

	// public abstract Markup tagDescription(List[] restrictions, boolean doTag,
	// String[] patterns);

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
	/**
	 * @param facet
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

	public abstract double percentOn();

	public abstract double pValue();

	/**
	 * @param p
	 * @return 0 if p-value > significanceThreshold, else 1 if this predicate's
	 *         percentOn is larger than its parent, else -1
	 */
	public abstract int chiColorFamily(double significanceThreshold);

}