package edu.cmu.cs.bungee.client.query;

import java.io.DataInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.CollationKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import edu.cmu.cs.bungee.javaExtensions.AccumulatingQueueThread;
import edu.cmu.cs.bungee.javaExtensions.IntHashtable;
import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.QueueThread;
import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * @author mad Holds a set of filters that has been applied to a database, and
 *         all the associated facet and item information
 */
public final class Query implements ItemPredicate {

	/**
	 * Allow adding, deleting, reparenting, renaming facets and changing the
	 * items they apply to?
	 */
	private boolean isEditable = false;

	private final ServletInterface db;

	private static final Object ITEM_ORDER = "item_order";

	private static final Object RESTRICTED = "restricted";

	private Object baseTable = ITEM_ORDER;

	// private String itemURLgetter;

	/**
	 * Mouse documentation for what happens if you click on the Selected Item
	 * thumbnail. The empty string disables clicking.
	 */
	public final String itemURLdoc;

	// private final String[] itemDescFields;

	private final String matchFieldsPrefix;

	/**
	 * What you call an item, e.g. 'image', 'work', etc
	 */
	public final String genericObjectLabel;

	private final List displayedPerspectives = new ArrayList();

	/**
	 * Number of top-level facets
	 */
	public final int nAttributes;

	/**
	 * the number of items that satisfy the current filters
	 */
	int onCount;

	/**
	 * the number of items in the [restricted] collection
	 */
	int totalCount;

	private final Set searches = new LinkedHashSet();

	private final Set clusters = new LinkedHashSet();

	private final Set itemLists = new LinkedHashSet();

	private Perspective[] allPerspectives;

	/**
	 * Called in thread Bungee.dataUpdater
	 * 
	 * @param server
	 *            the Bungee View server to connect to, e.g.
	 *            http://localhost/bungeeOLD/Bungee
	 * @param dbName
	 *            the database to connect to, e.g. movie
	 */
	public Query(String server, String dbName) {
		db = new ServletInterface(server, dbName);
		String[][] dbs = db.getDatabases();
		for (int i = 0; i < dbs.length && name == null; i++) {
			if (dbName == null || dbName.length() == 0
					|| dbs[i][0].equalsIgnoreCase(dbName)) {
				name = dbs[i][1];
			}
		}
		assert name != null : dbName;
		// setQueryInvalid();
		int nFacets = db.facetCount + 1;
		totalCount = db.itemCount;
		onCount = totalCount;
		matchFieldsPrefix = getMatchFieldsPrefix(Util
				.splitComma(db.itemDescriptionFields));
		genericObjectLabel = Util.pluralize(db.label);
		itemURLdoc = db.doc;
		allPerspectives = new Perspective[nFacets];
		nAttributes = initPerspectives();
		isEditable = db.isEditable;
		// Util.print("Query return");
	}

	private static String getMatchFieldsPrefix(String[] itemDescFields) {
		// matchFieldsPrefix = new String[2];
		String s = " MATCH(it.facet_names";
		for (int i = 0; i < itemDescFields.length; i++)
			s += ",it." + itemDescFields[i];
		s += ") AGAINST (";
		// matchFieldsPrefix[0] = " WHERE" + s;
		return " AND" + s;
	}

	/**
	 * @return status of most recent servlet response
	 */
	public String errorMessage() {
		return db.errorMessage();
	}

	/**
	 * @return the number of filters on clusters
	 */
	public int nClusters() {
		return clusters.size();
	}

	/**
	 * @return the number of Informedia queries
	 */
	public int nItemLists() {
		return itemLists.size();
	}

	/**
	 * @return the clusters being filtered on
	 */
	public Set clusters() {
		return new HashSet(clusters);
	}

	/**
	 * @return the Informedia queries being filtered on
	 */
	public Set itemLists() {
		return new HashSet(itemLists);
	}

	/**
	 * @param cluster
	 * @return is cluster being filtered on?
	 */
	public boolean usesCluster(Cluster cluster) {
		return clusters.contains(cluster);
	}

	// /**
	// * @param name1
	// * @return a cluster being filtered on with this name, or null
	// */
	// public Cluster findCluster(String name1) {
	// for (Iterator it = clusters.iterator(); it.hasNext();) {
	// Cluster cluster = (Cluster) it.next();
	// if (cluster.toString().equals(name1)) {
	// return cluster;
	// }
	// }
	// return null;
	// }

	Prefetcher prefetcher;

	/**
	 * clean up when this query is no longer needed
	 */
	public void exit() {
		Util.print("...exiting Query priority="
				+ Thread.currentThread().getPriority());
		if (prefetcher != null) {
			prefetcher.exit();
			prefetcher = null;
		}
		if (nameGetter != null) {
			nameGetter.exit();
			nameGetter = null;
		}
		db.close();
		setQueryValid(); // Don't have things hanging around waiting; let
		// them get an error.
	}

	private List perspectivesToAdd = new LinkedList();

	private List perspectivesToRemove = new LinkedList();

	private Set orderedFacetTypes = new HashSet();

	NameGetter nameGetter;

	/**
	 * @return a Markup with no elements
	 */
	public static Markup emptyMarkup() {
		return new MarkupImplementation();
	}

	public String markupToText(Markup markup, PerspectiveObserver _redraw) {
		return markup.compile(genericObjectLabel).toText(_redraw);
	}

	public Markup parentDescription() {
		Markup result = Query.emptyMarkup();
		MarkupImplementation.descriptionNounPhrase(new LinkedList(), result);
		// return result.compile(getQuery().genericObjectLabel);
		return result;
	}

	/**
	 * @param restrictions
	 * @param connector
	 *            e.g. 'and' or 'or'
	 * @param descriptions
	 *            append f1, f2, ..., fn-1 connector fn to description
	 */
	public static void toEnglish(SortedSet restrictions, String connector,
			Markup descriptions) {
		MarkupImplementation.toEnglish(restrictions, connector, descriptions);
	}

	private void descriptionClauses(List phrases, Markup result) {
		MarkupImplementation.descriptionClauses(phrases, result, searches,
				clusters, itemLists);
	}

	/**
	 * @return e.g. '... for works from 20th century.'
	 */
	public Markup descriptionVerbPhrase() {
		Markup summary;
		if (isRestricted()) {
			summary = description();
			summary.add(0, "... for ");
			summary.add(".");
		} else {
			summary = emptyMarkup();
			summary.add("(No filters applied.)");
		}
		// Util.print("description: " + summary);
		return summary;
	}

	public String toString() {
		// Util.printStackTrace();
		return (isQueryValid() ? "<Query " : "Invalid Query ") + getName()
				+ ">";
	}

	/**
	 * @return e.g. 'works from 20th century.'
	 */
	public Markup description() {
		Markup summary;
		// if (isRestricted()) {
		// Util.print("query.description");
		List phrases = getPhrases();
		summary = emptyMarkup();
		MarkupImplementation.descriptionNounPhrase(phrases, summary);
		descriptionClauses(phrases, summary);
		// } else {
		// summary = parentDescription();
		// }
		// Util.print("description: " + summary);
		return summary;
	}

	public List getPhrases() {
		List phrases = new LinkedList();
		for (Iterator iterator = displayedPerspectives.iterator(); iterator
				.hasNext();) {
			Perspective p = (Perspective) iterator.next();
			if (p.getParent() == null) {
				Markup description = p.getDescription(true, null);
				if (!description.isEmpty())
					phrases.add(description);
			}
		}
		// Util.print("q.getPhrases return "
		// + Util.valueOfDeep(phrases));
		return phrases;
	}

	/**
	 * @param text
	 *            include text search filters?
	 * @param facet
	 *            include filters on facets?
	 * @param cluster
	 *            include filters on clusters?
	 * @return this query's number of filters
	 */
	public int nFilters(boolean text, boolean facet, boolean cluster,
			boolean itemList) {
		int result = text ? searches.size() : 0;
		if (cluster)
			result += nClusters();
		if (facet) {
			for (Iterator iterator = displayedPerspectives.iterator(); iterator
					.hasNext();) {
				Perspective p = (Perspective) iterator.next();
				if (p.getParent() == null && p.isAnyRestrictions()) {
					result++;
					// Util.print("filter on " + p);
				}
			}
		}
		if (itemList)
			result += nItemLists();
		return result;
	}

	/**
	 * @return description of this query's number and types of filters
	 */
	public String describeNfilters() {
		int nText = nFilters(true, false, false, false);
		int nFacet = nFilters(false, true, false, false);
		StringBuffer buf = new StringBuffer();
		int nClusters = nClusters();
		int nItemLists = nItemLists();
		buf.append("filters on ");
		if (nFacet > 0) {
			buf.append(nFacet).append(" categories");
			if (nText > 0 || nClusters > 0 || nItemLists > 0)
				buf.append(" and ");
		}
		if (nText > 0) {
			buf.append(nText).append(" keywords");
			if (nClusters > 0 || nItemLists > 0)
				buf.append(" and ");
		}
		if (nClusters > 0)
			buf.append(" and ").append(nClusters).append(" clusters");
		if (nItemLists > 0)
			buf.append(" and ").append(nItemLists)
					.append(" Informedia queries");
		buf.append(".");
		// buf.append(".)");
		return buf.toString();
	}

	/**
	 * @param restrictions
	 *            the restrictions on a facet, like {2006, 2007}
	 * @return a rank label, like ' ->21st century\n ->2006 and 2007'
	 */
	public static Markup facetSetDescription(SortedSet restrictions) {
		return MarkupImplementation.restrictionsDescription(restrictions);
	}

	boolean isTopLevel(int facetID) {
		return facetID <= nAttributes;
	}

	/**
	 * @param name1
	 *            name of facet to find
	 * @param isTopLevelOnly
	 *            does facet have to have no parent?
	 * @return the facet with this name Called only when replaying or editing
	 */
	public Perspective findUsedPerspective(String name1, boolean isTopLevelOnly) {
		int nPerspectives = displayedPerspectives.size();
		for (int i = nPerspectives - 1; i >= 0; i--) {
			Perspective p = (Perspective) displayedPerspectives.get(i);
			if (p.getName().equals(name1)
					&& (!isTopLevelOnly || p.depth() == 0))
				return p;
		}
		return null;
	}

	/**
	 * @param name1
	 *            name of facet to find
	 * @param parent
	 *            parent of facet to find
	 * @return the facet with this name and parent Called only when replaying or
	 *         editing
	 */
	public Perspective findPerspective(String name1, Perspective parent) {
		if (parent != null) {
			for (Iterator it = parent.getChildIterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (p.getName().equals(name1))
					return p;
			}
		} else {
			return findUsedPerspective(name1, true);
		}
		return null;
	}

	/**
	 * @return a ListIterator over the displayed perspectives
	 */
	public ListIterator perspectivesIterator() {
		return displayedPerspectives.listIterator();
	}

	/**
	 * @return Ensure there's a [displayed] Perspective for all facet's
	 *         ancestors, and then tell its parent to toggle it.
	 */
	public boolean toggleFacet(Perspective facet, int modifiers) {
		Perspective parent = facet.getParent();
		if (!Perspective.isExcludeAction(modifiers)
				&& !parent.isRestriction(true) && !parent.isTopLevel()) {
			toggleFacet(parent, modifiers);
		}
		parent.displayAncestors();
		boolean result = parent.toggleFacet(facet, modifiers);
		isRestricted = computeIsRestricted();
		return result;
	}

	public SortedSet allRestrictions() {
		SortedSet result = new TreeSet();
		for (Iterator iterator = displayedPerspectives.iterator(); iterator
				.hasNext();) {
			Perspective p = (Perspective) iterator.next();
			result.addAll(p.allRestrictions());
		}
		return result;
	}

	/**
	 * @param required
	 *            polarity of filters we're interested in
	 * @return all facets filtered on with required polarity
	 */
	public SortedSet allRestrictions(boolean required) {
		SortedSet result = new TreeSet();
		for (Iterator iterator = displayedPerspectives.iterator(); iterator
				.hasNext();) {
			Perspective p = (Perspective) iterator.next();
			result.addAll(p.restrictions(required));
		}
		return result;
	}

	public boolean isRestriction(ItemPredicate facet, boolean required) {
		boolean result = false;
		for (Iterator iterator = displayedPerspectives.iterator(); iterator
				.hasNext();) {
			Perspective p = (Perspective) iterator.next();
			result = p.isRestriction(facet, required);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see query.ItemPredicate#isRestriction()
	 */
	public boolean isRestriction() {
		return false;
	}

	private boolean isRestricted;

	/**
	 * @return allRestrictions() > 0
	 */
	public boolean isRestricted() {
		return isRestricted;
	}

	/**
	 * @return allRestrictions() > 0 (i.e. ignores restrictData)
	 */
	private boolean computeIsRestricted() {
		// Wrong - we want to know if there are restrictions before querying the
		// database.
		// return onCount < totalCount;
		boolean result = searches.size() > 0 || nClusters() > 0
				|| nItemLists() > 0;
		for (Iterator it = displayedPerspectives.iterator(); it.hasNext()
				&& !result;) {
			Perspective p = (Perspective) it.next();
			result = p.isRestricted();
		}
		return result;
	}

	void addPerspective(ItemPredicate p) {
		if (perspectivesToRemove.contains(p))
			perspectivesToRemove.remove(p);
		else
			perspectivesToAdd.add(p);
	}

	void removePerspective(ItemPredicate p) {
		if (perspectivesToAdd.contains(p))
			perspectivesToAdd.remove(p);
		else
			perspectivesToRemove.add(p);
	}

	private int initPerspectives() {
		int facetTypeID = 0;
		ResultSet rs = null;

		prefetcher = new Prefetcher(this);
		prefetcher.start();

		nameGetter = new NameGetter(this);
		nameGetter.start();

		try {
			rs = db.initPerspectives();
			while (rs.next()) {

				Perspective p = new Perspective(++facetTypeID, null, rs
						.getString(1), rs.getInt(5), rs.getInt(4), rs
						.getString(2), rs.getString(3), this);
				cachePerspective(facetTypeID, p);
				// Perspective p = ensurePerspective(++facet_type_id, null, rs
				// .getString(1), rs.getInt(5), rs.getInt(4));
				// p.instantiate(rs.getString(2), rs.getString(3), this);
				displayedPerspectives.add(p);
				p.totalCount = rs.getInt(6);
				if (rs.getInt(7) != 0)
					orderedFacetTypes.add(p);
				prefetcher.add(p);
			}

			initFacetTypes(displayedPerspectives);

		} catch (Throwable se) {
			Util.err("SQL Exception in initPerspectives: " + se.getMessage());
			se.printStackTrace();
		} finally {
			close(rs);
		}
		return facetTypeID;
	}

	boolean isOrdered(Perspective p) {
		return orderedFacetTypes.contains(p);
	}

	private String[][] joinStrings;

	private String getJoinString(int joinIndex, boolean required) {
		if (joinStrings == null || joinIndex >= joinStrings.length) {
			joinStrings = new String[joinIndex + 10][2];
			for (int i = 0; i < joinIndex + 10; i++) {
				String s = " JOIN item_facet_heap i" + i
						+ " ON rnd.record_num = i" + i + ".record_num AND i"
						+ i + ".facet_id ";
				joinStrings[i][0] = " INNER" + s;
				joinStrings[i][1] = " LEFT" + s;
			}
		}
		return joinStrings[joinIndex][required ? 0 : 1];
	}

	// First update onItems. Second column is an autoincrement.
	//
	// TRUNCATE TABLE onItems;
	//
	// INSERT INTO onItems
	//
	// SELECT record_num, NULL FROM item it
	// WHERE MATCH(it.facet_names, ...) AGAINST ('France' IN BOOLEAN MODE)
	// AND MATCH(it.facet_names, ...) AGAINST ('Italy' IN BOOLEAN MODE)
	// ...
	//
	// OR <rnd is only used to restrict the possible items, so the queries could
	// be
	// optimized a little to leave this out in the normal case, but it would be
	// messy work>
	//
	// SELECT [DISTINCT] rnd.record_num, NULL FROM [item_order | restricted]
	// rnd
	// 
	// INNER JOIN item_facet_heap i0 ON rnd.record_num = i0.record_num
	// [AND i0 | INNER JOIN mult m0 ON i0.facet_id = m0.facet_id AND m0.]
	// .facet_id [= | IN (] <facet_id(s)> [)]
	//
	// LEFT JOIN item_facet_heap i1 ON rnd.record_num = i1.record_num AND ...
	// ...
	// [INNER JOIN item it ON rnd.record_num = it.record_num
	// AND MATCH(it.facet_names, ...) AGAINST ('France' IN BOOLEAN MODE)
	// AND MATCH(it.facet_names, ...) AGAINST ('Italy' IN BOOLEAN MODE)
	// ...]
	// [WHERE i1.record_num IS NULL]
	//
	// ORDER BY random_ID;
	//
	// True iff we need to update facet counts (i.e. unless onCount == 0 or
	// totalCount)

	/**
	 * Called in thread Bungee.dataUpdater
	 * 
	 * @param selectedItem
	 *            tell database what item we want to keep track of
	 * @param nNeighbors
	 *            tell database how many neighbors of selectedItem we want to
	 *            keep track of
	 * @return does Bungee View need to update facet counts (i.e. 0 < onCount <
	 *         totalCount)
	 */
	public boolean updateOnItems(Item selectedItem, int nNeighbors) {
		// Util.print("Query updateOnItems " + selectedItem);
		isRestricted = computeIsRestricted();
		String onSQL = onItemsQuery(null);
		// Util.print("onSQL: " + onSQL);
		onCount = db.updateOnItems(onSQL, selectedItem != null ? selectedItem
				.getId() : 0, onItemsTable(), nNeighbors);
		if (onCount >= 0)
			return (onCount > 0);
		else {
			onCount = totalCount;
			return false;
		}
		// } else {
		// db.decacheOffsets();
		// onCount = totalCount;
		// return false;
		// }
	}

	// 0 item_order; 1 restricted; 2 onItems
	int onItemsTable() {
		return isRestricted() ? 2 : isRestrictedData() ? 1 : 0;
	}

	void adjoin(StringBuffer qJOIN, SortedSet exclude, boolean reqtType,
			int joinIndex) {
		qJOIN.append(getJoinString(joinIndex, reqtType));
		if (exclude.size() > 1) {
			qJOIN.append("IN (");
			boolean first = true;
			for (Iterator it = exclude.iterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (first)
					first = false;
				else
					qJOIN.append(",");
				qJOIN.append(p.getID());
			}
			qJOIN.append(")");
		} else {
			qJOIN.append("= ");
			qJOIN.append(((Perspective) exclude.first()).getID());
		}
	}

	/**
	 * @param toIgnore
	 *            ignore restrictions on this ItemPredicate. Used by
	 *            PerspectiveList.
	 * @return SQL rendering of the current query, or null if there are no
	 *         restrictions.
	 */
	String onItemsQuery(ItemPredicate toIgnore) {
		boolean needDistinct = false;
		StringBuffer qJOIN = new StringBuffer(
				displayedPerspectives.size() * 100);

		if (searches.size() > 0) {
			qJOIN
					.append(" INNER JOIN item it ON rnd.record_num = it.record_num");
			int i = 0;
			for (Iterator it = searches.iterator(); it.hasNext(); i++) {
				String search = (String) it.next();
				qJOIN.append(matchFieldsPrefix);
				qJOIN.append(JDBCSample.quote(search));
				qJOIN.append(" IN BOOLEAN MODE)");
			}
		}

		int joinIndex = 0;
		for (Iterator it = perspectivesIterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (p.getParent() == null) {
				SortedSet restrictions = p
						.getRestrictionFacetInfos(false, true);
				if (toIgnore != null) {
					Set toRemove = new HashSet();
					for (Iterator rit = restrictions.iterator(); rit.hasNext();) {
						Perspective r = (Perspective) rit.next();
						if (r.hasAncestor(toIgnore))
							toRemove.add(r);
					}
					restrictions.removeAll(toRemove);
				}
				if (!restrictions.isEmpty()) {
					if (restrictions.size() > 1)
						needDistinct = true;
					adjoin(qJOIN, restrictions, true, joinIndex++);
				}
			}
		}
		int minClusterSize = 1;
		if (nClusters() > 0)
			for (Iterator it = clusters.iterator(); it.hasNext();) {
				Cluster cluster = (Cluster) it.next();
				adjoin(qJOIN, cluster.allRestrictions(), true, joinIndex++);
				minClusterSize *= cluster.quorumSize();
				needDistinct = false;
			}
		SortedSet exclude = allRestrictions(false);
		if (exclude.size() > 0) {
			adjoin(qJOIN, exclude, false, joinIndex);
			qJOIN.append(" WHERE i").append(joinIndex).append(
					".record_num IS NULL");
		}
		if (nItemLists() > 0) {
			qJOIN.append(exclude.size() > 0 ? " AND" : " WHERE").append(
					" rnd.record_num IN (").append(
					((ItemList) itemLists.toArray()[0]).getItems()).append(")");
		}
		String result = null;
		if (qJOIN.length() > 0) {
			if (minClusterSize > 1)
				qJOIN.append(" GROUP BY rnd.record_num HAVING COUNT(*) >= ")
						.append(minClusterSize);
			// isRestricted = true;

			result = (needDistinct ? "SELECT DISTINCT" : "SELECT")
					+ " rnd.record_num" + " FROM " + baseTable + " rnd" + qJOIN; // + "
			// ORDER
			// BY
			// random_ID";
		}
		// Util.print("query: " + result);
		return result;
	}

	private ResultSet getFilteredCounts() {
		ResultSet rs = db.getFilteredCounts(
				getFilteredCountsInternal(perspectivesToAdd),
				getFilteredCountsInternal(perspectivesToRemove));
		return rs;
	}

	private ResultSet getFilteredCountTypes() {
		return db.getFilteredCountTypes();
	}

	String getFilteredCountsInternal(List persps) {
		if (persps.size() == 0)
			return null;
		boolean isFirst = true;
		StringBuffer buf = new StringBuffer(persps.size() * 7);
		for (Iterator it = persps.iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (isFirst)
				isFirst = false;
			else
				buf.append(", ");
			buf.append(p.getID());
		}
		persps.clear();
		return buf.toString();
	}

	/**
	 * @param p
	 * @return what would p's children's onCounts be if there were no filters on
	 *         p? rows are [Perspective, onCount] If returned value is null, no
	 *         other Perspective is restricted; use totalCounts
	 */
	public ResultSet getCountsIgnoringFacet(Perspective p) {
		String subQuery = onItemsQuery(p);
//		 Util.print(" getCountsIgnoringFacet Query=" + p + " " + subQuery);
		if (subQuery != null) {
			ResultSet result = db.getCountsIgnoringFacet(subQuery, p.getID());
			assert result != null;
			return result;
		}
		return null;
	}

	// ResultSet init() {
	// return db.init();
	// }

	/**
	 * remove all filters
	 */
	public void clear() {
		int nPerspectives = displayedPerspectives.size();
		for (int i = nPerspectives - 1; i >= 0; i--) {
			Perspective p = (Perspective) displayedPerspectives.get(i);
			if (p.getParent() == null)
				clearPerspective(p);
		}
		searches.clear();
		clusters.clear();
		itemLists.clear();
		isRestricted = computeIsRestricted();
	}

	void clearPerspective(Perspective p) {
		// Util.print("clearPerspective " + p);
		for (int i = displayedPerspectives.size() - 1; i >= 0; i--) {
			Perspective child = (Perspective) displayedPerspectives.get(i);
			if (child.getParent() == p) {
				removeRestriction(child);
				clearPerspective(p);
				// multiple perspectives may be deleted from
				// displayedPerspectives,
				// so just start over;
				return;
			}
		}
		p.clearRestrictions();
		// Don't want stale numbers in mouse doc from SelectedItem deeply
		// nested facets.
		if (p.isPrefetched()) {
			synchronized (childIndexesBusy) {
				// prefetching can call this if restrictedData and all child
				// counts
				// = 0
				// in which case resetData will barf cause not prefetched yet
				p.resetData(-1);
			}
		}
	}

	/**
	 * Perspective has already removed the restriction.
	 * 
	 * @param p
	 */
	void removeRestriction(Perspective p) {
		// Util.print("removeRestriction " + p + " " + usesPerspective(p));
		removeRestrictionInternal(p);

		// why do we do this?
		if (p.getParent() != null
				&& p.getParent().getParent() != null
				&& !p.getParent().isRestricted()
				&& !p.getParent().getParent()
						.isRestriction(p.getParent(), true)) {
			Util.print("removeRestriction of parent " + p.parent);
			removeRestriction(p.getParent());
		}
	}

	public void removeRestrictionInternal(Perspective p) {
		if (displaysPerspective(p)) {
			// Util.print("removeRestrictionInternal " + p);
			assert SwingUtilities.isEventDispatchThread() : Util
					.printStackTrace();
			displayedPerspectives.remove(p);
			assert !displayedPerspectives.isEmpty() : this;
			removePerspective(p);
			clearPerspective(p);
		}
	}

	/**
	 * @param s
	 *            add a filter on s
	 */
	public void addTextSearch(String s) {
		// Util.print("Query.addTextSearch");
		searches.add(s);
		isRestricted = computeIsRestricted();
	}

	/**
	 * @param s
	 *            filter to remove
	 * @return was anything removed?
	 */
	public boolean removeTextSearch(String s) {
		boolean result = searches.remove(s);
		isRestricted = computeIsRestricted();
		return result;
	}

	/**
	 * @return the set of Strings being filtered on
	 */
	public Set getSearches() {
		return searches;
	}

	/**
	 * @return the number of text search filters
	 */
	public int nSearches() {
		return searches.size();
	}

	void initFacetTypes(List facetTypes) {
		ResultSet rs = null;
		try {
			rs = db.init();
			// Util.print("Query.updateData " + isTotal + " " +
			// (newPerspective != null ? newPerspective.facet_type_name : "")
			// + " " + isHighlight);
			for (Iterator it = facetTypes.iterator(); it.hasNext();) {
				Perspective facetType = (Perspective) it.next();
				int fetchType = 0;
				facetType.initPerspective(rs, fetchType);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			close(rs);
		}
		perspectivesToAdd = new LinkedList(facetTypes);
	}

	/**
	 * @param facet
	 *            refresh facet counts after an edit operation
	 */
	public void refetch(Perspective facet) {
		assert isEditable;
		extendAllPerspectives(facet.childrenOffset() + facet.nChildren());
		facet.resetData(0);
		int fetchType = 1;
		prefetch(facet, fetchType);
	}

	// /**
	// * Can be called from thread prefetcher
	// */
	// void initPerspective(Perspective p, int fetchType) {
	// initPerspective(db.prefetch(p, fetchType), p,
	// fetchType > 4 ? fetchType - 4 : fetchType);
	// }

	void prefetch(Perspective p, int fetchType) {
		DataInputStream in = db.prefetch(p, fetchType);
		p.initPerspective(in, fetchType);
		db.closeNcatch(in, "prefetch", null);
	}

	void waitForPrefetch(Perspective facet) {
		// Util.print("waitForPrefetch");
		synchronized (facet) {
			while (!facet.isPrefetched()) {
				try {
					facet.wait();
				} catch (InterruptedException e) {
					// Our wait is over
				}
			}
		}
		// Util.print("....waitForPrefetch return");
	}

	ResultSet getLetterOffsets(Perspective facet, CollationKey prefix) {
		return db.getLetterOffsets(facet, prefix.getSourceString());
	}

	final Object childIndexesBusy = "childIndexesBusy";

	/**
	 * Update all facet onCounts. Called in thread Bungee.dataUpdater
	 * 
	 * @param resetOnly
	 *            onCount == 0 or totalCount; no need to query database
	 */
	public void updateData(boolean resetOnly) {
//		 Util.print("Query.updateData " + resetOnly);
		ResultSet counts = null;
		ResultSet typeCounts = null;
		try {
			if (!resetOnly) {
				counts = getFilteredCounts();
				typeCounts = getFilteredCountTypes();
			}
			synchronized (childIndexesBusy) {
				// Util.print("updateData " + resetOnly + " "
				// + displayedPerspectives.size());
				// Don't use Iterator here. If query is being modified it will
				// barf, and we know we'll be called again, so it doesn't matter
				// if we skip anything.
				for (int i = 0; i < displayedPerspectives.size(); i++) {
					Perspective p = (Perspective) displayedPerspectives.get(i);
					// Util.print(" " + p + " " + p.isPrefetched());
					if (!p.isPrefetched())
						waitForPrefetch(p);
					p.resetData(0);
				}
				if (!resetOnly) {
					// long start = new Date().getTime();
					updateDataInternal(counts);
					updateDataInternal(typeCounts);
				}
				// Util.print(" updateData done");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (counts != null)
				close(counts);
			if (typeCounts != null)
				close(typeCounts);
			// setQueryValid();
			// updateBigDeal();
		}
//		 Util.print("...updateData return");
	}

	private void updateDataInternal(ResultSet rs) {
		if (rs != null) {
			// Will be null if onCount = 0 or onCount = totalCount

			// Map cHist = new TreeMap();
			// Map fHist = new TreeMap();
			// int prevF = 0;

			try {
				// Perspective prevPerspective = null;
				// int cumCount = 0;
				while (rs.next()) {
					int pID = rs.getInt(1);

					// incf(fHist, pID - prevF);
					// prevF = pID;
					// incf(cHist, rs.getInt(2));

					Perspective v = findPerspective(pID);
					assert v != null : "Can't find perspective " + pID;
					// if (v.parent != prevPerspective) {
					// if (prevPerspective != null)
					// prevPerspective.setTotalChildOnCount(cumCount);
					// prevPerspective = v.parent;
					// cumCount = 0;
					// }
					int count = Util.min(v.totalCount, rs.getInt(2));
					// not if restricted.
					assert 0 <= count && count <= v.totalCount : count + " "
							+ v;
					v.onCount = count;
//					if ("1993".equals(v.getNameIfPossible()))
//					 Util.print("Setting " + v + ".onCount=" + count);
					// cumCount += count;
				}
				// if (prevPerspective != null)
				// prevPerspective.setTotalChildOnCount(cumCount);
			} catch (SQLException se) {
				Util.err("SQL Exception in perspective.updateData: "
						+ se.getMessage());
				se.printStackTrace();
			}

			// showHist(cHist, "Counts:");
			// showHist(fHist, "\nDelta Facets:");

		}
	}

	// private double positiveBigDeal;
	// private double negativeBigDeal;
	//
	// void updateBigDeal() {
	// double expectedPercent = percentOn();
	// positiveBigDeal = Perspective.unwarp(0.6, expectedPercent);
	// negativeBigDeal = Perspective.unwarp(0.4, expectedPercent);
	// for (Iterator it = displayedPerspectives.iterator(); it.hasNext();) {
	// Perspective facetType = (Perspective) it.next();
	// facetType.computeBigDeals();
	// }
	// }
	//
	// boolean isBigDeal(double obervedPercent) {
	// return obervedPercent > positiveBigDeal
	// || obervedPercent < negativeBigDeal;
	// }

	// void showHist(Map map, String label) {
	// Util.print("\n" + label);
	// int total = 0;
	// Iterator it = map.keySet().iterator();
	// while (it.hasNext()) {
	// Integer Key = (Integer) it.next();
	// int key = Key.intValue();
	// int value = ((Integer) map.get(Key)).intValue();
	// total += value;
	// Util.print(key + "\t" + value);
	// }
	// Util.print("Total\t" + total);
	// }
	//
	// Object incf(Map map, int key, int increment) {
	// Integer Key = new Integer(key);
	// Integer old = (Integer) map.get(Key);
	// if (old != null)
	// increment += old.intValue();
	// return map.put(Key, new Integer(increment));
	// }
	//
	// Object incf(Map map, int key) {
	// return incf(map, key, 1);
	// }

	void insertPerspective(Perspective p) {
		assert SwingUtilities.isEventDispatchThread() : Util.printStackTrace();
		boolean added = false;
		assert !displayedPerspectives.contains(p) : displayedPerspectives;
		for (ListIterator it = displayedPerspectives.listIterator(); it
				.hasNext();) {
			Perspective inList = (Perspective) it.next();
			if (inList.childrenOffset() > p.childrenOffset()) {
				it.previous();
				it.add(p);
				added = true;
				break;
			}
		}
		if (!added)
			displayedPerspectives.add(p);

		addPerspective(p);
		if (!p.isPrefetched()) {
			p.ensureInstantiatedPerspective();
			queuePrefetch(p);
			// prefetchData(p);
		}
	}

	public Collection displayedPerspectives() {
		return displayedPerspectives;
	}

	public boolean displaysPerspective(Perspective p) {
		return displayedPerspectives.contains(p);
	}

	public void close(ResultSet rs) {
		try {
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// offsets are zero-based, but db is one-based.
	// does not retrieve element maxOffset
	public ResultSet offsetItems(int minOffset, int maxOffset) {
		// Util.print("offsetItems " + minOffset + " " + maxOffset + " " +
		// isRestricted());
		return db.offsetItems(minOffset, maxOffset, onItemsTable());
	}

	public void reorderItems(int facet_id) {
		db.reorderItems(facet_id);
	}

	public ResultSet[] getThumbs(SortedSet items, int imageW, int imageH,
			int quality) {
		StringBuffer itemIDs = new StringBuffer();
		for (Iterator it = items.iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			if (itemIDs.length() > 0)
				itemIDs.append(",");
			itemIDs.append(item.getId());
		}
		return db.getThumbs(itemIDs.toString(), imageW, imageH, quality);
	}

	// public ResultSet getThumbSizes(int facet) {
	// return db.getThumbSizes(facet);
	// }

	public ResultSet getDescAndImage(Item item, int imageW, int imageH,
			int quality) {
		return db.getDescAndImage(item.getId(), imageW, imageH, quality);
	}

	public int itemIndex(Item item, int nNeighbors) {
		return db.itemIndex(item.getId(), onItemsTable(), nNeighbors);
	}

	public int[] itemIndexFromURL(String URL) {
		return db.itemIndexFromURL(URL, onItemsTable());
	}

	/**
	 * @param item
	 * @return rows are [parent_facet_id, f.facet_id, name, n_child_facets,
	 *         first_child_offset, n_items]
	 */
	ResultSet getItemInfo(Item item) {
		return db.getItemInfo(item.getId());
	}

	public String getItemURL(Item item) {
		if (itemURLdoc != null && itemURLdoc.length() > 0)
			return db.getItemURL(item.getId());
		else
			return null;
	}

	public boolean isShortSearch() {
		for (Iterator iter = searches.iterator(); iter.hasNext();) {
			String searchText = (String) iter.next();
			if (isShortSearch(searchText) != null)
				return true;
		}
		return false;
	}

	public static String isShortSearch(String searchText) {
		if (searchText.matches(".*[^a-zA-Z0-9 \t'_+\\-*()\"].*"))
			return "Use only letters, digits, space, tab, _, ', +, -, *, (, ), and \".";
		if (searchText.matches(".*(-|\\+)[^a-zA-Z0-9'_()\"].*"))
			return "+ and - must immediately precede a word, parenthesis, or quotation mark";
		if (searchText.matches(".*[^a-zA-Z0-9'_]\\*.*"))
			return "* must immediately follow a word";
		String quote = "\\G(-|\\+)?(\".+?\")(\\s++|\\z)";
		String paren = "\\G(-|\\+)?(\\(.+?\\))(\\s++|\\z)";
		String word = "\\G(-|\\+)?(\\w*+)(\\*)?(?:\\s++|\\z)";
		String error = "\\G(\\S++)(\\s*+)(\\s*+)";
		Pattern term = Pattern.compile(quote + "|" + paren + "|" + word + "|"
				+ error);
		boolean positive = false;
		Matcher m = term.matcher(searchText);
		while (m.find()) {
			int groupOffset = 0;
			while (m.group(2 + groupOffset) == null && groupOffset <= 9)
				groupOffset += 3;
			boolean negative = "-".equals(m.group(1 + groupOffset));
			boolean stem = "*".equals(m.group(3 + groupOffset));
			// boolean stem = m.group(4 + groupOffset) != null;
			String s = m.group(2 + groupOffset);
			// Util.print(groupOffset + " " + s + " " + negative);
			if (groupOffset == 9)
				return "Illegal construct: " + m.group(1 + groupOffset);
			if (s.length() > 0) {
				if (!negative)
					positive = true;
				if (s.length() < 4 && !stem)
					return "Search words must have at least 4 characters: " + s;
				if (s.matches("\".*[+\\-*()].*"))
					return "Use only letters, digits, _, and ' inside quotation marks.";
			}
		}
		if (!positive)
			return "You have to have at least one term that doesn't have a '-' in front of it.";
		return null;
	}

	Perspective ensurePerspective(int facet, Perspective _parent, String name1,
			int children_offset, int n_children) {
		Perspective result = findPerspectiveIfPossible(facet);
		// Util.print("ensurePerspective " + _parent + " " + name1 + " " +
		// result
		// + " " + n_children + " " + children_offset);
		// if (result != null)
		// Util.print(" " + result.getNameIfPossible());
		if (result == null) {
			result = new Perspective(facet, _parent, name1, children_offset,
					n_children);
			cachePerspective(facet, result);
			// Util.print("ensurePerspective " + result);
			if (_parent != null) {
				// Util.print("ensur " + _parent + " " +
				// _parent.children_offset());
				_parent.addFacet(facet - _parent.childrenOffset() - 1, result);
			}
		} else { // if (/* name != null && */result.getNameIfPossible() ==
			// null) {
			// prefetch hasn't happened yet
			result.setName(name1);
			result.setNchildren(n_children, children_offset);
		}
		assert result.getParent() == _parent : result + " "
				+ result.getParent() + " " + _parent;
		// Util.print("ensurePerspective " + result);
		return result;
	}

	/**
	 * Do our counts match our restictions? They are initially, by the time our
	 * constructor returns,
	 */
	private boolean queryInvalid = false;

	/**
	 * Used to determine whether perspective chiSq tables are up to date
	 */
	public int updateIndex = 1;

	private String name;

	/**
	 * @return whether restrictions, searches, and clusters are consistent with
	 *         onCounts. set to invalid by Bungee.updateAllData and to valid by
	 *         Query.updateData
	 */
	public boolean isQueryValid() {
		return !queryInvalid;
	}

	public void setQueryInvalid() {
		// Util.print("setQueryInvalid ");

		// boolean result = false;
		// if (!queryInvalid) {
		queryInvalid = true;
		updateIndex++;
		// result = true;
		// }
		// Util.print("....setQueryValid return " + result);
		// Util.printStackTrace();
		// return result;
	}

	public synchronized void setQueryValid() {
		// Util.print("setQueryValid");
		// Util.printStackTrace();
		queryInvalid = false;
		redraw();
		notifyAll();
	}

	private Set badOnCounts = new HashSet();

	void addBadOnCount(PerspectiveObserver redraw) {
		badOnCounts.add(redraw);
	}

	private void redraw() {
		for (Iterator it = badOnCounts.iterator(); it.hasNext();) {
			PerspectiveObserver redraw = (PerspectiveObserver) it.next();
			redraw.redraw();
		}
		badOnCounts.clear();
	}

	/**
	 * Add a Perspective or Runnable to the prefetch queue. (Runnables should be
	 * queued after the Perspective they should run on. If a runnable might
	 * already be on the queue, should remove it first to preserve this
	 * property, as duplicates aren't added to the queue.)
	 * 
	 * @param p
	 */
	public void queuePrefetch(Object p) {
		prefetcher.add(p);
	}

	public void unqueuePrefetch(Object p) {
		prefetcher.remove(p);
	}

	// public void queuePrefetch(List args) {
	// assert args.size() == 2 : args;
	// assert args.get(0) instanceof Perspective : args.get(0);
	// assert args.get(1) instanceof Runnable : args.get(1);
	// prefetcher.add(args);
	// }

	public synchronized void waitForValidQuery() {
		// Util.print("waitForValidQuery");
		// Util.printStackTrace();
		while (!isQueryValid()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// Our wait is over
			}
		}
		// Util.print("....waitForValidQuery return");
	}

	public Iterator getFacetIterator(int facet_id, int nFacets) {
		return Util.arrayIterator(allPerspectives, facet_id, nFacets);
	}

	public Perspective findPerspective(int facet) {
		// Perspective result = null;
		// while ((result = allPerspectives[facet]) == null) {
		// try {
		// wait();
		// } catch (InterruptedException e) {
		// }
		// }
		// return result;
		assert allPerspectives[facet] != null : facet;
		return allPerspectives[facet];
	}

	public Perspective findPerspectiveIfPossible(int facet) {
		return allPerspectives[facet];
	}

	private void cachePerspective(int facet_id, Perspective perspective) {
		allPerspectives[facet_id] = perspective;
	}

	public boolean restrictData() {
		name += " / " + markupToText(description(), null);
		// Util.print("Query.restrictData " + displayedPerspectives);
		for (int i = 0; i < allPerspectives.length; i++) {
			Perspective p = allPerspectives[i];
			if (p != null) {
				assert p.onCount >= 0 || !displaysPerspective(p.parent) : p;
				if (p.isPrefetched() && !displaysPerspective(p))
					p.setPrefetched(false);
				p.totalCount = p.onCount;
			}
		}
		for (int i = displayedPerspectives.size() - 1; i >= 0; i--) {
			// restictData will remove unused and non-top-level perspectives
			// at worst, going backwards will call restrictData twice
			((ItemPredicate) displayedPerspectives.get(i)).restrictData();
		}
		db.restrict();
		baseTable = RESTRICTED;
		totalCount = onCount;
		clear();
		return totalCount > 0;
	}

	/**
	 * Can be called from thread prefetcher
	 */
	public boolean isRestrictedData() {
		return baseTable == RESTRICTED;
	}

	public void toggleCluster(Cluster cluster) {
		boolean found = clusters.remove(cluster);
		if (!found)
			clusters.add(cluster);
		isRestricted = computeIsRestricted();
	}

	public void toggleItemList(ItemList itemList) {
		boolean found = itemLists.remove(itemList);
		if (!found)
			itemLists.add(itemList);
		isRestricted = computeIsRestricted();
	}

	/**
	 * @param maxClusters
	 *            find the maxClusters'th most significant clusters
	 * @param facetRestriction
	 *            is the empty string or a where clause restricting facet_id, or
	 *            even joins, e.g. INNER JOIN facet f ON f.facet_id = i.facet_id
	 *            WHERE f.lft BETWEEN 56 AND 87
	 * @param p
	 *            only return clusters whose p-value <= p
	 * @param parent
	 *            add all the clusters as children of parent
	 */
	public void clusterTree(int maxClusters, String facetRestriction, double p,
			DisplayTree parent) {
		ResultSet[] rs = db.cluster(maxClusters, 4, facetRestriction, p);
		double pValue = 0;
		try {
			for (int i = 0; i < rs.length; i++) {
				FacetTree tree = new FacetTree(parent, rs[i], this);
				assert pValue <= (pValue = ((Cluster) tree.treeObject())
						.pValue());
				// Util.print(i + " " + pValue + " " + tree.treeObject());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Collection addItemFacet(Perspective facet, Item item) {
		return updateIDnCount(db.addItemFacet(facet.getID(), item.getId()),
				null, null);
	}

	public Collection addItemsFacet(Perspective facet) {
		return updateIDnCount(db.addItemsFacet(facet.getID()), null, null);
	}

	public Collection removeItemsFacet(Perspective facet) {
		return updateIDnCount(db.removeItemsFacet(facet.getID()), null, null);
	}

	public Collection addChildFacet(Perspective parent, String facetName) {
		assert findPerspective(facetName, parent) == null : facetName
				+ " is already a child of " + parent;
		int parent_id = parent == null ? 0 : parent.getID();
		if (parent != null) {
			parent.incfChildren(1);
		}
		return updateIDnCount(db.addChildFacet(parent_id, facetName),
				facetName, parent);
	}

	public Collection removeItemFacet(Perspective facet, Item item) {
		return updateIDnCount(db.removeItemFacet(facet.getID(), item.getId()),
				null, null);
	}

	public Collection reparent(Perspective parent, Perspective child) {
		assert findPerspective(child.getName(), parent) == null : parent
				+ " already has a child named like " + child;
		child.getParent().incfChildren(-1);
		parent.incfChildren(1);
		child.setParent(parent);
		return updateIDnCount(db.reparent(parent.getID(), child.getID()), null,
				null);
	}

	public void writeback() {
		db.writeback();
	}

	public boolean isEditable() {
		return isEditable;
	}

	Collection updateIDnCount(ResultSet rs, String facetName,
			ItemPredicate parent) {
		assert isEditable();
		// Only create new facets for children of this parent (or if new
		// parent_id == 0)
		Collection result = new ArrayList();
		try {
			while (rs.next()) {
				int oldID = rs.getInt(2);
				int newID = rs.getInt(1);
				int cnt = rs.getInt(3);
				int offset = rs.getInt(4);
				int parent_facet_id = rs.getInt(5);
				Perspective existingParent = parent_facet_id == 0 ? null
						: findPerspectiveIfPossible(parent_facet_id);
				extendAllPerspectives(Math.max(oldID, newID));
				Perspective p = findPerspectiveIfPossible(oldID);
				if (existingParent != null
						&& displaysPerspective(existingParent)
						// if it's not used, maxTotalCount == -1 and this will
						// blow
						&& cnt > existingParent.maxChildTotalCount()) {
					Util.print("updateIDnCount setting max child count to "
							+ cnt + " (" + p + ")");
					existingParent.setMaxChildTotalCount(cnt);
				}

				StringBuffer buf = new StringBuffer();
				buf.append("updateIDnCount");
				if (p == null && facetName != null)
					buf.append(" name='").append(facetName).append("'");
				if (p == null)
					buf.append(" old ID=").append(oldID);
				else
					buf.append(" old facet=").append(p);
				buf.append(" old parent=").append(existingParent);
				buf.append(" new ID=").append(newID);
				buf.append(" new parent ID=").append(parent_facet_id);
				buf.append(" count=").append(cnt);
				buf.append(" offset=").append(offset);
				Util.print(buf.toString());

				if (p != null) {
					// if (p.isInstantiated())
					// p.sortDataIndexByIndex();
					cachePerspective(oldID, null);
					cachePerspective(newID, p);
					p.setID(newID);
					p.setChildrenOffset(offset);
					p.totalCount = cnt;
					if (displaysPerspective(p)) {
						result.add(p);
					}
					if (p.getParent() != null) {
						p.getParent().addFacetAllowingNulls(
								newID - p.getParent().childrenOffset() - 1, p);
					}
				} else if (parent_facet_id == 0) {
					p = new Perspective(newID, existingParent, facetName,
							offset, parent_facet_id == 0 ? 1 : 0, "content",
							" that show ; that don't show ", this);
					cachePerspective(newID, p);

					// Util.print("NEW FACET " + newID + " " + name + " parent="
					// + existingParent + " parent_facet_id="
					// + parent_facet_id);
					insertPerspective(p);
					waitForPrefetch(p);
					// parent = p;
				} else if (existingParent != null && existingParent == parent) {
					// Util.print("NEW FACET " + newID + " " + name + " parent="
					// + existingParent + " parent_facet_id="
					// + parent_facet_id);
					p = ensurePerspective(newID, existingParent, facetName,
							offset, parent_facet_id == 0 ? 1 : 0);
				}
				p.totalCount = cnt;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		for (Iterator it = result.iterator(); it.hasNext();)
			// Do this after all the renames
			refetch((Perspective) it.next());
		return result;
	}

	/**
	 * Should only be needed when Art.isEditable
	 */
	private void extendAllPerspectives(int index) {
		totalCount = Math.max(totalCount, index + 1);
		if (index >= allPerspectives.length) {
			Perspective[] newPerspectives = new Perspective[index * 2];
			System.arraycopy(allPerspectives, 0, newPerspectives, 0,
					allPerspectives.length);
			allPerspectives = newPerspectives;
		}
	}

	public void rotate(Item item, String theta) {
		db.rotate(item.getId(), theta);
	}

	public void rename(Perspective p, String newName) {
		db.rename(p.getID(), newName);
	}

	public void setItemDescription(Item item, String description) {
		db.setItemDescription(item.getId(), description);
	}

	public String[][] getDatabases() {
		return db.getDatabases();
	}

	public String[] opsSpec(String replay) {
		return db.opsSpec(replay);
	}

	public String getSession() {
		return db.getSession();
	}

	public String aboutCollection() {
		return db.aboutCollection();
	}

	ResultSet getNames(String string) {
		return db.getNames(string);
	}

	public static final class Item implements Comparable {
		private static final IntHashtable items = new IntHashtable();

		private final int id;

		private Item(int _id) {
			id = _id;
		}

		public static Item lookupItem(int id) {
			return (Item) items.get(id);
		}

		public static Item ensureItem(int id) {
			Item result = (Item) items.get(id);
			if (result == null) {
				result = new Item(id);
				items.put(id, result);
			}
			return result;
		}

		public int getId() {
			return id;
		}

		public String toString() {
			return "<Item " + id + ">";
		}

		public int compareTo(Object arg0) {
			assert arg0 instanceof Item : arg0;
			return getId() - ((Item) arg0).getId();
		}
	}

	public static final class ItemList {
		String string;
		String name;

		public ItemList(String _name, String items) {
			string = items;
			name = _name;
		}

		public String getItems() {
			return string;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			return name;
		}
	}

	public int chiColorFamily(double significanceThreshold) {
		return 0;
	}

	public Markup describeFilter() {
		return descriptionVerbPhrase();
	}

	public Markup facetDoc(int modifiers) {
		Markup result = emptyMarkup();
		result.add("Explore within the Result List");
		return result;
	}

	public Query query() {
		return this;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int nRestrictions() {
		return nFilters(false, true, false, false);
	}

	public double pValue() {
		return 1;
	}

	public double percentOn() {
		return onCount / (double) totalCount;
	}

	/*
	 * This will return the previous onCount while query is invalid
	 * 
	 * @see edu.cmu.cs.bungee.client.query.ItemPredicate#guessOnCount()
	 */
	public int guessOnCount() {
		return onCount;
	}

	public int getOnCount() {
		return onCount;
	}

	public String getName() {
		StringBuffer buf = new StringBuffer();
		buf.append(getOnCount()).append(" ").append(
				description().compile(genericObjectLabel).toText());
		if (isRestrictedData())
			buf.append(" in collection ").append(name);
		return buf.toString();
	}

	public String getName(PerspectiveObserver _redraw) {
		return getName();
	}

	public String getNameIfPossible() {
		return name;
	}

	public ItemPredicate getParent() {
		return null;
	}

	public boolean isEffectiveChildren() {
		return false;
	}

	public boolean zeroHits(Perspective facet, int modifiers) {
		return !facet.getParent().isRestricted()
				&& facet.getOnCount() == (Perspective
						.isExcludeAction(modifiers) ? getOnCount() : 0);
	}

	private boolean maybeInsertSemicolon(StringBuffer buf, boolean firstTime) {
		if (!firstTime) {
			buf.append(";");
		}
		return false;
	}

	private boolean insertFacets(StringBuffer buf, boolean firstTime,
			SortedSet restrictions, boolean required) {
		if (restrictions.size() > 0) {
			String[] descs = new String[restrictions.size()];
			int i = 0;
			for (Iterator rit = restrictions.iterator(); rit.hasNext();) {
				Perspective r = (Perspective) rit.next();
				descs[i++] = r.fullName();
			}
			// descs[0] = p.getName()
			// + () + descs[0];
			firstTime = maybeInsertSemicolon(buf, firstTime);
			buf.append(required ? "+:" : "-:");
			buf.append(Util.join(descs, "|"));
		}
		return firstTime;
	}

	/**
	 * Ignores restrictData
	 * 
	 * @return a representation of the current filters from which
	 *         Bungee.setInitialState can recreate them.
	 */
	public String bookmark() {
		boolean emptyQuery = true;
		StringBuffer buf = new StringBuffer();
		for (Iterator it = searches.iterator(); it.hasNext();) {
			String search = (String) it.next();
			emptyQuery = maybeInsertSemicolon(buf, emptyQuery);
			buf.append("TextSearch:").append(search);
		}
		for (Iterator it = perspectivesIterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (p.getParent() == null) {
				boolean[] reqtTypes = { true, false };
				for (int type = 0; type < 2; type++) {
					SortedSet restrictions = p.getRestrictionFacetInfos(false,
							reqtTypes[type]);
					emptyQuery = insertFacets(buf, emptyQuery, restrictions,
							reqtTypes[type]);
				}
			}
		}
		// Do clusters last, so cluster.query will reflect other filters in
		// setInitialState
		for (Iterator it = clusters.iterator(); it.hasNext();) {
			Cluster cluster = (Cluster) it.next();
			emptyQuery = maybeInsertSemicolon(buf, emptyQuery);
			buf.append("Cluster:").append(emptyQuery);
			emptyQuery = insertFacets(buf, emptyQuery, cluster
					.allRestrictions(), true);
		}
		return buf.toString();
	}

	/**
	 * @return the number of rows in raw_facet_type whose ordered column is
	 *         true.
	 */
	public double nOrderedAttributes() {
		int n = 0;
		for (int i = 1; i <= nAttributes; i++)
			if (findPerspective(i).isOrdered())
				n++;
		return n;
	}

	public String getFacetIDs(int startIndex, int endIndex) {
		ResultSet rs = offsetItems(startIndex, endIndex);
		StringBuffer buf = new StringBuffer();
		try {
			int i = startIndex;
			// rs can have extra rows if it was cached
			while (rs.next() && i++ < endIndex) {
				if (buf.length() > 0)
					buf.append(",");
				buf.append(rs.getInt(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return buf.toString();
	}

	/**
	 * MySQL timestamp granularity is one second, and users can perform more
	 * than one action in that time, so we need to remember the order
	 * explicitly.
	 */
	private int userActionIndex = 0;

	public void printUserAction(int location, String object, int modifiers) {
		assert SwingUtilities.isEventDispatchThread() : "Should call this from one thread to ensure order is correct";
		StringBuffer buf = new StringBuffer();
		
		buf.append(userActionIndex++).append(",");
		buf.append(location).append(",");
		buf.append(object).append(",");
		buf.append(modifiers);
		db.printUserAction(buf.toString());
	}
}

final class FirstDoubleComparator implements Comparator {

	public int compare(Object data1, Object data2) {
		return Util.sgn(value(data1) - value(data2));
	}

	private double value(Object data) {
		return ((Double) ((Object[]) data)[0]).doubleValue();
	}
}

final class Prefetcher extends QueueThread {

	Query q;

	Prefetcher(Query _q) {
		super("Prefetcher", null, true, -2);
		q = _q;
	}

	public void process(Object info) {
		if (q != null) {
			if (info instanceof Perspective) {
				((Perspective) info).prefetchData();
			} else
				javax.swing.SwingUtilities.invokeLater((Runnable) info);
		}
	}
}

final class NameGetter extends AccumulatingQueueThread {

	Query q;

	NameGetter(Query _q) {
		super("NameGetter", 0);
		q = _q;
	}

	public void process(Object perspectives) {
		// Util.print("GetPerspectiveNames.process " + perspectives);
		// q.waitForValidQuery(); // This can cause deadlock, because updateData
		// waits for prefetching.
		Object[] objects = (Object[]) perspectives;
		SortedSet facets = new TreeSet();
		StringBuffer buf = new StringBuffer();
		// int[] facets = new int[objects.length];
		// int facetIndex = 0;
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] != null && objects[i] instanceof Perspective) {
				Perspective p = (Perspective) objects[i];
				if (p.getNameIfPossible() == null) {
					facets.add(p);
					// int facet = p.getID();
					// facets[facetIndex++] = facet;
					if (buf.length() > 0)
						buf.append(",");
					buf.append(Integer.toString(p.getID()));
				}
			}
		}
		if (!facets.isEmpty()) {
			// facets = Util.subArray(facets, 0, facetIndex - 1);
			// Arrays.sort(facets);
			// Util.print(buf.toString());
			ResultSet rs = q.getNames(buf.toString());
			// rs is sorted by facetID
			try {
				for (Iterator it = facets.iterator(); it.hasNext();) {
					rs.next();
					Perspective p = (Perspective) it.next();
					p.setName(rs.getString(1));
					// Util.print("nameGetter " + p);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		javax.swing.SwingUtilities.invokeLater(new Redraw(objects));
	}

	final class Redraw implements Runnable {
		final Object[] nodes;

		Redraw(Object[] _nodes) {
			nodes = _nodes;
			// Util.print("Redrawer " + Util.join(nodes));
		}

		public void run() {
			if (q.nameGetter != null) {
				for (int i = 0; i < nodes.length; i++) {
					if (nodes[i] != null
							&& nodes[i] instanceof PerspectiveObserver) {
						((PerspectiveObserver) nodes[i]).redraw();
					}
				}
			}
		}
	}

	/**
	 * Bungee secretly knows this value, so keep synchronized
	 */
	static final int ERROR = 24;

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.bungee.javaExtensions.QueueThread#reportError(java.lang.Throwable)
	 */
	public void reportError(Throwable e) {
		super.reportError(e);
		q.printUserAction(ERROR, Util.printStackTrace(e), 0);
	}

}