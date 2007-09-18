package edu.cmu.cs.bungee.dbScripts;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;


import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.image.codec.jpeg.ImageFormatException;

import edu.cmu.cs.bungee.javaExtensions.*;

public class Movie {

	static final String titlePattern = "\"?(.*?\"?\\s*\\([0-9/VXI\\?]*\\)(?: \\(.*?\\))?(?: \\{.*\\})?)";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			SaxMovieHandler handler = new SaxMovieHandler(args[0], false);
			String directory = "C:\\Projects\\ArtMuseum\\InfoVisMovieContest";
			 parser.parse(directory + "\\moviedb.xml", handler);
			handler.checkList("MPAA Rating", "MV: " + titlePattern
					+ " Rated (\\S*) for (.*)", directory
					+ "\\mpaa-ratings-reasons-reformatted.list");
			// handler
			// .checkList(
			// "Location",
			// titlePattern + "\\s*(.+?)(?:\\s*\\(.*\\))*",
			// directory + "\\locations.list");
			// handler
			// .checkList(
			// "Length",
			// titlePattern + "\\s*(?:[a-zA-Z ]*:)?(\\d+)(?:\\s*\\(.*\\))*",
			// directory + "\\running-times.list");
			// handler
			// .checkList(
			// "Subject",
			// titlePattern + "\\s*(.+)",
			// directory + "\\keywords.list");
			//						
			// //
			// SaxHandler sHandler = new SaxHandler(args[0], false);
			// sHandler.promoteSingletons();

			// http://www.starpulse.com/Movies/Crash/

			// SaxWFMovieHandler callback = new SaxWFMovieHandler(args[0],
			// false);
			// callback.moviegoods();
			// ParserDelegator HTMLparser = new ParserDelegator();
			// for (int i = 2000; i < 2008; i++) {
			// String URLname =
			// "http://www.wfu.edu/cgi-bin/cgiwrap-3.9/~library/media/film_search.cgi?date="
			// + i + "&language=&title=&boolean=and&Submit=Search";
			// URL URL = new URL(URLname);
			// Reader reader = new BufferedReader(new InputStreamReader(
			// URL.openStream()));
			// callback.year = Integer.toString(i);
			// callback.URL = URL;
			// HTMLparser.parse(reader, callback, true);
			// }

		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}
}

final class SaxWFMovieHandler extends HTMLEditorKit.ParserCallback {

	String year;

	URL URL;

	static final boolean dontUpdate = false;

	JDBCSample jdbc;

	private String dbName;

	private String img;

	private String td;

	private String recordURL;

	String data;

	SaxWFMovieHandler(String connectString, boolean clearTables) {
		jdbc = new JDBCSample(null);
		try {
			jdbc.openMySQL(connectString);
			if (clearTables) {
				// createTables();
				// clearTables();
				// parseTGM();
				// parse043codes();
				// checkMultipleParents("places_hierarchy");
				// checkMultipleParents("TGM");
			}

			Matcher m = Pattern.compile("/(\\w*)\\?").matcher(connectString);
			if (m.find()) {
				dbName = m.group(1);
			}
			Util.print("dbname = " + dbName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void jackasscritics() throws SQLException {
		ResultSet rs = jdbc.SQLquery("SELECT item.record_num, title FROM item "
				+ "LEFT JOIN images ON item.record_num = images.record_num "
				+ "WHERE images.record_num IS NULL"
		// + " AND title > 'Painu'"
				);
		while (rs.next()) {
			String[] titleWords = rs.getString(2).split(" ");
			int recordNum = rs.getInt(1);
			for (int i = 0; i < titleWords.length; i++)
				titleWords[i] = titleWords[i].toLowerCase();
			String title = Util.join(titleWords, "_");
			if (title.endsWith(",_the"))
				title = title.replaceAll(",_the", "");
			if (title.endsWith(",_a"))
				title = title.replaceAll(",_a", "");
			try {
				String img1 = "http://www.jackasscritics.com/images/movies/"
						+ title + "_01.jpg";
				String[] loc = img1.split("/");
				loadImageInternal(
						"C:\\Projects\\ArtMuseum\\InfoVisMovieContest\\images",
						img1, URL, recordNum, loc[loc.length - 1]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void starPulse() throws SQLException {
		ResultSet rs = jdbc
				.SQLquery("SELECT record_num, title FROM item WHERE URI IS NULL"
						+ " AND title > 'Painu'");
		while (rs.next()) {
			String[] titleWords = rs.getString(2).split(" ");
			// for (int i=0; i<titleWords.length; i++)
			// titleWords[i] = SaxMovieHandler.capitalize(titleWords[i]);
			try {
				URL url = new URL("http://www.starpulse.com/Movies/"
						+ Util.join(titleWords, "_") + "/index.html");
				String content = Util.fetch(url);
				int index = content
						.indexOf("http://images.starpulse.com/AMGPhotos/");
				if (index > 0) {
					int endIndex = content.indexOf(".jpg", index);
					assert endIndex > 0 : index + " " + content;
					String img1 = content.substring(index, endIndex + 4);
					int recordNum = rs.getInt(1);
					String[] loc = img1.split("/");
					loadImageInternal(
							"C:\\Projects\\ArtMuseum\\InfoVisMovieContest\\images",
							img1, URL, recordNum, loc[loc.length - 1]);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private PreparedStatement moviegoods;

	void moviegoods() throws SQLException, IOException {
		moviegoods = jdbc
				.prepareStatement("SELECT record_num FROM item WHERE keyTitle = ?");
		for (int year1 = 2001; year1 < 2008; year1++) {
			Util.print(year1);
			for (int page = 1; page < 21; page++) {
				Util.print("Page " + page);
				URL url;
				try {
					url = new URL(
							"http://www.moviegoods.com/find.asp?mscssid=&str1="
									+ year1 + "&pre1=&opt1=RD&showForm=N&page="
									+ page);
					String content = Util.fetch(url);
					if (content != null) {
						String[] records = content
								.split("<img src=\"/Assets/product_images/");
						for (int record = 1; record < records.length; record++) {
							String html = records[record];
							int index1 = html.indexOf("ALT=\"");
							int index2 = html.indexOf("\">", index1);
							String title = html.substring(index1 + 5, index2);
							moviegoods.setString(1, title);
							int recordNum = jdbc.SQLqueryInt(moviegoods,
									"Lookup record from title");
							if (recordNum >= 0) {
								int index3 = html.indexOf(".JPG\"");
								String img1 = "http://www.moviegoods.com/Assets/product_images/"
										+ html.substring(0, index3 + 4);
								String loc = recordNum + ".jpg";
								try {
									loadImageInternal(
											"C:\\Projects\\ArtMuseum\\InfoVisMovieContest\\images",
											img1, URL, recordNum, loc);
								} catch (ImageFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					} else {
						page = 100;
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes,
			int position) {
		// Util.print("startElement " + tag);
		getValue(attributes, "sdg");
		if (tag.toString().equals("tr")) {
			img = null;
		} else if (tag.toString().equals("td")) {
			td = getValue(attributes, "class");
			// Util.print("td=" + td);
		} else if (tag.toString().equals("a") && "title".equals(td)) {
			recordURL = getValue(attributes, "href");
			// Util.print("recordURL=" + recordURL);
			// data = "";
		}
	}

	String getValue(MutableAttributeSet attributes, String attrName) {
		Enumeration e = attributes.getAttributeNames();
		while (e.hasMoreElements()) {
			Object attr = e.nextElement();
			// Util.print(" " + attr + "='" + attributes.getAttribute(attr) +
			// "'");
			if (attr.toString().equals(attrName))
				return attributes.getAttribute(attr).toString();
		}
		return null;
	}

	// protected void dumpAttributes(AttributeSet atts) {
	// Enumeration enum = atts.getAttributeNames();
	//
	// // Sort them to ensure the same order every time:
	// TreeSet t = new TreeSet();
	// while (enum.hasMoreElements())
	// t.add(enum.nextElement().toString());
	//
	// Iterator iter = t.iterator();
	//
	// while (iter.hasNext()) {
	// String a = iter.next().toString();
	//
	// // if (hideImplied)
	// // if (a.equals("_implied_"))
	// // continue;
	//
	// Object o = atts.getAttribute(a);
	// if (o == null)
	// Util.print(" " + a + "=null");
	// else {
	// String v = o.toString();
	// Util.print(" " + a + "='" + v + "'");
	// }
	// }
	// }

	public void handleText(char[] chars, int position) {
		String s = new String(chars);
		// Util.print("handleText " + s);
		// if (data != null) {
		data = s;
		// }
	}

	public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes,
			int position) {
		if (tag.toString().equals("img") && "thumb".equals(td)) {
			img = getValue(attributes, "src");
			// Util.print("img=" + img);
		}
	}

	private PreparedStatement lookupTitle;

	private PreparedStatement setURI;

	public void handleEndTag(HTML.Tag tag, int position) {
		// Util.print("endElement " + tag);
		String value = null;
		if (data != null && data.length() > 0)
			value = SaxMovieHandler.trim(data);
		if ("a".equals(tag.toString()) && "title".equals(td)) {
			// assert value != null : "No " + qName + " " + recordURL;
			try {
				if (lookupTitle == null) {
					lookupTitle = jdbc
							.prepareStatement("SELECT DISTINCT item.record_num FROM item "
									+ "INNER JOIN raw_item_facet i_f ON item.record_num = i_f.record_num "
									+ "INNER JOIN raw_facet f ON i_f.facet_id = f.facet_id "
									+ "WHERE title = ? AND f.name = ? AND URI IS NULL");
				}
				value = hackTitle(value);
				lookupTitle.setString(1, value);
				lookupTitle.setString(2, year);
				int recordNum = jdbc.SQLqueryInt(lookupTitle, "Lookup title");
				Util.print("title: '" + value + "' " + recordNum);
				if (recordNum >= 0) {
					if (setURI == null) {
						setURI = jdbc
								.prepareStatement("UPDATE item SET URI = ? WHERE record_num = ?");
					}
					setURI.setString(1, recordURL);
					setURI.setInt(2, recordNum);
					jdbc.SQLupdate(setURI, "Set URI");
					if (img != null && img.length() > 0)
						loadImageInternal(
								"C:\\Projects\\ArtMuseum\\InfoVisMovieContest\\images",
								img, URL, recordNum, img);
				}
			} catch (Throwable e) {
				Util.err(year + " " + value);
				e.printStackTrace();
			}
		}
		td = null;
		// data = null;
	}

	String hackTitle(String title) {
		if (title.startsWith("The "))
			return title.substring(4) + ", The";
		if (title.startsWith("A "))
			return title.substring(4) + ", A";
		return title;
	}

	void loadImageInternal(String directory, String thumbURL, URL base,
			int record, String loc) throws ImageFormatException,
			InterruptedException, IOException {
		// Util.print(thumbURL + " " + record);
		String[] filenames = loc.split("\\.");
		// Util.print(loc);
		// PrintArray.printArray(filenames);
		String filename = directory + filenames[filenames.length - 2];
		int w = 0;
		int h = 0;
		if ((new File(filename + CreateRawSaxHandler.imageFileExtension)).exists()) {
			filename += CreateRawSaxHandler.imageFileExtension;
			Util.print("Using existing file " + filename);
		} else {
			Vector info = CreateRawSaxHandler.download(base, thumbURL, filename);
			if (info != null) {
				filename = (String) info.get(0);
				w = ((Integer) info.get(1)).intValue();
				h = ((Integer) info.get(2)).intValue();
			} else {
				Util.print("Cant find " + thumbURL);
				filename = null;
			}
		}
		if (filename != null)
			setImage(record, filename, loc, Math.max(0, w), Math.max(0, h));
	}

	private PreparedStatement setImage;

	private void setImage(int record_num, String filename, String URI, int w,
			int h) {
		try {
			if (setImage == null) {
				setImage = jdbc
						.prepareStatement("INSERT INTO images VALUES(?, LOAD_FILE(?), ?, ?, ?)");
			}
			setImage.setInt(1, record_num);
			setImage.setString(2, filename);
			setImage.setString(3, URI);
			setImage.setInt(4, w);
			setImage.setInt(5, h);
			jdbc.SQLupdate(setImage, "Set image");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

final class SaxMovieHandler extends DefaultHandler {

	static final boolean dontUpdate = false;

	JDBCSample jdbc;

	private String dbName;

	private int recordNum;

	private int facetID;

	private String subfield = null;

	StringBuffer data;

	private final static String[] attrTables = { // "title" // ,
	// "description"
	// "series", "summary", "note"
	};

	SaxMovieHandler(String connectString, boolean clearTables) {

		Matcher m = Pattern.compile("/(\\w*)\\?").matcher(connectString);
		if (m.find()) {
			dbName = m.group(1);
		}
		Util.print("dbname = " + dbName);

		jdbc = new JDBCSample(null);
		try {
			jdbc.openMySQL(connectString);
			if (clearTables) {
				createTables();
				clearTables();
				// parseTGM();
				// parse043codes();
				// checkMultipleParents("places_hierarchy");
				// checkMultipleParents("TGM");
			}

			recordNum = jdbc.SQLqueryInt("SELECT MAX(record_num) FROM item");
			facetID = Math
					.max(
							jdbc
									.SQLqueryInt("SELECT MAX(facet_id) FROM raw_facet"),
							jdbc
									.SQLqueryInt("SELECT MAX(facet_type_id) FROM raw_facet_type") + 100);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private PreparedStatement lookupKey;

	public void checkList(String type, String pattern, String filename) {
		Pattern listPattern = Pattern.compile(pattern);
		try {
			if (lookupKey == null) {
				lookupKey = jdbc
						.prepareStatement("SELECT record_num FROM item WHERE keyTitle = ?");
			}
			BufferedReader r = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = r.readLine()) != null) {
				Matcher m = listPattern.matcher(line);
				if (m.matches()) {
					String key = m.group(1);
					lookupKey.setString(1, key);
					recordNum = jdbc.SQLqueryInt(lookupKey,
							"Lookup record_num from title");
					if (recordNum >= 0) {
						String value = m.group(2);
						if (type.equals("Length")) {
							// int hours = (value != null) ? Integer
							// .parseInt(value) : 0;
							// int minutes = Integer.parseInt(m.group(3));
							// value = Integer.toString(hours * 60 + minutes);
							addLength(value);
						} else if (type.equals("Location")) {
							String[] values = value.split(", ");
							int parent = locationType();
							for (int i = values.length - 1; i >= 0; i--) {
								parent = addTriple(locationType(), values[i],
										parent);
							}
						} else if (type.equals("Subject")) {
							String[] values = value.split("-");
							for (int i = values.length - 1; i >= 0; i--) {
								values[i] = capitalize(values[i]);
							}
							value = Util.join(values, " ");
							addTriple(subjectType(), value, subjectType());
						} else if (type.equals("MPAA Rating")) {
							int rating = addTriple(mpaaType(), value,
									mpaaType());
							String[] values = m.group(3).split(",|\\.|( and )");
							for (int i = values.length - 1; i >= 0; i--) {
								String reason = values[i].trim();
								if (reason.startsWith("for ")) {
									reason = reason.substring(3).trim();
								}
								if (reason.length() > 0) {
									reason = capitalize(reason);
									addTriple(mpaaType(), reason, rating);
								}
							}
						} else
							assert false : "Unknown type: " + type;
						// Util.print(key + " " + type + " " + value);
					}
				} else {
					Util.print("Don't recognize " + line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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
		String copyFrom = "hp2";
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

		// String[] tablesToCopy = { "raw_facet_type", "globals" };
		// for (int i = 0; i < tablesToCopy.length; i++) {
		// db("REPLACE INTO " + tablesToCopy[i] + " SELECT * FROM " + copyFrom
		// + "." + tablesToCopy[i]);
		// }
		for (int i = 0; i < attrTables.length; i++) {
			db("CREATE TABLE IF NOT EXISTS " + attrTables[i] + " LIKE "
					+ copyFrom + ".title");
		}
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

	public void startElement(String uri, String localName, String qName,
			Attributes attrs) {
		if (qName.equals("movie")) {
			newItem(attrs.getValue("title"));
			String nr = attrs.getValue("numratings");
			if (nr != null) {
				int nRatings = Integer.parseInt(nr);
				if (nRatings > 0) {
					String rating = attrs.getValue("rating");
					addRating(rating);
				}
			}
			String box = attrs.getValue("boxoffice");
			if (box != null)
				addBox(box);
			String year = attrs.getValue("year");
			addTriple(yearType(), year, -1);
			// addTitle(attrs.getValue("title"));
			String release = attrs.getValue("releasedate");
			if (release != null)
				addRelease(release);
		} else {
			if (qName.equals("actor"))
				subfield = attrs.getValue("sex");
			else if (qName.equals("oscar"))
				subfield = attrs.getValue("type");
			data = new StringBuffer();
		}
	}

	private PreparedStatement newItem;

	// Pattern titlePattern =
	// Pattern.compile("\"?(.*?)\"?\\s*\\([0-9/VXI]*\\)");

	Pattern titlePattern = Pattern.compile(Movie.titlePattern);

	void newItem(String title) {
		try {
			if (newItem == null) {
				newItem = jdbc
						.prepareStatement("INSERT INTO item VALUES(null, null, ?, ?, ?)");
			}
			Matcher m = titlePattern.matcher(title);
			if (!m.find()) {
				assert false : "Bad title: " + title;
			}
			newItem.setInt(1, ++recordNum);
			newItem.setString(2, m.group(1));
			newItem.setString(3, title);
			if (!dontUpdate) {
				jdbc.SQLupdate(newItem, "Add new item");
			} else
				Util
						.print("INSERT INTO item VALUES(null, null, <next recordNum>, null)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void characters(char[] ch, int start, int length) {
		if (data != null) {
			data.append(ch, start, length);
		}
	}

	static String trim(String value) {
		String result = value.replaceAll("\\A\\W*", "");
		result = result.replaceAll("[\\W&&[^\\)]]*\\z", "");
		// Util.print(value + " => " + result);
		return result;
	}

	private Hashtable oscarType;

	String oscarType(String name) {
		if (oscarType == null) {
			oscarType = new Hashtable();
			oscarType.put("bestpicture", "Best Picture");
			oscarType.put("cinematography", "Cinematography");
			oscarType.put("directing", "Directing");
			oscarType.put("leadingactor", "Leading Actor");
			oscarType.put("leadingactress", "Leading Actress");
			oscarType.put("supportingactor", "Supporting Actor");
			oscarType.put("supportingactress", "Supporting Actress");
		}
		return (String) oscarType.get(name);
	}

	public void endElement(String uri, String localName, String qName) {
		String value = null;
		if (data != null && data.length() > 0)
			value = trim(data.toString());
		if ("director,cinematographer".indexOf(qName) >= 0) {
			assert value != null : "No " + qName + " " + recordNum;
			int type = qName.equals("director") ? directorType()
					: cinematographerType();
			addTriple(type, value, -1);
		} else if (qName.equals("genre")) {
			assert value != null : "No " + qName + " " + recordNum;
			addTriple(genreType(), capitalize(value), -1);
		} else if (qName.equals("actor")) {
			assert value != null : "No " + qName + " " + recordNum;
			int type = subfield.equals("male") ? actorType() : actressType();
			addTriple(type, value, -1);
		} else if (qName.equals("oscar")) {
			int parent = addTriple(oscarType(), oscarType(subfield), -1);
			if (value != null)
				addTriple(oscarType(), value, parent);
		}
		data = null;
	}

	static String capitalize(String s) {
		if (s.length() == 0)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private PreparedStatement addTriple;

	int addTriple(int type, String value, int parent) {
		// Util.print("addTriple " + recordNum + " " + attr + " " + value);
		if (parent < 0)
			parent = type;
		int facet = lookupFacet(value, parent, type);
		try {
			if (addTriple == null) {
				addTriple = jdbc
						.prepareStatement("REPLACE INTO raw_item_facet VALUES(?, ?)");
			}
			addTriple.setInt(1, recordNum);
			addTriple.setInt(2, facet);
			jdbc.SQLupdate(addTriple, "Add triple");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return facet;
	}

	// int getFacet(int type, int parent, String value) {
	// // int type = lookupFacetType(name);
	// int result = lookupFacet(value, parent, type);
	// return result;
	// }

	private PreparedStatement lookupFacets;

	private PreparedStatement addFacet;

	private int lookupFacet(String name, int parent, int type) {
		// Util.print("lookupFacet " + name + " " + type + " " + parentName);
		int result = -1;
		try {
			if (lookupFacets == null) {
				lookupFacets = jdbc
						.prepareStatement("SELECT f.facet_id FROM raw_facet f WHERE f.name = ? AND f.parent_facet_id = ?");
			}
			// int parent = parentName == null ? type : lookupFacet(
			// parentName, null, type);
			lookupFacets.setString(1, name);
			lookupFacets.setInt(2, parent);
			result = jdbc.SQLqueryInt(lookupFacets, "Get facet ID");
			if (result < 0) {
				if (addFacet == null) {
					addFacet = jdbc
							.prepareStatement("INSERT INTO raw_facet VALUES(?, ?, null, ?, ?)");
				}
				addFacet.setInt(1, ++facetID);
				addFacet.setString(2, name);
				addFacet.setInt(3, parent);
				addFacet.setInt(4, type);
				jdbc.SQLupdate(addFacet, "Add facet");
				result = facetID;
			}
		} catch (SQLException e) {
			Util.err(name + " " + parent + " " + type);
			e.printStackTrace();
		}
		return result;
	}

	private PreparedStatement lookupFacetType;

	private int lookupFacetType(String name) {
		int result = -1;
		try {
			if (lookupFacetType == null) {
				lookupFacetType = jdbc
						.prepareStatement("SELECT f.facet_type_id FROM raw_facet_type f WHERE f.name = ?");
			}
			lookupFacetType.setString(1, name);
			result = jdbc.SQLqueryInt(lookupFacetType, "Get facet type ID");
			if (result < 0) {
				db("INSERT INTO raw_facet_type VALUES(null, "
						+ JDBCSample.quote(name) + ", null, null, null, null)");
				result = lookupFacetType(name);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	private void addRating(String rating) {
		// Util.print(rating);
		// Util.print(rating.split("\\.").length);
		String parentName = rating.split("\\.")[0];
		while (parentName.length() < 2)
			parentName = " " + parentName;
		parentName += ".x";
		// int type = lookupFacetType("Average Rating");
		int parent = addTriple(ratingType(), parentName, -1);
		addTriple(ratingType(), rating, parent);
	}

	private String format(String box, int nChars) {
		box = Util.addCommas(Integer.parseInt(box));
		return pad(box, nChars);
	}

	private String pad(String s, int nChars) {
		while (s.length() < nChars)
			s = " " + s;
		return s;
	}

	private int boxType = -1;

	private int boxType() {
		if (boxType < 0)
			boxType = lookupFacetType("Box Office");
		return boxType;
	}

	private int lengthType = -1;

	private int lengthType() {
		if (lengthType < 0)
			lengthType = lookupFacetType("Length");
		return lengthType;
	}

	private int ratingType = -1;

	private int ratingType() {
		if (ratingType < 0)
			ratingType = lookupFacetType("Average Rating");
		return ratingType;
	}

	private int releaseType = -1;

	private int releaseType() {
		if (releaseType < 0)
			releaseType = lookupFacetType("Release Date");
		return releaseType;
	}

	private int oscaType = -1;

	private int oscarType() {
		if (oscaType < 0)
			oscaType = lookupFacetType("Oscar");
		return oscaType;
	}

	private int yearType = -1;

	private int yearType() {
		if (yearType < 0)
			yearType = lookupFacetType("Year");
		return yearType;
	}

	private int locationType = -1;

	private int locationType() {
		if (locationType < 0)
			locationType = lookupFacetType("Location");
		return locationType;
	}

	private int subjectType = -1;

	private int subjectType() {
		if (subjectType < 0)
			subjectType = lookupFacetType("Subject");
		return subjectType;
	}

	private int mpaaType = -1;

	private int mpaaType() {
		if (mpaaType < 0)
			mpaaType = lookupFacetType("MPAA Rating");
		return mpaaType;
	}

	private int actressType = -1;

	private int actressType() {
		if (actressType < 0)
			actressType = lookupFacetType("Actress");
		return actressType;
	}

	private int genreType = -1;

	private int genreType() {
		if (genreType < 0)
			genreType = lookupFacetType("Genre");
		return genreType;
	}

	private int actorType = -1;

	private int actorType() {
		if (actorType < 0)
			actorType = lookupFacetType("Actor");
		return actorType;
	}

	private int directorType = -1;

	private int directorType() {
		if (directorType < 0)
			directorType = lookupFacetType("Director");
		return directorType;
	}

	private int cinematographerType = -1;

	private int cinematographerType() {
		if (cinematographerType < 0)
			cinematographerType = lookupFacetType("Cinematographer");
		return cinematographerType;
	}

	private void addBox(String box) {
		int len = box.length();
		String parentName = box.substring(0, 1);
		while (parentName.length() < box.length())
			parentName += "0";
		parentName = format(parentName, 11);
		parentName = parentName.replace('0', 'x');
		box = format(box, 11);
		int type = boxType();
		int parent = lookupFacet(parentName, type, type);
		if (len > 1)
			addTriple(boxType(), box, parent);
	}

	private void addLength(String minutes) {
		int len = minutes.length();
		String parentName = minutes.substring(0, 1);
		while (parentName.length() < minutes.length())
			parentName += "0";
		parentName = format(parentName, 3);
		parentName = parentName.replace('0', 'x');
		minutes = format(minutes, 3);
		int type = lengthType();
		int parent = lookupFacet(parentName, type, type);
		if (len > 1)
			addTriple(lengthType(), minutes, parent);
	}

	private Pattern releasePattern = Pattern
			.compile("(\\d{1,2}) (\\w{3}) (\\d{4})");

	private PreparedStatement lookupMonthOrder;

	private PreparedStatement setMonthOrder;

	private void addRelease(String release) {
		Matcher m = releasePattern.matcher(release);
		if (!m.matches()) {
			assert false : "Bad release: " + release;
		} else {
			String year = m.group(3);
			String month = month(m.group(2)) + " " + year;
			// int type = lookupFacetType("Release Date");
			int yearID = addTriple(releaseType(), year, -1);
			int monthID = addTriple(releaseType(), month, yearID);
			addTriple(releaseType(), release, monthID);
			try {
				if (lookupMonthOrder == null) {
					lookupMonthOrder = jdbc
							.prepareStatement("SELECT ordered_child_names FROM raw_facet WHERE name = ? AND parent_facet_id = ?");
				}
				lookupMonthOrder.setString(1, year);
				lookupMonthOrder.setInt(2, releaseType());
				if (jdbc.SQLqueryString(lookupMonthOrder, "Lookup month order") == null) {
					if (setMonthOrder == null) {
						setMonthOrder = jdbc
								.prepareStatement("UPDATE raw_facet SET ordered_child_names = ? WHERE name = ? AND parent_facet_id = ?");
					}
					setMonthOrder.setString(1,
							"Jan ?,Feb ?,Mar ?,Apr ?,May ?,Jun ?,Jul ?,Aug ?,Sep ?,Oct ?,Nov ?,Dec ?"
									.replaceAll("?", year));
					setMonthOrder.setString(2, year);
					setMonthOrder.setInt(3, releaseType());
					jdbc.SQLupdate(setMonthOrder, "Set month order");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private Hashtable months;

	private String month(String abbr) {
		if (months == null) {
			months = new Hashtable();
			String[] names = "January,February,March,April,May,June,July,August,September,October,November,December"
					.split(",");
			for (int i = 0; i < names.length; i++) {
				String abb = names[i].substring(0, 3);
				months.put(abb, names[i]);
			}
		}
		String result = (String) months.get(abbr);
		assert result != null;
		return result;
	}
}
