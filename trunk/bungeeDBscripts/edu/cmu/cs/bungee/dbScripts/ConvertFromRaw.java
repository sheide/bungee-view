package edu.cmu.cs.bungee.dbScripts;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import JSci.maths.statistics.ChiSqr;
import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.ApplicationSettings;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.StringToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.Token;

public class ConvertFromRaw {

	static final int maxErrorsToPrint = 8;

	private static final int MIN_FACETS = 1000;

	static StringToken sm_db = new StringToken("db", "database to convert", "",
			Token.optRequired | Token.optSwitch, "");
	static StringToken sm_server = new StringToken("server", "MySQL server",
			"", Token.optSwitch, "jdbc:mysql://localhost/");
	static StringToken sm_user = new StringToken("user", "MySQL user", "",
			Token.optSwitch, "bungee");
	static StringToken sm_pass = new StringToken("pass", "MySQL user password",
			"", Token.optSwitch, "p5pass");
	static ApplicationSettings sm_main = new ApplicationSettings();
	static {
		sm_main.addToken(sm_db);
		sm_main.addToken(sm_server);
		sm_main.addToken(sm_user);
		sm_main.addToken(sm_pass);
	}

	// private String connectString;

	private JDBCSample jdbc;

	// static final int NO_ERRORS = 0;
	//
	// static final int INPUT_ERRORS = 1;
	//
	// static final int OUTPUT_ERRORS = 2;

	public static void main(String[] args) throws Exception {
		if (sm_main.parseArgs(args)) {
			ConvertFromRaw convert = new ConvertFromRaw(sm_server.getValue(),
					sm_db.getValue(), sm_user.getValue(), sm_pass.getValue());
			convert.convert();
		}
	}

	public void convert() throws SQLException {
		// createTables();
		canonicalize(MIN_FACETS);
		// addImageSizes();
	}

	public ConvertFromRaw(String _server, String _db, String _user, String _pass)
			throws SQLException, InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		jdbc = new JDBCSample(_server, _db, _user, _pass);
	}

	public ConvertFromRaw(JDBCSample _jdbc) {
		jdbc = _jdbc;
	}

	private void canonicalize(int minFacets) throws SQLException {
		// Create facet from raw_facet & raw_facet_type. Also update item_facet.
		if (checkRawErrors() == 0) {
			summarize();
			computeItemCounts();
			// createMultiples();
			createTemp();
			// if (orderChildren()) {
			// countDescendents();
			computeIDs();

			int nCounts = getMaxCount();
			String countType = jdbc.unsignedTypeForMaxValue(nCounts);
			int nFacets = jdbc
					.SQLqueryInt("SELECT MAX(canonical_facet_id) FROM temp");
			String facet_idType = jdbc.unsignedTypeForMaxValue(Math.max(
					minFacets, nFacets));
			int nItems = jdbc.SQLqueryInt("SELECT MAX(record_num) FROM item");
			String item_idType = jdbc.unsignedTypeForMaxValue(nItems);
			jdbc.print("   countType: " + nCounts + " " + countType);
			jdbc.print("facet_idType: " + nFacets + " " + facet_idType);
			jdbc.print(" item_idType: " + nItems + " " + item_idType);
			createItemFacet(facet_idType, item_idType);
			// finishMult();
			createFacet(countType, facet_idType);
			createAncestor(facet_idType);
			addLftRgt();
			// createAncestorNames();
			createItemFacetType();
			createRandomItemIDs();
			createUserActions();

			jdbc.print("\nChecking output tables for errors...");
			int nErrors = 0; // No longer true... checkTempCount();
			nErrors += findBrokenLinks(false, maxErrorsToPrint);
			// result = checkCanonErrors();

			nErrors += setUpFacetSearch();
			jdbc.print("...found " + nErrors + " errors in output tables.");

			createPairsTable(facet_idType, item_idType);
			createCorrelationsTable(jdbc, facet_idType);

			// createEntropy(facet_idType);

			jdbc.print("\nCleaning up...");
			// purgeMultiples();
			// jdbc.SQLupdate("DROP TABLE item_multiples");
			// jdbc.SQLupdate("DROP TABLE multiples");
			jdbc.SQLupdate("DROP TABLE temp");
			jdbc.SQLupdate("DROP TABLE itemCounts");
			jdbc.SQLupdate("OPTIMIZE TABLE facet, item_facet, item ");

			// Clean up in case we've created non-temporary versions for
			// debugging
			jdbc.SQLupdate("DROP TABLE IF EXISTS onItems");
			jdbc.SQLupdate("DROP TABLE IF EXISTS restricted");
			jdbc.SQLupdate("DROP TABLE IF EXISTS relevantFacets");
			jdbc.SQLupdate("DROP TABLE IF EXISTS renames");
			jdbc.SQLupdate("DROP TABLE IF EXISTS rft");
			jdbc.SQLupdate("DROP TABLE IF EXISTS clusterInfo");
			jdbc.SQLupdate("DROP TABLE IF EXISTS clusterFacets21");

			jdbc.print("\nDone.");
			// }
		}
	}

	// private void createEntropy(String facet_idType) throws SQLException {
	// jdbc.SQLupdate("DROP TABLE IF EXISTS entropy");
	// jdbc.SQLupdate("CREATE TABLE entropy (facet_id " + facet_idType
	// + ", entropy FLOAT unsigned NOT NULL, PRIMARY KEY (facet_id))");
	// jdbc
	// .SQLupdate("INSERT INTO entropy "
	// + "(SELECT facet_id, -p*log(p)-(1-p)*log(1-p) entropy FROM"
	// +
	// " (SELECT facet_id, (n_items +0.000000001) / (SELECT COUNT(*) FROM item) p FROM facet"
	// + " WHERE parent_facet_id > 0) probs)");
	// jdbc.SQLupdate("DROP FUNCTION IF EXISTS entropy");
	// jdbc
	// .SQLupdate("CREATE FUNCTION entropy (on_count INTEGER, total_count INTEGER)"
	// + " RETURNS FLOAT DETERMINISTIC "
	// + "BEGIN"
	// + " DECLARE p DEFAULT (on_count + 0.000001) / total_count;"
	// + " RETURN IF(p>=1,0,-p*log(p)-(1-p)*log(1-p)); "
	// + "END");
	// }

	private void summarize() throws SQLException {
		printErrors(
				"SELECT if(r.sort<0 OR COUNT(*) < 2,'ignore ', '       ') include, "
						+ "if(f.parent_facet_id is null, 0, count(*)) n_children,"
						+ "r.name, substring_index(group_concat(f.name separator '%$%'), '%$%', 1) example "
						+ "FROM raw_facet_type r left join raw_facet f on f.parent_facet_id = r.facet_type_id "
						+ "group by r.facet_type_id ORDER BY r.sort",
				"Summary:",
				"Include, Number of Immediate Children, Facet Type, Example Child",
				999);
		printErrors("SELECT name FROM raw_facet WHERE name like ' %'",
				"Warning: facet name starts with blank space", "Name");
	}

	private void createTemp() throws SQLException {
		jdbc.print("\nCreating temp...");

		jdbc.SQLupdate("DROP TABLE IF EXISTS temp");
		// Can't make this temporary, or you get "can't reopen table foo"
		// errors.
		jdbc.SQLupdate("CREATE TABLE temp ("
				+ " facet_id int unsigned NOT NULL default 0,"
				+ " name VARCHAR(255) NOT NULL DEFAULT '<unnamed>'," // Increasing
				// varchar
				// size
				// above
				// 255
				// slows
				// things
				// waaaay
				// down
				+ " parent_facet_id int unsigned NOT NULL default 0,"
				+ " canonical_facet_id int unsigned NOT NULL default 0,"
				// + " ordered_child_names longtext,"
				+ " n_child_facets int NOT NULL default 0,"
				+ " children_offset int NOT NULL default 0,"
				+ " PRIMARY KEY (facet_id)," + " KEY key1 (parent_facet_id ),"
				+ " KEY name (name)" + " ) ENGINE=MyISAM DEFAULT CHARSET=utf8");
	}

	private void computeIDs() throws SQLException {
		jdbc.SQLupdate("DROP PROCEDURE IF EXISTS computeIDs");
		jdbc
				.SQLupdate("CREATE PROCEDURE computeIDs(parent INT, grandparent INT, canonical INT, parent_name VARCHAR(255)) "
						+ "BEGIN "
						+ " DECLARE done INT DEFAULT 0; "
						+ " DECLARE child, nChildren, offset INT DEFAULT 0; "
						+ " DECLARE child_name VARCHAR(255); "
						+ " DECLARE cur CURSOR FOR SELECT facet_id, name FROM raw_facet"
						+ " WHERE parent_facet_id = parent ORDER BY sort, name;"
						+ " DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1; "
						+ " SELECT COUNT(*) INTO nChildren FROM raw_facet WHERE parent_facet_id = parent; "
						+ " SET offset = @child_offset; "
						+ " INSERT INTO temp VALUES(parent, parent_name, grandparent, canonical, nChildren, offset);"
						+ " SET @child_offset = @child_offset+nChildren; "
						+ " OPEN cur; "
						+ " REPEAT "
						+ "  FETCH cur INTO child, child_name; "
						+ "  IF NOT done THEN "
						+ "   SET offset = offset + 1; "
						+ "   CALL computeIDs(child, parent, offset, child_name);"
						+ "  END IF; "
						+ " UNTIL done END REPEAT; "
						+ " CLOSE cur; " + "END ");

		// Skip facet_types that have fewer than two children
		ResultSet rs = jdbc
				.SQLquery("SELECT ft.facet_type_id, ft.name "
						+ "FROM raw_facet_type ft "
						+ "INNER JOIN raw_facet f ON f.parent_facet_id = ft.facet_type_id "
						+ "WHERE ft.sort >= 0 GROUP BY ft.facet_type_id "
						+ "HAVING COUNT(*) > 1 ORDER BY ft.sort");
		int nFacetTypes = MyResultSet.nRows(rs);
		jdbc.SQLupdate("SET @child_offset = " + nFacetTypes
				+ ", max_sp_recursion_depth = 100;");
		int ID = 1;
		PreparedStatement computeIDs = jdbc
				.lookupPS("CALL computeIDs(?, 0, ?, ?)");
		while (rs.next()) {
			computeIDs.setInt(1, rs.getInt(1));
			computeIDs.setInt(2, ID++);
			computeIDs.setString(3, rs.getString(2));
			jdbc.SQLupdate(computeIDs);
		}
		jdbc.SQLupdate("DROP PROCEDURE computeIDs");
	}

	private void computeItemCounts() throws SQLException {
		jdbc.print("\nComputing item counts...");
		// Note that facet types have n_items = 0

		jdbc.SQLupdate("DROP TABLE IF EXISTS itemCounts");

		jdbc.SQLupdate("CREATE TABLE itemCounts ("
				+ " facet_id int unsigned NOT NULL default 0,"
				+ " cnt int unsigned NOT NULL default 0,"
				+ " PRIMARY KEY (facet_id)"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8");

		jdbc.SQLupdate("INSERT INTO itemCounts"
				+ " SELECT rif.facet_id, COUNT(*) cnt"
				+ " FROM raw_item_facet rif"
				+ " INNER JOIN raw_facet f ON f.facet_id = rif.facet_id"
				// + " INNER JOIN raw_facet_type type ON type.facet_type_id =
				// f.facet_type_idxx AND type.sort > 0"
				+ " GROUP BY facet_id");

		int maxCount = getMaxCount();
		String maxFacet = jdbc
				.SQLqueryString("SELECT GROUP_CONCAT(f.name SEPARATOR ', ') "
						+ "FROM raw_facet f INNER JOIN itemCounts c ON f.facet_id = c.facet_id"
						+ " WHERE c.cnt = " + maxCount);
		jdbc.print("Max count = " + maxCount + " for " + maxFacet);
	}

	private int getMaxCount() throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT MAX(cnt) "
						+ "FROM itemCounts c INNER JOIN raw_facet f ON c.facet_id = f.facet_id "
				// + "INNER JOIN raw_facet_type ft on f.facet_type_idxx =
				// ft.facet_type_id "
				// + "WHERE ft.sort > 0"
				);
	}

	private void createFacet(String countType, String facet_idType)
			throws SQLException {
		jdbc.print("\nCreating facet...");
		jdbc.SQLupdate("DROP TABLE IF EXISTS facet");

		// int maxLftRgt = jdbc
		// .SQLqueryInt("SELECT MAX(canonical_facet_id) FROM temp") * 2;
		// String lftRgtType = jdbc.unsignedTypeForMaxValue(maxLftRgt);

		facet_idType += " NOT NULL";

		jdbc.SQLupdate("CREATE TABLE facet (" +

		" facet_id " + facet_idType + "," +

		" name VARCHAR(200) NOT NULL," +

		" parent_facet_id " + facet_idType + " ," +

		" n_items " + countType + " NOT NULL," +

		" n_child_facets " + facet_idType + " ," +

		" first_child_offset " + facet_idType + "," +

		" is_alphabetic BIT NOT NULL," +

		// " usage_count MEDIUMINT," +

				" PRIMARY KEY (facet_id)," +

				" KEY parent (parent_facet_id)" +

				") ENGINE=MyISAM DEFAULT CHARSET=utf8");

		// parent.canonical_facet_id and cnt will be NULL for facet types.
		jdbc
				.SQLupdate("INSERT INTO facet"
						+ " SELECT "
						+ " facet.canonical_facet_id AS facet_id,"
						+ " facet.name,"
						+ " IFNULL(parent.canonical_facet_id, 0) AS parent_facet_id,"
						+ " IFNULL(cnt, 0) AS n_items,"
						+ " facet.n_child_facets,"
						+ " IF(facet.n_child_facets > 0, facet.children_offset, 0),"
						+ " TRUE AS is_alphabetic"
						// + " 0 AS usage_count"
						+ " FROM (temp facet"
						+ " LEFT JOIN itemCounts ON itemCounts.facet_id = facet.facet_id)"
						+ " LEFT JOIN temp parent ON facet.parent_facet_id = parent.facet_id");

		jdbc.SQLupdate(" UPDATE facet,"
				+ " (SELECT DISTINCT parent.facet_id unalph"
				+ " FROM facet child1, facet child2, facet parent"
				+ " WHERE child1.parent_facet_id = parent.facet_id "
				+ " AND child2.parent_facet_id = parent.facet_id "
				+ " AND child2.facet_id = child1.facet_id + 1 "
				+ " AND child2.name < child1.name) foo"
				+ " SET is_alphabetic = FALSE" + " WHERE facet_id = unalph");
	}

	private void createAncestor(String facet_idType) throws SQLException {
		jdbc.print("\nCreating ancestor...");
		jdbc.SQLupdate("DROP TABLE IF EXISTS ancestor");

		facet_idType += " NOT NULL";
		jdbc
				.SQLupdate("CREATE TABLE ancestor ("
						+ " facet_id "
						+ facet_idType
						+ ", ancestor_id "
						+ facet_idType
						+ ", INDEX facet (facet_id)) ENGINE=MyISAM DEFAULT CHARSET=utf8");

		jdbc
				.SQLupdate("INSERT INTO ancestor SELECT facet_id, facet_id FROM facet");
		int nAncestors = 0;
		int prevAncestors = 0;

		PreparedStatement getNAncestors = jdbc
				.lookupPS("SELECT COUNT(*) FROM ancestor");
		PreparedStatement updateAncestor = jdbc
				.lookupPS("INSERT INTO ancestor SELECT a.facet_id, f.parent_facet_id "
						+ "FROM facet f INNER JOIN ancestor a ON f.facet_id = a.ancestor_id "
						+ "LEFT JOIN ancestor b ON a.facet_id = b.facet_id AND f.parent_facet_id = b.ancestor_id "
						+ "WHERE f.parent_facet_id > 0 AND b.facet_id IS NULL");

		while ((nAncestors = jdbc.SQLqueryInt(getNAncestors)) > prevAncestors) {
			prevAncestors = nAncestors;
			// Util.print(nAncestors);
			jdbc.SQLupdate(updateAncestor);
		}
	}

	private void createItemFacet(String facet_idType, String item_idType)
			throws SQLException {
		jdbc.print("\nCreating itemFacet...");
		jdbc.SQLupdate("DROP TABLE IF EXISTS item_facet");

		jdbc.SQLupdate("CREATE TABLE item_facet ("
				+ "record_num mediumint(6) unsigned NOT NULL default '0', "
				+ "facet_id int(10) unsigned NOT NULL default '0', "
				+ "KEY facet (facet_id), " + "KEY item (record_num)) "
				+ "ENGINE=MyISAM DEFAULT CHARSET=latin1;");

		jdbc
				.SQLupdate("INSERT INTO item_facet"
						+ " SELECT "
						+ " record_num,"
						+ " canonical_facet_id AS facet_id"
						+ " FROM raw_item_facet INNER JOIN temp ON raw_item_facet.facet_id = temp.facet_id"
						+ " WHERE canonical_facet_id > 0");

		jdbc.print("  creating heap versions...");
		jdbc.SQLupdate("DROP TABLE IF EXISTS item_facetntype_heap");

		jdbc
				.SQLupdate("CREATE TABLE item_facetntype_heap ("
						+ " record_num  "
						+ item_idType
						+ " NOT NULL,"
						+ " facet_id "
						+ facet_idType
						+ " NOT NULL,"
						+ " PRIMARY KEY (record_num, facet_id),"
						+ " KEY item (record_num),"
						+ " KEY facet (facet_id)"
						+ ") ENGINE=HEAP DEFAULT CHARSET=ascii PACK_KEYS=1 ROW_FORMAT=FIXED");
		int nFacetTypes = jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM temp WHERE parent_facet_id = 0");
		jdbc.SQLupdate("CREATE OR REPLACE VIEW item_facet_heap AS"
				+ " SELECT * FROM item_facetntype_heap WHERE facet_id > "
				+ nFacetTypes);
		jdbc.SQLupdate("CREATE OR REPLACE VIEW item_facet_type_heap AS"
				+ " SELECT * FROM item_facetntype_heap WHERE facet_id <= "
				+ nFacetTypes);

		// jdbc
		// .SQLupdate("INSERT INTO item_facet_heap SELECT record_num, facet_id FROM item_facet");
	}

	// rgt table is used by populateRandomItemIDs
	private void addLftRgt() throws SQLException {
		jdbc.print("\nComputing lft & rgt ...");
		int maxRgt = 2 * jdbc
				.SQLqueryInt("SELECT MAX(canonical_facet_id) FROM temp");
		String facet_idType = jdbc.unsignedTypeForMaxValue(maxRgt);

		jdbc
				.SQLupdate("CREATE TEMPORARY TABLE rgt ("
						+ " facet_id  "
						+ facet_idType
						+ " ,"
						+ " rgt "
						+ facet_idType
						+ " ,"
						+ " PRIMARY KEY (facet_id)"
						+ ") ENGINE=HEAP DEFAULT CHARSET=ascii PACK_KEYS=1 ROW_FORMAT=FIXED");

		int nChildren = jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM facet WHERE parent_facet_id = 0");
		int rgt = 1;
		for (int i = 0; i < nChildren; i++) {
			rgt = addLftRgtInternal(1 + i, rgt);
		}

		int nFacets = jdbc.SQLqueryInt("SELECT COUNT(*) FROM facet");
		int nRgt = jdbc.SQLqueryInt("SELECT COUNT(*) FROM rgt");
		assert nFacets == nRgt : nFacets + " " + nRgt;
	}

	private PreparedStatement lftRgt1;

	private PreparedStatement lftRgt2;

	private PreparedStatement lftRgt3;

	private int addLftRgtInternal(int facet, int lft) throws SQLException {
		int rgt = lft + 1;
		if (lftRgt1 == null) {
			lftRgt1 = jdbc
					.lookupPS("SELECT first_child_offset FROM facet WHERE facet_id = ? AND n_child_facets > 0");
			lftRgt2 = jdbc
					.lookupPS("SELECT n_child_facets FROM facet WHERE facet_id = ?");
			lftRgt3 = jdbc.lookupPS("INSERT INTO rgt VALUES(?, ?)");
		}
		lftRgt1.setInt(1, facet);
		int child = jdbc.SQLqueryInt(lftRgt1);
		if (child >= 0) {
			lftRgt2.setInt(1, facet);
			int nChildren = jdbc.SQLqueryInt(lftRgt2);
			for (int i = 0; i < nChildren; i++) {
				rgt = addLftRgtInternal(child + 1 + i, rgt);
			}
		}
		// lftRgt3.setInt(1, lft);
		lftRgt3.setInt(1, facet);
		lftRgt3.setInt(2, rgt);
		jdbc.SQLupdate(lftRgt3);
		return rgt + 1;
	}

	private void createItemFacetType() throws SQLException {
		// jdbc.SQLupdate("DROP TABLE IF EXISTS item_facet_type_heap");
		// jdbc
		// .SQLupdate("CREATE TABLE item_facet_type_heap LIKE item_facet_heap");
		jdbc
				.SQLupdate("INSERT INTO item_facetntype_heap SELECT * FROM item_facet ");
		jdbc
				.SQLupdate("INSERT INTO item_facetntype_heap SELECT distinct i.record_num, p.facet_id "
						+ "FROM facet f INNER JOIN facet p ON f.parent_facet_id = p.facet_id "
						+ "INNER JOIN item_facet i ON i.facet_id = f.facet_id "
						+ "WHERE p.parent_facet_id = 0");
		jdbc
				.SQLupdate("UPDATE facet f SET n_items = "
						+ "(SELECT COUNT(*) FROM item_facet_type_heap h WHERE h.facet_id = f.facet_id) "
						+ "WHERE f.parent_facet_id = 0");
	}

	private void createRandomItemIDs() throws SQLException {
		jdbc.print("\nCreating random IDs...");
		jdbc.SQLupdate("DROP TABLE IF EXISTS item_order");
		jdbc.SQLupdate("DROP TABLE IF EXISTS item_order_heap");

		String countType = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(record_num) FROM item")); // (getMaxCount
		// ());
		String item_idType = jdbc.unsignedTypeForMaxValue(jdbc
				.SQLqueryInt("SELECT MAX(record_num) FROM item"));

		StringBuffer update = new StringBuffer(
				"INSERT INTO item_order SELECT record_num, 0");
		StringBuffer create = new StringBuffer();
		StringBuffer indexes = new StringBuffer();
		create.append(" (record_num ").append(item_idType);
		create.append(" NOT NULL, random_id ").append(countType);
		ResultSet rs = jdbc
				.SQLquery("SELECT f.facet_id, rgt FROM facet f INNER JOIN rgt USING (facet_id) "
						+ "WHERE parent_facet_id = 0 ORDER BY facet_id");
		while (rs.next()) {
			String name = "col" + rs.getInt(1);

			update.append(", 0");

			create.append(" NOT NULL, ").append(name).append(" ").append(
					countType);
			appendIndex(indexes, name);
		}
		update.append(" FROM item");
		create.append(", PRIMARY KEY (record_num))");
		// + "ENGINE=HEAP DEFAULT CHARSET=ascii PACK_KEYS=1 ROW_FORMAT=FIXED");
		appendIndex(indexes, "random_id");
		// indexes.append(", ADD UNIQUE INDEX random_id USING BTREE (random_id)");
		// Util.print(update.toString());
		// Util.print(create.toString());
		// Util.print(indexes.toString());
		jdbc.SQLupdate("CREATE TABLE item_order" + create.toString());
		jdbc.SQLupdate("CREATE TABLE item_order_heap" + create.toString()
				+ "ENGINE=HEAP");

		jdbc.SQLupdate(update.toString());

		populateRandomItemIDs(rs, item_idType);

		jdbc.SQLupdate(indexes.toString());
		jdbc.SQLupdate("DROP TABLE rgt");
		jdbc.SQLupdate("INSERT INTO item_order_heap "
				+ "SELECT * FROM item_order");
	}

	private void appendIndex(StringBuffer indexes, String name) {
		if (indexes.length() == 0)
			indexes.append("ALTER TABLE item_order_heap ");
		else
			indexes.append(", ");
		indexes.append("ADD UNIQUE INDEX ").append(name).append(
				" USING BTREE (").append(name).append(")");
	}

	// Set all the facet_type columns
	private void populateRandomItemIDs(ResultSet rs, String item_id_column_type)
			throws SQLException {
		jdbc.SQLupdate("DROP TABLE IF EXISTS random_item_ID");

		jdbc.SQLupdate("CREATE TEMPORARY TABLE random_item_ID (record_num "
				+ item_id_column_type + ", random_id " + item_id_column_type
				+ " auto_increment, PRIMARY KEY (record_num),"
				+ " UNIQUE INDEX random_id USING BTREE (random_id))"
				+ " ENGINE=HEAP " // DEFAULT
				// CHARSET=ascii
				// "
				+ "PACK_KEYS=1 ROW_FORMAT=FIXED");

		jdbc.SQLupdate("INSERT INTO random_item_ID"
				+ " SELECT record_num, NULL FROM item ORDER BY RAND()");

		jdbc.SQLupdate("UPDATE item_order io, random_item_id rid "
				+ "SET io.random_id = "
				+ "rid.random_id WHERE io.record_num = rid.record_num");

		rs.beforeFirst();
		int prevRgt = 0;
		while (rs.next()) {
			jdbc.SQLupdate("TRUNCATE TABLE random_item_ID");
			String name = "col" + rs.getInt(1);
			int rgt = rs.getInt(2);
			jdbc
					.SQLupdate("INSERT INTO random_item_ID"
							+ " SELECT record_num, NULL"
							+ " FROM item_facet_heap ifh"
							+ " INNER JOIN facet f on f.facet_id = ifh.facet_id"
							+ " INNER JOIN rgt on f.facet_id = rgt.facet_id"
							+ " WHERE rgt > " + prevRgt + " AND rgt <= " + rgt
							+ " GROUP BY record_num"
							+ " ORDER BY MIN(rgt), record_num");

			jdbc.SQLupdate("UPDATE item_order io, random_item_id rid "
					+ "SET io." + name + " = "
					+ "rid.random_id WHERE io.record_num = rid.record_num");

			jdbc.SQLupdate("INSERT INTO random_item_id "
					+ " SELECT record_num, NULL FROM item_order " + " WHERE "
					+ name + " = 0 ORDER BY record_num");

			jdbc.SQLupdate("UPDATE item_order io, random_item_id rid "
					+ "SET io." + name + " = "
					+ "rid.random_id WHERE io.record_num = rid.record_num");

			prevRgt = rgt;
		}

		jdbc.SQLupdate("DROP TABLE random_item_ID");
	}

	void createUserActions() throws SQLException {
		jdbc.SQLupdate("CREATE TABLE IF NOT EXISTS user_actions ("
				+ "timestamp datetime NOT NULL,"
				+ " location tinyint(3) unsigned NOT NULL,"
				+ " object varchar(255) NOT NULL,"
				+ " modifiers mediumint(9) NOT NULL,"
				+ " session int(11) NOT NULL,"
				+ " client varchar(255) NOT NULL)");
	}

	// private boolean isPowerOf2(int n) {
	// for (int i = 0, pow = 1; i < 30; i++, pow = pow << 1) {
	// if (pow == n)
	// return true;
	// else if (pow > n)
	// return false;
	// }
	// return false;
	// }
	//
	// int checkCanonErrors() throws SQLException {
	// jdbc.print("\nChecking output tables for errors...");
	// int nErrors = 0; // No longer true... checkTempCount();
	// nErrors += findBrokenLinks(false, 8);
	// jdbc.print("...found " + nErrors + " errors in output tables.");
	// return nErrors == 0 ? NO_ERRORS : OUTPUT_ERRORS;
	// }
	//
	// private int checkTempCount() throws SQLException {
	// int nErrors = 0;
	// int nRawFacets = jdbc.SQLqueryInt("SELECT COUNT(*) FROM raw_facet");
	// int nRawFacetTypes = jdbc
	// .SQLqueryInt("SELECT COUNT(DISTINCT facet_type_id) "
	// + "FROM raw_facet_type ft INNER JOIN temp ON temp.facet_id =
	// ft.facet_type_id");
	// int nFacets = jdbc.SQLqueryInt("SELECT COUNT(*) FROM facet");
	// if (nRawFacetTypes + nRawFacets != nFacets) {
	// jdbc.print("\nERROR: nRawFacets + nRawFacetTypes != nFacets: "
	// + nRawFacets + " " + nRawFacetTypes + " " + nFacets);
	// nErrors += 1;
	// }
	// return nErrors;
	// }
	//
	// private int checkInTemp() throws SQLException {
	// return printErrors(
	// "SELECT raw.facet_id, raw.name, raw.parent_facet_id"
	// + " FROM raw_facet raw LEFT JOIN temp ON raw.facet_id = temp.facet_id"
	// + " WHERE temp.facet_id IS NULL",
	// "\nERROR: Not in temp",
	// "raw.facet_id, raw.name, raw.parent_facet_id", 8);
	// }
	//
	// private int checkNoIDs() throws SQLException {
	// return printErrors(
	// "SELECT facet.facet_id, facet.name, facet.parent_facet_id,
	// facet.which_child"
	// + " FROM temp facet LEFT JOIN temp parent ON facet.parent_facet_id =
	// parent.facet_id"
	// + " WHERE (facet.canonical_facet_id IS NULL OR facet.canonical_facet_id =
	// 0)"
	// + " AND parent.children_offset > 0",
	// "\nERROR: no canonical ID",
	// "[facet_id, name, parent_facet_id, which_child", 8);
	// }

	private int printErrors(String query, String message, String fieldList)
			throws SQLException {
		return printErrors(query, message, fieldList, maxErrorsToPrint);
	}

	private int printErrors(String query, String message, String fieldList,
			int maxPrint) throws SQLException {
		ResultSet rs = jdbc.SQLquery(query);
		int nErrors = MyResultSet.nRows(rs);
		// It takes lots of VM for big result sets, so do LIMIT here and COUNT
		// below.
		if (nErrors > 0 && maxPrint > 0) {
			jdbc.print(message + " [" + fieldList + "]:");
			String[] fields = fieldList.split(",");
			try {
				while (rs.next() && rs.getRow() <= maxPrint) {
					StringBuffer buf = new StringBuffer();
					buf.append("[");
					for (int i = 0; i < fields.length; i++) {
						if (i > 0)
							buf.append(", ");
						buf.append(rs.getString(i + 1));
					}
					buf.append("]");
					jdbc.print(buf.toString());
				}
				if (nErrors > maxPrint)
					jdbc.print("... " + (nErrors - maxPrint) + " more");
			} catch (SQLException se) {
				jdbc.print("SQL Exception: " + se.getMessage());
				se.printStackTrace(System.out);
			} finally {
				jdbc.close(rs);
			}
		}
		return nErrors;
	}

	private int checkRawErrors() throws SQLException {
		jdbc.print("\nChecking input tables for errors...");

		// purgeMultiples();
		int maxRawFacetTypes = jdbc
				.SQLqueryInt("SELECT MAX(facet_type_id) FROM raw_facet_type");

		int nErrors = printErrors(
				"SELECT raw.facet_id, raw.parent_facet_id, parent.name"
						+ " FROM raw_facet raw LEFT JOIN raw_facet parent"
						+ " ON raw.parent_facet_id = parent.facet_id"
						+ " WHERE raw.name IS NULL OR raw.name = ''",
				"\nERROR: No name in raw_facet",
				"facet_id, parent_facet_id, parent.name");
		jdbc
				.SQLupdate("REPLACE INTO raw_facet (SELECT raw.facet_id, CONCAT('<unnamed', raw.facet_id, '>'),"
						+ " raw.parent_facet_id, raw.sort FROM raw_facet raw"
						+ " INNER JOIN raw_facet child ON child.parent_facet_id = raw.facet_id"
						+ " WHERE raw.name IS NULL OR raw.name = '')");
		jdbc.SQLupdate("DELETE FROM raw_facet WHERE name IS NULL OR name = ''");

		nErrors += fixDuplicates();

		nErrors += fixMissingItemFacets(maxErrorsToPrint);

		nErrors += findBrokenLinks(true, maxErrorsToPrint);

		nErrors += printErrors(
				"SELECT raw.facet_id, raw.name, raw.parent_facet_id"
						+ " FROM raw_facet raw LEFT JOIN raw_facet parent ON raw.parent_facet_id = parent.facet_id"
						+ " WHERE parent.facet_id IS NULL"
						+ " AND raw.parent_facet_id > " + maxRawFacetTypes,
				"\nERROR: No parent in raw_facet",
				"facet_id, name, parent_facet_id");

		jdbc.print("...found " + nErrors + " errors in raw tables.\n");

		return nErrors;
	}

	public int findBrokenLinks(boolean raw, int maxPrint) throws SQLException {
		String i_f = raw ? "raw_item_facet" : "item_facet_heap";
		String f = raw ? "raw_facet" : "facet";

		int nErrors = deleteUnusedItems(maxPrint, i_f)
				+ deleteUnusedFacets(raw, maxPrint, f, i_f);

		// String q0 = "SELECT i_f.facet_id, i_f.record_num"
		// + " FROM " + i_f
		// + " i_f LEFT JOIN item ON item.record_num = i_f.record_num"
		// + " WHERE item.record_num IS NULL";

		String q0 = "select bar.record_num, group_concat(facet_id)" + "from "
				+ i_f + " inner join" + "(select foo.record_num from"
				+ "(select distinct record_num from " + i_f + ") foo "
				+ "left join item using (record_num)"
				+ "where item.record_num is null) bar using (record_num)"
				+ "group by bar.record_num";

		int noSuchItems = printErrors(q0, "\nERROR: No such item",
				"record_num, facet_id", maxPrint);
		nErrors += noSuchItems;

		// String q = "SELECT i_f.facet_id, i_f.record_num" + " FROM " + i_f
		// + " i_f LEFT JOIN " + f
		// + " facet ON facet.facet_id = i_f.facet_id"
		// + " WHERE facet.facet_id IS NULL";

		String q = "select bar.facet_id, group_concat(record_num)" + "from "
				+ i_f + " inner join" + "(select foo.facet_id from"
				+ "(select distinct facet_id from " + i_f + ") foo "
				+ "left join " + f + " facet using (facet_id)"
				+ "where facet.facet_id is null) bar using (facet_id)"
				+ "group by bar.facet_id";

		int noSuchFacets = printErrors(q, "\nERROR: No such facet",
				"facet_id, record_num", maxPrint);
		nErrors += noSuchFacets;

		if (raw && (noSuchFacets > 0 || noSuchItems > 0)) {

			String q2 = "SELECT i_f.facet_id, i_f.record_num"
					+ " FROM "
					+ i_f
					+ " i_f LEFT JOIN "
					+ f
					+ " facet ON facet.facet_id = i_f.facet_id"
					+ " LEFT JOIN item ON item.record_num = i_f.record_num"
					+ " WHERE facet.facet_id IS NULL OR item.record_num IS NULL";

			jdbc.print("Deleting " + (noSuchFacets + noSuchItems)
					+ " bogus facet/item links from raw_item_facet");
			jdbc.SQLupdate("DELETE raw_item_facet FROM raw_item_facet, (" + q2
					+ ") unused"
					+ " WHERE raw_item_facet.record_num = unused.record_num"
					+ " AND raw_item_facet.facet_id = unused.facet_id");
		}

		return nErrors;
	}

	int deleteUnusedItems(int maxPrint, String i_f) throws SQLException {

		jdbc.SQLupdate("DROP TABLE IF EXISTS usedItems");
		jdbc.SQLupdate("CREATE TABLE usedItems ("
				+ " record_num int unsigned NOT NULL default 0,"
				+ " PRIMARY KEY (record_num)"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
		jdbc.SQLupdate("INSERT INTO usedItems"
				+ " SELECT DISTINCT record_num FROM " + i_f);

		jdbc.SQLupdate("DROP TABLE IF EXISTS unusedItems");
		jdbc.SQLupdate("CREATE TABLE unusedItems ("
				+ " record_num int unsigned NOT NULL default 0,"
				+ " description TEXT," + " PRIMARY KEY (record_num)"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
		String descField = jdbc
				.SQLqueryString("SELECT SUBSTRING_INDEX(itemDescriptionFields, ',', 1) FROM globals");
		jdbc
				.SQLupdate("INSERT INTO unusedItems"
						+ "(SELECT item.record_num, "
						+ descField
						+ " FROM item LEFT JOIN usedItems used ON item.record_num = used.record_num"
						+ " WHERE used.record_num IS NULL)");

		int nErrors = printErrors("SELECT * FROM unusedItems",
				"\nERROR: Item is not associated with any facets",
				"record_num, " + descField, maxPrint);
		assert nErrors == 0 : i_f + " "
				+ jdbc.SQLqueryInt("Select count(*) from unusedItems");

		if (nErrors > 0) {
			jdbc.print("Deleting " + nErrors + " bogus items with no facets");
			jdbc
					.SQLupdate("DELETE FROM item WHERE record_num IN (SELECT record_num FROM unusedItems)");
		}

		jdbc.SQLupdate("DROP TABLE usedItems");
		jdbc.SQLupdate("DROP TABLE unusedItems");
		return nErrors;
	}

	int deleteUnusedFacets(boolean raw, int maxPrint, String f, String i_f)
			throws SQLException {
		jdbc.SQLupdate("DROP TABLE IF EXISTS usedFacets");
		jdbc.SQLupdate("CREATE TABLE usedFacets ("
				+ " facet_id int unsigned NOT NULL default 0,"
				+ " PRIMARY KEY (facet_id)"
				+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8");
		jdbc.SQLupdate("INSERT INTO usedFacets"
				+ " SELECT DISTINCT facet_id FROM " + i_f);
		if (!raw)
			jdbc.SQLupdate("INSERT INTO usedFacets"
					+ " SELECT facet_id FROM facet WHERE parent_facet_id = 0");

		jdbc.SQLupdate("DROP TABLE IF EXISTS unusedFacets");

		jdbc
				.SQLupdate("CREATE TABLE unusedFacets AS"
						+ " SELECT parent.name parent_name, facet.name, facet.facet_id"
						+ " FROM "
						+ (raw ? "raw_facet facet left join raw_facet parent"
								: "facet left join facet parent")
						+ " on facet.parent_facet_id = parent.facet_id"
						+ " LEFT JOIN usedFacets used ON facet.facet_id = used.facet_id"
						+ " WHERE used.facet_id IS NULL");

		int unusedFacets = printErrors(
				"SELECT parent_name, CONCAT('[', COUNT(*), ' facets: ', GROUP_CONCAT(name), '] [', GROUP_CONCAT(facet_id), ']') FROM unusedFacets"
						+ " GROUP BY parent_name",
				"\nERROR: Facet is not associated with any items",
				"parent, facet names. facet ids", maxPrint);
		if (raw && unusedFacets > 0) {
			unusedFacets = jdbc
					.SQLqueryInt("SELECT COUNT(*) FROM unusedFacets");
			jdbc.print("Deleting " + unusedFacets
					+ " unused facets from raw_facet");
			jdbc.SQLupdate("DELETE raw_facet r1 FROM " + f
					+ " r1, unusedFacets bar WHERE r1.facet_id = bar.facet_id");
		}
		jdbc.SQLupdate("DROP TABLE IF EXISTS unusedFacets");
		jdbc.SQLupdate("DROP TABLE usedFacets");
		return unusedFacets;
	}

	int fixMissingItemFacets(int maxPrint) throws SQLException {
		jdbc.SQLupdate("DROP TABLE IF EXISTS missingFacets");

		jdbc
				.SQLupdate("CREATE TABLE missingFacets AS"
						+ " SELECT raw.name, raw.facet_id, raw_parent.name parent_name, raw.parent_facet_id, child.record_num"
						+ " FROM ((raw_facet raw INNER JOIN raw_item_facet child ON raw.facet_id = child.facet_id)"
						+ " INNER JOIN raw_facet raw_parent ON raw.parent_facet_id = raw_parent.facet_id)"
						+ " LEFT JOIN raw_item_facet parent ON"
						+ " (parent.facet_id = raw.parent_facet_id AND parent.record_num = child.record_num)"
						+ " WHERE (parent.facet_id IS NULL OR parent.facet_id = 0)");

		int nErrors = printErrors(
				"SELECT name, facet_id, parent_name, parent_facet_id, CONCAT('[', COUNT(*), ' records: ', "
						+ "GROUP_CONCAT(record_num), ']') FROM missingFacets"
						+ " GROUP BY facet_id",
				"\nERROR: Item has facet child, but not its parent",
				"name, facet_id, parent_name, parent_facet_id, record_nums",
				maxPrint);
		if (nErrors > 0) {
			jdbc.print("Adding missing facets...");
			jdbc
					.SQLupdate("INSERT INTO raw_item_facet"
							+ "	SELECT DISTINCT record_num, parent_facet_id FROM missingFacets");

			nErrors += fixMissingItemFacets(maxPrint);
		}
		jdbc.SQLupdate("DROP TABLE IF EXISTS missingFacets");
		return nErrors;
	}

	int fixDuplicates() throws SQLException {
		jdbc.ensureIndex("raw_facet", "name", "name");
		jdbc.ensureIndex("raw_item_facet", "facet", "facet_id");

		jdbc.SQLupdate("DROP TABLE IF EXISTS duplicates");

		jdbc
				.SQLupdate("CREATE TEMPORARY TABLE duplicates AS"
						+ " SELECT old.facet_id AS old_id, MAX(new.facet_id) AS new_id,"
						+ " MAX(old.name) AS old_name, MAX(old.parent_facet_id) AS parent"
						+ " FROM raw_facet old, raw_facet new"
						+ " WHERE old.name = new.name"
						+ " AND old.parent_facet_id = new.parent_facet_id"
						+ " AND old.facet_id < new.facet_id"
						+ " GROUP BY old.facet_id");

		int nErrors = printErrors("SELECT * FROM duplicates",
				"\nERROR: duplicate facets",
				"copy1, copy2, name, parent_facet_id");
		if (nErrors > 0) {
			jdbc.print("Merging copy1s into copy2s...");
			jdbc
					.SQLupdate("REPLACE INTO raw_item_facet (SELECT record_num, dup.new_id FROM"
							+ " raw_item_facet raw INNER JOIN duplicates dup ON raw.facet_id = dup.old_id)");
			jdbc
					.SQLupdate("DELETE FROM raw_item_facet USING raw_item_facet, duplicates dup"
							+ "	WHERE facet_id = dup.old_id");

			jdbc.SQLupdate("UPDATE raw_facet facet, duplicates dup"
					+ " SET parent_facet_id = dup.new_id"
					+ " WHERE parent_facet_id = dup.old_id");

			jdbc
					.SQLupdate("DELETE FROM raw_facet USING raw_facet, duplicates dup"
							+ " WHERE facet_id = dup.old_id");
			nErrors += fixDuplicates();
		}

		jdbc.SQLupdate("DROP TABLE IF EXISTS duplicates");

		return nErrors;
	}

	private int setUpFacetSearch() throws SQLException {
		jdbc.print("\nSetting up facet search...");
		int nErrors = 0;
		String itemDescs = jdbc
				.SQLqueryString("SELECT itemDescriptionFields FROM globals");
		if (itemDescs != null) {
			String[] itemDescFields = itemDescs.split(",");
			for (int i = 0; i < itemDescFields.length; i++) {
				String field = itemDescFields[i];
				// CONCAT_WS ignores nulls, so replace empty strings with NULLs.
				jdbc.SQLupdate("UPDATE item SET " + field
						+ " = NULL WHERE LENGTH(TRIM(" + field + ")) = 0");

				// boolean OK = true;
				// boolean indexExists = false;
				// ResultSet rs = jdbc
				// .SQLquery("SHOW INDEX FROM item WHERE key_name = 'search'");
				// int n = 0;
				// while (OK && rs.next()) {
				// indexExists = true;
				// String col = rs.getString("column_name");
				// if (Util.isMember(itemDescFields, col)
				// || col.equals("facet_names"))
				// n++;
				// else
				// OK = false;
				// }
				// if (!OK || (n != itemDescFields.length)) {
				// if (indexExists)
				// jdbc.SQLupdate("ALTER TABLE item DROP INDEX search");
				// jdbc
				// .SQLupdate("ALTER TABLE item ADD FULLTEXT
				// search(facet_names,"
				// + itemDescs + ")");
				// }
			}

			// Before you create the index, make sure that MODIFY
			// facet_names TEXT NOT NULL
			jdbc.ensureIndex("item", "search", "facet_names," + itemDescs,
					"FULLTEXT");

			// Set up facet search
			jdbc.SQLupdate("DROP TABLE IF EXISTS item_temp");
			jdbc.SQLupdate("CREATE TABLE item_temp ("
					+ " record_num INT NOT NULL DEFAULT 0,"
					+ " facet_names TEXT," + " PRIMARY KEY (record_num))");
			jdbc
					.SQLupdate("INSERT INTO item_temp"
							+ " SELECT it.record_num, GROUP_CONCAT(DISTINCT f.name)"
							+ " FROM item_facet it use index (item) INNER JOIN facet f ON it.facet_id = f.facet_id"
							// + " WHERE name != 'No Categorization' AND name !=
							// 'No
							// Further Categorization'"
							+ " GROUP BY it.record_num"); // ORDER BY NULL");
			jdbc.SQLupdate("ALTER TABLE item DISABLE KEYS");
			jdbc.SQLupdate("UPDATE item i, item_temp it"
					+ " SET i.facet_names = it.facet_names"
					+ " WHERE i.record_num = it.record_num");
			jdbc.SQLupdate("ALTER TABLE item ENABLE KEYS");
			jdbc.SQLupdate("DROP TABLE item_temp");
		}
		return nErrors;
	}

	private static void createCorrelationsTable(JDBCSample jdbc1,
			String facet_idType) throws SQLException {
		jdbc1.SQLupdate(" DROP TABLE IF EXISTS correlations; ");
		jdbc1.SQLupdate("CREATE TABLE correlations ( " + "  facet1 "
				+ facet_idType + " NOT NULL, " + "  facet2 " + facet_idType
				+ " NOT NULL, " + "  correlation FLOAT NOT NULL, "
				+ "KEY facet1 (facet1), KEY facet2 (facet2))");
	}

	public static int maxFacetTypeID(JDBCSample jdbc) throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT MAX(facet_id) FROM facet WHERE parent_facet_id = 0");
	}

	public static void ensureDBinitted(JDBCSample jdbc) throws SQLException {

		// TEMPORARY until DBs updated
		// try {
		// jdbc
		// .SQLupdate("create table if not exists item_facetNtype_heap like item_facet_heap;");
		// jdbc.SQLupdate("DROP TABLE IF EXISTS item_facet_heap");
		// jdbc.SQLupdate("DROP TABLE IF EXISTS item_facet_type_heap");
		// } catch (Exception e) {
		// // Skip if DB already updated
		// }

		if (jdbc.SQLqueryInt("SELECT COUNT(*) FROM item_order_heap") == 0) {
			// long st=new Date().getTime();
			// jdbc.SQLquery("select count(*) from (SELECT * FROM item_facet) foo");
			// jdbc.print("q "+(new Date().getTime()-st));
			// jdbc.SQLupdate("ALTER TABLE item_facet_heap DISABLE KEYS");
			jdbc.SQLupdate("TRUNCATE TABLE item_facetNtype_heap");
			// jdbc.print("disable "+(new Date().getTime()-st));
			jdbc
					.SQLupdate("INSERT INTO item_facetNtype_heap SELECT * FROM item_facet;");
			// jdbc.print("insert "+(new Date().getTime()-st));
			// jdbc.SQLupdate("ALTER TABLE item_facet_heap ENABLE KEYS");
			// jdbc.print("enable "+(new Date().getTime()-st));
			// jdbc.SQLupdate("TRUNCATE TABLE item_facet_type_heap;");
			jdbc.SQLupdate("INSERT INTO item_facetNtype_heap "
					+ "SELECT distinct i.record_num, f.parent_facet_id "
					+ "FROM facet f "
					+ "INNER JOIN item_facet i ON i.facet_id = f.facet_id "
					+ "WHERE f.parent_facet_id <= " + maxFacetTypeID(jdbc));
			int nTypes = jdbc
					.SQLqueryInt("SELECT MAX(facet_id) FROM facet WHERE parent_facet_id = 0");

			jdbc.SQLupdate("CREATE OR REPLACE VIEW item_facet_heap AS"
					+ " SELECT * FROM item_facetNtype_heap WHERE facet_id > "
					+ nTypes);
			jdbc.SQLupdate("CREATE OR REPLACE VIEW item_facet_type_heap AS"
					+ " SELECT * FROM item_facetNtype_heap WHERE facet_id <= "
					+ nTypes);
			jdbc.SQLupdate("TRUNCATE TABLE item_order_heap;");
			jdbc.SQLupdate("INSERT INTO item_order_heap "
					+ "SELECT * FROM item_order");
			// updateUsageCounts();
		}
	}

	// Called on demand by topCandidates.
	public static void populateCorrelations(JDBCSample jdbc1)
			throws SQLException {

		// Temporary until DBs updated. user must be root.
		// try {
		// jdbc1.SQLqueryInt("SELECT COUNT(*) FROM correlations");
		// } catch (SQLException e) {
		// createCorrelationsTable(jdbc1, jdbc1.unsignedTypeForMaxValue(Math
		// .max(MIN_FACETS, jdbc1
		// .SQLqueryInt("SELECT MAX(facet_id) FROM facet"))));
		// }

		if (jdbc1.SQLqueryInt("SELECT COUNT(*) FROM correlations") == 0) {
			jdbc1.print("Finding pair correlations...");
			jdbc1.SQLupdate("DROP FUNCTION IF EXISTS correlation");
			jdbc1
					.SQLupdate("CREATE FUNCTION correlation(n1 INT, n2 INT, n12 INT, total INT) "
							+ " RETURNS FLOAT DETERMINISTIC NO SQL "
							+ "BEGIN "
							+ " DECLARE p1 FLOAT DEFAULT n1/total; "
							+ " DECLARE p2 FLOAT DEFAULT n2/total; "
							+ " DECLARE p12 FLOAT DEFAULT n12/total; "
							+ " DECLARE num FLOAT DEFAULT p12 - p1*p2; "
							+ " RETURN IF(num=0, 0, num/SQRT((p1-p1*p1)*(p2-p2*p2))); "
							+ "END");
			ensureDBinitted(jdbc1);
			String[][] tabless = { { "item_facet_heap", "item_facet_heap" },
					{ "item_facet_type_heap", "item_facet_heap" },
					{ "item_facet_type_heap", "item_facet_type_heap" } };
			for (int i = 0; i < tabless.length; i++) {
				String[] tables = tabless[i];
				String table1 = tables[0];
				String table2 = tables[1];
				jdbc1
						.SQLupdate("INSERT INTO correlations "
								+ "SELECT i1.facet_id, i2.facet_id, correlation(f1.n_items, f2.n_items, "
								+ " COUNT(*), (SELECT COUNT(*) FROM item)) correlation FROM "
								+ table1
								+ " i1 INNER JOIN "
								+ table2
								+ " i2 USING (record_num) "
								+ "INNER JOIN facet f1 ON f1.facet_id=i1.facet_id "
								+ "INNER JOIN facet f2 ON f2.facet_id=i2.facet_id "
								+ "WHERE i1.facet_id < i2.facet_id "
								+ "GROUP BY i1.facet_id, i2.facet_id "
								+ "HAVING correlation != 0");
			}
			jdbc1.SQLupdate("DROP FUNCTION correlation");
		}
	}

	// Should get rid of pairs and use correlations instead
	private void createPairsTable(String facet_idType, String item_idType)
			throws SQLException {
		jdbc.SQLupdate(" DROP TABLE IF EXISTS pairs; ");
		jdbc.SQLupdate("CREATE TABLE pairs ( " + "  facet1 " + facet_idType
				+ " NOT NULL, " + "  facet2 " + facet_idType + " NOT NULL, "
				+ "  cnt " + item_idType + " NOT NULL, "
				// + " spMutInf FLOAT NOT NULL DEFAULT '0', "
				+ "  p FLOAT NOT NULL, "
				// + " spMutInfRank INT(10) UNSIGNED NOT NULL DEFAULT '0', "
				// + " chSqRank INT(10) UNSIGNED NOT NULL DEFAULT '0' "
				// + " INDEX facet1 (facet1), "
				+ "  PRIMARY KEY (facet1, facet2)" + ")");
	}

	// Called on demand by cluster.
	public static void populatePairs(JDBCSample jdbc1) throws SQLException {
		if (jdbc1.SQLqueryInt("SELECT COUNT(*) FROM pairs") == 0) {
			jdbc1.print("Finding pairs with high Chi Squared...");
			jdbc1.SQLupdate("DROP PROCEDURE IF EXISTS mutInf");
			jdbc1
					.SQLupdate("CREATE PROCEDURE mutInf() "
							+ "BEGIN "
							+ " DECLARE done INT DEFAULT 0; "
							+ " DECLARE f1, n1 INT DEFAULT 0; "
							+ " DECLARE curf1 CURSOR FOR SELECT facet_id, n_items FROM facet WHERE n_items > 1;"
							+ " DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1; "
							+

							// " SELECT COUNT(*) INTO nItems FROM item; "
							// +

							" OPEN curf1; "
							+

							" REPEAT "
							+ "  FETCH curf1 INTO f1, n1; "
							+ "  IF NOT done THEN "
							+ "   INSERT INTO pairs SELECT f1, f2.facet_id, COUNT(*) cnt, 0 "
							+ "   FROM facet f2 INNER JOIN item_facet i2 ON f2.facet_id = i2.facet_id "
							+ "INNER JOIN item_facet i1 ON i1.record_num = i2.record_num AND i1.facet_id = f1 "
							+ "LEFT JOIN ancestor a ON f2.facet_id = a.facet_id AND f1 = a.ancestor_id "
							+ "          WHERE f2.facet_id > f1 AND a.facet_id IS NULL AND f2.n_items > 1 "
							+ "GROUP BY f2.facet_id, f2.n_items "
							+ "HAVING cnt > 1 AND 10*cnt>n1 AND 10*cnt>f2.n_items; "
							+

							" END IF; " + " UNTIL done END REPEAT; "
							+ " CLOSE curf1; " + "END ");

			jdbc1.SQLupdate("CALL mutInf()");
			jdbc1.SQLupdate("DROP PROCEDURE mutInf");

			PreparedStatement setChiSquared = jdbc1
					.lookupPS("UPDATE pairs SET p = ? WHERE facet1 = ? AND facet2 = ?");
			int nItems = jdbc1.SQLqueryInt("SELECT COUNT(*) FROM item");
			int[][] table = new int[2][2];
			ResultSet rs = jdbc1
					.SQLquery("SELECT facet1, facet2, cnt, f1.n_items, f2.n_items "
							+ "FROM pairs INNER JOIN facet f1 on f1.facet_id = facet1 "
							+ "INNER JOIN facet f2 ON f2.facet_id = facet2");
			while (rs.next()) {
				int facet1 = rs.getInt(1);
				int facet2 = rs.getInt(2);
				table[1][1] = rs.getInt(3);
				int x1 = rs.getInt(4);
				int y1 = rs.getInt(5);
				// int x0 = nItems - x1;
				// int y0 = nItems - y1;
				table[0][1] = y1 - table[1][1];
				table[1][0] = x1 - table[1][1];
				table[0][0] = nItems - table[0][1] - table[1][0] - table[1][1];
				if (table[0][0] < 0) {
					Util.print(facet1 + " " + facet2 + " " + x1 + " " + y1
							+ " " + table[1][1] + " " + nItems);
				}
				float p = (float) ChiSqr.pValue(table);
				// if (table[1][1] > 20)
				// jdbc.print(facet1 + " " + facet2 + " " + x1 + " " + y1 + " "
				// +
				// table[1][1] + " " + p);
				setChiSquared.setFloat(1, p);
				setChiSquared.setInt(2, facet1);
				setChiSquared.setInt(3, facet2);
				jdbc1.SQLupdate(setChiSquared);
			}
		}
	}

	// private void loadImages(String directory) throws SQLException {
	// jdbc
	// .SQLupdate("INSERT INTO images (SELECT record_num, LOAD_FILE(CONCAT('"
	// + directory + "', filename)) FROM item)");
	// }

	@SuppressWarnings("unused")
	private void addImageSizes() throws SQLException, IOException {
		String[] tables = { "images" };
		for (int i = 0; i < tables.length; i++) {
			boolean done = false;
			jdbc.print(jdbc.SQLqueryInt("SELECT COUNT(*) FROM " + tables[i]
					+ " WHERE w <= 0")
					+ " unsized " + tables[i] + " remain.");

			PreparedStatement updateImages = jdbc.lookupPS("UPDATE "
					+ tables[i] + " SET w = ?, h = ? WHERE record_num = ?");

			while (!done) {
				ResultSet rs = jdbc.SQLquery("SELECT record_num, image FROM "
						+ tables[i] + " WHERE w <= 0 LIMIT 100");
				// It runs out of memory if you try to retrieve too many images.
				done = true;
				// int record = 0;
				while (rs.next()) {
					done = false;
					InputStream blobStream = rs.getBlob(2).getBinaryStream();
					BufferedImage image = ImageIO.read(blobStream);
					int item = rs.getInt(1);
					int w = image.getWidth();
					int h = image.getHeight();
					assert w > 0 && h > 0 : item + " " + w + "x" + h;
					// System.out.println(tables[i] + " " + ++record + ". " + w
					// + "x" + h);

					updateImages.setInt(1, w);
					updateImages.setInt(2, h);
					updateImages.setInt(3, item);
					jdbc.SQLupdate(updateImages);
				}
			}
		}
	}

}
