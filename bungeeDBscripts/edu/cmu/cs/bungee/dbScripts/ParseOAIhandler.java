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
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
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
final class ParseOAIhandler extends DefaultHandler {

	private static final String namespaceMARC = "http://www.loc.gov/MARC21/slim";
	private static final String namespaceDC = "http://purl.org/dc/elements/1.1/";

	private boolean verbose;
	private ConvertFromRaw converter;

	static final boolean dontUpdate = false;

	private int recordNum;

	/**
	 * Flag whether we're lexically inside a record
	 */
	private boolean isInRecord;

	private int maxNameLength = -1;

	private int facetID;

	private String tag = null;

	private String subfield = null;

	StringBuffer data;

	private Hashtable<String, String[]> subFacetTable = new Hashtable<String, String[]>();

	private String set;

	private JDBCSample jdbc;

	private String setSpec;

	private Hashtable<String, String> setNames = new Hashtable<String, String>();

	private String dbName;

	private Map<String, String> moves;

	private Map<String, String> renames;

	private Hashtable<String, String> placeAbbrevs;

	/**
	 * Places within states that help recognize place names in the subject
	 * hierarchy. Applies to the pattern "<city> ([<place>,] <state abbrev>)"
	 */
	private Collection<String> cities;

	/**
	 * These are use for place fields only
	 */
	private String[] abbrevs = { "Alabama;al:ala", "Alaska;ak",
			"American Samoa;as:a.s", "Arizona;az:ariz", "Arkansas;ar:ark",
			"British Columbia;bc:b.c", "California;ca:cal:calif",
			"Colorado;co:col:colo", "Connecticut;ct:conn", "Delaware;de:del",
			"Federated States Of Micronesia;fm", "Florida;fl:fla",
			"Georgia;ga", "Guam;gu", "Hawaii;hi", "Idaho;id",
			"Illinois;il:ill", "Indiana;in:ind", "Iowa;ia", "Kansas;ks:kan",
			"Kentucky;ky", "Louisiana;la", "Maine;me", "Marshall Islands;mh",
			"Maryland;md", "Massachusetts;ma:mass", "Michigan;mi:mich",
			"Minnesota;mn:minn", "Mississippi;ms:miss", "Missouri;mo",
			"Montana;mt:mont", "Nebraska;ne:neb", "Nevada;nv:nev",
			"New Hampshire;nh", "New Jersey;nj:n.j", "New Mexico;nm:n.m",
			"New York;ny:n.y", "North Carolina;nc:n.c", "North Dakota;nd:n.d",
			"Northern Mariana Islands;mp", "Nova Scotia;ns:n.s", "Ohio;oh",
			"Oklahoma;ok:okla", "Ontario;ont", "Oregon;or:ore", "Palau;pw",
			"Pennsylvania;pa:penn", "Puerto Rico;pr:p.r",
			"Rhode Island;ri:r.i", "South Carolina;sc:s.c",
			"South Dakota;sd:s.d",
			"Tennessee;tn:tenn",
			"Texas;tx:tex",
			// "United States;us",
			"Utah;ut", "Vermont;vt", "Virgin Islands;vi:v.i", "Virginia;va",
			"Washington;wa:wash", "Washington DC;dc:d.c:washington, d.c",
			"West Virginia;wv:w va:wva:w.v", "Wisconsin;wi:wis:wisc",
			"Wyoming;wy:wyo" };

	ParseOAIhandler(JDBCSample _jdbc, String db, boolean _verbose,
			Collection<String[]> _renames, Collection<String[]> _moves,
			Collection<String> _places) {
		renames = stringPair2Map(_renames, false);
		moves = stringPair2Map(_moves, true);
		jdbc = _jdbc;
		dbName = db;
		verbose = _verbose;
		cities = _places;

		// parseTGM();
		// parse043codes();
		// checkMultipleParents("places_hierarchy");
		// checkMultipleParents("TGM");

		try {
			recordNum = jdbc.SQLqueryInt("SELECT MAX(record_num) FROM item");
			facetID = Math
					.max(
							jdbc
									.SQLqueryInt("SELECT MAX(facet_id) FROM raw_facet"),
							jdbc
									.SQLqueryInt("SELECT MAX(facet_type_id) FROM raw_facet_type") + 100);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	Map<String, String> stringPair2Map(Collection<String[]> pairs,
			boolean multipleValuesOK) {
		Map<String, String> result = null;
		if (pairs != null) {
			result = new LinkedHashMap<String, String>();
			for (String[] entry : pairs) {
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

	int db(PreparedStatement SQL, String desc) {
		int nRows = 0;
		if (dontUpdate) {
			Util.print(desc);
		} else {
			try {
				nRows = jdbc.SQLupdate(SQL, desc);
			} catch (SQLException e) {
				System.err.println("SQLException for " + desc);
				e.printStackTrace();
			}
		}
		return nRows;
	}

	ConvertFromRaw convertFromRaw() {
		if (converter == null)
			converter = new ConvertFromRaw(jdbc);
		return converter;
	}

	private void log(String s) {
		if (verbose)
			Util.print(s);
	}

	private String rename(String s) {
		// Util.print("rename " + renames);
		String result = renames == null ? null : renames.get(s);
		if (result == null)
			result = s;
		return result;
	}

	/**
	 * Rename facets according to the explicit list 'rename'. This can correct
	 * common spelling errors, for instance.
	 * 
	 * @throws SQLException
	 */
	private void rename() throws SQLException {
		for (Map.Entry<String, String> entry : renames.entrySet()) {
			String from = truncateName(entry.getKey(), false);
			String to = truncateName(entry.getValue(), false);

			PreparedStatement ps = jdbc
					.lookupPS("SELECT old.parent_facet_id FROM raw_facet old, raw_facet new "
							+ "WHERE old.parent_facet_id = new.parent_facet_id and old.name = ? and new.name = ? LIMIT 1");
			ps.setString(1, from);
			ps.setString(2, to);
			int parent = jdbc.SQLqueryInt(ps, "Look for rename collision");
			if (parent > 0) {
				int fromID = lookupFacet(from, parent);
				int toID = lookupFacet(to, parent);
				assert fromID > 0 : from + " " + ancestorString(parent);
				assert toID > 0 : to + " " + ancestorString(parent);
				merge(fromID, toID);
			} else {
				PreparedStatement updateFromRawFacet = jdbc
						.lookupPS("UPDATE raw_facet SET name = ? "
								+ "WHERE name = ?");
				updateFromRawFacet.setString(1, to);
				updateFromRawFacet.setString(2, from);
				db(updateFromRawFacet, "Update raw_facet");

				// db("UPDATE facet SET name = '" + rename[i][1]
				// + "' WHERE name = '" + rename[i][0] + "'");
			}
		}
	}

	// private void copyImages(String copyFrom) throws SQLException {
	// Util.print("\nCopying images from " + copyFrom);
	// convertFromRaw().ensureIndex("images", "URI", "URI");
	// convertFromRaw().ensureIndex(copyFrom + ".images", "URI", "URI");
	// db("REPLACE INTO images "
	// + "SELECT it.record_num, copy_im.image, copy_im.URI, copy_im.w, copy_im.h
	// "
	// + "FROM item it "
	// + "LEFT JOIN images im ON it.record_num = im.record_num "
	// + "INNER JOIN " + copyFrom
	// + ".images copy_im ON copy_im.uri = it.uri "
	// + "WHERE im.image is NULL");
	// Util.print("...done");
	// }

	/**
	 * Don't assume either images table has a URI column
	 * 
	 * @param copyFrom
	 * @throws SQLException
	 */
	void copyImagesNoURI(String copyFrom) throws SQLException {
		Util.print("\nCopying images from " + copyFrom);
		jdbc.ensureIndex("item", "URI", "URI(51)");
		jdbc.ensureIndex(copyFrom + ".item", "URI", "URI(51)");
		int isURIcolumn = jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM information_schema.columns "
						+ "WHERE table_schema = " + dbName
						+ " AND table_name = 'images' AND column_name = 'URI'");
		int n = db("REPLACE INTO images	"
				+ "SELECT it.record_num, copy_im.image, "
				+ (isURIcolumn > 0 ? "copy_im.URI, " : "")
				+ "copy_im.w, copy_im.h " + "FROM item it "
				+ "LEFT JOIN images im ON it.record_num = im.record_num "
				+ "INNER JOIN " + copyFrom
				+ ".images copy_im ON copy_it.record_num = copy_im.record_num "
				+ "INNER JOIN " + copyFrom
				+ ".item copy_it ON copy_it.uri = it.uri "
				+ "WHERE im.image is NULL");
		Util.print("..." + n + " images copied");
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

		PreparedStatement getMinID = jdbc
				.lookupPS("SELECT MIN(record_num) FROM item WHERE record_num >= ?");
		getMinID.setInt(1, 0);
		int first = jdbc.SQLqueryInt(getMinID, "Lookup MIN item.record_num");
		for (int i = 1; i < first; i++) {
			getMinID.setInt(1, i);
			int old = jdbc.SQLqueryInt(getMinID, "Lookup MIN item.record_num");
			if (old > i) {
				for (int j = 0; j < tables.length; j++) {
					PreparedStatement renumber = jdbc.lookupPS("UPDATE "
							+ tables[j]
							+ " SET record_num = ? WHERE record_num = ?");
					renumber.setInt(1, i);
					renumber.setInt(2, old);
					db(renumber, "Renumber");
				}
			} else
				break;
		}
		Util.print("...renumbering done");
	}

	private String ancestorString(int facet) throws SQLException {
		return Util.join(ancestors(facet, false), " -- ");
	}

	private String[] ancestors(int facet, boolean showID) throws SQLException {
		// int parent = getParentFacet(facet);
		if (facet > 0) {
			return (String[]) Util.endPush(ancestors(getParentFacet(facet),
					showID), getName(facet)
					+ (showID ? " (" + facet + ")" : ""), String.class);
		} else
			return null;
	}

	// private int[] ancestors(int facet) throws SQLException {
	// // int parent = getParentFacet(facet);
	// if (facet > 0) {
	// return Util.push(ancestors(getParentFacet(facet)), facet);
	// } else
	// return null;
	// }

	void printDuplicates() throws SQLException {
		Util.print("Listing duplicate facet names...");
		ResultSet rs = jdbc
				.SQLquery("SELECT GROUP_CONCAT(f.facet_id), COUNT(DISTINCT parent.name) cnt "
						+ "FROM raw_facet f LEFT JOIN raw_facet parent ON f.parent_facet_id = parent.facet_id "
						+ "GROUP BY f.name HAVING COUNT(*) > 1 AND cnt > 1");
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
					Util.print(Util.join(ancestors(facet1, false)));
				}
			}
			Util.print("");
		}
		Util.print("\nListing duplicate facet names...done");
	}

	/**
	 * Change the facet hierarchy by moving the subtrees explicitly listed in
	 * moves. All children of the old lineage are reparented to the new lineage,
	 * which is created if nececssary. The now-childless old parent is then
	 * deleted.
	 * 
	 * @throws SQLException
	 */
	private void promote(int maxIterations) throws SQLException {
		convertFromRaw().fixDuplicates();
		boolean change = false;
		if (maxIterations > 0) {
			Util.print("\n moving ...");

			// results maps from the parentName to a set of prommoted children
			// and a set of unpromoted children
			Map<String, List<String>[]> results = new Hashtable<String, List<String>[]>();
			Map.Entry<String, String> entry = null;
			try {
				for (Iterator<Entry<String, String>> it = moves.entrySet()
						.iterator(); it.hasNext();) {
					entry = it.next();
					String oldName = entry.getKey();
					String[] newNames = Util.splitSemicolon(entry.getValue());
					for (int k = 0; k < newNames.length; k++) {
						String newName = newNames[k];
						String[] olds = oldName.split(" -- ");
						String[] news = newName.split(" -- ");
						int parentFacet = facetType(olds[0]);
						assert parentFacet > 0 : "Can't find '" + olds[0]
								+ "' in raw_facet_type, from '" + data + "'";

						for (int j = 1; j < olds.length && parentFacet > 0
								&& !"*".equals(olds[j]); j++)
							parentFacet = lookupFacet(olds[j], parentFacet);
						boolean isPromoted = parentFacet > 0;
						if (isPromoted) {
							String[] leafs = { olds[olds.length - 1] };
							if ("*".equals(olds[olds.length - 1])) {
								assert false : "No need for '*': " + oldName
										+ " " + newName;
								PreparedStatement ps = jdbc
										.lookupPS("SELECT facet_id FROM raw_facet WHERE parent_facet_id = ?");
								ps.setInt(1, parentFacet);
								leafs = jdbc.SQLqueryStringArray(ps,
										"get children");
							}
							for (int i = 0; i < leafs.length; i++) {
								olds[olds.length - 1] = leafs[i];
								logPromote(Arrays.asList(olds), isPromoted,
										results);
								int type = facetType(news[0]);
								int newParent = type;
								for (int j = 1; j < news.length; j++) {
									int childType = lookupFacet(news[j],
											newParent);
									if (childType < 0) {
										childType = newFacet(news[j], newParent);
									}
									newParent = childType;
								}
								assert newParent > 0 : newName;
								assert newParent < 1000000 : newParent;
								assert parentFacet > 0 : oldName;
								assert parentFacet < 1000000 : oldName;
								// promoteInternal(parentFacet, newParent);
								merge(parentFacet, newParent);
								change = true;
							}
						} else {
							// log("promote: Can't find " + oldName + " to
							// promote");
						}
					}
				}
			} catch (Throwable e) {
				Util.err("Error moving " + Util.valueOfDeep(entry) + ": ");
				e.printStackTrace();
			}
			printPromoteLog(results);
			Util.print("... moving done");
		}
		if (change)
			promote(maxIterations - 1);
	}

	private void logPromote(List<String> olds, boolean isPromoted,
			Map<String, List<String>[]> results) {
		if (verbose) {
			List<String> names = new LinkedList<String>(olds);
			// add a dummy child so it can go in either the good or bad list
			names.add("leaf");
			// Util.print(names);
			logPromoteInternal(names, names.size(), isPromoted, results);
		}
	}

	@SuppressWarnings("unchecked")
	private void logPromoteInternal(List<String> olds, int generation,
			boolean isPromoted, Map<String, List<String>[]> results) {
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
			List<String> prev = prevResults[isPromoted ? 0 : 1];
			if (!prev.contains(oldName))
				prev.add(oldName);
			logPromoteInternal(olds, generation - 1, isPromoted, results);
			// if (isPromoted)
			// Util.print("promote record for " + parentName + " "
			// + Util.valueOfDeep(prevResults));
		}
	}

	private void printPromoteLog(Map<String, List<String>[]> promotions) {
		printPromoteLogInternal("", promotions);
	}

	private void printPromoteLogInternal(String name,
			Map<String, List<String>[]> promotions) {
		List<String>[] results = promotions.get(name);
		if (results != null) {
			// "leaf" record won't exist
			List<String> bads = results[1];
			List<String> goods = results[0];
			bads.removeAll(goods);
			if (bads.size() > 0 && name.length() > 0) {
				log("... failed to find " + bads.size() + " children of "
						+ name + ", including " + bads.get(0));
			}
			// if (goods.size() > 0) {
			// log("... found " + goods.size() + " children of " + name
			// + ", including " + goods.get(0));
			// }
			for (String goodName : goods) {
				printPromoteLogInternal(goodName, promotions);
			}
		}
	}

	// private void promoteInternal(int oldParent, int newParent)
	// throws SQLException {
	// log("mmm " + ancestorString(oldParent) +" => " +
	// ancestorString(newParent));
	// assert newParent != oldParent : newParent + " " + oldParent;
	// ResultSet rs = jdbc
	// .SQLquery("SELECT facet_id FROM raw_facet WHERE parent_facet_id = "
	// + oldParent);
	// while (rs.next()) {
	// int child = rs.getInt(1);
	// String childName = getName(child);
	// assert childName != null : getName(oldParent) + " "
	// + getName(newParent);
	// log("lll "+ancestorString(child)+ " => " + ancestorString(newParent));
	// log(childName + " => " + getName(newParent));
	// setParent(child, newParent);
	// }
	// db("REPLACE INTO raw_item_facet SELECT record_num, " + newParent
	// + " FROM raw_item_facet WHERE facet_id = " + oldParent);
	// db("DELETE FROM raw_facet WHERE facet_id = " + oldParent);
	// db("DELETE FROM raw_item_facet WHERE facet_id = " + oldParent);
	// }

	void useTGM() throws SQLException {
		if (renames != null)
			rename();
		// useTGMinternal("loc.tgm", facetType("Subject"));
		// useTGMinternal("places_hierarchy",
		// facetType("Location of Publication"));
		// useTGMinternal("loc.places_hierarchy", facetType("Places"));
		// rename();

		if (moves != null)
			promote(5);
		if (renames != null || moves != null) {
			convertFromRaw().fixMissingItemFacets(0);
			// promote(toPromoteDoublingItems);

			// fixFacetTypes();
			convertFromRaw().fixDuplicates();
			convertFromRaw().fixMissingItemFacets(0);
			convertFromRaw().findBrokenLinks(true, 0);

			// if (moves != null)
			// promoteSingletons();
			convertFromRaw().findBrokenLinks(true, 0);
		}
	}

	/**
	 * For every combination of
	 * 
	 * grandparent -- parent -- child
	 * 
	 * where parent has no other children and every item that is a parent is
	 * also a child, delete parent and make child a direct child of grandparent.
	 * 
	 * @throws SQLException
	 */
	private void promoteSingletons() throws SQLException {
		convertFromRaw().fixMissingItemFacets(0);
		ResultSet rs = jdbc
				.SQLquery("SELECT p.facet_id, p.parent_facet_id, f.facet_id FROM raw_facet f "
						+ "INNER JOIN raw_facet p ON f.parent_facet_id = p.facet_id "
						+ "GROUP BY p.facet_id HAVING COUNT(*) = 1");
		boolean change = false;
		while (rs.next()) {
			int singleton = rs.getInt(3);
			int parent = rs.getInt(1);
			int grandparent = rs.getInt(2);
			if (getFacetName(singleton) != null && getName(grandparent) != null
					&& nItems(singleton) == nItems(parent)) {
				// Name check ensures previous promotions haven't deleted any of
				// the facets involved.
				change = true;
				log("Promoting singleton " + getFacetName(singleton) + " from "
						+ getFacetName(parent) + " to " + getName(grandparent));
				setParent(singleton, grandparent);

				PreparedStatement deleteFromRawFacet = jdbc
						.lookupPS("DELETE FROM raw_facet WHERE facet_id = ?");
				deleteFromRawFacet.setInt(1, parent);
				db(deleteFromRawFacet, "Delete from raw_facet");
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

		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id, facet_type_id FROM raw_facet f INNER JOIN raw_facet_type ft ON f.parent_facet_id = ft.facet_type_id"
						+ " GROUP BY facet_type_id HAVING COUNT(*) = 1");
		boolean change = false;
		while (rs.next()) {
			int singleton = rs.getInt(1);
			if (nItems(singleton) == jdbc
					.SQLqueryInt("SELECT COUNT(DISTINCT record_num) FROM raw_facet f "
							+ "INNER JOIN raw_item_facet i ON f.facet_id = i.facet_id "
							+ "WHERE f.parent_facet_id = " + singleton)) {
				change = true;
				log("Promoting singleton "
						+ Util.join(ancestors(singleton, false)));
				int type = rs.getInt(2);

				PreparedStatement updateRawFacet = jdbc
						.lookupPS("UPDATE raw_facet set parent_facet_id = ? "
								+ "WHERE parent_facet_id = ?");
				updateRawFacet.setInt(1, type);
				updateRawFacet.setInt(2, singleton);
				db(updateRawFacet, "Update raw_facet");

				PreparedStatement deleteFromRawFacet = jdbc
						.lookupPS("DELETE FROM raw_facet WHERE facet_id = ?");
				deleteFromRawFacet.setInt(1, singleton);
				db(deleteFromRawFacet, "Delete from raw_facet");
			}
		}
		if (change) {
			convertFromRaw().fixDuplicates();
			promoteSingletons();
		}
	}

	private int nItems(int facetID1) throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM raw_item_facet WHERE facet_id = "
						+ facetID1);
	}

	// private void useTGMinternal(String TGMtable, int facetTypeID)
	// throws SQLException {
	// // Util.print("\nuseTGMinternal " + TGMtable + " " + facetTypeID);
	// boolean change = false;
	// ResultSet rs = jdbc
	// .SQLquery("SELECT GROUP_CONCAT(DISTINCT t.term SEPARATOR ' -- '),
	// t.broader, GROUP_CONCAT(DISTINCT p.facet_id) "
	// + "FROM (raw_facet r INNER JOIN "
	// + TGMtable
	// + " t ON r.name = t.term) "
	// + "LEFT JOIN raw_facet p ON t.broader = p.name" // AND
	// // p.facet_type_idxx
	// // = "
	// // + facetTypeID
	// + " WHERE r.parent_facet_id = "
	// + facetTypeID
	// + " GROUP BY t.broader");
	// while (rs.next()) {
	// String parentIDs = rs.getString(3);
	// int[] parents = null;
	// if (parentIDs != null) {
	// String[] parentIDstrings = parentIDs.split(",");
	// parents = new int[0];
	// for (int i = 0; i < parentIDstrings.length; i++) {
	// int parent = Integer.parseInt(parentIDstrings[i]);
	// if (getFacetType(parent) == facetTypeID)
	// parents = Util.push(parents, parent);
	// }
	// }
	// parents = pickParent(parents);
	// int nParents = parents == null ? 0 : parents.length;
	// // Use a separator not likely to occur in a term
	// String[] terms = rs.getString(1).split(" -- ");
	// String broader = rs.getString(2);
	// if (nParents > 1) {
	// notAdding(terms, broader, parents);
	// } else {
	// int parent = nParents == 1 ? parents[0] : -1;
	// // if (rs.getString(1).indexOf("California") >= 0)
	// // Util.print(rs.getString(1) + " => " + broader + " "
	// // + parent);
	// if (parent < 0) {
	// change = true;
	// parent = newFacet(broader, facetTypeID);
	// }
	// for (int i = 0; i < terms.length; i++) {
	// String term = terms[i];
	// int facet = lookupFacet(term, facetTypeID);
	// // log(term + " => " + broader + " " + facet + " " +
	// // parent);
	// if (facet > 0)
	// setParent(facet, parent);
	// else {
	// // term occurs multiple times in TGM table and we've
	// // already processed one of them. Add a duplicate to
	// // this parent, copying over all children and items.
	// addDuplicateFacet(term, parent, facetTypeID);
	// }
	// }
	// }
	// }
	// if (change)
	// useTGMinternal(TGMtable, facetTypeID);
	// }
	//
	// private int[] pickParent(int[] parents) throws SQLException {
	// int[] result = parents;
	// if (moves != null)
	// if (parents != null && parents.length > 1) {
	// for (int i = 0; i < parents.length && result == parents; i++) {
	// String path = Util.join(
	// Util.reverse(ancestors(parents[i], false)), " -- ")
	// .toLowerCase();
	// for (Iterator it = moves.iterator(); it.hasNext()
	// && result == parents;) {
	// String[] entry = (String[]) it.next();
	// String key = entry[0];
	// String value = entry[1];
	// if (key.toLowerCase().indexOf(path) >= 0
	// || value.toLowerCase().indexOf(path) >= 0) {
	// result = new int[1];
	// result[0] = parents[i];
	// // Util.print("Found parent " + path);
	// // Util.print(toPromote[j]);
	// }
	// }
	// }
	// }
	// return result;
	// }
	//
	// private Hashtable previousNotAddingWarnings = new Hashtable();
	//
	// private void notAdding(String[] termList, String broader, int[] parents)
	// throws SQLException {
	// String terms = Util.join(termList);
	// String[] prev = (String[]) previousNotAddingWarnings.get(terms);
	// if (!Util.isMember(prev, broader)) {
	// Util.print("\nNot adding parent " + broader + " to [" + terms
	// + "]\n multiple matches:");
	// // int example = -1;
	// for (int i = 0; i < parents.length; i++) {
	// int parent = parents[i];
	// // if (i == 1)
	// // example = getParentFacet(parent);
	// PreparedStatement ps = jdbc.lookupPS("SELECT GROUP_CONCAT(name) FROM
	// raw_facet
	// WHERE parent_facet_id = ? GROUP BY parent_facet_id");
	// ps.setInt(1, parent);
	// Util.print(" " + Util.join(ancestors(parent, true))
	// + "\n children: "
	// + jdbc.SQLqueryString(ps, "not adding"));
	// }
	// // Util
	// // .print(" Suggest changing bad parent_facet_ids to the right
	// // one, then running ConvertFromRaw.");
	// // Util.print(" e.g. UPDATE raw_facet SET parent_facet_id = "
	// // + example + " WHERE facet_id = " + parents[0]);
	//
	// String bad = Util.join(Util.reverse(ancestors(parents[0], false)),
	// " -- ");
	// String good = Util.join(Util.reverse(ancestors(parents[1], false)),
	// " -- ");
	// Util
	// .print(" Suggest promoting bad parent_facet_ids to the right one, e.g.
	// add to toPromote:");
	// Util.print("\"" + bad + ";" + good + "\"");
	//
	// prev = (String[]) Util.push(prev, broader, String.class);
	// if (prev.length == 1)
	// previousNotAddingWarnings.put(terms, prev);
	// }
	// }

	private int setParent(int facet, int parent) {
		int result = facet;
		if (!dontUpdate) {
			try {
				PreparedStatement getParent = jdbc
						.lookupPS("SELECT old.facet_id FROM raw_facet new, raw_facet old "
								+ "WHERE old.parent_facet_id = ? AND old.name = new.name AND new.facet_id = ? LIMIT 1");
				getParent.setInt(1, parent);
				getParent.setInt(2, facet);
				int old = jdbc.SQLqueryInt(getParent,
						"Check for setParent collision");
				if (old > 0) {
					merge(facet, old);
					result = old;
				} else {
					PreparedStatement setParent = jdbc
							.lookupPS("UPDATE raw_facet SET parent_facet_id = ? WHERE facet_id = ?");
					setParent.setInt(1, parent);
					setParent.setInt(2, facet);
					jdbc.SQLupdate(setParent, "Set facet parent");
				}
			} catch (SQLException e) {
				Util.err(facet + " " + parent);
				e.printStackTrace();
			}
		} else {
			Util.print("UPDATE raw_facet SET parent_facet_id = " + parent
					+ " WHERE facet_id = " + facet);
		}
		return result;
	}

	// private int lookupSomeFacet(String name, int facetType) throws
	// SQLException {
	// int[] facets = lookupFacets(name, facetType);
	// if (facets.length > 0)
	// return facets[0];
	// else
	// return -1;
	// }

	private int lookupFacet(String name, int parent) {
		int result = -1;
		try {
			// Use index hint because it's spending lots of time in 'statistics'
			PreparedStatement lookupFacet = jdbc
					.lookupPS("SELECT facet_id FROM raw_facet USE INDEX (name) WHERE name = ? AND parent_facet_id = ? LIMIT 1");
			lookupFacet.setString(1, truncateName(name, true));
			lookupFacet.setInt(2, parent);
			result = jdbc.SQLqueryInt(lookupFacet,
					"Lookup facet from name and parent");
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

	private int[] lookupFacets(String name, int type) throws SQLException {
		int[] result = new int[0];
		ResultSet rs = null;
		try {
			PreparedStatement lookupFacets = jdbc
					.lookupPS("SELECT facet_id FROM raw_facet WHERE name = ?");
			lookupFacets.setString(1, truncateName(name, true));
			// lookupSomeFacet.setInt(2, type);
			for (rs = jdbc.SQLquery(lookupFacets,
					"Find one facet with name and type"); rs.next();) {
				int facet = rs.getInt(1);
				if (getFacetType(facet) == type) {
					result = Util.push(result, facet);
				}
			}
		} finally {
			if (rs != null)
				jdbc.close(rs);
		}
		return result;
	}

	// private void addDuplicateFacet(String term, int parent, int facetType)
	// throws SQLException {
	// int old = lookupSomeFacet(term, facetType);
	// if (verbose) {
	// PreparedStatement ps = jdbc.lookupPS("SELECT name FROM raw_facet WHERE
	// facet_id = ?");
	// ps.setInt(1, parent);
	// String s1 = jdbc.SQLqueryString(ps, "lookup name");
	// ps = jdbc.lookupPS("SELECT p.name FROM raw_facet r "
	// + "INNER JOIN raw_facet p ON r.parent_facet_id = p.facet_id "
	// + "WHERE r.facet_id = ?");
	// ps.setInt(1, old);
	// String s2 = jdbc.SQLqueryString(ps, "lookup name");
	// log("addDuplicateFacet " + term + ", " + s1 + ", " + s2);
	// }
	// assert old > 0 : term + " " + parent + " " + facetType;
	// parent = newFacet(term, parent);
	//
	// ResultSet rs = itemFacets(old);
	// while (rs.next()) {
	// addItemFacet(rs.getInt(1), parent);
	// }
	//
	// String[] childNames = null;
	// rs = childFacets(old);
	// while (rs.next()) {
	// childNames = (String[]) Util.push(childNames, rs.getString(1),
	// String.class);
	// }
	// if (childNames != null) {
	// for (int i = 0; i < childNames.length; i++) {
	// addDuplicateFacet(childNames[i], parent, facetType);
	// }
	// }
	// }

	private void merge(int from, int to) throws SQLException {
		log("merging " + ancestorString(from) + " into " + ancestorString(to));

		PreparedStatement ps = jdbc
				.lookupPS("SELECT new.facet_id, old.facet_id FROM raw_facet new, raw_facet old "
						+ "WHERE new.name = old.name and new.parent_facet_id = ? AND old.parent_facet_id = ? LIMIT 1");
		ps.setInt(1, to);
		ps.setInt(2, from);
		ResultSet rs = jdbc.SQLquery(ps, "Look for merge name collision");
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

		PreparedStatement updateRawFacet = jdbc
				.lookupPS("UPDATE raw_facet SET parent_facet_id = ? "
						+ "WHERE parent_facet_id = ?");
		updateRawFacet.setInt(1, to);
		updateRawFacet.setInt(2, from);
		db(updateRawFacet, "Update raw_facet");

		PreparedStatement updateRawItemFacet = jdbc
				.lookupPS("REPLACE INTO raw_item_facet "
						+ "SELECT record_num, ? FROM raw_item_facet WHERE facet_id = ?");
		updateRawItemFacet.setInt(1, to);
		updateRawItemFacet.setInt(2, from);
		db(updateRawItemFacet, "Update raw_item_facet");

		PreparedStatement deleteFromRawItemFacet = jdbc
				.lookupPS("DELETE FROM raw_item_facet WHERE facet_id = ?");
		deleteFromRawItemFacet.setInt(1, from);
		db(deleteFromRawItemFacet, "Delete from raw_item_facet");

		PreparedStatement deleteFromRawFacet = jdbc
				.lookupPS("DELETE FROM raw_facet WHERE facet_id = ?");
		deleteFromRawFacet.setInt(1, from);
		db(deleteFromRawFacet, "Delete from raw_facet");
	}

	/**
	 * Assumes attributes are the same, and we're just collecting all the facets
	 * 
	 * @param from
	 * @param to
	 * @throws SQLException
	 */
	private void mergeItems(int from, int to) throws SQLException {
		PreparedStatement mergeFacets = jdbc
				.lookupPS("REPLACE INTO raw_item_facet "
						+ "SELECT ?, facet_id FROM raw_item_facet WHERE record_num = ?");
		mergeFacets.setInt(1, to);
		mergeFacets.setInt(2, from);
		db(mergeFacets, "Merge item facets");

		PreparedStatement deleteItem = jdbc
				.lookupPS("DELETE FROM item WHERE record_num = ?");
		deleteItem.setInt(1, from);
		db(deleteItem, "Delete merged item");

		PreparedStatement deleteItemFacets = jdbc
				.lookupPS("DELETE FROM raw_item_facet WHERE record_num = ?");
		deleteItemFacets.setInt(1, from);
		db(deleteItemFacets, "Delete merged itemFacets");
	}

	// private ResultSet childFacets(int parent) {
	// ResultSet rs = null;
	// try {
	// PreparedStatement childFacets = jdbc.lookupPS("SELECT name FROM raw_facet
	// WHERE parent_facet_id = ?");
	// childFacets.setInt(1, parent);
	// rs = jdbc.SQLquery(childFacets, "Get child facet names");
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// return rs;
	// }
	//
	// private ResultSet itemFacets(int facet) {
	// ResultSet rs = null;
	// try {
	// PreparedStatement itemFacets = jdbc.lookupPS("SELECT record_num FROM
	// raw_item_facet WHERE facet_id = ?");
	// itemFacets.setInt(1, facet);
	// rs = jdbc.SQLquery(itemFacets, "Get items having facet");
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// return rs;
	// }

	private void addItemFacet(int record_num, int facet_id) {
		// Util.print("addItemFacet " + facet_id);
		if (!dontUpdate) {
			try {
				PreparedStatement itemFacet = jdbc
						.lookupPS("REPLACE INTO raw_item_facet VALUES(?, ?)");
				itemFacet.setInt(1, record_num);
				itemFacet.setInt(2, facet_id);
				jdbc.SQLupdate(itemFacet, "Add item/facet relationship");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			Util.print("REPLACE INTO raw_item_facet VALUES(" + record_num
					+ ", " + facet_id + ")");
	}

	private int newFacet(String name, int parent) throws SQLException {
		// if (name.equals("Aerial views")) {
		// Util.err(name + " " + getName(parent) + " '" + data + "' " + tag
		// + subfield);
		// // Util.printStackTrace();
		// }
		name = truncateName(name, false);
		if (!dontUpdate) {
			assert lookupFacet(name, parent) < 0 : name + " " + parent + " "
					+ getName(parent);
			try {
				PreparedStatement newFacet = jdbc
						.lookupPS("INSERT INTO raw_facet VALUES(?, ?, ?, null)");
				newFacet.setInt(1, ++facetID);
				newFacet.setString(2, name);
				newFacet.setInt(3, parent);
				// newFacet.setInt(4, sort);
				jdbc.SQLupdate(newFacet, "Add new facet");
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

	private String truncateName(String name, boolean noWarn)
			throws SQLException {
		if (maxNameLength < 0) {
			maxNameLength = jdbc
					.SQLqueryInt("SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE table_name = 'raw_facet' AND column_name = 'name'"
							+ " AND table_schema = '" + dbName + "'");
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

//	void loadImagesx(String URLexpr, String pattern, int maxDimension,
//			int quality) throws SQLException, ImageFormatException,
//			InterruptedException, IOException {
//		Pattern cPattern = Pattern.compile("will link to the future digital reproduction");
//		ResultSet rs = jdbc.SQLquery("SELECT uri from temp.nothumb");
//		int i = 0;
//		while (rs.next()) {
//			String loc = rs.getString(1);
//			if (true || i++ % 10 == 0) {
//				Util.print("\n" + loc);
//				try {
//					URL thumbURL = new URL(loc);
//
//					try {
//						Runtime.getRuntime().exec(
//								"C:\\Program Files\\Mozilla Firefox\\firefox.exe \""
//										+ loc + "\"");
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//
////					 Matcher matcher = cPattern.matcher(Util.readURL(loc));
////					 boolean found = matcher.find();
////					 Util.print(found);
////					 if (found) {
////					 jdbc.SQLupdate("DELETE FROM temp.nothumb WHERE URI = '"+loc+"'");
////					 }
//					 
//				} catch (MalformedURLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//
//	}

	void loadImages(String URLexpr, String pattern, int maxDimension,
			int quality) throws SQLException, ImageFormatException,
			InterruptedException {
//		pattern = "\"(/cgi.*?gif)\"";
		Pattern cPattern = pattern == null ? null : Pattern.compile(pattern);
		// Matcher matcher1 = cPattern.matcher("<IMG
		// SRC=\"/gmd/gmd388/g3884/g3884n/cw0588000.gif\">");
		// Util.print(matcher1.find());
		// Util.print(pattern);
		for (int ll = 0; ll < 1; ll++) {
			ResultSet rs = jdbc.SQLquery("SELECT item." + URLexpr
					+ ", item.record_num " + "FROM item LEFT JOIN images "
					+ "ON item.record_num = images.record_num "
					+ "inner join temp.nothumb noth on noth.uri = item.uri "
					+ "WHERE images.image IS NULL"
			// "WHERE item.record_num = 195202"
					);
			Util.print(MyResultSet.nRows(rs) + " images to load");
			URL ammemURL = null;
			try {
				ammemURL = new URL("http://memory.loc.gov/");
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
							thumbURL = ammemURL;
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
				PreparedStatement setImage = jdbc
						.lookupPS("INSERT INTO images VALUES(?, ?, ?, ?)");
				setImage.setInt(1, record_num);
				setImage.setBytes(2, blobStream.toByteArray());
				setImage.setInt(3, imageWidth);
				setImage.setInt(4, imageHeight);
				jdbc.SQLupdate(setImage, "Set image");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			Util.err("Error getting image from " + url);
		}
	}

	@Override
	public void startDocument() {
		// System.out.println("Document processing started");
		// db("ALTER TABLE raw_facet ADD INDEX name (name)");
	}

	@Override
	public void endDocument() {
		// db("ALTER TABLE raw_facet DROP INDEX name");
		// System.out.println("Document processing finished");
		Util.print("...done\n");
	}

	private int recordNum() {
		assert recordNum > 0;
		return recordNum;
	}

	void newItem() {
		try {
			PreparedStatement newItem = jdbc
					.lookupPS("INSERT INTO item VALUES(null, null, ?, null, null, null, null, null)");
			newItem.setInt(1, ++recordNum);
			if (!dontUpdate) {
				jdbc.SQLupdate(newItem, "Add new empty item");
			} else
				Util
						.print("INSERT INTO item VALUES(null, null, <next recordNum>, null, null, null, null, null)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void setCollection(int collection) {
		if (!dontUpdate) {
			try {
				PreparedStatement setCollection = jdbc
						.lookupPS("INSERT INTO raw_item_facet VALUES(?, ?);");
				setCollection.setInt(1, recordNum());
				setCollection.setInt(2, collection);
				jdbc.SQLupdate(setCollection, "Set item's collection");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			Util.print("INSERT INTO raw_item_facet VALUES(" + recordNum()
					+ ", " + collection + ");");
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attrs) {
		assert Util.ignore(qName);
		// Util.print("startElement: " + qName + " " + uri + " " + localName);
		if (localName.equals("record")) {
			if (!isInRecord) {
				// loc-grabill.xml has nested <record><marc:record> tags, so
				// don't create two records
				isInRecord = true;
				newItem();
				if (set != null) {
					try {
						Field field = getField("Collection", Field.FACET);
						int[] facets = getFacets(field, set);
						assert dontUpdate || facets != null : field + " '"
								+ set + "'";
						assert facets.length == 1 : "'" + set + "'";
						setCollection(facets[0]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else if (localName.equals("datafield") && uri.equals(namespaceMARC)) {
			subFacetTable.clear();
			// ind1 = attrs.getValue("ind1");
			// ind2 = attrs.getValue("ind2");
			tag = attrs.getValue("tag");
		} else if (localName.equals("controlfield")
				&& uri.equals(namespaceMARC)) {
			subFacetTable.clear();
			// ind1 = attrs.getValue("ind1");
			// ind2 = attrs.getValue("ind2");
			tag = attrs.getValue("tag");
			data = new StringBuffer();
		} else if (localName.equals("request")) {
			// Initial ListRecords request will specify the set. (Subsequent
			// requests with a ResumptionToken won't specify set, but it will be
			// the same one.)
			String setSpec1 = attrs.getValue("set");
			if (setSpec1 != null) {
				set = setNames.get(setSpec1);
				// Util.print(setSpec + " => " + set);
				assert set != null;
			}
		} else if (localName.equals("setSpec") || localName.equals("setName")) {
			// These are only used as pairs to remember the association between
			// the short name and the descriptive name. Only the request/set
			// above associates records with sets.
			data = new StringBuffer();
		} else if (isInRecord && localName.equals("subfield")
				&& uri.equals(namespaceMARC)) {
			subfield = attrs.getValue("code");
			data = new StringBuffer();
		} else if (isInRecord && uri.equals(namespaceDC)) {
			// tag = qName;
			data = new StringBuffer();
		}
	}

	// private String whereAmI() {
	// return "tag=" + tag + "; subfield=" + subfield + "; data=" + data;
	// }

	@Override
	public void endElement(String uri, String localName, String qName) {
		assert Util.ignore(qName);

		if (isInRecord) {
			// Util.print(tag+" "+subfield+" "+localName+"
			// "+(data==null?"":data.toString()));
			if ("010".equals(tag) && "a".equals(subfield)
					&& localName.equals("subfield")) {
				marc010 = data.toString();
			} else if ("001".equals(tag))
				marc001 = data.toString();
		}

		if (localName.equals("record"))
			isInRecord = false;
		if (data != null && data.length() > 1) {
			String value = trim(data.toString());
			if (localName.equals("setSpec")) {
				setSpec = value;
			} else if (localName.equals("setName")) {
				setNames.put(setSpec, value);
			} else {
				Field field = null;
				if (localName.equals("subfield") && uri.equals(namespaceMARC))
					field = getField();
				else if (uri.equals(namespaceDC))
					field = getDCfield(localName);
				else {
					field = getField(localName, Field.FACET);
					assert field.facetType == -1 : "Test whether this is ever needed: "
							+ field;
				}
				if (field != null) {
					// Util.print("endElement " + recordNum + " " + field + " "
					// + value);
					try {
						switch (field.type) {
						case Field.ATTRIBUTE:
						case Field.FUNCTIONAL_ATTRIBUTE:
							PreparedStatement ps = jdbc.lookupPS("SELECT "
									+ field.name
									+ " FROM item WHERE record_num = ?");
							ps.setInt(1, recordNum());
							String oldValue = jdbc.SQLqueryString(ps,
									"Get old attribute value");

							// Temporary for LoC
							if (field.name.equalsIgnoreCase("URI")) {
								storeBogus();
							}

							if (oldValue != null) {
								if (field.type == Field.FUNCTIONAL_ATTRIBUTE) {

									// Temporary for LoC
									if (!field.name.equalsIgnoreCase("URI"))

										Util.err("Multiple values for "
												+ field.name + ": " + oldValue
												+ " " + value);

									// If there's a clash, go with the first
									// value. This is the right thing for
									// multiple MARC 856 fields, at least,
									// according to Caroline Arms.
									return;
								} else {
									value = oldValue + "\n" + value;
								}
							}
							// if (field.name.equals("URI")) {
							// // handleURI(value);
							// ResultSet duplicates = lookupItem(value);
							// while (duplicates.next()) {
							// Util.err("Multiple items with URI: "
							// + value);
							// // int duplicate = duplicates.getInt(1);
							// // String[] tables = { "item",
							// // "raw_item_facet" };
							// // for (int i = 0; i < tables.length; i++)
							// // db("DELETE FROM " + tables[i]
							// // + " WHERE record_num = "
							// // + duplicate);
							// // updateImageID(duplicate);
							// }
							// }

							PreparedStatement updateItem = jdbc
									.lookupPS("UPDATE item SET " + field.name
											+ " = ? WHERE record_num = ?");
							updateItem.setString(1, value);
							updateItem.setInt(2, recordNum);
							db(updateItem, "Update item");
							break;
						// case Field.ATTRIBUTE:
						// db("REPLACE INTO " + field.name + " VALUES("
						// + recordNum + "," + JDBCSample.quote(value)
						// + ", '');");
						// break;
						case Field.FACET:
							if (field.facetType >= 0) {
								int[] facets = getFacets(field, value);
								if (facets != null) {
									for (int i = 0; i < facets.length; i++) {
										int facet = facets[i];
										checkBogusPlace(field, value);
										addItemFacet(recordNum(), facet);
									}
								}
							}
							break;
						default:
							assert false : field.type;
							break;
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					// Util.print("...endElement ");
				}
			}
		}
		data = null;
	}

	void checkBogusPlace(Field field, String value) throws SQLException {
		if (field == getField("Places", Field.FACET) && value.charAt(0) == '1') {
			storeBogus();
		}
	}

	void storeBogus() throws SQLException {
		// Util.print("mm " + recordNum()+" " + marc001);
		PreparedStatement ps = jdbc
				.lookupPS("INSERT INTO bogus VALUES (?, ?, ?, ?, ?, ?, ?)");
		ps.setString(1, marc001);
		ps.setString(2, marc010);
		ps.setString(3, tag);
		ps.setString(4, subfield);
		ps.setString(5, data.toString());
		ps.setInt(6, recordNum());
		ps.setString(7, setSpec);
		db(ps, "bogus");
	}

	// private void updateImageID(int old) {
	// if (!dontUpdate) {
	// try {
	// PreparedStatement updateImageID = jdbc.lookupPS("UPDATE images SET
	// record_num
	// = ? WHERE record_num = ?");
	// updateImageID.setInt(1, recordNum());
	// updateImageID.setInt(2, old);
	// jdbc.SQLupdate(updateImageID, "Set new record_num for image");
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// } else
	// Util.print("UPDATE images SET record_num = " + recordNum()
	// + " WHERE record_num = " + old + "");
	// }

	// private ResultSet lookupItem(String uri) {
	// ResultSet rs = null;
	// try {
	// PreparedStatement lookupItem = jdbc
	// .lookupPS("SELECT record_num FROM item WHERE URI = ? AND record_num !=
	// ?");
	// lookupItem.setString(1, uri);
	// lookupItem.setInt(2, recordNum());
	// rs = jdbc.SQLquery(lookupItem, "Lookup item from URI");
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// return rs;
	// }

	@Override
	public void characters(char[] ch, int start, int length) {
		// StringBuffer temp = new StringBuffer();
		// temp.append(ch, start, length);
		// Util.print("characters: " + temp);
		if (data != null) {
			data.append(ch, start, length);
		}
	}

	private String trim(String value) {
		// Get rid of any characters before the first word
		// String result = value.replaceAll("\\A\\W*", "");
		String result = value.replaceAll("\\A.*?\\b", "");

		// Get rid of any characters after the last word (but retain ')')
		// result = result.replaceAll("[\\W&&[^\\)]]*\\z", "");
		result = result.replaceAll("\\b(?:[^\\)]\\B)*\\z", "");
		// if (!result.equals(value))
		// Util.print(value + " => " + result);
		return result;
	}

	/**
	 * @param field
	 * @param value
	 * @return the leaf facets that field.value map to
	 * @throws SQLException
	 */
	int[] getFacets(Field field, String value) throws SQLException {
		String[][] hierValues = hierValues(field, value);
		int[] result = null;
		if (hierValues.length > 0) {
			field = hierField(field);
			for (int p = 0; p < hierValues.length; p++) {
				String[] hierValue = hierValues[p];
				// Util.printDeep(hierValue);
				if (hierValue != null && hierValue.length > 0) {
					int[] topLevel = getFacets(field, hierValue[0], field
							.getFacetType());
					if (topLevel != null) {
						for (int level = 1; level < hierValue.length; level++) {
							int[] facets = null;
							for (int i = 0; i < topLevel.length; i++) {
								facets = Util.append(facets, getFacets(field,
										hierValue[level], topLevel[i]));
							}
							topLevel = facets;
						}
					}
					result = Util.append(result, topLevel);
				}
			}
		} else
			result = new int[0];
		// Util.print("...getFacets ");
		return result;
	}

	private Field hierField(Field f) {
		if (f.name.equals("043 Places"))
			f = getField("Places", Field.FACET);
		else if (f.name.equals("045 Date"))
			f = getField("Date", Field.FACET);
		return f;
	}

	/**
	 * @param value
	 *            facet name
	 * @param key
	 *            either "z" for Places or "corp" for Specific Organizations
	 * @return ancestor facets
	 */
	private String[] hierValuesInternal(String value, String key) {
		String[] result = subFacetTable.get(key);
		// if (result != null && !result[result.length - 1].equals(value)) {
		// addToHierarchy(value, result[result.length - 1], table);
		// }
		if (result == null || !result[result.length - 1].equals(value)) {
			result = (String[]) Util.endPush(result, value, String.class);
			subFacetTable.put(key, result);
		}
		return result;
	}

	private static final Pattern yearPattern = Pattern.compile("\\d{4}");

	private boolean isYear(String s) {
		return yearPattern.matcher(s).matches();
	}

	/**
	 * @param value
	 *            arbitrary string representing a date
	 * @return ["20th century", "1990s", "1996", "12", "31"]
	 * @throws SQLException
	 */
	private String[] hierDateValues(String value) throws SQLException {
		String[] result = { value };
		String parentName = null;
		String[] date = value.split("-");
		if (date.length == 1) {
			date = value.split("/");
			if (date.length == 1) {
				if (value.length() == 4) {
					parentName = value.substring(0, 3) + "0s";
				} else if (value.length() == 5 && value.endsWith("0s")) {
					try {
						parentName = century(value.substring(0, 2));
					} catch (java.lang.NumberFormatException e) {
						Util.err(value);
						e.printStackTrace();
					}
				}
			} else {
				String year = date[date.length - 1];
				if (year.length() == 2) {
					year = "19" + year;
				}
				if (year.length() == 4) {
					if (date.length == 2)
						parentName = year;
					else if (date.length == 3)
						parentName = date[0] + "/" + year;
				}
			}
		} else if (date.length == 2 && isYear(date[0]) && isYear(date[1])) {
			if (date[0].substring(0, 3).equals(date[1].substring(0, 3))) {
				parentName = date[0].substring(0, 3) + "0s";
			} else if (date[0].substring(0, 2).equals(date[1].substring(0, 2))) {
				parentName = century(date[0].substring(0, 2));
			} else {
				int century1 = Integer.parseInt(date[0].substring(0, 2));
				int century2 = Integer.parseInt(date[1].substring(0, 2));
				if (century1 + 1 == century2) {
					parentName = century(date[0].substring(0, 2)).substring(0,
							4)
							+ "-" + century(date[1].substring(0, 2));
				}
			}
		}
		if (parentName != null) {
			result = (String[]) Util.append(hierDateValues(parentName), result,
					String.class);
		}
		// Util.print("hierDateValues " + value + " => " + result);
		return result;
	}

	private Hashtable<String, String> getPlaceAbbrevs() {
		if (placeAbbrevs == null) {
			placeAbbrevs = new Hashtable<String, String>();
			for (int i = 0; i < abbrevs.length; i++) {
				String[] x = abbrevs[i].split(";");
				placeAbbrevs.put(x[0].toLowerCase(), x[0]);
				String[] abbrs = x[1].split(":");
				for (int j = 0; j < abbrs.length; j++)
					placeAbbrevs.put(abbrs[j].toLowerCase(), x[0]);
			}
		}
		return placeAbbrevs;
	}

	private String getName(int facet) throws SQLException {
		String result = getFacetName(facet);
		if (result == null)
			result = getFacetTypeName(facet);
		return result;
	}

	private String getFacetName(int facet_id) throws SQLException {
		PreparedStatement ps = jdbc
				.lookupPS("SELECT name FROM raw_facet WHERE facet_id = ?");
		ps.setInt(1, facet_id);
		String result = jdbc.SQLqueryString(ps, "lookup name");
		return result;
	}

	private String getFacetTypeName(int facet_type_id) throws SQLException {
		PreparedStatement ps = jdbc
				.lookupPS("SELECT name FROM raw_facet_type WHERE facet_type_id = ?");
		ps.setInt(1, facet_type_id);
		String result = jdbc.SQLqueryString(ps, "lookup name");
		return result;
	}

	private int getFacetType(int facet_id) throws SQLException {
		for (int facet_type_id = facet_id; facet_type_id > 0; facet_type_id = getParentFacet(facet_type_id)) {
			facet_id = facet_type_id;
		}
		return facet_id;
	}

	private int getParentFacet(int facet_id) throws SQLException {
		PreparedStatement ps = jdbc
				.lookupPS("SELECT parent_facet_id FROM raw_facet WHERE facet_id = ?");
		ps.setInt(1, facet_id);
		return jdbc.SQLqueryInt(ps, "lookup parent id");
	}

	/**
	 * This just looks up atomic locations, not 'Ogden, UT'
	 * 
	 * @param value
	 * @return e.g. [["North America", "United States", "Utah"]]
	 */
	private String[][] hierPlaces(String value) {
		String[][] result = { { value } };
		if (moves != null) {
			String ancestorss = moves.get("Places -- " + value);
			if (ancestorss != null) {
				String[] ancestors = Util.splitSemicolon(ancestorss);
				result = new String[ancestors.length][];
				for (int i = 0; i < ancestors.length; i++) {
					String[] ancestorFacets = ancestors[i].split(" -- ");
					assert ancestorFacets[0].equals("Places") : value + " "
							+ ancestorss;
					result[i] = (String[]) Util.subArray(ancestorFacets, 1,
							String.class);
				}
			}
		}
		// String broad;
		// PreparedStatement ps = jdbc.lookupPS("SELECT broader FROM
		// loc.places_hierarchy WHERE term = ?");
		// ps.setString(1, "Places -- " + result[0]);
		// try {
		// while ((broad = jdbc.SQLqueryString(ps, "lookup broader")) != null) {
		// result = (String[]) Util.push(result, broad, String.class);
		// ps.setString(1, result[0]);
		// }
		// } catch (AssertionError e) {
		// // Skip if there are multiple parents for term
		// // in
		// // the
		// // places hierarchy
		// }
		// if (result.length>1)
		// Util.print("hierPlaces " + Util.join(result));
		return result;
	}

	/**
	 * @param value
	 *            e.g. "downtown (ogden, utah)" or "ogden (utah)" or "ogden,
	 *            utah" 'Victoria Falls (Zambia and Zimbabwe)' 'Keystone, W. Va'
	 * @return [["North America", "United States", "Utah", "Ogden", "downtown"]]
	 *         [[Africa, Zambia, Victoria Falls ], [Africa, Zimbabwe, Victoria
	 *         Falls ]] [[North America, United States, West Virginia,
	 *         Keystone]]
	 */
	private String[][] trimPlaces(String value) {
		String[][] result = { { value } };
		// group(1) is everything up to the first comma or parenthesis
		// group(2) is everything after the first comma or paren up to the next
		// close parenthesis, colon, or the end
		Pattern p = Pattern.compile("\\b([^(]+?)(?:,|\\()(.*?)(\\z|:|\\))");
		Matcher m = p.matcher(value);
		if (m.find()) {
			String narrow = m.group(1); // e.g. "downtown"
			String broadAbbrev = m.group(2);
			if (broadAbbrev != null) {
				// broadAbbrev = broadAbbrev.toLowerCase();
				broadAbbrev = broadAbbrev.replaceAll("\\.", "");
				String[] multipleParents = broadAbbrev.split(" [aA][nN][dD] ");
				if (multipleParents.length > 1) {
					String[][] x = new String[multipleParents.length][];
					x[0] = result[0];
					result = x;
				}
				for (int j = 0; j < multipleParents.length; j++) {
					String[] broads = multipleParents[j].split(","); // e.g.
					// ["Ogden",
					// "UT"]
					// or
					// ["UT"]
					String lastBroad = broads[broads.length - 1].trim()
							.toLowerCase(); // e.g. UT
					String[] broad = lookupLocation(lastBroad); // e.g.
					// "Location --
					// North America
					// -- United
					// States --
					// Utah"
					if (broad == null)
						broad = lookupLocation(m.group(2));
					// If broad is still null, it might be "Russia
					// (Federation)", which isn't a place so we should ignore it
					if (broad != null) {
						result[j] = broad;
						for (int i = broads.length - 2; i >= 0; i--) {
							// use loop in case broads is ["Ogden", "UT", "US"]
							result[j] = (String[]) Util.endPush(result[j],
									broads[i].trim(), String.class);
						}
						result[j] = (String[]) Util.endPush(result[j], narrow,
								String.class); // e.g. ["Location", "North
						// America", "United States",
						// "Utah", "Ogden", "downtown"]
					}
					// if (result[j] != null) {
					// PreparedStatement ps = jdbc.lookupPS("SELECT broader FROM
					// loc.places_hierarchy WHERE term = ?");
					// ps.setString(1, result[j][0]);
					// try {
					// while ((broad = jdbc.SQLqueryString(ps,
					// "lookup broader")) != null) {
					// assert false : Util.valueOfDeep(result[j])
					// + " " + broad;
					// result[j] = (String[]) Util.push(result[j],
					// broad, String.class);
					// ps.setString(1, result[j][0]);
					// }
					// } catch (AssertionError e) {
					// // Skip if there are multiple parents for term
					// // in
					// // the
					// // places hierarchy
					// }
					// }
				}
			}
		}
		result = (String[][]) Util.delete(result, null, String[].class);
		// if (result[0].length > 1)
		// Util.err("trimPlaces '" + value + "' => "
		// + Util.valueOfDeep(result) + " '" + m.group(1) + "' '"
		// + m.group(2) + "'");
		return result;
	}

	/**
	 * This just looks up atomic locations, not 'Ogden, UT'
	 * 
	 * @param possibleLocation
	 * @return e.g. [["North America", "United States", "Utah"]]
	 */
	private String[] lookupLocation(String possibleLocation) {
		String unabbreviated = getPlaceAbbrevs().get(
				possibleLocation.toLowerCase());
		String[] result = null;
		if (unabbreviated != null) {
			result = new String[1];
			result[0] = unabbreviated;
		}
		if (result == null) {
			String[][] hierPlaces = hierPlaces(possibleLocation);
			if (hierPlaces.length == 1 && hierPlaces[0].length > 1)
				result = hierPlaces[0];
		}
		return result;
	}

	/**
	 * The ancestor facets must not include the facet type; this is encoded in
	 * the field.
	 * 
	 * @param field
	 *            raw field from XML file
	 * @param value
	 *            raw string from XML file
	 * @return [<ancestor facets alternetive 1>, ...]
	 * @throws SQLException
	 */
	private String[][] hierValues(Field field, String value)
			throws SQLException {
		if (field.getFacetType() < 0)
			// return new String[0][];
			assert false;
		// Util.print("hierValues " + field + " " + value);
		String[][] result = { { value } };
		if (field.name.equals("Places")) {
			result[0] = hierValuesInternal(value, "z");
			if (result[0].length == 1)
				result = trimPlaces(result[0][0]);
			// Util.printDeep(result);
			if (result[0].length == 1)
				result = hierPlaces(result[0][0]);
		} else if (field.name.equals("Date of Creation")
				|| field.name.equals("Date")) {
			value = trimDate();
			// String[] date = value.split(" ");
			// if (date.length == 4 && date[0].equalsIgnoreCase("between")
			// && date[2].equalsIgnoreCase("and")) {
			// value = date[1] + "-" + date[3];
			// } else if (date.length == 3 &&
			// date[1].equalsIgnoreCase("or")) {
			// value = date[0] + "-" + date[2];
			// }
			result[0] = hierDateValues(value);
		} else if (field.name.equals("Specific Organizations")) {
			result[0] = hierValuesInternal(value, "corp");
		} else if (field.name.equals("043 Places")) {
			result[0] = lookupGAC(trim(value));
		} else if (field.name.equals("045 Date")) {
			// Util.print("045 code = '" + value + "'");
			boolean OK = false;
			if (value.length() == 4) {
				// see http://www.oclc.org/bibformats/en/0xx/045.shtm
				String lowRange = code045(value.substring(0, 2));
				String hiRange = code045(value.substring(2, 4));
				if (lowRange != null && hiRange != null) {
					OK = true;
					if (!value.substring(0, 2).equals(value.substring(2, 4))) {
						lowRange = lowRange.split("-")[0] + "-"
								+ hiRange.split("-")[1];
					}
					value = lowRange;
				}
			} else {
				// see
				// http://www.loc.gov/marc/bibliographic/ecbdnumb.html#mrcb045
				Pattern p = Pattern
						.compile("\\b([cd])(\\d{4})(\\d{2})?(\\d{2})?(\\d{2})?\\z");
				Matcher m = p.matcher(value);
				if (m.find()) {
					String era = m.group(1);
					String year = m.group(2);
					String month = m.group(3);
					String day = m.group(4);
					String newValue = (day != null) ? month + "/" + day + "/"
							+ year : (month != null) ? month + "/" + year
							: year;
					if (era.charAt(0) == 'd') {
						value = newValue;
						OK = true;
					} else if (era.charAt(0) == 'c') {
						value = newValue + " B.C.";
						OK = true;
					}
				}
				if (OK)
					result[0] = hierDateValues(value);
				else {
					Util.err("Bad 045 Date Code " + value);
					result[0] = new String[0];
				}
			}
		} else if (field.name.equals("Subject")) {
			String[] subjects = value.split("\\.?; ?");
			result = new String[subjects.length][];
			for (int i = 0; i < result.length; i++) {
				if (i < subjects.length)
					result[i] = subjects[i].split("-{2,}");
				for (int j = 0; j < result[i].length; j++) {
					result[i][j] = result[i][j].trim();
					String s = result[i][j];
					s = rename(s);
					if (s.length() == 0) {
						Util.err("Ignoring zero length facet name in '"
								+ result[i][j] + "' in '"
								+ Util.join(result[i], " -- ") + "' in '"
								+ value + "'");
						result[i] = null;
						break;
					} else if (s.charAt(0) == '-'
							|| (s.charAt(s.length() - 1) == '-' && result[i].length > j + 1)
							|| s.indexOf("-pitts") >= 0
							|| s.indexOf("-penns") >= 0
							|| (s.indexOf("enns") != s.indexOf("ennsylvania"))
							|| (s.indexOf("itts") != s.indexOf("ittsburgh"))) {
						Util.print("WARNING ignoring suspicious facet name '"
								+ s + "' in '" + result[i][j] + "' in '"
								+ Util.join(result[i], " -- ") + "' in '"
								+ value + "'");
						result[i] = null;
						break;
					}
					int k = Util.member(result[i], s, j + 1);
					if (k > 0) {
						if (s.equalsIgnoreCase("Pennsylvania")
								&& result[i][k - 1]
										.equalsIgnoreCase("Pittsburgh")) {
							result[i] = (String[]) Util.deleteIndex(result[i],
									k, String.class);
						} else {
							Util.print("WARNING repeated facet name '"
									+ result[i][j] + "' in '"
									+ Util.join(result[i], " -- ") + "' in '"
									+ value + "'");
							result[i] = (String[]) Util.subArray(result[i], 0,
									j, String.class);
							Util.print("...treating as: ["
									+ Util.join(result[i], ",") + "] ");
						}
					}
					if (!result[i][0].equals("Places")
							&& getPlaceAbbrevs().get(s.toLowerCase()) != null) {
						String[] place = (String[]) Util.push(Util.subArray(
								result[i], j, String.class), "Places",
								String.class);
						if (j > 0) {
							result = (String[][]) Util.endPush(result, place,
									String[].class);
							result[i] = (String[]) Util.subArray(result[i], 0,
									j - 1, String.class);
						} else
							result[i] = place;
					}

					Matcher m = cityState.matcher(s);
					if (m.find()) {
						String state = getPlaceAbbrevs().get(
								m.group(3).toLowerCase());
						if (state != null) {
							state = "Places--" + state;
							String city = m.group(2);
							if (city != null && city.length() > 0)
								state += "--" + city;
							if (cities != null
									&& cities
											.contains(m.group(1).toLowerCase())) {
								state += "--" + m.group(1);
								result[i] = state.split("--");
							} else {
								result[i][j] = m.group(1);
								result = (String[][]) Util.endPush(result,
										state.split("--"), String[].class);
							}
						} else {
							// Util.print("Not a state: " + s);
							Pattern p2 = Pattern
									.compile("(.+)\\s*\\((.+)\\)\\z");
							Matcher m2 = p2.matcher(s);
							m2.find();
							String[] augmented = null;
							result[i][j] = m2.group(1);
							if (j > 0)
								augmented = (String[]) Util.subArray(result[i],
										0, j - 1, String.class);
							augmented = (String[]) Util.endPush(augmented, m2
									.group(2), String.class);
							augmented = (String[]) Util.append(augmented, Util
									.subArray(result[i], j, String.class),
									String.class);
							result[i] = augmented;
						}
					}
				}
			}
			for (int i = 0; i < result.length; i++) {
				if (result[i] != null && !result[i][0].equals("Places")
						&& Util.member(result[i], "Pennsylvania", 1) > 0)
					Util.print("Should this be a place? ["
							+ Util.join(result[i], ",") + "] " + value);
			}
		}
		// if (value.indexOf("stanbul") >= 0)
		// Util.print("hierValues " + value + " => "
		// + Util.join(result, " -- "));
		return result;
	}

	/**
	 * E.g. "Downtown (Pittsburgh, Pa.)" where Pa is in abbrevs. Only adds
	 * Downtown if it is in cities.
	 */
	private Pattern cityState = Pattern
			.compile("\\s*(.*?)\\,?\\s*\\((\\w*?)\\.?,?\\s*(\\w+)\\.?\\)\\z");
	private String marc001;
	private String marc010;

	private String trimDate() {
		return trimDate(data.toString());
	}

	private final static String[] months = { "jan", "feb", "mar", "apr", "may",
			"jun", "jul", "aug", "sep", "oct", "nov", "dec", "jany", "feby",
			"mar", "apl", "may", "jun", "jul", "augt", "sepr", "octr", "novr",
			"decr", "january", "february", "march", "april", "may", "june",
			"july", "august", "september", "october", "november", "december",
			"sept" };

	private int month(String monthName) {
		int month = Util.member(months, monthName.toLowerCase());
		if (month == 36)
			month = 9;
		else if (month >= 0)
			month = 1 + month % 12;
		return month;
	}

	private String trimDate(final String value) {
		// Get rid of prefixes in front of the first digit: 'ca', 'c', '(c)',
		// '(C)', 'approximately', etc.
		// possibly followed by '.' and whitespace
		String result = value
				.replaceAll(
						"\\b(?:ca|c|\\([cC]\\)|approximately|between|before|after|photographed|published)\\.?\\s*(\\d)",
						"$1");

		// Get rid of (?) suffixes
		result = result.replaceAll("\\(\\?\\)", "");
		result = result.replaceAll("\\d\\?", "");

		// '[19]75' => '1975', '19]75' => '1975'
		result = result.replaceAll("\\[((?:17)|(?:18)|(?:19)|(?:20))\\]", "$1");
		result = result.replaceAll("\\b((?:17)|(?:18)|(?:19)|(?:20))\\]", "$1");
		result = result.replaceAll("19\\]", "19");

		Pattern p = Pattern.compile("\\b(\\d{2})--\\??");
		Matcher m = p.matcher(result);
		while (m.find()) {
			result = century(m.group(1));
			m.reset(result);
		}

		// '1920's' => '1920s'
		result = result.replaceAll("\\b(\\d{4})'s\\z", "$1s");

		// '1942/1945' => '1942-1945'
		result = result.replaceAll("\\b(\\d{4})/(\\d{4})\\z", "$1-$2");

		// '1945-4-15' => '4/15/1945'
		result = result.replaceAll("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\z",
				"$2/$3/$1");

		// '1945-6' => '1945-1946'
		result = result.replaceAll("\\b(\\d{3})(\\d)-(\\d)\\z", "$1$2-$1$3");
		// '1945-46' => '1945-1946'
		result = result.replaceAll("\\b(\\d{2})(\\d{2})-(\\d{2})\\z",
				"$1$2-$1$3");

		// '194[5]' => '1945'
		result = result.replaceAll("\\b(\\d{3})\\[(\\d)\\]", "$1$2");

		// '194-?', '194-' => '1940s'
		result = result.replaceAll("\\b(\\d{3})-\\??", "$10s");

		p = Pattern.compile("\\b(\\d{4})\\s*(?:\\W|to|and|adn|or)\\s*(\\d{4})");
		m = p.matcher(result);
		if (m.find()) {
			String start = m.group(1);
			String end = m.group(2);
			if (end.compareTo(start) < 0) {
				String s = end;
				end = start;
				start = s;
			}
			result = start + "-" + end;
		}

		p = Pattern.compile("(\\d{4})\\s*([a-zA-Z]{3,})\\.?\\s*(\\d{0,2})");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(2);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(3);
				if (day.length() == 0)
					result = month + "/" + m.group(1);
				else
					result = month + "/" + day + "/" + m.group(1);
				m.reset(result);
			}
		}

		p = Pattern
				.compile("\\b(\\d{0,2})\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{4})\\z");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(2);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(1);
				String year = m.group(3);
				if (day.length() == 0)
					result = month + "/" + year;
				else
					result = month + "/" + day + "/" + year;
				m.reset(result);
				// Util.print(value + " " + result);
			}
		}

		// 'Nov. 20, 1908' => '11/20/1908'
		p = Pattern
		// .compile("\\b\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\z");
				// .compile("\\b([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\z");
				.compile("\\b\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\D");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(1);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(2);
				String year = m.group(3);
				if (day.length() == 0)
					result = month + "/" + year;
				else
					result = month + "/" + day + "/" + year;
				m.reset(result);
				// Util.print(value + " " + result);
			}
		}

		// 'foo, 1945' => '1945'
		result = result.replaceAll("\\b\\D+,(\\s*\\d{4})", "$1");

		// for Chartres
		p = Pattern.compile("\\b(\\d{4})(\\d{2})(\\d{2})\\z");
		m = p.matcher(result);
		if (m.find()) {
			String year = m.group(1);
			String month = m.group(2);
			String day = m.group(3);
			if (Integer.parseInt(month) <= 12 && Integer.parseInt(day) <= 31)
				result = year;
		}

		// if (!value.equals(result))
		// Util.print(value + " => " + result);
		result = trim(result);

		return result;
	}

	private String century(String prefix) {
		try {
			String suffix = "th century";
			int n = Integer.parseInt(prefix) + 1;
			if (n < 4 || n > 20) {
				switch (n % 10) {
				case 1:
					suffix = "st century";
					break;
				case 2:
					suffix = "nd century";
					break;
				case 3:
					suffix = "rd century";
					break;
				}
			}
			return n + suffix;
		} catch (NumberFormatException e) {
			Util.err(data.toString());
			e.printStackTrace();
			return prefix;
		}
	}

	private int[] getFacets(Field field, String value, int parent)
			throws SQLException {
		int[] result = null;
		int facetType = field.getFacetType();
		// Util.print("getFacets " + field + " " + value + " " + parent + " " +
		// facetType);
		if (parent < 0) {
			result = lookupFacets(value, facetType);
			parent = facetType;
		} else {
			int theResult = lookupFacet(value, parent);
			// if (theResult < 0) {
			// theResult = lookupFacet(value, facetType);
			// if (theResult > 0)
			// setParent(theResult, parent);
			// }
			if (theResult > 0)
				result = Util.push(result, theResult);
		}
		if (result == null) {
			// if (parent != facetType) {
			// if (Util.join(foo, " -- ").indexOf("Pearce, James
			// Alfred") >= 0)
			// Util.print("getFacets adding " + value + " " + parent + "["
			// + facetType + "] " + Util.join(foo, " -- "));
			int theResult = newFacet(value, parent);
			result = Util.push(result, theResult);
			// }
			// else
			// Util.err("getFacets not adding " + value + " " + parent + "["
			// + facetType + "] " + Util.join(foo, " -- "));
		}
		// if (result != null)
		// Util.print("..getFacets(" + value + ") => "
		// + getFacetName(result[0]));
		// else
		// Util.print("..getFacets(" + value + ") => null");
		return result;
	}

	private String code045(String code) {
		String result = null;
		int code1 = code.charAt(0) - 'a';
		int code2 = code.charAt(1) - '0';
		if (code.length() == 2 && code1 >= 0 && code1 <= 25
				&& (code.charAt(1) == '-' || (code2 >= 0 && code2 <= 9))) {
			if (code1 == 0) {
				assert code2 == 0;
				result = "before 2999 B.C.";
			} else if (code1 > 0 && code1 < 4) {
				int digit1 = 2 - (code1 - 1);
				int digit2 = 9 - code2;
				int end = 100 * (digit1 * 10 + digit2);
				int start = end + 99;
				result = start + "-" + end;
			} else if (code.charAt(1) == '-') {
				int digit1 = code1 - 4;
				int start = digit1 * 100;
				int end = start + 99;
				result = start + "-" + end;
			} else {
				int digit1 = code1 - 4;
				int digit2 = code2;
				int start = 10 * (digit1 * 10 + digit2);
				int end = start + 9;
				result = start + "-" + end;
			}
		}
		// Util.print("code 045 " + code + " => " + result);
		return result;
	}

	// private Map preparedStatements = new Hashtable();
	//
	// private PreparedStatement jdbc.lookupPS(String SQL) throws SQLException {
	// PreparedStatement ps = (PreparedStatement) preparedStatements.get(SQL);
	// if (ps == null) {
	// ps = jdbc.prepareStatement(SQL);
	// preparedStatements.put(SQL, ps);
	// }
	// return ps;
	// }

	/**
	 * @param name
	 *            known to be a place name
	 * @return <ancestor hierarchy>
	 */
	private String[] lookupGAC(String name) {
		String[] result = null;
		try {
			PreparedStatement lookupGAC = jdbc
					.lookupPS("SELECT broader FROM 043places WHERE term = ?");
			lookupGAC.setString(1, name);
			String place = jdbc.SQLqueryString(lookupGAC,
					"Lookup place name from GAC code");
			assert place != null : name;
			result = place.split(" -- ");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	int facetType(String name) throws SQLException {
		// Util.print("facetType " + name);
		int result = -1;
		PreparedStatement facetTypeQuery = jdbc
				.lookupPS("SELECT facet_type_id FROM raw_facet_type WHERE name = ?");
		facetTypeQuery.setString(1, name);
		result = jdbc.SQLqueryInt(facetTypeQuery, "Lookup facet type");
		return result;
	}

	// int newFacet(String name, Field field) {
	// return newFacet(name, field.getFacetType(this));
	// }

	// private int newFacet(String name, int facetType) throws SQLException {
	// return newFacet(name, facetType, facetType);
	// }

	private Field getDCfield(String tag1) {
		Field result = null;
		if (tag1 != null) {
			if (tag1.equals("identifier")) {
				result = getField("URI", Field.FUNCTIONAL_ATTRIBUTE);
			} else if (tag1.equals("publisher")) {
				result = getField("Publisher", Field.FACET);
			} else if (tag1.equals("format")) {
				result = getField("Subject", Field.FACET);
			} else if (tag1.equals("rights")) {
				result = getField("Rights", Field.FACET);
			} else if (tag1.equals("title")) {
				result = getField("Title", Field.ATTRIBUTE);
			} else if (tag1.equals("type")) {
				result = getField("Type", Field.FACET);
			} else if (tag1.equals("date")) {
				result = getField("Date", Field.FACET);
			} else if (tag1.equals("description")) {
				result = getField("Description", Field.ATTRIBUTE);
			} else if (tag1.equals("subject")) {
				result = getField("Subject", Field.FACET);
			} else if (tag1.equals("language")) {
				result = getField("Language", Field.FACET);
			} else if (tag1.equals("creator")) {
				result = getField("Creator", Field.FACET);
			} else if (tag1.equals("source")) {
				result = getField("Source", Field.FACET);
			} else {
				Util.print("Ignoring " + tag1 + " " + data.toString());
			}
		}
		return result;
	}

	// marc21 codes explained at http://www.loc.gov/marc/bibliographic/
	private Field getField() {
		if (tag != null) {
			if ("043".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return getField("043 Places", Field.FACET);
				}
			} else if ("045".indexOf(tag) >= 0) {
				if ("ab".indexOf(subfield) >= 0) {
					return getField("045 Date", Field.FACET);
				}
			} else if ("100,110,700".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return getField("Creator", Field.FACET);
				} else if ("f".indexOf(subfield) >= 0) {
					return getField("Date", Field.FACET);
				} else if ("t".indexOf(subfield) >= 0) {
					return getField("Title", Field.ATTRIBUTE);
				} else if ("bcdegpqx56".indexOf(subfield) >= 0) {
					return null;
				}
			} else if ("130,240,245,246,740".indexOf(tag) >= 0) {
				if ("abps".indexOf(subfield) >= 0) {
					return getField("Title", Field.ATTRIBUTE);
				} else if (subfield.equals("f")) {
					return getField("Date of Creation", Field.FACET);
				} else if (subfield.equals("h")) {
					return getField("Format", Field.FACET);
				} else if ("l".indexOf(subfield) >= 0) {
					return getField("Language", Field.FACET);
				} else if ("cign56".indexOf(subfield) >= 0) {
					return null;
				}
			} else if ("257,260".indexOf(tag) >= 0) {
				if ("ae".indexOf(subfield) >= 0) {
					return getField("Location of Publication", Field.FACET);
				} else if ("cg".indexOf(subfield) >= 0) {
					return getField("Date of Creation", Field.FACET);
				} else if ("bf6".indexOf(subfield) >= 0) {
					return null;
				}
			} else if (tag.equals("300")) {
				return getField("Description", Field.ATTRIBUTE);
			} else if (tag.equals("440") || tag.equals("490")) {
				return getField("Series", Field.ATTRIBUTE);
			} else if (tag.equals("520")) {
				return getField("Summary", Field.ATTRIBUTE);
			} else if ("500,504,505,511,518,545,586".indexOf(tag) >= 0) {
				return getField("Note", Field.ATTRIBUTE);
			} else if ("552".indexOf(tag) >= 0) {
				if (subfield.equals("o"))
					return getField("Note", Field.ATTRIBUTE);
				else
					return null;
			} else if (tag.equals("600")) {
				if (subfield.equals("a")) {
					return getField("Specific People", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("610") || tag.equals("710")) {
				if (subfield.equals("a") || subfield.equals("b")) {
					return getField("Specific Organizations", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("611")) {
				return subjectUsualSuspects();
			} else if ("650,653,654".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return getField("Subject", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("655,755".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return getField("Format", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("630".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return getField("Title", Field.ATTRIBUTE);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("651")) {
				if (subfield.equals("a")) {
					return getField("Places", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("656")) {
				if (subfield.equals("a")) {
					return getField("Occupation", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("657")) {
				if (subfield.equals("a")) {
					return getField("Function", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("662".indexOf(tag) >= 0) {
				if ("abcdfgh".indexOf(subfield) >= 0) {
					return getField("Places", Field.FACET);
				} else
					return null;
			} else if ("242,730".indexOf(tag) >= 0) {
				if ("at".indexOf(subfield) >= 0) {
					return getField("Title", Field.ATTRIBUTE);
				} else if ("f".indexOf(subfield) >= 0) {
					return getField("Date of Creation", Field.FACET);
				} else
					return null;
			} else if (tag.equals("752")) {
				if ("abcdfgh".indexOf(subfield) >= 0) {
					return getField("Places", Field.FACET);
				} else
					return null;
			} else if (tag.equals("856")) {
				if (subfield.equals("u")) {
					return getField("URI", Field.FUNCTIONAL_ATTRIBUTE);
				} else
					return null;
			} else if (("010,017,020,034,035,037,040,041,042,050,051,052,066,072,074,082,086,090,111,250,255,265,501,506,"
					+ "507,508,510,524,530,533,534,540,541,546,550,555,561,580,581,583,585,590,711,772,773,810,830,850,"
					+ "852,859,880").indexOf(tag) >= 0) {
				// Known ignorable
				return null;
			}
		}
		Util.print("Ignoring " + tag + subfield + " " + data.toString());
		return null;
	}

	private Field subjectUsualSuspects() {
		if (subfield.equals("z")) {
			return getField("Places", Field.FACET);
		} else if (subfield.equals("y")) {
			return getField("Date", Field.FACET);
		} else if (subfield.equals("v") || subfield.equals("x")) {
			return getField("Subject", Field.FACET);
		} else
			return null;
	}

	private static Hashtable<String, Field[]> fieldCache = new Hashtable<String, Field[]>();

	Field getField(String _name, int _type) {
		// if (Util.isMember(SaxHandler.ignoreFields, _name))
		// return null;
		Field[] fields = fieldCache.get(_name);
		if (fields == null) {
			fields = new Field[3];
			fieldCache.put(_name, fields);
		}
		if (fields[_type] == null) {
			fields[_type] = new Field(_name, _type);
		}
		return fields[_type];
	}

	enum DuplicateStatus {
		SAME, DIFFERENT, NA
	}

	void mergeDuplicateItems(String facetTypeNamesToMerge) throws SQLException {
		String[] toMerge = facetTypeNamesToMerge.split(",");
		int[] facetTypesToMerge = new int[toMerge.length];
		for (int i = 0; i < toMerge.length; i++) {
			facetTypesToMerge[i] = facetType(toMerge[i]);
		}
		jdbc.ensureIndex("item", "URI", "URI(51)");
		ResultSet pairs = jdbc
				.SQLquery("SELECT i1.record_num, i2.record_num, i1.URI FROM item i1, item i2 "
						+ "where i1.record_num < i2.record_num "
						+ "and i1.URI = i2.URI");
		Util.print(MyResultSet.nRows(pairs)
				+ " possible duplicate items to merge...");
		String descriptionFields = jdbc
				.SQLqueryString("SELECT itemDescriptionFields FROM globals");
		PreparedStatement getDescription = jdbc.lookupPS("SELECT CONCAT_WS("
				+ descriptionFields + ") FROM item WHERE record_num = ?");
		while (pairs.next()) {
			int item1 = pairs.getInt(1);
			int item2 = pairs.getInt(2);
			switch (isDuplicate(item1, item2, facetTypesToMerge, getDescription)) {
			case SAME:
				// Util.err("merge " + item1 + " " + item2 + " "
				// + pairs.getString(3));
				mergeItems(item1, item2);
				break;
			case DIFFERENT:
				Util.err(item1 + " and " + item2 + " have the same URI: "
						+ pairs.getString(3));
			}
		}
		Util.print("...merging items done.");
	}

	private DuplicateStatus isDuplicate(int item1, int item2,
			int[] facetTypesToMerge, PreparedStatement getDescription)
			throws SQLException {
		getDescription.setInt(1, item1);
		String desc1 = jdbc.SQLqueryString(getDescription, "Get description");
		getDescription.setInt(1, item2);
		String desc2 = jdbc.SQLqueryString(getDescription, "Get description");
		// Util.print("isDuplicate "+item1+" "+item2+" "+desc1);
		DuplicateStatus result = desc1 == null || desc2 == null ? DuplicateStatus.NA
				: desc1.equals(desc2) ? DuplicateStatus.SAME
						: DuplicateStatus.DIFFERENT;
		if (result == DuplicateStatus.SAME) {
			PreparedStatement getFacets1 = jdbc
					.lookupPS("SELECT facet_id dummy1 "
							+ "FROM raw_item_facet WHERE record_num = ? ORDER BY facet_id");
			getFacets1.setInt(1, item1);
			ResultSet rs1 = jdbc.SQLquery(getFacets1, "Get facets");

			// Need two copies since we need two ResultSets open simultaneously
			PreparedStatement getFacets2 = jdbc
					.lookupPS("SELECT facet_id dummy2 "
							+ "FROM raw_item_facet WHERE record_num = ? ORDER BY facet_id");
			getFacets2.setInt(1, item2);
			ResultSet rs2 = jdbc.SQLquery(getFacets2, "Get facets");
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
			jdbc.close(rs1);
			jdbc.close(rs2);
		} else {
			// Util.print("Descriptions differ:\n" + desc1 + "\n\n" + desc2);
		}
		return result;
	}

	boolean scrollToUnmergedFacet(ResultSet rs, int[] facetTypesToMerge)
			throws SQLException {
		boolean result = rs.next();
		if (result
				&& Util.isMember(facetTypesToMerge, getFacetType(rs.getInt(1))))
			result = scrollToUnmergedFacet(rs, facetTypesToMerge);
		return result;
	}

	class Field {
		static final int FACET = 0;

		private static final int ATTRIBUTE = 1;

		private static final int FUNCTIONAL_ATTRIBUTE = 2;

		final String name;

		final int type;

		final int facetType;

		Field(String _name, int _type) {
			name = _name;
			type = _type;
			int ft = -1;
			if (type == FACET)
				try {
					ft = facetType(name);
					if (ft < 0 && !name.equals("043 Places")
							&& !name.equals("045 Date")) {
						Util.err("Warning: Can't find '" + name
								+ "' in raw_facet_type, from '" + data + "'");
						// Util.printStackTrace();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			facetType = ft;
		}

		int getFacetType() {
			assert type == FACET;
			// if (facetType < 0) {
			// facetType = handler.facetType(name);
			// }
			return facetType;
		}

		@Override
		public String toString() {
			return "<Field " + name + ">";
		}
	}
}
