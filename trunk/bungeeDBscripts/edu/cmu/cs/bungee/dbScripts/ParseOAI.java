package edu.cmu.cs.bungee.dbScripts;

import java.sql.SQLException;
import java.util.Hashtable;

import org.xml.sax.Attributes;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Here is an example of parsing of XML data with help of document-handler.
 * 
 * Args for HP: -db hp3 -user root -pass tartan01 -reset -renumber -verbose
 * -directory C:\Projects\ArtMuseum\HistoricPittsburgh\OAI-2007-Dec\ -files
 * drlimg
 * -accdbib.xml,drlimg-aerialbib.xml,drlimg-allegobbib.xml,drlimg-cmabib.xml
 * ,drlimg -cmaharrisbib.xml,drlimg-consolbib.xml,drlimg-cpbib.xml,drlimg-
 * darlingtonbib .xml
 * ,drlimg-fcoxbib.xml,drlimg-fwagbib.xml,drlimg-gnbib.xml,drlimg-gretbib.xml
 * ,drlimg
 * -gtbib.xml,drlimg-hjhzbib.xml,drlimg-iksbib.xml,drlimg-jalbib.xml,drlimg
 * -jbenbib.xml,drlimg-kabib.xml,drlimg-lyshbib.xml,drlimg-mestbib.xml,drlimg-
 * pghrailbib
 * .xml,drlimg-ppsbib.xml,drlimg-rrbib.xml,drlimg-shourekbib.xml,drlimg
 * -smokebib.
 * xml,drlimg-spencerbib.xml,drlimg-trimbib.xml,drlimg-uapittbib.xml,drlimg
 * -uebib.xml,drlimg-unionarcadebib.xml,drlimg-urbanbib.xml"hp-drlimg-chathambib.xml,hp-drlimg-darlfamilybib.xml,hp-drlimg-fairbanksbib.xml,hp-drlimg-kaufbib.xml,hp-drlimg-pghprintsbib.xml,hp-drlimg-rustbib.xml,hp-drlimg-stotzbib.xml,hp-drlimg-switchbib.xml"
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
 * Args for CiteSeer
 * 
 * -db CiteSeer -user root -pass tartan01 -reset -renumber -verbose -directory
 * "C:\Documents and Settings\mad\My Documents\Projects\ArtMuseum\Populate Data
 * Files" -files "C:\Documents and Settings\mad\My
 * Documents\Projects\ArtMuseum\CiteSeer\oai_citeseer\oai_citeseer1.dump"
 * -cities Places.txt -renames Rename.txt -moves
 * TGM.txt,places_hierarchy.txt,Moves.txt
 * 
 * Args for InternetArchive
 * 
 * -db InternetArchive -user root -pass tartan01 -reset -renumber -verbose
 * -directory "C:\Documents and Settings\mad\My
 * Documents\Projects\ArtMuseum\Populate Data Files" -files "C:\Documents and
 * Settings\mad\My
 * Documents\Projects\ArtMuseum\InternetArchive\images-mediatype-Image.xml"
 * -cities Places.txt -renames Rename.txt -moves
 * TGM.txt,places_hierarchy.txt,Moves.txt
 * 
 * DO NOT END DIRECTORY NAME WITH '\"'. It will quote the quotation mark.
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

public class ParseOAI extends Options {

	// private static final String namespaceMARC =
	// "http://www.loc.gov/MARC21/slim";
	// private static final String namespaceDC =
	// "http://purl.org/dc/elements/1.1/";

	/**
	 * Flag whether we're lexically inside a record
	 */
	private boolean isInRecord;

	private String tag = null;

	private String subfield = null;

	StringBuffer data;

	private String set;

	private String setSpec;

	private Hashtable<String, String> setNames = new Hashtable<String, String>();

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
			new ParseOAI().init(true, args);
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	@Override
	public void startDocument() {
		// System.out.println("Document processing started");
		// db("ALTER TABLE raw_facet ADD INDEX name (name)");
		/**
		 * Holds successive subfield values for concatenating hierarchical
		 * lists. E.g. in
		 * 
		 * <marc:subfield code="z">Virgina</marc:subfield>
		 * 
		 * <marc:subfield code="z">Richmond</marc:subfield>
		 */
		handler.parserState = new Hashtable<String, String[]>();
	}

	@Override
	public void endDocument() {
		// db("ALTER TABLE raw_facet DROP INDEX name");
		// System.out.println("Document processing finished");
		Util.print("...done\n");
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
				handler.newItem();
				if (set != null) {
					try {
						Field field = Facet.getDateFacet("Collection");
						boolean inserted = field.insert(set, handler);
						assert inserted : field + " '" + set + "'";
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else if (localName.equals("request")) {
			// Initial ListRecords request will specify the set. (Subsequent
			// requests with a ResumptionToken won't specify set, but it
			// will be
			// the same one.)
			String setSpec1 = attrs.getValue("set");
			if (setSpec1 != null) {
				set = setNames.get(setSpec1);
				// Util.print(setSpec + " => " + set);
				assert set != null;
			}
		} else if (localName.equals("setSpec") || localName.equals("setName")) {
			// These are only used as pairs to remember the association
			// between
			// the short name and the descriptive name. Only the request/set
			// above associates records with sets.
			data = new StringBuffer();
		} else if (qName.startsWith("marc:")) {
			if (localName.equals("datafield")) {
				clearSubfacetState();
				// ind1 = attrs.getValue("ind1");
				// ind2 = attrs.getValue("ind2");
				tag = attrs.getValue("tag");
			} else if (localName.equals("controlfield")) {
				clearSubfacetState();
				// ind1 = attrs.getValue("ind1");
				// ind2 = attrs.getValue("ind2");
				tag = attrs.getValue("tag");
				data = new StringBuffer();
			} else if (isInRecord && localName.equals("subfield")) {
				subfield = attrs.getValue("code");
				data = new StringBuffer();
			}
		} else if (qName.startsWith("dc:")) {
			if (isInRecord) {
				// tag = qName;
				data = new StringBuffer();
			}
		}
	}

	@SuppressWarnings("unchecked")
	void clearSubfacetState() {
		((Hashtable<String, String[]>) handler.parserState).clear();
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		// StringBuffer temp = new StringBuffer();
		// temp.append(ch, start, length);
		// Util.print("characters: " + temp);
		if (data != null) {
			data.append(ch, start, length);
		}
	}

	// private String whereAmI() {
	// return "tag=" + tag + "; subfield=" + subfield + "; data=" + data;
	// }

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (localName.equals("record"))
			isInRecord = false;
		if (data != null && data.length() > 1) {
			String value = GenericFacetValueParser.trim(data.toString());

			// Util.print("endElement handling "
			// + (isMARC ? tag + " " + subfield + " " : "") + qName + " "
			// + value);
			try {
				if (localName.equals("setSpec")) {
					setSpec = value;
				} else if (localName.equals("setName")) {
					setNames.put(setSpec, value);
				} else {
					Field field = null;
					if (qName.startsWith("marc:")) {
						if (localName.equals("subfield")) {
							field = MARC.getInstance().decode(tag, subfield);
						}
						// field = Facet.getGenericFacet(localName, this);
						assert field != null
								|| localName.equals("controlfield") : "endElement not handling "
								+ tag
								+ " "
								+ subfield
								+ " "
								+ qName + " " + uri;
					} else if (qName.startsWith("dc:")) {
						field = DC.getInstance().decode(localName);
						assert field != null : "endElement not handling " + qName + " " + uri;
					}
					if (field != null) {
						// Util.print("endElement " + recordNum + " " + field +
						// " "
						// + value);
						try {
							field.insert(value, handler);
						} catch (SQLException e) {
							e.printStackTrace();
						}
						// Util.print("...endElement ");
					}

				}
			} catch (Throwable e) {
				throw new IllegalArgumentException("While parsing " + qName
						+ " = " + value, e);
			}
		}
		data = null;
	}
}
