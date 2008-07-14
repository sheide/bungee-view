package edu.cmu.cs.bungee.dbScripts;

import java.util.Hashtable;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Maps from a field name (like 651a or topic) to a Field.
 * 
 */
class Decoder {

	private Hashtable<String, Field> codes = new Hashtable<String, Field>();

	Field decode(String name) {
		return codes.get(name);
	}

	void addCode(String name, Field field) {
//		Util.print("addCode " + name + " " + field);
		codes.put(name, field);
	}

}

class DC extends Decoder {

	private static DC self;

	static DC getInstance() {
		if (self == null)
			self = new DC();
		return self;
	}

	DC() {
		addCode("identifier", FunctionalAttribute.getAttribute("URI", true));
		addCode("title", Attribute.getAttribute("Title"));
		addCode("description", Attribute.getAttribute("Description"));
		addCode("publisher", Facet.getGenericFacet("Publisher"));
		addCode("format", Facet.getGenericFacet("Subject"));
		addCode("rights", Facet.getGenericFacet("Rights"));
		addCode("type", Facet.getGenericFacet("Type"));
		addCode("date", Facet.getDateFacet("Date"));
		addCode("subject", Facet.getGenericFacet("Subject"));
		addCode("language", Facet.getGenericFacet("Language"));
		addCode("creator", Facet.getGenericFacet("Creator"));
		addCode("source", Facet.getGenericFacet("Source"));
	}

}

class MARC extends Decoder {

	private static MARC self;

	static MARC getInstance() {
		if (self == null)
			self = new MARC();
		return self;
	}

	// marc21 codes explained at http://www.loc.gov/marc/bibliographic/
	Field decode(String tag, String subfield) {
		Field result = super.decode(tag + subfield);
		if (result == null)
			result = super.decode(tag);
		if (result == null) {
			Util.print("Ignoring " + tag + subfield);
		} else if (result == Field.IGNORABLE_FIELD)
			result = null;
		return result;
	}

	/**
	 * Adds all combinations of tags and subfields, mapping to field.
	 * 
	 * @param tagList
	 * @param subfieldList
	 * @param field
	 */
	void addCodes(String tagList, String subfieldList, Field field) {
		String[] tags = Util.splitComma(tagList);
		for (int i = 0; i < tags.length; i++) {
			if (subfieldList == null) {
				addCode(tags[i], field);
			} else {
				for (int j = 0; j < subfieldList.length(); j++) {
					addCode(tags[i] + subfieldList.charAt(j), field);
				}
			}
		}
	}

	private void usualSuspects(String tagList) {
		addCodes(tagList, "z", Facet.getParsingFacet("Places",
				NamedEntitySubFacetValueParser.getInstance()));
		addCodes(tagList, "y", Facet.getDateFacet("Date"));
		addCodes(tagList, "v", Facet.getSubjectFacet("Subject"));
		addCodes(tagList, null, Field.IGNORABLE_FIELD);
	}

	MARC() {
		addCode("043a", Facet.getPlace043Facet("Places"));
		addCodes("045", "ab", Facet.getDate045Facet("Date"));
		addCodes("100,110,700", "a", Facet.getGenericFacet("Creator"));
		addCodes("100,110,700", "f", Facet.getDateFacet("Date"));
		addCodes("100,110,700", "t", Attribute.getAttribute("Title"));
		addCodes("100,110,700", "bcdegpqx56", Field.IGNORABLE_FIELD);
		addCodes("130,240,245,246,740", "abps", Attribute.getAttribute("Title"));
		addCodes("130,240,245,246,740", "f", Facet
				.getDateFacet("Date of Creation"));
		addCodes("130,240,245,246,740", "h", Facet.getGenericFacet("Format"));
		addCodes("130,240,245,246,740", "l", Facet.getGenericFacet("Language"));
		addCodes("130,240,245,246,740", "cign56", Field.IGNORABLE_FIELD);
		addCodes("257,260", "ae", Facet
				.getPlaceFacet("Location of Publication"));
		addCodes("257,260", "cg", Facet.getDateFacet("Date of Creation"));
		addCodes("257,260", "bf6", Field.IGNORABLE_FIELD);
		addCode("300", Attribute.getAttribute("Description"));
		addCodes("440,490", null, Attribute.getAttribute("Series"));
		addCodes("500,504,505,511,518,545,586", null, Attribute
				.getAttribute("Note"));
		addCode("520", Attribute.getAttribute("Summary"));
		addCode("552o", Attribute.getAttribute("Note"));
		addCode("552", Field.IGNORABLE_FIELD);
		addCode("600a", Facet.getGenericFacet("Specific People"));
		usualSuspects("600,610,611,710,650,653,654,655,755,630,651,656,657,752");
		addCodes("610,710", "ab", Facet.getParsingFacet(
				"Specific Organizations", NamedEntitySubFacetValueParser
						.getInstance()));
		addCode("630a", Attribute.getAttribute("Title"));
		addCodes("650,653,654", "a", Facet.getSubjectFacet("Subject"));
		addCode("651a", Facet.getPlaceFacet("Places"));
		addCodes("655,755", "a", Facet.getGenericFacet("Format"));
		addCode("656a", Facet.getGenericFacet("Occupation"));
		addCode("657a", Facet.getGenericFacet("Function"));
		addCodes("662", "abcdfgh", Facet.getPlaceFacet("Places"));
		addCode("662", Field.IGNORABLE_FIELD);
		addCodes("242,730", "at", Attribute.getAttribute("Title"));
		addCodes("242,730", "f", Facet.getDateFacet("Date of Creation"));
		addCodes("242,730", null, Field.IGNORABLE_FIELD);
		addCodes("752", "abcdfgh", Facet.getPlaceFacet("Places"));
		addCode("856u", FunctionalAttribute.getAttribute("URI", true));
		addCodes(
				"010,017,020,034,035,037,040,041,042,050,051,052,066,072,074,082,086,090,111,250,255,265,501,506,"
						+ "507,508,510,524,530,533,534,540,541,546,550,555,561,580,581,583,585,590,711,772,773,810,830,850,"
						+ "852,856,859,880", null, Field.IGNORABLE_FIELD);
	}
}
