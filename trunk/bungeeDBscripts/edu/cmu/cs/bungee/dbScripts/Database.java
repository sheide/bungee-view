package edu.cmu.cs.bungee.dbScripts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Utilities for low level updating of Bungee View items and facets. Assumes
 * you're populating a database, so keeps track of the next facet_id and
 * record_num to be created.
 * 
 */
public class Database {

	boolean verbose;
	JDBCSample jdbc;
	String descriptionFields;

	private int recordNum;

	/**
	 * Truncate names to this length before inserting
	 */
	private int maxNameLength = -1;

	/**
	 * Don't update the database; just print the SQL updates
	 */
	static final boolean dontUpdate = false;

	int facetID = -1;

	/**
	 * Detect when we start creating items, and cache all facets. Then as long
	 * as no one else updates the database, we can just work with the cache. As
	 * soon as someone asks for getJDBC, we update the database and clear our
	 * cache.
	 */
	private FacetCache facetCache;

	Database(JDBCSample _jdbc, boolean _verbose) {
		verbose = _verbose;
		jdbc = _jdbc;
		try {
			createTables();
			recordNum = Math.max(1, jdbc
					.SQLqueryInt("SELECT MAX(record_num) FROM item"));

			descriptionFields = jdbc
					.SQLqueryString("SELECT itemDescriptionFields FROM globals");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	JDBCSample getJdbc() {
		// No one else should muck with the database while we're reading data,
		// so we can cache stuff.
		try {
			startReadingData(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jdbc;
	}

	PreparedStatement lookupPS(String SQL) throws SQLException {
		return jdbc.lookupPS(SQL);
	}

	boolean isReadingData() {
		return facetCache != null;
	}

	void startReadingData(boolean state) throws SQLException {
		if (state != isReadingData()) {
			if (isReadingData()) {
				// assert false;
				facetCache = null;
				mergeDuplicateFacets();
			} else {
				facetCache = new FacetCache();
			}
		}
	}

	/**
	 * This has to be here instead of in populate to avoid circular dependencies
	 * between initializing facetID and creating the tables.
	 */
	void createTables() throws SQLException {
		String copyFrom = "wpa";

		String[] tablesToCopy = { "raw_facet_type", "globals" /*, "043places"*/ };
		for (int i = 0; i < tablesToCopy.length; i++) {
			if (!jdbc.tableExists(tablesToCopy[i])) {
				db("CREATE TABLE " + tablesToCopy[i] + " LIKE " + copyFrom
						+ "." + tablesToCopy[i]);
				db("REPLACE INTO " + tablesToCopy[i] + " SELECT * FROM "
						+ copyFrom + "." + tablesToCopy[i]);
			}
		}

		String[] tables = { "images", "raw_facet", "user_actions",
				"raw_item_facet", "item"/*
										 * "places_hierarchy", "gac", "tgm",
										 * "URIs"
										 */
		};
		for (int i = 0; i < tables.length; i++) {
			db("CREATE TABLE IF NOT EXISTS " + tables[i] + " LIKE " + copyFrom
					+ "." + tables[i]);
		}

		// db("CREATE OR REPLACE VIEW thumbnails AS SELECT * FROM images");

		// for (int i = 0; i < attrTables.length; i++) {
		// db("CREATE TABLE IF NOT EXISTS " + attrTables[i] + " LIKE "
		// + copyFrom + ".title");
		// }
	}

	/**
	 * @param name
	 *            known to be a place name
	 * @return <ancestor hierarchy>
	 */
	String[] lookupGAC(String name) {
		String[] result = null;
		try {
			PreparedStatement lookupGAC = lookupPS("SELECT broader FROM wpa.043places WHERE term = ?");
			lookupGAC.setString(1, name);
			String place = jdbc.SQLqueryString(lookupGAC);
			assert place != null : name;
			result = place.split(" -- ");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	int recordNum() {
		assert recordNum > 0;
		return recordNum;
	}

	PreparedStatement newItemQuery;

	PreparedStatement newItemQuery() throws SQLException {
		if (newItemQuery == null) {
			int nItemDescColumns = jdbc
					.SQLqueryInt("SELECT COUNT(*) FROM information_schema.COLUMNS"
							+ " WHERE table_schema = '"
							+ jdbc.dbName
							+ "' AND table_name = 'item'") - 3;
			String query = "INSERT INTO item VALUES(null, null, ?";
			for (int i = 0; i < nItemDescColumns; i++) {
				query += ", NULL";
			}
			query += ")";
			newItemQuery = lookupPS(query);
			// Util.print(nItemDescColumns + " " + query);
		}
		return newItemQuery;
	}

	String itemDescColumns() throws SQLException {
		return jdbc.SQLqueryString("SELECT itemDescriptionFields FROM globals");
	}

	void newItem() {
		newItem(recordNum + 1);
	}

	void newItem(int itemID) {
		assert itemID > 0 : itemID;
		try {
			startReadingData(true);
			recordNum = itemID;
			PreparedStatement newItem = newItemQuery();
			newItem.setInt(1, itemID);
			if (!dontUpdate) {
				jdbc.SQLupdate(newItem);
			}
		} catch (SQLException e) {
			try {
				Util.print(itemDescColumns());
				throw (e);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	void insertAttribute(String column, String value) throws SQLException {
		PreparedStatement updateItem = lookupPS("UPDATE item SET " + column
				+ " = ? WHERE record_num = ?");
		updateItem.setString(1, value);
		updateItem.setInt(2, recordNum);
		db(updateItem, "Update item");
	}

	// This is the only call we have to worry about as far as the facetCache
	void insertFacet(List<String> hierValue) throws SQLException {
		int facet = getFacet(hierValue);
		addItemFacet(recordNum(), facet);
	}

	private int getFacet(List<String> hierValue) throws SQLException {
		assert isReadingData();
		int result;
		if (isReadingData()) {
			result = facetCache.getFacet(hierValue);
		} else {
			result = 0;
			for (Iterator<String> it = hierValue.iterator(); it.hasNext();) {
				String name = it.next();
				int child = lookupFacet(name, result);
				if (child > 0)
					result = child;
				else
					result = newFacet(name, result);
			}
		}
		return result;
	}

	/**
	 * Cached value of raw_facet - hopefully faster access during parsing.
	 * 
	 */
	private class FacetCache {
		private Map<List<String>, Integer> names2facet = new Hashtable<List<String>, Integer>();

		FacetCache() throws SQLException {
			int nFacets = nextFacetID();
			Util.print("Starting to create records, with <= " + nFacets
					+ " tags already in database.");
			Vector<List<String>> id2facet = new Vector<List<String>>(nFacets);
			id2facet.setSize(nFacets);
			ResultSet rs = jdbc
					.SQLquery("SELECT facet_type_id, name FROM raw_facet_type");
			while (rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);

				List<String> hierValue = new ArrayList<String>(1);
				hierValue.add(name);
				names2facet.put(hierValue, id);
				id2facet.set(id, hierValue);
			}
			rs = jdbc
					.SQLquery("SELECT facet_id, name, parent_facet_id FROM raw_facet");
			while (rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);
				int parentID = rs.getInt(3);

				List<String> parent = id2facet.get(parentID);
				List<String> hierValue = new ArrayList<String>(parent);
				hierValue.add(parent.size(), name);
				names2facet.put(hierValue, id);
				id2facet.set(id, hierValue);
			}
		}

		int getFacet(List<String> hierValue) throws SQLException {
			Integer result = names2facet.get(hierValue);
			if (result == null) {
				assert hierValue.size() > 1 : "Can't find tag category "
						+ hierValue;
				// Defensive copy
				hierValue = new ArrayList<String>(hierValue);
				List<String> parent = hierValue
						.subList(0, hierValue.size() - 1);
				result = newFacet(hierValue.get(hierValue.size() - 1),
						getFacet(parent));
				names2facet.put(hierValue, result);
			}
			assert result != null;
			return result;
		}
	}

	void addItemFacet(int record_num, int facet_id) throws SQLException {
		// Util.print("addItemFacet " + facet_id);
		assert getName(facet_id) != null;
		assert getAttribute("record_num") != null;
		if (!dontUpdate) {
			// try {
			PreparedStatement itemFacet = lookupPS("REPLACE INTO raw_item_facet VALUES(?, ?)");
			// Util.print("addItemFacet " + record_num + " " + facet_id);
			itemFacet.setInt(1, record_num);
			itemFacet.setInt(2, facet_id);
			jdbc.SQLupdate(itemFacet);
			// } catch (SQLException e) {
			// e.printStackTrace();
			// }
		} else
			Util.print("REPLACE INTO raw_item_facet VALUES(" + record_num
					+ ", " + facet_id + ")");
	}

	int nextFacetID() throws SQLException {
		if (facetID < 0) {
			facetID = Math
					.max(
							jdbc
									.SQLqueryInt("SELECT MAX(facet_id) FROM raw_facet"),
							jdbc
									.SQLqueryInt("SELECT MAX(facet_type_id) FROM raw_facet_type") + 100);
		}
		return ++facetID;
	}

	int newFacet(String name, int parent) throws SQLException {
		// if (name.equals("Aerial views")) {
		// Util.err(name + " " + getName(parent) + " '" + data + "' " + tag
		// + subfield);
		// // Util.printStackTrace();
		// }
//		Util.print("new facet "+name);
		name = truncateName(name, false);
		if (!dontUpdate) {
			// facetCache compares names in binary mode, so may create facets
			// seen as duplicates by MySQL. We'll merge them later.
			assert facetCache != null || lookupFacet(name, parent) < 0 : name
					+ " (" + lookupFacet(name, parent) + ") " + getName(parent)
					+ " (" + parent + ")";
			try {
				PreparedStatement newFacet = lookupPS("INSERT INTO raw_facet VALUES(?, ?, ?, null)");
				newFacet.setInt(1, nextFacetID());
				newFacet.setString(2, name);
				newFacet.setInt(3, parent);
				// newFacet.setInt(4, sort);
				jdbc.SQLupdate(newFacet);
			} catch (SQLException e) {
				Util.err("Problem creating facet: name='" + name + "'; parent="
						+ getName(parent));
				e.printStackTrace();
			}
		} else
			Util.print("INSERT INTO raw_facet VALUES(<next facet_id>, " + name
					+ ", " + parent + ", null)");
		return facetID;
	}

	String truncateName(String name, boolean noWarn) throws SQLException {
		if (maxNameLength < 0) {
			maxNameLength = jdbc
					.SQLqueryInt("SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE table_name = 'raw_facet' AND column_name = 'name'"
							+ " AND table_schema = '" + jdbc.dbName + "'");
			log("Setting maxNameLength to " + maxNameLength);
		}
		if (name.length() > maxNameLength) {
			String tName = name.substring(0, maxNameLength - 1);
			if (!noWarn)
				Util.err("Truncating facet name '" + name + "' to '" + tName
						+ "'");
			name = tName;
		}
		return name;
	}

	int setParent(int child, int parent) {
		int result = child;
		if (!dontUpdate) {
			try {
				PreparedStatement getParent = lookupPS("SELECT facet_id FROM raw_facet "
						+ "WHERE parent_facet_id = ? AND name = ? LIMIT 1");
				getParent.setInt(1, parent);
				getParent.setString(2, getName(child));
				int old = jdbc.SQLqueryInt(getParent);
				if (old > 0) {
					merge(child, old);
					result = old;
				} else {
					PreparedStatement setParent = lookupPS("UPDATE raw_facet SET parent_facet_id = ? WHERE facet_id = ?");
					setParent.setInt(1, parent);
					setParent.setInt(2, child);
					jdbc.SQLupdate(setParent);
				}
			} catch (SQLException e) {
				Util.err(child + " " + parent);
				e.printStackTrace();
			}
		} else {
			Util.print("UPDATE raw_facet SET parent_facet_id = " + parent
					+ " WHERE facet_id = " + child);
		}
		return result;
	}

	void merge(int from, int to) throws SQLException {
		log("merging " + ancestorString(from, null) + " into "
				+ ancestorString(to, null));

		PreparedStatement ps = lookupPS("SELECT new.facet_id, old.facet_id FROM raw_facet new, raw_facet old "
				+ "WHERE new.name = old.name and new.parent_facet_id = ? AND old.parent_facet_id = ? LIMIT 1");
		ps.setInt(1, to);
		ps.setInt(2, from);
		ResultSet rs = jdbc.SQLquery(ps);
		int[][] childMergers = new int[MyResultSet.nRows(rs)][2];
		int index = 0;
		while (rs.next()) {
			int childFrom = rs.getInt(2);
			int childTo = rs.getInt(1);
			int[] merger = { childFrom, childTo };
			childMergers[index++] = merger;
		}
		rs.close();
		for (int i = 0; i < childMergers.length; i++) {
			merge(childMergers[i][0], childMergers[i][1]);
		}

		setChildrensParent(from, to);
		mergeFacet(from, to);
	}

	/**
	 * Clean up problems caused by comparing strings in binary mode.
	 */
	void mergeDuplicateFacets() throws SQLException {
		ResultSet pairs = jdbc
				.SQLquery("SELECT f1.facet_id, f2.facet_id FROM raw_facet f1"
						+ " INNER JOIN raw_facet f2 USING (parent_facet_id, name) "
						+ "WHERE f1.facet_id < f2.facet_id");
		int rows = MyResultSet.nRows(pairs);
		if (rows > 0) {
			Util.print(rows + " duplicate facets to merge...");
			while (pairs.next()) {
				int facet1 = pairs.getInt(1);
				int facet2 = pairs.getInt(2);
				merge(facet1, facet2);
			}
			Util.print("...merging facets done.");
		}
	}

	/**
	 * setParent all children of from to to
	 * 
	 * @param from
	 * @param to
	 * @throws SQLException
	 */
	void setChildrensParent(int from, int to) throws SQLException {
		PreparedStatement updateRawFacet = lookupPS("UPDATE raw_facet set parent_facet_id = ? "
				+ "WHERE parent_facet_id = ?");
		updateRawFacet.setInt(1, to);
		updateRawFacet.setInt(2, from);
		db(updateRawFacet, "Update raw_facet");
	}

	void deleteFacet(int facet) throws SQLException {
		PreparedStatement deleteFromRawFacet = lookupPS("DELETE FROM raw_facet WHERE facet_id = ?");
		deleteFromRawFacet.setInt(1, facet);
		db(deleteFromRawFacet, "Delete from raw_facet");

		PreparedStatement deleteFromRawItemFacet = lookupPS("DELETE FROM raw_item_facet WHERE facet_id = ?");
		deleteFromRawItemFacet.setInt(1, facet);
		db(deleteFromRawItemFacet, "Delete from raw_facet");
	}

	void deleteItem(int item) throws SQLException {
		PreparedStatement deleteItem = lookupPS("DELETE FROM item WHERE record_num = ?");
		deleteItem.setInt(1, item);
		db(deleteItem, "Delete merged item");

		PreparedStatement deleteItemFacets = lookupPS("DELETE FROM raw_item_facet WHERE record_num = ?");
		deleteItemFacets.setInt(1, item);
		db(deleteItemFacets, "Delete merged itemFacets");
	}

	void mergeFacet(int from, int to) throws SQLException {
		PreparedStatement updateRawItemFacet = lookupPS("REPLACE INTO raw_item_facet "
				+ "SELECT record_num, ? FROM raw_item_facet WHERE facet_id = ?");
		updateRawItemFacet.setInt(1, to);
		updateRawItemFacet.setInt(2, from);
		db(updateRawItemFacet, "Update raw_item_facet");

		deleteFacet(from);
	}

	void setName(int facet, String name) throws SQLException {
		PreparedStatement updateFromRawFacet = lookupPS("UPDATE raw_facet SET name = ? "
				+ "WHERE facet_id = ?");
		updateFromRawFacet.setString(1, name);
		updateFromRawFacet.setInt(2, facet);
		db(updateFromRawFacet, "Update facet name");
	}

	/**
	 * Assumes attributes are the same, and we're just collecting all the facets
	 * 
	 * @param from
	 * @param to
	 * @throws SQLException
	 */
	void mergeItems(int from, int to) throws SQLException {
		PreparedStatement mergeFacets = lookupPS("REPLACE INTO raw_item_facet "
				+ "SELECT ?, facet_id FROM raw_item_facet WHERE record_num = ?");
		mergeFacets.setInt(1, to);
		mergeFacets.setInt(2, from);
		db(mergeFacets, "Merge item facets");

		deleteItem(from);
	}

	int db(String sql) {
		int nRows = 0;
		if (dontUpdate) {
			Util.print(sql);
		} else {
			try {
				nRows = jdbc.SQLupdate(sql);
			} catch (SQLException e) {
				System.err.println("SQLException for " + sql);
				e.printStackTrace();
			}
		}
		return nRows;
	}

	int db(PreparedStatement SQL, String desc) throws SQLException {
		int nRows = 0;
		if (dontUpdate) {
			Util.print(desc);
		} else {
			try {
				nRows = jdbc.SQLupdate(SQL);
			} catch (SQLException e) {
				System.err.println("SQLException for " + desc);
				e.printStackTrace();
				throw (e);
			}
		}
		return nRows;
	}

	void log(String s) {
		if (verbose)
			Util.print(s);
	}

	String getName(int facet) throws SQLException {
		String result = getFacetName(facet);
		if (result == null)
			result = getFacetTypeName(facet);
		return result;
	}

	String getFacetName(int facet_id) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT name FROM raw_facet WHERE facet_id = ?");
		ps.setInt(1, facet_id);
		String result = jdbc.SQLqueryString(ps);
		return result;
	}

	private String getFacetTypeName(int facet_type_id) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT name FROM raw_facet_type WHERE facet_type_id = ?");
		ps.setInt(1, facet_type_id);
		String result = jdbc.SQLqueryString(ps);
		return result;
	}

	int getFacetType(int facet_id) throws SQLException {
		for (int facet_type_id = facet_id; facet_type_id > 0; facet_type_id = getParentFacet(facet_type_id)) {
			facet_id = facet_type_id;
		}
		return facet_id;
	}

	private int getParentFacet(int facet_id) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT parent_facet_id FROM raw_facet WHERE facet_id = ?");
		ps.setInt(1, facet_id);
		return jdbc.SQLqueryInt(ps);
	}

	String getAttribute(String column) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT " + column
				+ " FROM item WHERE record_num = ?");
		ps.setInt(1, recordNum());
		return jdbc.SQLqueryString(ps);
	}

	int getFacetType(String name) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT facet_type_id FROM raw_facet_type WHERE name = ?");
		ps.setString(1, name);
		int result = jdbc.SQLqueryInt(ps);
		assert result > 0 : name;
		return result;
	}

	int nItems(int facetID1) throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM raw_item_facet WHERE facet_id = "
						+ facetID1);
	}

	int lookupFacet(String name, int parent) {
		int result = -1;
		try {
			// Use index hint because it's spending lots of time in 'statistics'
			PreparedStatement lookupFacet = lookupPS("SELECT facet_id FROM raw_facet USE INDEX (name) WHERE name = ? AND parent_facet_id = ? LIMIT 1");
			lookupFacet.setString(1, truncateName(name, true));
			lookupFacet.setInt(2, parent);
			result = jdbc.SQLqueryInt(lookupFacet);
		} catch (Throwable e) {
			try {
				Util.err("rethrowing " + e);
				throw (new RuntimeException("While looking up " + name + " "
						+ parent + "(" + getName(parent) + ")", e));
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		// Util.print("lookupFacet " + name + " " + parent + " => " +
		// result);
		return result;
	}

//	/**
//	 * @param name
//	 *            look for facets with this name
//	 * @param type
//	 *            look for facets of this type
//	 * @return all facets with name and type
//	 * @throws SQLException
//	 */
//	int[] lookupFacets(String name, int type) throws SQLException {
//		int[] result = new int[0];
//		ResultSet rs = null;
//		try {
//			PreparedStatement lookupFacets = lookupPS("SELECT facet_id FROM raw_facet WHERE name = ?");
//			lookupFacets.setString(1, truncateName(name, true));
//			// lookupSomeFacet.setInt(2, type);
//			for (rs = jdbc.SQLquery(lookupFacets,
//					"Find one facet with name and type"); rs.next();) {
//				int facet = rs.getInt(1);
//				if (getFacetType(facet) == type) {
//					result = Util.push(result, facet);
//				}
//			}
//		} finally {
//			if (rs != null)
//				jdbc.close(rs);
//		}
//		return result;
//	}

	int[] getChildIDs(int parentFacet) throws SQLException {
		PreparedStatement ps = lookupPS("SELECT facet_id FROM raw_facet WHERE parent_facet_id = ?");
		ps.setInt(1, parentFacet);
		return jdbc.SQLqueryIntArray(ps);
	}

	String ancestorString(int facet, String delimiter) throws SQLException {
		if (delimiter == null)
			delimiter = " -- ";
		return Util.join(ancestors(facet, false), delimiter);
	}

	private List<String> ancestors(int facet, boolean showID)
			throws SQLException {
		List<String> result;
		if (facet > 0) {
			result = ancestors(getParentFacet(facet), showID);
			result.add(getName(facet) + (showID ? " (" + facet + ")" : ""));
		} else
			result = new LinkedList<String>();
		return result;
	}

	String itemDescription(int item) throws SQLException {
		PreparedStatement getDescription = lookupPS("SELECT CONCAT_WS("
				+ descriptionFields + ") FROM item WHERE record_num = ?");
		getDescription.setInt(1, item);
		return jdbc.SQLqueryString(getDescription);
	}
}
