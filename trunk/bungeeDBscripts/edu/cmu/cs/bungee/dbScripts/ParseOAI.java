package edu.cmu.cs.bungee.dbScripts;

import java.io.File;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
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
 * Args for HP: -db hp3 -user root -pass tartan01 -reset -verbose -directory
 * C:\Projects\ArtMuseum\HistoricPittsburgh\OAI-2007-Dec\ -files
 * drlimg-accdbib.xml,drlimg-aerialbib.xml,drlimg-allegobbib.xml,drlimg-cmabib.xml,drlimg-cmaharrisbib.xml,drlimg-consolbib.xml,drlimg-cpbib.xml,drlimg-darlingtonbib.xml,drlimg-fcoxbib.xml,drlimg-fwagbib.xml,drlimg-gnbib.xml,drlimg-gretbib.xml,drlimg-gtbib.xml,drlimg-hjhzbib.xml,drlimg-iksbib.xml,drlimg-jalbib.xml,drlimg-jbenbib.xml,drlimg-kabib.xml,drlimg-lyshbib.xml,drlimg-mestbib.xml,drlimg-pghrailbib.xml,drlimg-ppsbib.xml,drlimg-rrbib.xml,drlimg-shourekbib.xml,drlimg-smokebib.xml,drlimg-spencerbib.xml,drlimg-trimbib.xml,drlimg-uapittbib.xml,drlimg-uebib.xml,drlimg-unionarcadebib.xml,drlimg-urbanbib.xml
 * "hp-drlimg-chathambib.xml,hp-drlimg-darlfamilybib.xml,hp-drlimg-fairbanksbib.xml,hp-drlimg-kaufbib.xml,hp-drlimg-pghprintsbib.xml,hp-drlimg-rustbib.xml,hp-drlimg-stotzbib.xml,hp-drlimg-switchbib.xml"
 * -cities Places.txt -renames Rename.txt -moves
 * Moves.txt,TGM.txt,places_hierarchy.txt -image_url_getter URI -image_regexp
 * src=\"(/cgi-bin.*?)\"
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
	static BooleanToken sm_verbose = new BooleanToken("verbose",
			"print tag hierarchy manipulations?", "", Token.optSwitch, false);

	static ApplicationSettings sm_main = new ApplicationSettings();
	static {
		sm_main.addToken(sm_db);
		sm_main.addToken(sm_server);
		sm_main.addToken(sm_user);
		sm_main.addToken(sm_pass);
		sm_main.addToken(sm_image_url_getter);
		sm_main.addToken(sm_image_regexp);
		sm_main.addToken(sm_image_max_dimension);
		sm_main.addToken(sm_image_quality);
		sm_main.addToken(sm_files);
		sm_main.addToken(sm_directory);
		sm_main.addToken(sm_reset);
		sm_main.addToken(sm_verbose);
		sm_main.addToken(sm_places);
		sm_main.addToken(sm_renames);
		sm_main.addToken(sm_moves);
	}

	static JDBCSample createJDBC(String server, String db, String user,
			String pass) {
		String connectString = server + db + "?user=" + user;
		if (pass != null)
			connectString += "&password=" + pass;
		return createJDBC(connectString);
	}

	static JDBCSample createJDBC(String connectString) {
		JDBCSample jdbc = new JDBCSample(null);
		try {
			jdbc.openMySQL(connectString);

			// createTables();
			// clearTables();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jdbc;
	}

	static void parseTGM(Map use, Map bt, String directory, String filename) {
		if (filename != null) {
			File f = new File(directory, filename);
			// BufferedReader in = new BufferedReader(f);
			// while (true) {
			// String[] s = in.readLine().split("\t");
			// abbrevs.put(s[0], Util.subArray(s, 1, String.class));
			// }
		}

	}

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
				JDBCSample jdbc = createJDBC(sm_server.getValue(), dbName,
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
					SAXParser parser = SAXParserFactory.newInstance()
							.newSAXParser();
					for (int i = 0; i < filenames.length; i++) {
						Util.print("\nParsing " + filenames[i]);
						parser.parse(
								directory + filenames[i].replace(':', '-'),
								handler);
					}
				}
				handler.renumber();
				handler.useTGM();
				if (verbose)
					handler.printDuplicates();
//				handler.copyImagesNoURI("hp2");
				String urlGetter = sm_image_url_getter.getValue();
				if (urlGetter != null)
					handler.loadImages(urlGetter, sm_image_regexp.getValue(),
							sm_image_max_dimension.getValue(), sm_image_quality
									.getValue());
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	private static Hashtable parsePairFiles(String directory, String filenames) {
		Hashtable result = null;
		if (filenames != null) {
			result = new Hashtable();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String filename = fnames[j];
				if (directory != null)
					filename = directory + filename;
				String[] renames = Util.readFile(filename).split("\n");
				for (int i = 0; i < renames.length; i++) {
					String[] pair = renames[i].split("\t");
					result.put(pair[0], pair[1]);
				}
			}
		}
		return result;
	}

	private static Set parseSingletonFiles(String directory, String filenames) {
		Set result = null;
		if (filenames != null) {
			result = new HashSet();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String filename = fnames[j];
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

		String[] tablesToCopy = { "raw_facet_type", "globals" };
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
