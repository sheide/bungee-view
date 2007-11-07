package edu.cmu.cs.bungee.client.query;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * @author mad A little language for tagged sequences of query components, for
 *         generating natural language descriptions
 */
public interface Markup extends List {

	/**
	 * Add an 's' to the next token
	 */
	public static final Object PLURAL_TAG = new Character('s');

	/**
	 * insert a newline
	 */
	public static final Object NEWLINE_TAG = new Character('\n');

	/**
	 * render subsequent tokens in the default color
	 */
	public static final Object DEFAULT_COLOR_TAG = new Character('c');

	/**
	 * render subsequent tokens in the default text style
	 */
	public static final Object DEFAULT_STYLE_TAG = new Character('b');

	/**
	 * render subsequent tokens in italics
	 */
	public static final Object ITALIC_STRING_TAG = new Character('i');

	/**
	 * insert 'image' or 'work' or whatever, as specified in
	 * globals.genericObjectLabel
	 */
	public static final Object GENERIC_OBJECT_LABEL = "Generic Object Label";
	/**
	 * The '->' symbol that goes in front of rank labels
	 */
	public static final String parentIndicatorPrefix = "\u2192"; // '\u2023'

	/**
	 * Colors used for facets significantly positively associated with the
	 * current filters
	 */
	public static final Color[] POSITIVE_ASSOCIATION_COLORS = { new Color(0x003320),
			new Color(0x006640), new Color(0x00BB70), new Color(0x00FF90) };

	/**
	 * Colors used for facets significantly negatively associated with the
	 * current filters
	 */
	public static final Color[] NEGATIVE_ASSOCIATION_COLORS = { new Color(0x662000),
			new Color(0x662000), new Color(0xBB5000), new Color(0xFF6000) };

	/**
	 * Colors used for facets in positive filters
	 */
	public static final Color[] INCLUDED_COLORS = { new Color(0x003300),
			new Color(0x006600), new Color(0x00BB00), new Color(0x00FF00) };

	/**
	 * Colors used for facets in negative filters
	 */
	public static final Color[] EXCLUDED_COLORS = { new Color(0x330000),
			new Color(0x660000), new Color(0xBB0000), new Color(0xFF0000) };

	/**
	 * Colors used for facets not significantly associated with the current
	 * filters
	 */
	public static final Color[] UNASSOCIATED_COLORS = { new Color(0x555555),
			new Color(0x999999), new Color(0xFFFFFF) };

	/**
	 * @param genericObjectLabel
	 *            what you call items, e.g. 'image' or 'work'
	 * @return a Markup ready to render
	 */
	public Markup compile(String genericObjectLabel);

	/**
	 * @return a copy of this markup.
	 */
	public Markup copy();

	/**
	 * @return this Markup rendered as a String
	 */
	public String toText();

	/**
	 * @param _redraw
	 *            callback object when any unknown facet names are read in
	 * @return this Markup rendered as a String
	 */
	public String toText(PerspectiveObserver _redraw);
}

final class MarkupImplementation extends ArrayList implements Markup {

	public Markup compile(String genericObjectLabel) {
		Markup v = new MarkupImplementation();
		Object prev = null;
		for (Iterator it = iterator(); it.hasNext();) {
			Object o = it.next();
			if (o == GENERIC_OBJECT_LABEL)
				o = genericObjectLabel;
			assert o != null : Util.valueOfDeep(this);
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
		// Util.valueOfDeep(patterns));
		int nPolaritiesUsed = 0;
		Markup result = new MarkupImplementation();
		for (int polarity = 0; polarity < restrictions.length; polarity++) {
			if (restrictions[polarity] != null) {
				List polarityDesc = new LinkedList(restrictions[polarity]);
				if (polarityDesc.size() > 0) {
					nPolaritiesUsed++;
					String pattern = patterns[polarity];
					int i = pattern.indexOf('~');
					polarityDesc.add(0, Markup.DEFAULT_COLOR_TAG);
					if (i >= 0) {
						polarityDesc.add(0, pattern.substring(0, i));
						polarityDesc.add(pattern.substring(i + 1));
					} else {
						polarityDesc.add(0, pattern);
					}
					if (doTag && result.size() == 0) {
						result.add(tag);
						if (nPolaritiesUsed == 1 && polarity == 1
								&& tag.equals("object")) {
							// negative restrictions only
							result.add(PLURAL_TAG);
							result.add(GENERIC_OBJECT_LABEL);
							nPolaritiesUsed++;
						}
					}
					if (nPolaritiesUsed == 2) {
						polarityDesc.add(0, "but");
						polarityDesc.add(")");
					}
					if (polarity > 0)
						// There's too much green. Only color-emphasize the
						// non-default case.
						// There's still too much red - better to put the color
						// commands in the pattern (e.g. only around "don't"
						polarityDesc.add(0, Perspective.filterColors[polarity]);
					if (nPolaritiesUsed == 2) {
						polarityDesc.add(0, " (");
					}
					result.addAll(polarityDesc);
				}
			}
		}
		// Util.printDeep(patterns);
		// Util.printDeep(result);
		// Util.print("");
		return result;
	}

	// public static Markup description(Perspective[] restrictions) {
	// SortedMap parentalGroups = new TreeMap();
	// for (int i = 0; i < restrictions.length; i++) {
	// Perspective child = restrictions[i];
	// Perspective parent = child.getFacetType();
	// SortedSet children = (SortedSet) parentalGroups.get(parent);
	// if (children == null) {
	// children = new TreeSet();
	// parentalGroups.put(parent, children);
	// }
	// children.add(child);
	// }
	// Markup[] phrases = new Markup[0];
	// Iterator it = parentalGroups.entrySet().iterator();
	//
	// while (it.hasNext()) {
	// Map.Entry entry = (Entry) it.next();
	// Perspective parent = (Perspective) entry.getKey();
	// SortedSet children = (SortedSet) entry.getValue();
	// Perspective[] info = (Perspective[]) (children)
	// .toArray(new Perspective[0]);
	// Markup[] descriptions = new Markup[1];
	// Markup description = new MarkupImplementation();
	// toEnglish(info, " and ", description);
	// Markup result = parent.tagDescription(descriptions, true, null);
	// if (result.size() > 0)
	// phrases = (Markup[]) Util.push(phrases, result, Markup.class);
	// }
	// // Util.print("q.getPhrases return "
	// // + Util.valueOfDeep(phrases));
	//
	// Markup summary = new MarkupImplementation();
	// descriptionNounPhrase(phrases, summary);
	// descriptionClauses(phrases, summary, null, null);
	//
	// return summary;
	// }

	static void descriptionNounPhrase(List phrases, Markup result) {
		for (Iterator it = phrases.iterator(); it.hasNext();) {
			Markup phrase = (Markup) it.next();
			if (phrase.get(0).equals("object")) {
				for (int j = 1; j < phrase.size(); j++) {
					if (phrase.get(j) instanceof Perspective)
						result.add(Markup.PLURAL_TAG);
					result.add(phrase.get(j));
				}
			}
		}
		if (result.size() == 0) {
			result.add(Markup.PLURAL_TAG);
			result.add(Markup.GENERIC_OBJECT_LABEL);
		}
		// if (onCount != 1)
		// for (int i = 0; i < objects.size(); i++)
		// objects[i] = Util.pluralize(objects[i]);
		// result.add(Util.toEnglish(result, " and "));
		// Util.print("descriptionNounPhrase: " + result);
	}

	static void descriptionClauses(List phrases, Markup result,
			Set searches, Set clusters) {
		// Util.print("\nq.descriptionClauses "
		// + Util.valueOfDeep(phrases));
		// Util.printDeep(result);
		for (Iterator it = phrases.iterator(); it.hasNext();) {
			Markup phrase = (Markup) it.next();
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
		for (Iterator it = phrases.iterator(); it.hasNext();) {
			Markup phrase = (Markup) it.next();
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
			result.add(Markup.INCLUDED_COLORS[2]);
			result.add(search);
			result.add(Markup.DEFAULT_COLOR_TAG);
			result.add("'");
		}
		for (Iterator it = clusters.iterator(); it.hasNext();) {
			Cluster cluster = (Cluster) it.next();
			String s;
			switch (cluster.nRestrictions()) {
			case 1:
				s = "that has the tag {";
				break;
			case 2:
				s = "that has both of the tags {";
				break;
			default:
				s = " that have at least " + cluster.quorumSize() + " of the "
						+ cluster.nRestrictions() + " tags {";
				break;
			}
			if (first) {
				result.add(" ");
				first = false;
			} else
				result.add(" and ");
			result.add(s);
			// result.add(Markup.greens[2]);
			toEnglish(cluster.allRestrictions(), ", ", result);
			// result.add(Markup.DEFAULT_COLOR_TAG);
			result.add("}");
		}
		// Util.print(phrases);
	}

	private static final Set emptySet = Collections
			.unmodifiableSet(new HashSet());

	static Markup facetDescription(Perspective facet) {
		Markup[] descriptions = { new MarkupImplementation(), null };
		List phrases = new LinkedList();
		if (facet != null) {
			descriptions[0].add(facet);
			phrases.add(facet.tagDescription(descriptions, true, null));
		}
		Markup summary = new MarkupImplementation();
		// summary.add(" "); // descriptionNounPhrase assumes there is exactly
		// one canned prefix
		descriptionNounPhrase(phrases, summary);
		descriptionClauses(phrases, summary, emptySet, emptySet);
		return summary;
	}

	// static Markup clusterDescription(Cluster facet) {
	// return description(facet.allRestrictions());
	// }

	static Markup restrictionsDescription(SortedSet restrictions) {
		Perspective aRestriction = (Perspective) restrictions.first();
		Perspective parent = aRestriction.getParent();
		Markup content = Query.emptyMarkup();
		String prefix = parent != null ? parent.namePrefix() : "";
		if (prefix.length() > 0)
			content.add(prefix);
		Query.toEnglish(restrictions, " and ", content);

		Markup[] descriptions = new Markup[2];
		boolean[] reqtTypes = { true, false };
		for (int type = 0; type < 2; type++) {
			boolean reqtType = reqtTypes[type];

			SortedSet info = new TreeSet();
			for (Iterator it = restrictions.iterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				info.addAll(p.getRestrictionFacetInfos(true, reqtType));
			}

			if (info != null && !info.isEmpty()) {
				descriptions[type] = Query.emptyMarkup();
				Query.toEnglish(info, " or ", descriptions[type]);
			}
		}
		Markup result = aRestriction.tagDescription(descriptions, false, Util
				.splitSemicolon(" ; NOT "));

		if (result.size() > 0) {
			content.add(Markup.NEWLINE_TAG);
			content.add(aRestriction.namePrefix());
			content.addAll(result);
		}
		return content;
	}

	private static boolean topLevelFacetClause(Markup phrase, Markup result) {
		for (int j = 1; j < phrase.size(); j++) {
			Object o = phrase.get(j);
			if (o instanceof Perspective) {
				Perspective facet = (Perspective) o;
				if (facet.getParent() == null) {
					result
							.add(" having"
									+ Util.indefiniteArticle(facet
											.getNameIfPossible()));
					result.add(facet);
					return true;
				}
			}
		}
		return false;
	}

	public String toText() {
		return toText(null);
	}

	public String toText(PerspectiveObserver _redraw) {
		boolean plural = false;
		StringBuffer result = new StringBuffer();
		for (Iterator iterator = iterator(); iterator.hasNext();) {
			Object o = iterator.next();
			if (o == Markup.PLURAL_TAG) {
				assert !plural;
				plural = true;
			} else if (o == Markup.NEWLINE_TAG) {
				result.append("\n");
			} else if (o instanceof String) {
				assert !plural : this;
				result.append(o);
			} else if (o instanceof ItemPredicate) {
				ItemPredicate facet = (ItemPredicate) o;
				String name = _redraw == null ? facet.getNameIfPossible()
						: facet.getName(_redraw);
				result.append(name);
				if (plural) {
					Util.pluralize(result);
					plural = false;
				}
			} else {
				assert o == Markup.DEFAULT_COLOR_TAG
						|| o == Markup.DEFAULT_STYLE_TAG
						|| o == Markup.ITALIC_STRING_TAG || o instanceof Color : o;
			}
		}
		return result.toString();
	}

	static void toEnglish(SortedSet info, String connector, Markup descriptions) {
		int len = info.size();
		if (len > 0) {
			// if (len > 1)
			// Arrays.sort(info); // , Perspective.indexComparator);
			boolean first = true;
			for (Iterator it = info.iterator(); it.hasNext();) {
				Object o = it.next();
				if (first) {
					first = false;
				} else if (o == info.last()) {
					descriptions.add(connector);
				} else {
					descriptions.add(", ");
				}
				descriptions.add(o);
			}
		}
	}

	public Markup copy() {
		Markup result = new MarkupImplementation();
		result.addAll(this);
		return result;
	}

}
