package edu.cmu.cs.bungee.client.query;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Query.ItemList;

public class QuerySQL {

	public static String onItemsQuery(Set searches1, int[] includeIDs,
			int[] excludeIDs, Set clusters, String matchFieldsPrefix1,
			boolean needDistinct, Set itemLists, String baseTable2) {
		List include = new LinkedList();
		// Arrays.sort(includeIDs);
		for (int i = 0; i < includeIDs.length; i++) {
			int currentID = includeIDs[i];
			int[] IDs = { currentID };
			include.add(IDs);
		}
		return onItemsQuery(searches1, include, excludeIDs, clusters,
				matchFieldsPrefix1, needDistinct, itemLists, baseTable2);
	}

	/**
	 * @param matchFieldsPrefix1
	 * @return SQL rendering of the current query, or null if there are no
	 *         restrictions.
	 */
	public static String onItemsQuery(Set searches1, List include,
			int[] exclude, Set clusters, String matchFieldsPrefix1,
			boolean needDistinct, Set itemLists, String baseTable2) {
		StringBuffer qJOIN = new StringBuffer(1000);

		if (searches1.size() > 0) {
			qJOIN
					.append(" INNER JOIN item it ON rnd.record_num = it.record_num");
			int i = 0;
			for (Iterator it = searches1.iterator(); it.hasNext(); i++) {
				String search = (String) it.next();
				qJOIN.append(matchFieldsPrefix1);
				qJOIN.append(quote(search));
				qJOIN.append(" IN BOOLEAN MODE)");
			}
		}

		int joinIndex = 0;
		for (Iterator it = include.iterator(); it.hasNext();) {
			int[] restrictions = (int[]) it.next();
			adjoin(qJOIN, restrictions, true, joinIndex++);
		}
		int minClusterSize = 1;
		if (clusters.size() > 0)
			for (Iterator it = clusters.iterator(); it.hasNext();) {
				int[] cluster = (int[]) it.next();
				adjoin(qJOIN, cluster, true, joinIndex++);
				minClusterSize *= cluster.length / 2 + 1;
				needDistinct = false;
			}
		if (exclude.length > 0) {
			adjoin(qJOIN, exclude, false, joinIndex);
			qJOIN.append(" WHERE i").append(joinIndex).append(
					".record_num IS NULL");
		}
		if (itemLists.size() > 0) {
			qJOIN.append(exclude.length > 0 ? " AND" : " WHERE").append(
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

	static void adjoin(StringBuffer qJOIN, int[] exclude, boolean reqtType,
			int joinIndex) {
		qJOIN.append(getJoinString(joinIndex, reqtType));
		if (exclude.length > 1) {
			qJOIN.append("IN (");
			boolean first = true;
			for (int i = 0; i < exclude.length; i++) {
				if (first)
					first = false;
				else
					qJOIN.append(",");
				qJOIN.append(exclude[i]);
			}
			qJOIN.append(")");
		} else {
			qJOIN.append("= ");
			qJOIN.append(exclude[0]);
		}
	}

	private static String[][] joinStrings;

	private static String getJoinString(int joinIndex, boolean required) {
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

	public static String quote(String s) {
		s = s.replaceAll("\\\\", "\\\\\\\\");
		s = s.replaceAll("'", "\\\\'");
		// Util.print(s);
		return "'" + s + "'";
	}

}
