package edu.cmu.cs.bungee.client.query;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.javaExtensions.AccumulatingQueueThread;
import edu.cmu.cs.bungee.javaExtensions.IntHashtable;
import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.QueueThread;
import edu.cmu.cs.bungee.javaExtensions.Util;


public class Query implements Serializable {

	private static final long serialVersionUID = 8017632565997166607L;

	public static final boolean isEditable = false;

	private final ServletInterface db;

	private String baseTable = "item_order";

	// private String itemURLgetter;

	public final String itemURLdoc;

	// private final String[] itemDescFields;

	private final String matchFieldsPrefix;

	public final String genericObjectLabel;

	private final ArrayList displayedPerspectives = new ArrayList();

	public final int nAttributes;

	public int onCount;

	public int totalCount;

	private final Set searches = new HashSet();

	private final Set clusters = new HashSet();

	private Perspective[] allPerspectives;

	public Query(String server, String dbName) {
		db = new ServletInterface(server, dbName);
		setQueryInvalid();
		int nFacets = db.facetCount + 1;
		totalCount = db.itemCount;
		onCount = totalCount;
		matchFieldsPrefix = getMatchFieldsPrefix(Util
				.splitComma(db.itemDescriptionFields));
		genericObjectLabel = db.label;
		itemURLdoc = db.doc;
		allPerspectives = new Perspective[nFacets];
		nAttributes = initPerspectives();
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

	public String errorMessage() {
		return db.errorMessage();
	}

	public int nClusters() {
		return clusters.size();
	}

	public Set clusters() {
		return new HashSet(clusters);
	}

	public boolean usesCluster(Cluster cluster) {
		return clusters.contains(cluster);
	}

	public Cluster findCluster(String name) {
		for (Iterator it = clusters.iterator(); it.hasNext();) {
			Cluster cluster = (Cluster) it.next();
			if (cluster.toString().equals(name)) {
				return cluster;
			}
		}
		return null;
	}

	Prefetcher prefetcher;

	public void exit() {
		Util.print("Query.exit");
		if (prefetcher != null) {
			prefetcher.exit();
			prefetcher = null;
		}
		db.close();
		setQueryValid(); // Don't have things hanging around waiting; let
		// them get an error.
	}

	private Perspective[] perspectivesToAdd;

	private Perspective[] perspectivesToRemove;

	private ItemPredicate[] orderedFacetTypes;

	NameGetter nameGetter;

	public static Markup emptyMarkup() {
		return new MarkupImplementation();
	}

	public static void toEnglish(Perspective[] facets, String connector,
			Markup descriptions) {
		MarkupImplementation.toEnglish(facets, connector, descriptions);
	}

	private void descriptionClauses(Markup[] phrases, Markup result) {
		MarkupImplementation.descriptionClauses(phrases, result, searches,
				clusters);
	}

	public Markup description() {
		Markup summary = emptyMarkup();
		if (isRestricted()) {
			// Util.print("query.description");
			Markup[] phrases = getPhrases();
			// StringBuffer result = new StringBuffer(phrases.length * 20);
			summary.add("... for ");
			MarkupImplementation.descriptionNounPhrase(phrases, summary);
			descriptionClauses(phrases, summary);
			summary.add(".");
		} else {
			summary.add("(No filters applied.)");
		}
		// Util.print("description: " + summary);
		return summary;
	}

	public Markup[] getPhrases() {
		Markup[] phrases = new Markup[0];
		Iterator it = perspectivesIterator();
		while (it.hasNext()) {
			Perspective p = (Perspective) it.next();
			if (p.getParent() == null) {
				Markup description = p.getDescription(true, null);
				if (description.size() > 0)
					phrases = (Markup[]) Util.push(phrases, description,
							Markup.class);
			}
		}
		// Util.print("q.getPhrases return "
		// + PrintArray.printArrayString(phrases));
		return phrases;
	}

	public int nFilters(boolean text, boolean facet) {
		int result = text ? searches.size() : 0;
		if (facet) {
			Iterator it = perspectivesIterator();
			while (it.hasNext()) {
				Perspective p = (Perspective) it.next();
				if (p.getParent() == null && p.isAnyRestrictions())
					result++;
			}
		}
		return result;
	}

	public String describeNfilters() {
		int nText = nFilters(true, false);
		int nFacet = nFilters(false, true);
		StringBuffer buf = new StringBuffer();
		int nClusters = nClusters();
		buf.append("filters on ");
		if (nFacet > 0) {
			buf.append(nFacet).append(" categories");
			if (nText > 0 || nClusters > 0)
				buf.append(" and ");
		}
		if (nText > 0) {
			buf.append(nText).append(" keywords");
			if (nClusters > 0)
				buf.append(" and ");
		}
		if (nClusters > 0)
			buf.append(" and ").append(nClusters).append(" clusters");
		buf.append(".");
		// buf.append(".)");
		return buf.toString();
	}

	public boolean isTopLevel(int facetID) {
		Perspective p = findPerspectiveIfPossible(facetID);
		if (p != null)
			return p.topLevel();
		else
			return false;
	}

	public Perspective findUsedPerspective(String name, boolean isTopLevel) {
		int nPerspectives = displayedPerspectives.size();
		for (int i = nPerspectives - 1; i >= 0; i--) {
			Perspective p = (Perspective) displayedPerspectives.get(i);
			if (p.getName().equals(name) && (!isTopLevel || p.topLevel()))
				return p;
		}
		return null;
	}

	public Perspective findPerspective(String name, Perspective parent) {
		if (parent != null) {
			int nChildren = parent.nChildren();
			for (int i = 0; i < nChildren; i++) {
				Perspective p = parent.getNthChild(i);
				if (p.getName().equals(name))
					return p;
			}
		} else {
			return findUsedPerspective(name, true);
		}
		return null;
	}

	public ListIterator perspectivesIterator() {
		return displayedPerspectives.listIterator();
	}

	// public boolean selectFacet(Perspective facet, int modifiers) {
	// Util.print("Query.selectFacet " + facet.getName() + " "
	// + facet.isRestriction());
	// // Date start = new Date();
	// boolean result = false;
	// if (facet.isRestriction()) {
	// // Util.print((p != null && usesPerspective(p)) + " " +
	// // !isRestriction(tree.facet) + " "
	// // + (p != null ? p.facet_type_name : ""));
	// result = facet.parent.toggleFacet(facet, modifiers);
	// } else {
	// addIntermediateFacets(facet, modifiers);
	// result = true;
	// }
	//
	// // Date end = new Date();
	// // long delay = end.getTime() - start.getTime();
	// // Util.print("selectFacet time " + delay + " " + p + " " +
	// // !isRestriction(tree.facet));
	// return result;
	// }

	/**
	 * @return Ensure there's a [displayed] Perspective for facet and all its
	 *         ancestors.
	 */
	// This is weird, because we're recursing UP the tree.
	public boolean toggleFacet(Perspective facet, int modifiers) {
		Perspective parent = facet.getParent();
		if (!usesPerspective(parent)) {
			if (Perspective.isExcludeAction(modifiers))
				insertAncestors(parent);
			else
				toggleFacet(parent, modifiers);
		}
		return parent.toggleFacet(facet, modifiers);
	}

	// Return lowest level restrictions only, including
	// facet_types, e.g. Date if there are no Date restrictions.
	// public Perspective[] dontUnderline() {
	// Perspective[] result = null;
	// Iterator it = perspectivesIterator();
	// while (it.hasNext()) {
	// Perspective p = (Perspective) it.next();
	// Perspective[] restrs = p.restrictions();
	// if (restrs.length > 0)
	// for (int i = 0; i < restrs.length; i++) {
	// Perspective restr = restrs[i];
	// if (!usesPerspective(restr))
	// result = (Perspective[]) Util.push(result, restr,
	// Perspective.class);
	// }
	// else
	// result = (Perspective[]) Util
	// .push(result, p, Perspective.class);
	// }
	// return result;
	// }

	public Perspective[] restrictions() {
		Perspective[] result = {};
		Iterator it = perspectivesIterator();
		while (it.hasNext()) {
			ItemPredicate p = (ItemPredicate) it.next();
			result = (Perspective[]) Util.append(result, p.allRestrictions(),
					Perspective.class);
		}
		return result;
	}

	public Perspective[] restrictions(boolean required) {
		Perspective[] result = {};
		Iterator it = perspectivesIterator();
		while (it.hasNext()) {
			Perspective p = (Perspective) it.next();
			result = (Perspective[]) Util.append(result, p
					.restrictions(required), Perspective.class);
		}
		return result;
	}

	// public boolean isRestriction(Perspective facet) {
	// boolean result = false;
	// Iterator it = perspectivesIterator();
	// while (it.hasNext() && !result) {
	// Perspective p = (Perspective) it.next();
	// result = p.isRestriction(facet);
	// }
	// return result;
	// }

	public boolean isRestricted() {
		// Wrong - we want to know if there are restrictions before querying the
		// database.
		// return onCount < totalCount;
		boolean result = (searches.size() > 0) || (nClusters() > 0);
		if (!result) {
			int n = displayedPerspectives.size();
			for (int i = 0; i < n && !result; i++) {
				Perspective p = (Perspective) displayedPerspectives.get(i);
				result = p.isRestricted();
			}
		}
		return result;
	}

	void addPerspective(ItemPredicate p) {
		if (perspectivesToRemove != null
				&& Util.isMember(perspectivesToRemove, p))
			perspectivesToRemove = (Perspective[]) Util.delete(
					perspectivesToRemove, p, Perspective.class);
		else
			perspectivesToAdd = (Perspective[]) Util.push(perspectivesToAdd, p,
					Perspective.class);
	}

	void removePerspective(ItemPredicate p) {
		if (perspectivesToAdd != null && Util.isMember(perspectivesToAdd, p))
			perspectivesToAdd = (Perspective[]) Util.delete(perspectivesToAdd,
					p, Perspective.class);
		else
			perspectivesToRemove = (Perspective[]) Util.push(
					perspectivesToRemove, p, Perspective.class);
	}

	private int initPerspectives() {
		int facet_type_id = 0;
		ResultSet rs = null;
		try {
			rs = db.initPerspectives();
			while (rs.next()) {

				Perspective p = new Perspective(++facet_type_id, null, rs
						.getString(1), rs.getInt(5), rs.getInt(4), rs
						.getString(2), rs.getString(3), this);
				cachePerspective(facet_type_id, p);
				// Perspective p = ensurePerspective(++facet_type_id, null, rs
				// .getString(1), rs.getInt(5), rs.getInt(4));
				// p.instantiate(rs.getString(2), rs.getString(3), this);
				displayedPerspectives.add(p);
				p.totalCount = rs.getInt(6);
				if (rs.getInt(7) != 0)
					orderedFacetTypes = (ItemPredicate[]) Util.push(
							orderedFacetTypes, p, Perspective.class);
			}
			// nAttributes = facet_type_id;
			Perspective[] persps = (Perspective[]) displayedPerspectives
					.toArray(new Perspective[0]);

			initFacetTypes(persps);

			prefetcher = new Prefetcher(this, persps);
			prefetcher.start();

			nameGetter = new NameGetter(this);
			nameGetter.start();

		} catch (SQLException se) {
			Util.err("SQL Exception in initPerspectives: " + se.getMessage());
			se.printStackTrace();
		} finally {
			close(rs);
		}
		return facet_type_id;
	}

	public boolean isOrdered(Perspective p) {
		return Util.isMember(orderedFacetTypes, p.getFacetType());
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
	public boolean updateOnItems(Item selectedItem, int nNeighbors) {
		// Util.print("Query updateOnItems " + selectedItem);
		String onSQL = onItemsQuery(null);
		if (onSQL != null) {
			// Util.print(onSQL);
			onCount = db.updateOnItems(onSQL, selectedItem != null ? selectedItem.getId() : 0, onItemsTable(),
					nNeighbors);
			return (onCount > 0);
		} else {
			db.decacheOffsets();
			onCount = totalCount;
		}
		return false;
	}

	// 0 item_order; 1 restricted; 2 onItems
	int onItemsTable() {
		return isRestricted() ? 2 : isRestrictedData() ? 1 : 0;
	}

	void adjoin(StringBuffer qJOIN, Perspective[] restrictions,
			boolean reqtType, int joinIndex) {
		qJOIN.append(getJoinString(joinIndex, reqtType));
		if (restrictions.length > 1) {
			qJOIN.append("IN (");
			for (int i = 0; i < restrictions.length; i++) {
				if (i > 0)
					qJOIN.append(",");
				qJOIN.append(restrictions[i].getID());
			}
			qJOIN.append(")");
		} else {
			qJOIN.append("= ");
			qJOIN.append(restrictions[0].getID());
		}
	}

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
				Perspective[] restrictions = p.getRestrictionFacetInfos(false,
						true);
				if (toIgnore != null) {
					for (int i = restrictions.length - 1; i >= 0; i--) {
						if (restrictions[i].hasAncestor(toIgnore))
							restrictions = (Perspective[]) Util.delete(
									restrictions, restrictions[i],
									Perspective.class);
					}
				}
				if (restrictions.length > 0) {
					if (restrictions.length > 1)
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
				minClusterSize *= Math.ceil((cluster.size() + 1) / 2.0);
				needDistinct = false;
			}
		Perspective[] exclude = restrictions(false);
		if (exclude.length > 0) {
			adjoin(qJOIN, exclude, false, joinIndex);
			qJOIN.append(" WHERE i").append(joinIndex).append(
					".record_num IS NULL");
		}
		if (qJOIN.length() > 0) {
			if (minClusterSize > 1)
				qJOIN.append(" GROUP BY rnd.record_num HAVING COUNT(*) >= ")
						.append(minClusterSize);
			// isRestricted = true;

			return (needDistinct ? "SELECT DISTINCT" : "SELECT")
					+ " rnd.record_num" + " FROM " + baseTable + " rnd" + qJOIN; // + "
			// ORDER
			// BY
			// random_ID";
		}
		return null;
	}

	private ResultSet getFilteredCounts() {
		ResultSet rs = db.getFilteredCounts(
				getFilteredCountsInternal(perspectivesToAdd),
				getFilteredCountsInternal(perspectivesToRemove));
		perspectivesToAdd = null;
		perspectivesToRemove = null;
		return rs;
	}

	private ResultSet getFilteredCountTypes() {
		return db.getFilteredCountTypes();
	}

	String getFilteredCountsInternal(Perspective[] persps) {
		if (persps == null || persps.length == 0)
			return null;
		StringBuffer buf = new StringBuffer(persps.length * 7);
		for (int i = 0; i < persps.length; i++) {
			if (i > 0)
				buf.append(", ");
			buf.append(persps[i].getID());
		}
		return buf.toString();
	}

	public ResultSet getCountsIgnoringFacet(Perspective p) {
		String subQuery = onItemsQuery(p);
		// Util.print(" toIgnoreQuery=" + p + " " + subQuery);
		if (subQuery != null) {
			return db.getCountsIgnoringFacet(subQuery, p.getID());
		}
		return null;
	}

	ResultSet init() {
		return db.init();
	}

	public void clear() {
		int nPerspectives = displayedPerspectives.size();
		for (int i = nPerspectives - 1; i >= 0; i--) {
			Perspective p = (Perspective) displayedPerspectives.get(i);
			if (p.getParent() == null)
				clearPerspective(p);
		}
		searches.clear();
		clusters.clear();
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
		if (p.isPrefetched())
			// prefetching can call this if restrictedData and all child counts
			// = 0
			// in which case resetData will barf cause not prefetched yet
			p.resetData(-1);
	}

	// Perspective has already removed the restriction.
	void removeRestriction(Perspective p) {
		// Util.print("removeRestriction " + p + " " + usesPerspective(p));
		if (usesPerspective(p)) {
			displayedPerspectives.remove(p);
			removePerspective(p);
			clearPerspective(p);
		}
		if (p.getParent() != null
				&& p.getParent().getParent() != null
				&& !p.getParent().isRestricted()
				&& !p.getParent().getParent()
						.isRestriction(p.getParent(), true)) {
			// Util.print("removeRestriction of parent " + p.parent);
			removeRestriction(p.getParent());
		}
	}

	public void addTextSearch(String s) {
		// Util.print("Query.addTextSearch");
		searches.add(s);
	}

	public boolean removeTextSearch(String s) {
		return searches.remove(s);
	}

	public Set getSearches() {
		return searches;
	}

	void initFacetTypes(Perspective[] facetTypes) {
		ResultSet rs = null;
		try {
			rs = init();
			// Util.print("Query.updateData " + isTotal + " " +
			// (newPerspective != null ? newPerspective.facet_type_name : "")
			// + " " + isHighlight);
			for (int i = 0; i < facetTypes.length; i++) {
				initPerspective(rs, facetTypes[i], 0);
			}
		} finally {
			close(rs);
		}
		perspectivesToAdd = facetTypes;
	}

	public void refetch(Perspective facet) {
		extendAllPerspectives(facet.children_offset() + facet.nChildren());
		facet.resetData(0);
		initPerspective(db.prefetch(facet.getID(), 1), facet, 1);
	}

	public void prefetchData(Perspective facet) {
		if (!facet.isPrefetched()) {
			synchronized (facet) {
				if (!facet.isPrefetched()) {
					facet.ensureInstantiatedPerspective();

					int fetchType = 1;
					if (facet.getTotalChildTotalCount() > 0)
						fetchType = 3;
					else if (isRestrictedData())
						fetchType = 5;
					if (facet.nChildren() > 100)
						fetchType += 1;
					initPerspective(db.prefetch(facet.getID(), fetchType),
							facet, fetchType > 4 ? fetchType - 4 : fetchType);
					facet.setPrefetched();
					facet.notifyAll();
				}
			}
		}
	}

	public void waitForPrefetch(Perspective facet) {
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

	// cases:
	// 0: initFacetTypes - count .
	// 1: prefetch a new facet. count, nChildren, offset, name
	// 2: prefetch a new facet. count, nChildren, offset
	// 3: prefetch a top-level facet. nChildren, offset, name
	// 4: prefetch a top-level facet. nChildren, offset
	void initPerspective(ResultSet rs, Perspective p, int fetchType) {
		assert p.isInstantiated();
		try {
			boolean isCount = fetchType <= 2;
			boolean isName = fetchType == 1 || fetchType == 3;
			boolean isOffset = fetchType > 0;
			// Map cHist = new TreeMap();
			// Map offHist = new TreeMap();
			// Map ncHist = new TreeMap();

			// Util.print("Query.initPerspective " + isName + " " + isCount + "
			// "
			// + p + " " + p.nChildren());

			int facet_id = p.children_offset();
			int nRemainingChildren = p.nChildren();
			int cumCount = 0;
			int maxCount = -1;
			int count = -1;
			String name = null;
			while (--nRemainingChildren >= 0) {
				rs.next();
				Perspective v;
				if (isOffset) {
					int fieldOffset = isCount ? 1 : 0;
					int childrenOffset = rs.getInt(fieldOffset + 2);
					int nChildren = rs.getInt(fieldOffset + 1);
					if (isName)
						name = rs.getString(fieldOffset + 3);
					v = ensurePerspective(++facet_id, p, name, childrenOffset,
							nChildren);

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
					v = ensurePerspective(++facet_id, p, null, 0, 0);
				}
				if (isCount) {
					count = rs.getInt(1);
					// incf(cHist, count);

					assert count > 0 || isRestrictedData() : p + " " + v + " "
							+ count;
					// Need to DELETE FROM facet WHERE count = 0
					cumCount += count;
					// v.onCount = count;
					if (count > maxCount) {
						// Util
						// .print("initPerspective setting max child count to "
						// + count + " (" + v + ")");
						maxCount = count;
					}
					// Util.print("Setting " + v + " totalCount=" + count);
					v.totalCount = count;
					v.cumCount = cumCount;
				}
			}

			// showHist(cHist, "Counts:");
			// showHist(offHist, "Offsets:");
			// showHist(ncHist, "nChilds:");
			if (isCount) {
				p.setTotalChildTotalCount(cumCount);
				p.setMaxChildTotalCount(maxCount);
				// if (p.parent != null) {
				// // For a new facet, any other displayed siblings have to
				// // make room.
				// p.parent.updateChildPercents();
				// }
			}
			// assert p.isDataIndexByOnComplete();
		} catch (SQLException se) {
			Util.err("SQL Exception in perspective.updateData: "
					+ se.getMessage());
			se.printStackTrace();
		}
	}

	final public Object childIndexesBusy = "childIndexesBusy";

	public void updateData(boolean resetOnly) {
		// Util.print("Query.updateData " + resetOnly);
		ResultSet counts = null;
		ResultSet typeCounts = null;
		if (!resetOnly) {
			counts = getFilteredCounts();
			typeCounts = getFilteredCountTypes();
		}
		synchronized (childIndexesBusy) {
			for (int i = 0; i < displayedPerspectives.size(); i++) {
				Perspective p = ((Perspective) displayedPerspectives.get(i));
				if (!p.isPrefetched())
					waitForPrefetch(p);
				p.resetData(0);
			}
			if (!resetOnly) {
				// long start = new Date().getTime();
				updateDataInternal(counts);
				updateDataInternal(typeCounts);
				// try {
				close(counts);
				close(typeCounts);
				// } catch (SQLException e) {
				// e.printStackTrace();
				// }
			}
		}
		// Util.print("...updateData return");
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
					// if (v.parent == null)
					// Util.print(v + " " + count);
					// not if restricted.
					assert 0 <= count && count <= v.totalCount : count + " "
							+ v;
					v.onCount = count;
					// Util.print("Setting " + v + ".onCount=" + count);
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

	void insertAncestors(Perspective facet) {
		if (!usesPerspective(facet.getParent()))
			insertAncestors(facet.getParent());
		insertPerspective(facet);
	}

	void insertPerspective(Perspective p) {
		boolean added = false;
		ListIterator it = perspectivesIterator();
		while (it.hasNext()) {
			Perspective inList = (Perspective) it.next();
			if (inList.children_offset() > p.children_offset()) {
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

	public boolean usesPerspective(Perspective p) {
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

	public ResultSet getThumbs(Item[] items, int imageW, int imageH, int quality) {
		int[] itemIDs = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			itemIDs[i] = items[i].getId();
		}
		return db.getThumbs(Util.join(itemIDs), imageW, imageH, quality);
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

	public int[] itemIndexFromURL(String URL, int nNeighbors) {
		return db.itemIndexFromURL(URL, onItemsTable(), nNeighbors);
	}

	ResultSet getItemInfo(Item item) {
		return db.getItemInfo(item.getId());
	}

	public String getItemURL(Item item) {
		return db.getItemURL(item.getId());
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

	public Perspective ensurePerspective(int facet, Perspective _parent,
			String name, int children_offset, int n_children) {
		Perspective result = findPerspectiveIfPossible(facet);
		// if (_parent.facet_id > 4) {
		// Util.print(_parent + " " + name + " " + result + " " + n_children);
		// if (result != null)
		// Util.print(" " + result.getNameIfPossible());
		// }
		if (result == null) {
			result = new Perspective(facet, _parent, name, children_offset,
					n_children);
			cachePerspective(facet, result);
			// Util.print("ensurePerspective " + result);
			if (_parent != null) {
				_parent.addFacet(facet - _parent.children_offset() - 1, result);
			}
		} else if (/* name != null && */result.getNameIfPossible() == null) {
			// prefetch hasn't happened yet
			result.setName(name);
			result.setNchildren(n_children, children_offset);
		}
		assert result.getParent() == _parent : result + " "
				+ result.getParent() + " " + _parent;
		// Util.print("ensurePerspective " + result);
		return result;
	}

	private boolean queryInvalid = true;

	/**
	 * Used to determine whether perspective chiSq tables are up to date
	 */
	int updateIndex = 1;

	public boolean isQueryValid() {
		return !queryInvalid;
	}

	public void setQueryInvalid() {
		// Util.print("setQueryInvalid ");

		// boolean result = false;
		// if (!queryInvalid) {
		queryInvalid = true;
		// result = true;
		// }
		// Util.print("....setQueryValid return " + result);
		// Util.printStackTrace();
		// return result;
	}

	public synchronized void setQueryValid() {
		// Util.print("setQueryValid");
		// Util.printStackTrace();
		updateIndex++;
		queryInvalid = false;
		notifyAll();
	}

	public void queuePrefetch(Object p) {
		prefetcher.add(p);
	}

	public void queuePrefetch(List args) {
		assert args.size() == 2 : args;
		assert args.get(0) instanceof Perspective : args.get(0);
		assert args.get(1) instanceof Runnable : args.get(1);
		prefetcher.add(args);
	}

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

	public void restrict() {
		// Util.print("Query.restrict " + displayedPerspectives);
		for (int i = 0; i < allPerspectives.length; i++) {
			Perspective p = allPerspectives[i];
			if (p != null) {
				p.totalCount = p.onCount;
			}
		}
		for (int i = displayedPerspectives.size() - 1; i >= 0; i--) {
			// restictData will remove unused and non-top-level perspectives
			// at worst, going backwards will call restrictData twice
			((ItemPredicate) displayedPerspectives.get(i)).restrictData();
		}
		db.restrict();
		baseTable = "restricted";
		totalCount = onCount;
		clear();
	}

	public boolean isRestrictedData() {
		return baseTable.equals("restricted");
	}

	public void toggleCluster(Cluster cluster) {
		boolean found = clusters.remove(cluster);
		if (!found)
			clusters.add(cluster);
		// boolean found = false;
		// if (clusters != null) {
		// for (int i = clusters.length - 1; i >= 0 && !found; i--) {
		// if (Arrays.equals(clusters[i].facets, cluster.facets)) {
		// if (clusters.length == 1) {
		// clusters = null;
		// } else {
		// clusters = (Cluster[]) Util.delete(clusters,
		// clusters[i], Cluster.class);
		// }
		// found = true;
		// }
		// }
		// }
		// if (!found) {
		// clusters = (Cluster[]) Util.push(clusters, cluster, Cluster.class);
		// }
	}

	public ResultSet[] clusterRS(int maxClusters, String facetRestriction,
			double p) {
		return db.cluster(maxClusters, 4, facetRestriction, p);
	}

	// public ResultSet clusterRS(Perspective perspective) {
	// return db.cluster(perspective.facet_id);
	// }
	//
	// public int clusterCount(Perspective perspective) {
	// return db.clusterCount(perspective.facet_id);
	// }
	//
	// public Cluster cluster(Perspective perspective, ResultSet rs, int nItems)
	// {
	// Perspective[] result = null;
	// try {
	// if (rs != null) {
	// rs.last();
	// // Util.print(rs.getRow());
	// if (rs.getRow() > 0) {
	// result = new Perspective[rs.getRow()];
	// rs.beforeFirst();
	// while (rs.next()) {
	// if (rs.getInt(7) == 0) {
	// Perspective p = allPerspectives[rs.getInt(2)];
	// // Util.print("clluster " + rs.getString(3));
	// assert p != null : perspective + " "
	// + rs.getString(3) + " " + rs.getInt(2);
	// result[rs.getRow() - 1] = p;
	// }
	// }
	// }
	// rs.close();
	// }
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// Cluster c = null;
	// if (result != null) {
	// result = (Perspective[]) Util.delete(result, (Perspective) null,
	// Perspective.class);
	// c = new Cluster(result, perspective, nItems);
	// }
	// return c;
	// }
	//
	// static FirstDoubleComparator firstDoubleComparator = new
	// FirstDoubleComparator();
	//
	// public Perspective[] associates() {
	// ArrayList associates = new ArrayList();
	// for (int i = 0; i < allPerspectives.length; i++) {
	// Perspective facet = allPerspectives[i];
	// if (facet != null && facet.onCount > 1
	// && Art.chiColorFamily(facet) == 1) {
	// double p = ChiSqr.pValue(Art.chiSqTable(facet, null));
	// Object[] x = { new Double(p), facet };
	// // PrintArray.printArray(Art.chiSqTable(allPerspectives[i],
	// // null));
	// // Util.print(facet.totalCount + " " + facet.onCount + " " +
	// // facet);
	// associates.add(x);
	// }
	// }
	// Collections.sort(associates, firstDoubleComparator);
	// Perspective[] result = new Perspective[associates.size()];
	// for (int i = 0; i < associates.size(); i++) {
	// result[i] = (Perspective) ((Object[]) associates.get(i))[1];
	// }
	// return result;
	// }

	public Collection addItemFacet(Perspective facet, Item item) {
		return updateIDnCount(db.addItemFacet(facet.getID(), item.getId()), null, null);
	}

	public Collection addItemsFacet(Perspective facet) {
		return updateIDnCount(db.addItemsFacet(facet.getID()), null, null);
	}

	public Collection removeItemsFacet(Perspective facet) {
		return updateIDnCount(db.removeItemsFacet(facet.getID()), null, null);
	}

	public Collection addChildFacet(Perspective parent, String name) {
		assert findPerspective(name, parent) == null : name
				+ " is already a child of " + parent;
		int parent_id = parent == null ? 0 : parent.getID();
		if (parent != null) {
			parent.incfChildren(1);
		}
		return updateIDnCount(db.addChildFacet(parent_id, name), name, parent);
	}

	public Collection removeItemFacet(Perspective facet, Item item) {
		return updateIDnCount(db.removeItemFacet(facet.getID(), item.getId()), null,
				null);
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

	Collection updateIDnCount(ResultSet rs, String name, ItemPredicate parent) {
		assert Query.isEditable;
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
				if (existingParent != null && usesPerspective(existingParent)
				// if it's not used, maxTotalCount == -1 and this will blow
						&& cnt > existingParent.maxChildTotalCount()) {
					Util.print("updateIDnCount setting max child count to "
							+ cnt + " (" + p + ")");
					existingParent.setMaxChildTotalCount(cnt);
				}

				StringBuffer buf = new StringBuffer();
				buf.append("updateIDnCount");
				if (p == null && name != null)
					buf.append(" name='").append(name).append("'");
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
					if (usesPerspective(p)) {
						result.add(p);
					}
					if (p.getParent() != null) {
						p.getParent().addFacetAllowingNulls(
								newID - p.getParent().children_offset() - 1, p);
					}
				} else if (parent_facet_id == 0) {
					p = new Perspective(newID, existingParent, name, offset,
							parent_facet_id == 0 ? 1 : 0, "content",
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
					p = ensurePerspective(newID, existingParent, name, offset,
							parent_facet_id == 0 ? 1 : 0);
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

	public void printUserAction(int location, String object, int modifiers) {
		db.printUserAction(location, object, modifiers);
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
			return ((Item) arg0).getId() - getId();
		}
	}
}

class FirstDoubleComparator implements Comparator, Serializable {

	private static final long serialVersionUID = -2594094099399551219L;

	public int compare(Object data1, Object data2) {
		return Util.sgn(value(data1) - value(data2));
	}

	public double value(Object data) {
		return ((Double) ((Object[]) data)[0]).doubleValue();
	}
}

class Prefetcher extends QueueThread {

	Query q;

	public synchronized void exit() {
		if (q != null) {
			q.prefetcher = null;
			q = null;
		}
		super.exit();
	}

	public Prefetcher(Query _q, ItemPredicate[] facets) {
		super("GetPerspectiveNames", facets, true, -2);
		q = _q;
	}

	public void process(Object info) {
		ItemPredicate facet = null;
		Runnable runnable = null;
		if (info instanceof Perspective) {
			facet = (ItemPredicate) info;
		} else if (info instanceof List) {
			List infoVector = (List) info;
			facet = (ItemPredicate) infoVector.get(0);
			runnable = (Runnable) infoVector.get(1);
		} else {
			assert false : info;
		}
		// Util.print("GetPerspectiveNames.process " + ((Perspective)
		// facet).getName());
		// q.waitForValidQuery(); // This can cause deadlock, because updateData
		// waits for prefetching.
		if (q != null) {
			q.prefetchData((Perspective) facet);
			if (runnable != null) {
				javax.swing.SwingUtilities.invokeLater(runnable);
			}
		}
	}
}

class NameGetter extends AccumulatingQueueThread {

	Query q;

	public synchronized void exit() {
		if (q != null) {
			q.nameGetter = null;
			q = null;
		}
		super.exit();
	}

	public NameGetter(Query _q) {
		super("GetPerspectiveNames", -2);
		q = _q;
	}

	public void process(Object perspectives) {
		// Util.print("GetPerspectiveNames.process " + ((Perspective)
		// facet).getName());
		// q.waitForValidQuery(); // This can cause deadlock, because updateData
		// waits for prefetching.
		if (q != null) {
			Object[] objects = (Object[]) perspectives;
			int[] facets = new int[objects.length];
			int facetIndex = 0;
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] != null && objects[i] instanceof Perspective) {
					Perspective p = (Perspective) objects[i];
					if (p.getNameIfPossible() == null) {
						int facet = p.getID();
						facets[facetIndex++] = facet;
					}
				}
			}
			if (facetIndex > 0) {
				facets = Util.subArray(facets, 0, facetIndex - 1);
				Arrays.sort(facets);
				ResultSet rs = q.getNames(Util.join(facets));
				facetIndex = 0;
				try {
					while (rs.next()) {
						Perspective p = q.findPerspective(facets[facetIndex++]);
						p.setName(rs.getString(1));
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			javax.swing.SwingUtilities.invokeLater(new Redraw(objects));
		}
	}

	class Redraw implements Runnable {
		final Object[] nodes;

		Redraw(Object[] _nodes) {
			nodes = _nodes;
		}

		public void run() {
			for (int i = 0; i < nodes.length; i++) {
				if (nodes[i] != null && nodes[i] instanceof PerspectiveObserver)
					((PerspectiveObserver) nodes[i]).redraw();
			}
		}
	}
}