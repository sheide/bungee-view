package edu.cmu.cs.bungee.dbScripts;

import java.io.BufferedReader;
import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import edu.cmu.cs.bungee.javaExtensions.*;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.ApplicationSettings;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.StringToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.Token;

/**
 * Here is an example of parsing of XML data with help of document-handler.
 */

public class ParseOAI {
	static StringToken sm_db = new StringToken("db", "database to create", "",
			Token.optRequired | Token.optSwitch, "");
	static StringToken sm_server = new StringToken("server", "MySQL server",
			"", Token.optSwitch, "jdbc:mysql://localhost/");
	static StringToken sm_user = new StringToken("user", "MySQL user", "",
			Token.optSwitch, "p5");
	static StringToken sm_pass = new StringToken("pass", "MySQL user password",
			"", Token.optSwitch, "p5pass");

	static StringToken sm_TGM = new StringToken("tgm",
			"TGM-format files to apply", "", Token.optSwitch, "places.csv");
	static StringToken sm_files = new StringToken("files", "OAI files to read",
			"", Token.optRequired, "");
	static StringToken sm_directory = new StringToken("directory",
			"default directory for file arguments", "", Token.optSwitch, ".");
	// static StringToken sm_ = new StringToken("", "",
	// "", Token.optSwitch, "");

	static ApplicationSettings sm_main = new ApplicationSettings();
	static {
		sm_main.addToken(sm_db);
		sm_main.addToken(sm_server);
		sm_main.addToken(sm_user);
		sm_main.addToken(sm_pass);
		sm_main.addToken(sm_TGM);
		sm_main.addToken(sm_files);
		sm_main.addToken(sm_directory);
	}

	static JDBCSample createJDBC(String server, String db, String user,
			String pass) {
		String connectString = server + db + user;
		 if (pass != null)
		 connectString += "&password=" + pass;
		 return createJDBC(connectString);
	}

	static JDBCSample createJDBC(String connectString) {
		JDBCSample jdbc = new JDBCSample(null);
			try {
				jdbc.openMySQL(connectString);

//				createTables();
//				clearTables();
				
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
				JDBCSample jdbc = createJDBC(sm_server.getValue(), sm_db
						.getValue(), sm_user.getValue(), sm_pass.getValue());

				SAXParser parser = SAXParserFactory.newInstance()
						.newSAXParser();
				String directory = sm_directory.getValue();

				Map use = new Hashtable();
				Map bt = new Hashtable();
				parseTGM(use, bt, directory, sm_TGM.getValue());

				ParseOAIhandler handler = new ParseOAIhandler(jdbc, use, bt);

				String[] filenames = sm_files.getValue().split(",");
				// Historic Pittsburgh fron Archives Service Center
				// "drlimg:kabib", "drlimg:pghrailbib",
				// "drlimg:unionarcadebib", "drlimg:aerialbib",
				// "drlimg:allegobbib", "drlimg:cpbib",
				// "drlimg:darlingtonbib", "drlimg:fcoxbib", "drlimg:gnbib",
				// "drlimg:gtbib", "drlimg:iksbib", "drlimg:rrbib",
				// "drlimg:spencerbib", "drlimg:uapittbib", "drlimg:uebib",
				// "drlimg:urbanbib", "drlimg:consolbib", "drlimg:shourekbib",
				// "drlimg:smokebib",
				//
				// From CMoA
				// "drlimg:cmabib", "drlimg:cmaharrisbib",
				//
				// From Heinz History Center
				// "drlimg:accdbib", "drlimg:fwagbib", "drlimg:gretbib",
				// "drlimg:hjhzbib", "drlimg:jbenbib", "drlimg:jalbib",
				// "drlimg:lyshbib", "drlimg:mestbib", "drlimg:ppsbib",
				// "drlimg:trimbib",
				//
				// "drlimg:chartresbib", "drlimg:vezelaybib"
				//
				// "drlimg:visualsbib"
				for (int i = 0; i < filenames.length; i++) {
					Util.print("\nParsing " + filenames[i]);
					parser.parse(directory + "\\OAI\\"
							+ filenames[i].replace(':', '-') + ".xml", handler);
				}
				handler.renumber();
				handler.useTGM();
				handler.collectDescriptions();
				// handler.copyImages("hp3");
				// handler.loadDRLimages(directory + "\\thumbs\\");
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	int db(String sql) {
		int nRows = 0;
		boolean dontUpdate = true;
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
	
	private static String[] attrTables;
	
	private static JDBCSample jdbc;

	private void clearTables() {
		String[] tables = { "raw_facet", "raw_item_facet", "item",
		/* "places_hierarchy" */};
		for (int i = 0; i < tables.length; i++) {
			db("TRUNCATE TABLE " + tables[i]);
		}
		for (int i = 0; i < attrTables.length; i++) {
			db("TRUNCATE TABLE " + attrTables[i]);
		}
	}

	private void createTables() {
		String copyFrom = "wpa";
		String[] tables = { "images", "raw_facet", "raw_facet_type",
				"user_actions", "raw_item_facet", "item", "globals" /*
																	 * "places_hierarchy",
																	 * "gac",
																	 * "tgm",
																	 * "URIs"
																	 */
		};
		for (int i = 0; i < tables.length; i++) {
			db("CREATE TABLE IF NOT EXISTS " + tables[i] + " LIKE " + copyFrom
					+ "." + tables[i]);
		}

		// db("CREATE OR REPLACE VIEW thumbnails AS SELECT * FROM images");

		String[] tablesToCopy = { "raw_facet_type", "globals" };
		for (int i = 0; i < tablesToCopy.length; i++) {
			db("REPLACE INTO " + tablesToCopy[i] + " SELECT * FROM " + copyFrom
					+ "." + tablesToCopy[i]);
		}
		for (int i = 0; i < attrTables.length; i++) {
			db("CREATE TABLE IF NOT EXISTS " + attrTables[i] + " LIKE "
					+ copyFrom + ".title");
		}
	}
}
