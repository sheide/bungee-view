package edu.cmu.cs.bungee.dbScripts;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;


import com.sun.image.codec.jpeg.ImageFormatException;

import edu.cmu.cs.bungee.javaExtensions.Util;


public class Chartres {

	CreateRawSaxHandler handler;

	String[] colNames;

	String[] record;

	private int lineNo;

	public static void main(String[] args) throws ImageFormatException,
			SQLException, InterruptedException {
		CreateRawSaxHandler handler = new CreateRawSaxHandler(args[1], false);
		String directory = args[0];
		String[] spreadsheets = { "visuals.tab" };

		handler
				.db("UPDATE raw_facet_type SET name = 'Creator' WHERE name = 'Photographer'");
		for (int i = 0; i < spreadsheets.length; i++) {
			Util.print("\nParsing " + spreadsheets[i]);
			BufferedReader in = Util.getReader(directory + "\\"
					+ spreadsheets[i]);
			if (in != null) {
				new Chartres(handler, in);
			}
			Util.print("...done");
		}
		handler.renumber();
		// handler.useTGM();
		handler
				.db("UPDATE raw_facet_type SET name = 'Photographer' WHERE name = 'Creator'");
		handler.collectDescriptions();
		handler.convertFromRaw().fixMissingItemFacets(0);

		handler.loadDRLimages(directory + "\\thumbs\\");
	}

	Chartres(CreateRawSaxHandler _handler, BufferedReader in) {
		handler = _handler;
		try {
			colNames = in.readLine().split("\t");
			String line;
			lineNo = 0;
			while ((line = in.readLine()) != null) {
				if (line.length() > 0) {
					lineNo++;
					record = line.split("\t");
					parse();
				}
			}
			in.close();

			// Enumeration e = h.keys();
			// while (e.hasMoreElements()) {
			// Util.print(e.nextElement());
			// }

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	int colIndex(String name) {
		return Util.member(colNames, name);
	}

	String colValue(String name) {
		int i = colIndex(name);
		String result = null;
		if (i >= 0 && record.length > i)
			result = record[i];
		else {
			// PrintArray.printArray(record);
			Util.print(lineNo + " WARNING: No " + name);
		}
		return result;
	}

	// Hashtable h = new Hashtable();

	private void parse() {
		try {
			String set = "visuals"; // colValue("Location");
			if (set != null) {
				handler.newItem();
				Field field = Field.getField("Collection", Field.FACET);
				int[] facets = handler.getFacets(field, set);
				assert CreateRawSaxHandler.dontUpdate || facets != null : field + " '"
						+ set + "'";
				if (facets != null) {
					assert facets.length == 1 : "'" + set + "'";
					handler.setCollection(facets[0]);
				}
				set = set.toLowerCase();
				if (set.endsWith("zelay"))
					set = "vezelay";
//				String URI = "http://images.library.pitt.edu/cgi-bin/i/image/image-idx?med=1;"
//						+ "sid=c39ae7f151bea87291bdd4bc1787196f;c="
//						+ set
//						+ ";evl=full-image;quality=4;"
//						+ "view=entry;subview=detail;lasttype=boolean;cc="
//						+ set
//						+ ";entryid=x-"
//						+ colValue("Identifier")
//						+ ";viewid=" + colValue("Image");

				String URI = "http://images.library.pitt.edu/cgi-bin/i/image/image-idx?med=1;" +
						"sid=8f283c84306a0146a0b4a40704f9b234;c=visuals;q1=visuals;rgn1=visuals_all;" +
						"evl=full-image;quality=3;view=entry;subview=detail;lasttype=boolean;cc=visuals;entryid=x-"
						+ colValue("identifier") + ";viewid=" + colValue("Image");

				parseDCfield("identifier", URI);
				parseDCfield("title", colValue("Title of Work"));
				// parseDCfield("title", colValue("Title of Image"));
				// parseDCfield("description", colValue("Description"));
				// String startDate = colValue("Start Date");
				// String endDate = colValue("End Date");
				// String date;
				// if (startDate != null) {
				// if (endDate != null)
				// date = startDate + "-" + endDate;
				// else
				// date = startDate;
				// } else
				// date = endDate;
				// parseDCfield("date", date);
				// parseDCfield("creator", colValue("Photographer"));

				// String[] subject = { colValue("Type of Work"),
				// colValue("Subtype of Work"),
				// colValue("Category of Work") };
				// subject = (String[]) Util.deleteEquals(subject, "",
				// String.class);
				// String subjects = Util.join(subject, " -- ");
				// // h.put(subjects, this);
				// parseDCfield("subject", subjects);

				parseDCfield("subject", Util.join(colValue("Type of Work")
						.split(";"), " -- "));
				// parseField("Subtype of Work", colValue("Subtype of Work"));
				// parseField("Category of Work", colValue("Category of Work"));
				// String[] material = colValue("Material").split(",");
				// for (int i = 0; i < material.length; i++) {
				// parseField("Material", material[i].trim());
				// }
				// parseField("Style", colValue("Style"));
				// parseField("Condition", colValue("Condition"));
				// parseField("Bibliography",
				// URLtail(colValue("Bibliography")));
				// parseField("Diagram", URLtail(colValue("Diagram")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	String URLtail(String URL) {
		int index = URL != null ? URL.lastIndexOf('/') : -1;
		return index < 0 ? URL : URL.substring(index + 1);
	}

//	private void parseField(String field, String value) throws SAXException {
//		if (value != null) {
//			handler.data = new StringBuffer(value);
//			handler.endElement(null, null, field);
//		}
//	}

	private void parseDCfield(String field, String value) {
		if (value != null) {
			handler.data = new StringBuffer(value);
			handler.endElement(null, null, "dc:" + field);
		}
	}
}
