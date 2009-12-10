package edu.cmu.cs.bungee.dbScripts;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.image.codec.jpeg.ImageFormatException;

import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.ApplicationSettings;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.BooleanToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.IntegerToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.StringToken;
import edu.cmu.cs.bungee.javaExtensions.psk.cmdline.Token;

/**
 * This controls populating the raw tables of a Bungee View database. The
 * default is to use a parser, in which case subclasses should override
 * startElement, etc. Otherwise they should override readData.
 * 
 */
class Options extends DefaultHandler {

	Populate handler;

	void init(boolean isParser, String[] args) throws ImageFormatException,
			SQLException, InterruptedException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException {
		addOptions(isParser);
		if (args != null)
			populate(args);
	}

	void addOptions(boolean isParser) {
		addStringToken("db", "database to write to", Token.optRequired
				| Token.optSwitch, "");
		addStringToken("server", "MySQL server", Token.optSwitch,
				"jdbc:mysql://localhost/");
		addStringToken("user", "MySQL user", Token.optSwitch, "bungee");
		addStringToken("pass", "MySQL user password", Token.optRequired, "");
		addStringToken(
				"toMerge",
				"Merge items with the same URI that differ only in these tag categories",
				Token.optSwitch, null);

		addStringToken("copy_images_from",
				"Database to copy images from, based on the item.URI column",
				Token.optSwitch, null);
		addStringToken("image_url_getter",
				"SQL expression for thumbnail location", Token.optSwitch, null);
		addStringToken("image_regexp", "Java regexp to apply to url",
				Token.optSwitch, null);
		addIntegerToken("image_max_dimension", "Maximum thumbnail size",
				Token.optSwitch, 200);
		addIntegerToken("image_quality", "Thumbnail quality (1-100)",
				Token.optSwitch, 80);
		if (isParser)
			addStringToken("files", "OAI files to read", Token.optSwitch, null);
		addStringToken("cities", "List of place names", Token.optSwitch, null);
		addStringToken("renames", "List of name pairs", Token.optSwitch, null);
		addStringToken("moves", "List of name pairs", Token.optSwitch, null);
		addStringToken("directory", "default directory for file arguments",
				Token.optSwitch, "./");
		addBooleanToken("reset", "clear raw_facet, raw_item_facet, and item?",
				Token.optSwitch);
		addBooleanToken("renumber",
				"renumber record_num to use sequential IDs starting at 1",
				Token.optSwitch);
		addBooleanToken("verbose", "print tag hierarchy manipulations?",
				Token.optSwitch);
		addBooleanToken("dont_read_data",
				"Don't change items or facets; just load images",
				Token.optSwitch);
	}

	ApplicationSettings sm_main = new ApplicationSettings();
	private Map<String, Token> options = new Hashtable<String, Token>();

	// JDBCSample jdbc;

	void addStringToken(String optionName, String documentation,
			int tokenOptions, String defaultValue) {
		Token token = new StringToken(optionName, documentation, "",
				tokenOptions, defaultValue);
		options.put(optionName, token);
		sm_main.addToken(token);
	}

	void addIntegerToken(String optionName, String documentation,
			int tokenOptions, int defaultValue) {
		Token token = new IntegerToken(optionName, documentation, "",
				tokenOptions, defaultValue);
		options.put(optionName, token);
		sm_main.addToken(token);
	}

	void addBooleanToken(String optionName, String documentation,
			int tokenOptions) {
		Token token = new BooleanToken(optionName, documentation, "",
				tokenOptions);
		options.put(optionName, token);
		sm_main.addToken(token);
	}

	String getStringValue(String optionName) {
		StringToken token = (StringToken) options.get(optionName);
		if (token == null)
			return null;
		else
			return token.getValue();
	}

	int getIntegerValue(String optionName) {
		return ((IntegerToken) options.get(optionName)).getValue();
	}

	boolean getBooleanValue(String optionName) {
		BooleanToken token = (BooleanToken) options.get(optionName);
		if (token == null)
			return false;
		else
			return token.getValue();
	}

	void populate(String[] args) throws SQLException, ImageFormatException,
			InterruptedException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException {
		boolean result = sm_main.parseArgs(args);
		if (result) {
			String dbName = getStringValue("db");
			JDBCSample initJdbc = new JDBCSample(getStringValue("server"),
					"mysql", getStringValue("user"), getStringValue("pass"));
			if (!initJdbc.databaseExists(dbName)) {
			initJdbc.SQLupdate("CREATE DATABASE " + dbName);
			initJdbc
					.SQLupdate("GRANT SELECT, INSERT, UPDATE, CREATE, DELETE, ALTER ROUTINE, EXECUTE,"
							+ " CREATE TEMPORARY TABLES, CREATE VIEW, DROP ON "
							+ dbName
							+ ".* TO bungee@localhost");
			}
			JDBCSample jdbc = new JDBCSample(getStringValue("server"), dbName,
					getStringValue("user"), getStringValue("pass"));
			handler = new Populate(jdbc, getBooleanValue("verbose"));

			if (!getBooleanValue("dont_read_data")) {

				// Allows NULLS until compile sets a value
//				jdbc.SQLupdate("ALTER TABLE item MODIFY facet_names TEXT");
				if (getBooleanValue("reset"))
					handler.clearTables();

				String directory = getStringValue("directory");
				String renamesFile = getStringValue("renames");
				if (renamesFile != null)
					handler.setRenames(parsePairFiles(directory, renamesFile));
				String movesFile = getStringValue("moves");
				if (movesFile != null)
					handler.setMoves(parsePairFiles(directory, movesFile));
				String citiesFile = getStringValue("cities");
				if (citiesFile != null)
					handler
							.setPlaces(parseSingletonFiles(directory,
									citiesFile));

				readData();
				handler.cleanUp();

				if (getBooleanValue("renumber"))
					handler.renumber();
				handler.renameNmove();
				if (getBooleanValue("verbose"))
					handler.printDuplicates();

				String facetTypesToMerge = getStringValue("toMerge");
				if (facetTypesToMerge != null)
					handler.mergeDuplicateItems(facetTypesToMerge);
				handler.promoteSingletons();
				handler.compile();
			}

			String copyImagesFrom = getStringValue("copy_images_from");
			if (copyImagesFrom != null)
				handler.copyImagesNoURI(copyImagesFrom);
			String urlGetter = getStringValue("image_url_getter");
			if (urlGetter != null)
				handler.loadImages(urlGetter, getStringValue("image_regexp"),
						getIntegerValue("image_max_dimension"),
						getIntegerValue("image_quality"));
		}
	}

	void readData() throws SAXException, IOException,
			ParserConfigurationException {
		String filenameList = getStringValue("files");
		if (filenameList != null) {
			String directory = getStringValue("directory");
			SAXParser parser = getParser();
			String[] filenames = filenameList.split(",");
			for (int i = 0; i < filenames.length; i++) {
				File[] matches = getFilenames(directory, filenames[i]);
				for (int j = 0; j < matches.length; j++) {
					Util.print("\nParsing " + matches[j]);
					parser.parse(matches[j].toString(), this);
				}
			}
			Util.print("... done parsing ");
		}
	}

	SAXParser getParser() throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();
		return parser;
	}

	static File[] getFilenames(String directory, String filenamePattern) {
		File temp = new File(filenamePattern);
		if (temp.isAbsolute()) {
			directory = temp.getParent().toString();
			filenamePattern = temp.getName();
//			Util.print("Absolute "+filenamePattern);
		}
		final String pattern = filenamePattern.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir1, String name) {
				return name.matches(pattern);
			}
		};
		File dir = new File(directory);
		String[] names = dir.list(filter);
		Util.print(dir + " " + pattern);
		File[] result = new File[names.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new File(dir, names[i].replace(':', '-'));
		}
		return result;
	}

	private static List<String[]> parsePairFiles(String directory,
			String filenames) {
		List<String[]> result = null;
		if (filenames != null) {
			result = new LinkedList<String[]>();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String[] renames = readLines(directory, fnames[j]);
				for (int i = 0; i < renames.length; i++) {
					String[] pair = renames[i].split("\t");
					assert pair.length == 2 : "On line " + (i + 1)
							+ " of file " + fnames[j]
							+ ":\n Should have exactly one tab character: "
							+ renames[i];
					result.add(pair);
				}
			}
		}
		return result;
	}

	private static Set<String> parseSingletonFiles(String directory,
			String filenames) {
		Set<String> result = null;
		if (filenames != null) {
			result = new HashSet<String>();
			String[] fnames = filenames.split(",");
			for (int j = 0; j < fnames.length; j++) {
				String[] renames = readLines(directory, fnames[j]);
				for (int i = 0; i < renames.length; i++) {
					result.add(renames[i]);
				}
			}
		}
		return result;
	}

	private static String[] readLines(String directory, String filename) {
		Util.print("Parsing " + filename);
		File f = new File(directory, filename);
		return Util.readFile(f).split("\n");
	}
}
