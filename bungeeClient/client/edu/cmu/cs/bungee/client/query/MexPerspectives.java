package edu.cmu.cs.bungee.client.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;

public class MexPerspectives implements ItemPredicate {

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((facets == null) ? 0 : facets.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MexPerspectives other = (MexPerspectives) obj;
		if (facets == null) {
			if (other.facets != null)
				return false;
		} else if (!facets.equals(other.facets))
			return false;
		return true;
	}

	final SortedSet facets;

	public MexPerspectives(Collection facets) {
		super();
		// Util.print("mexp "+facets);
		this.facets = new TreeSet(facets);
	}

	public MexPerspectives(Perspective start, Perspective end) {
		super();
		// Util.print("mexp "+start+" "+end);
		this.facets = new TreeSet();
		for (Iterator it = start.getParent().getChildIterator(start, end); it
				.hasNext();) {
			Perspective p = (Perspective) it.next();
			facets.add(p);
		}
	}

	public SortedSet allRestrictions() {
		return facets;
	}

	public int chiColorFamily(double significanceThreshold) {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result = Math.max(result, type
					.chiColorFamily(significanceThreshold));
		}
		return result;
	}

	public int compareTo(Object caused) {
		int result = -1;
		if (caused instanceof MexPerspectives) {
			result = ((Perspective) facets.first())
					.compareTo(((MexPerspectives) caused).facets.first());
			if (result == 0)
				result = ((Perspective) facets.last())
				.compareTo(((MexPerspectives) caused).facets.last());
		}
		return result;
	}

	public Markup describeFilter() {
		assert false;
		return null;
	}

	public Markup facetDoc(int modifiers) {
		assert false;
		return null;
	}

	public String getName() {
		return getName(null);
	}

	public String getName(PerspectiveObserver _redraw) {
		return ((Perspective) facets.first()).getName() + " - "
				+ ((Perspective) facets.last()).getName();
	}

	public String getNameIfPossible() {
		return ((Perspective) facets.first()).getNameIfPossible() + " - "
				+ ((Perspective) facets.last()).getNameIfPossible();
	}

	public int getOnCount() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.getOnCount();
		}
		return result;
	}

	public int getTotalCount() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.getTotalCount();
		}
		return result;
	}

	public int guessOnCount() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.guessOnCount();
		}
		return result;
	}

	public boolean isEffectiveChildren() {
		boolean result = false;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result |= type.isEffectiveChildren();
		}
		return result;
	}

	public boolean isRestricted() {
		boolean result = false;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result |= type.isRestricted();
		}
		return result;
	}

	public boolean isRestriction(ItemPredicate facet, boolean required) {
		boolean result = false;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result |= type.isRestriction(facet, required);
		}
		return result;
	}

	public boolean isRestriction() {
		boolean result = false;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result |= type.isRestriction();
		}
		return result;
	}

	public int nRestrictions() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.nRestrictions();
		}
		return result;
	}

	public double pValue() {
		assert false;
		return 0;
	}

	public int parentOnCount() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.parentOnCount();
		}
		return result;
	}

	public int parentTotalCount() {
		int result = 0;
		for (Iterator it = facets.iterator(); it.hasNext();) {
			Perspective type = (Perspective) it.next();
			result += type.parentTotalCount();
		}
		return result;
	}

	public double percentOn() {
		assert false;
		return 0;
	}

	public double percentageRatio() {
		assert false;
		return 0;
	}

	public Query query() {
		return ((Perspective) facets.first()).query();
	}

	public boolean restrictData() {
		assert false;
		return false;
	}

	public String getServerID() {
		// Util.print("GSID "+facets.toString());
		 return Query.getItemPredicateIDs(facets).replace(',', '-') ;
	}

	public String toString() {
		return toString(null);
	}

	public String toString(PerspectiveObserver redrawer) {
		return "<MexPerspectives " + getName(redrawer) + ">";
	}

}
