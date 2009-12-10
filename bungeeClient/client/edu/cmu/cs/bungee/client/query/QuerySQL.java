package edu.cmu.cs.bungee.client.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import edu.cmu.cs.bungee.client.query.Query.ItemList;
import edu.cmu.cs.bungee.javaExtensions.Util;

public class QuerySQL {

	// public static String onItemsQuery(Set searches1, int[] includeIDs,
	// int[] excludeIDs, Set clusters, String matchFieldsPrefix1,
	// boolean needDistinct, Set itemLists, String baseTable2) {
	// List include = new LinkedList();
	// // Arrays.sort(includeIDs);
	// for (int i = 0; i < includeIDs.length; i++) {
	// int currentID = includeIDs[i];
	// int[] IDs = { currentID };
	// include.add(IDs);
	// }
	// return onItemsQuery(searches1, include, excludeIDs, clusters,
	// matchFieldsPrefix1, needDistinct, itemLists, baseTable2);
	// }

	/**
	 * @param searches1
	 *            Query.searches
	 * @param includeFacetLists
	 *            CNF of Perspectives required by query
	 * @param excludeFacets
	 *            Perspectives excluded by query
	 * @param clusters
	 *            Query.clusterIDs()
	 * @param matchFieldsPrefix1
	 *            Query.matchFieldsPrefix
	 * @param needDistinct
	 *            whether to add 'DISTINCT'
	 * @param itemLists
	 *            Query.itemLists
	 * @param baseTable2
	 *            Query.baseTable
	 * @return SQL rendering of the current query, or null if there are no
	 *         restrictions.
	 */
	public static String onItemsQuery(Collection<String> searches1,
			Collection<? extends Collection<? extends ItemPredicate>> includeFacetLists,
			Collection<? extends ItemPredicate> excludeFacets,
			Collection<Cluster> clusters, String matchFieldsPrefix1,
			boolean needDistinct, Set<ItemList> itemLists, String baseTable2) {
		StringBuffer qJOIN = new StringBuffer(1000);

		if (searches1.size() > 0) {
			qJOIN
					.append(" INNER JOIN item it ON rnd.record_num = it.record_num");
			int i = 0;
			for (Iterator<String> it = searches1.iterator(); it.hasNext(); i++) {
				String search = it.next();
				qJOIN.append(matchFieldsPrefix1);
				qJOIN.append(quote(search));
				qJOIN.append(" IN BOOLEAN MODE)");
			}
		}

		int joinIndex = 0;
		for (Iterator<? extends Collection<? extends ItemPredicate>> it = includeFacetLists
				.iterator(); it.hasNext();) {
			Collection<? extends ItemPredicate> restrictions = it.next();
			adjoin(qJOIN, restrictions, true, joinIndex++);
		}
		int minClusterSize = 1;
		if (clusters.size() > 0)
			for (Iterator<Cluster> it = clusters.iterator(); it.hasNext();) {
				Cluster cluster = it.next();
				// int[] cluster = (int[]) it.next();
				SortedSet<Perspective> facets = cluster.getFacets();
				adjoin(qJOIN, facets, true, joinIndex++);
				minClusterSize *= facets.size() / 2 + 1;
				needDistinct = false;
			}
		if (excludeFacets.size() > 0) {
			adjoin(qJOIN, excludeFacets, false, joinIndex);
			qJOIN.append(" WHERE i").append(joinIndex).append(
					".record_num IS NULL");
		}
		if (itemLists.size() > 0) {
			qJOIN.append(excludeFacets.size() > 0 ? " AND" : " WHERE").append(
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
					+ " rnd.record_num" + " FROM " + baseTable2 + " rnd"
					+ qJOIN; // +
			// "
			// ORDER
			// BY
			// random_ID";
		}
		// Util.print("query: " + result);
		return result;
	}

	static void adjoin(StringBuffer qJOIN,
			Collection<? extends ItemPredicate> sortedItemPredicates,
			boolean isRequired, int joinIndex) {
		qJOIN.append(getJoinString(joinIndex, isRequired));
		itemPredsSQLexpr(qJOIN, sortedItemPredicates, isRequired, "i"
				+ joinIndex + ".facet_id");
	}

	/**
	 * 
	 * Add a predicate requiring the SQL variable to [not] be in the Collection
	 * of ItemPredicates. For shorter command lines, uses BETWEEN to group
	 * seqeuential Perspectives.
	 * 
	 * @param qJOIN
	 * @param sortedItemPredicates
	 *            Perspective
	 * @param isRequired
	 * @param variable
	 */
	public static StringBuffer itemPredsSQLexpr(StringBuffer qJOIN,
			Collection<? extends ItemPredicate> sortedItemPredicates,
			boolean isRequired, String variable) {
		if (qJOIN == null)
			qJOIN = new StringBuffer();
		List<String> predicates = new LinkedList<String>();
		SortedSet<ItemPredicate> itemPreds = Perspective.coalesce(
				sortedItemPredicates, false);
		for (Iterator<ItemPredicate> it = itemPreds.iterator(); it.hasNext();) {
			ItemPredicate p = it.next();
			if (p instanceof MexPerspectives) {
				MexPerspectives m = (MexPerspectives) p;
				SortedSet<Perspective> facets = m.facets;
				// if (facets.size() > 10) {
				// if statement below doesn't deal correctly with
				// MexPerspectives, so handle them all here
				it.remove();
				predicates.add(variable + " BETWEEN " + facets.first().getID()
						+ " AND " + facets.last().getID());
				// }
			}
		}
		if (itemPreds.size() > 1) {
			predicates.add(variable + " IN ("
					+ Query.getItemPredicateIDs(itemPreds) + ")");
		} else if (itemPreds.size() == 1) {
			predicates.add(variable + " = "
					+ Query.getItemPredicateIDs(itemPreds));
		}
		if (predicates.size() > 1)
			qJOIN.append("(").append(Util.join(predicates, " OR ")).append(")");
		else
			qJOIN.append(predicates.get(0));
		return qJOIN;
	}

	private static String[][] joinStrings;

	/**
	 * @param joinIndex
	 * @param required
	 *            whether to use INNER or LEFT
	 * @return INNER|LEFT JOIN item_facetNtype_heap i<joinIndex> ON
	 *         rnd.record_num = i<joinIndex>.record_num AND
	 *         i<joinIndex>.facet_id
	 */
	private static String getJoinString(int joinIndex, boolean required) {
		if (joinStrings == null || joinIndex >= joinStrings.length) {
			joinStrings = new String[joinIndex + 10][2];
			for (int i = 0; i < joinIndex + 10; i++) {
				String s = " JOIN item_facetNtype_heap i" + i
						+ " ON rnd.record_num = i" + i + ".record_num AND "
				// + i + ".facet_id "
				;
				joinStrings[i][0] = " INNER" + s;
				joinStrings[i][1] = " LEFT" + s;
			}
		}
		return joinStrings[joinIndex][required ? 0 : 1];
	}

	public static String quote(String s) {
		s = s.replaceAll("\\\\", "\\\\\\\\");
		s = s.replaceAll("'", "\\\\'");
		// Util.print(s);
		return "'" + s + "'";
	}

}
