package edu.cmu.cs.bungee.servlet;

/* 

 Created on Mar 4, 2005 

 Bungee View lets you search, browse, and data-mine an image collection.  
 Copyright (C) 2006  Mark Derthick

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.  See gpl.html.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 You may also contact the author at 
 mad@cs.cmu.edu, 
 or at
 Mark Derthick
 Carnegie-Mellon University
 Human-Computer Interaction Institute
 Pittsburgh, PA 15213

 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import JSci.maths.statistics.ChiSq2x2;

import edu.cmu.cs.bungee.dbScripts.ConvertFromRaw;
import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet.Column;

// Permissions to add
// GRANT SELECT, INSERT, UPDATE, CREATE, DELETE, CREATE TEMPORARY TABLES ON
// chartresvezelay.* TO p5@localhost

// Dump like this:
// mysqldump -u root -p --add-drop-database --databases wpa > wpa.sql

class Database {

	private JDBCSample jdbc;

	Database(String _server, String _db, String _user, String _pass,
			GenericServlet _servlet) throws SQLException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, ServletException {
		String connectString = _server + _db + "?user=" + _user;
		if (_pass != null)
			connectString += "&password=" + _pass;
		// servlet = _servlet;
		jdbc = new JDBCSample(_servlet);
		jdbc.openMySQL(connectString);
		ensureDBinitted();
		String item_id_column_type = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(record_num) FROM item"));
		String facet_id_column_type = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(facet_id) FROM facet"));

		reorderItems(-1); // order by random

		String[] createTempTables = {

				"CREATE TEMPORARY TABLE onItems (record_num "
						+ item_id_column_type
						+ ", PRIMARY KEY (record_num)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				"CREATE TEMPORARY TABLE restricted (record_num "
						+ item_id_column_type
						+ ", PRIMARY KEY (record_num)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				"CREATE TEMPORARY TABLE relevantFacets (" + "facet_id "
						+ facet_id_column_type + ", "
						+ "PRIMARY KEY USING BTREE (facet_id)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

		};
		jdbc.SQLupdate(createTempTables);

		filteredCountQuery = jdbc
				.prepareStatement("SELECT f.facet_id, COUNT(*) AS cnt "
						+ "FROM relevantFacets f "
						+ "INNER JOIN item_facet_heap i_f USING (facet_id) "
						+ "INNER JOIN onItems USING (record_num) "
						+ "GROUP BY f.facet_id ORDER BY f.facet_id");

		filteredCountTypeQuery = jdbc
				.prepareStatement("SELECT f.facet_id, COUNT(*) AS cnt "
						+ "FROM item_facet_type_heap f "
						+ "INNER JOIN onItems USING (record_num) "
						+ "GROUP BY f.facet_id ");

		String[] prefetchFROM = {
				" FROM facet WHERE parent_facet_id = ? ORDER BY facet_id",
				" FROM (SELECT facet_id, count(restricted.record_num) AS n_items, n_child_facets, first_child_offset, name"
						+ " FROM facet INNER JOIN item_facet_heap USING (facet_id)"
						+ " LEFT JOIN restricted USING (record_num) WHERE parent_facet_id = ?"
						+ " GROUP BY facet_id) foo ORDER BY facet_id" };

		prefetchQuery = jdbc
				.prepareStatement("SELECT n_items, n_child_facets, name "
						+ prefetchFROM[0]);

		prefetchNoCountQuery = jdbc
				.prepareStatement("SELECT n_child_facets, name"
						+ prefetchFROM[0]);

		prefetchNoNameQuery = jdbc
				.prepareStatement("SELECT n_items, n_child_facets"
						+ prefetchFROM[0]);

		prefetchNoCountNoNameQuery = jdbc
				.prepareStatement("SELECT n_child_facets" + prefetchFROM[0]);

		prefetchQueryRestricted = jdbc
				.prepareStatement("SELECT n_items, n_child_facets, name"
						+ prefetchFROM[1]);

		prefetchNoNameQueryRestricted = jdbc
				.prepareStatement("SELECT n_items, n_child_facets"
						+ prefetchFROM[1]);

		getItemInfoQuery = jdbc
				.prepareStatement(
						"SELECT parent_facet_id, f.facet_id, name, "
								+ "n_child_facets, first_child_offset, n_items"
								+ " FROM item_facet_heap i INNER JOIN facet f USING (facet_id)"
								+ " WHERE record_num = ? ORDER BY f.facet_id",
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);

		printUserActionStmt = jdbc
				.prepareStatement("INSERT INTO user_actions VALUES(NOW(), ?, ?, ?, ?, ?, ?)");
	}

	void close() throws SQLException {
		jdbc.close();
		jdbc = null;
	}

	String aboutCollection() throws SQLException {
		return jdbc.SQLqueryString("SELECT aboutURL FROM globals");
	}

	int facetCount() throws SQLException {
		return jdbc.SQLqueryInt("SELECT COUNT(*) FROM facet");
	}

	int itemCount() throws SQLException {
		return jdbc.SQLqueryInt("SELECT COUNT(*) FROM item");
	}

	String[] getGlobals() throws SQLException, ServletException {
		String[] result = null;
		ResultSet rs = null;
		try {
			rs = jdbc
					.SQLquery("SELECT itemDescriptionFields, genericObjectLabel, itemURL, itemURLdoc, isEditable FROM globals");
			if (!rs.next())
				error("Can't get globals");
			String itemDescriptionFields = rs.getString(1);

			// Work around MySQL Connector bug: once the PreparedStatement
			// encounters a null image, it returns null from then on.
			imageQuery = // jdbc.prepareStatement(
			"SELECT CONCAT_WS('\n \n', " + itemDescriptionFields
					+ ") descript, image, w, h FROM item LEFT JOIN images "
					+ "ON item.record_num = images.record_num "
					+ "WHERE item.record_num = ";

			String itemURLgetter = rs.getString(3);
			itemIdPS = jdbc.prepareStatement("SELECT " + itemURLgetter
					+ " FROM item WHERE record_num = ?");
			itemURLPS = jdbc
					.prepareStatement("SELECT record_num FROM item WHERE "
							+ itemURLgetter + " = ?");

			String[] resultx = { itemDescriptionFields, rs.getString(2),
					rs.getString(4), rs.getString(5) };
			result = resultx;
		} finally {
			if (rs != null)
				jdbc.close(rs);
		}
		return result;
	}

	private PreparedStatement itemIdPS;

	private PreparedStatement itemURLPS;

	String getItemURL(int item) throws SQLException, ServletException {
		// Util.print("itemDesc " + item);
		// try {
		synchronized (itemIdPS) {
			itemIdPS.setInt(1, item);
			String result = jdbc.SQLqueryString(itemIdPS, "Get item URL.");
			myAssert(result != null, "Can't find "
					+ jdbc.SQLqueryString("SELECT itemURL FROM globals")
					+ " for record_num " + item);
			return result;
		}
		// } catch (SQLException se) {
		// System.err
		// .println("SQL Exception in getItemID: " + se.getMessage());
		// se.printStackTrace();
		// }
	}

	private void ensureDBinitted() throws SQLException {
		if (jdbc.SQLqueryInt("SELECT COUNT(*) FROM item_facet_heap") == 0) {
			jdbc
					.SQLupdate("INSERT INTO item_facet_heap SELECT * FROM item_facet;");
			jdbc.SQLupdate("TRUNCATE TABLE item_facet_type_heap;");
			jdbc
					.SQLupdate("INSERT INTO item_facet_type_heap "
							+ "SELECT distinct i.record_num, p.facet_id "
							+ "FROM facet f INNER JOIN facet p ON f.parent_facet_id = p.facet_id "
							+ "INNER JOIN item_facet i ON i.facet_id = f.facet_id "
							+ "WHERE p.parent_facet_id = 0;");
			jdbc.SQLupdate("TRUNCATE TABLE item_order_heap;");
			jdbc.SQLupdate("INSERT INTO item_order_heap "
					+ "SELECT * FROM item_order");
			updateUsageCounts();
		}
	}

	private void updateUsageCounts() throws SQLException {
		jdbc
				.SQLupdate("UPDATE facet, (SELECT COUNT(*) cnt, object FROM user_actions "
						+ "WHERE location IN (1,2,6) GROUP BY object) temp "
						+ "SET usage_count = cnt WHERE object = facet_id");
	}

	String sortedResultTable(int i) throws ServletException {
		switch (i) {
		case 0:
			// no filters
			return "item_order_heap";
		case 1:
			// restricted but no filters
			return "restricted";
		case 2:
			// filters applied
			return "onItems";
		}
		myAssert(false, "Bad table index: " + i);
		return null;
	}

	/**
	 * Update offsetItemsQuery (used by offsetItems) and itemOffsetQuery (1 and
	 * 2) (used by itemOffset) so they return indexes sorted appropriately.
	 * 
	 * The indexes into these two arrays show which table contains the on items:
	 * 
	 * @see #sortedResultTable
	 * 
	 * @param facetType
	 *            the item_order_heap column to sort by -1 means random, 0 means
	 *            ID, else the facet_type_ID
	 * @throws SQLException
	 * @throws ServletException
	 * 
	 */
	void reorderItems(int facetType) throws SQLException, ServletException {
		String columnToSortBy = facetType < 0 ? "random_ID"
				: facetType == 0 ? "record_num" : "col" + facetType;
		offsetItemsQuery = new PreparedStatement[3];
		itemOffsetQuery1 = new PreparedStatement[offsetItemsQuery.length];
		itemOffsetQuery2 = new PreparedStatement[offsetItemsQuery.length];
		for (int i = 1; i < offsetItemsQuery.length; i++) {
			offsetItemsQuery[i] = jdbc
					.prepareStatement("SELECT o.record_num FROM "
							+ sortedResultTable(i)
							+ " o INNER JOIN item_order_heap r USING (record_num)"
							+ " ORDER BY r." + columnToSortBy + " LIMIT ?, ?");

			// Have to use 2 queries to work around the "can't reopen temporary
			// table" problem in MySQL
			// Only call query 2 if query 1 result > 0 (should never be exactly
			// 0)
			// Argument to query 2 is the result of query 1
			itemOffsetQuery1[i] = jdbc.prepareStatement("SELECT s."
					+ columnToSortBy + " FROM item_order_heap s INNER JOIN "
					+ sortedResultTable(i)
					+ " USING (record_num) WHERE s.record_num = ?");
			itemOffsetQuery2[i] = jdbc
					.prepareStatement("SELECT COUNT(*) FROM item_order_heap r "
							+ "INNER JOIN " + sortedResultTable(i)
							+ " USING (record_num) WHERE r." + columnToSortBy
							+ " < ?");
		}
		offsetItemsQuery[0] = jdbc.prepareStatement("SELECT record_num FROM "
				+ "item_order_heap ORDER BY " + columnToSortBy + " LIMIT ?, ?");

		itemOffsetQuery1[0] = jdbc.prepareStatement("SELECT s."
				+ columnToSortBy
				+ " FROM item_order_heap s WHERE s.record_num = ?");
		itemOffsetQuery2[0] = jdbc
				.prepareStatement("SELECT COUNT(*)-1 FROM item_order_heap "
						+ " WHERE " + columnToSortBy + " <= ?");
	}

	int getItemFromURL(String URL) throws SQLException {
		synchronized (itemURLPS) {
			itemURLPS.setString(1, URL);
			return jdbc.SQLqueryInt(itemURLPS, "Get item record_num from URL.");
		}
	}

	@SuppressWarnings("unchecked")
	void getCountsIgnoringFacet(String subQuery, String facet_id,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT f.facet_id, COUNT(onItemsFake.record_num) AS cnt "
						+ "FROM facet f "
						+ "INNER JOIN item_facet_heap i_f USING (facet_id) "
						+ "INNER JOIN ("
						+ subQuery
						+ ") onItemsFake USING (record_num) "
						+ "WHERE f.parent_facet_id = "
						+ facet_id
						+ " GROUP BY f.facet_id ORDER BY f.facet_id");
		sendResultSet(rs, MyResultSet.SINT_PINT, out);
	}

	private PreparedStatement filteredCountQuery;

	private PreparedStatement filteredCountTypeQuery;

	@SuppressWarnings("unchecked")
	void getFilteredCounts(String perspectivesToAdd,
			String perspectivesToRemove, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		updateRelevantFacets(perspectivesToAdd, perspectivesToRemove);
		synchronized (filteredCountQuery) {
			ResultSet rs = jdbc.SQLquery(filteredCountQuery,
					"Get filtered counts.");
			sendResultSet(rs, MyResultSet.SINT_PINT, out);
		}
	}

	@SuppressWarnings("unchecked")
	void getFilteredCountTypes(DataOutputStream out) throws SQLException,
			ServletException, IOException {
		synchronized (filteredCountTypeQuery) {
			ResultSet rs = jdbc.SQLquery(filteredCountTypeQuery,
					"Get filtered count types.");
			sendResultSet(rs, MyResultSet.SINT_PINT, out);
		}
	}

	void updateRelevantFacets(String perspectivesToAdd,
			String perspectivesToRemove) throws SQLException {
		List<String> SQL = new ArrayList<String>();
		if (perspectivesToAdd != null && perspectivesToAdd.length() > 0) {
			SQL.add(updateRelevantFacetsInternal(perspectivesToAdd, false));
			perspectivesToAdd = null;
		}
		if (perspectivesToRemove != null && perspectivesToRemove.length() > 0) {
			SQL.add(updateRelevantFacetsInternal(perspectivesToRemove, true));
			perspectivesToRemove = null;
		}
		// jdbc
		// .print(jdbc
		// .SQLqueryString("SELECT GROUP_CONCAT(facet_id) FROM
		// relevantFacets"));
		if (SQL.size() > 0) {
			if (SQL.size() == 1) {
				jdbc.SQLupdate(SQL.get(0));
			} else {
				jdbc.SQLupdate(SQL.toArray(new String[0]));
			}
		}
		// jdbc
		// .print(jdbc
		// .SQLqueryString("SELECT GROUP_CONCAT(facet_id) FROM
		// relevantFacets"));
	}

	private static String updateRelevantFacetsInternal(String persps,
			boolean isDelete) {
		StringBuffer buf = new StringBuffer(persps.length() + 80);
		if (isDelete)
			buf
					.append("DELETE FROM relevantFacets USING relevantFacets, facet "
							+ "WHERE relevantFacets.facet_id = facet.facet_id "
							+ "AND parent_facet_id IN (");
		else
			buf
					.append("REPLACE INTO relevantFacets SELECT facet_id FROM facet WHERE parent_facet_id IN (");
		// Use REPLACE in case asynchrony has messed things up
		buf.append(persps);
		buf.append(")");
		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	void initPerspectives(DataOutputStream out) throws SQLException,
			ServletException, IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT facet.name, descriptionCategory, descriptionPreposition, "
						+ "n_child_facets, first_child_offset, n_items, isOrdered "
						+ "FROM raw_facet_type ft INNER JOIN facet USING (name) "
						+ "WHERE facet.parent_facet_id = 0 "
						+ "ORDER BY facet.facet_id");
		sendResultSet(rs, MyResultSet.STRING_STRING_STRING_INT_INT_INT_INT, out);
	}

	int updateOnItems(String onSQL) throws SQLException {
		// lastQuery = onSQL;
		jdbc.SQLupdate("TRUNCATE TABLE onItems");
		return jdbc.SQLupdate("INSERT INTO onItems " + onSQL);
	}

	private PreparedStatement prefetchQuery;

	private PreparedStatement prefetchNoCountQuery;

	private PreparedStatement prefetchNoNameQuery;

	private PreparedStatement prefetchNoCountNoNameQuery;

	private PreparedStatement prefetchQueryRestricted;

	private PreparedStatement prefetchNoNameQueryRestricted;

	// The client uses java 1.4, so the compiler can't check the MyResultSet
	// constants
	@SuppressWarnings("unchecked")
	void prefetch(int facet_id, int args, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		// log("prefetch " + facet_id + " " + args);
		int children_offset = jdbc
				.SQLqueryInt("SELECT first_child_offset FROM facet WHERE facet_id = "
						+ facet_id);
		writeInt(children_offset, out);
		int isAlphabetic = jdbc.SQLqueryInt("SELECT is_alphabetic FROM facet WHERE facet_id = "
						+ facet_id);
		writeInt(isAlphabetic, out);

		String message = "getting initial counts and names of facet children";
		PreparedStatement ps;
		List<Object> types;
		switch (args) {
		case 1:
			ps = prefetchQuery;
			types = MyResultSet.INT_INT_STRING;
			break;
		case 2:
			ps = prefetchNoNameQuery;
			types = MyResultSet.INT_INT;
			break;
		case 3:
			ps = prefetchNoCountQuery;
			types = MyResultSet.INT_STRING;
			break;
		case 4:
			ps = prefetchNoCountNoNameQuery;
			types = MyResultSet.INT;
			break;
		case 5:
			ps = prefetchQueryRestricted;
			types = MyResultSet.INT_INT_STRING;
			break;
		default:
			myAssert(args == 6, "prefetch args=" + args);
			ps = prefetchNoNameQueryRestricted;
			types = MyResultSet.INT_INT;
			break;
		}
		synchronized (ps) {
			// try {
			ps.setInt(1, facet_id);
			ResultSet rs = jdbc.SQLquery(ps, message);
			sendResultSet(rs, types, out);
		}
	}

	@SuppressWarnings("unchecked")
	void getNames(String facets, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT name FROM facet WHERE facet_id IN(" + facets
						+ ") ORDER BY facet_id");
		sendResultSet(rs, MyResultSet.STRING, out);
	}

	@SuppressWarnings("unchecked")
	void init(DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet rs = jdbc.SQLquery("SELECT f.n_items as cnt "
				+ "FROM facet f INNER JOIN facet parent "
				+ "ON f.parent_facet_id = parent.facet_id "
				+ "WHERE parent.parent_facet_id = 0 " + "ORDER BY f.facet_ID");
		sendResultSet(rs, MyResultSet.INT, out);
	}

	/**
	 * Return the recordNum's for a range of offsets.
	 */
	private PreparedStatement[] offsetItemsQuery;

	@SuppressWarnings("unchecked")
	void offsetItems(int minOffset, int maxOffset, int table,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		synchronized (offsetItemsQuery) {
			int nRows = maxOffset - minOffset;
			PreparedStatement s = offsetItemsQuery[table];
			s.setInt(1, minOffset);
			s.setInt(2, nRows);
			ResultSet rs = jdbc.SQLquery(s, "Get offsets");
			// myAssert(MyResultSet.nRows(rs) == nRows
			// || jdbc.SQLqueryInt("SELECT COUNT(*) FROM "
			// + sortedResultTable(table)) < nRows, minOffset
			// + "-" + nRows + " " + MyResultSet.nRows(rs));
			sendResultSet(rs, MyResultSet.INT, out);
		}
	}

	@SuppressWarnings("unchecked")
	ResultSet getThumbs(String items, int imageW, int imageH, int quality,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT record_num, image, w, h FROM images WHERE record_num IN("
						+ items + ") ORDER BY record_num");
		sendResultSet(rs, MyResultSet.SINT_IMAGE_INT_INT, imageW, imageH,
				quality, out);
		return rs;
	}

	private String imageQuery;

	@SuppressWarnings("unchecked")
	ResultSet getDescAndImage(int item, int imageW, int imageH, int quality,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		// synchronized (imageQuery) {
		// imageQuery.setInt(1, item);
		ResultSet rs = jdbc.SQLquery(imageQuery + item);
		sendResultSet(rs, MyResultSet.STRING_IMAGE_INT_INT, imageW, imageH,
				quality, out);
		return rs;
		// }
	}

	/**
	 * Return the ordinal for a single Item.
	 */
	private PreparedStatement[] itemOffsetQuery1;
	/**
	 * Return the offset for that ordinal.
	 */
	private PreparedStatement[] itemOffsetQuery2;

	// private String lastQuery;

	/**
	 * @param item
	 * @param table
	 *            see reorderItems
	 * @return the offset into the on items table of this item. -1 means not
	 *         found.
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	int itemOffset(int item, int table) throws SQLException {
		int offset = -1;
		PreparedStatement s1 = itemOffsetQuery1[table];
		synchronized (s1) {
			s1.setInt(1, item);

			int ordinal = jdbc.SQLqueryInt(s1,
					"Get onItems offset from record_num.");
			if (ordinal > 0) {
				PreparedStatement s2 = itemOffsetQuery2[table];
				s2.setInt(1, ordinal);
				offset = jdbc.SQLqueryInt(s2,
						"Get onItems offset from record_num.");
			}

			// try {
			// int onCount = jdbc.SQLqueryInt("SELECT COUNT(*) FROM "
			// + sortedResultTable(table));
			// if (onCount < 5 || offset >= onCount) {
			// printRecords(
			// jdbc
			// .SQLquery("SELECT s.record_num, s.random_id FROM item_order_heap
			// s "
			// + "INNER JOIN onItems Using (record_num)"),
			// MyResultSet.INT_INT);
			// log(item + " " + offset + " " + table);
			// throw new ServletException("Bad index: " + offset + " >= "
			// + onCount + " for table " + table + " item " + item
			// + " query " + lastQuery);
			// }
			// } catch (ServletException e) {
			// e.printStackTrace();
			// }
		}
		// Util.print("itemIndex " + item + " => " + result);
		return offset;
	}

	private PreparedStatement getItemInfoQuery;

	@SuppressWarnings("unchecked")
	void getItemInfo(int item, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		// try {
		synchronized (getItemInfoQuery) {
			getItemInfoQuery.setInt(1, item);
			// } catch (SQLException e) {
			// e.printStackTrace();
			// }
			ResultSet rs = jdbc.SQLquery(getItemInfoQuery,
					"Get item facets for building FacetTree.");
			sendResultSet(rs, MyResultSet.PINT_SINT_STRING_INT_INT_INT, out);
		}
	}

	private PreparedStatement printUserActionStmt;

	void printUserAction(String client, int session, int actionIndex,
			int location, String object, int modifiers) throws SQLException {
		synchronized (printUserActionStmt) {
			printUserActionStmt.setInt(1, actionIndex);
			printUserActionStmt.setInt(2, location);
			printUserActionStmt.setString(3, object);
			printUserActionStmt.setInt(4, modifiers);
			printUserActionStmt.setInt(5, session);
			printUserActionStmt.setString(6, client);
			jdbc.SQLupdate(printUserActionStmt, "Print user action.");
		}
	}

	void restrict() throws SQLException {
		jdbc.SQLupdate("TRUNCATE TABLE restricted");
		jdbc.SQLupdate("INSERT INTO restricted SELECT * FROM onItems");
	}

	void addItemFacet(int facet, int item, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkNonNegative(item); // Dummy instance has ID=0
		checkPositive(facet);
		int prev = facet;
		int ancestor;
		while ((ancestor = jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
						+ prev)) > 0) {
			jdbc.SQLupdate("REPLACE INTO item_facet_heap VALUES(" + item + ", "
					+ prev + ")");
			prev = ancestor;
		}
		jdbc.SQLupdate("REPLACE INTO item_facet_type_heap VALUES(" + item
				+ ", " + prev + ")");
		updateFacetCounts(facet, out);
	}

	void addItemsFacet(int facet, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		checkPositive(facet);
		int prev = facet;
		int ancestor;
		while ((ancestor = jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
						+ prev)) > 0) {
			jdbc.SQLupdate("REPLACE INTO item_facet_heap SELECT record_num, "
					+ prev + " FROM onItems");
			prev = ancestor;
		}
		jdbc.SQLupdate("REPLACE INTO item_facet_type_heap SELECT record_num, "
				+ prev + " FROM onItems");
		updateFacetCounts(facet, out);
	}

	int[] removeItemsFacet(int facet, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkPositive(facet);
		jdbc.SQLupdate("DELETE FROM ifh USING item_facet_heap ifh, onItems oi "
				+ "WHERE ifh.record_num = oi.record_num AND ifh.facet_id = "
				+ facet);
		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
						+ facet);
		int[] result = null;
		boolean hasChildren = false;
		while (rs.next()) {
			hasChildren = true;
			result = Util.append(result, removeItemsFacet(rs.getInt(1), null));
		}
		if (!hasChildren) {
			result = new int[1];
			result[0] = facet;
		}
		int grandparent = jdbc
				.SQLqueryInt("SELECT parent.parent_facet_id FROM facet f "
						+ "INNER JOIN facet parent ON f.parent_facet_id = parent.facet_id "
						+ "WHERE f.facet_id = " + facet);
		checkNonNegative(grandparent);
		if (grandparent == 0) {
			int parent = jdbc
					.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
							+ facet);
			checkPositive(parent);
			jdbc
					.SQLupdate("DELETE FROM ifh USING item_facet_type_heap ifh, onItems oi "
							+ "WHERE ifh.record_num = oi.record_num AND ifh.facet_id = "
							+ parent);
		}
		if (out != null)
			updateFacetCounts(result, out);
		return result;
	}

	int[] removeItemFacet(int facet, int item, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkPositive(facet);
		checkPositive(item);
		jdbc.SQLupdate("DELETE FROM item_facet_heap WHERE record_num = " + item
				+ " AND facet_id = " + facet);
		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
						+ facet);
		int[] result = null;
		boolean hasChildren = false;
		while (rs.next()) {
			hasChildren = true;
			result = Util.append(result, removeItemFacet(rs.getInt(1), item,
					null));
		}
		if (!hasChildren) {
			result = new int[1];
			result[0] = facet;
		}
		int grandparent = jdbc
				.SQLqueryInt("SELECT parent.parent_facet_id FROM facet f "
						+ "INNER JOIN facet parent ON f.parent_facet_id = parent.facet_id "
						+ "WHERE f.facet_id = " + facet);
		checkNonNegative(grandparent);
		if (grandparent == 0) {
			int parent = jdbc
					.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
							+ facet);
			checkPositive(parent);
			jdbc
					.SQLupdate("DELETE FROM item_facet_type_heap WHERE record_num = "
							+ item + " AND facet_id = " + parent);
		}
		if (out != null)
			updateFacetCounts(result, out);
		return result;
	}

	void addChildFacet(int parent, String name, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkNonNegative(parent);
		int child = jdbc.SQLqueryInt("SELECT MAX(facet_id) + 1 FROM facet");
		checkPositive(child);
		jdbc.SQLupdate("INSERT INTO facet VALUES(" + child + ", '" + name
				+ "', " + parent + ", 1, 0, 0, 0)");
		if (parent > 0) {
			addItemFacet(child, 0, null);
			renumber(parent);
			child = jdbc
					.SQLqueryInt("SELECT facet_id FROM renames WHERE old_facet_id = "
							+ child);
			checkPositive(child);
			updateFacetCounts(child, out);
		} else {
			int order = jdbc
					.SQLqueryInt("SELECT MAX(sort) + 1 FROM raw_facet_type");
			myAssert(order < -127 || order > 127, "Bad order " + order);
			jdbc.SQLupdate("INSERT INTO raw_facet_type VALUES(" + child + ", '"
					+ name
					+ "', null, 'content', ' that show ; that don\\'t show ', "
					+ order + ", 0)");
			addChildFacet(child, "dummy", out);
		}
	}

	void reparent(int parent, int child, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkNonNegative(parent);
		checkPositive(child);
		int oldParent = jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
						+ child);
		jdbc.SQLupdate("UPDATE facet SET parent_facet_id = " + parent
				+ " WHERE facet_id = " + child);
		ResultSet rs = jdbc
				.SQLquery("SELECT record_num FROM item_facet_heap WHERE facet_id = "
						+ child);
		while (rs.next()) {
			int item = rs.getInt(1);
			addItemFacet(parent, item, null);
		}
		renumber(oldParent);
		renumber(parent);
		child = jdbc
				.SQLqueryInt("SELECT facet_id FROM renames WHERE old_facet_id = "
						+ child);
		int[] leafs = { oldParent, child };
		updateFacetCounts(leafs, out);
	}

	private void createRename() throws SQLException {
		jdbc
				.SQLupdate("CREATE TEMPORARY TABLE IF NOT EXISTS renames (old_facet_id INT, facet_id INT, PRIMARY KEY (facet_id))");
		// jdbc
		// .SQLupdate("TRUNCATE TABLE renames");
	}

	private void renumber(int facet) throws SQLException, ServletException {
		boolean relevant = jdbc
				.SQLqueryInt("SELECT f.facet_id FROM relevantFacets "
						+ "INNER JOIN facet f USING (facet_id) WHERE parent_facet_id = "
						+ facet + " LIMIT 1") > 0;
		if (relevant)
			updateRelevantFacets(null, Integer.toString(facet));
		createRename();
		int child = jdbc.SQLqueryInt("SELECT MAX(facet_id) + 1 FROM facet");
		jdbc.SQLupdate("UPDATE facet SET first_child_offset = " + (child - 1)
				+ " WHERE facet_id = " + facet);
		ResultSet rs = null;
		try {
			rs = jdbc
					.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
							+ facet + " ORDER BY name");
			while (rs.next()) {
				int old = rs.getInt(1);
				// jdbc.print(old + " => " + child);
				jdbc.SQLupdate("INSERT INTO renames VALUES(" + old + ", "
						+ child + ")");
				jdbc.SQLupdate("UPDATE item_facet_heap SET facet_id = " + child
						+ " WHERE facet_id = " + old);
				jdbc.SQLupdate("UPDATE facet SET facet_id = " + child
						+ " WHERE facet_id = " + old);
				jdbc.SQLupdate("UPDATE facet SET parent_facet_id = " + child
						+ " WHERE parent_facet_id = " + old);
				child++;
			}
		} finally {
			rs.close();
		}
		if (relevant)
			updateRelevantFacets(Integer.toString(facet), null);
		try {
			rs = jdbc
					.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
							+ facet + " ORDER BY name");
			while (rs.next()) {
				child = rs.getInt(1);
				renumber(child);
			}
		} finally {
			rs.close();
		}
	}

	private void updateFacetCounts(int facet, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		int[] leafs = { facet };
		updateFacetCounts(leafs, out);
	}

	@SuppressWarnings("unchecked")
	private void updateFacetCounts(int[] leafFacets, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		int[] ancestors = null;
		for (int i = 0; i < leafFacets.length; i++) {
			int ancestor = leafFacets[i];
			do {
				int nChildren = jdbc
						.SQLqueryInt("SELECT COUNT(*) FROM facet WHERE parent_facet_id = "
								+ ancestor);
				int nItems = 0;
				int parent = jdbc
						.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
								+ ancestor);
				if (parent > 0) {
					nItems = jdbc
							.SQLqueryInt("SELECT COUNT(*) FROM item_facet_heap WHERE facet_id = "
									+ ancestor);
				} else {
					nItems = jdbc
							.SQLqueryInt("SELECT COUNT(DISTINCT record_num) FROM item_facet_heap ifh "
									+ "INNER JOIN facet f USING (facet_id) WHERE parent_facet_id = "
									+ ancestor);
				}
				if (nItems == 0) {
					addItemFacet(ancestor, 0, null);
					nItems = 1;
				}
				jdbc.SQLupdate("UPDATE facet f SET n_items = " + nItems + ", "
						+ "n_child_facets = " + nChildren
						+ " WHERE facet_id = " + ancestor);
				ancestors = Util.push(ancestors, ancestor);
				ancestor = parent;
			} while (ancestor > 0 && !Util.isMember(ancestors, ancestor));
		}

		if (out != null) {
			createRename();
			// jdbc.print(jdbc.SQLqueryString("SELECT
			// group_concat(concat(old_facet_id, ' => ', facet_id)) FROM renames
			// GROUP BY 1"));
			String renamed = jdbc
					.SQLqueryString("SELECT GROUP_CONCAT(facet_id) FROM renames");
			String updated = Util.join(ancestors) + ", " + renamed;
			ResultSet rs = jdbc
					.SQLquery("SELECT f.facet_id facet_id, IFNULL(old_facet_id, f.facet_id) old, n_items, "
							+ "first_child_offset, parent_facet_id "
							+ "FROM facet f LEFT JOIN renames r USING (facet_id) "
							+ "WHERE f.facet_id IN ("
							+ updated
							+ ") ORDER BY f.facet_id");
			// printRecords(rs, MyResultSet.SINT_INT_INT_INT_INT);
			sendResultSet(rs, MyResultSet.SINT_INT_INT_INT_INT, out);
			jdbc.SQLupdate("DROP TABLE renames");
		}
	}

	void writeBack() throws SQLException {
		int delta = jdbc.SQLqueryInt("SELECT MAX(facet_id) FROM facet");
		jdbc
				.SQLupdate("UPDATE facet f, facet parent SET f.parent_facet_id = f.parent_facet_id + "
						+ delta
						+ " WHERE f.parent_facet_id = parent.facet_id "
						+ "AND parent.parent_facet_id > 0");
		jdbc.SQLupdate("UPDATE facet SET facet_id = facet_id + " + delta
				+ " WHERE parent_facet_id > 0");
		jdbc.SQLupdate("UPDATE item_facet_heap SET facet_id = facet_id + "
				+ delta);
		jdbc.SQLupdate("delete from item_facet_heap where record_num = 0");

		jdbc.SQLupdate("DROP TABLE IF EXISTS rft");
		jdbc
				.SQLupdate("CREATE TEMPORARY TABLE rft AS"
						+ " SELECT IFNULL(f.facet_id, 0) oldID, COUNT(*) ID, r.name, "
						+ "r.descriptionCategory, r.descriptionPreposition, r.sort, r.isOrdered "
						+ "FROM raw_facet_type r LEFT JOIN facet f USING (name) "
						+ "INNER JOIN raw_facet_type prev ON prev.name <= r.name "
						+ "WHERE f.parent_facet_id = 0 OR f.parent_facet_id IS NULL "
						+ "GROUP BY r.name ORDER BY null");

		jdbc.SQLupdate("UPDATE facet f, rft SET f.parent_facet_id = rft.ID + "
				+ (2 * delta) + " WHERE f.parent_facet_id = rft.oldID");
		jdbc.SQLupdate("UPDATE facet SET parent_facet_id = parent_facet_id - "
				+ (2 * delta) + " WHERE parent_facet_id > " + (2 * delta));

		jdbc.SQLupdate("TRUNCATE TABLE raw_facet_type");
		jdbc
				.SQLupdate("INSERT INTO raw_facet_type SELECT ID, name, "
						+ "descriptionCategory, descriptionPreposition, sort, isOrdered FROM rft");
		// jdbc.SQLupdate("DROP TABLE rft");
		jdbc.SQLupdate("TRUNCATE TABLE raw_item_facet");
		jdbc
				.SQLupdate("INSERT INTO raw_item_facet SELECT * FROM item_facet_heap");
		jdbc.SQLupdate("TRUNCATE TABLE raw_facet");
		jdbc
				.SQLupdate("INSERT INTO raw_facet SELECT facet_id, name, parent_facet_id, parent_facet_id, '' FROM facet "
						+ "WHERE parent_facet_id > 0");
		// while (jdbc
		// .SQLqueryInt("SELECT 1 FROM raw_facet f, raw_facet parent WHERE
		// f.facet_type_idxx != parent.facet_type_idxx "
		// + "AND f.parent_facet_id = parent.facet_id LIMIT 1") > 0) {
		// jdbc
		// .SQLupdate("UPDATE raw_facet f, raw_facet parent SET
		// f.facet_type_idxx = parent.facet_type_idxx "
		// + "WHERE f.parent_facet_id = parent.facet_id");
		// }
		ConvertFromRaw converter = new ConvertFromRaw(jdbc);
		converter.findBrokenLinks(true, 1);
		converter.convert(1000);
	}

	void rotate(int item, int clockwiseDegrees) throws SQLException,
			IOException, ServletException {
		ResultSet rs = jdbc
				.SQLquery("SELECT image, URI FROM images WHERE record_num = "
						+ item);
		if (rs.next()) {
			InputStream in = rs.getBlob(1).getBinaryStream();
			BufferedImage im = Util.readCompatibleImage(in);
			BufferedImage rot = Util.rotate(im, Math
					.toRadians(clockwiseDegrees));
			int w = rot.getWidth();
			int h = rot.getHeight();
			Util.writeImage(rot, 85, "C:\\temp\\temp.jpg");
			jdbc
					.SQLupdate("UPDATE images SET image = LOAD_FILE('C:\\\\temp\\\\temp.jpg'), w = "
							+ w + ", h = " + h + "WHERE record_num = " + item);

			String filename = rs.getString(2);
			myAssert(filename.endsWith(".jpg"), "rotate filname=" + filename);
			File f = new File(filename);
			im = Util.read(f.toURL());
			f.renameTo(new File(filename.substring(0, filename.length() - 4)
					+ "_unrotated.jpg"));
			rot = Util.rotate(im, Math.toRadians(clockwiseDegrees));
			Util.writeImage(rot, 85, filename);
		}
	}

	void rename(int facet, String name) throws SQLException {
		jdbc.SQLupdate("UPDATE facet SET name = '" + name
				+ "' WHERE facet_id = " + facet);
		jdbc.SQLupdate("UPDATE raw_facet_type SET name = '" + name
				+ "' WHERE facet_type_id = " + facet);
	}

	// facetRestriction is the empty string or a where clause restricting
	// facet_id, or even joins, e.g.
	// INNER JOIN facet f ON f.facet_id = i.facet_id WHERE f.lft BETWEEN 56 AND
	// 87
	void cluster(int maxClusters, int maxClusterSize, String facetRestriction,
			double pValue, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		if (maxClusterSize > 3)
			// MySQL crashes on size 4 query
			maxClusterSize = 3;

		createClusterTables(maxClusterSize);

		for (int n = 1; n <= maxClusterSize; n++) {
			pValue = addClusters(n, pValue, maxClusters, facetRestriction);
		}

		extractClustersFromTables(maxClusters, out);
	}

	void createClusterTables(int maxClusterSize) throws SQLException {
		jdbc.SQLupdate("CREATE TEMPORARY TABLE IF NOT EXISTS clusterInfo "
				+ "(cluster_id MEDIUMINT UNSIGNED NOT NULL PRIMARY KEY, "
				+ "nFacets TINYINT UNSIGNED NOT NULL, "
				+ "nOn MEDIUMINT UNSIGNED NOT NULL, "
				+ "nTotal MEDIUMINT UNSIGNED NOT NULL, "
				+ "pValue FLOAT NOT NULL) "
				// + "ENGINE=HEAP "
				+ "PACK_KEYS=1 ROW_FORMAT=FIXED;");

		// jdbc.SQLupdate("DROP TABLE IF EXISTS clusterFacets21");
		// facet_index == 0 means ancestor
		String clusterFacetsDef = ""
				+ "(cluster_id MEDIUMINT UNSIGNED NOT NULL, "
				+ "facet_id MEDIUMINT UNSIGNED NOT NULL, "
				+ "facet_index TINYINT NOT NULL, "
				+ "INDEX facet_index (facet_index), "
				+ "INDEX cluster (cluster_id), "
				+ "INDEX facet (facet_id, facet_index), "
				+ "PRIMARY KEY (cluster_id, facet_index)) ";
		jdbc.SQLupdate("CREATE TEMPORARY TABLE IF NOT EXISTS clusterFacets21 "
				+ clusterFacetsDef);

		// jdbc.SQLupdate("TRUNCATE TABLE clusterFacets21");
		// Truncate doesn't work with these MERGE tables. Need DELETE with a
		// WHERE clause
		jdbc.SQLupdate("DELETE FROM clusterFacets21 WHERE cluster_id > 0");
		jdbc.SQLupdate("TRUNCATE TABLE clusterInfo");
		try {
			myAssert(
					jdbc.SQLqueryInt("SELECT COUNT(*) FROM clusterFacets21") == 0,
					"Truncate didnt work");
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// work around MySQL limitation that you can't use a temporary table
		// more than once in a query
		int nCFtables = Math.max(3, maxClusterSize);
		for (int i = 1; i <= nCFtables; i++) {
			for (int j = 1; j <= nCFtables; j++) {
				if (j != i && !(i == 2 && j == 1)) {
					jdbc
							.SQLupdate("CREATE TEMPORARY TABLE IF NOT EXISTS clusterFacets"
									+ i
									+ j
									+ clusterFacetsDef
									+ "ENGINE=MERGE UNION(clusterFacets21)");
				}
			}
		}
	}

	private synchronized double addClusters(int nFacets, double pValue,
			int maxClusters, String facetRestriction) throws SQLException,
			ServletException {
		int neededClusters = maxClusters
				- jdbc.SQLqueryInt("SELECT COUNT(*) FROM clusterInfo");
		if (pValue > 0 || neededClusters > 0) {
			ResultSet rs = null;
			PreparedStatement addCluster = null, addClusterFacets = null;
			try {
				addCluster = jdbc
						.prepareStatement("INSERT INTO clusterInfo VALUES(?, ?, ?, ?, ?)");
				addClusterFacets = jdbc
						.prepareStatement("INSERT INTO clusterFacets21 VALUES(?, ?, ?)");
				int q = jdbc.SQLqueryInt("SELECT COUNT(*) FROM onItems");
				int db = jdbc.SQLqueryInt("SELECT COUNT(*) FROM item");
				int c = jdbc
						.SQLqueryInt("SELECT MAX(cluster_id) FROM clusterInfo");
				rs = jdbc.SQLquery(clusterQuery(nFacets, facetRestriction));
				while (rs.next() && (pValue > 0 || neededClusters > 0)) {
					int con = rs.getInt(1);
					int ctot = rs.getInt(2);
					if (con * db > q * ctot) {
						double p = ChiSq2x2.pValue(db, q, ctot, con);
						myAssert(p >= 0, "p = " + p);
						if (p < pValue || (p == pValue && neededClusters > 0)) {
							addCluster.setInt(1, ++c);
							addCluster.setInt(2, nFacets);
							addCluster.setInt(3, con);
							addCluster.setInt(4, ctot);
							addCluster.setDouble(5, p);
							jdbc.SQLupdate(addCluster, "Set pValue");
							addClusterFacets.setInt(1, c);
							for (int i = 0; i < nFacets; i++) {
								addClusterFacets.setInt(2, rs.getInt(i + 3));
								addClusterFacets.setInt(3, i + 1);
								jdbc.SQLupdate(addClusterFacets,
										"Insert cluster");
							}
							if (--neededClusters < 0) {
								double newPvalue = jdbc
										.SQLqueryDouble("SELECT pValue FROM clusterInfo ORDER BY pValue LIMIT "
												+ maxClusters + ", 1");
								myAssert(newPvalue <= pValue, newPvalue
										+ " should be less than " + pValue);
								pValue = newPvalue;
							}
						}
					}
				}
				// rs.last();
				// log("addClusters " + nFacets + " " + neededClusters + " " +
				// rs.getRow() + " " + pValue);
			} finally {
				if (rs != null)
					rs.close();
				if (addCluster != null)
					addCluster.close();
				if (addClusterFacets != null)
					addClusterFacets.close();
			}
		}
		return pValue;
	}

	@SuppressWarnings("unchecked")
	void extractClustersFromTables(int maxClusters, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		ResultSet rs = null;
		ResultSet rs1 = null;
		try {
			int nRows = 0;
			int prevRows = 0;
			while ((nRows = jdbc
					.SQLqueryInt("SELECT COUNT(*) FROM clusterFacets21")) > prevRows) {
				// Add ancestors of all the facets, with facet_index < 0
				prevRows = nRows;
				jdbc
						.SQLupdate("INSERT INTO clusterFacets21 "
								+ "SELECT cf.cluster_id, parent_facet_id, "
								+ "MIN(previ.facet_index) - ABS(MIN(cf.facet_index)) - 1 "
								+ "FROM clusterFacets12 cf "
								+ "INNER JOIN facet f ON cf.facet_id = f.facet_id "
								+ "LEFT JOIN clusterFacets31 previ ON cf.cluster_id = previ.cluster_id "
								+ "LEFT JOIN clusterFacets23 dup ON cf.cluster_id = dup.cluster_id AND dup.facet_id = parent_facet_id "
								+ "WHERE parent_facet_id > 0 "
								+ "AND dup.facet_id IS NULL "
								+ "GROUP BY cf.cluster_id, parent_facet_id ORDER BY NULL");
			}
			rs = jdbc
					.SQLquery("SELECT cluster_id, pValue, nOn, nTotal FROM clusterInfo "
							+ "ORDER BY pValue, nOn/nTotal DESC, nFacets DESC LIMIT "
							+ maxClusters);
			rs.last();
			nRows = rs.getRow();
			writeInt(nRows, out);
			rs.beforeFirst();
			while (rs.next()) {
				rs1 = jdbc
						.SQLquery("SELECT f.parent_facet_id, f.facet_id, f.name, f.n_child_facets, f.first_child_offset, "
								+ "f.n_items, cf.facet_index <= 0 isAncestor, "
								+ rs.getDouble(2)
								+ ", "
								+ rs.getInt(3)
								+ ", "
								+ rs.getInt(4)
								+ " FROM clusterfacets21 cf "
								+ "INNER JOIN facet f USING (facet_id) "
								+ "WHERE cf.cluster_id = "
								+ rs.getInt(1)
								+ " AND f.parent_facet_id > 0"
								+ " ORDER BY f.facet_id");
				sendResultSet(
						rs1,
						MyResultSet.INT_PINT_STRING_INT_INT_INT_INT_DOUBLE_PINT_PINT,
						out);
				rs1 = null;
			}
		} finally {
			if (rs != null)
				rs.close();
			if (rs1 != null)
				rs1.close();
		}
	}

	// This isn't right for restrictedData; should compute ctot using table
	// 'restricted'
	private static String clusterQuery(int nFacets, String facetRestriction)
			throws ServletException {
		myAssert(nFacets > 0, "clusterQuery facets=" + nFacets);
		if (nFacets == 1)
			return "SELECT COUNT(DISTINCT i.record_num) con, f.n_items ctot, i.facet_id "
					+ "FROM item_facet i "
					+ "INNER JOIN onItems USING (record_num) "
					+ "INNER JOIN facet f USING (facet_id) "
					+ (facetRestriction == null ? "" : facetRestriction)
					+ " GROUP BY i.facet_id " + "HAVING con > 1 ORDER BY null";
		else if (nFacets == 2)
			// nFacets == 2 is a different pattern because it uses the table
			// pairs
			return "SELECT STRAIGHT_JOIN COUNT(*) con, cnt ctot, pairs.facet1, pairs.facet2 "
					+ "FROM pairs, clusterFacets12, clusterFacets21, onItems, "
					+ "item_facet_heap i1, "
					+ "item_facet_heap i2 "
					+ "WHERE clusterFacets12.facet_id = pairs.facet1 AND clusterFacets21.facet_id = pairs.facet2 "
					+ "AND pairs.facet1 = i1.facet_id AND pairs.facet2 = i2.facet_id "
					+ "AND i1.record_num = i2.record_num AND i1.record_num = onItems.record_num "
					+ "AND clusterFacets12.facet_index = 1 AND clusterFacets21.facet_index = 1 "
					+ "GROUP BY pairs.facet1, pairs.facet2 "
					+ "HAVING con > 1 ORDER BY null";
		else
			return clusterQueryInternal(nFacets);
	}

	private static String clusterQueryInternal(int nFacets)
			throws ServletException {
		// This only makes sense for nFacets > 2, and the nFacets == 4 version
		// crashes MySQL
		myAssert(nFacets == 3, "clusterQuery facets=" + nFacets);
		String cfExpr = "SELECT STRAIGHT_JOIN "
				+ "COUNT(DISTINCT o.record_num) con, COUNT(DISTINCT i1.record_num) ctot, ";
		int nCfTables = nFacets * (nFacets - 1);
		String[] cfTables = new String[nCfTables];
		String[] ifTables = new String[nFacets];
		String[] ifJoinTables = new String[nFacets + 1];
		String[] constraints = new String[3 * nFacets * (nFacets - 1)];
		int cfTableIndex = 0;
		int constraintIndex = 0;
		for (int cluster = 1; cluster <= nFacets; cluster++) {
			int facetIndex = nFacets - 1;
			String prevTable = null;
			for (int facet = nFacets; facet > 0; facet--) {
				if (facet != cluster) {
					String table = "clusterFacets" + cluster + facet;
					cfTables[cfTableIndex++] = table;
					constraints[constraintIndex++] = table + ".facet_index = "
							+ facetIndex--;
					if (prevTable != null)
						constraints[constraintIndex++] = table
								+ ".cluster_id = " + prevTable + ".cluster_id";
					if (cluster > 1 && (cluster > 2 || facet > 1))
						constraints[constraintIndex++] = table
								+ ".facet_id = clusterFacets"
								+ (facet == 1 ? 2 : 1) + facet + ".facet_id";
					prevTable = table;
				}
			}
			ifJoinTables[cluster] = "i" + cluster;
			ifTables[cluster - 1] = ifJoinTables[cluster] + ".facet_id";
			if (cluster > 1) {
				constraints[constraintIndex++] = "i1.record_num = i" + cluster
						+ ".record_num";
				constraints[constraintIndex++] = ifJoinTables[cluster]
						+ ".facet_id = clusterFacets1" + cluster + ".facet_id";
			} else {
				constraints[constraintIndex++] = ifJoinTables[cluster]
						+ ".facet_id = clusterFacets2" + cluster + ".facet_id";
			}
		}
		constraints[constraintIndex++] = "clusterFacets21.facet_id < clusterFacets12.facet_id";
		cfExpr += Util.join(ifTables, ", ") + " FROM ";
		cfExpr += Util.join(cfTables, " INNER JOIN ");
		ifJoinTables[0] = cfExpr;
		cfExpr = Util.join(ifJoinTables, " INNER JOIN item_facet_heap ");
		cfExpr += " LEFT JOIN onItems o ON i1.record_num = o.record_num WHERE ";
		cfExpr += Util.join(constraints, " AND ");
		cfExpr += " GROUP BY " + Util.join(ifTables, ", ")
				+ " HAVING con > 1 ORDER BY null";
		return cfExpr;
	}

	void printRecords(ResultSet result, List<Object> types)
			throws SQLException, ServletException {
		StringBuffer buf = new StringBuffer();
		result.last();
		int nRows = result.getRow();
		int nCols = types.size();
		buf.append(nRows).append(" rows, ").append(nCols).append(
				" cols in result set");
		result.beforeFirst();
		for (int i = 0; i < nRows && i < 5; i++) {
			result.next();
			buf.append("\n");
			for (int j = 0; j < nCols; j++) {
				Object type = types.get(j);
				if (type == MyResultSet.Column.IntegerType
						|| type == MyResultSet.Column.SortedIntegerType
						|| type == MyResultSet.Column.SortedNMIntegerType
						|| type == MyResultSet.Column.PositiveIntegerType)
					buf.append(result.getInt(j + 1)).append("\t");
				else if (type == MyResultSet.Column.StringType)
					buf.append(result.getString(j + 1)).append("\t");
				else if (type == MyResultSet.Column.DoubleType)
					buf.append(result.getDouble(j + 1)).append("\t");
				else if (type == MyResultSet.Column.ImageType)
					buf.append("<image>\t");
				else
					error("Unknown ColumnType: " + type);
			}
		}
		result.beforeFirst();
		log(buf.toString());
	}

	private void log(String message) {
		jdbc.print(message);
	}

	static void myAssert(boolean condition, String msg) throws ServletException {
		if (!condition)
			error(msg);
	}

	private static void error(String message) throws ServletException {
		throw (new ServletException(message));
	}

	private static void checkNonNegative(int id) throws ServletException {
		if (id < 0) {
			error("Bad ID: " + id);
		}
	}

	private static void checkPositive(int id) throws ServletException {
		if (id <= 0) {
			error("Bad ID: " + id);
		}
	}

	private PreparedStatement setItemDescriptionQuery;

	void setItemDescription(int item, String description) throws SQLException {
		if (setItemDescriptionQuery == null) {
			setItemDescriptionQuery = jdbc
					.prepareStatement("UPDATE item SET description = ? WHERE record_num = ? ");
		}
		synchronized (setItemDescriptionQuery) {
			setItemDescriptionQuery.setInt(2, item);
			setItemDescriptionQuery.setString(1, description);
			jdbc.SQLupdate(setItemDescriptionQuery, "Update item description.");
		}
	}

	@SuppressWarnings("unchecked")
	void opsSpec(int session, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT CONCAT_WS(',',"
						+ " TIMESTAMPDIFF(SECOND, (SELECT MIN(timestamp) FROM user_actions WHERE session = "
						+ session
						+ "),"
						+ " timestamp),"
						+ " location, object, modifiers) FROM user_actions WHERE session = "
						+ session + " ORDER BY action_number");
		sendResultSet(rs, MyResultSet.STRING, out);
	}

	void sendResultSet(ResultSet result, List<Object> types,
			DataOutputStream out) throws ServletException, SQLException,
			IOException {
		sendResultSet(result, types, -1, -1, -1, out);
	}

	void sendResultSet(ResultSet result, List<Object> types, int imageW,
			int imageH, int quality, DataOutputStream out)
			throws ServletException, SQLException, IOException {
		if (result == null) {
			writeInt(0, out);
			jdbc.print("sendResultSet given null result set.");
		} else {
			try {
				result.last();
				int nRows = result.getRow();
				int nCols = types.size();
				writeInt(nRows + 1, out);
				writeInt(nCols, out);
				for (int i = 0; i < nCols; i++) {
					myAssert(!(types.get(i) == Column.ImageType && (types
							.get(i + 1) != Column.IntegerType || types
							.get(i + 2) != Column.IntegerType)),
							"Images must be followed by width and height");
					writeCol(result, i + 1, types.get(i), imageW, imageH,
							quality, out);
				}
			} finally {
				jdbc.close(result);
			}
		}
	}

	private static void writeCol(ResultSet result, int colIndex, Object type,
			int imageW, int imageH, int quality, DataOutputStream out)
			throws ServletException, SQLException, IOException {
		result.beforeFirst();
		boolean sorted = false;
		boolean positive = false;
		if (type == Column.SortedIntegerType) {
			sorted = true;
			positive = true;
			writeIntCol(result, colIndex, out, sorted, positive);
		} else if (type == Column.PositiveIntegerType) {
			positive = true;
			writeIntCol(result, colIndex, out, sorted, positive);
		} else if (type == MyResultSet.Column.IntegerType) {
			writeIntCol(result, colIndex, out, sorted, positive);
		} else if (type == MyResultSet.Column.SortedNMIntegerType) {
			writeIntCol(result, colIndex, out, true, false);
		} else if (type == MyResultSet.Column.StringType) {
			writeStringCol(result, colIndex, out);
		} else if (type == MyResultSet.Column.DoubleType) {
			writeDoubleCol(result, colIndex, out);
		} else if (type == MyResultSet.Column.ImageType) {
			writeBlobCol(result, colIndex, imageW, imageH, quality, out);
		} else {
			throw (new ServletException("Unknown ColumnType: " + type));
		}
	}

	private static void writeIntCol(ResultSet result, int colIndex,
			OutputStream out, boolean sorted, boolean positive)
			throws ServletException, SQLException, IOException {
		int prev = 0;
		int prevValue = -1;
		while (result.next()) {
			int value = result.getInt(colIndex);
			if (sorted) {
				int diff = value - prev;
				myAssert(diff >= 0, "Column " + colIndex + " is not sorted: "
						+ value + " < " + prev);
				myAssert(!positive || diff != 0, "Column " + colIndex
						+ " is not monotonically increasing: " + value + " < "
						+ prev);
				prev = value;
				value = diff;
			}
			if (prevValue < 0) {
				prevValue = value;
			} else {
				prevValue = writeIntOrTwo(prevValue, value, out, positive);
			}
		}
		if (prevValue >= 0)
			writeIntOrTwo(prevValue, -1, out, positive);
	}

	private static void writeStringCol(ResultSet result, int colIndex,
			DataOutputStream out) throws SQLException, IOException {
		while (result.next()) {
			writeString(result.getString(colIndex), out);
		}
	}

	private static void writeDoubleCol(ResultSet result, int colIndex,
			DataOutputStream out) throws SQLException, IOException {
		while (result.next()) {
			writeDouble(result.getDouble(colIndex), out);
		}
	}

	private static void writeBlobCol(ResultSet result, int colIndex,
			int imageW, int imageH, int quality, OutputStream out)
			throws ServletException, SQLException, IOException {
		while (result.next()) {
			// if (imageW > 0
			// && (result.getInt(colIndex + 1) > imageW || result
			// .getInt(colIndex + 2) > imageH))
			writeBlob(result.getBlob(colIndex), imageW, imageH, quality, result
					.getInt(colIndex + 1), result.getInt(colIndex + 2), out);
			// else
			// writeBlob(result.getBlob(colIndex), -1, -1, out);
		}
	}

	static void writeString(String s, DataOutputStream out) throws IOException {
		if (s == null)
			s = "";
		// Avoid an error, at least, and hope for the best.
		out.writeUTF(s);
	}

	private static void writeDouble(double n, DataOutputStream out)
			throws IOException {
		out.writeDouble(n);
	}

	static int writeInt(int n, OutputStream out) throws ServletException,
			IOException {
		myAssert(n >= 0, n + " Tried to write a negative int.");
		myAssert(n < 1073741824, n + " Tried to write a too-large int:");
		if (n < 128)
			out.write(n);
		else if (n < 16384) {
			out.write((n >> 8) | 128);
			out.write(n);
		} else if (n < 2097152) {
			out.write((n >> 16) | 192);
			out.write(n >> 8);
			out.write(n);
		} else {
			out.write((n >> 24) | 224);
			out.write(n >> 16);
			out.write(n >> 8);
			out.write(n);
		}
		return n;
	}

	private static int writeIntOrTwo(int n, int nextN, OutputStream out,
			boolean positive) throws ServletException, IOException {
		// Util.print("writeIntOrTwo " + n + " " + nextN + " " + positive);
		if (positive) {
			myAssert(nextN != 0 && n != 0, "Tried to write 0 to a PINT: " + n
					+ " " + nextN);
			n--;
			nextN--;
		}
		myAssert(n >= 0, n + " Tried to write a negative int.");
		myAssert(n < 1073741824 && nextN < 1073741824, n
				+ " Tried to write a too-large int:");
		if (n < 8 && nextN >= 0 && nextN < 8) {
			out.write(n << 3 | nextN | 64);
			nextN = -2;
		} else if (n < 64) {
			out.write(n);
		} else if (n < 16384) {
			out.write((n >> 8) | 128);
			out.write(n);
		} else if (n < 2097152) {
			out.write((n >> 16) | 192);
			out.write(n >> 8);
			out.write(n);
		} else {
			out.write((n >> 24) | 224);
			out.write(n >> 16);
			out.write(n >> 8);
			out.write(n);
		}
		if (positive)
			nextN++;
		return nextN;
	}

	private static void writeBlob(Blob blob, int desiredW, int desiredH,
			int quality, int actualW, int actualH, OutputStream out)
			throws ServletException, IOException, SQLException {
		if (blob == null || desiredW < 0)
			writeInt(0, out);
		else if (2 * Math.min(desiredW, actualW) * Math.min(desiredH, actualH) < actualW
				* actualH) {
			// will call this again with resized blob
			resize(blob, desiredW, desiredH, quality, actualW, actualH, out);
		} else {
			int n = (int) blob.length();
			writeInt(n + 1, out);
			InputStream s = null;
			try {
				s = blob.getBinaryStream();
				int x;
				// StringBuffer buf = new StringBuffer(6000);
				// int i = 0;
				while ((x = s.read()) >= 0) {
					out.write(x);
					// if (i < 100)
					// buf.append(i++).append(" ").append(x).append(";");
				}
			} finally {
				s.close();
			}
			// log(buf.toString());
		}
	}

	private static void resize(Blob blob, int desiredW, int desiredH,
			int quality, double actualW, double actualH, OutputStream out)
			throws SQLException, IOException, ServletException {
		InputStream blobStream = blob.getBinaryStream();
		double ratio = Math.min(desiredW / actualW, desiredH / actualH);
		desiredW = (int) (actualW * ratio);
		desiredH = (int) (actualH * ratio);

		BufferedImage resized = Util.resize(ImageIO.read(blobStream), desiredW,
				desiredH, false);
		ByteArrayOutputStream byteArrayStream = null;
		try {
			byteArrayStream = new ByteArrayOutputStream();

			// non-JAI - JPEG
			JPEGImageEncoder encoder = JPEGCodec
					.createJPEGEncoder(byteArrayStream);
			JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(resized);
			param.setQuality(quality / 100f, false);
			encoder.setJPEGEncodeParam(param);
			encoder.encode(resized);

			int len = byteArrayStream.size();
			if (len * 3 < blob.length() * 2) {
				writeInt(len + 1, out);
				byteArrayStream.writeTo(out);
			} else {
				writeBlob(blob, desiredW, desiredH, quality, desiredW,
						desiredH, out);
			}
		} finally {
			byteArrayStream.close();
		}
	}

	String dbDescs(String dbNameList) throws SQLException, ServletException {
		myAssert(dbNameList!=null&&dbNameList.length()>0, "Empty db name list");
		String[] dbNames = Util.splitComma(dbNameList);
		String[] dbDescs = new String[dbNames.length];
		for (int i = 0; i < dbDescs.length; i++) {
			String desc = jdbc.SQLqueryString("SELECT description FROM "
					+ dbNames[i] + ".globals");
			dbDescs[i] = dbNames[i] + "," + desc;
		}
		log(Util.join(dbDescs, ";"));
		return Util.join(dbDescs, ";");
	}
}
