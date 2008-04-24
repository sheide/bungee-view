package edu.cmu.cs.bungee.dbScripts;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.ApplicationSettings;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.BooleanToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.IntegerToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.StringToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.Token;

/**
 * Here is an example of parsing of XML data with help of document-handler.
 * 
 * Args for HP: -db hp3 -user root -pass tartan01 -reset -renumber -verbose
 * -directory C:\Projects\ArtMuseum\HistoricPittsburgh\OAI-2007-Dec\ -files
 * drlimg-accdbib.xml,drlimg-aerialbib.xml,drlimg-allegobbib.xml,drlimg-cmabib.xml,drlimg-cmaharrisbib.xml,drlimg-consolbib.xml,drlimg-cpbib.xml,drlimg-darlingtonbib.xml,drlimg-fcoxbib.xml,drlimg-fwagbib.xml,drlimg-gnbib.xml,drlimg-gretbib.xml,drlimg-gtbib.xml,drlimg-hjhzbib.xml,drlimg-iksbib.xml,drlimg-jalbib.xml,drlimg-jbenbib.xml,drlimg-kabib.xml,drlimg-lyshbib.xml,drlimg-mestbib.xml,drlimg-pghrailbib.xml,drlimg-ppsbib.xml,drlimg-rrbib.xml,drlimg-shourekbib.xml,drlimg-smokebib.xml,drlimg-spencerbib.xml,drlimg-trimbib.xml,drlimg-uapittbib.xml,drlimg-uebib.xml,drlimg-unionarcadebib.xml,drlimg-urbanbib.xml
 * "hp-drlimg-chathambib.xml,hp-drlimg-darlfamilybib.xml,hp-drlimg-fairbanksbib.xml,hp-drlimg-kaufbib.xml,hp-drlimg-pghprintsbib.xml,hp-drlimg-rustbib.xml,hp-drlimg-stotzbib.xml,hp-drlimg-switchbib.xml"
 * -cities Places.txt -renames Rename.txt -moves
 * TGM.txt,places_hierarchy.txt,Moves.txt -image_url_getter URI -image_regexp
 * src=\"(/cgi-bin.*?)\"
 * 
 * Args for LoC2: VM argument: -DentityExpansionLimit=200000
 * 
 * -db loc2 -user root -pass tartan01 -reset -verbose -directory
 * C:\Projects\ArtMuseum\LoC\2008\ -files loc-*.xml -cities Places.txt -renames
 * Rename.txt -moves TGM.txt,places_hierarchy.txt,Moves.txt -toMerge Collection
 * -image_url_getter URI 
 * 
 * Can't get disjunction to work:
 * 
 * -image_regexp SRC=\"(/gmd/.*?\.gif)\"
 * 
 * -image_regexp SRC=\"(http://.*?\.gif)\"
 * 
 * 
 * -cities Places.txt -renames Rename.txt -moves
 * TGM.txt,places_hierarchy.txt,Moves.txt
 * 
 * Make sure moves.txt is the last -moves files
 */

public class ParseOAI {
	static StringToken sm_db = new StringToken("db", "database to write to",
			"", Token.optRequired | Token.optSwitch, "");
	static StringToken sm_server = new StringToken("server", "MySQL server",
			"", Token.optSwitch, "jdbc:mysql://localhost/");
	static StringToken sm_user = new StringToken("user", "MySQL user", "",
			Token.optSwitch, "bungee");
	static StringToken sm_pass = new StringToken("pass", "MySQL user password",
			"", Token.optRequired, "");
	static StringToken sm_toMerge = new StringToken(
			"toMerge",
			"Merge items with the same URI that differ only in these tag categories",
			"", Token.optSwitch, null);

	static StringToken sm_copyImagesFrom = new StringToken("copy images from",
			"Database to copy images from, based on the item.URI column", "",
			Token.optSwitch, null);
	static StringToken sm_image_url_getter = new StringToken(
			"image_url_getter", "SQL expression for thumbnail location", "",
			Token.optSwitch, null);
	static StringToken sm_image_regexp = new StringToken("image_regexp",
			"Java regexp to apply to url", "", Token.optSwitch, null);
	static IntegerToken sm_image_max_dimension = new IntegerToken(
			"image_max_dimension", "Maximum thumbnail size", "",
			Token.optSwitch, 200);
	static IntegerToken sm_image_quality = new IntegerToken("image_quality",
			"Thumbnail quality (1-100)", "", Token.optSwitch, 80);
	static StringToken sm_files = new StringToken("files", "OAI files to read",
			"", Token.optSwitch, null);
	static StringToken sm_places = new StringToken("cities",
			"List of place names", "", Token.optSwitch, null);
	static StringToken sm_renames = new StringToken("renames",
			"List of name pairs", "", Token.optSwitch, null);
	static StringToken sm_moves = new StringToken("moves",
			"List of name pairs", "", Token.optSwitch, null);
	static StringToken sm_directory = new StringToken("directory",
			"default directory for file arguments", "", Token.optSwitch, ".");
	static BooleanToken sm_reset = new BooleanToken("reset",
			"clear raw_facet, raw_item_facet, and item?", "", Token.optSwitch,
			false);
	static BooleanToken sm_renumber = new BooleanToken("renumber",
			"renumber record_num to use sequential IDs starting at 1", "",
			Token.optSwitch, false);
	static BooleanToken sm_verbose = new BooleanToken("verbose",
			"print tag hierarchy manipulations?", "", Token.optSwitch, false);

	static ApplicationSettings sm_main = new ApplicationSettings();
	static {
		sm_main.addToken(sm_db);
		sm_main.addToken(sm_server);
		sm_main.addToken(sm_user);
		sm_main.addToken(sm_pass);
		sm_main.addToken(sm_copyImagesFrom);
		sm_main.addToken(sm_image_url_getter);
		sm_main.addToken(sm_toMerge);
		sm_main.addToken(sm_image_regexp);
		sm_main.addToken(sm_image_max_dimension);
		sm_main.addToken(sm_image_quality);
		sm_main.addToken(sm_files);
		sm_main.addToken(sm_directory);
		sm_main.addToken(sm_reset);
		sm_main.addToken(sm_renumber);
		sm_main.addToken(sm_verbose);
		sm_main.addToken(sm_places);
		sm_main.addToken(sm_renames);
		sm_main.addToken(sm_moves);
	}

	// static void parseTGM(Map use, Map bt, String directory, String filename)
	// {
	// if (filename != null) {
	// File f = new File(directory, filename);
	// // BufferedReader in = new BufferedReader(f);
	// // while (true) {
	// // String[] s = in.readLine().split("\t");
	// // abbrevs.put(s[0], Util.subArray(s, 1, String.class));
	// // }
	// }
	//
	// }

	/**
	 * Application entry point
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {
		try {
			if (sm_main.parseArgs(args)) {
				String dbName = sm_db.getValue();
				JDBCSample jdbc = new JDBCSample(sm_server.getValue(), dbName,
						sm_user.getValue(), sm_pass.getValue());
				createTables(jdbc, dbName);
				if (sm_reset.getValue()) {
					clearTables(jdbc);
				}
				String directory = sm_directory.getValue();
				boolean verbose = sm_verbose.getValue();

				ParseOAIhandler handler = new ParseOAIhandler(jdbc, dbName,
						verbose, parsePairFiles(directory, sm_renames
								.getValue()), parsePairFiles(directory,
								sm_moves.getValue()), parseSingletonFiles(
								directory, sm_places.getValue()));

				String filenameList = sm_files.getValue();
				if (filenameList != null) {
					String[] filenames = filenameList.split(",");
					SAXParserFactory factory = SAXParserFactory.newInstance();
					factory.setNamespaceAware(true);
					SAXParser parser = factory.newSAXParser();
					for (int i = 0; i < filenames.length; i++) {
						String[] matches = getFilenames(directory, filenames[i]);
						for (int j = 0; j < matches.length; j++) {
							Util.print("\nParsing " + matches[j]);
							parser.parse(directory
									+ matches[j].replace(':', '-'), handler);
						}
					}
					Util.print("... done parsing ");
				}
				if (sm_renumber.getValue()) {
					handler.renumber();
				}
				handler.useTGM();
				if (verbose)
					handler.printDuplicates();
				String copyImagesFrom = sm_copyImagesFrom.getValue();
				if (copyImagesFrom != null)
					handler.copyImagesNoURI(copyImagesFrom);
				String urlGetter = sm_image_url_getter.getValue();
				if (urlGetter != null)
					handler.loadImages(urlGetter, sm_image_regexp.getValue(),
							sm_image_max_dimension.getValue(), sm_image_quality
									.getValue());
				String facetTypesToMerge = sm_toMerge.getValue();
				if (facetTypesToMerge != null)
					handler.mergeDuplicateItems(facetTypesToMerge);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	private static String[] getFilenames(String directory,
			final String filenamePattern) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir1, String name) {
				String pattern = filenamePattern.replaceAll("\\\\", "\\\\\\\\");
				pattern = pattern.replaceAll("\\.", "\\\\.");
				pattern = pattern.replaceAll("\\*", ".*");
				return name.matches(pattern);
			}
		};
		return new File(directory).list(filter);
	}

	private static List<String[]>  parsePairFiles(String directory, String filenames) {
		List<String[]> result = null;
		if (filenames != null) {
			result = new LinkedList<String[]> ();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String filename = fnames[j];
				Util.print("Parsing " + filename);
				if (directory != null)
					filename = directory + filename;
				String[] renames = Util.readFile(filename).split("\n");
				for (int i = 0; i < renames.length; i++) {
					String[] pair = renames[i].split("\t");
					assert pair.length == 2 : "On line " + (i + 1)
							+ " of file " + filename
							+ ":\n Should have exactly one tab character: "
							+ renames[i];
					result.add(pair);
				}
			}
		}
		return result;
	}

	private static Set<String>  parseSingletonFiles(String directory, String filenames) {
		Set<String>  result = null;
		if (filenames != null) {
			result = new HashSet<String> ();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String filename = fnames[j];
				Util.print("Parsing " + filename);
				if (directory != null)
					filename = directory + filename;
				String[] renames = Util.readFile(filename).split("\n");
				for (int i = 0; i < renames.length; i++) {
					result.add(renames[i]);
				}
			}
		}
		return result;
	}

	static int db(JDBCSample jdbc, String sql) {
		int nRows = 0;
		boolean dontUpdate = false;
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

	// private static String[] attrTables;

	// private static JDBCSample jdbc;

	private static void clearTables(JDBCSample jdbc) {
		String[] tables = { "raw_facet", "raw_item_facet", "item",
		/* "places_hierarchy" */};
		for (int i = 0; i < tables.length; i++) {
			db(jdbc, "TRUNCATE TABLE " + tables[i]);
		}
		// for (int i = 0; i < attrTables.length; i++) {
		// db("TRUNCATE TABLE " + attrTables[i]);
		// }
	}

	private static void createTables(JDBCSample jdbc, String db)
			throws SQLException {
		String copyFrom = "wpa";

		String[] tablesToCopy = { "raw_facet_type", "globals", "043places" };
		for (int i = 0; i < tablesToCopy.length; i++) {
			if (!tableExists(jdbc, db, tablesToCopy[i])) {
				db(jdbc, "CREATE TABLE " + tablesToCopy[i] + " LIKE "
						+ copyFrom + "." + tablesToCopy[i]);
				db(jdbc, "REPLACE INTO " + tablesToCopy[i] + " SELECT * FROM "
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
			db(jdbc, "CREATE TABLE IF NOT EXISTS " + tables[i] + " LIKE "
					+ copyFrom + "." + tables[i]);
		}

		// db("CREATE OR REPLACE VIEW thumbnails AS SELECT * FROM images");

		// for (int i = 0; i < attrTables.length; i++) {
		// db("CREATE TABLE IF NOT EXISTS " + attrTables[i] + " LIKE "
		// + copyFrom + ".title");
		// }
	}

	static boolean tableExists(JDBCSample jdbc, String db, String table)
			throws SQLException {
		return jdbc
				.SQLqueryInt("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
						+ db + "' AND TABLE_NAME = '" + table + "'") > 0;
	}
}
