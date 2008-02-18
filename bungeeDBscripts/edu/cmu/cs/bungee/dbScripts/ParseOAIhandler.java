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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Populate database from OAI format XML files.
 * 
 */
final class ParseOAIhandler extends DefaultHandler {

	boolean verbose;

	// static final String imageFileExtension = ".jpeg";
	//
	// private static final String imageMIMEtype = "jpeg";

	private ConvertFromRaw converter;

	static final boolean dontUpdate = false;

	private int recordNum;

	private int facetID;

	private String tag = null;

	private String subfield = null;

	StringBuffer data;

	private Hashtable subFacetTable = new Hashtable();

	// private final static String[] attrTables = { "title", "description"
	// "series", "summary", "note"
	// };

	// private final static String[] attrSeparators = { "\\n", "; ", "\\n",
	// "\\n",
	// "\\n" };

	// /**
	// * This helps recognize tags that would otherwise be classified as
	// 'Subject'
	// * to be 'Places'.
	// */
	// private final static String[] places = { "downtown", "pittsburgh",
	// "homestead", "oakland", "hill district", "south side",
	// "north side", "east liberty", "o'hara township", "fifth avenue",
	// "shadyside", "penn avenue", "mckees rocks", "rankin",
	// "lawrenceville", "highland park", "forbes avenue",
	// "mount washington", "harmony", "squirrel hill", "schenley park",
	// "washington, dc", "washington, d.c.", "washington, d.c" };
	//
	// private final static String[] toPromote = {
	// "Subject -- Places;Places",
	// "Subject -- jpeg;Type -- image -- jpeg",
	// "Subject -- Color;Format -- Color",
	// "Subject -- Maps;Format -- Maps",
	// "Subject -- Hand-colored;Format -- Color -- Hand-colored",
	// "Subject -- Cartoons;Format -- Cartoons",
	// "Subject -- Maps, Manuscript;Format -- Maps -- Maps, Manuscript",
	// // "Subject -- Pictures;Format -- Pictures",
	// "Subject -- Pictures;Format",
	// "Subject -- Albums;Format -- Albums",
	// "Subject -- Reproductions;Format -- Reproductions",
	// "Subject -- Stereographs;Format -- Stereographs",
	// "Subject -- Design drawings;Format -- Design drawings",
	// "Subject -- Cityscapes;Format -- Cityscapes",
	// "Subject -- People;Kinds of People",
	// // "Subject -- Organizations;Kinds of Organizations",
	// "Subject -- Objects -- Built environment -- Architectural & site
	// components;Built environment",
	// "Subject -- Objects -- Built environment -- Facilities;Built
	// environment",
	// "Subject -- Bands;Subject -- Music -- Bands",
	// "Subject -- Towboats -- Interiors;Subject -- Towboats",
	//
	// "Places -- North America -- United States;Places",
	// "Places -- North America;Places",
	//
	// "Subject -- Baseball team -- St. Louise Cardinals -- Uniforms;Subject --
	// Objects -- Clothing & dress -- Uniforms",
	// "Subject -- Equitable Gas Company -- Disasters;Subject -- Disasters",
	// "Subject -- Railroads -- Accidents;Subject -- Accidents",
	// "Subject -- World War, 1939-1945 -- Women;Subject -- People -- Women",
	// "Subject -- World War, 1939 -- Women;Subject -- People -- Women",
	// "Subject -- Employment -- Economic & social conditions -- Women;Subject
	// -- People -- Women",
	// "Subject -- Backyards;Subject -- Sites -- Yards -- Backyards",
	// "Subject -- Festive decorations;Subject -- Objects -- Decorations --
	// Festive decorations",
	// "Subject -- Veterans -- Education;Subject -- Disciplines -- Education",
	// "Subject -- Communication -- Advertising -- Food;Subject -- Objects --
	// Food",
	// "Subject -- Buildings -- Cleaning;Subject -- Activities -- Cleaning",
	// "Subject -- Places -- Pennsylvania -- Pittsburgh -- Cleaning;Subject --
	// Activities -- Cleaning",
	// "Subject -- World War, 1935-1945 -- War work -- Schools;Subject --
	// Educational facilities -- Schools",
	// "Subject -- Places -- Pennsylvania -- Pittsburgh -- Interiors;Subject --
	// Buildings -- Interiors",
	// "Subject -- Atomic power-plants -- Models;Subject -- Objects -- Models",
	// "Subject -- Open-hearth furnaces -- Models;Subject -- Objects -- Models",
	// "Subject -- Blast furnaces -- Models;Subject -- Objects -- Models",
	// "Subject -- Aircraft -- Airplanes -- Engines;Subject -- Engines",
	// "Subject -- Objects -- Food -- Transportation;Subject -- Transportation",
	// "Subject -- Natural phenomena -- Air -- Pollution;Subject -- Pollution",
	// "Subject -- Indians of North America -- Dance;Subject -- Art -- Dance",
	// "Subject -- West Mifflin (Pa.) -- Aerial views;Subject -- Aerial views",
	// "Subject -- Point State Park (Pittsburgh, Pa.) -- Aerial views;Subject --
	// Aerial views",
	// "Subject -- Sharpsburg (Pa.) -- Aerial views;Subject -- Aerial views",
	// "Subject -- Etna (Pa.) -- Aerial views;Subject -- Aerial views",
	// "Subject -- H.J. Heinz Company (Pittsburgh, Pa.) -- Aerial views;Subject
	// -- Aerial views",
	// "Subject -- Aliquippa (Pa.) -- Aerial views;Subject -- Aerial views",
	//
	// "Places -- Pennsylvania -- Pittsburgh -- Design and Construction;Built
	// environment -- Design and Construction"
	//
	// };

	/**
	 * These are use for place fields only
	 */
	private String[] abbrevs = { "Alabama;al:ala", "Alaska;ak",
			"American Samoa;as", "Arizona;az:ariz", "Arkansas;ar:ark",
			"British Columbia;bc", "California;ca:cal:calif",
			"Colorado;co:col:colo", "Connecticut;ct:conn", "Delaware;de:del",
			"Federated States Of Micronesia;fm", "Florida;fl:fla",
			"Georgia;ga", "Guam;gu", "Hawaii;hi", "Idaho;id",
			"Illinois;il:ill", "Indiana;in:ind", "Iowa;ia", "Kansas;ks:kan",
			"Kentucky;ky", "Louisiana;la", "Maine;me", "Marshall Islands;mh",
			"Maryland;md", "Massachusetts;ma:mass", "Michigan;mi:mich",
			"Minnesota;mn:minn", "Mississippi;ms:miss", "Missouri;mo",
			"Montana;mt:mont", "Nebraska;ne:neb", "Nevada;nv:nev",
			"New Hampshire;nh", "New Jersey;nj", "New Mexico;nm",
			"New York;ny", "North Carolina;nc", "North Dakota;nd",
			"Northern Mariana Islands;mp", "Nova Scotia;ns", "Ohio;oh",
			"Oklahoma;ok:okla", "Ontario;ont", "Oregon;or:ore", "Palau;pw",
			"Pennsylvania;pa:penn", "Puerto Rico;pr", "Rhode Island;ri",
			"South Carolina;sc",
			"South Dakota;sd",
			"Tennessee;tn:tenn",
			"Texas;tx:tex",
			// "United States;us",
			"Utah;ut", "Vermont;vt", "Virgin Islands;vi", "Virginia;va",
			"Washington;wa:wash", "Washington DC;dc:d.c:washington, d.c",
			"West Virginia;wv:w va:wva", "Wisconsin;wi:wis:wisc",
			"Wyoming;wy:wyo" };

	// private final static String[] promoteBuilt = {
	// "Architectural & site components;Built environment",
	// "Facilities;Built environment" };
	//
	// private final static String[] promotePlaces = {
	// "North America -- United States;Places", "North America;Places" };

	// private final static String[][] rename = {
	// { "People associated with Agriculture",
	// "Agriculture-associated people" },
	// { "People associated with commercial activities",
	// "Commercial activity -associated people" },
	// { "People associated with education & communication",
	// "Educators & Communicators" },
	// { "People associated with entertainment & sports",
	// "Entertainment & Sports -associated people" },
	// { "People associated with health & safety",
	// "Health & Safety -associated people" },
	// { "People associated with manual labor", "Manual Laborers" },
	// { "People associated with military activities",
	// "Military Activity -associated people" },
	// { "People associated with politics & government",
	// "Politics & Government -associated people" },
	// { "People associated with religion", "Religion-associated people" },
	// { "People associated with transportation",
	// "Transportation-associated people" },
	// { "People with disabilities", "Disabled people" },
	// { "Clothing and dress", "Clothing & dress" },
	// { "Pittsubrgh", "Pittsburgh" }, { "Pittsburhgh", "Pittsburgh" },
	// { "Pittsurgh", "Pittsburgh" }, { "Pittsbu", "Pittsburgh" },
	// { "Pittsburhgh", "Pittsburgh" }, { "Pittsubrgh", "Pittsburgh" },
	// { "Pittsurgh", "Pittsburgh" }, { "Pennylvania", "Pennsylvania" },
	// { "Pennsyvlania", "Pennsylvania" },
	// { "Pennsylvnaia", "Pennsylvania" },
	// { "Pennsylvnia", "Pennsylvania" },
	// { "Pennslyvania", "Pennsylvania" },
	// { "Pennsylvaania", "Pennsylvania" },
	// { "Pennsyvlanis", "Pennsylvania" },
	// { "Pennslvania", "Pennsylvania" },
	// { "Pennsyvania", "Pennsylvania" }, { "Pennsylva", "Pennsylvania" },
	// { "Pennsylvani", "Pennsylvania" }, { "Penns", "Pennsylvania" },
	// { "Pennsylv", "Pennsylvania" }, { "Pennsylania", "Pennsylvania" },
	// { "Pennsylavnia", "Pennsylvania" },
	// { "Pennsylvaia", "Pennsylvania" },
	// { "Pennsylvania.", "Pennsylvania" },
	// { "Pennsyvlvania", "Pennsylvania" }, { "Pa.)", "Pennsylvania" },
	// { "Pennsylva nia", "Pennsylvania" },
	//
	// };

	private String rename(String s) {
		// Util.print("rename " + renames);
		String result = renames == null ? null : (String) renames.get(s);
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
		if (renames != null)
			for (Iterator it = renames.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String from = truncateName((String) entry.getKey(), false);
				String to = truncateName((String) entry.getValue(), false);
				db("UPDATE raw_facet SET name = '" + to + "' WHERE name = '"
						+ from + "'");
				// db("UPDATE facet SET name = '" + rename[i][1]
				// + "' WHERE name = '" + rename[i][0] + "'");
			}
	}

	private String set;

	JDBCSample jdbc;

	private String setSpec;

	private Hashtable setNames = new Hashtable();

	private String dbName;

	private Map moves;

	private Map renames;

	/**
	 * Places within states that help recognize place names in the subject
	 * hierarchy. Applies to the pattern "<city> ([<place>,] <state abbrev>)"
	 */
	private Set cities;

	ParseOAIhandler(JDBCSample _jdbc, String db, boolean _verbose,
			Map _renames, Map _moves, Set _places) {
		renames = _renames;
		jdbc = _jdbc;
		dbName = db;
		verbose = _verbose;
		moves = _moves;
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

	static ParseOAIhandler getHandler(String connectString, String db) {
		JDBCSample jdbc = ParseOAI.createJDBC(connectString);
		// return new ParseOAIhandler(jdbc, new Hashtable(), new Hashtable(),
		// db,
		// false);
		return null;
	}

	void copyImages(String copyFrom) throws SQLException {
		Util.print("\nCopying images from " + copyFrom);
		convertFromRaw().ensureIndex("images", "URI", "URI");
		convertFromRaw().ensureIndex(copyFrom + ".images", "URI", "URI");
		db("REPLACE INTO images	"
				+ "SELECT it.record_num, him.image, him.URI, him.w, him.h FROM item it LEFT JOIN images im ON it.record_num = im.record_num "
				+ "INNER JOIN " + copyFrom + ".images him ON him.uri = it.uri	"
				+ "WHERE im.image is NULL");
		Util.print("...done");
	}

	void copyImagesNoURI(String copyFrom) throws SQLException {
		Util.print("\nCopying images from " + copyFrom);
		convertFromRaw().ensureIndex("item", "URI", "URI(500)");
		convertFromRaw().ensureIndex(copyFrom + ".item", "URI", "URI(500)");
		int n = db("REPLACE INTO images	"
				+ "SELECT it.record_num, copy_im.image, copy_im.w, copy_im.h "
				+ "FROM item it "
				+ "LEFT JOIN images im ON it.record_num = im.record_num "
				+ "INNER JOIN " + copyFrom
				+ ".item copy_it ON copy_it.uri = it.uri	" + "INNER JOIN "
				+ copyFrom
				+ ".images copy_im ON copy_it.record_num = copy_im.record_num "
				+ "WHERE im.image is NULL");
		Util.print("..." + n + " images copied");
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

	ConvertFromRaw convertFromRaw() {
		if (converter == null)
			converter = new ConvertFromRaw(jdbc);
		return converter;
	}

	void log(String s) {
		if (verbose)
			Util.print(s);
	}

	// private void parseTGM() {
	// db("TRUNCATE TABLE tgm");
	// String[] filenames = { "c:\\Projects\\ArtMuseum\\LoC\\tgm1.txt",
	// "c:\\Projects\\ArtMuseum\\LoC\\tgm2.txt" };
	// for (int i = 0; i < filenames.length; i++) {
	// String filename = filenames[i];
	// try {
	// String term = null;
	// BufferedReader in = new BufferedReader(new FileReader(filename));
	// while (true) {
	// String s = in.readLine();
	// if (s.startsWith("MT: ")) {
	// term = s.substring(4);
	// } else if (s.startsWith("BT: ")) {
	// String bterm = s.substring(4);
	// db("INSERT INTO TGM VALUES(" + JDBCSample.quote(term)
	// + ", " + JDBCSample.quote(bterm) + ")");
	// } else if (s.startsWith("USE: ")) {
	// String bterm = s.substring(5);
	// db("INSERT INTO TGM VALUES(" + JDBCSample.quote(term)
	// + ", " + JDBCSample.quote(bterm) + ")");
	// }
	// }
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	//
	// private void parse043codes() {
	// db("TRUNCATE TABLE gac");
	// db("TRUNCATE TABLE places_hierarchy");
	// String filename = "c:\\Projects\\ArtMuseum\\LoC\\gac.txt";
	// try {
	// BufferedReader in = new BufferedReader(new FileReader(filename));
	// while (true) {
	// String[] s = in.readLine().split("=");
	// assert s.length == 2 : s;
	// String code = s[0];
	// String value = s[1];
	// String[] codeComponents = code.split("-");
	// if (codeComponents.length > 1) {
	// String largerCode = JDBCSample.quote(Util.join(Util
	// .subArray(codeComponents, 0,
	// codeComponents.length - 2, String.class),
	// "-"));
	// // if (largerCode.equals("e-ur"))
	// // largerCode = "e";
	// String largerValue = jdbc
	// .SQLqueryString("SELECT broader FROM gac WHERE term = "
	// + largerCode);
	// assert largerValue != null : largerCode;
	// // db("REPLACE INTO places_hierarchy VALUES("
	// // + JDBCSample.quote(value)
	// // + ", SUBSTRING_INDEX("
	// // + JDBCSample.quote(largerValue)
	// // + ", ' -- ', -1))");
	// value = largerValue + " -- " + value;
	// }
	// db("INSERT INTO gac VALUES(" + JDBCSample.quote(code) + ", "
	// + JDBCSample.quote(value) + ")");
	// }
	// computePlacesHierarchy();
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// private void computePlacesHierarchy() throws SQLException {
	// ResultSet rs = jdbc.SQLquery("SELECT broader FROM gac");
	// while (rs.next()) {
	// String[] hierarchy = rs.getString(1).split(" -- ");
	// if (hierarchy.length > 1) {
	// String largerValue = Util.join(Util.subArray(hierarchy, 0,
	// hierarchy.length - 2, String.class), " -- ");
	// String value = hierarchy[hierarchy.length - 1];
	// db("REPLACE INTO places_hierarchy VALUES("
	// + JDBCSample.quote(value) + ", SUBSTRING_INDEX("
	// + JDBCSample.quote(largerValue) + ", ' -- ', -1))");
	// }
	// }
	// }
	//
	// private boolean checkMultipleParents(String TGMtable) throws SQLException
	// {
	// ResultSet rs = jdbc
	// .SQLquery("SELECT term, GROUP_CONCAT(broader SEPARATOR ' -- ') FROM "
	// + TGMtable + " t GROUP BY term HAVING COUNT(*) > 1");
	// boolean multiple = false;
	// while (rs.next()) {
	// String term = JDBCSample.quote(rs.getString(1));
	// String[] parents = rs.getString(2).split(" -- ");
	// String[] immediateParents = rs.getString(2).split(" -- ");
	// for (int i = 0; i < parents.length; i++) {
	// String[] toLose = (String[]) Util.setIntersection(ancestors(
	// parents[i], TGMtable), immediateParents, String.class);
	// if (toLose != null) {
	// for (int j = 0; j < toLose.length; j++) {
	// db("DELETE FROM " + TGMtable + " WHERE term = " + term
	// + " AND broader = "
	// + JDBCSample.quote(toLose[j]));
	// }
	// immediateParents = (String[]) Util.setDifference(
	// immediateParents, toLose, String.class);
	// }
	// }
	// if (immediateParents.length > 1) {
	// if (!multiple) {
	// multiple = true;
	// Util.print("\nWARNING: " + TGMtable
	// + " has multiple parents for:");
	// }
	// Util.print(term + ": " + Util.join(immediateParents));
	// }
	// }
	// return multiple;
	// }

	private String[] renumberTables = { "images", "item", "raw_item_facet" };

	/**
	 * Re-assigns record numbers to items so that they are consecutive starting
	 * at 1.
	 * 
	 * @throws SQLException
	 */
	void renumber() throws SQLException {
		String[] tables = renumberTables; // Util.append(attrTables,
		// renumberTables);
		int first = jdbc.SQLqueryInt("SELECT MIN(record_num) FROM item");
		for (int i = 1; i < first; i++) {
			int old = jdbc
					.SQLqueryInt("SELECT MIN(record_num) FROM item WHERE record_num >= "
							+ i);
			if (old > i) {
				for (int j = 0; j < tables.length; j++) {
					db("UPDATE " + tables[j] + " SET record_num = " + i
							+ " WHERE record_num = " + old);
				}
			} else
				break;
		}
	}

	// private String[] ancestors(String term, String table) throws SQLException
	// {
	// String[] result = null;
	// ResultSet rs = jdbc.SQLquery("SELECT broader FROM " + table
	// + " WHERE term = " + JDBCSample.quote(term));
	// while (rs.next()) {
	// String s = rs.getString(1);
	// result = (String[]) Util.append(result, (String[]) Util.push(
	// ancestors(s, table), s, String.class), String.class);
	// }
	// // Util.print(" ancestors " + term + " => " + Util.join(result));
	// return result;
	// }

	private String ancestorString(int facet) throws SQLException {
		return Util.join(ancestors(facet, false), " -- ");
	}

	private String[] ancestors(int facet, boolean showID) throws SQLException {
		// int parent = getParentFacet(facet);
		if (facet > 0) {
			return (String[]) Util.push(
					ancestors(getParentFacet(facet), showID), getName(facet)
							+ (showID ? " (" + facet + ")" : ""), String.class);
		} else
			return null;
	}

	private int[] ancestors(int facet) throws SQLException {
		// int parent = getParentFacet(facet);
		if (facet > 0) {
			return (int[]) Util.push(ancestors(getParentFacet(facet)), facet);
		} else
			return null;
	}

	void printDuplicates() throws SQLException {
		Util.print("Listing duplicate facet names...");
		ResultSet rs = jdbc
				.SQLquery("SELECT GROUP_CONCAT(facet_id) FROM raw_facet GROUP BY name HAVING COUNT(*) > 1");
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
				Util.print(Util.join(ancestors(facet1, false)));
			}
			Util.print("");
		}
		Util.print("\nListing duplicate facet names...done");
	}

	/**
	 * Change the facet hierarchy by moving the subtrees explicitly listed in
	 * 'toPromote'. All children of the old lineage are reparented to the new
	 * lineage, which is created if nececssary. The now-childless old parent is
	 * then deleted.
	 */
	private void promote() {
		boolean change = false;
		if (moves != null)
			for (Iterator it = moves.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				try {
					String key = (String) entry.getKey();
					String value = (String) entry.getValue();
					String[] olds = key.split(" -- ");
					String[] news = value.split(" -- ");
					int parentFacet = facetType(olds[0]);

					for (int j = 1; j < olds.length && parentFacet > 0; j++)
						parentFacet = lookupFacet(olds[j], parentFacet);
					if (parentFacet > 0) {
						int type = facetType(news[0]);
						int newParent = type;
						for (int j = 1; j < news.length; j++) {
							int childType = lookupFacet(news[j], newParent);
							if (childType < 0) {
								childType = newFacet(news[j], newParent);
							}
							newParent = childType;
						}
						assert newParent > 0 : value;
						promoteInternal(parentFacet, newParent);
						change = true;
					} else {
						// log("promote: Can't find " + key + " to promote");
					}
				} catch (SQLException e) {
					Util.err("Can't promote: " + entry + e);
				}
			}
		if (change)
			promote();
	}

	private void promoteInternal(int oldParent, int newParent)
			throws SQLException {
		assert newParent != oldParent : newParent + " " + oldParent;
		ResultSet rs = jdbc
				.SQLquery("SELECT facet_id FROM raw_facet WHERE parent_facet_id = "
						+ oldParent);
		while (rs.next()) {
			int child = rs.getInt(1);
			setParent(child, newParent);
			log(getFacetName(child) + " => " + getName(newParent));
		}
		db("REPLACE INTO raw_item_facet SELECT record_num, " + newParent
				+ " FROM raw_item_facet WHERE facet_id = " + oldParent);
		db("DELETE FROM raw_facet WHERE facet_id = " + oldParent);
		db("DELETE FROM raw_item_facet WHERE facet_id = " + oldParent);
	}

	void useTGM() throws SQLException {
		rename();
		// useTGMinternal("loc.tgm", facetType("Subject"));
		// useTGMinternal("places_hierarchy",
		// facetType("Location of Publication"));
		// useTGMinternal("loc.places_hierarchy", facetType("Places"));
		// rename();
		convertFromRaw().fixDuplicates();

		promote();
		convertFromRaw().fixMissingItemFacets(0);
		// promote(toPromoteDoublingItems);

		// fixFacetTypes();
		convertFromRaw().fixDuplicates();
		convertFromRaw().fixMissingItemFacets(0);
		convertFromRaw().findBrokenLinks(true, 0);

		if (moves != null)
			promoteSingletons();
		convertFromRaw().findBrokenLinks(true, 0);
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
	void promoteSingletons() throws SQLException {
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
				db("DELETE FROM raw_facet WHERE facet_id = " + parent);
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
				db("UPDATE raw_facet set parent_facet_id = " + type
						+ " WHERE parent_facet_id = " + singleton);
				db("DELETE FROM raw_facet WHERE facet_id = " + singleton);
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

	// private void fixFacetTypes() {
	// db("UPDATE raw_facet r, raw_facet_type p "
	// + "SET r.facet_type_idxx = p.facet_type_id "
	// + "WHERE r.parent_facet_id = p.facet_type_id");
	// while (db("UPDATE raw_facet r, raw_facet p "
	// + "SET r.facet_type_idxx = p.facet_type_idxx "
	// + "WHERE r.parent_facet_id = p.facet_id "
	// + "AND r.facet_type_idxx != p.facet_type_idxx") > 0) {
	// // Loop till they are all updated
	// }
	// }

	private void useTGMinternal(String TGMtable, int facetTypeID)
			throws SQLException {
		// Util.print("\nuseTGMinternal " + TGMtable + " " + facetTypeID);
		boolean change = false;
		// ResultSet rs = jdbc
		// .SQLquery("SELECT GROUP_CONCAT(DISTINCT t.term SEPARATOR ' -- '),
		// t.broader, IFNULL(MAX(p.facet_id), -1) "
		// + "FROM (raw_facet r INNER JOIN "
		// + TGMtable
		// + " t ON r.name = t.term) "
		// + "LEFT JOIN raw_facet p ON t.broader = p.name AND
		// p.facet_type_id = "
		// + facetTypeID
		// + " LEFT JOIN raw_facet child ON child.name = r.name AND
		// child.parent_facet_id = p.facet_id "
		// + "WHERE child.facet_type_id IS NULL AND r.facet_type_id = "
		// + facetTypeID
		// + " AND r.parent_facet_id = r.facet_type_id GROUP BY t.broader");
		ResultSet rs = jdbc
				.SQLquery("SELECT GROUP_CONCAT(DISTINCT t.term SEPARATOR ' -- '), t.broader, GROUP_CONCAT(DISTINCT p.facet_id) "
						+ "FROM (raw_facet r INNER JOIN "
						+ TGMtable
						+ " t ON r.name = t.term) "
						+ "LEFT JOIN raw_facet p ON t.broader = p.name" // AND
						// p.facet_type_idxx
						// = "
						// + facetTypeID
						+ " WHERE r.parent_facet_id = "
						+ facetTypeID
						+ " GROUP BY t.broader");
		while (rs.next()) {
			String parentIDs = rs.getString(3);
			int[] parents = null;
			if (parentIDs != null) {
				String[] parentIDstrings = parentIDs.split(",");
				parents = new int[0];
				for (int i = 0; i < parentIDstrings.length; i++) {
					int parent = Integer.parseInt(parentIDstrings[i]);
					if (getFacetType(parent) == facetTypeID)
						parents = Util.push(parents, parent);
				}
			}
			parents = pickParent(parents);
			int nParents = parents == null ? 0 : parents.length;
			// Use a separator not likely to occur in a term
			String[] terms = rs.getString(1).split(" -- ");
			String broader = rs.getString(2);
			if (nParents > 1) {
				notAdding(terms, broader, parents);
			} else {
				int parent = nParents == 1 ? parents[0] : -1;
				// if (rs.getString(1).indexOf("California") >= 0)
				// Util.print(rs.getString(1) + " => " + broader + " "
				// + parent);
				if (parent < 0) {
					change = true;
					parent = newFacet(broader, facetTypeID);
				}
				for (int i = 0; i < terms.length; i++) {
					String term = terms[i];
					int facet = lookupFacet(term, facetTypeID);
					// log(term + " => " + broader + " " + facet + " " +
					// parent);
					if (facet > 0)
						setParent(facet, parent);
					else {
						// term occurs multiple times in TGM table and we've
						// already processed one of them. Add a duplicate to
						// this parent, copying over all children and items.
						addDuplicateFacet(term, parent, facetTypeID);
					}
				}
			}
		}
		if (change)
			useTGMinternal(TGMtable, facetTypeID);
	}

	int[] pickParent(int[] parents) throws SQLException {
		int[] result = parents;
		if (moves != null)
			if (parents != null && parents.length > 1) {
				for (int i = 0; i < parents.length && result == parents; i++) {
					String path = Util.join(
							Util.reverse(ancestors(parents[i], false)), " -- ")
							.toLowerCase();
					for (Iterator it = moves.entrySet().iterator(); it
							.hasNext()
							&& result == parents;) {
						Entry entry = (Entry) it.next();
						String key = (String) entry.getKey();
						String value = (String) entry.getValue();
						if (key.toLowerCase().indexOf(path) >= 0
								|| value.toLowerCase().indexOf(path) >= 0) {
							result = new int[1];
							result[0] = parents[i];
							// Util.print("Found parent " + path);
							// Util.print(toPromote[j]);
						}
					}
				}
			}
		return result;
	}

	private Hashtable previousNotAddingWarnings = new Hashtable();

	private void notAdding(String[] termList, String broader, int[] parents)
			throws SQLException {
		String terms = Util.join(termList);
		String[] prev = (String[]) previousNotAddingWarnings.get(terms);
		if (!Util.isMember(prev, broader)) {
			Util.print("\nNot adding parent " + broader + " to [" + terms
					+ "]\n  multiple matches:");
			// int example = -1;
			for (int i = 0; i < parents.length; i++) {
				int parent = parents[i];
				// if (i == 1)
				// example = getParentFacet(parent);
				Util
						.print("   "
								+ Util.join(ancestors(parent, true))
								+ "\n     children: "
								+ jdbc
										.SQLqueryString("SELECT GROUP_CONCAT(name) FROM raw_facet WHERE parent_facet_id = "
												+ parent
												+ " GROUP BY parent_facet_id"));
			}
			// Util
			// .print(" Suggest changing bad parent_facet_ids to the right
			// one, then running ConvertFromRaw.");
			// Util.print(" e.g. UPDATE raw_facet SET parent_facet_id = "
			// + example + " WHERE facet_id = " + parents[0]);

			String bad = Util.join(Util.reverse(ancestors(parents[0], false)),
					" -- ");
			String good = Util.join(Util.reverse(ancestors(parents[1], false)),
					" -- ");
			Util
					.print(" Suggest promoting bad parent_facet_ids to the right one, e.g. add to toPromote:");
			Util.print("\"" + bad + ";" + good + "\"");

			prev = (String[]) Util.push(prev, broader, String.class);
			if (prev.length == 1)
				previousNotAddingWarnings.put(terms, prev);
		}
	}

	private void setParent(int facet, int parent) {
		if (!dontUpdate) {
			try {
				PreparedStatement setParent = lookupPS("UPDATE raw_facet SET parent_facet_id = ? WHERE facet_id = ?");
				setParent.setInt(1, parent);
				setParent.setInt(2, facet);
				jdbc.SQLupdate(setParent, "Set facet parent");
			} catch (SQLException e) {
				Util.err(facet + " " + parent);
				e.printStackTrace();
			}
		} else {
			Util.print("UPDATE raw_facet SET parent_facet_id = " + parent
					+ " WHERE facet_id = " + facet);
		}
	}

	private int lookupSomeFacet(String name, int facetType) throws SQLException {
		int[] facets = lookupFacets(name, facetType);
		if (facets.length > 0)
			return facets[0];
		else
			return -1;
	}

	private int lookupFacet(String name, int parent) {
		int result = -1;
		try {
			PreparedStatement lookupFacet = lookupPS("SELECT facet_id FROM raw_facet WHERE name = ? AND parent_facet_id = ?");
			lookupFacet.setString(1, truncateName(name, true));
			lookupFacet.setInt(2, parent);
			result = jdbc.SQLqueryInt(lookupFacet,
					"Lookup facet from name and parent " + name + " " + parent);
		} catch (SQLException e) {
			Util.err("While looking up " + name + " " + parent);
			e.printStackTrace();
		}
		// Util.print("lookupFacet " + name + " " + parent + " => " +
		// result);
		return result;
	}

	private int[] lookupFacets(String name, int type) throws SQLException {
		int[] result = new int[0];
		ResultSet rs = null;
		try {
			PreparedStatement lookupFacets = lookupPS("SELECT facet_id FROM raw_facet WHERE name = ?");
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

	// private PreparedStatement lookupFacets;
	//
	// private ResultSet lookupFacets(String name, int type) {
	// ResultSet rs = null;
	// try {
	// if (lookupFacets == null) {
	// lookupFacets = jdbc
	// .prepareStatement("SELECT f.facet_id FROM raw_facet f WHERE f.name = ?
	// AND f.facet_type_idxx = ?");
	// }
	// lookupFacets.setString(1, truncateName(name, false));
	// lookupFacets.setInt(2, type);
	// rs = jdbc.SQLquery(lookupFacets, "Get child facet names");
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// return rs;
	// }

	private void addDuplicateFacet(String term, int parent, int facetType)
			throws SQLException {
		int old = lookupSomeFacet(term, facetType);
		log("addDuplicateFacet "
				+ term
				+ ", "
				+ jdbc
						.SQLqueryString("SELECT name FROM raw_facet WHERE facet_id = "
								+ parent)
				+ ", "
				+ jdbc
						.SQLqueryString("SELECT p.name FROM raw_facet r "
								+ "INNER JOIN raw_facet p ON r.parent_facet_id = p.facet_id "
								+ "WHERE r.facet_id = " + old));
		assert old > 0 : term + " " + parent + " " + facetType;
		parent = newFacet(term, parent);

		ResultSet rs = itemFacets(old);
		while (rs.next()) {
			addItemFacet(rs.getInt(1), parent);
		}

		String[] childNames = null;
		rs = childFacets(old);
		while (rs.next()) {
			childNames = (String[]) Util.push(childNames, rs.getString(1),
					String.class);
		}
		if (childNames != null) {
			for (int i = 0; i < childNames.length; i++) {
				addDuplicateFacet(childNames[i], parent, facetType);
			}
		}
	}

	private void merge(int from, int to) throws SQLException {
		log("merging " + ancestorString(from) + " into " + ancestorString(to));
		db("UPDATE raw_facet SET parent_facet_id = " + to
				+ " WHERE parent_facet_id = " + from);
		db("REPLACE INTO raw_item_facet SELECT record_num, " + to
				+ " FROM raw_item_facet WHERE facet_id = " + from);
		db("DELETE FROM raw_item_facet WHERE facet_id = " + from);
		db("DELETE FROM raw_facet WHERE facet_id = " + from);
	}

	private ResultSet childFacets(int parent) {
		ResultSet rs = null;
		try {
			PreparedStatement childFacets = lookupPS("SELECT name FROM raw_facet WHERE parent_facet_id = ?");
			childFacets.setInt(1, parent);
			rs = jdbc.SQLquery(childFacets, "Get child facet names");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	private ResultSet itemFacets(int facet) {
		ResultSet rs = null;
		try {
			PreparedStatement itemFacets = lookupPS("SELECT record_num FROM raw_item_facet WHERE facet_id = ?");
			itemFacets.setInt(1, facet);
			rs = jdbc.SQLquery(itemFacets, "Get items having facet");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	private void addItemFacet(int record_num, int facet_id) {
		// Util.print("addItemFacet " + facet_id);
		if (!dontUpdate) {
			try {
				PreparedStatement itemFacet = lookupPS("REPLACE INTO raw_item_facet VALUES(?, ?)");
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

	int maxNameLength = -1;

	private int newFacet(String name, int parent) throws SQLException {
		// if (name.startsWith("Pearce, James Alfred")) {
		// Util.print(name + " '" + data.toString() + "' " + tag
		// + subfield);
		// Util.printStackTrace();
		// }
		name = truncateName(name, false);
		if (!dontUpdate) {
			assert lookupFacet(name, parent) < 0 : name + " " + parent + " "
					+ getFacetName(parent);
			try {
				PreparedStatement newFacet = lookupPS("INSERT INTO raw_facet VALUES(?, ?, ?, null)");
				newFacet.setInt(1, ++facetID);
				newFacet.setString(2, name);
				newFacet.setInt(3, parent);
				// newFacet.setInt(4, facetType);
				jdbc.SQLupdate(newFacet, "Add new facet");
			} catch (SQLException e) {
				Util.err("Problem creating facet: name='" + name + "'; parent="
						+ getFacetName(parent));
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

	// void collectDescriptions() {
	// for (int i = 0; i < attrTables.length; i++) {
	// db("UPDATE item i set " + attrTables[i]
	// + " = (select GROUP_CONCAT(value SEPARATOR '"
	// + attrSeparators[i] + "') FROM " + attrTables[i]
	// + " a WHERE a.record_num = i.record_num AND i."
	// + attrTables[i] + " IS NULL GROUP BY a.record_num)");
	// // db("TRUNCATE TABLE " + attrTables[i]);
	// }
	// }

	// private void loadImages(String directory) throws SQLException,
	// ImageFormatException, InterruptedException {
	// ResultSet rs = jdbc
	// .SQLquery("SELECT DISTINCT it.keyURI, it.itemPage, it.record_num,
	// it.imagePage "
	// + "FROM URIs it LEFT JOIN images im "
	// + "ON it.record_num = im.record_num WHERE im.image IS NULL"
	// + " AND it.imagePage IS NOT NULL");
	// URL base = null;
	// try {
	// base = new URL("http://memory.loc.gov/");
	// } catch (MalformedURLException e1) {
	// }
	// while (rs.next()) {
	// String loc = rs.getString(1);
	// int record = rs.getInt(3);
	// String imageURL = rs.getString(4);
	// Util.print("\n" + loc);
	// URL url;
	// try {
	// url = new URL(loc);
	// BufferedReader reader = new BufferedReader(
	// new InputStreamReader(url.openStream()));
	// int state = 0; // 0 - seen nothing; 1 - seen "click on
	// // picture"; 2 - found image; 3 - seen <p>
	// int start;
	// while (state < 2) {
	// String s = reader.readLine();
	// if (s.indexOf("<I>No digital image available</I>") >= 0) {
	// Util.print("<no image>");
	// assert imageURL == "<none>" : record + " " + loc;
	// // db("UPDATE URIs SET imagePage = '<none>' WHERE
	// // record_num = "
	// // + record);
	// state = 3;
	// }
	// if (state == 0) {
	// if (s.indexOf("Click on picture for ") >= 0)
	// state = 1;
	// if (s.indexOf("<IMG SRC=\"http://memory.loc.gov/") >= 0)
	// state = 1;
	// }
	// if (state == 1) {
	// if ((start = s.indexOf("<IMG SRC=")) >= 0) {
	// int end = s.indexOf(".gif\">");
	// if (end > start) {
	// state = 2;
	// String thumbURL = s.substring(start + 10,
	// end + 4);
	// loadImageInternal(directory, thumbURL, base,
	// record, loc);
	// } else
	// state = 3;
	// } else if (s.indexOf("<P>") >= 0) {
	// state = 3;
	// }
	// }
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	void loadImages(String URLexpr, String pattern, int maxDimension,
			int quality) throws SQLException, ImageFormatException,
			InterruptedException {
		int nToLoad = jdbc
				.SQLqueryInt("SELECT COUNT(*) "
						+ "FROM item LEFT JOIN images "
						+ "ON item.record_num = images.record_num WHERE images.image IS NULL");
		Util.print(nToLoad + " images to load");
		if (nToLoad > 0) {
			Pattern cPattern = null;
			if (pattern != null)
				cPattern = Pattern.compile(pattern);
			ResultSet rs = jdbc
					.SQLquery("SELECT "
							+ URLexpr
							+ ", item.record_num "
							+ "FROM item LEFT JOIN images "
							+ "ON item.record_num = images.record_num WHERE images.image IS NULL");
			try {
				while (rs.next()) {
					String loc = rs.getString(1);
					int record = rs.getInt(2);
					Util.print("\n" + loc);
					URL thumbURL = new URL(loc);
					if (cPattern != null) {
						Matcher matcher = cPattern.matcher(Util.readURL(loc));
						if (matcher.find()) {
							loc = matcher.group(1);
						} else {
							Util.err("Can't find pattern in " + loc);
						}
						thumbURL = new URL(thumbURL, loc);
					}
					loadImageInternal(thumbURL, record, maxDimension, quality);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void loadImageInternal(URL url, int record_num, int maxDimension,
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
				PreparedStatement setImage = lookupPS("INSERT INTO images VALUES(?, ?, ?, ?)");
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

	// static Vector convertImageJ2K(String inFile, String outFile, int quality)
	// throws InterruptedException, ImageFormatException, IOException {
	// Image image = Toolkit.getDefaultToolkit().createImage(inFile);
	// MediaTracker mediaTracker = new MediaTracker(new Container());
	// mediaTracker.addImage(image, 0);
	// mediaTracker.waitForID(0);
	// // Util.print("errors? => " + mediaTracker.isErrorAny());
	// // determine thumbnail size from WIDTH and HEIGHT
	// int imageWidth = image.getWidth(null);
	// int imageHeight = image.getHeight(null);
	// // assert imageWidth > 0 && imageHeight > 0 : imageWidth + "x" +
	// // imageHeight;
	// if (inFile != outFile) {
	// BufferedImage thumbImage = Util.createCompatibleImage(imageWidth,
	// imageHeight);
	// Graphics2D graphics2D = thumbImage.createGraphics();
	// graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	// RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	// graphics2D.drawImage(image, 0, 0, imageWidth, imageHeight, null);
	//
	// ImageWriter writer = (ImageWriter) ImageIO
	// .getImageWritersByFormatName(imageMIMEtype).next();
	// ImageOutputStream ios = null;
	// OutputStream os1 = new FileOutputStream(outFile);
	// ios = ImageIO.createImageOutputStream(os1);
	// writer.setOutput(ios);
	// J2KImageWriteParam paramJ2K = new J2KImageWriteParam();
	//
	// // You can set whether it is lossy here
	// paramJ2K.setLossless(false);
	// paramJ2K.setFilter("w9x7");
	// paramJ2K.setEncodingRate(8);// 8 bits per pixel is pretty near
	// // identical to original
	// paramJ2K.setWriteCodeStreamOnly(true);
	// paramJ2K.setProgressionType("res");
	//
	// System.out.println("JPEG2000 Parameter: ");
	// System.out.print("getCodeBlockSize: ");
	// Util.printDeep(paramJ2K.getCodeBlockSize());
	// System.out.println("getComponentTransformation: "
	// + paramJ2K.getComponentTransformation());
	// System.out.println("getEPH: " + paramJ2K.getEPH());
	// System.out.println("getLossless: " + paramJ2K.getLossless());
	// System.out
	// .println("getEncodingRate: " + paramJ2K.getEncodingRate());
	// System.out.println("getFilter: " + paramJ2K.getFilter());
	// System.out.println("getProgressionType: "
	// + paramJ2K.getProgressionType());
	// System.out.println("getSOP: " + paramJ2K.getSOP());
	// System.out.println("getWriteCodeStreamOnly: "
	// + paramJ2K.getWriteCodeStreamOnly());
	// RenderedOp renImage7 = AWTImageDescriptor.create(thumbImage, null);
	// IIOImage ioimage = new IIOImage(renImage7, null, null);
	// writer.write(null, ioimage, paramJ2K);
	// ios.close();
	// os1.close();
	// writer.dispose();
	// }
	// Vector result = new Vector(3);
	// result.add(outFile);
	// result.add(new Integer(imageWidth));
	// result.add(new Integer(imageHeight));
	// Util.print("convertImage return " + result);
	// return result;
	// }

	public void startDocument() {
		// System.out.println("Document processing started");
		// db("ALTER TABLE raw_facet ADD INDEX name (name)");
	}

	public void endDocument() {
		// db("ALTER TABLE raw_facet DROP INDEX name");
		// System.out.println("Document processing finished");
		Util.print("...done\n");
	}

	void newItem() {
		try {
			PreparedStatement newItem = lookupPS("INSERT INTO item VALUES(null, null, ?, null, null, null, null, null)");
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
				PreparedStatement setCollection = lookupPS("INSERT INTO raw_item_facet VALUES(?, ?);");
				setCollection.setInt(1, recordNum);
				setCollection.setInt(2, collection);
				jdbc.SQLupdate(setCollection, "Set item's collection");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			Util.print("INSERT INTO raw_item_facet VALUES(" + recordNum + ", "
					+ collection + ");");
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attrs) {
		if (qName.equals("record")) {
			newItem();
			if (set != null) {
				try {
					Field field = Field.getField("Collection", Field.FACET);
					int[] facets = getFacets(field, set);
					assert dontUpdate || facets != null : field + " '" + set
							+ "'";
					assert facets.length == 1 : "'" + set + "'";
					setCollection(facets[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (qName.equals("marc:datafield")) {
			subFacetTable.clear();
			// ind1 = attrs.getValue("ind1");
			// ind2 = attrs.getValue("ind2");
			tag = attrs.getValue("tag");
		} else if (qName.equals("request")) {
			String setSpec1 = attrs.getValue("set");
			if (setSpec1 != null) {
				set = (String) setNames.get(setSpec1);
				// Util.print(setSpec + " => " + set);
				assert set != null;
			}
		} else if (qName.equals("setSpec") || qName.equals("setName")) {
			data = new StringBuffer();
		} else if (qName.equals("marc:subfield")) {
			subfield = attrs.getValue("code");
			data = new StringBuffer();
		} else if (qName.startsWith("dc:")) {
			// tag = qName;
			data = new StringBuffer();
		}
	}

	public void endElement(String uri, String localName, String qName) {
		if (data != null && data.length() > 1) {
			String value = trim(data.toString());
			// Util.print(qName + " " + value);
			if (qName.equals("setSpec")) {
				setSpec = value;
			} else if (qName.equals("setName")) {
				setNames.put(setSpec, value);
			} else {
				Field field = null;
				if (qName.equals("marc:subfield"))
					field = getField();
				else if (qName.startsWith("dc:"))
					field = getDCfield(qName);
				else
					field = Field.getField(qName, Field.FACET);
				if (field != null) {
					try {
						switch (field.type) {
						case Field.ATTRIBUTE:
						case Field.FUNCTIONAL_ATTRIBUTE:
							if (field.name.equals("URI")) {
								// handleURI(value);
								ResultSet duplicates = lookupItem(value);
								while (duplicates.next()) {
									int duplicate = duplicates.getInt(1);
									String[] tables = { "item",
											"raw_item_facet" };
									for (int i = 0; i < tables.length; i++)
										db("DELETE FROM " + tables[i]
												+ " WHERE record_num = "
												+ duplicate);
									// for (int i = 0; i < attrTables.length;
									// i++)
									// db("DELETE FROM " + attrTables[i]
									// + " WHERE record_num = "
									// + duplicate);
									updateImageID(duplicate);
								}
							}
							db("UPDATE item SET " + field.name + " = "
									+ JDBCSample.quote(value)
									+ " WHERE record_num = " + recordNum + ";");
							break;
						// case Field.ATTRIBUTE:
						// db("REPLACE INTO " + field.name + " VALUES("
						// + recordNum + "," + JDBCSample.quote(value)
						// + ", '');");
						// break;
						case Field.FACET:
							int[] facets = getFacets(field, value);
							if (facets != null) {
								for (int i = 0; i < facets.length; i++) {
									int facet = facets[i];
									addItemFacet(recordNum, facet);
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
				}
			}
		}
		data = null;
	}

	// private PreparedStatement updateKey;
	//
	// private PreparedStatement updateImageURI;
	//
	// private PreparedStatement insertURI;
	//
	// private PreparedStatement getUpdateKeyQuery() throws SQLException {
	// if (updateKey == null)
	// updateKey = jdbc
	// .prepareStatement("UPDATE URIs SET keyURI = ? WHERE record_num = ?");
	// return updateKey;
	// }
	//
	// private PreparedStatement getUpdateImageQuery() throws SQLException {
	// if (updateImageURI == null)
	// updateImageURI = jdbc
	// .prepareStatement("UPDATE URIs SET imagePage = ? WHERE record_num =
	// ?");
	// return updateImageURI;
	// }
	//
	// private void handleURI(String value) throws SQLException {
	// if (jdbc
	// .SQLqueryInt("SELECT record_num FROM URIs WHERE record_num = "
	// + recordNum) > 0) {
	// getUpdateKeyQuery();
	// updateKey.setString(1, value);
	// updateKey.setInt(2, recordNum);
	// jdbc.SQLupdate(updateKey, "Update URIs key");
	// } else {
	// if (insertURI == null)
	// insertURI = jdbc
	// .prepareStatement("INSERT INTO URIs VALUES(null, ?, null, ?)");
	// insertURI.setString(1, value);
	// insertURI.setInt(2, recordNum);
	// jdbc.SQLupdate(insertURI, "Insert itemPage into URIs");
	// }
	// }

	// private void setImageURI() throws SQLException {
	// // db("UPDATE URIs SET keyURI = itemPage WHERE keyURI IS NULL");
	// // db("UPDATE URIs, item SET URIs.record_num = item.record_num WHERE
	// // URIs.keyURI = item.URI");
	// URL base = null;
	// try {
	// base = new URL("http://memory.loc.gov/");
	// } catch (MalformedURLException e1) {
	// }
	// Pattern p = Pattern
	// .compile("<!-- The following URL will result in display of this
	// document -->\n<!-- (.*) -->");
	// Pattern q = Pattern
	// .compile("(?s)lick on (?:(?:the picture)|(?:picture for)) .*?(?:<A)
	// (?:TARGET=NEW )?HREF=\"(/(?:cgi-bin|phpdata)/.*?)\">(?:\\w|\n)*?<IMG
	// SRC=\"(.*?)\"");
	// Pattern r = Pattern.compile("<!-- (http://.*)>");
	// Pattern qq = Pattern
	// .compile("(?:>No digital (?:image|version) available<|To download the
	// film for future viewing)");
	// Pattern rr = Pattern
	// .compile("(?:To go from the key map to an individual image, click on
	// "
	// + "|<title>The Library of Congress Pageturner</title>"
	// + "|This image(?:ry)? was compressed (?:using JPEG2000
	// technology|with the MrSID Publisher))");
	// Pattern problem = Pattern
	// .compile("<title>Handle Problem Report \\(Library of
	// Congress\\)</title>");
	// Pattern multiple = Pattern
	// .compile("<a href=\"(.*?)\"><img src=\"/pp/preview\\.gif\"
	// alt=\"Preview Images\">");
	//
	// ResultSet rs = jdbc
	// .SQLquery(
	// // "SELECT itemPage, record_num FROM URIs WHERE imagePage IS
	// // NULL"
	//
	// // + " AND keyURI != itemPage"
	// "SELECT itemPage, uris.record_num from item it left join thumbnails
	// im on it.record_num = im.record_num"
	// + " inner join uris on it.record_num = uris.record_num"
	// + " where image is null"
	// // + " AND uris.record_num = 98879"
	// );
	// while (rs.next()) {
	// try {
	// int record = rs.getInt(2);
	// Util.print(record);
	// String loc = rs.getString(1);
	// URL url = new URL(base, loc);
	// String s = fetch(url);
	//
	// Matcher m = problem.matcher(s);
	// if (m.find()) {
	// String keyURI = jdbc
	// .SQLqueryString("SELECT keyURI FROM URIs WHERE record_num = "
	// + record);
	// if (!keyURI.equals(loc)) {
	// loc = keyURI;
	// url = new URL(base, loc);
	// s = fetch(url);
	// m = problem.matcher(s);
	// if (m.find()) {
	// Util
	// .print("Can't find any document for "
	// + url);
	// continue;
	// }
	// } else {
	// Util.print("Can't find any document for " + url);
	// continue;
	// }
	// }
	//
	// m = p.matcher(s);
	// getUpdateKeyQuery();
	// if (m.find()) {
	// updateKey.setString(1, m.group(1));
	// url = new URL(base, m.group(1));
	// } else {
	// updateKey.setString(1, loc);
	// Util.print("Can't find canonical URL for " + url);
	// }
	// updateKey.setInt(2, record);
	// jdbc.SQLupdate(updateKey, "Update URIs key");
	//
	// m = multiple.matcher(s);
	// if (m.find()) {
	// Util.print("Found multiple for " + url);
	// loc = rs.getString(1);
	// url = new URL(base, m.group(1));
	// s = fetch(url);
	// url = new URL(base, m.group(1));
	// }
	//
	// m = q.matcher(s);
	// if (m.find()) {
	// String imageURLstring = m.group(1);
	// URL imageURL = new URL(url, imageURLstring);
	// String s2 = fetch(imageURL);
	// Matcher mm = r.matcher(s2);
	// if (mm.find()) {
	// getUpdateImageQuery();
	// updateImageURI.setString(1, imageURLstring);
	// updateImageURI.setInt(2, record);
	// jdbc.SQLupdate(updateImageURI, "Update URIs image");
	// } else {
	// mm = rr.matcher(s2);
	// if (mm.find()) {
	// getUpdateImageQuery();
	// updateImageURI.setString(1, imageURLstring);
	// updateImageURI.setInt(2, record);
	// jdbc.SQLupdate(updateImageURI,
	// "Update URIs image");
	// } else
	// Util
	// .print("Can't find canonical image URL for "
	// + url + " " + imageURL);
	// }
	// loadImageInternal(m.group(2), base, record, loc);
	// } else {
	// m = qq.matcher(s);
	// if (m.find()) {
	// getUpdateImageQuery();
	// updateImageURI.setString(1, "<none>");
	// updateImageURI.setInt(2, record);
	// jdbc.SQLupdate(updateImageURI, "Update URIs image");
	// } else
	// Util.print("Can't find image URL for " + url);
	// }
	// Thread.sleep(500);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }

	// private PreparedStatement creator;

	//
	// private void creators(String name) throws SQLException {
	// if (creator == null) {
	// creator = jdbc
	// .prepareStatement("REPLACE INTO creators VALUES(?, ?)");
	// }
	// Util.print(name);
	// creator.setString(1, name);
	// creator.setString(2, name);
	// jdbc.SQLupdate(creator, "Set creator in two formats");
	// }

	private void updateImageID(int old) {
		if (!dontUpdate) {
			try {
				PreparedStatement updateImageID = lookupPS("UPDATE images SET record_num = ? WHERE record_num = ?");
				updateImageID.setInt(1, recordNum);
				updateImageID.setInt(2, old);
				jdbc.SQLupdate(updateImageID, "Set new record_num for image");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			Util.print("UPDATE images SET record_num = " + recordNum
					+ " WHERE record_num = " + old + "");
	}

	private ResultSet lookupItem(String uri) {
		ResultSet rs = null;
		try {
			PreparedStatement lookupItem = lookupPS("SELECT record_num FROM item WHERE URI = ? AND record_num != ?");
			lookupItem.setString(1, uri);
			lookupItem.setInt(2, recordNum);
			rs = jdbc.SQLquery(lookupItem, "Lookup item from URI");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	public void characters(char[] ch, int start, int length) {
		if (data != null) {
			data.append(ch, start, length);
		}
	}

	private String trim(String value) {
		String result = value.replaceAll("\\A\\W*", "");
		result = result.replaceAll("[\\W&&[^\\)]]*\\z", "");
		// Util.print(value + " => " + result);
		return result;
	}

	int[] getFacets(Field field, String value) throws SQLException {
		String[][] hierValues = hierValues(field, value);
		// Util.print("getFacets " + field + " " + value + " " +
		// hierValues.length);
		if (hierValues.length > 0) {
			int[] result = null;
			field = hierField(field);
			for (int p = 0; p < hierValues.length; p++) {
				String[] hierValue = hierValues[p];
				// Util.printDeep(hierValue);
				if (hierValue != null && hierValue.length > 0) {
					int[] topLevel = getFacets(field, hierValue[0], field
							.getFacetType(this));
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
			return result;
		} else
			return new int[0];
	}

	private Field hierField(Field f) {
		if (f.name.equals("043 Places"))
			f = Field.getField("Places", Field.FACET);
		else if (f.name.equals("045 Date"))
			f = Field.getField("Date", Field.FACET);
		return f;
	}

	/**
	 * @param value
	 *            facet name
	 * @param key
	 *            either "z" for Places or "corp" for Specific Organizations
	 * @return
	 */
	private String[] hierValuesInternal(String value, String key) {
		String[] result = (String[]) subFacetTable.get(key);
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

	private Hashtable placeAbbrevs;

	private Hashtable getPlaceAbbrevs() {
		if (placeAbbrevs == null) {
			placeAbbrevs = new Hashtable();
			for (int i = 0; i < abbrevs.length; i++) {
				String[] x = abbrevs[i].split(";");
				placeAbbrevs.put(x[0].toLowerCase(), x[0]);
				String[] abbrs = x[1].split(":");
				for (int j = 0; j < abbrs.length; j++)
					placeAbbrevs.put(abbrs[j], x[0]);
			}
		}
		return placeAbbrevs;
	}

	// private void test() throws SQLException {
	// String facetTypeName = "Date of Creation";
	// Field field = Field.getField(facetTypeName, Field.FACET);
	// ResultSet rs = jdbc
	// .SQLquery("SELECT name, facet_id FROM raw_facet WHERE facet_type_id = "
	// + facetType(facetTypeName));
	// while (rs.next()) {
	// subFacetTable.clear();
	// String name = rs.getString(1);
	// int facet_id = rs.getInt(2);
	// int[] facets = null;
	// try {
	// data = new StringBuffer();
	// data.append(name);
	// facets = getFacets(field, name);
	// } catch (AssertionError e) {
	// // Can happen when we create duplicates
	// }
	// if (facets != null) {
	// assert facets.length == 1;
	// if (facets[0] != facet_id) {
	// String newName = facets[0] == 999999 ? "foo"
	// : getFacetName(facets[0]);
	// int parent = facets[0] == 999999 ? 999999
	// : getParentFacet(facets[0]);
	// Util.print(" " + name + " => " + newName);
	// db("UPDATE raw_facet SET name = "
	// + JDBCSample.quote(newName)
	// + ", parent_facet_id = " + parent
	// + " WHERE facet_id = " + facet_id);
	// }
	// }
	// }
	// }
	//
	// private void test2() throws SQLException {
	// String facetTypeName = "Places";
	// Field field = Field.getField(facetTypeName, Field.FACET);
	// ResultSet rs = jdbc
	// .SQLquery("SELECT name, facet_id FROM raw_facet WHERE facet_type_id = "
	// + facetType(facetTypeName)
	// // + " AND name LIKE 'West Bank'"
	// + " ORDER BY name");
	// while (rs.next()) {
	// subFacetTable.clear();
	// String name = rs.getString(1);
	// int facet_id = rs.getInt(2);
	// int[] facets = null;
	// try {
	// data = new StringBuffer();
	// data.append(name);
	// facets = getFacets(field, name);
	// } catch (AssertionError e) {
	// // Can happen when we create duplicates
	// }
	// if (facets != null) {
	// Util.print(name + " => "
	// + Util.join(ancestors(facets[0], true)));
	// for (int f = 0; f < facets.length; f++) {
	// if (facets[f] != facet_id) {
	// String newName = facets[f] == 999999 ? "foo"
	// : getFacetName(facets[f]);
	// int parent = facets[f] == 999999 ? 999999
	// : getParentFacet(facets[f]);
	// Util
	// .print(" " + name + " => " + newName + " "
	// + parent);
	// Util.print("... "
	// + Util.join(ancestors(facets[0], true)));
	//
	// if (f == 0) {
	// db("UPDATE raw_facet SET name = "
	// + JDBCSample.quote(newName)
	// + ", parent_facet_id = " + parent
	// + " WHERE facet_id = " + facet_id);
	// } else
	// addDuplicateFacet(newName, parent, field.facetType);
	// }
	// }
	// }
	//
	// // String name = rs.getString(1);
	// // int facet_id = rs.getInt(2);
	// // String[] foo = trimPlaces(name);
	// // if (foo.length > 1)
	// // Util.print(name + " => " + Util.join(foo, " -- "));
	// }
	// }

	private String getName(int facet) throws SQLException {
		String result = getFacetName(facet);
		if (result == null)
			result = getFacetTypeName(facet);
		return result;
	}

	private String getFacetName(int facet_id) throws SQLException {
		return jdbc
				.SQLqueryString("SELECT name FROM raw_facet WHERE facet_id = "
						+ facet_id);
	}

	private String getFacetTypeName(int facet_type_id) throws SQLException {
		return jdbc
				.SQLqueryString("SELECT name FROM raw_facet_type WHERE facet_type_id = "
						+ facet_type_id);
	}

	private int getFacetType(int facet_id) throws SQLException {
		for (int facet_type_id = facet_id; facet_type_id > 0; facet_type_id = getParentFacet(facet_type_id)) {
			facet_id = facet_type_id;
		}
		return facet_id;
	}

	private int getParentFacet(int facet_id) throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT parent_facet_id FROM raw_facet WHERE facet_id = "
						+ facet_id);
	}

	private String[] hierPlaces(String value) throws SQLException {
		String[] result = { value };
		String broad;
		try {
			while ((broad = jdbc
					.SQLqueryString("SELECT broader FROM loc.places_hierarchy WHERE term = "
							+ JDBCSample.quote(result[0]))) != null)
				result = (String[]) Util.push(result, broad, String.class);
		} catch (AssertionError e) {
			// Skip if there are multiple parents for term
			// in
			// the
			// places hierarchy
		}
		// Util.print("hierPlaces " + Util.join(result));
		return result;
	}

	private String[][] trimPlaces(String value) throws SQLException {
		String[][] result = { { value } };
		Pattern p = Pattern.compile("\\b([^(]+)\\w*(?:,|\\()(.*?)(\\z|:|\\))");
		Matcher m = p.matcher(value);
		if (m.find()) {
			String narrow = m.group(1);
			String broadAbbrev = m.group(2);
			if (broadAbbrev != null) {
				broadAbbrev = broadAbbrev.toLowerCase();
				broadAbbrev = broadAbbrev.replaceAll("\\.", "");
				String[] multipleParents = broadAbbrev.split(" and ");
				if (multipleParents.length > 1) {
					String[][] x = new String[multipleParents.length][];
					x[0] = result[0];
					result = x;
				}
				for (int j = 0; j < multipleParents.length; j++) {
					String[] broads = multipleParents[j].split(",");
					String lastBroad = broads[broads.length - 1].trim();
					String broad = (String) getPlaceAbbrevs().get(lastBroad);
					if (broad == null)
						broad = jdbc
								.SQLqueryString("SELECT term FROM loc.places_hierarchy WHERE term = "
										+ JDBCSample.quote(m.group(2))
										+ " LIMIT 1");
					if (broad == null)
						broad = jdbc
								.SQLqueryString("SELECT term FROM loc.places_hierarchy WHERE term = "
										+ JDBCSample.quote(lastBroad)
										+ " LIMIT 1");
					if (broad == null)
						broad = jdbc
								.SQLqueryString("SELECT broader FROM loc.places_hierarchy WHERE broader = "
										+ JDBCSample.quote(lastBroad)
										+ " LIMIT 1");
					if (broad != null) {
						result[j] = broad.split(" -- ");
						for (int i = broads.length - 2; i >= 0; i--)
							result[j] = (String[]) Util.endPush(result[j],
									broads[i].trim(), String.class);
						result[j] = (String[]) Util.endPush(result[j], narrow,
								String.class);
					}
					if (result[j] != null) {
						try {
							while ((broad = jdbc
									.SQLqueryString("SELECT broader FROM loc.places_hierarchy WHERE term = "
											+ JDBCSample.quote(result[j][0]))) != null)
								result[j] = (String[]) Util.push(result[j],
										broad, String.class);
						} catch (AssertionError e) {
							// Skip if there are multiple parents for term
							// in
							// the
							// places hierarchy
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * @param field
	 *            raw field from XML file
	 * @param value
	 *            raw string from XML file
	 * @return [<ancestor facets alternetive 1>, ...]
	 * @throws SQLException
	 */
	private String[][] hierValues(Field field, String value)
			throws SQLException {
		String[][] result = { { value } };
		if (field.name.equals("Places")) {
			result[0] = hierValuesInternal(value, "z");
			if (result[0].length == 1)
				result = trimPlaces(result[0][0]);
			// Util.printDeep(result);
			if (result[0].length == 1)
				result[0] = hierPlaces(result[0][0]);
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
						String state = (String) getPlaceAbbrevs().get(
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
		// if (result.length > 1)
		// Util.print("hierValues " + value + " => "
		// + Util.join(result, " -- "));
		return result;
	}

	/**
	 * E.g. "Downtown (Pittsburgh, Pa.)" where Pa is in abbrevs. Only adds
	 * Downtown if it is in cities.
	 */
	Pattern cityState = Pattern
			.compile("\\s*(.*?)\\,?\\s*\\((\\w*?)\\.?,?\\s*(\\w+)\\.?\\)\\z");

	private String trimDate() {
		return trimDate(data.toString());
	}

	final static String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jany", "Feby", "Mar",
			"Apl", "May", "Jun", "Jul", "Augt", "Sepr", "Octr", "Novr", "Decr",
			"January", "February", "March", "April", "May", "June", "July",
			"August", "September", "October", "November", "December", "Sept" };

	private int month(String monthName) {
		int month = Util.member(months, monthName);
		if (month == 36)
			month = 9;
		else if (month >= 0)
			month = 1 + month % 12;
		return month;
	}

	String trimDate(String value) {
		String result = value
				.replaceAll(
						"\\b(?:ca|c|\\([cC]\\)|approximately|between|before|after|photographed|published)\\.?\\s*(\\d)",
						"$1");

		result = result.replaceAll("\\[19\\]", "19");
		result = result.replaceAll("19\\]", "19");

		Pattern p = Pattern.compile("\\b(\\d{2})--\\??");
		Matcher m = p.matcher(result);
		while (m.find()) {
			result = century(m.group(1));
			m.reset(result);
		}

		result = result.replaceAll("\\b(\\d{4})'s\\z", "$1s");

		result = result.replaceAll("\\b(\\d{4})/(\\d{4})\\z", "$1-$2");

		result = result.replaceAll("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\z",
				"$2/$3/$1");

		result = result.replaceAll("\\b(\\d{4})-(\\d{1,2})\\z", "$2/$1");

		result = result.replaceAll("\\b(\\d{3})\\[(\\d)\\]", "$1$2");

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

		p = Pattern
				.compile("\\b\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\z");
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

		result = result.replaceAll("\\b\\D*,\\s*(\\d{4})", "$1");

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
		int facetType = field.getFacetType(this);
		// Util.print("getFacets " + field + " " + value + " " + parent + "
		// " + facetType);
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

	private Map preparedStatements = new Hashtable();

	private PreparedStatement lookupPS(String SQL) throws SQLException {
		PreparedStatement ps = (PreparedStatement) preparedStatements.get(SQL);
		if (ps == null) {
			ps = jdbc.prepareStatement(SQL);
			preparedStatements.put(SQL, ps);
		}
		return ps;
	}

	/**
	 * @param name
	 *            known to be a place name
	 * @return <ancestor hierarchy>
	 */
	private String[] lookupGAC(String name) {
		String[] result = null;
		try {
			PreparedStatement lookupGAC = lookupPS("SELECT broader FROM loc.gac WHERE term = ?");
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

	int facetType(String name) {
		int result = -1;
		try {
			PreparedStatement facetTypeQuery = lookupPS("SELECT facet_type_id FROM raw_facet_type WHERE name = ?");
			facetTypeQuery.setString(1, name);
			result = jdbc.SQLqueryInt(facetTypeQuery, "Lookup facet type");
			assert result > 0 : "Can't find '" + name + "' in raw_facet_type";
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	// int newFacet(String name, Field field) {
	// return newFacet(name, field.getFacetType(this));
	// }

	// private int newFacet(String name, int facetType) throws SQLException {
	// return newFacet(name, facetType, facetType);
	// }

	Field getDCfield(String tag1) {
		Field result = null;
		if (tag1 != null) {
			if (tag1.equals("dc:identifier")) {
				result = Field.getField("URI", Field.FUNCTIONAL_ATTRIBUTE);
			} else if (tag1.equals("dc:publisher")) {
				result = Field.getField("Publisher", Field.FACET);
			} else if (tag1.equals("dc:format")) {
				result = Field.getField("Subject", Field.FACET);
			} else if (tag1.equals("dc:rights")) {
				result = Field.getField("Rights", Field.FACET);
			} else if (tag1.equals("dc:title")) {
				result = Field.getField("Title", Field.ATTRIBUTE);
			} else if (tag1.equals("dc:type")) {
				result = Field.getField("Type", Field.FACET);
			} else if (tag1.equals("dc:date")) {
				result = Field.getField("Date", Field.FACET);
			} else if (tag1.equals("dc:description")) {
				result = Field.getField("Description", Field.ATTRIBUTE);
			} else if (tag1.equals("dc:subject")) {
				result = Field.getField("Subject", Field.FACET);
			} else if (tag1.equals("dc:language")) {
				result = Field.getField("Language", Field.FACET);
			} else if (tag1.equals("dc:creator")) {
				result = Field.getField("Creator", Field.FACET);
			} else if (tag1.equals("dc:source")) {
				result = Field.getField("Source", Field.FACET);
			} else {
				Util.print("Ignoring " + tag1 + " " + data.toString());
			}
		}
		return result;
	}

	// marc21 codes explained at http://www.loc.gov/marc/bibliographic/
	Field getField() {
		if (tag != null) {
			if ("043".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return Field.getField("043 Places", Field.FACET);
				}
			} else if ("045".indexOf(tag) >= 0) {
				if ("ab".indexOf(subfield) >= 0) {
					return Field.getField("045 Date", Field.FACET);
				}
			} else if ("100,110,700".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return Field.getField("Creator", Field.FACET);
				} else if ("f".indexOf(subfield) >= 0) {
					return Field.getField("Date", Field.FACET);
				} else if ("t".indexOf(subfield) >= 0) {
					return Field.getField("Title", Field.ATTRIBUTE);
				} else if ("bcdegpqx56".indexOf(subfield) >= 0) {
					return null;
				}
			} else if ("130,240,245,246,740".indexOf(tag) >= 0) {
				if ("abps".indexOf(subfield) >= 0) {
					return Field.getField("Title", Field.ATTRIBUTE);
				} else if (subfield.equals("f")) {
					return Field.getField("Date of Creation", Field.FACET);
				} else if (subfield.equals("h")) {
					return Field.getField("Format", Field.FACET);
				} else if ("l".indexOf(subfield) >= 0) {
					return Field.getField("Language", Field.FACET);
				} else if ("cign56".indexOf(subfield) >= 0) {
					return null;
				}
			} else if ("257,260".indexOf(tag) >= 0) {
				if ("ae".indexOf(subfield) >= 0) {
					return Field.getField("Location of Publication",
							Field.FACET);
				} else if ("cg".indexOf(subfield) >= 0) {
					return Field.getField("Date of Creation", Field.FACET);
				} else if ("bf6".indexOf(subfield) >= 0) {
					return null;
				}
			} else if (tag.equals("300")) {
				return Field.getField("Description", Field.ATTRIBUTE);
			} else if (tag.equals("440") || tag.equals("490")) {
				return Field.getField("Series", Field.ATTRIBUTE);
			} else if (tag.equals("520")) {
				return Field.getField("Summary", Field.ATTRIBUTE);
			} else if ("500,504,505,511,518,545".indexOf(tag) >= 0) {
				return Field.getField("Note", Field.ATTRIBUTE);
			} else if (tag.equals("600")) {
				if (subfield.equals("a")) {
					return Field.getField("Specific People", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("610") || tag.equals("710")) {
				if (subfield.equals("a") || subfield.equals("b")) {
					return Field
							.getField("Specific Organizations", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("611")) {
				return subjectUsualSuspects();
			} else if ("650,653,654".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return Field.getField("Subject", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("655,755".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return Field.getField("Format", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("630".indexOf(tag) >= 0) {
				if (subfield.equals("a")) {
					return Field.getField("Title", Field.ATTRIBUTE);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("651")) {
				if (subfield.equals("a")) {
					return Field.getField("Places", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("656")) {
				if (subfield.equals("a")) {
					return Field.getField("Occupation", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if (tag.equals("657")) {
				if (subfield.equals("a")) {
					return Field.getField("Function", Field.FACET);
				} else
					return subjectUsualSuspects();
			} else if ("242,730".indexOf(tag) >= 0) {
				if ("at".indexOf(subfield) >= 0) {
					return Field.getField("Title", Field.ATTRIBUTE);
				} else if ("f".indexOf(subfield) >= 0) {
					return Field.getField("Date of Creation", Field.FACET);
				} else
					return null;
			} else if (tag.equals("752")) {
				if ("abcdfgh".indexOf(subfield) >= 0) {
					return Field.getField("Places", Field.FACET);
				} else
					return null;
			} else if (tag.equals("856")) {
				if (subfield.equals("u")) {
					return Field.getField("URI", Field.FUNCTIONAL_ATTRIBUTE);
				} else
					return null;
			} else if (("010,017,020,034,035,037,040,041,042,050,051,052,066,072,074,086,090,111,250,255,265,501,506,"
					+ "507,508,510,530,533,534,540,541,546,555,561,580,581,583,585,590,711,772,773,810,830,850,852,859,880")
					.indexOf(tag) >= 0) {
				// Known ignorable
				return null;
			}
		}
		Util.print("Ignoring " + tag + subfield + " " + data.toString());
		return null;
	}

	private Field subjectUsualSuspects() {
		if (subfield.equals("z")) {
			return Field.getField("Places", Field.FACET);
		} else if (subfield.equals("y")) {
			return Field.getField("Date", Field.FACET);
		} else if (subfield.equals("v") || subfield.equals("x")) {
			return Field.getField("Subject", Field.FACET);
		} else
			return null;
	}

	// String getAuthority() {
	// if (ind2 != null) {
	// if (ind2.equals("0")) {
	// return "LCSH";
	// } else if (ind2.equals("1")) {
	// return "LCSH-Children";
	// } else if (ind2.equals("2")) {
	// return "MESH";
	// } else if (ind2.equals("3")) {
	// return "NAL";
	// } else if (ind2.equals("5")) {
	// return "CSH";
	// } else if (ind2.equals("6")) {
	// return "RVM";
	// } else if (ind2.equals("7")) {
	// return "2";
	// }
	// }
	// return null;
	// }
}

class Field {
	static final int FACET = 0;

	static final int ATTRIBUTE = 1;

	static final int FUNCTIONAL_ATTRIBUTE = 2;

	String name;

	// String authority;

	int type;

	int facetType = -1;

	private static Hashtable fieldCache = new Hashtable();

	private Field(String _name, int _type) {
		name = _name;
		type = _type;
	}

	static Field getField(String _name, int _type) {
		// if (Util.isMember(SaxHandler.ignoreFields, _name))
		// return null;
		Field[] fields = (Field[]) fieldCache.get(_name);
		if (fields == null) {
			fields = new Field[3];
			fieldCache.put(_name, fields);
		}
		if (fields[_type] == null) {
			fields[_type] = new Field(_name, _type);
		}
		return fields[_type];
	}

	int getFacetType(ParseOAIhandler handler) {
		assert type == FACET;
		if (facetType < 0) {
			facetType = handler.facetType(name);
		}
		return facetType;
	}

	public String toString() {
		return "<Field " + name + ">";
	}
}
