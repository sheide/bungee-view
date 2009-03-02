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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

// Permissions to add (ALTER ROUTINE is for finding clusters):
// GRANT SELECT, INSERT, UPDATE, CREATE, DELETE, ALTER ROUTINE
// CREATE TEMPORARY TABLES ON
// chartresvezelay.* TO p5@localhost

// In order to edit, you also need these:
//
//GRANT DROP, ALTER, CREATE ROUTINE ON *.* TO BVeditor@localhost
//
// For revert, need SUPER, REPLICATION CLIENT 
//
// for now, we're just using root to revert
//
// Dump like this:
// mysqldump -u root -p --add-drop-database --databases wpa > wpa.sql

public class Database {

	private String dbName;

	private JDBCSample jdbc;

	public Database(String _server, String _db, String _user, String _pass,
			GenericServlet _servlet) throws SQLException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, ServletException {
		dbName = _db;
		// String connectString = _server + _db + "?user=" + _user;
		// if (_pass != null)
		// connectString += "&password=" + _pass;
		// servlet = _servlet;
		jdbc = new JDBCSample(_server, _db, _user, _pass, _servlet);
		ensureDBinitted();
		String item_id_column_type = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(record_num) FROM item"));
		String facet_id_column_type = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(facet_id) FROM facet"));

		reorderItems(-1); // order by random

		// String itemStatesDef = " (record_num " + item_id_column_type
		// + ", state smallint unsigned NOT NULL"
		// + ", PRIMARY KEY (record_num)) ENGINE=";

		String[] createTempTables = {

				"CREATE TEMPORARY TABLE if not exists onItems (record_num "
						+ item_id_column_type
						+ ", PRIMARY KEY (record_num)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				"CREATE TEMPORARY TABLE if not exists restricted (record_num "
						+ item_id_column_type
						+ ", PRIMARY KEY (record_num)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				"CREATE TEMPORARY TABLE if not exists relevantFacets ("
						+ "facet_id " + facet_id_column_type + ", "
						+ "PRIMARY KEY USING BTREE (facet_id)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				// "CREATE TEMPORARY TABLE IF NOT EXISTS itemStates1"
				// + itemStatesDef + "MYISAM",
				//
				// "CREATE TEMPORARY TABLE IF NOT EXISTS itemStates2"
				// + itemStatesDef + "MERGE UNION(itemStates1)",

				"CREATE TEMPORARY TABLE IF NOT EXISTS mutInf (facet_id "
						+ facet_id_column_type + ", mutInf FLOAT, "
						+ "PRIMARY KEY (facet_id)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

				"CREATE TEMPORARY TABLE IF NOT EXISTS state_items (record_num "
						+ item_id_column_type
						+ ", PRIMARY KEY (record_num)) ENGINE=HEAP "
						+ "PACK_KEYS=1 ROW_FORMAT=FIXED",

		// "CREATE TEMPORARY TABLE IF NOT EXISTS state_counts (facet_id "
		// + facet_id_column_type + ", cnt FLOAT, "
		// + "PRIMARY KEY (facet_id)) ENGINE=HEAP "
		// + "PACK_KEYS=1 ROW_FORMAT=FIXED",

		};
		jdbc.SQLupdate(createTempTables);

		// mutInfQuery1 = jdbc
		// .lookupPS(
		// "INSERT INTO mutInf (SELECT facet_id, -p*log(p)-(1-p)*log(1-p) entropy FROM"
		// +
		// " (SELECT facet_id, n_items/? p FROM facet WHERE parent_facet_id > 0) foo "
		// + "WHERE p > 0 and p < 1)");

		// mutInfQuery2 = jdbc
		// .lookupPS(
		// "UPDATE mutInf, (SELECT facet_id, IF(p>=1,0,-p*log(p)-(1-p)*log(1-p)) entropy FROM "
		// + "(SELECT facet_id, (COUNT(*)+0.000001)/? p "
		// + "FROM item_facet_heap i_f "
		// + "INNER JOIN itemStates USING (record_num) "
		// + "GROUP BY facet_id ORDER BY null) foo"
		// + ") counts SET mutInf=mutInf-entropy*?"
		// + " WHERE mutInf.facet_id=counts.facet_id");

		// mutInfQuery2 = jdbc
		// .lookupPS(
		// "UPDATE mutInf, (SELECT facet_id, -p*log(p)-(1-p)*log(1-p) entropy FROM "
		// + "(SELECT facet_id, COUNT(*) / ? p FROM state_items"
		// + " INNER JOIN item_facet_heap i_f USING (record_num)"
		// + " GROUP BY facet_id ORDER BY null) foo"
		// + " WHERE p > 0 and p < 1"
		// + ") counts SET mutInf = mutInf-entropy * ?"
		// + " WHERE mutInf.facet_id = counts.facet_id");

		// mutInfQuery2 = jdbc
		// .lookupPS(
		// "UPDATE mutInf, SELECT facet_id, sum(entropy(fCount, cnt)) entropy FROM ((SELECT straight_join facet_id, COUNT(*) fCount, state FROM "
		// + "itemStates1 FORCE INDEX (PRIMARY) INNER JOIN item_facet_heap i_f"
		// +
		// " FORCE INDEX (item) USING (record_num) GROUP BY facet_id, state ORDER BY null) foo, "
		// +"(SELECT state, COUNT(*) cnt from itemstates2 group by state) sc" +
		// " where sc.state=foo.state group by facet_id"
		// + ") counts SET mutInf = mutInf-entropy * ?"
		// + " WHERE mutInf.facet_id = counts.facet_id");

		filteredCountQuery = jdbc
				.lookupPS("SELECT f.facet_id, COUNT(*) AS cnt "
						+ "FROM relevantFacets f "
						+ "INNER JOIN item_facet_heap i_f USING (facet_id) "
						+ "INNER JOIN onItems USING (record_num) "
						+ "GROUP BY f.facet_id ORDER BY f.facet_id");

		filteredCountTypeQuery = jdbc
				.lookupPS("SELECT f.facet_id, COUNT(*) AS cnt "
						+ "FROM item_facet_type_heap f "
						+ "INNER JOIN onItems USING (record_num) "
						+ "GROUP BY f.facet_id ");

		String[] prefetchFROM = {
				" FROM facet WHERE parent_facet_id = ? ORDER BY facet_id",

				// This would be faster if we first calculate the facet_id range
				// from parent_facet_id and using that in the WHERE clause.
				// But then we'd need two ?s and so have to change the code that
				// uses these queries.
				" FROM (SELECT facet_id, count(restricted.record_num) AS n_items, n_child_facets, first_child_offset, name"
						+ " FROM facet INNER JOIN item_facet_heap USING (facet_id)"
						+ " LEFT JOIN restricted USING (record_num) WHERE parent_facet_id = ?"
						+ " GROUP BY facet_id) foo ORDER BY facet_id" };

		prefetchQuery = jdbc.lookupPS("SELECT n_items, n_child_facets, name "
				+ prefetchFROM[0]);

		prefetchNoCountQuery = jdbc.lookupPS("SELECT n_child_facets, name"
				+ prefetchFROM[0]);

		prefetchNoNameQuery = jdbc.lookupPS("SELECT n_items, n_child_facets"
				+ prefetchFROM[0]);

		prefetchNoCountNoNameQuery = jdbc.lookupPS("SELECT n_child_facets"
				+ prefetchFROM[0]);

		prefetchQueryRestricted = jdbc
				.lookupPS("SELECT n_items, n_child_facets, name"
						+ prefetchFROM[1]);

		prefetchNoNameQueryRestricted = jdbc
				.lookupPS("SELECT n_items, n_child_facets" + prefetchFROM[1]);

		getItemInfoQuery = jdbc
				.prepareStatement(
						"SELECT parent_facet_id, f.facet_id, name, "
								+ "n_child_facets, first_child_offset, n_items"
								+ " FROM item_facet_heap i INNER JOIN facet f USING (facet_id)"
								+ " WHERE record_num = ? ORDER BY f.facet_id",
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);

		getFacetInfoQuery = jdbc.prepareStatement(
				"SELECT parent_facet_id, facet_id, name, "
						+ "n_child_facets, first_child_offset, n_items"
						+ " FROM facet " + " WHERE facet_id = ?",
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		printUserActionStmt = jdbc
				.lookupPS("INSERT INTO user_actions VALUES(NOW(), ?, ?, ?, ?, ?, ?)");

		// ORDER BY clause doesn't make any difference if facets are ordered
		// correctly (by utf8_general_ci),
		// but will correct for bad alphabetization in old databases.
		//
		// Used to have "LOCATE(?, name) = 1" instead of LIKE, but LOCATE
		// doesn't respect the collation properly
		getLetterOffsetsQuery = jdbc
				.lookupPS("SELECT MIN(SUBSTRING(name, CHAR_LENGTH(?) + 1, 1)) letter, MAX(facet_id) max_facet "
						+ "FROM facet WHERE parent_facet_id = ? AND name LIKE CONCAT(?, '%') "
						+ "GROUP BY SUBSTRING(name, CHAR_LENGTH(?) + 1, 1) ORDER BY max_facet");
	}

	void close() throws SQLException {
		jdbc.close();
		jdbc = null;
	}

	String aboutCollection() throws SQLException {
		return jdbc.SQLqueryString("SELECT aboutURL FROM globals");
	}

	int facetCount() throws SQLException {
		// This is only used to initialize Query.allPersectives. If IDs are
		// screwed up, it's better to make it big enough to not get an array
		// index out of bounds error.
		return jdbc.SQLqueryInt("SELECT MAX(facet_id) FROM facet");
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

			String[] fields = itemDescriptionFields.split(",");
			String[] nonNullFields = new String[fields.length];
			for (int i = 0; i < fields.length; i++) {
				nonNullFields[i] = "IF(" + fields[i]
						+ " IS NULL,'', CONCAT('\n" + i + "\n', " + fields[i]
						+ "))";
				// nonNullFields[i] = "'\n" + i + "\n', IFNULL(" + fields[i]
				// + ", '')";
			}
			String desc = "CONCAT(" + Util.join(nonNullFields) + ")";
			// Work around MySQL Connector bug: once the PreparedStatement
			// encounters a null image, it returns null from then on.
			imageQuery = // jdbc.prepareStatement(
			"SELECT " + desc
					+ " descript, image, w, h FROM item LEFT JOIN images "
					+ "ON item.record_num = images.record_num "
					+ "WHERE item.record_num = ";
			// log(imageQuery);

			/**
			 * itemURLgetter should stick to atomic table references in any FROM
			 * clause, so that copyImagesNoURI can parse it and prepend explicit
			 * schema names. Any FROM clause should be followed immediately by
			 * WHERE. I.e. don't use JOIN syntax in expressions like this:
			 * 
			 * (SELECT xref FROM movie, shotbreak WHERE movie.movie_id =
			 * shotbreak.movie_id AND shotbreak.shotbreak_id = item.shotbreak)
			 * 
			 */
			String itemURLgetter = rs.getString(3);
			if (itemURLgetter != null && itemURLgetter.length() > 0) {
				itemIdPS = jdbc.lookupPS("SELECT " + itemURLgetter
						+ " FROM item WHERE record_num = ?");
				itemURLPS = jdbc.lookupPS("SELECT record_num FROM item WHERE "
						+ itemURLgetter + " = ?");
			}

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

	private String xxx;

	String getItemURL(int item) throws SQLException, ServletException {
		// Util.print("itemDesc " + item);
		// try {
		String result = null;
		if (itemIdPS != null) {
			synchronized (itemIdPS) {
				itemIdPS.setInt(1, item);
				result = jdbc.SQLqueryString(itemIdPS);
				if (result == null) {
					myAssert(
							false,
							"Can't find "
									+ jdbc
											.SQLqueryString("SELECT itemURL FROM globals")
									+ " for record_num " + item);
				}
			}
		}
		// } catch (SQLException se) {
		// System.err
		// .println("SQL Exception in getItemID: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	private void ensureDBinitted() throws SQLException {
		if (jdbc.SQLqueryInt("SELECT COUNT(*) FROM item_facet_heap") == 0) {
			jdbc
					.SQLupdate("INSERT INTO item_facet_heap SELECT * FROM item_facet;");
			jdbc.SQLupdate("TRUNCATE TABLE item_facet_type_heap;");
			jdbc.SQLupdate("INSERT INTO item_facet_type_heap "
					+ "SELECT distinct i.record_num, f.parent_facet_id "
					+ "FROM facet f "
					+ "INNER JOIN item_facet i ON i.facet_id = f.facet_id "
					+ "WHERE f.parent_facet_id <= " + maxFacetTypeID());
			jdbc.SQLupdate("TRUNCATE TABLE item_order_heap;");
			jdbc.SQLupdate("INSERT INTO item_order_heap "
					+ "SELECT * FROM item_order");
			// updateUsageCounts();
		}
	}

	int maxFacetTypeID() throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT MAX(facet_id) FROM facet WHERE parent_facet_id = 0");
	}

	// private void updateUsageCounts() throws SQLException {
	// jdbc
	// .SQLupdate("UPDATE facet, (SELECT COUNT(*) cnt, object FROM user_actions
	// "
	// + "WHERE location IN (1,2,6) GROUP BY object) temp "
	// + "SET usage_count = cnt WHERE object = facet_id");
	// }

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
		if (offsetItemsQuery == null) {
			offsetItemsQuery = new PreparedStatement[3];
			itemOffsetQuery1 = new PreparedStatement[offsetItemsQuery.length];
			itemOffsetQuery2 = new PreparedStatement[offsetItemsQuery.length];
		}
		synchronized (offsetItemsQuery) {
			for (int i = 1; i < offsetItemsQuery.length; i++) {
				offsetItemsQuery[i] = jdbc.lookupPS("SELECT o.record_num FROM "
						+ sortedResultTable(i)
						+ " o INNER JOIN item_order_heap r USING (record_num)"
						+ " ORDER BY r." + columnToSortBy + " LIMIT ?, ?");

				xxx = "SELECT o.record_num FROM " + sortedResultTable(i)
						+ " o INNER JOIN item_order_heap r USING (record_num)"
						+ " ORDER BY r." + columnToSortBy;

				// Have to use 2 queries to work around the "can't reopen
				// temporary
				// table" problem in MySQL
				// Only call query 2 if query 1 result > 0 (should never be
				// exactly
				// 0)
				// Argument to query 2 is the result of query 1
				itemOffsetQuery1[i] = jdbc.lookupPS("SELECT s."
						+ columnToSortBy
						+ " FROM item_order_heap s INNER JOIN "
						+ sortedResultTable(i)
						+ " USING (record_num) WHERE s.record_num = ?");
				itemOffsetQuery2[i] = jdbc
						.lookupPS("SELECT COUNT(*) FROM item_order_heap r "
								+ "INNER JOIN " + sortedResultTable(i)
								+ " USING (record_num) WHERE r."
								+ columnToSortBy + " < ?");
			}
			offsetItemsQuery[0] = jdbc.lookupPS("SELECT record_num FROM "
					+ "item_order_heap ORDER BY " + columnToSortBy
					+ " LIMIT ?, ?");

			itemOffsetQuery1[0] = jdbc.lookupPS("SELECT s." + columnToSortBy
					+ " FROM item_order_heap s WHERE s.record_num = ?");
			itemOffsetQuery2[0] = jdbc
					.lookupPS("SELECT COUNT(*)-1 FROM item_order_heap "
							+ " WHERE " + columnToSortBy + " <= ?");
		}
	}

	private PreparedStatement itemURLPS;

	int getItemFromURL(String URL) throws SQLException {
		int result = 0;
		if (itemURLPS != null) {
			synchronized (itemURLPS) {
				itemURLPS.setString(1, URL);
				result = jdbc.SQLqueryInt(itemURLPS);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	void getCountsIgnoringFacet(String subQuery, String facet_id,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet range = jdbc
				.SQLquery("SELECT min(facet_id), max(facet_id) from facet where parent_facet_id = "
						+ facet_id);
		range.next();
		int minChild = range.getInt(1);
		int maxChild = range.getInt(2);
		range.close();
		// Imitating relevantFacets is way faster than using parent_facet_id
		// with another join
		ResultSet rs = jdbc
				.SQLquery("SELECT i_f.facet_id, COUNT(onItemsFake.record_num) AS cnt "
						+ "FROM item_facet_heap i_f "
						+ "INNER JOIN ("
						+ subQuery
						+ ") onItemsFake USING (record_num) "
						+ "WHERE i_f.facet_id >= "
						+ minChild
						+ " AND i_f.facet_id <= "
						+ maxChild
						+ " GROUP BY i_f.facet_id ORDER BY i_f.facet_id");
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
			ResultSet rs = jdbc.SQLquery(filteredCountQuery);
			sendResultSet(rs, MyResultSet.SINT_PINT, out);
		}
	}

	@SuppressWarnings("unchecked")
	void getFilteredCountTypes(DataOutputStream out) throws SQLException,
			ServletException, IOException {
		synchronized (filteredCountTypeQuery) {
			ResultSet rs = jdbc.SQLquery(filteredCountTypeQuery);
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
		// Combine flags isOrdered, etc, so we don't get version skew errors
		// from adding new flags
		sendResultSet(rs, MyResultSet.STRING_STRING_STRING_INT_INT_INT_INT, out);
	}

	@SuppressWarnings("unchecked")
	void init(DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT f.n_items as cnt "
						+ "FROM facet f WHERE f.parent_facet_id > 0 AND f.parent_facet_id <= "
						+ maxFacetTypeID() + " ORDER BY f.facet_ID");
		sendResultSet(rs, MyResultSet.INT, out);
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
		PreparedStatement ps1 = jdbc
				.lookupPS("SELECT first_child_offset, is_alphabetic FROM facet WHERE facet_id = ?");
		ps1.setInt(1, facet_id);
		ResultSet rs1 = jdbc.SQLquery(ps1);
		myAssert(MyResultSet.nRows(rs1) == 1, "Bad nRows: " + facet_id + " "
				+ MyResultSet.nRows(rs1));
		rs1.next();
		int children_offset = rs1.getInt(1);
		writeInt(children_offset, out);
		int isAlphabetic = rs1.getInt(2);
		// log(facet_id+ " is_alphabetic: "+isAlphabetic);

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
			ResultSet rs = jdbc.SQLquery(ps);
			sendResultSet(rs, types, out);
		}

		writeInt(isAlphabetic, out);

		// Assuming beginner mode is much more common, don't be so eager to
		// getLetterOffsets
		// if (isAlphabetic > 0)
		// getLetterOffsets(facet_id, "", out);
	}

	private PreparedStatement getLetterOffsetsQuery;

	@SuppressWarnings("unchecked")
	void getLetterOffsets(int parentFacetID, String prefix, DataOutputStream out)
			throws SQLException, IOException {
		synchronized (getLetterOffsetsQuery) {
			getLetterOffsetsQuery.setString(1, prefix);
			getLetterOffsetsQuery.setInt(2, parentFacetID);
			getLetterOffsetsQuery.setString(3, prefix);
			getLetterOffsetsQuery.setString(4, prefix);
			ResultSet rs = jdbc.SQLquery(getLetterOffsetsQuery);
			if (MyResultSet.nRows(rs) == 0) {
				// client should never ask unless there are some such facets
				log("Found no offsets for prefix '" + prefix
						+ "' among children of " + parentFacetID);
			}
			try {
				// If an old DB has facets alphabetized wrong, just ignore it.
				sendResultSet(rs, MyResultSet.STRING_SINT, out);
			} catch (ServletException e) {
				log("Exception in getLetterOffsets - probably mis-alphabetized facet names: '"
						+ prefix + "' " + e);
			}
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
			ResultSet rs = jdbc.SQLquery(s);
			if (false && MyResultSet.nRows(rs) != nRows) {
				int onCount = jdbc.SQLqueryInt("SELECT COUNT(*) FROM "
						+ sortedResultTable(table));

				if (onCount >= maxOffset) {
					log(xxx);
					ResultSet zz = jdbc.SQLquery(xxx);
					log(sortedResultTable(table) + " nRows="
							+ MyResultSet.nRows(zz));
				}

				myAssert(onCount < maxOffset, minOffset + "-" + nRows + " "
						+ MyResultSet.nRows(rs) + " " + onCount);
			}
			sendResultSet(rs, MyResultSet.INT, out);
		}
	}

	@SuppressWarnings("unchecked")
	void getThumbs(String items, int imageW, int imageH, int quality,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		ResultSet rs = jdbc
				.SQLquery("SELECT record_num, image, w, h FROM images WHERE record_num IN("
						+ items + ") ORDER BY record_num");
		sendResultSet(rs, MyResultSet.SINT_IMAGE_INT_INT, imageW, imageH,
				quality, out);
		rs = jdbc
				.SQLquery("SELECT * FROM item_facet_heap WHERE record_num IN("
						+ items
						+ ") UNION SELECT * FROM item_facet_type_heap WHERE record_num IN("
						+ items + ") ORDER BY record_num");
		sendResultSet(rs, MyResultSet.SNMINT_PINT, out);
	}

	private String imageQuery;

	private PreparedStatement getItemInfoQuery;

	/**
	 * @param item
	 * @param desiredImageW
	 *            -1 means don't retrieve an image
	 * @param desiredImageH
	 * @param quality
	 * @param out
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void getDescAndImage(int item, int desiredImageW, int desiredImageH,
			int quality, DataOutputStream out) throws SQLException,
			ServletException, IOException {

		synchronized (getItemInfoQuery) {
			getItemInfoQuery.setInt(1, item);
			ResultSet rs = jdbc.SQLquery(getItemInfoQuery);
			sendResultSet(rs, MyResultSet.PINT_SINT_STRING_INT_INT_INT, out);
		}

		ResultSet rs = jdbc.SQLquery(imageQuery + item);
		myAssert(MyResultSet.nRows(rs) == 1, "Non-unique result for "
				+ imageQuery + item);
		sendResultSet(rs, MyResultSet.STRING_IMAGE_INT_INT, desiredImageW,
				desiredImageH, quality, out);
	}

	private PreparedStatement getFacetInfoQuery;

	@SuppressWarnings("unchecked")
	void getFacetInfo(int facet, DataOutputStream out) throws SQLException,
			ServletException, IOException {

		synchronized (getFacetInfoQuery) {
			getFacetInfoQuery.setInt(1, facet);
			ResultSet rs = jdbc.SQLquery(getFacetInfoQuery);
			sendResultSet(rs, MyResultSet.PINT_SINT_STRING_INT_INT_INT, out);
		}
	}

	/**
	 * Return the ordinal for a single Item.
	 */
	private PreparedStatement[] itemOffsetQuery1;
	/**
	 * Return the offset for that ordinal.
	 */
	private PreparedStatement[] itemOffsetQuery2;

	// private PreparedStatement mutInfQuery1;
	//
	// private PreparedStatement mutInfQuery2;

	// private String lastQuery;

	/**
	 * @param item
	 * @param table
	 *            see reorderItems
	 * @return the offset into the on items table of this item. -1 means not
	 *         found.
	 * @throws SQLException
	 */
	int itemOffset(int item, int table) throws SQLException {
		int offset = -1;
		PreparedStatement s1 = itemOffsetQuery1[table];
		synchronized (s1) {
			s1.setInt(1, item);

			int ordinal = jdbc.SQLqueryInt(s1);
			if (ordinal > 0) {
				PreparedStatement s2 = itemOffsetQuery2[table];
				s2.setInt(1, ordinal);
				offset = jdbc.SQLqueryInt(s2);
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

	// private static Object itemStatesLock = "itemStatesLock";

	/**
	 * Given facets="1,2,3" return the counts in the 2^3 combinations
	 * 
	 * @param facetNames
	 *            list of facets to find co-occurence counts among
	 * @param table
	 *            either "restricted" or "item_order_heap"
	 * @param out
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void getPairCounts(String facetNames, String candidates, int table,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {

		// We're not dealing with facet types, for speed
		// List[] candidateTypes = new LinkedList[2];
		// candidateTypes[0] = new LinkedList<String>();
		// candidateTypes[1] = new LinkedList<String>();
		// if (candidates.length() > 0) {
		// String[] IDs = candidates.split(",");
		// for (int i = 0; i < IDs.length; i++) {
		// int ID = Integer.parseInt(IDs[i]);
		// int type = ID <= maxFacetTypeID() ? 0 : 1;
		// candidateTypes[type].add(IDs[i]);
		// }
		// }

		// synchronized (itemStatesLock) {
		// updateItemStates(facetNames, table);

		// List<String> selects = new LinkedList<String>();
		// selects.add(getPairCountSQL(facetNames, null, null));

		// if (candidateTypes[0].size() > 0)
		// selects.add(getPairCountSQL(Util.join(candidateTypes[0], ","),
		// "item_facet_type_heap"));
		// if (candidateTypes[1].size() > 0)
		// selects.add(getPairCountSQL(Util.join(candidateTypes[1], ","),
		// "item_facet_heap"));

		String stateCountsQuery;
		if (candidates.length() > 0) {
			stateCountsQuery = getPairCountSQL(facetNames, candidates,
					"item_facet_heap")
					+ " ORDER BY perspective";
		} else {
			stateCountsQuery = getPairCountSQL(facetNames, null, null)
					+ " ORDER BY null";
		}

		// String stateCountsQuery = Util.join(selects, " UNION ");
		// if (selects.size() > 1)
		// log(stateCountsQuery);
		ResultSet rs = jdbc.SQLquery(stateCountsQuery);
		sendResultSet(rs, MyResultSet.SNMINT_INT_INT, out);
		// }
	}

	// private void updateItemStates(String facetNames, int table)
	// throws SQLException {
	// String itemTable = table == 1 ? "restricted" : "item_order_heap";
	// String[] result = updateItemStatesInternal(facetNames);
	// String x = result[0] + " FROM " + itemTable + " items " + result[1];
	// String updateItemStatesQuery =
	// "INSERT INTO itemStates1 SELECT items.record_num, "
	// + x;
	// // log(updateItemStatesQuery);
	// jdbc.SQLupdate(jdbc.lookupPS("DELETE FROM itemStates1"));
	// jdbc.SQLupdate(updateItemStatesQuery);
	// }

	private String[] updateItemStatesInternal(String facetNames)
			throws SQLException {
		String[] facets = Util.splitComma(facetNames);
		int nFacets = facets.length;
		String[] states = new String[nFacets];
		String[] joins = new String[nFacets];
		for (int i = 0; i < nFacets; i++) {
			int facet = Integer.parseInt(facets[i]);
			String ifTable = facet <= maxFacetTypeID() ? "item_facet_type_heap"
					: "item_facet_heap";
			joins[i] = "LEFT JOIN " + ifTable + " i" + i + " ON i" + i
					+ ".record_num = items.record_num AND i" + i
					+ ".facet_id = " + facet;

			// First facet reprsented by low order bit
			int iComplement = nFacets - i - 1;
			states[i] = "(i" + iComplement + ".facet_id IS NOT NULL)*"
					+ (1 << iComplement);
		}
		String[] result = { Util.join(states, " + ") + " state",
				Util.join(joins, " ") };
		return result;
	}

	String getPairCountSQL(String facetIDs, String candidates, String ifhTable)
			throws SQLException {
		// boolean isCandidates = candidates.length() > 0;
		String[] result = updateItemStatesInternal(facetIDs);
		// String suffix = ifhTable == null ? "1" : ifhTable
		// .equals("item_facet_heap") ? "2" : "3";
		String sql = "SELECT " + (ifhTable == null ? "0" : "items.facet_id")
				+ " perspective, " + result[0] + ", COUNT(*) FROM "
				+ (ifhTable == null ? "item_order_heap" : ifhTable) + " items "
				+ result[1];
		if (ifhTable != null)
			sql += " WHERE items.facet_id IN (" + candidates + ")";
		sql += " GROUP BY perspective, state";
		// if (ifhTable != null)
		// sql += ", " + ifhTable + " items WHERE items.facet_id IN ("
		// + candidates + ") AND itemStates" + suffix
		// + ".record_num=items.record_num";
		// sql += " GROUP BY state";
		// if (ifhTable != null)
		// sql += ", items.facet_id";
		return sql;
	}

	// @SuppressWarnings("unchecked")
	// void topCandidatesNEW(String facetNames, int n, int table,
	// DataOutputStream out) throws SQLException, ServletException,
	// IOException {
	// synchronized (itemStatesLock) {
	// updateItemStates(facetNames, table);
	// // PreparedStatement clear = jdbc
	// // .lookupPS("TRUNCATE TABLE state_counts");
	// // jdbc.SQLupdate(clear);
	// // PreparedStatement stateCounts = jdbc
	// // .lookupPS(
	// //
	// "INSERT INTO state_counts SELECT state, COUNT(*) FROM itemstates1 GROUP BY state"
	// // );
	// // jdbc.SQLupdate(stateCounts);
	// PreparedStatement mutInf = jdbc
	// .lookupPS("SELECT entropy.facet_id "
	// + "FROM (SELECT i_f.facet_id, COUNT(*) fCount, its.state"
	// // too slow to check item_facet_type_heap
	// +
	// " FROM item_facet_heap i_f, itemStates1 its WHERE i_f.record_num = its.record_num"
	// + " GROUP BY facet_id, its.state ORDER BY NULL) foo, "
	// +
	// "(SELECT state, COUNT(*) cnt FROM itemstates2 GROUP BY state ORDER BY NULL) sc, entropy"
	// +
	// " WHERE sc.state = foo.state AND fCount < cnt AND entropy.facet_id = foo.facet_id"
	// + " GROUP BY entropy.facet_id"
	// +
	// " ORDER BY entropy - SUM(-fCount *log(fCount / cnt)-(cnt-fCount)*log(1-fCount / cnt))"
	// + " / (SELECT COUNT(*) FROM item) DESC" + " LIMIT "
	// + n);
	// ResultSet rs = jdbc.SQLquery(mutInf);
	// sendResultSet(rs, MyResultSet.INT, out);
	// }
	// }

	// @SuppressWarnings("unchecked")
	// void topCandidatesInteraction(String perspectiveIDs, int n, int table,
	// DataOutputStream out) throws SQLException, ServletException,
	// IOException {
	// String[] perspectiveNames = perspectiveIDs.split(",");
	// int nPerspectives = perspectiveNames.length;
	// if (nPerspectives != 2) {
	// // topCandidatesCorrelation(perspectiveIDs, nPerspectives, table,
	// // out);
	// } else {
	// PreparedStatement ps = jdbc
	// .lookupPS("SELECT facet_id FROM "
	// +
	// "(SELECT facet_id, sum(d0) d0, sum(d1) d1, if(sum(vd0)=0,var, sum(vd0)) vd0, if (sum(vd1)=0,var,sum(vd1)) vd1, SUM(r) / SUM(r1) d00 "
	// + "FROM (SELECT facet_id, 2*z*z/varDenom var, if (substate=0, d, 0) d0,"
	// +
	// " if (substate=1, d, 0) d1, if (substate=0, vd, 0) vd0, if (substate=1, vd, 0) vd1,"
	// + " if (vd=0,0, d/vd) r , if (vd=0,varDenom/2/z/z, d/d/vd) r1 "
	// +
	// "FROM (SELECT facet_id, substate, SUM(p) d, z, varDenom, (SUM(varNum) + (2-COUNT(*))*z*z)/varDenom vd "
	// + "FROM (SELECT facet_id, state%2=1 substate, state, IF(state>1,-p,p) p,"
	// +
	// " (z*z  + 4 * n * p * (1 - p)) varNum,  (4 * (n + z * z) * (n + z * z)) varDenom, z "
	// +
	// "FROM (SELECT facet_id, foo.state, bar.cnt n, foo.cnt / bar.cnt p, 1.96 z "
	// +
	// "FROM (SELECT i2.facet_id, (i0.record_num IS NOT NULL)*2 + (i1.record_num IS NOT NULL)*1 state, COUNT(*) cnt "
	// + "FROM item_order rnd "
	// +
	// "LEFT JOIN item_facet_heap i0 ON rnd.record_num = i0.record_num AND i0.facet_id = ? "
	// +
	// "LEFT JOIN item_facet_heap i1 ON rnd.record_num = i1.record_num AND i1.facet_id = ? "
	// + "LEFT JOIN item_facet_heap i2 ON rnd.record_num = i2.record_num "
	// +
	// "where i2.facet_id != i0.facet_id or i0.facet_id is null and i2.facet_id != i1.facet_id or i1.facet_id is null "
	// + "GROUP BY i2.facet_id, state ORDER BY null) foo ,"
	// +
	// " (SELECT (i0.record_num IS NOT NULL)*2 + (i1.record_num IS NOT NULL)*1 state, COUNT(*) cnt "
	// +
	// "FROM item_order rnd LEFT JOIN item_facet_heap i0 ON rnd.record_num = i0.record_num AND i0.facet_id = ? "
	// +
	// "LEFT JOIN item_facet_heap i1 ON rnd.record_num = i1.record_num AND i1.facet_id = ? "
	// + "GROUP BY state ORDER BY null) bar "
	// + "where foo.state = bar.state) baz) fras "
	// + "GROUP BY facet_id, substate ORDER BY null) ack) bab "
	// + "GROUP BY facet_id ORDER BY null) gab "
	// +
	// "order by (d0 - d00) * (d0 - d00) / vd0 + (d1 - d00) * (d1 - d00) / vd1 desc "
	// + "limit ?");
	// int f1 = Integer.parseInt(perspectiveNames[0]);
	// int f2 = Integer.parseInt(perspectiveNames[1]);
	// ps.setInt(1, f1);
	// ps.setInt(2, f2);
	// ps.setInt(3, f1);
	// ps.setInt(4, f2);
	// ps.setInt(5, n);
	//
	// ResultSet rs = jdbc.SQLquery(ps);
	// sendResultSet(rs, MyResultSet.INT, out);
	// }
	// }

	String topCandQuery(int nFacets) {
		String x = "?, ?, ?, ?, ?, ?, ?";
		String IDs = x.substring(0, 3 * nFacets - 2);
		String result = "SELECT candidateID "
				+ "FROM (SELECT candidateID, "
				+ "ABS(jointProb - primaryProb*candidateProb)/"
				+ "SQRT((primaryProb-primaryProb*primaryProb)*(candidateProb-candidateProb*candidateProb)) corr "
				+ "FROM (SELECT candidateID, "
				+ "(SELECT n_items FROM facet"
				+ " WHERE facet_id=primaryID)/(SELECT COUNT(*) FROM item_order_heap) primaryProb, "
				+ "n_items/(SELECT COUNT(*) FROM item_order_heap) candidateProb, "
				+ "jointCount/(SELECT COUNT(*) FROM item_order_heap) jointProb "
				+ "FROM ( "
				+ "SELECT candidate.facet_id candidateID, candidate.n_items, prime.facet_id primaryID, "
				+ "(SELECT COUNT(*) FROM item_facet_heap i1 WHERE candidate.facet_id=i1.facet_id "
				+ "AND EXISTS (SELECT * FROM item_facet_heap i0"
				+ " WHERE i0.record_num=i1.record_num AND i0.facet_id = prime.facet_id)) jointCount "
				+ "FROM facet candidate, (SELECT facet_id FROM facet WHERE facet_id IN ("
				+ IDs + ")) prime " + "WHERE candidate.facet_id NOT IN (" + IDs
				+ ") AND candidate.parent_facet_id > 0 " + ") foo "
				// + "HAVING candidateProb>0.00001 "
				+ ") bar " + ") baz " + "GROUP BY candidateID "
				+ "ORDER BY POW(SUM(corr),2) - SUM(corr*corr) DESC LIMIT ?";
		return result;
	}

	@SuppressWarnings("unchecked")
	void topCandidates(String perspectiveIDs, int n, int table,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		myAssert(table != 1,
				"topCandidates needs a second case for restricted queries.");
		// String baseTable = table == 1 ? "restricted" : "item_order_heap";
		String sql = "CREATE TEMPORARY TABLE IF NOT EXISTS jointCounts (" +
				" candidateID smallint(5) unsigned NOT NULL," +
				" primaryID smallint(5) unsigned NOT NULL," +
				" jointCount bigint(21) NOT NULL default 0," +
				" PRIMARY KEY USING HASH (candidateID, primaryID)" +
				") ENGINE=MEMORY DEFAULT CHARSET=utf8";
		jdbc.SQLupdate(sql);
		jdbc.SQLupdate("TRUNCATE TABLE jointCounts");
		sql = "INSERT INTO jointCounts "
				+ "SELECT i1.facet_id candidateID, i0.facet_id primaryID, COUNT(*) jointCount "
				+ "FROM item_facet_heap i0 "
				+ "INNER JOIN item_facet_heap i1 ON i0.record_num = i1.record_num"
				+ " WHERE i1.facet_id NOT IN (" + perspectiveIDs
				+ ") AND i0.facet_id IN (" + perspectiveIDs
				+ ") GROUP BY candidateID, primaryID " + "ORDER BY NULL";
//		log(sql);
		jdbc.SQLupdate(sql);
		sql = "SELECT candidateID " +
			"FROM (SELECT candidateID," +
			"ABS(jointProb - primaryProb*candidateProb)/" +
			"SQRT((primaryProb-primaryProb*primaryProb)*(candidateProb-candidateProb*candidateProb)) corr " +
			"FROM (SELECT candidateID," +
			"(SELECT n_items FROM facet" +
			" WHERE facet_id=primaryID)/(SELECT COUNT(*) FROM item_order_heap) primaryProb," +
			"n_items/(SELECT COUNT(*) FROM item_order_heap) candidateProb," +
			"jointCount/(SELECT COUNT(*) FROM item_order_heap) jointProb " +
			"FROM (" 			
				+ "SELECT candidate.facet_id candidateID, candidate.n_items, prime.facet_id primaryID, foo.jointCount "
				+ "FROM facet candidate INNER JOIN facet prime LEFT JOIN "
				+ "jointCounts foo "
				+ "ON foo.candidateID = candidate.facet_id AND foo.primaryID = prime.facet_id "
				+ "WHERE candidate.parent_facet_id > 0 "
				+ "AND candidate.facet_id NOT IN ("
				+ perspectiveIDs
				+ ")"
				+ " AND prime.facet_id IN (" + perspectiveIDs + "))" + 
				" foo " +
				"HAVING candidateProb>0.00001" +
				") bar ) baz " +
				"GROUP BY candidateID " +
				"ORDER BY POW(SUM(corr),2) - SUM(corr*corr) DESC LIMIT " + n;
//		log(sql);
		ResultSet rs = jdbc.SQLquery(sql);
		sendResultSet(rs, MyResultSet.INT, out);
	}

	@SuppressWarnings("unchecked")
	void topCandidates1(String perspectiveIDs, int n, int table,
			DataOutputStream out) throws SQLException, ServletException,
			IOException {
		myAssert(table != 1,
				"topCandidates needs a second case for restricted queries.");
		// String baseTable = table == 1 ? "restricted" : "item_order_heap";
		String sql = "SELECT candidateID "
				+ "FROM (SELECT candidateID, "
				+ "ABS(jointProb - primaryProb*candidateProb)/"
				+ "SQRT((primaryProb-primaryProb*primaryProb)*(candidateProb-candidateProb*candidateProb)) corr "
				+ "FROM (SELECT candidateID, "
				+ "(SELECT n_items FROM facet"
				+ " WHERE facet_id=primaryID)/(SELECT COUNT(*) FROM item_order_heap) primaryProb, "
				+ "n_items/(SELECT COUNT(*) FROM item_order_heap) candidateProb, "
				+ "jointCount/(SELECT COUNT(*) FROM item_order_heap) jointProb "
				+ "FROM ( "
				+ "SELECT candidate.facet_id candidateID, candidate.n_items, prime.facet_id primaryID, "
				+ "(SELECT COUNT(*) FROM item_facet_heap i1 WHERE candidate.facet_id=i1.facet_id "
				+ "AND EXISTS (SELECT * FROM item_facet_heap i0"
				+ " WHERE i0.record_num=i1.record_num AND i0.facet_id = prime.facet_id)) jointCount "
				+ "FROM facet candidate, (SELECT facet_id FROM facet WHERE facet_id IN ("
				+ perspectiveIDs + ")) prime "
				+ "WHERE candidate.facet_id NOT IN (" + perspectiveIDs
				+ ") AND candidate.parent_facet_id > 0 " + ") foo "
				// + "HAVING candidateProb>0.00001 "
				+ ") bar " + ") baz " + "GROUP BY candidateID "
				+ "ORDER BY POW(SUM(corr),2) - SUM(corr*corr) DESC LIMIT " + n;
		// log(sql);
		ResultSet rs = jdbc.SQLquery(sql);
		sendResultSet(rs, MyResultSet.INT, out);
	}

	// @SuppressWarnings("unchecked")
	// void topCandidates(String perspectiveIDs, int n, int table,
	// DataOutputStream out) throws SQLException, ServletException,
	// IOException {
	// String[] perspectiveNames = perspectiveIDs.split(",");
	// int nPerspectives = perspectiveNames.length;
	// int[] IDs = new int[nPerspectives];
	// String[] addends = new String[IDs.length];
	// String[] where = new String[IDs.length];
	// String[] clauses = new String[IDs.length];
	// String baseTable = table == 1 ? "restricted" : "item_order_heap";
	// for (int i = 0; i < IDs.length; i++) {
	// IDs[i] = Integer.parseInt(perspectiveNames[i]);
	// clauses[i] = "(select f1.facet_id id"
	// + i
	// + ", f0.n_items/(SELECT COUNT(*) "
	// + "FROM item_order_heap) p1"
	// + i
	// + ", f1.n_items/(SELECT COUNT(*) FROM "
	// + baseTable
	// + ") p2"
	// + i
	// + ", "
	// + "COUNT(*)/(SELECT COUNT(*) FROM item_order_heap) p12"
	// + i
	// + " "
	// +
	// "FROM item_order rnd INNER JOIN item_facet_heap i0 ON rnd.record_num = i0.record_num AND i0.facet_id = "
	// + IDs[i]
	// + " INNER JOIN item_facet_heap i1 ON rnd.record_num = i1.record_num "
	// + "INNER JOIN facet f0 on f0.facet_id = i0.facet_id "
	// + "INNER JOIN facet f1 on f1.facet_id = i1.facet_id "
	// + "where f0.facet_id != f1.facet_id "
	// + "group by f1.facet_id " + "order by null) t" + i;
	// where[i] = "id" + i + " = id0";
	// addends[i] = "abs(p12" + i + " - p1" + i + "*p2" + i + ")/"
	// + "sqrt((p1" + i + "-p1" + i + "*p1" + i + ")*(p2" + i
	// + "-p2" + i + "*p2" + i + "))";
	// }
	//
	// String sql = "SELECT id0 FROM " + Util.join(clauses)
	// + " WHERE " + Util.join(where, " AND ") + " ORDER BY "
	// + Util.join(addends, " + ") + " DESC LIMIT " + n;
	// log(sql);
	// ResultSet rs = jdbc.SQLquery(sql);
	// sendResultSet(rs, MyResultSet.INT, out);
	// }

	// @SuppressWarnings("unchecked")
	// void topCandidatesMutInf(String perspectiveIDs, int n, int table,
	// DataOutputStream out) throws SQLException, ServletException,
	// IOException {
	// String[] perspectiveNames = perspectiveIDs.split(",");
	// int nPerspectives = perspectiveNames.length;
	// int[] IDs = new int[nPerspectives];
	// for (int i = 0; i < IDs.length; i++) {
	// IDs[i] = Integer.parseInt(perspectiveNames[i]);
	// // log("topCandidates "
	// // + i
	// // + " "
	// // + jdbc
	// // .SQLqueryString("Select name from facet where facet_id = "
	// // + IDs[i]));
	// }
	// String baseTable = table == 1 ? "restricted" : "item_order_heap";
	// double total = jdbc.SQLqueryInt("SELECT COUNT(*) FROM " + baseTable);
	// jdbc.SQLupdate("TRUNCATE TABLE mutInf");
	// mutInfQuery1.setDouble(1, total);
	// jdbc.SQLupdate(mutInfQuery1);
	//
	// // debug();
	//
	// for (int substate = 0; substate < 1 << nPerspectives; substate++) {
	// jdbc.SQLupdate("TRUNCATE TABLE state_items");
	// int[] include = new int[Util.weight(substate)];
	// int[] exclude = new int[nPerspectives - include.length];
	// int includeIndex = 0;
	// int excludeIndex = 0;
	// for (int i = 0; i < nPerspectives; i++) {
	// if (Util.isBit(substate, i)) {
	// include[includeIndex++] = IDs[i];
	// } else {
	// exclude[excludeIndex++] = IDs[i];
	// }
	// }
	// String SQL = QuerySQL.onItemsQuery(Collections.EMPTY_SET, include,
	// exclude, Collections.EMPTY_SET, null, true,
	// Collections.EMPTY_SET, baseTable);
	// int substateCount = jdbc
	// .SQLupdate("INSERT INTO state_items SELECT record_num FROM ("
	// + SQL + ") foo");
	// if (substateCount > 0) {
	// double p = substateCount / total;
	// // log(substate + " " + p);
	// mutInfQuery2.setInt(1, substateCount);
	// mutInfQuery2.setDouble(2, p);
	// jdbc.SQLupdate(mutInfQuery2);
	//
	// // debug();
	//
	// // ResultSet rs1 = jdbc
	// // .SQLquery("SELECT f.facet_id, COUNT(*)/? AS p "
	// // + "FROM relevantFacets f "
	// // + "INNER JOIN item_facet_heap i_f USING (facet_id) "
	// // + "INNER JOIN substateItems USING (record_num) "
	// // + "GROUP BY f.facet_id ORDER BY f.facet_id"
	// // + ")
	//
	// }
	// }
	//
	// // debug();
	//
	// ResultSet rs = jdbc
	// .SQLquery("SELECT facet_id FROM mutInf ORDER BY mutInf DESC LIMIT "
	// + n);
	// sendResultSet(rs, MyResultSet.INT, out);
	// }

	// @SuppressWarnings("unchecked")
	// private void debug() throws SQLException {
	// ResultSet rs1 = jdbc
	// .SQLquery(
	// "SELECT name,1000*mutInf FROM mutInf inner join facet using (facet_id) where mutinf.facet_id=802 ORDER BY mutInf DESC LIMIT "
	// + 1);
	// // log(MyResultSet.valueOfDeep(rs1, MyResultSet.STRING_INT, 100));
	// printRecords(rs1, MyResultSet.STRING_INT);
	// // log("total=" + total);
	// }

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
			jdbc.SQLupdate(printUserActionStmt);
		}
	}

	void restrict() throws SQLException {
		jdbc.SQLupdate("TRUNCATE TABLE restricted");
		jdbc.SQLupdate("INSERT INTO restricted SELECT * FROM onItems");
	}

	private boolean facetTableMucked = false;

	private void muckWithFacetTable() throws SQLException {
		jdbc.SQLupdate("CREATE TEMPORARY TABLE IF NOT EXISTS renames"
				+ " (old_facet_id INT, facet_id INT, PRIMARY KEY (facet_id))");
		if (!facetTableMucked) {
			facetTableMucked = true;

			// Get the types from raw_facet, as sort type can vary. Then
			// truncate it so we can insert the real data.
			jdbc.SQLupdate("DROP TABLE IF EXISTS facet_map");
			jdbc
					.SQLupdate("CREATE TEMPORARY TABLE facet_map AS"
							+ " (SELECT facet_id facet, parent_facet_id raw_facet, sort FROM raw_facet LIMIT 1)");
			jdbc.SQLupdate("TRUNCATE TABLE facet_map");

			// facet_types are in a different table, so can't roll these queries
			// into ensureFacetMapInternal
			jdbc
					.SQLupdate("INSERT INTO facet_map (SELECT f.facet_id facet, rf.facet_type_id raw_facet, rf.sort"
							+ " FROM facet f, raw_facet_type rf"
							+ " WHERE f.parent_facet_id = 0 AND f.name=rf.name)");
			ResultSet rs = jdbc
					.SQLquery("SELECT facet, raw_facet FROM facet_map");
			while (rs.next()) {
				int facet = rs.getInt(1);
				int raw_facet = rs.getInt(2);
				ensureFacetMapInternal(facet, raw_facet);
			}

			jdbc.SQLupdate("ALTER TABLE facet_map ADD PRIMARY KEY (facet)");
		}
	}

	private void ensureFacetMapInternal(int parent, int raw_parent)
			throws SQLException {
		PreparedStatement insertPS = jdbc
				.lookupPS("INSERT INTO facet_map (SELECT f.facet_id facet, rf.facet_id raw_facet, rf.sort"
						+ " FROM facet f, raw_facet rf"
						+ " WHERE f.parent_facet_id = ? AND rf.parent_facet_id = ? AND f.name = rf.name)");
		insertPS.setInt(1, parent);
		insertPS.setInt(2, raw_parent);
		jdbc.SQLupdate(insertPS);

		PreparedStatement findPS = jdbc
				.lookupPS("SELECT f.facet_id facet, rf.facet_id raw_facet"
						+ " FROM facet f, raw_facet rf"
						+ " WHERE f.parent_facet_id = ? AND rf.parent_facet_id = ? AND f.name = rf.name"
						+ " AND f.n_child_facets > 0");
		findPS.setInt(1, parent);
		findPS.setInt(2, raw_parent);
		ResultSet rs = jdbc.SQLquery(findPS);
		int[][] rsCache = new int[MyResultSet.nRows(rs)][];
		int i = 0;
		while (rs.next()) {
			// Have to store these temporarily because recursive call reuses
			// findPS
			int facet = rs.getInt(1);
			int raw_facet = rs.getInt(2);
			int[] temp = { facet, raw_facet };
			rsCache[i++] = temp;
		}
		for (int j = 0; j < rsCache.length; j++) {
			int[] is = rsCache[j];
			ensureFacetMapInternal(is[0], is[1]);
		}
	}

	private void changeID(int oldID, int newID) throws SQLException {

		PreparedStatement ps = jdbc
				.lookupPS("UPDATE facet_map SET facet = ? WHERE facet = ?");
		ps.setInt(1, newID);
		ps.setInt(2, oldID);
		jdbc.SQLupdate(ps);

		ps = jdbc.lookupPS("INSERT INTO renames VALUES(?, ?)");
		ps.setInt(1, oldID);
		ps.setInt(2, newID);
		jdbc.SQLupdate(ps);

		ps = jdbc
				.lookupPS("UPDATE item_facet_heap SET facet_id = ? WHERE facet_id = ?");
		ps.setInt(1, newID);
		ps.setInt(2, oldID);
		jdbc.SQLupdate(ps);

		ps = jdbc.lookupPS("UPDATE facet SET facet_id = ? WHERE facet_id = ?");
		ps.setInt(1, newID);
		ps.setInt(2, oldID);
		jdbc.SQLupdate(ps);

		ps = jdbc
				.lookupPS("UPDATE facet SET parent_facet_id = ? WHERE parent_facet_id = ?");
		ps.setInt(1, newID);
		ps.setInt(2, oldID);
		jdbc.SQLupdate(ps);
	}

	/**
	 * Add facet and all its ancestors to item.
	 * 
	 * @param facet
	 * @param item
	 * @param out
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	void addItemFacet(int facet, int item, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkNonNegative(item); // Dummy instance has ID=0
		addItemsFacet(facet, "SELECT " + item + " record_num", out);

		// checkPositive(facet);
		//
		// for (int ancestor = facet; ancestor > 0; ancestor =
		// parentFacetID(ancestor)) {
		// String table = parentFacetID(ancestor) > 0 ? "item_facet_heap"
		// : "item_facet_type_heap";
		// jdbc.SQLupdate("REPLACE INTO " + table + " VALUES(" + item + ", "
		// + ancestor + ")");
		// }
		//
		// // int prev = facet;
		// // int ancestor;
		// // while ((ancestor = parentFacetID(prev)) > 0) {
		// // jdbc.SQLupdate("REPLACE INTO item_facet_heap VALUES(" + item +
		// ", "
		// // + prev + ")");
		// // prev = ancestor;
		// // }
		// // jdbc.SQLupdate("REPLACE INTO item_facet_type_heap VALUES(" + item
		// // + ", " + prev + ")");
		//
		// updateFacetCounts(facet, out);
	}

	/**
	 * Add facet to all query results (as stored in table)
	 * 
	 * @param facet
	 * @param table
	 * @param out
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	void addItemsFacet(int facet, int table, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		String tableName = sortedResultTable(table);
		addItemsFacet(facet, "select record_num from " + tableName, out);
	}

	private void addItemsFacet(int facet, String itemExpr, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkPositive(facet);

		for (int ancestor = facet; ancestor > 0; ancestor = parentFacetID(ancestor)) {
			String heapTable = parentFacetID(ancestor) > 0 ? "item_facet_heap"
					: "item_facet_type_heap";
			jdbc.SQLupdate("REPLACE INTO " + heapTable + " SELECT record_num, "
					+ ancestor + " FROM (" + itemExpr + ") foo");
		}

		// int prev = facet;
		// int ancestor;
		// while ((ancestor = parentFacetID(prev)) > 0) {
		// jdbc.SQLupdate("REPLACE INTO item_facet_heap SELECT record_num, "
		// + prev + " FROM " + tableName);
		// prev = ancestor;
		// }
		//jdbc.SQLupdate("REPLACE INTO item_facet_type_heap SELECT record_num, "
		// + prev + " FROM " + tableName);

		updateFacetCounts(facet, out);
	}

	Set<Integer> removeItemsFacet(int facet, int table, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkPositive(facet);
		String tableName = sortedResultTable(table);
		jdbc.SQLupdate("DELETE FROM ifh USING item_facet_heap ifh, "
				+ tableName + " oi "
				+ "WHERE ifh.record_num = oi.record_num AND ifh.facet_id = "
				+ facet);
		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
						+ facet);
		Set<Integer> result = new HashSet<Integer>();
		boolean hasChildren = false;
		while (rs.next()) {
			hasChildren = true;
			result.addAll(removeItemsFacet(rs.getInt(1), table, null));
		}
		if (!hasChildren) {
			result.add(facet);
		}
		int grandparent = jdbc
				.SQLqueryInt("SELECT parent.parent_facet_id FROM facet f "
						+ "INNER JOIN facet parent ON f.parent_facet_id = parent.facet_id "
						+ "WHERE f.facet_id = " + facet);
		checkNonNegative(grandparent);
		if (grandparent == 0) {
			int parent = parentFacetID(facet);
			checkPositive(parent);
			jdbc
					.SQLupdate("DELETE FROM ifh USING item_facet_type_heap ifh, "
							+ tableName
							+ " oi "
							+ "WHERE ifh.record_num = oi.record_num AND ifh.facet_id = "
							+ parent);
		}
		if (out != null)
			updateFacetCounts(result, out);
		return result;
	}

	private int parentFacetID(int facet) throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
						+ facet);
	}

	Set<Integer> removeItemFacet(int facet, int item, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkPositive(facet);
		checkPositive(item);
		jdbc.SQLupdate("DELETE FROM item_facet_heap WHERE record_num = " + item
				+ " AND facet_id = " + facet);
		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id FROM facet WHERE parent_facet_id = "
						+ facet);
		Set<Integer> result = new HashSet<Integer>();
		boolean hasChildren = false;
		while (rs.next()) {
			hasChildren = true;
			result.addAll(removeItemFacet(rs.getInt(1), item, null));
		}
		if (!hasChildren) {
			result.add(facet);
		}
		int grandparent = jdbc
				.SQLqueryInt("SELECT parent.parent_facet_id FROM facet f "
						+ "INNER JOIN facet parent ON f.parent_facet_id = parent.facet_id "
						+ "WHERE f.facet_id = " + facet);
		checkNonNegative(grandparent);
		if (grandparent == 0) {
			int parent = parentFacetID(facet);
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
		int child;
		// if (parent > 0) {
		child = jdbc.SQLqueryInt("SELECT MAX(facet_id) + 1 FROM facet");
		// } else {
		// child = jdbc
		// .SQLqueryInt(
		// "SELECT MIN(facet_id) FROM facet WHERE parent_facet_id > 0");
		// int childParent = jdbc
		// .SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
		// + child);
		// myAssert(childParent > 0, child + "");
		// renumber(childParent);
		// }
		// int child = jdbc.SQLqueryInt("SELECT MAX(facet_id) + 1 FROM facet");
		checkPositive(child);
		jdbc.SQLupdate("INSERT INTO facet VALUES(" + child + ", '" + name
				+ "', " + parent + ", 1, 0, 0, 1)");
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
			int type = jdbc
					.SQLqueryInt("SELECT MAX(facet_type_id) + 1 FROM raw_facet_type");
			myAssert(order > 0 && order < 127, "Bad order " + order);
			jdbc.SQLupdate("INSERT INTO raw_facet_type VALUES(" + type + ", '"
					+ name
					+ "', 'content', ' that show ; that don\\'t show ', "
					+ order + ", 0, 1)");
			addChildFacet(child, "dummy", out);
		}
	}

	/**
	 * Does not remove instances from old parent, because they might be there
	 * for a different reason.
	 * 
	 * @param parent
	 * @param child
	 * @param out
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	void reparent(int parent, int child, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		checkNonNegative(parent);
		checkPositive(child);
		int oldParent = parentFacetID(child);
		jdbc.SQLupdate("UPDATE facet SET parent_facet_id = " + parent
				+ " WHERE facet_id = " + child);
		addItemsFacet(parent,
				"SELECT record_num FROM item_facet_heap WHERE facet_id = "
						+ child, null);

		// ResultSet rs = jdbc
		// .SQLquery("SELECT record_num FROM item_facet_heap WHERE facet_id = "
		// + child);
		// while (rs.next()) {
		// int item = rs.getInt(1);
		// addItemFacet(parent, item, null);
		// }

		renumber(oldParent);
		renumber(parent);
		child = jdbc
				.SQLqueryInt("SELECT facet_id FROM renames WHERE old_facet_id = "
						+ child);
		Set<Integer> leafs = new HashSet<Integer>();
		leafs.add(oldParent);
		leafs.add(child);
		updateFacetCounts(leafs, out);
	}

	/**
	 * Renumber parent's scattered and/or unordered children by moving them to a
	 * fresh set of IDs greater than all existing IDs and sorting by name (since
	 * we don't know sort). Recurse on each child.
	 * 
	 * @param parent
	 * @throws SQLException
	 * @throws ServletException
	 */
	private void renumber(int parent) throws SQLException, ServletException {

		// Remove children (with old ID) from relevantFacets table
		boolean relevant = jdbc
				.SQLqueryInt("SELECT f.facet_id FROM relevantFacets "
						+ "INNER JOIN facet f USING (facet_id) WHERE parent_facet_id = "
						+ parent + " LIMIT 1") > 0;
		if (relevant)
			updateRelevantFacets(null, Integer.toString(parent));

		// Create facet_map table if necessary
		muckWithFacetTable();

		int firstChildID = jdbc
				.SQLqueryInt("SELECT MAX(facet_id) + 1 FROM facet");
		jdbc.SQLupdate("UPDATE facet SET first_child_offset = "
				+ (firstChildID - 1) + " WHERE facet_id = " + parent);
		PreparedStatement ps = jdbc.lookupPS("SELECT facet_id FROM facet"
				+ " WHERE parent_facet_id = ? ORDER BY name");
		ps.setInt(1, parent);
		ResultSet rs = null;
		int newChildID = firstChildID;
		try {
			rs = jdbc.SQLquery(ps);
			while (rs.next()) {
				int oldChildID = rs.getInt(1);
				// jdbc.print("Renumber " + oldChildID + " => " + newChildID);
				changeID(oldChildID, newChildID++);
			}
		} finally {
			rs.close();
		}

		// Replace children (with new ID) into relevantFacets table
		if (relevant)
			updateRelevantFacets(Integer.toString(parent), null);

		// Wait until all children are moved to recurse, so MAX(facet_id) will
		// be
		// correct
		for (int child = firstChildID; child < newChildID; child++) {
			renumber(child);
		}
	}

	private void updateFacetCounts(int facet, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		Set<Integer> leafs = new HashSet<Integer>();
		leafs.add(facet);
		updateFacetCounts(leafs, out);
	}

	/**
	 * Recompute facet counts for these leafFacets and all their ancestors. Add
	 * a dummy instance if there are no real instances yet.
	 * 
	 * @param leafFacets
	 * @param out
	 *            for all renamed facets and their ancestors, write:
	 * 
	 *            facet_id, old_facet_id, n_items, first_child_offset,
	 *            parent_facet_id
	 * 
	 * @throws SQLException
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void updateFacetCounts(Set<Integer> leafFacets, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		int[] ancestors = null;
		for (Iterator<Integer> it = leafFacets.iterator(); it.hasNext();) {
			int ancestor = it.next().intValue();
			do {
				int nChildren = jdbc
						.SQLqueryInt("SELECT COUNT(*) FROM facet WHERE parent_facet_id = "
								+ ancestor);
				int nItems = 0;
				int parent = parentFacetID(ancestor);
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
			muckWithFacetTable();
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
		if (facetTableMucked) {
			int delta = jdbc.SQLqueryInt("SELECT MAX(facet_id) FROM facet");
			updateFacetIDs(delta);
			recomputeRawFacetType(delta);
			recomputeRawFacet();

			ConvertFromRaw converter = new ConvertFromRaw(jdbc);
			converter.findBrokenLinks(true, 1);
			converter.convert(1000);
		}
	}

	/**
	 * Renumber non-top-level facet.facet_id's to leave a big gap after the
	 * top-level ones
	 * 
	 * @param delta
	 * @throws SQLException
	 */
	private void updateFacetIDs(int delta) throws SQLException {
		jdbc
				.SQLupdate("UPDATE facet f, facet parent SET f.parent_facet_id = f.parent_facet_id + "
						+ delta
						+ " WHERE f.parent_facet_id = parent.facet_id "
						+ "AND parent.parent_facet_id > 0");
		jdbc.SQLupdate("UPDATE facet SET facet_id = facet_id + " + delta
				+ " WHERE parent_facet_id > 0");

		// It turns out to be faster to use item_facet as a scratchpad compared
		// to item_facet_heap.
		// item_facet may not be up to date, though, after editing
		jdbc.SQLupdate("TRUNCATE TABLE item_facet");
		jdbc.SQLupdate("INSERT INTO item_facet SELECT * FROM item_facet_heap");
		jdbc.SQLupdate("UPDATE item_facet SET facet_id = facet_id + " + delta);
		jdbc.SQLupdate("DELETE FROM item_facet WHERE record_num = 0");
	}

	private void recomputeRawFacetType(int delta) throws SQLException {
		jdbc.SQLupdate("DROP TABLE IF EXISTS rft");
		jdbc
				.SQLupdate("CREATE TEMPORARY  TABLE rft AS"
						+ " SELECT IFNULL(f.facet_id, -1) oldID, COUNT(*) ID, r.name, "
						+ "r.descriptionCategory, r.descriptionPreposition, r.sort, r.isOrdered "
						+ "FROM raw_facet_type r LEFT JOIN facet f USING (name) "
						+ "INNER JOIN raw_facet_type prev ON prev.name <= r.name "
						+ "WHERE f.parent_facet_id = 0 OR f.parent_facet_id IS NULL "
						+ "GROUP BY r.name ORDER BY null");

		// Use high IDs as scratchpad for updating all parent_facet_ids in
		// parallel
		jdbc.SQLupdate("UPDATE facet f, rft SET f.parent_facet_id = rft.ID + "
				+ (2 * delta) + " WHERE f.parent_facet_id = rft.oldID");
		jdbc.SQLupdate("UPDATE facet SET parent_facet_id = parent_facet_id - "
				+ (2 * delta) + " WHERE parent_facet_id > " + (2 * delta));

		jdbc.SQLupdate("UPDATE facet f, rft SET f.facet_id = rft.ID + "
				+ (2 * delta) + " WHERE f.facet_id = rft.oldID");
		jdbc.SQLupdate("UPDATE facet SET facet_id = facet_id - " + (2 * delta)
				+ " WHERE facet_id > " + (2 * delta));

		jdbc.SQLupdate("TRUNCATE TABLE raw_facet_type");
		jdbc
				.SQLupdate("INSERT INTO raw_facet_type SELECT ID, name, "
						+ "descriptionCategory, descriptionPreposition, sort, isOrdered "
						+ "FROM rft");
		jdbc.SQLupdate("DROP TABLE rft");
	}

	private void recomputeRawFacet() throws SQLException {
		jdbc.SQLupdate("TRUNCATE TABLE raw_item_facet");
		jdbc.SQLupdate("INSERT INTO raw_item_facet SELECT * FROM item_facet");

		jdbc.SQLupdate("TRUNCATE TABLE raw_facet");
		jdbc
				.SQLupdate("INSERT INTO raw_facet SELECT facet_id, name, parent_facet_id, sort FROM facet"
						+ " LEFT JOIN facet_map ON facet = facet_id "
						+ "WHERE parent_facet_id > 0");
	}

	void revert(String dateString) throws SQLException, ServletException {
		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = format.parse(dateString);
			File dataDir = new File(jdbc.SQLqueryString("SELECT @@datadir"));
			File dbDir = new File(dataDir, dbName);
			File MySQLdir = dataDir.getParentFile();
			File bkpDir = new File(new File(MySQLdir, "dataBKP"), dbName);

			// Find the date of the last restore before date, and log this
			// restore
			File restoreFile = new File(bkpDir, "restoreDates.txt");
			String restoreFileString = Util.readFile(restoreFile);
			String[] restoreDates = restoreFileString.split("\n");
			restoreFileString += "\n" + format.format(new Date());
			Util.writeFile(restoreFile, restoreFileString);
			Date from = null;
			for (int i = 0; i < restoreDates.length; i++) {
				Date restored = format.parse(restoreDates[i]);
				if (!restored.after(date))
					from = restored;
				else if (from == null)
					myAssert(false, "Attempt to revert to " + date
							+ ", which predates the full backup of " + restored);
				else
					break;
			}

			// Grab all the log files, and rely on from & date to pick out the
			// operations we want
			StringBuffer logFiles = new StringBuffer();
			File[] logFileNames = dataDir.listFiles(Util
					.getFilenameFilter(".*bin\\.\\d+"));
			for (int i = 0; i < logFileNames.length; i++) {
				File file = logFileNames[i];
				if (new Date(file.lastModified()).after(from))
					logFiles.append(" ").append(file.getName());
			}

			// This requires SUPER permission, which we don't really want to
			// give to p5
			// ResultSet rs = jdbc.SQLquery("SHOW BINARY LOGS");
			// while (rs.next()) {
			// String logfile = rs.getString("Log_name");
			// File f = new File(dataDir, logfile);
			// if (f.exists()) {
			// logFiles.append(" ").append(logfile);
			// }
			// }

			// Make sure logs are flushed and that files won't be in use when we
			// try to delete them
			exec("net stop \"MySQL\"");

			String[] corruptTables = { "facet", "raw_facet", "raw_facet_type",
					"raw_item_facet", "globals", "item", "item_facet",
					"item_facet_heap", "item_facet_type_heap" };
			for (int i = 0; i < corruptTables.length; i++) {
				exec("del /S /Q \"" + corruptTables[i] + ".*", dbDir);
				exec("copy \"" + bkpDir + "\\" + corruptTables[i] + ".* .",
						dbDir);
			}
			exec("net start \"MySQL\"");

			Pattern p = Pattern.compile(".*user=(.+?)&.*password=(.+?)&");
			Matcher m = p.matcher(jdbc.toString());
			m.find();
			String user = m.group(1);
			String pass = m.group(2);
			// exec("dir", dataDir);
			// exec("pwd", dataDir);
			exec("..\\bin\\mysqlbinlog --database=" + dbName
					+ " --start-date=\"" + format.format(from)
					+ "\" --stop-date=\"" + format.format(date) + "\""
					+ logFiles + " | ..\\bin\\mysql -u " + user + " -p" + pass
			// + "root -p<insert root password here>"
					, dataDir);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void exec(String command) throws IOException {
		exec(command, null);
	}

	private void exec(String command, File workingDirectory) throws IOException {
		log(command);
		Process proc = Runtime.getRuntime().exec("cmd.exe /C " + command, null,
				workingDirectory);

		// put a BufferedReader on the ls output
		InputStream inputstream = proc.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
		BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

		// read the ls output
		String line;
		while ((line = bufferedreader.readLine()) != null) {
			log(line);
		}

		// check for ls failure
		try {
			if (proc.waitFor() != 0) {
				log("exit value = " + proc.exitValue());
			}
		} catch (InterruptedException e) {
			log(e.toString());
		}
	}

	void rotate(int item, int clockwiseDegrees) throws SQLException,
			IOException, ServletException {
		ResultSet rs = jdbc
				.SQLquery("SELECT image, URI FROM images WHERE record_num = "
						+ item);
		if (rs.next()) {

			// Rotate images.image
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

			// Rotate URI
			String filename = rs.getString(2);
			myAssert(filename.endsWith(".jpg"), "rotate filname=" + filename);
			File f = new File(filename);
			BufferedImage im2 = Util.read(f.toURL());
			f.renameTo(new File(filename.substring(0, filename.length() - 4)
					+ "_unrotated.jpg"));
			BufferedImage rot2 = Util.rotate(im2, Math
					.toRadians(clockwiseDegrees));
			Util.writeImage(rot2, 85, filename);
		}
	}

	void rename(int facet, String name) throws SQLException {
		if (jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM facet WHERE facet_id = "
						+ facet) == 0) {
			String oldName = jdbc
					.SQLqueryString("SELECT name FROM facet WHERE facet_id = "
							+ facet);
			jdbc.SQLupdate("UPDATE raw_facet_type SET name = '" + name
					+ "' WHERE name = '" + oldName + "'");
		}
		jdbc.SQLupdate("UPDATE facet SET name = '" + name
				+ "' WHERE facet_id = " + facet);
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

		ConvertFromRaw.populatePairs(jdbc);
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
			try {
				PreparedStatement addCluster = jdbc
						.lookupPS("INSERT INTO clusterInfo VALUES(?, ?, ?, ?, ?)");
				PreparedStatement addClusterFacets = jdbc
						.lookupPS("INSERT INTO clusterFacets21 VALUES(?, ?, ?)");
				int q = jdbc.SQLqueryInt("SELECT COUNT(*) FROM onItems");
				int db = jdbc.SQLqueryInt("SELECT COUNT(*) FROM item");
				int c = jdbc
						.SQLqueryInt("SELECT MAX(cluster_id) FROM clusterInfo");
				rs = jdbc.SQLquery(clusterQuery(nFacets, facetRestriction));
				while (rs.next() && (pValue > 0 || neededClusters > 0)) {
					int con = rs.getInt(1);
					int ctot = rs.getInt(2);
					if (con * db > q * ctot) {
						double p = ChiSq2x2.getInstance(this, db, q, ctot, con)
								.pvalue();
						myAssert(p >= 0, "p = " + p);
						if (p < pValue || (p == pValue && neededClusters > 0)) {
							addCluster.setInt(1, ++c);
							addCluster.setInt(2, nFacets);
							addCluster.setInt(3, con);
							addCluster.setInt(4, ctot);
							addCluster.setDouble(5, p);
							jdbc.SQLupdate(addCluster);
							addClusterFacets.setInt(1, c);
							for (int i = 0; i < nFacets; i++) {
								addClusterFacets.setInt(2, rs.getInt(i + 3));
								addClusterFacets.setInt(3, i + 1);
								jdbc.SQLupdate(addClusterFacets);
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

	@SuppressWarnings("unchecked")
	void caremediaPlayArgs(String items, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		log("caremediaPlayArgs " + items);
		// event.segment_id is always NULL
		ResultSet rs = jdbc
				.SQLquery("SELECT segment.segment_id,"
						+ " TIMESTAMPDIFF(SECOND, copyright_date, start_date) start_offset,"
						+ " TIMESTAMPDIFF(SECOND, copyright_date, end_date)"
						+ " FROM item INNER JOIN event ON item.record_num = event.event_id"
						+ " INNER JOIN movie USING (movie_id)"
						+ " INNER JOIN segment ON segment.movie_id = movie.movie_id"
						+ " WHERE record_num IN (" + items

						// Ignore events that straddle movie boundaries
						+ ") HAVING start_offset >= 0"

						+ " ORDER BY segment.segment_id");
		printRecords(rs, MyResultSet.SNMINT_INT_INT);
		sendResultSet(rs, MyResultSet.SNMINT_INT_INT, out);
	}

	@SuppressWarnings("unchecked")
	void caremediaGetItems(String segments, DataOutputStream out)
			throws SQLException, ServletException, IOException {
		// Seems messed up - client sends items, not segments
		// log("caremediaPlayArgs"+item);
		ResultSet rs = jdbc
				.SQLquery("SELECT record_num"
						+ " FROM item "
						+ " INNER JOIN movie USING (movie_id)"
						+ " INNER JOIN segment ON segment.movie_id = movie.movie_id"
						+ " WHERE segment_id IN (" + segments
						+ ") ORDER BY record_num");
		// printRecords(rs, MyResultSet.INT_INT_INT);
		sendResultSet(rs, MyResultSet.SINT, out);
	}

	void printRecords(ResultSet result, List<Object> types) {
		log(MyResultSet.valueOfDeep(result, types, 5));
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

	// private PreparedStatement setItemDescriptionQuery;

	// void setItemDescription(int item, String rawText) throws SQLException {
	// // if (setItemDescriptionQuery == null) {
	// // setItemDescriptionQuery = jdbc
	// // .lookupPS("UPDATE item SET description = ? WHERE record_num = ? ");
	// // }
	// // synchronized (setItemDescriptionQuery) {
	// // setItemDescriptionQuery.setInt(2, item);
	// // setItemDescriptionQuery.setString(1, description);
	// // jdbc.SQLupdate(setItemDescriptionQuery);
	// // }
	//
	// String[] fields = jdbc.SQLqueryString(
	// "SELECT itemDescriptionFields FROM globals").split(",");
	// String[] values = rawText.split("\\n\\d+\\n");
	// Pattern p = Pattern.compile("\\n(\\d+)\\n");
	// Matcher m = p.matcher(rawText);
	// int valueIndex = 1;
	// while (m.find()) {
	// int fieldIndex = Integer.parseInt(m.group(1));
	// assert fieldIndex >= 0 && fieldIndex < fields.length;
	// String value = values[valueIndex++];
	// String field = fields[fieldIndex];
	// jdbc.SQLupdate("UPDATE item SET " + field + " = "
	// + JDBCSample.quote(value) + " WHERE record_num = " + item);
	// }
	// assert valueIndex == values.length;
	// }

	void setItemDescription(int item, String displayText) throws SQLException {
		String[] values = displayText.split("(?:\\n|\\r)+<\\w+>(?:\\n|\\r)+");
		// Util.print(Util.valueOfDeep(values));
		// StringBuffer buf = new StringBuffer();
		String fieldNames = jdbc
				.SQLqueryString("SELECT itemDescriptionFields FROM globals");
		String[] fields = fieldNames.split(",");
		Pattern p = Pattern.compile("(?:\\n|\\r)+<(\\w+)>(?:\\n|\\r)+");
		Matcher m = p.matcher(displayText);
		int valueIndex = 1;
		while (m.find()) {
			String fieldName = m.group(1);
			if (!Util.isMember(fields, fieldName)) {
				fieldNames += "," + fieldName;
				jdbc.SQLupdate("UPDATE globals SET itemDescriptionFields = "
						+ JDBCSample.quote(fieldNames));
				jdbc.SQLupdate("ALTER TABLE item ADD COLUMN `" + fieldName
						+ "` TEXT DEFAULT NULL");
			}
			String value = values[valueIndex++];
			// int fieldIndex = Util.member(fields, fieldName);
			// Util.print("found "+fieldName+" "+fieldIndex);
			// assert fieldIndex >= 0 : "Must add new fields manually: "
			// + fieldName;
			jdbc.SQLupdate("UPDATE item SET " + fieldName + " = "
					+ JDBCSample.quote(value) + " WHERE record_num = " + item);
			// buf.append("\n").append(fieldIndex).append("\n");
			// buf.append(value);
		}
		// Util.print("getRawText '"+displayText+"'\n'"+buf+"'");
	}

	@SuppressWarnings("unchecked")
	void opsSpec(int session, DataOutputStream out) throws SQLException,
			ServletException, IOException {
		// order by timestamp for sessions before we recorded action_numbers
		ResultSet rs = jdbc
				.SQLquery("SELECT CONCAT_WS(',',"
						+ " TIMESTAMPDIFF(SECOND, (SELECT MIN(timestamp) FROM user_actions WHERE session = "
						+ session
						+ "),"
						+ " timestamp),"
						+ " location, object, modifiers) FROM user_actions WHERE session = "
						+ session + " ORDER BY action_number, timestamp");
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
		// boolean sorted = false;
		// boolean positive = false;
		if (type == Column.SortedIntegerType) {
			writeIntCol(result, colIndex, out, true, true);
		} else if (type == Column.PositiveIntegerType) {
			writeIntCol(result, colIndex, out, false, true);
		} else if (type == MyResultSet.Column.IntegerType) {
			writeIntCol(result, colIndex, out, false, false);
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

			// The check in writeIntOrTwo is not always sufficient to detect
			// negative values, because it can look like a flag for 'no next
			// value'
			myAssert(value >= 0, "Column " + colIndex + ", row "
					+ result.getRow() + " contains the negative integer "
					+ value);

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
			try {
				writeBlob(result.getBlob(colIndex), imageW, imageH, quality,
						result.getInt(colIndex + 1), result
								.getInt(colIndex + 2), out);
			} catch (IllegalArgumentException e) {
				// Java may barf "IllegalArgumentException: Invalid ICC Profile
				// Data"
				// on images that other applications handle fine. See
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6404011
				writeInt(0, out);
			} catch (Exception e) {
				throw new ServletException("Got exception " + e
						+ " while writing blob on row " + result.getRow()
						+ " col " + colIndex + " " + currentRowToString(result));
			}
			// else
			// writeBlob(result.getBlob(colIndex), -1, -1, out);
		}
	}

	static String currentRowToString(ResultSet rs) throws SQLException {
		StringBuffer buf = new StringBuffer();
		ResultSetMetaData meta = rs.getMetaData();
		for (int col = 1; col <= meta.getColumnCount(); col++) {
			buf.append(" Col ").append(col).append("=");
			int type = meta.getColumnType(col);
			switch (type) {
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.BIT:
			case Types.BOOLEAN:
			case Types.DECIMAL:
				buf.append(rs.getInt(col));
				break;
			case Types.CHAR:
			case Types.VARCHAR:
				buf.append(rs.getString(col));
				break;
			case Types.CLOB:
			case Types.BLOB:
				buf.append("<BLOB>");
				break;
			default:
				buf.append("<Unhandled Type ").append(type).append(">");
				break;
			}
			buf.append(";");
		}
		return buf.toString();
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
		int newW = (int) Math.round(actualW * ratio);
		int newH = (int) Math.round(actualH * ratio);
		myAssert(newW == desiredW || newH == desiredH, "WARNING: bad resize: "
				+ desiredW + "x" + desiredH + " " + newW + "x" + newH);

		BufferedImage resized = Util.resize(ImageIO.read(blobStream), newW,
				newH, false);
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
				writeBlob(blob, newW, newH, quality, newW, newH, out);
			}
		} finally {
			byteArrayStream.close();
		}
	}

	String dbDescs(String dbNameList) throws SQLException, ServletException {
		myAssert(dbNameList != null && dbNameList.length() > 0,
				"Empty db name list");
		String[] dbNames = Util.splitComma(dbNameList);
		StringBuffer dbDescs = new StringBuffer();
		for (int i = 0; i < dbNames.length; i++) {
			dbDescsInternal(dbDescs, dbNames[i]);
		}
		if (!Util.isMember(dbNames, dbName)) {
			// In case URL specified a "hidden" database, get it's description
			// too.
			dbDescsInternal(dbDescs, dbName);
		}
		// log(Util.join(dbDescs, ";"));
		return dbDescs.toString();
	}

	void dbDescsInternal(StringBuffer dbDescs, String name) throws SQLException {
		String desc = jdbc.SQLqueryString("SELECT description FROM " + name
				+ ".globals");
		if (dbDescs.length() > 0)
			dbDescs.append(";");
		dbDescs.append(name).append(",").append(desc);
	}
}
