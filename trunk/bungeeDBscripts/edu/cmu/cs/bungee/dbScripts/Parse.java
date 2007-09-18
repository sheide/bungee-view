package edu.cmu.cs.bungee.dbScripts;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import edu.cmu.cs.bungee.javaExtensions.*;


/**
 * Here is an example of parsing of XML data with help of document-handler.
 */

public class Parse {
	/**
	 * Application entry point
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();

			SAXParser parser = factory.newSAXParser();
			CreateRawSaxHandler handler = new CreateRawSaxHandler(args[1], true);
			String directory = args[0];

			String[] collections = {

			// Historic Pittsburgh fron Archives Service Center
					"drlimg:kabib", "drlimg:pghrailbib",
					"drlimg:unionarcadebib", "drlimg:aerialbib",
					"drlimg:allegobbib", "drlimg:cpbib",
					"drlimg:darlingtonbib", "drlimg:fcoxbib", "drlimg:gnbib",
					"drlimg:gtbib", "drlimg:iksbib", "drlimg:rrbib",
					"drlimg:spencerbib", "drlimg:uapittbib", "drlimg:uebib",
					"drlimg:urbanbib", "drlimg:consolbib", "drlimg:shourekbib",
					"drlimg:smokebib",

					// From CMoA
					"drlimg:cmabib", "drlimg:cmaharrisbib",

					// From Heinz History Center
					"drlimg:accdbib", "drlimg:fwagbib", "drlimg:gretbib",
					"drlimg:hjhzbib", "drlimg:jbenbib", "drlimg:jalbib",
					"drlimg:lyshbib", "drlimg:mestbib", "drlimg:ppsbib",
					"drlimg:trimbib",

			// "drlimg:chartresbib", "drlimg:vezelaybib"

			// "drlimg:visualsbib"

			};
			for (int i = 0; i < collections.length; i++) {
				Util.print("\nParsing " + collections[i]);
				parser.parse(directory + "\\OAI\\"
						+ collections[i].replace(':', '-') + ".xml", handler);
			}
			handler.renumber();
			handler.useTGM();
			handler.collectDescriptions();
			// handler.copyImages("hp3");
			// handler.loadDRLimages(directory + "\\thumbs\\");

		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}
}
