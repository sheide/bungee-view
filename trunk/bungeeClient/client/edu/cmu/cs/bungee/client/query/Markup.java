package edu.cmu.cs.bungee.client.query;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import edu.cmu.cs.bungee.javaExtensions.PrintArray;
import edu.cmu.cs.bungee.javaExtensions.Util;


public interface Markup extends List {

	public static final Object PLURAL_TAG = new Character('s');

	public static final Object NEWLINE_TAG = new Character('\n');

	public static final Object DEFAULT_COLOR_TAG = new Character('c');

	public static final Object DEFAULT_STYLE_TAG = new Character('b');

	public static final Object ITALIC_STRING_TAG = new Character('i');

	public static final Object GENERIC_OBJECT_LABEL = "Generic Object Label";

	public static final Color[] blues = { new Color(0x003320),
			new Color(0x006640), new Color(0x00BB70), new Color(0x00FF90) };

	public static final Color[] oranges = { new Color(0x662000),
			new Color(0x662000), new Color(0xBB5000), new Color(0xFF6000) };

	public static final Color[] greens = { new Color(0x003300),
			new Color(0x006600), new Color(0x00BB00), new Color(0x00FF00) };

	public static final Color[] reds = { new Color(0x330000),
			new Color(0x660000), new Color(0xBB0000), new Color(0xFF0000) };

	public static final Color[] whites = { new Color(0x555555),
			new Color(0x999999), new Color(0xFFFFFF) };

	public Markup compile(String genericObjectLabel);

	public Markup copy();
}

class MarkupImplementation extends Vector implements Markup {

	public Markup compile(String genericObjectLabel) {
		Markup v = new MarkupImplementation();
		Object prev = null;
		for (Iterator it = iterator(); it.hasNext();) {
			Object o = it.next();
			if (o == GENERIC_OBJECT_LABEL)
				o = genericObjectLabel;
			assert o != null : PrintArray.printArrayString(this);
			boolean thisIsString = o instanceof String;
			if (thisIsString && prev == PLURAL_TAG) {
				int prevIndex = v.size() - 1;
				prev = Util.pluralize((String) o);
				v.set(prevIndex, prev);
			} else if (thisIsString && prev instanceof String) {
				// Util.print("merging " + v.get(i-1) + "-" + o);
				int prevIndex = v.size() - 1;
				prev = (String) v.get(prevIndex) + (String) o;
				v.set(prevIndex, prev);
			} else {
				v.add(o);
				prev = o;
			}
		}
		return v;
	}

	static Markup tagDescription(List[] restrictions, boolean doTag,
			String[] patterns, String tag) {
		// Util.print("tagDescription " + restrictions + " " +
		// PrintArray.printArrayString(patterns));
		int nRestrictions = 0;
		Markup result = new MarkupImplementation();
		for (int j = 0; j < restrictions.length; j++) {
			List restriction = restrictions[j];
			if (restriction != null && restriction.size() > 0) {
				nRestrictions++;
				String pattern = patterns[j];
				int i = pattern.indexOf('~');
				restriction.add(0, Markup.DEFAULT_COLOR_TAG);
				if (i >= 0) {
					restriction.add(0, pattern.substring(0, i));
					restriction.add(pattern.substring(i + 1));
				} else {
					restriction.add(0, pattern);
				}
				if (doTag && result.size() == 0) {
					result.add(tag);
					if (nRestrictions == 1 && j == 1 && tag.equals("object")) {
						result.add(PLURAL_TAG);
						result.add(GENERIC_OBJECT_LABEL);
						nRestrictions++;
					}
				}
				if (nRestrictions == 2) {
					restriction.add(0, "but");
					restriction.add(")");
				}
				if (j > 0)
					// There's too much green. Only color-emphasize the
					// non-default case.
					// There's still too much red - better to put the color
					// commands in the pattern (e.g. only around "don't"
					restriction.add(0, Perspective.filterColors[j]);
				if (nRestrictions == 2) {
					restriction.add(0, " (");
				}
				result.addAll(restriction);
			}
		}
		// PrintArray.printArray(patterns);
		// PrintArray.printArray(result);
		// Util.print("");
		return result;
	}

//	public static Markup description(Perspective[] restrictions) {
//		SortedMap parentalGroups = new TreeMap();
//		for (int i = 0; i < restrictions.length; i++) {
//			Perspective child = restrictions[i];
//			Perspective parent = child.getFacetType();
//			SortedSet children = (SortedSet) parentalGroups.get(parent);
//			if (children == null) {
//				children = new TreeSet();
//				parentalGroups.put(parent, children);
//			}
//			children.add(child);
//		}
//		Markup[] phrases = new Markup[0];
//		Iterator it = parentalGroups.entrySet().iterator();
//
//		while (it.hasNext()) {
//			Map.Entry entry = (Entry) it.next();
//			Perspective parent = (Perspective) entry.getKey();
//			SortedSet children = (SortedSet) entry.getValue();
//			Perspective[] info = (Perspective[]) (children)
//					.toArray(new Perspective[0]);
//			Markup[] descriptions = new Markup[1];
//			Markup description = new MarkupImplementation();
//			toEnglish(info, " and ", description);
//			Markup result = parent.tagDescription(descriptions, true, null);
//			if (result.size() > 0)
//				phrases = (Markup[]) Util.push(phrases, result, Markup.class);
//		}
//		// Util.print("q.getPhrases return "
//		// + PrintArray.printArrayString(phrases));
//
//		Markup summary = new MarkupImplementation();
//		descriptionNounPhrase(phrases, summary);
//		descriptionClauses(phrases, summary, null, null);
//
//		return summary;
//	}

	static void descriptionNounPhrase(Markup[] phrases, Markup result) {
		for (int i = 0; i < phrases.length; i++) {
			Markup phrase = phrases[i];
			if (phrase.get(0).equals("object")) {
				for (int j = 1; j < phrase.size(); j++) {
					if (phrase.get(j) instanceof Perspective)
						result.add(Markup.PLURAL_TAG);
					result.add(phrase.get(j));
				}
			}
		}
		if (result.size() == 1)
			// 1 is for "Summary:"
			result.add(Markup.PLURAL_TAG);
		result.add(Markup.GENERIC_OBJECT_LABEL);
		// if (onCount != 1)
		// for (int i = 0; i < objects.size(); i++)
		// objects[i] = Util.pluralize(objects[i]);
		// result.add(Util.toEnglish(result, " and "));
		// Util.print("descriptionNounPhrase: " + result);
	}

	static void descriptionClauses(Markup[] phrases, Markup result,
			Set searches, Set clusters) {
		// Util.print("\nq.descriptionClauses "
		// + PrintArray.printArrayString(phrases));
		// PrintArray.printArray(result);
		int len = phrases.length;
		for (int i = 0; i < len; i++) {
			Markup phrase = phrases[i];
			if (phrase.get(0).equals("meta")
					&& !topLevelFacetClause(phrase, result)) {
				// result.add(" ");
				for (int j = 1; j < phrase.size(); j++) {
					result.add(phrase.get(j));
					// if (j == 1)
					// result.add(" ");
				}
			}
		}
		boolean first = true;
		for (int i = 0; i < len; i++) {
			Markup phrase = phrases[i];
			if (phrase.get(0).equals("content")
					&& !topLevelFacetClause(phrase, result)) {
				if (first) {
					// result.add(" that");
					first = false;
				} else
					result.add(" and");
				for (int j = 1; j < phrase.size(); j++) {
					result.add(phrase.get(j));
					// if (j == 1)
					// result.add(" ");
				}
			}
		}
			for (Iterator it = searches.iterator(); it.hasNext();) {
				String search = (String) it.next();
				String s;
				if (Util.nOccurrences(search, ' ') > 0)
					s = "whose description mentions one of the words '";
				else
					s = "whose description mentions '";
				if (first) {
					result.add(" ");
					first = false;
				} else
					result.add(" and ");
				result.add(s);
				result.add(Markup.greens[2]);
				result.add(search);
				result.add(Markup.DEFAULT_COLOR_TAG);
				result.add("'");
		}
		for (Iterator it = clusters.iterator(); it.hasNext();) {
			Cluster cluster = (Cluster) it.next();
				String search = cluster.toString();
				String s = " that has most of the features ";
				if (first) {
					result.add(" ");
					first = false;
				} else
					result.add(" and ");
				result.add(s);
				result.add(Markup.greens[2]);
				result.add(search);
				result.add(Markup.DEFAULT_COLOR_TAG);
			}
		// Util.print(phrases);
	}
	
	private static final Set emptySet = Collections.unmodifiableSet(new HashSet());

	static Markup facetDescription(Perspective facet) {
		Markup[] descriptions = { new MarkupImplementation(), null };
		Markup[] phrases = new Markup[0];
		if (facet != null) {
			descriptions[0].add(facet);
			phrases = new Markup[1];
			phrases[0] = facet.tagDescription(descriptions, true, null);
		}
		Markup summary = new MarkupImplementation();
		summary.add(" "); // descriptionNounPhrase assumes there is exactly
		// one canned prefix
		descriptionNounPhrase(phrases, summary);
		descriptionClauses(phrases, summary, emptySet, emptySet);
		return summary;
	}

//	static Markup clusterDescription(Cluster facet) {
//		return description(facet.allRestrictions());
//	}

	private static boolean topLevelFacetClause(Markup phrase, Markup result) {
		for (int j = 1; j < phrase.size(); j++) {
			Object o = phrase.get(j);
			if (o instanceof Perspective) {
				Perspective facet = (Perspective) o;
				if (facet.getParent() == null) {
					result.add(" having"
							+ Util.indefiniteArticle(facet.getNameIfPossible()));
					result.add(facet);
					return true;
				}
			}
		}
		return false;
	}

	static void toEnglish(Perspective[] facets, String connector,
			Markup descriptions) {
		int len = facets.length;
		if (len > 0) {
			if (len > 1)
				Arrays.sort(facets); // , Perspective.indexComparator);
			descriptions.add(facets[0]);
			if (len > 1) {
				for (int i = 1; i < facets.length - 1; i++) {
					descriptions.add(", ");
					descriptions.add(facets[i]);
				}
				descriptions.add(connector);
				descriptions.add(facets[facets.length - 1]);
			}
		}
	}

	public Markup copy() {
		Markup result = new MarkupImplementation();
		result.addAll(this);
		return result;
	}

}
