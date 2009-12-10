package edu.cmu.cs.bungee.dbScripts;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.helpers.DefaultHandler;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Populate database from OAI format XML files.
 * 
 */
final class Populate extends DefaultHandler {

	/**
	 * Parser can park state information here for the benefit of
	 * FacetValueParser
	 */
	Object parserState;
	private ConvertFromRaw converter;
	Database db;

	// private String dbName;

	Map<String, String> moves;

	private Map<String, String> renames;

	/**
	 * Places within states that help recognize place names in the subject
	 * hierarchy. Applies to the pattern "<city> ([<place>,] <state abbrev>)"
	 */
	Collection<String> cities;

	Populate(JDBCSample _jdbc, boolean _verbose) {
		db = new Database(_jdbc, _verbose);
		// dbName = db;
		// verbose = _verbose;

		// parseTGM();
		// parse043codes();
		// checkMultipleParents("places_hierarchy");
		// checkMultipleParents("TGM");

	}

	void setRenames(Collection<String[]> _renames) {
		renames = stringPair2Map(_renames, false);
	}

	void setMoves(Collection<String[]> _moves) {
		moves = stringPair2Map(_moves, true);
	}

	void setPlaces(Collection<String> _places) {
		cities = _places;
	}

	Map<String, String> stringPair2Map(Collection<String[]> pairs,
			boolean multipleValuesOK) {
		Map<String, String> result = null;
		if (pairs != null) {
			result = new LinkedHashMap<String, String>();
			for (String[] entry : pairs) {
				assert entry.length == 2 : "Bad entry: "
						+ Util.valueOfDeep(entry);
				String key = entry[0];
				String value = entry[1];
				String old = result.get(key);
				if (old != null) {
					if (multipleValuesOK) {
						value += ";" + old;
					} else {
						Util.err("Multiple entries for " + key + ": " + old
								+ " " + value);
					}
				}
				result.put(key, value);
			}
		}
		return result;
	}

	boolean verbose() {
		return db.verbose;
	}

	ConvertFromRaw convertFromRaw() {
		if (converter == null)
			converter = new ConvertFromRaw(db.getJdbc());
		return converter;
	}

	void compile() throws SQLException {
		convertFromRaw().convert();
	}

	void clearTables() {
		String[] tables = { "raw_facet", "raw_item_facet", "item",
		/* "places_hierarchy" */};
		for (int i = 0; i < tables.length; i++) {
			db.db("TRUNCATE TABLE " + tables[i]);
		}
		// for (int i = 0; i < attrTables.length; i++) {
		// db("TRUNCATE TABLE " + attrTables[i]);
		// }
	}

	void renameNmove() throws SQLException {
		if (renames != null)
			rename();
		// useTGMinternal("loc.tgm", facetType("Subject"));
		// useTGMinternal("places_hierarchy",
		// facetType("Location of Publication"));
		// useTGMinternal("loc.places_hierarchy", facetType("Places"));
		// rename();

		if (moves != null)
			move(5);
		if (renames != null || moves != null) {
			convertFromRaw().fixMissingItemFacets(0);
			// fixFacetTypes();
			convertFromRaw().fixDuplicates();
			convertFromRaw().fixMissingItemFacets(0);
			convertFromRaw().findBrokenLinks(true, 0);

			// if (moves != null)
			// promoteSingletons();
			// convertFromRaw().findBrokenLinks(true, 0);
		}
	}

	/**
	 * Rename facets according to the explicit list 'rename'. This can correct
	 * common spelling errors, for instance. Looks at all facets, irrespective
	 * of parent.
	 * 
	 * @throws SQLException
	 */
	private void rename() throws SQLException {
		for (Map.Entry<String, String> entry : renames.entrySet()) {
			String from = db.truncateName(entry.getKey(), false);
			String to = db.truncateName(entry.getValue(), false);

			// Check for a sibling already named to
			PreparedStatement ps = db
					.getJdbc()
					.lookupPS(
							"SELECT old.facet_id, new.facet_id"
									+ " FROM raw_facet old LEFT JOIN raw_facet new"
									+ " ON old.parent_facet_id = new.parent_facet_id AND new.name=?"
									+ " WHERE old.name = ?");
			ps.setString(1, to);
			ps.setString(2, from);
			ResultSet rs = db.getJdbc().SQLquery(ps);
			while (rs.next()) {
				int fromID = rs.getInt(1);
				int toID = rs.getInt(2);
				if (toID > 0) {
					db.merge(fromID, toID);
				} else {
					db.setName(fromID, from);
				}
			}
			rs.close();
		}
	}

	/**
	 * @param s
	 *            source facet name
	 * @return possibly renamed facet name
	 */
	String rename(String s) {
		// Util.print("rename " + renames);
		String result = renames == null ? null : renames.get(s);
		if (result == null)
			result = s;
		return result;
	}

	/**
	 * Change the facet hierarchy by moving the subtrees explicitly listed in
	 * moves. All children of the old lineage are reparented to the new lineage,
	 * which is created if nececssary. The now-childless old parent is then
	 * deleted.
	 * 
	 * @throws SQLException
	 */
	private void move(int maxIterations) throws SQLException {
		convertFromRaw().fixDuplicates();
		boolean change = false;
		if (maxIterations > 0) {
			Util.print("\n moving ...");

			// results maps from the parentName to a set of moved children
			// and a set of unmoved children
			Map<String, List<String>[]> results = new Hashtable<String, List<String>[]>();
			Map.Entry<String, String> entry = null;
			for (Iterator<Entry<String, String>> it = moves.entrySet()
					.iterator(); it.hasNext();) {
				try {
					entry = it.next();
					String oldName = entry.getKey();
					String[] newNames = Util.splitSemicolon(entry.getValue());
					for (int k = 0; k < newNames.length; k++) {
						String newName = newNames[k];
						String[] olds = oldName.split(" -- ");
						String[] news = newName.split(" -- ");
						int parentFacet = db.getFacetType(olds[0], true);
						// assert parentFacet > 0 : "Can't find '" + olds[0]
						// + "' in raw_facet_type";

						for (int j = 1; j < olds.length && parentFacet > 0
								&& !"*".equals(olds[j]); j++)
							parentFacet = db.lookupFacet(olds[j], parentFacet);
						boolean ismoved = parentFacet > 0;
						if (ismoved) {
							String[] leafs = { olds[olds.length - 1] };
							if ("*".equals(olds[olds.length - 1])) {
								assert false : "No need for '*': " + oldName
										+ " " + newName;
								// leafs = db.getChildIDs(parentFacet);
							}
							for (int i = 0; i < leafs.length; i++) {
								olds[olds.length - 1] = leafs[i];
								logmove(Arrays.asList(olds), ismoved, results);
								int type = db.getFacetType(news[0], false);
								int newParent = type;
								for (int j = 1; j < news.length; j++) {
									int childType = db.lookupFacet(news[j],
											newParent);
									if (childType < 0) {
										childType = db.newFacet(news[j],
												newParent);
									}
									newParent = childType;
								}
								assert newParent > 0 : newName;
								assert newParent < 1000000 : newParent;
								assert parentFacet > 0 : oldName;
								assert parentFacet < 1000000 : oldName;
								// moveInternal(parentFacet, newParent);
								db.merge(parentFacet, newParent);
								change = true;
							}
						} else {
							// log("move: Can't find " + oldName + " to
							// move");
						}
					}
				} catch (Throwable e) {
					Util.err("Error moving " + Util.valueOfDeep(entry) + ": ");
					e.printStackTrace();
				}
			}
			printmoveLog(results);
			Util.print("... moving done");
		}
		if (change)
			move(maxIterations - 1);
	}

	private void logmove(List<String> olds, boolean ismoved,
			Map<String, List<String>[]> results) {
		if (verbose()) {
			List<String> names = new LinkedList<String>(olds);
			// add a dummy child so it can go in either the good or bad list
			names.add("leaf");
			// Util.print(names);
			logmoveInternal(names, names.size(), ismoved, results);
		}
	}

	@SuppressWarnings("unchecked")
	private void logmoveInternal(List<String> olds, int generation,
			boolean ismoved, Map<String, List<String>[]> results) {
		if (generation > 0) {
			String oldName = Util.join(olds.subList(0, generation).toArray(),
					" -- ");
			String parentName = generation > 1 ? Util.join(olds.subList(0,
					generation - 1).toArray(), " -- ") : "";
			List<String>[] prevResults = results.get(parentName);
			if (prevResults == null) {
				prevResults = (List<String>[]) new List<?>[2];
				prevResults[0] = new LinkedList<String>();
				prevResults[1] = new LinkedList<String>();
				results.put(parentName, prevResults);
			}
			List<String> prev = prevResults[ismoved ? 0 : 1];
			if (!prev.contains(oldName))
				prev.add(oldName);
			logmoveInternal(olds, generation - 1, ismoved, results);
			// if (ismoved)
			// Util.print("move record for " + parentName + " "
			// + Util.valueOfDeep(prevResults));
		}
	}

	private void printmoveLog(Map<String, List<String>[]> promotions) {
		printmoveLogInternal("", promotions);
	}

	private void printmoveLogInternal(String name,
			Map<String, List<String>[]> promotions) {
		List<String>[] results = promotions.get(name);
		if (results != null) {
			// "leaf" record won't exist
			List<String> bads = results[1];
			List<String> goods = results[0];
			bads.removeAll(goods);
			if (bads.size() > 0 && name.length() > 0) {
				db.log("... failed to find " + bads.size() + " children of "
						+ name + ", including " + bads.get(0));
			}
			// if (goods.size() > 0) {
			// log("... found " + goods.size() + " children of " + name
			// + ", including " + goods.get(0));
			// }
			for (String goodName : goods) {
				printmoveLogInternal(goodName, promotions);
			}
		}
	}

	private String[] renumberTables = { "images", "item", "raw_item_facet" };

	/**
	 * Re-assigns record numbers to items so that they are consecutive starting
	 * at 1.
	 * 
	 * @throws SQLException
	 */
	void renumber() throws SQLException {
		Util.print("Renumbering items...");
		String[] tables = renumberTables; // Util.append(attrTables,
		// renumberTables);

		PreparedStatement getMinID = db.getJdbc().lookupPS(
				"SELECT MIN(record_num) FROM item WHERE record_num >= ?");
		getMinID.setInt(1, 0);
		int first = db.getJdbc().SQLqueryInt(getMinID);
		for (int i = 1; i < first; i++) {
			getMinID.setInt(1, i);
			int old = db.getJdbc().SQLqueryInt(getMinID);
			if (old > i) {
				for (int j = 0; j < tables.length; j++) {
					PreparedStatement renumber = db
							.getJdbc()
							.lookupPS(
									"UPDATE "
											+ tables[j]
											+ " SET record_num = ? WHERE record_num = ?");
					renumber.setInt(1, i);
					renumber.setInt(2, old);
					db.db(renumber, "Renumber");
				}
			} else
				break;
		}
		Util.print("...renumbering done");
	}

	/**
	 * For every combination of
	 * 
	 * grandparent -- parent -- singleton
	 * 
	 * where parent has no other children and every item that is a parent is
	 * also a singleton, delete parent and make singleton a direct child of
	 * grandparent.
	 * 
	 * @throws SQLException
	 */
	void promoteSingletons() throws SQLException {
		convertFromRaw().fixMissingItemFacets(0);
		ResultSet rs = db
				.getJdbc()
				.SQLquery(
						"SELECT p.facet_id, p.parent_facet_id, f.facet_id FROM raw_facet f "
								+ "INNER JOIN raw_facet p ON f.parent_facet_id = p.facet_id "
								+ "GROUP BY p.facet_id HAVING COUNT(*) = 1");
		PreparedStatement ps = db
				.lookupPS("UPDATE raw_facet f, raw_facet p SET f.sort ="
						+ " p.sort WHERE f.facet_id = ? AND p.facet_id = f.parent_facet_id");
		boolean change = false;
		while (rs.next()) {
			int singleton = rs.getInt(3);
			int parent = rs.getInt(1);
			int grandparent = rs.getInt(2);
			if (db.getFacetName(singleton) != null
					&& db.getName(grandparent) != null
					&& db.nItems(singleton) == db.nItems(parent)) {
				// Name check ensures previous promotions haven't deleted any of
				// the facets involved.
				change = true;
				db.log("Promoting singleton " + db.getFacetName(singleton)
						+ " from " + db.getFacetName(parent) + " to "
						+ db.getName(grandparent));
				ps.setInt(1, singleton);
				db.db(ps, "Update sort");
				db.setParent(singleton, grandparent);
				// Util.print("pp "
				// + db.getName(singleton)
				// + " "
				// + db.getName(parent)
				// + " "
				// + db.getJdbc().SQLqueryInt(
				// "SELECT sort FROM raw_facet where facet_id = "
				// + singleton)
				// + " "
				// + db.getJdbc().SQLqueryInt(
				// "SELECT sort FROM raw_facet where facet_id = "
				// + parent));
				db.deleteFacet(parent);
			} else {
				// log("Can't promote singleton "
				// + Util.join(ancestors(singleton, false)) + "\n"
				// + nItems(singleton) + " " + nItems(parent));
			}
		}
		if (change) {
			convertFromRaw().fixDuplicates();
			promoteSingletons();
		} else
			promoteSingletonCategories();
	}

	private void promoteSingletonCategories() throws SQLException {

		ResultSet rs = db
				.getJdbc()
				.SQLquery(
						"SELECT facet_id, facet_type_id FROM raw_facet f INNER JOIN raw_facet_type ft ON f.parent_facet_id = ft.facet_type_id"
								+ " GROUP BY facet_type_id HAVING COUNT(*) = 1");
		boolean change = false;
		while (rs.next()) {
			int singleton = rs.getInt(1);
			if (db.nItems(singleton) == db
					.getJdbc()
					.SQLqueryInt(
							"SELECT COUNT(DISTINCT record_num) FROM raw_facet f "
									+ "INNER JOIN raw_item_facet i ON f.facet_id = i.facet_id "
									+ "WHERE f.parent_facet_id = " + singleton)) {
				change = true;
				db.log("Promoting singleton "
						+ db.ancestorString(singleton, ", "));
				int type = rs.getInt(2);
				db.setChildrensParent(singleton, type);
				db.deleteFacet(singleton);
			}
		}
		if (change) {
			convertFromRaw().fixDuplicates();
			promoteSingletons();
		}
	}

	enum DuplicateStatus {
		SAME, DIFFERENT, NA
	}

	void mergeDuplicateItems(String facetTypeNamesToMerge) throws SQLException {
		String[] toMerge = facetTypeNamesToMerge.split(",");
		int[] facetTypesToMerge = new int[toMerge.length];
		for (int i = 0; i < toMerge.length; i++) {
			facetTypesToMerge[i] = db.getFacetType(toMerge[i], false);
		}
		db.getJdbc().ensureIndex("item", "URI", "URI(51)");
		ResultSet pairs = db.getJdbc().SQLquery(
				"SELECT i1.record_num, i2.record_num, i1.URI FROM item i1, item i2 "
						+ "where i1.record_num < i2.record_num "
						+ "and i1.URI = i2.URI");
		Util.print(MyResultSet.nRows(pairs)
				+ " possible duplicate items to merge...");
		while (pairs.next()) {
			int item1 = pairs.getInt(1);
			int item2 = pairs.getInt(2);
			switch (isDuplicate(item1, item2, facetTypesToMerge)) {
			case SAME:
				// Util.err("merge " + item1 + " " + item2 + " "
				// + pairs.getString(3));
				db.mergeItems(item1, item2);
				break;
			case DIFFERENT:
				Util.err(item1 + " and " + item2 + " have the same URI: "
						+ pairs.getString(3));
			}
		}
		Util.print("...merging items done.");
	}

	private DuplicateStatus isDuplicate(int item1, int item2,
			int[] facetTypesToMerge) throws SQLException {
		String desc1 = db.itemDescription(item1);
		String desc2 = db.itemDescription(item2);
		// Util.print("isDuplicate "+item1+" "+item2+" "+desc1);
		DuplicateStatus result = desc1 == null || desc2 == null ? DuplicateStatus.NA
				: desc1.equals(desc2) ? DuplicateStatus.SAME
						: DuplicateStatus.DIFFERENT;
		if (result == DuplicateStatus.SAME) {
			PreparedStatement getFacets1 = db
					.getJdbc()
					.lookupPS(
							"SELECT facet_id dummy1 "
									+ "FROM raw_item_facet WHERE record_num = ? ORDER BY facet_id");
			getFacets1.setInt(1, item1);
			ResultSet rs1 = db.getJdbc().SQLquery(getFacets1);

			// Need two copies since we need two ResultSets open simultaneously
			PreparedStatement getFacets2 = db
					.getJdbc()
					.lookupPS(
							"SELECT facet_id dummy2 "
									+ "FROM raw_item_facet WHERE record_num = ? ORDER BY facet_id");
			getFacets2.setInt(1, item2);
			ResultSet rs2 = db.getJdbc().SQLquery(getFacets2);
			while (result == DuplicateStatus.SAME) {
				boolean isRs1 = scrollToUnmergedFacet(rs1, facetTypesToMerge);
				boolean isRs2 = scrollToUnmergedFacet(rs2, facetTypesToMerge);
				if (!isRs1 && !isRs2)
					break;
				else if (!(isRs1 && isRs2 && rs1.getInt(1) == rs2.getInt(1)))
					result = DuplicateStatus.DIFFERENT;
				// if (!result)
				// Util.print("Difference: " + getName(rs1.getInt(1)) + " "
				// + getName(rs2.getInt(1)));
				// else
				// Util.print("Same: " + getName(rs1.getInt(1)) + " "
				// + getName(rs2.getInt(1)));
			}
			db.getJdbc().close(rs1);
			db.getJdbc().close(rs2);
		} else {
			// Util.print("Descriptions differ:\n" + desc1 + "\n\n" + desc2);
		}
		return result;
	}

	boolean scrollToUnmergedFacet(ResultSet rs, int[] facetTypesToMerge)
			throws SQLException {
		boolean result = rs.next();
		if (result
				&& Util.isMember(facetTypesToMerge, db.getFacetType(rs
						.getInt(1))))
			result = scrollToUnmergedFacet(rs, facetTypesToMerge);
		return result;
	}

	void printDuplicates() throws SQLException {
		Util.print("Listing possible duplicate facets based on name...");
		ResultSet rs = db
				.getJdbc()
				.SQLquery(
						"SELECT GROUP_CONCAT(facet_id)"
								+ "FROM raw_facet INNER JOIN (SELECT f.name "
								+ "FROM raw_facet f "
								+ "GROUP BY f.name HAVING COUNT(*) > 1) foo USING (name)"
								+ "GROUP BY name");
		while (rs.next()) {
			String[] facets = Util.splitComma(rs.getString(1));
			for (int i = 0; i < facets.length; i++) {
				int facet1 = Integer.parseInt(facets[i]);
				// int[] ancestors1 = ancestors(facet1);
				// for (int j = 0; j < facets.length; j++) {
				// if (i != j) {
				// int facet2 = Integer.parseInt(facets[j]);
				// if (getName(facet1) != null && getName(facet2) != null) {
				// int facetType1 = getFacetType(facet1);
				// int facetType2 = getFacetType(facet2);
				// if (facetType1 > 0 && facetType2 > 0
				// && getName(facetType1).equals("Date")
				// && getName(facetType2).equals("Subject")) {
				// merge(facet2, facet1);
				// }
				// }
				// int[] ancestors2 = ancestors(facet2);
				// if (Util.isMember(ancestors1,
				// getParentFacet(facet2)))
				// merge(facet2, facet1);
				// }
				// }
				if (facets.length > 4 && i > 2) {
					Util.print("... " + (facets.length - 2) + " more");
					break;
				} else {
					Util.print(db.ancestorString(facet1, ", "));
				}
			}
			Util.print("");
		}
		Util.print("\nListing possible duplicate facets...done");
	}

	/**
	 * 
	 * globals.itemURL should generate a unique ID for each item, so we can use
	 * that to identify corresponding records in another database and copy its
	 * images. However, we need to qualify that expression with copyFrom.
	 * qualifySQL is a kludge that we need to keep updated for all the databases
	 * we use.
	 * 
	 * For instance, itemURL might be:
	 * 
	 * (SELECT xref FROM movie, shotbreak WHERE movie.movie_id =
	 * shotbreak.movie_id AND shotbreak.shotbreak_id = item.shotbreak)
	 * 
	 * Assumes that images looks like:
	 * 
	 * record_num, image, [<globals.itemURL>,] w, h
	 * 
	 * where [] denotes an optional column, and that copyFrom.images has at
	 * least the four other columns.
	 * 
	 * @param copyFrom
	 *            database with images to copy
	 * @throws SQLException
	 */
	void copyImagesNoURI(String copyFrom) throws SQLException {
		Util.print("\nCopying images from " + copyFrom);
		JDBCSample jdbc = db.getJdbc();
		String dbName = jdbc.dbName;
		String uriGetter = jdbc.SQLqueryString("SELECT itemURL FROM globals");
		String copyFromURIgetter = jdbc.SQLqueryString("SELECT itemURL FROM "
				+ copyFrom + ".globals");
		// jdbc.ensureIndex(copyFrom, "item", "URI", "URI(51)", "");

		/**
		 * Does <this database>.images have a URI column? Assume columns
		 * 
		 * record_num, image, [URI,] w, h
		 */
		boolean isURIcolumn = jdbc.columnExists(dbName, "images", uriGetter);

		uriGetter = "it." + uriGetter;
		copyFromURIgetter = "copy_it."
				+ qualifySQL(copyFromURIgetter, copyFrom);
		int n = db.db("REPLACE INTO images "
				+ "SELECT it.record_num, copy_im.image, "
				+ (isURIcolumn ? copyFromURIgetter + ", " : "")
				+ "copy_im.w, copy_im.h " + "FROM item it "
				+ "LEFT JOIN images im ON it.record_num = im.record_num "
				+ "INNER JOIN " + copyFrom + ".item copy_it ON "
				+ copyFromURIgetter + " = " + uriGetter + " INNER JOIN "
				+ copyFrom
				+ ".images copy_im ON copy_it.record_num = copy_im.record_num "
				+ "WHERE im.image is NULL");
		Util.print("..." + n + " images copied");
	}

	private String qualifySQL(String sql, String schema) {
		int fromIndex = sql.indexOf("from");
		if (fromIndex > 0) {
			int whereIndex = sql.indexOf("where");
			String[] tokens = sql.substring(fromIndex,
					whereIndex > fromIndex ? whereIndex : sql.length()).split(
					",");
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim();
				tokens[i] = schema + "." + token;
			}
			String qualified = sql.substring(0, fromIndex) + Util.join(tokens);
			if (whereIndex > 0)
				qualified += sql.substring(whereIndex);
			sql = qualified;
		}
		return sql;
	}

	void loadImages(String URLexpr, String pattern, int maxDimension,
			int quality) throws SQLException, ImageFormatException,
			InterruptedException {
		// pattern = "\"(/cgi.*?gif)\"";
		Pattern cPattern = pattern == null ? null : Pattern.compile(pattern);
		// Matcher matcher1 = cPattern.matcher("<IMG
		// SRC=\"/gmd/gmd388/g3884/g3884n/cw0588000.gif\">");
		// Util.print(matcher1.find());
		// Util.print(pattern);
		for (int ll = 0; ll < 1; ll++) {
			ResultSet rs = db
					.getJdbc()
					.SQLquery(
							"SELECT item."
									+ URLexpr
									+ ", item.record_num "
									+ "FROM item LEFT JOIN images "
									+ "ON item.record_num = images.record_num "
//									+ "inner join temp.nothumb noth on noth.uri = item.uri "
									+ "WHERE images.image IS NULL"
					// "WHERE item.record_num = 195202"
					);
			Util.print(MyResultSet.nRows(rs) + " images to load");
//			URL ammemURL = null;
//			try {
//				ammemURL = new URL("http://memory.loc.gov/");
//			} catch (MalformedURLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			while (rs.next()) {
				try {
					String loc = rs.getString(1);
					int record = rs.getInt(2);
					Util.print("\n" + loc);
					URL thumbURL = new URL(loc);
					if (cPattern != null) {
						Matcher matcher = cPattern.matcher(Util.readURL(loc));
						if (matcher.find()) {
							loc = matcher.group(1);
//							thumbURL = ammemURL;
						} else {
							Util.print("Can't find pattern '" + pattern
									+ "' in " + loc);
							continue;
						}
						thumbURL = new URL(thumbURL, loc);
					}
					loadImageInternal(thumbURL, record, maxDimension, quality);
				} catch (IOException e) {
					Util.err("Error loading image " + rs.getString(1));
				}
			}
		}
	}

	private void loadImageInternal(URL url, int record_num, int maxDimension,
			int quality) throws ImageFormatException, InterruptedException,
			IOException {
		Util.print(url);
		Image image = Toolkit.getDefaultToolkit().createImage(url);
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);
		// Util.print("errors? => " + mediaTracker.isErrorAny());
		// determine thumbnail size from WIDTH and HEIGHT
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		int max = Math.max(imageWidth, imageHeight);
		if (max > 0) {
			if (max > maxDimension) {
				double ratio = maxDimension / (double) max;
				imageWidth = (int) Math.round(imageWidth * ratio);
				imageHeight = (int) Math.round(imageHeight * ratio);
			}
			// assert imageWidth > 0 && imageHeight > 0 : imageWidth + "x" +
			// imageHeight;
			BufferedImage thumbImage = new BufferedImage(imageWidth,
					imageHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = thumbImage.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(image, 0, 0, imageWidth, imageHeight, null);
			// save thumbnail image to OUTFILE
			ByteArrayOutputStream blobStream = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(blobStream);
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
			JPEGEncodeParam param = encoder
					.getDefaultJPEGEncodeParam(thumbImage);
			quality = Math.max(0, Math.min(quality, 100));
			param.setQuality(quality / 100.0f, false);
			encoder.setJPEGEncodeParam(param);
			encoder.encode(thumbImage);
			out.close();
			try {
				PreparedStatement setImage = db.getJdbc().lookupPS(
						"INSERT INTO images VALUES(?, ?, ?, ?)");
				setImage.setInt(1, record_num);
				setImage.setBytes(2, blobStream.toByteArray());
				setImage.setInt(3, imageWidth);
				setImage.setInt(4, imageHeight);
				db.getJdbc().SQLupdate(setImage);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			Util.err("Error getting image from " + url);
		}
	}

	void newItem() {
		db.newItem();
	}

	void newItem(int record_num) {
		db.newItem(record_num);
	}

	String getAttribute(String name) throws SQLException {
		return db.getAttribute(name);
	}

	private Set<Field> fields = new HashSet<Field>();

	void cleanUp() throws SQLException {
		for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
			Field field = it.next();
			field.cleanUp(db);
		}
	}

	void insertAttribute(Field field, String value) throws SQLException {
		fields.add(field);
		db.insertAttribute(field.name, value);
	}

	void insertFacet(Field field, List<String> hierValue) throws SQLException {
		fields.add(field);
		db.insertFacet(hierValue);
	}
}
